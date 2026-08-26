package org.zoxweb.server.net.protocols.pqc;

import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVInt;
import org.zoxweb.shared.util.NVLong;
import org.zoxweb.shared.util.NVStringList;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The check facade — the second bundled report consumer (META-TCP-PQC.md §8): one call, one
 * kebab-case report shaped like the hosted {@code check-qdz} service. The <b>summary</b> form
 * is a single hybrid-first session (falling back to the default offer); <b>detailed</b> adds
 * the supported protocol versions, the enumerated cipher suites with strength classification,
 * and the leaf revocation check. Facts come from the same {@link TCPPQCProtocol} sessions the
 * sweep uses — the facade only reshapes and classifies.
 * <p>
 * The API mirrors the sweep's: synchronous {@code check} on an owned or caller-supplied
 * (shared) {@link NIOSocket}, and {@code checkAsync} variants delivering the same report to a
 * callback from a {@link TaskUtil} worker.
 */
public final class PQCCheck {

    public static final long DEFAULT_SESSION_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private PQCCheck() {
    }

    /** Summary check with the default per-session timeout. */
    public static NVGenericMap check(InetSocketAddress remote) {
        return check(remote, DEFAULT_SESSION_TIMEOUT_MILLIS, false);
    }

    /**
     * @param remote        the endpoint to check
     * @param timeoutMillis per-session completion wait; {@code <= 0} uses the default
     * @param detailed      add protocol versions, cipher suites, and the revocation check
     * @return the kebab-case report; failures land in it ({@code overall-status: ERROR}),
     * never thrown
     */
    public static NVGenericMap check(InetSocketAddress remote, long timeoutMillis, boolean detailed) {
        NIOSocket nioSocket = null;
        try {
            try {
                nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            } catch (Exception e) {
                String message = "selector setup failed: "
                        + (e.getMessage() != null ? e.getMessage() : e.toString());
                return new NVGenericMap("check")
                        .build("host", remote.getHostString())
                        .build(new NVInt("port", remote.getPort()))
                        .build("secure", "no")
                        .build("error-message", message)
                        .build("trust-verdict", "UNKNOWN")
                        .build("trust-reason", message)
                        .build("overall-status", "ERROR");
            }
            return check(nioSocket, remote, timeoutMillis, detailed);
        } finally {
            SharedIOUtil.close(nioSocket);
        }
    }

