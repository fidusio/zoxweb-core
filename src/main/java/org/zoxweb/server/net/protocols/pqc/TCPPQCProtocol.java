package org.zoxweb.server.net.protocols.pqc;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVLong;

import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The TLS-posture auditor session (META-TCP-PQC.md): one connection, one handshake, one
 * observation. Opens a BCJSSE-driven TLS connection to the endpoint, and at handshake completion
 * ({@link #sslUpgraded(SSLConfigInt)} — the designed completion seam) captures the negotiated
 * session, the post-quantum key-exchange posture, and the full certificate chain, evaluates the
 * chain offline (PKIX against the JVM trust store, hostname, expiration), records everything in
 * the results bag, and closes. No application data is exchanged in either direction.
 * <p>
 * The handshake runs observe-first (trust-all): it completes against any certificate —
 * self-signed, expired, wrong host — and certificate judgment is reported, never thrown
 * (META-TCP-PQC.md §4). The SSL driver is untouched: this class is a completion target riding
 * the standard {@code TCPSessionCallback} pre-set-context upgrade path.
 * <p>
 * Every Bouncy Castle interaction is delegated to {@link PQCUtil} — the {@code pqc} package is
 * the only place in the tree with BC imports.
 */
public class TCPPQCProtocol extends TCPSessionCallback {

    public static final LogWrapper log = new LogWrapper(TCPPQCProtocol.class).setEnabled(false);

    public static final int DEFAULT_EXPIRY_THRESHOLD_DAYS = 30;

    private final NVGenericMap results = new NVGenericMap("results");
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private volatile Throwable closeCause;
    private final long openMillis = System.currentTimeMillis();
    private volatile int expiryThresholdDays = DEFAULT_EXPIRY_THRESHOLD_DAYS;
    private final AtomicBoolean clientCertRequested = new AtomicBoolean();
    private volatile X509Certificate[] capturedChain;
    private volatile X509Certificate[] completedChain;
    private volatile byte[] stapledOCSP;

    /** Audits the endpoint with the provider's default named-group offer. */
    public TCPPQCProtocol(String id, InetSocketAddress remote) throws GeneralSecurityException {
        this(id, remote, null);
    }

    /**
     * Audits the endpoint with a pinned named-group offer — the sweep's per-session control:
     * a PQ-only offer ({@code {"X25519MLKEM768"}}) that completes is the strong form of PQ
     * readiness; a classical-only offer probes the fallback posture.
     *
     * @param id          session id, null for a generated one
     * @param remote      the endpoint to audit (hostname used for SNI)
     * @param namedGroups the pinned offer, null for the provider default
     */
    public TCPPQCProtocol(String id, InetSocketAddress remote, String[] namedGroups)
            throws GeneralSecurityException {
        this(id, remote, namedGroups, null);
    }

    /**
     * Audits the endpoint with a pinned named-group offer and/or a pinned protocol-version
     * offer — the sweep's per-session controls (META-TCP-PQC.md §7): a single pinned version
     * probes "does the server accept this version", a pinned group list probes the key-exchange
     * posture.
     *
     * @param id          session id, null for a generated one
     * @param remote      the endpoint to audit (hostname used for SNI)
     * @param namedGroups the pinned group offer, null for the provider default
     * @param protocols   the pinned protocol versions (e.g. {@code {"TLSv1.2"}}), null for the
     *                    provider default
     */
    public TCPPQCProtocol(String id, InetSocketAddress remote, String[] namedGroups, String[] protocols)
            throws GeneralSecurityException {
        this(id, remote, namedGroups, protocols, null);
    }

    /**
     * Audits the endpoint with pinned named-group, protocol-version and/or cipher-suite offers.
     * Cipher pinning is the sweep's enumeration control (META-TCP-PQC.md §7): offering a
     * shrinking suite list discovers the server's supported set one handshake at a time.
     *
     * @param id          session id, null for a generated one
     * @param remote      the endpoint to audit (hostname used for SNI)
     * @param namedGroups the pinned group offer, null for the provider default
     * @param protocols   the pinned protocol versions, null for the provider default
     * @param ciphers     the pinned cipher-suite offer (provider suite names), null for the
     *                    provider default
     */
    public TCPPQCProtocol(String id, InetSocketAddress remote, String[] namedGroups,
                          String[] protocols, String[] ciphers)
            throws GeneralSecurityException {
        super(id);
        SSLContextInfo info = new SSLContextInfo(PQCUtil.createClientContext(false, clientCertRequested),
                remote, protocols, ciphers);
        info.setSSLGroupSetter(PQCUtil.engineConfigurer(
                namedGroups != null && namedGroups.length > 0 ? namedGroups : null, PQCUtil.DEFAULT_ALPN));
        setSSLContextInfo(info);
    }

    /** @return the audit report (final once the session is closed) */
    public NVGenericMap getResults() {
        return results;
    }

    /** @return the session's terminating cause, or null (clean audit — or a session still running) */
    public Throwable getCloseCause() {
        return closeCause;
    }

    /** Days-to-expiry threshold for the {@code expires_soon} flag (default 30). */
    public TCPPQCProtocol expiryThresholdDays(int days) {
        this.expiryThresholdDays = days;
        return this;
    }

    /** @return the peer chain as sent by the server (no appended anchor), or null */
    public X509Certificate[] getCapturedChain() {
        return capturedChain;
    }

    /** @return the chain completed with the resolved trust anchor; the sent chain when none */
    public X509Certificate[] getCompletedChain() {
        return completedChain != null ? completedChain : capturedChain;
    }

    /** @return the DER-encoded OCSP response the server stapled, or null */
    public byte[] getStapledOCSP() {
        return stapledOCSP;
    }

    /**
     * Pull-style completion wait: true once the session is closed (the report is final), false
     * on timeout.
     */
    public boolean waitForClose(long timeoutMillis) {
        if (isClosed() || timeoutMillis <= 0)
            return isClosed();
        try {
            return closeLatch.await(timeoutMillis, TimeUnit.MILLISECONDS) || isClosed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return isClosed();
        }
    }

    // ---- TCPSessionCallback hooks ----

    /** Not reached on the pre-set-context path (the upgrade rides {@code connected()}); defensive no-op. */
    @Override
    protected void connectedFinished() {
    }

    /** The auditor exchanges no application data; anything the peer sends post-handshake is ignored. */
    @Override
    public void accept(ByteBuffer byteBuffer) {
    }

    /**
     * Handshake completion — the single introspection point (META-TCP-PQC.md §3): capture,
     * evaluate, close. Every capture is best-effort; a single extraction failing leaves its keys
     * absent, never fails the audit.
     */
    @Override
    protected void sslUpgraded(SSLConfigInt sci) throws IOException {
        try {
            introspect(sci);
        } finally {
            results.build(new NVLong("latency_ms", System.currentTimeMillis() - openMillis));
            SharedIOUtil.close(this);
        }
    }

    /** Failure path: stash the close cause, record the error, close once. */
    @Override
    public void exception(Throwable e) {
        if (log.isEnabled()) log.getLogger().info("exception: " + e);
        if (!isClosed()) {
            closeCause = e;
            if (results.getNV("error") == null)
                results.build("error", e != null
                        ? (e.getMessage() != null ? e.getMessage() : e.toString())
                        : "session failed");
            // a server may abort after demanding a client certificate — that demand is a finding
            if (clientCertRequested.get() && results.getNV("client_cert_requested") == null)
                results.build(new NVBoolean("client_cert_requested", true));
            results.build(new NVLong("latency_ms", System.currentTimeMillis() - openMillis));
            SharedIOUtil.close(this);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            closeLatch.countDown();
        }
    }

    // ---- introspection (META-TCP-PQC.md §5) ----

    private void introspect(SSLConfigInt sci) {
        SSLSession session = sci.getSSLEngine().getSession();
        try {
            results.build("tls_protocol", session.getProtocol())
                    .build("tls_cipher", session.getCipherSuite());
        } catch (RuntimeException e) {
            if (log.isEnabled()) log.getLogger().info("session introspection failed: " + e);
        }

        // PQ key exchange — negotiated named group via the BC seam; absent is a legal answer
        int group = PQCUtil.negotiatedGroup(sci.getSSLEngine());
        String groupName = PQCUtil.groupName(group);
        if (groupName != null) {
            results.build("tls_kex_group", groupName)
                    .build(new NVBoolean("pqc_kex", PQCUtil.isPQGroup(group)));
        }

        // ALPN, stapled OCSP, client-cert demand — best-effort, absent when not seen
        String alpn = PQCUtil.applicationProtocol(sci.getSSLEngine());
        if (alpn != null)
            results.build("alpn", alpn);
        stapledOCSP = PQCUtil.stapledOCSPResponse(sci.getSSLEngine());
        if (stapledOCSP != null)
            results.build(new NVBoolean("ocsp_stapled", true));
        if (clientCertRequested.get())
            results.build(new NVBoolean("client_cert_requested", true));

        // certificate chain capture + offline evaluation
        try {
            Certificate[] rawChain = session.getPeerCertificates();
            if (rawChain != null && rawChain.length > 0 && rawChain[0] instanceof X509Certificate) {
                X509Certificate[] chain = new X509Certificate[rawChain.length];
                for (int i = 0; i < rawChain.length; i++)
                    chain[i] = (X509Certificate) rawChain[i];
                capturedChain = chain;
                evaluateAndCapture(chain);
            }
        } catch (Exception e) {
            if (log.isEnabled()) log.getLogger().info("certificate introspection failed: " + e);
        }
    }

    private void captureChain(X509Certificate[] chain) {
        NVGenericMap chainMap = new NVGenericMap("cert_chain");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            NVGenericMap certMap = new NVGenericMap("cert_" + i);
            certMap.build("subject", cert.getSubjectX500Principal().getName())
                    .build("issuer", cert.getIssuerX500Principal().getName())
                    .build("serial", cert.getSerialNumber().toString(16))
                    .build("sig_alg", cert.getSigAlgName())
                    .build("key_alg", cert.getPublicKey().getAlgorithm())
                    .build(new NVLong("key_size", keySize(cert.getPublicKey())))
                    .build("not_before", formatDate(cert.getNotBefore().getTime()))
                    .build("not_after", formatDate(cert.getNotAfter().getTime()));
            chainMap.add(certMap);
        }
        results.add(chainMap);

        X509Certificate leaf = chain[0];
        long daysToExpiry = (leaf.getNotAfter().getTime() - System.currentTimeMillis())
                / TimeUnit.DAYS.toMillis(1);
        results.build(new NVLong("days_to_expiry", daysToExpiry))
                .build(new NVBoolean("expires_soon", daysToExpiry < expiryThresholdDays))
                .build(new NVBoolean("pqc_cert", PQCUtil.isPQSignature(leaf)));
    }

    /**
     * Offline evaluation and report capture: PKIX verdict, the reported chain completed with
     * the resolved trust anchor (servers don't send the root), whole-chain time validity, and
     * the hostname match.
     */
    private void evaluateAndCapture(X509Certificate[] chain) {
        TrustOutcome trust = validateChain(chain);
        results.build(new NVBoolean("chain_trusted", trust.reason == null));
        if (trust.reason != null)
            results.build("chain_reason", trust.reason);

        X509Certificate[] displayChain = chain;
        X509Certificate last = chain[chain.length - 1];
        boolean selfSignedLast = last.getSubjectX500Principal().equals(last.getIssuerX500Principal());
        if (trust.anchor != null && !selfSignedLast) {
            displayChain = new X509Certificate[chain.length + 1];
            System.arraycopy(chain, 0, displayChain, 0, chain.length);
            displayChain[chain.length] = trust.anchor;
        }
        completedChain = displayChain;
        captureChain(displayChain);

        boolean timeValid = true;
        for (X509Certificate cert : displayChain) {
            try {
                cert.checkValidity();
            } catch (Exception expiredOrNotYet) {
                timeValid = false;
                break;
            }
        }
        results.build(new NVBoolean("chain_time_valid", timeValid));

        InetSocketAddress remote = getRemoteAddress();
        if (remote != null)
            results.build(new NVBoolean("hostname_match", hostnameMatch(remote.getHostString(), chain[0])));
    }

    private static final class TrustOutcome {
        String reason;          // null = trusted
        X509Certificate anchor; // the trust-store root, when identifiable
    }

    /** Offline PKIX evaluation against the JVM trust store; resolves the anchor when trusted. */
    private static TrustOutcome validateChain(X509Certificate[] chain) {
        TrustOutcome outcome = new TrustOutcome();
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    X509TrustManager xtm = (X509TrustManager) tm;
                    xtm.checkServerTrusted(chain, authType(chain[0].getPublicKey()));
                    outcome.anchor = resolveAnchor(chain[chain.length - 1], xtm.getAcceptedIssuers());
                    return outcome;
                }
            }
            outcome.reason = "no default X509TrustManager";
        } catch (Exception e) {
            outcome.reason = e.getMessage() != null ? e.getMessage() : e.toString();
        }
        return outcome;
    }

    /** The trust-store root that issued the last sent certificate, when identifiable. */
    private static X509Certificate resolveAnchor(X509Certificate last, X509Certificate[] roots) {
        if (roots != null) {
            for (X509Certificate root : roots) {
                if (root.getSubjectX500Principal().equals(last.getIssuerX500Principal())) {
                    try {
                        last.verify(root.getPublicKey());
                        return root;
                    } catch (Exception wrongKey) {
                        // same DN, different key (cross-signs): keep looking
                    }
                }
            }
        }
        return null;
    }

    /** Maps the leaf key algorithm to the key-exchange-style authType {@code checkServerTrusted} expects. */
    private static String authType(PublicKey key) {
        String alg = key.getAlgorithm();
        if ("RSA".equalsIgnoreCase(alg))
            return "RSA";
        if ("EC".equalsIgnoreCase(alg) || "EdDSA".equalsIgnoreCase(alg))
            return "ECDHE_ECDSA";
        if ("DSA".equalsIgnoreCase(alg))
            return "DHE_DSS";
        return "UNKNOWN";
    }

    private static boolean hostnameMatch(String host, X509Certificate leaf) {
        try {
            Collection<List<?>> sans = leaf.getSubjectAlternativeNames();
            if (sans != null) {
                for (List<?> san : sans) {
                    // dNSName(2) and iPAddress(7) entries
                    Integer type = (Integer) san.get(0);
                    if ((type == 2 || type == 7) && dnsMatch(host, String.valueOf(san.get(1))))
                        return true;
                }
                return false; // SANs present: CN fallback is not permitted
            }
            String dn = leaf.getSubjectX500Principal().getName();
            for (String part : dn.split(",")) {
                part = part.trim();
                if (part.startsWith("CN="))
                    return dnsMatch(host, part.substring(3));
            }
        } catch (Exception e) {
            // fall through: unmatchable
        }
        return false;
    }

    private static boolean dnsMatch(String host, String pattern) {
        host = host.toLowerCase();
        pattern = pattern.toLowerCase();
        if (pattern.startsWith("*.")) {
            int dot = host.indexOf('.');
            return dot > 0 && host.substring(dot + 1).equals(pattern.substring(2));
        }
        return host.equals(pattern);
    }

    static long keySize(PublicKey key) {
        if (key instanceof RSAPublicKey)
            return ((RSAPublicKey) key).getModulus().bitLength();
        if (key instanceof ECPublicKey)
            return ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
        if (key instanceof DSAPublicKey)
            return ((DSAPublicKey) key).getParams().getP().bitLength();
        return -1;
    }

    private static String formatDate(long millis) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(millis);
    }

    // ---- manual runner (META-TCP-PQC.md §10 live smoke) ----

    /**
     * Audits one endpoint:
     * <pre>
     *   TCPPQCProtocol &lt;host[:port]&gt; [group,group,...]   (port defaults to 443)
     * </pre>
     * Prints the report; exit codes: 0 audited, 1 failed, 2 no completion, 64 usage.
     */
    public static void main(String[] args) {
        int exit = 64;
        NIOSocket nioSocket = null;
        try {
            if (args.length < 1 || args.length > 2) {
                System.err.println("usage: TCPPQCProtocol <host[:port]> [group,group,...]");
                System.exit(64);
            }
            String host = args[0];
            int port = 443;
            int c = host.lastIndexOf(':');
            if (c > 0) {
                port = Integer.parseInt(host.substring(c + 1));
                host = host.substring(0, c);
            }
            String[] groups = args.length == 2 ? args[1].split(",") : null;

            TCPPQCProtocol audit = new TCPPQCProtocol(null, new InetSocketAddress(host, port), groups);
            System.out.println("AUDITING " + host + ":" + port
                    + (groups != null ? " groups=" + args[1] : " (default offer)"));
            nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            nioSocket.addClientSocket(audit);

            boolean closed = audit.waitForClose(TimeUnit.SECONDS.toMillis(15));
            System.out.println("results: " + audit.getResults());
            if (audit.getCloseCause() != null)
                System.out.println("cause: " + audit.getCloseCause());
            exit = !closed ? 2
                    : (audit.getCloseCause() == null && audit.getResults().getNV("error") == null ? 0 : 1);
            System.out.println("verdict: " + (exit == 0 ? "AUDITED (exit 0)"
                    : exit == 2 ? "NO COMPLETION (exit 2)" : "FAILED (exit 1)"));
            SharedIOUtil.close(audit);
        } catch (Exception e) {
            System.err.println("audit error: " + e);
            exit = 64;
        } finally {
            SharedIOUtil.close(nioSocket);
        }
        System.exit(exit);
    }
}
