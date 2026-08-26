package org.zoxweb.server.net.protocols.pqc;

import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVLong;
import org.zoxweb.shared.util.NVStringList;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The TLS-posture sweep orchestrator (META-TCP-PQC.md §7): runs {@link TCPPQCProtocol} sessions
 * against one endpoint with different pinned offers and aggregates the matrix — a
 * <b>baseline</b> session (default offer, full capture, embedded at the report's top level), a
 * <b>version matrix</b> (one session per candidate protocol version, pinned as the only enabled
 * version), a <b>group matrix</b> (hybrid-first-preferred, PQ-hybrid-only and classical-only
 * offers), and an opt-in
 * <b>cipher matrix</b> (per-version suite discovery by shrinking-offer loops plus a
 * server-preference confirmation). One handshake negotiates one version, one group, and one
 * cipher; the sweep is how "which versions / which suites / which posture" gets answered.
 * <p>
 * Per-candidate status: {@code supported} (handshake completed), {@code refused} (TLS-level
 * rejection — a refused legacy version is a finding, not an error — or an offer the local
 * provider cannot express, reported without opening a session), {@code error} (transport
 * failure or timeout — no posture information). A baseline transport error short-circuits the
 * matrices: an unreachable endpoint would only repeat the same failure per candidate.
 * <p>
 * One sweep runs its sessions sequentially on one {@link NIOSocket} — owned by the sweep, or
 * caller-supplied so many concurrent sweeps share one selector (the fleet form). The
 * {@code auditAsync} variants deliver the same report to a callback from a {@link TaskUtil}
 * worker instead of blocking the caller. Contains no Bouncy Castle imports — the BC seam stays
 * in {@link PQCUtil} behind {@link TCPPQCProtocol}.
 */
public final class PQCSweep {

    public static final long DEFAULT_SESSION_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);

    /** Version candidates, strongest first; each pinned as the only enabled protocol. */
    public static final String[] VERSION_CANDIDATES = {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};

    /** The PQ-hybrid-only group offer — completing is the strong form of PQ readiness. */
    public static final String[] PQ_ONLY_GROUPS =
            {"X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024"};

    /**
     * The hybrid-first full offer — the detection form: hybrids ahead of the classical
     * fallback, with a hybrid {@code key_share} (the group setter pins it). A PQ-capable server
     * negotiates PQ here even when the provider's classical-first default offer hides it, while
     * a PQ-less server still completes classically.
     */
    public static final String[] PQ_PREFERRED_GROUPS =
            {"X25519MLKEM768", "SecP256r1MLKEM768", "SecP384r1MLKEM1024",
                    "x25519", "secp256r1", "secp384r1"};

    /** The classical-only group offer — probes the non-PQ fallback posture. */
    public static final String[] CLASSICAL_ONLY_GROUPS = {"x25519", "secp256r1", "secp384r1"};

    private PQCSweep() {
    }

    /** Full sweep with the default per-session timeout, without cipher enumeration. */
    public static NVGenericMap audit(InetSocketAddress remote) {
        return audit(remote, DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    /** Full sweep without cipher enumeration. */
    public static NVGenericMap audit(InetSocketAddress remote, long sessionTimeoutMillis) {
        return audit(remote, sessionTimeoutMillis, false);
    }

    /** Full sweep, optional cipher matrix, no revocation check. */
    public static NVGenericMap audit(InetSocketAddress remote, long sessionTimeoutMillis,
                                     boolean enumerateCiphers) {
        return audit(remote, sessionTimeoutMillis, enumerateCiphers, false);
    }

    /**
     * Runs the full sweep: baseline, version matrix, group matrix, and optionally the cipher
     * matrix and the revocation check. Always returns a report — failures are recorded in it,
     * never thrown.
     *
     * @param remote               the endpoint to audit
     * @param sessionTimeoutMillis per-session completion wait
     * @param enumerateCiphers     opt-in cipher matrix (META-TCP-PQC.md §7) — one handshake per
     *                             discovered suite plus a terminating refusal per version, so it
     *                             multiplies the sweep's handshake count
     * @param checkRevocation      opt-in leaf revocation check ({@link PQCRevocation}) on the
     *                             baseline's captured chain: stapled OCSP → active OCSP → CRL,
     *                             soft-fail
     * @return the aggregated report: baseline keys at top level, plus {@code versions} and
     * {@code groups} sub-maps with one {@code status}/{@code reason} entry per candidate, a
     * {@code ciphers} sub-map when enumeration ran, and a {@code revocation} sub-map when the
     * check ran
     */
    public static NVGenericMap audit(InetSocketAddress remote, long sessionTimeoutMillis,
                                     boolean enumerateCiphers, boolean checkRevocation) {
        NIOSocket nioSocket = null;
        try {
            try {
                nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            } catch (Exception e) {
                return new NVGenericMap("pqc_sweep").build("error", "selector setup failed: "
                        + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }
            return audit(nioSocket, remote, sessionTimeoutMillis, enumerateCiphers, checkRevocation);
        } finally {
            SharedIOUtil.close(nioSocket);
        }
    }

    /**
     * Sweep on a caller-supplied {@link NIOSocket} — the fleet form: many concurrent sweeps
     * share one selector instead of each creating its own. The socket is never closed here;
     * its lifecycle belongs to the caller.
     */
    public static NVGenericMap audit(NIOSocket nioSocket, InetSocketAddress remote,
                                     long sessionTimeoutMillis, boolean enumerateCiphers,
                                     boolean checkRevocation) {
        NVGenericMap report = new NVGenericMap("pqc_sweep");
        long startMillis = System.currentTimeMillis();
        try {
            // baseline: default offer, full capture, embedded at top level
            SessionOutcome baseline = run(nioSocket, remote, null, null, sessionTimeoutMillis);
            for (GetNameValue<?> gnv : baseline.results.values())
                report.add(gnv);
            if (!"supported".equals(baseline.status)) {
                report.build("baseline_status", baseline.status);
                if (baseline.reason != null)
                    report.build("baseline_reason", baseline.reason);
                if ("error".equals(baseline.status))
                    return report; // unreachable endpoint: the matrices would only repeat it
            }

            // version matrix — a candidate the local provider cannot enable (e.g. disabled by
            // jdk.tls.disabledAlgorithms) is refused without a session: pinning it anyway makes
            // BCJSSE throw "No usable protocols enabled" past the session's failure hooks, and
            // the candidate would burn the full timeout to report a fake transport error
            List<String> localVersions = localProtocols();
            NVGenericMap versions = new NVGenericMap("versions");
            for (String version : VERSION_CANDIDATES)
                versions.add(candidate(version, localVersions.isEmpty() || localVersions.contains(version)
                        ? run(nioSocket, remote, null, new String[]{version}, sessionTimeoutMillis)
                        : new SessionOutcome("refused", "disabled or unsupported by the local provider",
                        new NVGenericMap("results"))));
            report.add(versions);

            // group matrix — "default" summarized from the baseline, the pinned offers probed
            SessionOutcome pqPreferred = run(nioSocket, remote, PQ_PREFERRED_GROUPS, null, sessionTimeoutMillis);
            SessionOutcome pqOnly = run(nioSocket, remote, PQ_ONLY_GROUPS, null, sessionTimeoutMillis);
            NVGenericMap groups = new NVGenericMap("groups");
            groups.add(candidate("default", baseline));
            groups.add(candidate("pq_preferred", pqPreferred));
            groups.add(candidate("pq_only", pqOnly));
            groups.add(candidate("classical_only",
                    run(nioSocket, remote, CLASSICAL_ONLY_GROUPS, null, sessionTimeoutMillis)));
            report.add(groups);

            // cipher matrix (opt-in): per-version discovery loops + preference confirmation
            if (enumerateCiphers)
                report.add(cipherMatrix(nioSocket, remote, sessionTimeoutMillis));

            // revocation (opt-in): baseline chain, stapled OCSP → active OCSP → CRL, soft-fail
            if (checkRevocation && baseline.chain != null && baseline.chain.length > 0)
                report.add(PQCRevocation.check(baseline.chain[0],
                        baseline.chain.length > 1 ? baseline.chain[1] : null,
                        baseline.stapledOCSP, sessionTimeoutMillis));

            // pqc_ready summary — facts, not a grade (META-TCP-PQC.md §8): kex readiness is the
            // operative question (harvest-now-decrypt-later); the certificate fact rides along
            boolean kexDefault = Boolean.TRUE.equals(baseline.results.getValue("pqc_kex"));
            boolean kexPreferred = "supported".equals(pqPreferred.status)
                    && Boolean.TRUE.equals(pqPreferred.results.getValue("pqc_kex"));
            boolean kexSupported = "supported".equals(pqOnly.status)
                    && Boolean.TRUE.equals(pqOnly.results.getValue("pqc_kex"));
            boolean cert = Boolean.TRUE.equals(baseline.results.getValue("pqc_cert"));
            NVGenericMap summary = new NVGenericMap("pqc_ready");
            summary.build(new NVBoolean("ready", kexDefault || kexPreferred || kexSupported))
                    .build(new NVBoolean("kex_default", kexDefault))
                    .build(new NVBoolean("kex_preferred", kexPreferred))
                    .build(new NVBoolean("kex_supported", kexSupported))
                    .build(new NVBoolean("cert", cert));
            report.add(summary);

            return report;
        } finally {
            // stamped on every path, the early baseline-error return included
            report.build(new NVLong("sweep_duration_ms", System.currentTimeMillis() - startMillis));
        }
    }

    /** Asynchronous sweep on the default {@link TaskUtil} processor. */
    public static void auditAsync(InetSocketAddress remote, long sessionTimeoutMillis,
                                  boolean enumerateCiphers, boolean checkRevocation,
                                  Consumer<NVGenericMap> callback) {
        auditAsync((Executor) null, remote, sessionTimeoutMillis, enumerateCiphers, checkRevocation, callback);
    }

    /**
     * Asynchronous sweep: returns immediately and delivers the finished report — the exact
     * report the synchronous {@code audit} returns — to the callback on the given executor
     * (any {@code ExecutorService} qualifies; null uses the default {@link TaskUtil}
     * processor). One task stays busy per running sweep; failures land in the report, never in
     * a thrown exception.
     */
    public static void auditAsync(Executor executor, final InetSocketAddress remote,
                                  final long sessionTimeoutMillis, final boolean enumerateCiphers,
                                  final boolean checkRevocation, final Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback is required", callback);
        (executor != null ? executor : TaskUtil.defaultTaskProcessor()).execute(
                () -> callback.accept(audit(remote, sessionTimeoutMillis, enumerateCiphers, checkRevocation)));
    }

    /** Asynchronous shared-socket sweep on the default {@link TaskUtil} processor. */
    public static void auditAsync(NIOSocket nioSocket, InetSocketAddress remote,
                                  long sessionTimeoutMillis, boolean enumerateCiphers,
                                  boolean checkRevocation, Consumer<NVGenericMap> callback) {
        auditAsync(null, nioSocket, remote, sessionTimeoutMillis, enumerateCiphers, checkRevocation, callback);
    }

    /**
     * Asynchronous sweep on a caller-supplied (shared) {@link NIOSocket} and executor — the
     * fleet form: launch one per endpoint against a single socket and collect the reports as
     * they arrive. The socket is never closed here; its lifecycle belongs to the caller. A
     * null executor uses the default {@link TaskUtil} processor.
     */
    public static void auditAsync(Executor executor, final NIOSocket nioSocket,
                                  final InetSocketAddress remote, final long sessionTimeoutMillis,
                                  final boolean enumerateCiphers, final boolean checkRevocation,
                                  final Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback and nioSocket are required", callback, nioSocket);
        (executor != null ? executor : TaskUtil.defaultTaskProcessor()).execute(
                () -> callback.accept(audit(nioSocket, remote, sessionTimeoutMillis, enumerateCiphers, checkRevocation)));
    }

    // ---- per-session plumbing ----

    private static final class SessionOutcome {
        final String status;   // supported / refused / error
        final String reason;   // null when supported
        final NVGenericMap results;
        final X509Certificate[] chain;  // as sent by the server, when captured
        final byte[] stapledOCSP;       // handshake-stapled, when present

        SessionOutcome(String status, String reason, NVGenericMap results) {
            this(status, reason, results, null, null);
        }

        SessionOutcome(String status, String reason, NVGenericMap results,
                       X509Certificate[] chain, byte[] stapledOCSP) {
            this.status = status;
            this.reason = reason;
            this.results = results;
            this.chain = chain;
            this.stapledOCSP = stapledOCSP;
        }
    }

    private static SessionOutcome run(NIOSocket nioSocket, InetSocketAddress remote,
                                      String[] namedGroups, String[] protocols, long timeoutMillis) {
        return run(nioSocket, remote, namedGroups, protocols, null, timeoutMillis);
    }

    /** One audited session with the given pinned offers; never throws. */
    private static SessionOutcome run(NIOSocket nioSocket, InetSocketAddress remote,
                                      String[] namedGroups, String[] protocols, String[] ciphers,
                                      long timeoutMillis) {
        TCPPQCProtocol session = null;
        try {
            session = new TCPPQCProtocol(null, remote, namedGroups, protocols, ciphers);
            nioSocket.addClientSocket(session);
            boolean closed = session.waitForClose(timeoutMillis);
            NVGenericMap results = session.getResults();
            X509Certificate[] chain = session.getCapturedChain();
            byte[] stapled = session.getStapledOCSP();
            if (results.getValue("tls_protocol") != null)
                return new SessionOutcome("supported", null, results, chain, stapled);
            if (!closed)
                return new SessionOutcome("error", "no completion within timeout", results, chain, stapled);
            Throwable cause = session.getCloseCause();
            // a TLS-level rejection is a refusal (a finding); anything else is transport
            boolean refused = cause instanceof SSLException;
            String reason = cause != null
                    ? (cause.getMessage() != null ? cause.getMessage() : cause.toString())
                    : "session failed";
            return new SessionOutcome(refused ? "refused" : "error", reason, results, chain, stapled);
        } catch (Exception e) {
            // an offer the local stack cannot express is a refusal of the candidate, not transport
            return new SessionOutcome("refused",
                    e.getMessage() != null ? e.getMessage() : e.toString(), new NVGenericMap("results"));
        } finally {
            SharedIOUtil.close(session);
        }
    }

    /** Folds one outcome into the matrix entry: status, negotiated parameters, reason. */
    private static NVGenericMap candidate(String name, SessionOutcome outcome) {
        NVGenericMap entry = new NVGenericMap(name);
        entry.build("status", outcome.status);
        copy(outcome.results, entry, "tls_protocol");
        copy(outcome.results, entry, "tls_cipher");
        copy(outcome.results, entry, "tls_kex_group");
        if (outcome.results.getNV("pqc_kex") != null)
            entry.add((GetNameValue<?>) outcome.results.getNV("pqc_kex"));
        if (outcome.reason != null)
            entry.build("reason", outcome.reason);
        return entry;
    }

    private static void copy(NVGenericMap from, NVGenericMap to, String key) {
        Object v = from.getValue(key);
        if (v instanceof String)
            to.build(key, (String) v);
    }

    // ---- cipher matrix (META-TCP-PQC.md §7) ----

    /**
     * Cipher-suite enumeration: per version, offer the provider's full candidate list, record
     * the server's pick, drop it from the next offer, and repeat until the server refuses. The
     * discovery order is the server's preference order over the offered set. A reversed
     * two-suite offer pair then confirms whether the server enforces its own order.
     */
    private static NVGenericMap cipherMatrix(NIOSocket nioSocket, InetSocketAddress remote,
                                             long timeoutMillis) {
        NVGenericMap ciphers = new NVGenericMap("ciphers");
        List<String> tls13 = discoverCiphers(nioSocket, remote, "TLSv1.3", providerSuites(true), timeoutMillis);
        List<String> tls12 = discoverCiphers(nioSocket, remote, "TLSv1.2", providerSuites(false), timeoutMillis);
        ciphers.add(new NVStringList("tls13", tls13));
        ciphers.add(new NVStringList("tls12", tls12));

        // preference confirmation on the richest version with at least two discovered suites
        List<String> basis = tls12.size() >= 2 ? tls12 : tls13;
        String version = tls12.size() >= 2 ? "TLSv1.2" : "TLSv1.3";
        if (basis.size() >= 2) {
            String first = negotiatedCipher(nioSocket, remote, version,
                    new String[]{basis.get(0), basis.get(1)}, timeoutMillis);
            String second = negotiatedCipher(nioSocket, remote, version,
                    new String[]{basis.get(1), basis.get(0)}, timeoutMillis);
            if (first != null && second != null)
                ciphers.build(new NVBoolean("server_cipher_preference", first.equals(second)));
        }
        return ciphers;
    }

    /** One version's discovery loop; never throws, returns the suites in discovery order. */
    static List<String> discoverCiphers(NIOSocket nioSocket, InetSocketAddress remote,
                                        String version, List<String> remaining,
                                        long timeoutMillis) {
        List<String> discovered = new ArrayList<String>();
        while (!remaining.isEmpty()) {
            SessionOutcome outcome = run(nioSocket, remote, null, new String[]{version},
                    remaining.toArray(new String[0]), timeoutMillis);
            if (!"supported".equals(outcome.status))
                break;
            Object negotiated = outcome.results.getValue("tls_cipher");
            // the pinned offer guarantees membership; a foreign pick would loop forever
            if (!(negotiated instanceof String) || !remaining.remove(negotiated))
                break;
            discovered.add((String) negotiated);
        }
        return discovered;
    }

    /** The negotiated suite of one session with the given pinned offer, or null. */
    private static String negotiatedCipher(NIOSocket nioSocket, InetSocketAddress remote,
                                           String version, String[] offer, long timeoutMillis) {
        Object v = run(nioSocket, remote, null, new String[]{version}, offer, timeoutMillis)
                .results.getValue("tls_cipher");
        return v instanceof String ? (String) v : null;
    }

    /**
     * The protocol versions the local BCJSSE provider can actually offer — the default engine's
     * enabled set, i.e. supported minus {@code jdk.tls.disabledAlgorithms}. Empty when not
     * determinable; the version matrix then probes every candidate rather than mask the sweep.
     */
    static List<String> localProtocols() {
        List<String> out = new ArrayList<String>();
        try {
            for (String p : PQCUtil.createClientContext(false).createSSLEngine().getEnabledProtocols())
                out.add(p);
        } catch (GeneralSecurityException e) {
            // not determinable: fall back to probing
        }
        return out;
    }

    /**
     * The offerable candidate universe: the local BCJSSE provider's supported suites for one
     * version family — TLS 1.3 suites carry no key-exchange component in the name (no
     * {@code "_WITH_"}). Signaling values ({@code *_SCSV}) are not offerable suites. Suites the
     * provider does not implement cannot be probed.
     */
    static List<String> providerSuites(boolean tls13) {
        List<String> out = new ArrayList<String>();
        try {
            for (String name : PQCUtil.createClientContext(false).createSSLEngine().getSupportedCipherSuites()) {
                if (name.endsWith("_SCSV"))
                    continue;
                if (!name.contains("_WITH_") == tls13)
                    out.add(name);
            }
        } catch (GeneralSecurityException e) {
            // no candidates: the matrix reports empty lists
        }
        return out;
    }

    // ---- manual runner ----

    /**
     * Sweeps one endpoint:
     * <pre>
     *   PQCSweep &lt;host[:port]&gt; [-ciphers] [-revocation] [-grade]   (port defaults to 443)
     * </pre>
     * {@code -ciphers} adds the opt-in cipher matrix, {@code -revocation} the opt-in leaf
     * revocation check, {@code -grade} prints the {@link PQCGrader} verdict alongside the
     * report (the report itself stays facts-only). Prints the aggregated report; exit codes:
     * 0 baseline supported, 1 baseline refused/failed, 64 usage.
     */
    public static void main(String[] args) {
        int exit = 64;
        try {
            boolean enumerateCiphers = false;
            boolean checkRevocation = false;
            boolean gradeIt = false;
            boolean usage = args.length < 1;
            for (int i = 1; i < args.length; i++) {
                if ("-ciphers".equals(args[i]))
                    enumerateCiphers = true;
                else if ("-revocation".equals(args[i]))
                    checkRevocation = true;
                else if ("-grade".equals(args[i]))
                    gradeIt = true;
                else
                    usage = true;
            }
            if (usage) {
                System.err.println("usage: PQCSweep <host[:port]> [-ciphers] [-revocation] [-grade]");
                System.exit(64);
            }
            String host = args[0];
            int port = 443;
            int c = host.lastIndexOf(':');
            if (c > 0) {
                port = Integer.parseInt(host.substring(c + 1));
                host = host.substring(0, c);
            }
            System.out.println("SWEEPING " + host + ":" + port
                    + (enumerateCiphers ? " (with cipher matrix)" : "")
                    + (checkRevocation ? " (with revocation)" : ""));
            NVGenericMap report = audit(new InetSocketAddress(host, port),
                    DEFAULT_SESSION_TIMEOUT_MILLIS, enumerateCiphers, checkRevocation);
            System.out.println("report: " + report);
            Object summary = report.getNV("pqc_ready");
            if (summary instanceof NVGenericMap)
                System.out.println("pqc_ready: " + ((NVGenericMap) summary).getValue("ready"));
            if (gradeIt)
                System.out.println("grade: " + PQCGrader.grade(report));
            exit = report.getValue("tls_protocol") != null ? 0 : 1;
            System.out.println("verdict: " + (exit == 0 ? "SWEPT (exit 0)" : "BASELINE FAILED (exit 1)"));
        } catch (Exception e) {
            System.err.println("sweep error: " + e);
            exit = 64;
        }
        System.exit(exit);
    }
}