    /**
     * Check on a caller-supplied {@link NIOSocket} — the fleet form: many concurrent checks
     * share one selector. The socket is never closed here; its lifecycle belongs to the caller.
     */
    public static NVGenericMap check(NIOSocket nioSocket, InetSocketAddress remote,
                                     long timeoutMillis, boolean detailed) {
        NVGenericMap out = new NVGenericMap("check");
        long startMillis = System.currentTimeMillis();
        long timeout = timeoutMillis > 0 ? timeoutMillis : DEFAULT_SESSION_TIMEOUT_MILLIS;
        out.build("host", remote.getHostString())
                .build(new NVInt("port", remote.getPort()))
                .build("scan-id", UUID.randomUUID().toString());

        TCPPQCProtocol session = null;
        try {
            // the hosted service's client shape: hybrids first; fall back to the default offer
            session = runSession(nioSocket, remote, PQCSweep.PQ_PREFERRED_GROUPS, timeout);
            if (session == null || session.getResults().getValue("tls_protocol") == null) {
                // fall back only when the failure might be offer-dependent (a TLS-level
                // rejection, or a hang with no cause); a transport failure (refused,
                // unreachable) would just repeat identically and double the wait
                Throwable cause = session != null ? session.getCloseCause() : null;
                boolean transportFailure = cause != null && !(cause instanceof SSLException);
                if (!transportFailure) {
                    SharedIOUtil.close(session);
                    session = runSession(nioSocket, remote, null, timeout);
                }
            }
            NVGenericMap results = session != null ? session.getResults() : new NVGenericMap("results");
            boolean success = results.getValue("tls_protocol") != null;
            if (!success) {
                // the service's scan-level error shape: no success key, error-message + UNKNOWN verdict
                Object error = results.getValue("error");
                String message = friendlyError(error != null ? String.valueOf(error) : null);
                out.build("secure", "no")
                        .build("error-message", message)
                        .build("trust-verdict", "UNKNOWN")
                        .build("trust-reason", message)
                        .build("overall-status", "ERROR");
                return out;
            }
            out.build(new NVBoolean("success", true))
                    .build("secure", "yes");

            Object tlsProtocol = results.getValue("tls_protocol");
            Object cipher = results.getValue("tls_cipher");
            Object group = results.getValue("tls_kex_group");
            String tlsVersion = String.valueOf(tlsProtocol);
            boolean pqKex = Boolean.TRUE.equals(results.getValue("pqc_kex"));
            out.build("tls-version", tlsVersion)
                    .build(new NVBoolean("tls-version-pqc-capable", "TLSv1.3".equals(tlsVersion)))
                    .build("key-exchange-type", kexType(group != null ? group.toString() : null))
                    .build("key-exchange-algorithm", group != null ? group.toString() : "UNKNOWN")
                    .build(new NVBoolean("key-exchange-pqc-ready", pqKex))
                    .build("cipher-suite", String.valueOf(cipher));

            X509Certificate[] chain = session.getCapturedChain();
            String trustVerdict = "UNKNOWN";
            boolean pqCert = false;
            if (chain != null && chain.length > 0) {
                X509Certificate leaf = chain[0];
                pqCert = PQCUtil.isPQSignature(leaf);
                String validity = validityState(leaf);
                boolean chainTrusted = Boolean.TRUE.equals(results.getValue("chain_trusted"));
                boolean chainTimeValid = !Boolean.FALSE.equals(results.getValue("chain_time_valid"));
                out.build("cert-signature-type", signatureType(leaf.getSigAlgName()))
                        .build("cert-signature-algorithm", leaf.getSigAlgName().toUpperCase())
                        .build("cert-public-key-type", publicKeyType(leaf.getPublicKey().getAlgorithm()))
                        .build(new NVLong("cert-public-key-size", TCPPQCProtocol.keySize(leaf.getPublicKey())))
                        .build("cert-not-before", isoMillis(leaf.getNotBefore()))
                        .build("cert-not-after", isoMillis(leaf.getNotAfter()))
                        .build(new NVBoolean("cert-time-valid", "VALID".equals(validity)))
                        .build("cert-validity-state", validity)
                        .build(new NVBoolean("cert-chain-time-valid", chainTimeValid))
                        .build(new NVBoolean("cert-chain-valid", chainTrusted))
                        .build("cert-chain-trust", chainTrusted ? "TRUSTED" : "UNTRUSTED")
                        .build(new NVBoolean("cert-hostname-valid",
                                Boolean.TRUE.equals(results.getValue("hostname_match"))))
                        .build("cert-subject", leaf.getSubjectX500Principal().getName())
                        .build("cert-issuer", leaf.getIssuerX500Principal().getName());
                out.add(chainMap(session.getCompletedChain()));
                trustVerdict = !chainTrusted ? "UNTRUSTED_CHAIN"
                        : !chainTimeValid ? "CHAIN_TIME_INVALID"
                        : "EXPIRED".equals(validity) ? "EXPIRED"
                        : "NOT_YET_VALID".equals(validity) ? "NOT_YET_VALID"
                        : "TRUSTED";
            }

            if (detailed) {
                detail(out, nioSocket, remote, session, timeout);
                if ("REVOKED".equals(out.getValue("revocation-status")))
                    trustVerdict = "REVOKED";
            }

            Object chainReason = results.getValue("chain_reason");
            out.build("trust-verdict", trustVerdict)
                    .build("trust-reason", "TRUSTED".equals(trustVerdict)
                            ? "Certificate chain anchors to a trusted Root CA [TRUSTED]"
                            : (chainReason != null ? String.valueOf(chainReason) : trustVerdict));

            // no-sneak semantics: a trust failure outranks PQ readiness
            String overall;
            if (!"TRUSTED".equals(trustVerdict) && !"UNKNOWN".equals(trustVerdict))
                overall = "UNTRUSTED";
            else if (pqKex)
                overall = "READY";
            else if ("TLSv1.3".equals(tlsVersion))
                overall = "PARTIAL";
            else
                overall = "NOT_READY";
            out.build("overall-status", overall);

            NVGenericMap recommendations = new NVGenericMap("recommendations");
            if (!pqKex)
                recommendations.build("enable-pqc-key-exchange",
                        "Enable PQC hybrid key exchange (X25519MLKEM768 or SecP256r1MLKEM768)");
            if (!pqCert)
                recommendations.build("upgrade-to-pqc-certificate",
                        "Consider migrating to PQC certificates (ML-DSA) for full quantum resistance");
            out.add(recommendations);
            return out;
        } catch (Exception e) {
            String message = friendlyError(e.getMessage() != null ? e.getMessage() : e.toString());
            if (out.getNV("secure") == null)
                out.build("secure", "no");
            if (out.getNV("error-message") == null)
                out.build("error-message", message);
            if (out.getNV("trust-verdict") == null)
                out.build("trust-verdict", "UNKNOWN").build("trust-reason", message);
            if (out.getNV("overall-status") == null)
                out.build("overall-status", "ERROR");
            return out;
        } finally {
            SharedIOUtil.close(session);
            out.build(new NVLong("scan-time-in-ms", System.currentTimeMillis() - startMillis));
        }
    }

