package org.zoxweb.server.net.protocols.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.BCExtendedSSLSession;
import org.bouncycastle.jsse.BCSSLConnection;
import org.bouncycastle.jsse.BCSSLEngine;
import org.bouncycastle.jsse.BCSSLParameters;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.tls.NamedGroup;
import org.bouncycastle.tls.SecurityParameters;
import org.bouncycastle.tls.TlsContext;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.protocols.ProtoUtil.ResKey;
import org.zoxweb.server.net.ssl.SSLCheckDisabler;
import org.zoxweb.server.security.SSLGroupSetterInt;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Bouncy Castle seam of the PQC auditor (META-TCP-PQC.md §6): every BC type the auditor
 * touches — provider construction, BCJSSE client contexts, named-group pinning, negotiated-group
 * introspection, PQ algorithm classification — lives here so the {@code pqc} package is the only
 * place in the tree with a Bouncy Castle import. The rest of the stack (the SSL driver, the
 * session callbacks, the meta validators) stays provider-agnostic {@code javax.net.ssl}.
 * <p>
 * Providers load through {@link SecUtil#loadProviders()} — the same registered BC / BCJSSE /
 * BCPQC set the rest of the security stack uses — resolved lazily on first use; contexts are
 * still created against the provider instance, never by name lookup.
 */
public final class PQCUtil {

    private PQCUtil() {
    }

    // the BCJSSE provider for the process, registered through SecUtil.loadProviders()
    private static volatile Provider bcJsseProvider;

    /**
     * @return the process-wide BCJSSE provider, lazily resolved through
     * {@link SecUtil#loadProviders()} — the same registered instance the rest of the security
     * stack uses (BC at position 1, BCJSSE at position 2). Falls back to a private
     * crypto-bound instance only if registration failed. Contexts are still created against
     * the instance, never by name lookup.
     */
    public static Provider bcJsseProvider() {
        if (bcJsseProvider == null) {
            synchronized (PQCUtil.class) {
                if (bcJsseProvider == null) {
                    // request OCSP stapling (status_request) unless the deployment decided
                    // otherwise — BCJSSE reads the standard JSSE property name
                    if (System.getProperty("jdk.tls.client.enableStatusRequestExtension") == null)
                        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
                    SecUtil.loadProviders();
                    Provider registered = SecUtil.getProvider(SecUtil.BC_BCJSSE);
                    bcJsseProvider = registered != null ? registered
                            : new BouncyCastleJsseProvider(new BouncyCastleProvider());
                }
            }
        }
        return bcJsseProvider;
    }

    /**
     * Builds a BCJSSE client {@code SSLContext}. With {@code certValidation} true the provider's
     * default trust (the JVM trust store) applies and an untrusted peer fails the handshake; with
     * false the handshake is observe-first (META-TCP-PQC.md §4) — it completes against any
     * certificate and judgment happens offline from the captured chain.
     */
    public static SSLContext createClientContext(boolean certValidation) throws GeneralSecurityException {
        return createClientContext(certValidation, null);
    }

    /**
     * Client context with an optional client-cert-request recorder: BCJSSE consults the key
     * manager when the server sends a CertificateRequest, so the recorder flips the flag and
     * offers no certificate — the fact ({@code client_cert_requested}) is captured without any
     * mTLS support.
     * <p>
     * One context per session, deliberately — never cache or share the result
     * (META-TCP-PQC.md §6). A shared BCJSSE context owns a session cache keyed by peer
     * host:port with no per-context way to disable resumption ({@code setSessionCacheSize(0)}
     * means unlimited), and a resumed handshake skips the key exchange and certificate steps
     * the audit observes. The recorder flag is per session too. A warm build costs
     * single-digit milliseconds.
     */
    public static SSLContext createClientContext(boolean certValidation, final AtomicBoolean clientCertRequested)
            throws GeneralSecurityException {
        SSLContext ctx = SSLContext.getInstance("TLS", bcJsseProvider());
        KeyManager[] keyManagers = clientCertRequested == null ? null : new KeyManager[]{
                new X509ExtendedKeyManager() {
                    @Override
                    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
                        clientCertRequested.set(true);
                        return null;
                    }

                    @Override
                    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                        clientCertRequested.set(true);
                        return null;
                    }

                    @Override
                    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                        return null;
                    }

                    @Override
                    public X509Certificate[] getCertificateChain(String alias) {
                        return null;
                    }

                    @Override
                    public String[] getClientAliases(String keyType, Principal[] issuers) {
                        return null;
                    }

                    @Override
                    public String[] getServerAliases(String keyType, Principal[] issuers) {
                        return null;
                    }

                    @Override
                    public PrivateKey getPrivateKey(String alias) {
                        return null;
                    }
                }
        };
        ctx.init(keyManagers,
                certValidation ? null : SSLCheckDisabler.SINGLETON.getTrustManagers(),
                new SecureRandom());
        return ctx;
    }

    /** The ALPN protocols a browser-like audit offers. */
    public static final String[] DEFAULT_ALPN = {"h2", "http/1.1"};

    /** Named-group pinning only, no ALPN — see {@link #engineConfigurer(String[], String[])}. */
    public static SSLGroupSetterInt groupSetter(final String[] groups) {
        return engineConfigurer(groups, null);
    }

    /**
     * @return an engine configurer riding the {@link SSLGroupSetterInt} hook: pins the
     * named-group offer through {@code BCSSLParameters} (e.g. {@code "X25519MLKEM768"} for a
     * PQ-only offer) — the sweep's per-session control (META-TCP-PQC.md §7) — and offers the
     * given ALPN protocols. Null groups leave the provider's offer untouched; a non-BC engine
     * passes through untouched.
     * <p>
     * Group pinning also pins the early {@code key_share} to the offer's first group (plus the
     * {@code x25519} fallback when offered): the provider's default key_share is classical-only,
     * so without this a client-order-honoring server picks {@code x25519} without a
     * HelloRetryRequest and a hybrid-first offer never reveals the endpoint's PQ support.
     */
    public static SSLGroupSetterInt engineConfigurer(final String[] groups, final String[] alpnProtocols) {
        return new SSLGroupSetterInt() {
            @Override
            public String[] getGroups() {
                return groups;
            }

            @Override
            public SSLEngine setGroups(SSLEngine sslEngine) {
                if (sslEngine instanceof BCSSLEngine) {
                    BCSSLEngine bc = (BCSSLEngine) sslEngine;
                    BCSSLParameters params = bc.getParameters();
                    if (groups != null && groups.length > 0) {
                        params.setNamedGroups(groups);
                        params.setEarlyKeyShares(earlyKeyShares(groups));
                    }
                    if (alpnProtocols != null && alpnProtocols.length > 0)
                        params.setApplicationProtocols(alpnProtocols);
                    bc.setParameters(params);
                }
                return sslEngine;
            }
        };
    }

    /** @return the negotiated ALPN protocol of a completed handshake, or null */
    public static String applicationProtocol(SSLEngine engine) {
        if (engine instanceof BCSSLEngine) {
            String protocol = ((BCSSLEngine) engine).getApplicationProtocol();
            return protocol != null && !protocol.isEmpty() ? protocol : null;
        }
        return null;
    }

    /**
     * @return the DER-encoded OCSP response the server stapled during the handshake
     * (RFC 6066 status_request, requested by default), or null when none was stapled
     */
    public static byte[] stapledOCSPResponse(SSLEngine engine) {
        try {
            if (engine instanceof BCSSLEngine) {
                BCExtendedSSLSession session = ((BCSSLEngine) engine).getBCSession();
                if (session != null) {
                    List<byte[]> responses = session.getStatusResponses();
                    if (responses != null && !responses.isEmpty())
                        return responses.get(0);
                }
            }
        } catch (Exception e) {
            // best-effort capture: absent is a legal answer
        }
        return null;
    }

    /** Key material goes with the top preference, plus the {@code x25519} fallback when offered. */
    private static String[] earlyKeyShares(String[] groups) {
        for (String group : groups) {
            if ("x25519".equalsIgnoreCase(group) && !group.equalsIgnoreCase(groups[0]))
                return new String[]{groups[0], group};
        }
        return new String[]{groups[0]};
    }

    /**
     * @return the negotiated {@link NamedGroup} code of a completed BCJSSE handshake, or -1 when
     * not determinable (non-BC engine, handshake not finished, or provider internals moved).
     * Best-effort: the public surface is {@code BCSSLEngine.getConnection()}; the one reflective
     * hop is the provider-internal {@code getTlsContext()}, after which
     * {@code SecurityParameters.getNegotiatedGroup()} is public {@code org.bouncycastle.tls} API.
     */
    public static int negotiatedGroup(SSLEngine engine) {
        try {
            if (engine instanceof BCSSLEngine) {
                BCSSLConnection connection = ((BCSSLEngine) engine).getConnection();
                if (connection != null) {
                    Method m = connection.getClass().getDeclaredMethod("getTlsContext");
                    m.setAccessible(true);
                    TlsContext tlsContext = (TlsContext) m.invoke(connection);
                    SecurityParameters sp = tlsContext.getSecurityParametersConnection();
                    if (sp != null)
                        return sp.getNegotiatedGroup();
                }
            }
        } catch (Exception e) {
            // best-effort introspection: absent is a legal answer
        }
        return -1;
    }

    /** @return the name of a named-group code ({@code "X25519MLKEM768"}), or null */
    public static String groupName(int namedGroup) {
        if (namedGroup < 0)
            return null;
        String name = NamedGroup.getStandardName(namedGroup);
        if (name == null) {
            name = NamedGroup.getText(namedGroup);
            // getText appends "(0xcode)" — keep the bare name
            int p = name != null ? name.indexOf('(') : -1;
            if (p > 0)
                name = name.substring(0, p);
        }
        return name;
    }

    /**
     * @return true when the named-group code is a KEM or KEM-hybrid group (ML-KEM and friends) —
     * the {@code pqc_kex} classification
     */
    public static boolean isPQGroup(int namedGroup) {
        if (namedGroup < 0)
            return false;
        try {
            if (NamedGroup.getKemName(namedGroup) != null)
                return true;
        } catch (Exception e) {
            // fall through to the name heuristic
        }
        String name = groupName(namedGroup);
        return name != null && name.toLowerCase().contains("mlkem");
    }

    private static final String[] PQ_SIG_TOKENS = {
            "ML-DSA", "MLDSA", "SLH-DSA", "SLHDSA", "DILITHIUM", "SPHINCS", "FALCON", "HASH-ML-DSA"
    };

    /**
     * @return true when the certificate's signature algorithm is post-quantum (ML-DSA / SLH-DSA
     * families, by name or NIST OID arc) — the {@code pqc_cert} classification
     */
    public static boolean isPQSignature(X509Certificate cert) {
        String name = cert.getSigAlgName();
        if (name != null) {
            String upper = name.toUpperCase();
            for (String token : PQ_SIG_TOKENS) {
                if (upper.contains(token))
                    return true;
            }
        }
        // NIST sigAlgs arc 2.16.840.1.101.3.4.3.x: ML-DSA = 17/18/19, HashML-DSA = 32/33/34,
        // SLH-DSA = 20..31, Hash SLH-DSA = 35..46
        String oid = cert.getSigAlgOID();
        if (oid != null && oid.startsWith("2.16.840.1.101.3.4.3.")) {
            try {
                int tail = Integer.parseInt(oid.substring("2.16.840.1.101.3.4.3.".length()));
                return tail >= 17 && tail <= 46;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    // ---- PQ-first client strategy: force the hybrid, downgrade on failure ----

    /** The strict PQ-first offer: the PQ-hybrid groups and nothing else. */
    public static final String[] PQ_STRICT_GROUPS =
            {"X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024"};

    /**
     * PQ-first audit of one endpoint: the client forces the PQ-hybrid groups
     * ({@link #PQ_STRICT_GROUPS}) as the <b>only</b> offer; if that handshake fails or times
     * out, it downgrades to the
     * provider's default offer and audits with that. The returned report carries which offer
     * produced it — {@code offer}: {@code "pq_only"} or {@code "downgraded_default"} — plus
     * {@code pq_only_reason} on the downgrade path. Never throws; failures land in the report.
     *
     * @param remote               the endpoint to audit
     * @param sessionTimeoutMillis per-attempt completion wait
     * @return the audit report of the attempt that connected (or the downgrade attempt's
     * failure report when neither did)
     */
    public static NVGenericMap pqFirstAudit(java.net.InetSocketAddress remote, long sessionTimeoutMillis) {
        NIOSocket nioSocket = null;
        try {
            try {
                nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            } catch (Exception e) {
                return new NVGenericMap("results").build("offer", "none")
                        .build(ResKey.ERROR, "selector setup failed: "
                                + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }

            TCPPQCProtocol pq = null;
            String pqReason = null;
            try {
                pq = new TCPPQCProtocol(null, remote, PQ_STRICT_GROUPS);
                nioSocket.addClientSocket(pq);
                boolean done = pq.waitForClose(sessionTimeoutMillis);
                if (done && pq.getResults().getValue(ResKey.TLS_PROTOCOL) != null) {
                    pq.getResults().build("offer", "pq_only");
                    return pq.getResults();
                }
                pqReason = pq.getCloseCause() != null
                        ? (pq.getCloseCause().getMessage() != null
                        ? pq.getCloseCause().getMessage() : pq.getCloseCause().toString())
                        : (done ? "no TLS session" : "no completion within timeout");
            } catch (Exception e) {
                pqReason = e.getMessage() != null ? e.getMessage() : e.toString();
            } finally {
                SharedIOUtil.close(pq);
            }

            // downgrade: the provider default offer
            TCPPQCProtocol fallback = null;
            try {
                fallback = new TCPPQCProtocol(null, remote);
                nioSocket.addClientSocket(fallback);
                fallback.waitForClose(sessionTimeoutMillis);
                NVGenericMap results = fallback.getResults();
                results.build("offer", "downgraded_default")
                        .build("pq_only_reason", pqReason);
                return results;
            } catch (Exception e) {
                NVGenericMap results = fallback != null ? fallback.getResults() : new NVGenericMap("results");
                results.build("offer", "downgraded_default")
                        .build("pq_only_reason", pqReason)
                        .build(ResKey.ERROR, e.getMessage() != null ? e.getMessage() : e.toString());
                return results;
            } finally {
                SharedIOUtil.close(fallback);
            }
        } finally {
            SharedIOUtil.close(nioSocket);
        }
    }

    // ---- manual runner: PQ-first with downgrade ----

    /**
     * PQ-first check of one endpoint — forces the PQ-hybrid-only offer, and downgrades to the
     * default offer when it fails:
     * <pre>
     *   PQCUtil &lt;host[:port]&gt;   (port defaults to 443)
     * </pre>
     * Exit codes: 0 PQ-only handshake succeeded, 1 downgraded (endpoint reachable but not PQ),
     * 2 unreachable, 64 usage.
     */
    public static void main(String[] args) {
        int exit = 64;
        try {
            if (args.length != 1) {
                System.err.println("usage: PQCUtil <host[:port]>");
                System.exit(64);
            }
            String host = args[0];
            int port = 443;
            int c = host.lastIndexOf(':');
            if (c > 0) {
                port = Integer.parseInt(host.substring(c + 1));
                host = host.substring(0, c);
            }
            NVGenericMap report = pqFirstAudit(new java.net.InetSocketAddress(host, port),
                    java.util.concurrent.TimeUnit.SECONDS.toMillis(15));

            Object offer = report.getValue("offer");
            if ("pq_only".equals(offer)) {
                System.out.println("PQ-ONLY OK   : " + report.getValue(ResKey.TLS_PROTOCOL)
                        + " kex=" + report.getValue(ResKey.TLS_KEX_GROUP)
                        + " cipher=" + report.getValue(ResKey.TLS_CIPHER));
                exit = 0;
            } else if (report.getValue(ResKey.TLS_PROTOCOL) != null) {
                System.out.println("DOWNGRADED   : pq-only failed (" + report.getValue("pq_only_reason") + ")");
                System.out.println("default offer: " + report.getValue(ResKey.TLS_PROTOCOL)
                        + " kex=" + report.getValue(ResKey.TLS_KEX_GROUP)
                        + " pqc_kex=" + report.getValue("pqc_kex"));
                exit = 1;
            } else {
                System.out.println("UNREACHABLE  : pq-only (" + report.getValue("pq_only_reason")
                        + "), default (" + report.getValue(ResKey.ERROR) + ")");
                exit = 2;
            }
            System.out.println("issuer       : " + issuerOf(report));
        } catch (Exception e) {
            System.err.println("pqc check error: " + e);
            exit = 64;
        }
        System.exit(exit);
    }

    /** The leaf issuer from a report's cert chain — the measurement-authenticity (MITM) check. */
    private static Object issuerOf(NVGenericMap report) {
        Object chain = report.getNV("cert_chain");
        if (chain instanceof NVGenericMap) {
            Object leaf = ((NVGenericMap) chain).getNV("cert_0");
            if (leaf instanceof NVGenericMap)
                return ((NVGenericMap) leaf).getValue("issuer");
        }
        return "unknown";
    }
}