    /** Asynchronous check on the default {@link TaskUtil} processor. */
    public static void checkAsync(InetSocketAddress remote, long timeoutMillis,
                                  boolean detailed, Consumer<NVGenericMap> callback) {
        checkAsync((Executor) null, remote, timeoutMillis, detailed, callback);
    }

    /**
     * Asynchronous check: returns immediately and delivers the finished report — the exact
     * report the synchronous {@code check} returns — to the callback on the given executor
     * (any {@code ExecutorService} qualifies; null uses the default {@link TaskUtil}
     * processor). Failures land in the report, never in a thrown exception.
     */
    public static void checkAsync(Executor executor, final InetSocketAddress remote,
                                  final long timeoutMillis, final boolean detailed,
                                  final Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback is required", callback);
        (executor != null ? executor : TaskUtil.defaultTaskProcessor()).execute(
                () -> callback.accept(check(remote, timeoutMillis, detailed)));
    }

    /** Asynchronous shared-socket check on the default {@link TaskUtil} processor. */
    public static void checkAsync(NIOSocket nioSocket, InetSocketAddress remote,
                                  long timeoutMillis, boolean detailed,
                                  Consumer<NVGenericMap> callback) {
        checkAsync(null, nioSocket, remote, timeoutMillis, detailed, callback);
    }

    /**
     * Asynchronous check on a caller-supplied (shared) {@link NIOSocket} and executor — the
     * fleet form: launch one per endpoint against a single socket and collect the reports as
     * they arrive. The socket is never closed here; its lifecycle belongs to the caller. A
     * null executor uses the default {@link TaskUtil} processor.
     */
    public static void checkAsync(Executor executor, final NIOSocket nioSocket,
                                  final InetSocketAddress remote, final long timeoutMillis,
                                  final boolean detailed, final Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback and nioSocket are required", callback, nioSocket);
        (executor != null ? executor : TaskUtil.defaultTaskProcessor()).execute(
                () -> callback.accept(check(nioSocket, remote, timeoutMillis, detailed)));
    }

    // ---- probes ----

    /** One session with the given pinned groups; the caller closes it after reading. */
    private static TCPPQCProtocol runSession(NIOSocket nioSocket, InetSocketAddress remote,
                                             String[] groups, long timeoutMillis) {
        try {
            TCPPQCProtocol session = new TCPPQCProtocol(null, remote, groups);
            nioSocket.addClientSocket(session);
            session.waitForClose(timeoutMillis);
            return session;
        } catch (Exception e) {
            return null;
        }
    }

    /** Detailed additions: protocol versions, enumerated suites, revocation. */
    private static void detail(NVGenericMap out, NIOSocket nioSocket, InetSocketAddress remote,
                               TCPPQCProtocol baseline, long timeoutMillis) {
        // supported protocol versions — only candidates the local provider can express
        List<String> supported = new ArrayList<String>();
        List<String> local = PQCSweep.localProtocols();
        for (String version : PQCSweep.VERSION_CANDIDATES) {
            if (!local.isEmpty() && !local.contains(version))
                continue;
            TCPPQCProtocol probe = null;
            try {
                probe = new TCPPQCProtocol(null, remote, null, new String[]{version});
                nioSocket.addClientSocket(probe);
                probe.waitForClose(timeoutMillis);
                if (probe.getResults().getValue("tls_protocol") != null)
                    supported.add(version);
            } catch (Exception e) {
                // candidate not probeable: absent from the list
            } finally {
                SharedIOUtil.close(probe);
            }
        }
        out.add(new NVStringList("supported-protocol-versions", supported));
        out.build(new NVBoolean("deprecated-protocols-supported",
                supported.contains("TLSv1.1") || supported.contains("TLSv1") || supported.contains("SSLv3")));

        // enumerated suites with strength classification
        List<String> names = new ArrayList<String>();
        names.addAll(PQCSweep.discoverCiphers(nioSocket, remote, "TLSv1.3",
                PQCSweep.providerSuites(true), timeoutMillis));
        names.addAll(PQCSweep.discoverCiphers(nioSocket, remote, "TLSv1.2",
                PQCSweep.providerSuites(false), timeoutMillis));
        NVGenericMap suites = new NVGenericMap("supported-cipher-suites");
        int index = 0;
        for (String name : names) {
            NVGenericMap suite = new NVGenericMap(String.valueOf(index++));
            suite.build("name", name)
                    .build("strength", suiteStrength(name))
                    .build("key-exchange", suiteKex(name))
                    .build(new NVBoolean("forward-secrecy", forwardSecrecy(name)));
            suites.add(suite);
        }
        out.add(suites);

        // revocation: stapled OCSP -> active OCSP -> CRL, soft-fail
        X509Certificate[] chain = baseline.getCapturedChain();
        if (chain != null && chain.length > 0) {
            NVGenericMap revocation = PQCRevocation.check(chain[0],
                    chain.length > 1 ? chain[1] : null, baseline.getStapledOCSP(), timeoutMillis);
            Object status = revocation.getValue("status");
            Object source = revocation.getValue("source");
            if ("not_checked".equals(status)) {
                out.build("revocation-method", "NOT_SUPPORTED");
                Object reason = revocation.getValue("reason");
                if (reason != null)
                    out.build("revocation-error", String.valueOf(reason));
            } else {
                out.build("revocation-method", source != null ? source.toString().toUpperCase() : "UNKNOWN")
                        .build("revocation-status", status != null ? status.toString().toUpperCase() : "UNKNOWN");
            }
        }
    }

    // ---- shaping ----

    private static NVGenericMap chainMap(X509Certificate[] chain) {
        NVGenericMap chainMap = new NVGenericMap("cert-chain");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            boolean selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
            NVGenericMap entry = new NVGenericMap(String.valueOf(i));
            if (i > 0)
                entry.build(new NVInt("index", i));
            entry.build("subject", cert.getSubjectX500Principal().getName())
                    .build("issuer", cert.getIssuerX500Principal().getName())
                    .build("not-before", isoMillis(cert.getNotBefore()))
                    .build("not-after", isoMillis(cert.getNotAfter()))
                    .build(new NVBoolean("time-valid", "VALID".equals(validityState(cert))));
            if (selfSigned)
                entry.build(new NVBoolean("self-signed", true));
            if (cert.getBasicConstraints() >= 0)
                entry.build(new NVBoolean("is-ca", true));
            entry.build("role", i == 0 ? "leaf" : (selfSigned ? "root" : "intermediate"));
            chainMap.add(entry);
        }
        return chainMap;
    }

    private static String kexType(String group) {
        if (group == null)
            return "UNKNOWN";
        String upper = group.toUpperCase();
        if (upper.contains("MLKEM"))
            return "PQC_HYBRID";
        if (upper.startsWith("FFDHE"))
            return "DHE";
        if (upper.startsWith("X25519") || upper.startsWith("X448") || upper.startsWith("SECP")
                || upper.startsWith("PRIME") || upper.startsWith("BRAINPOOL"))
            return "ECDHE";
        return "UNKNOWN";
    }

    private static String signatureType(String sigAlg) {
        if (sigAlg == null)
            return "UNKNOWN";
        String upper = sigAlg.toUpperCase();
        if (upper.contains("ML-DSA") || upper.contains("MLDSA") || upper.contains("SLH")
                || upper.contains("DILITHIUM") || upper.contains("SPHINCS") || upper.contains("FALCON"))
            return "PQC_SIGNATURE";
        if (upper.contains("ECDSA"))
            return "ECDSA";
        if (upper.contains("ED25519") || upper.contains("ED448") || upper.contains("EDDSA"))
            return "EDDSA";
        if (upper.contains("RSA"))
            return "RSA";
        return "UNKNOWN";
    }

    private static String publicKeyType(String keyAlg) {
        if (keyAlg == null)
            return "UNKNOWN";
        if ("EC".equalsIgnoreCase(keyAlg))
            return "ECDSA";
        if ("RSA".equalsIgnoreCase(keyAlg))
            return "RSA";
        if (keyAlg.toUpperCase().startsWith("ED"))
            return "EDDSA";
        return keyAlg.toUpperCase();
    }

    /**
     * Maps raw stack error text to the service's friendly wording ({@code error-message} /
     * {@code trust-reason}); unrecognized text passes through unchanged.
     */
    private static String friendlyError(String raw) {
        if (raw == null)
            return "session failed";
        String lower = raw.toLowerCase();
        if (lower.contains("unexpected_message"))
            return "Peer is not speaking TLS";
        if (lower.contains("connection refused"))
            return "Connection refused";
        if (lower.contains("closed by peer") || lower.contains("connection closed")
                || lower.contains("end of stream") || lower.contains("connection reset"))
            return "Connection closed by peer";
        if (lower.contains("timed out") || lower.contains("timeout"))
            return "Connection timed out";
        if (lower.contains("unresolved") || lower.contains("unknown host") || lower.contains("getaddrinfo"))
            return "Unknown host";
        if (lower.contains("handshake_failure"))
            return "TLS handshake refused (handshake_failure)";
        return raw;
    }

    private static String validityState(X509Certificate cert) {
        try {
            cert.checkValidity();
            return "VALID";
        } catch (CertificateExpiredException e) {
            return "EXPIRED";
        } catch (CertificateNotYetValidException e) {
            return "NOT_YET_VALID";
        }
    }

    private static String suiteStrength(String name) {
        String upper = name.toUpperCase();
        for (String token : new String[]{"_RC4", "_3DES", "_DES_", "DES40", "_NULL", "EXPORT", "ANON"}) {
            if (upper.contains(token))
                return "INSECURE";
        }
        return upper.contains("_CBC_") ? "WEAK" : "STRONG";
    }

    private static String suiteKex(String name) {
        if (!name.contains("_WITH_"))
            return "ECDHE/DHE"; // TLS 1.3: key exchange lives in supported_groups
        if (name.startsWith("TLS_ECDHE"))
            return "ECDHE";
        if (name.startsWith("TLS_DHE"))
            return "DHE";
        if (name.startsWith("TLS_RSA"))
            return "RSA";
        return "UNKNOWN";
    }

    private static boolean forwardSecrecy(String name) {
        String kex = suiteKex(name);
        return !"RSA".equals(kex) && !"UNKNOWN".equals(kex);
    }

    private static String isoMillis(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(date);
    }

    // ---- manual runner ----

    /**
     * Checks one endpoint, {@code check-qdz}-shaped:
     * <pre>
     *   PQCCheck &lt;host[:port]&gt; [-detailed]   (port defaults to 443)
     * </pre>
     * Prints the report; exit codes: 0 success, 1 failed, 64 usage.
     */
    public static void main(String[] args) {
        int exit = 64;
        try {
            boolean detailed = args.length == 2 && "-detailed".equals(args[1]);
            if (args.length < 1 || args.length > 2 || (args.length == 2 && !detailed)) {
                System.err.println("usage: PQCCheck <host[:port]> [-detailed]");
                System.exit(64);
            }
            String host = args[0];
            int port = 443;
            int c = host.lastIndexOf(':');
            if (c > 0) {
                port = Integer.parseInt(host.substring(c + 1));
                host = host.substring(0, c);
            }
            NVGenericMap report = check(new InetSocketAddress(host, port),
                    DEFAULT_SESSION_TIMEOUT_MILLIS, detailed);
            System.out.println("report: " + report);
            exit = "ERROR".equals(report.getValue("overall-status")) ? 1 : 0;
        } catch (Exception e) {
            System.err.println("check error: " + e);
            exit = 64;
        }
        System.exit(exit);
    }
}
