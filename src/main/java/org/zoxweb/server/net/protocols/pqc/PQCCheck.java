package org.zoxweb.server.net.protocols.pqc;

import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.*;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The check facade — the second bundled report consumer (META-TCP-PQC.md §8): one call, one
 * kebab-case report shaped like the hosted {@code check-qdz} service. The <b>summary</b> form
 * is a single hybrid-first session (falling back to the default offer); <b>detailed</b> adds
 * the supported protocol versions, the enumerated cipher suites with strength classification,
 * and the leaf revocation check. Facts come from the same {@link TCPPQCProtocol} sessions the
 * sweep uses — the facade only reshapes and classifies.
 * <p>
 * Every entry point runs on a caller-supplied {@link NIOSocket} — unlike the sweep, the facade
 * never builds a socket of its own. A selector loop costs a thread and a {@code Selector} to
 * stand up; the caller opens one once and reuses it across checks (a fleet of concurrent
 * checks rides a single selector), and the socket's lifecycle stays entirely the caller's.
 * <p>
 * The check is event-driven end to end. Each session registers a close hook
 * ({@link TCPPQCProtocol#onClose}) before it is opened; when the session closes — handshake
 * completion, failure, or the per-session timeout closing it — the hook queues the next stage
 * on the socket's executor. No thread is ever parked on a session: {@code asyncCheck} returns
 * as soon as the first session is launched, and {@code check} is the same pipeline with the
 * caller waiting on the final report. A stage runs to completion without blocking (it reshapes
 * the facts and launches at most one session), so a fleet of checks wider than the worker
 * pool cannot starve itself; the one exception is the detailed form's revocation stage, whose
 * OCSP/CRL exchange is ordinary blocking HTTP on the worker that runs it.
 */
public final class PQCCheck {

    public static final long DEFAULT_SESSION_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private PQCCheck() {
    }

    /**
     * Summary check with the default per-session timeout.
     */
    public static NVGenericMap check(NIOSocket nioSocket, InetSocketAddress remote) {
        return check(nioSocket, remote, DEFAULT_SESSION_TIMEOUT_MILLIS, false);
    }

    /**
     * Synchronous check on a caller-supplied {@link NIOSocket}: runs the asynchronous pipeline
     * and waits for its report. Only the calling thread waits; the sessions and stages run on
     * the socket's executor. The socket is never closed here; its lifecycle belongs to the
     * caller.
     *
     * @param remote        the endpoint to check
     * @param timeoutMillis per-session completion wait; {@code <= 0} uses the default
     * @param detailed      add protocol versions, cipher suites, and the revocation check
     * @return the kebab-case report; failures land in it ({@code overall-status: ERROR}),
     * never thrown
     */
    public static NVGenericMap check(NIOSocket nioSocket, InetSocketAddress remote,
                                     long timeoutMillis, boolean detailed) {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<NVGenericMap> report = new AtomicReference<NVGenericMap>();
        asyncCheck(nioSocket, remote, timeoutMillis, detailed, new Consumer<NVGenericMap>() {
            @Override
            public void accept(NVGenericMap r) {
                report.set(r);
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        NVGenericMap out = report.get();
        if (out == null) {
            // interrupted before the pipeline delivered: the service's scan-level error shape
            out = new NVGenericMap("check")
                    .build("host", remote.getHostString())
                    .build(new NVInt("port", remote.getPort()))
                    .build("secure", "no")
                    .build("error-message", "interrupted")
                    .build("trust-verdict", "UNKNOWN")
                    .build("trust-reason", "interrupted")
                    .build("overall-status", "ERROR");
        }
        return out;
    }

    /**
     * Asynchronous check: returns as soon as the first session is launched and delivers the
     * finished report — the exact report {@code check} returns — to the callback, from the
     * socket's executor (the default {@link TaskUtil} processor when the socket runs I/O on its
     * selector thread). The fleet form: launch one per endpoint against a single socket and
     * collect the reports as they arrive; no thread is held while a check is in flight. The
     * socket is never closed here; its lifecycle belongs to the caller. Failures land in the
     * report, never in a thrown exception.
     */
    public static void asyncCheck(NIOSocket nioSocket, InetSocketAddress remote,
                                  long timeoutMillis, boolean detailed,
                                  Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("nioSocket, remote and callback are required", nioSocket, remote, callback);
        new Pipeline(nioSocket, remote, timeoutMillis, detailed, callback).start();
    }

    // ---- the pipeline ----

    /**
     * One check in flight: the stages of the report, each launched from the previous
     * session's close hook. The report bag and the summary facts carried between stages are
     * touched by one stage at a time — the stages are strictly sequential, handed from hook to
     * executor task — so the pipeline needs no locking.
     */
    private static final class Pipeline {
        private final NIOSocket nioSocket;
        private final InetSocketAddress remote;
        private final long timeout;
        private final boolean detailed;
        private final Consumer<NVGenericMap> callback;
        private final Executor executor;
        private final NVGenericMap out = new NVGenericMap("check");
        private final long startMillis = System.currentTimeMillis();
        private final AtomicBoolean delivered = new AtomicBoolean();

        // summary facts carried into the detailed stages and the verdict
        private TCPPQCProtocol baseline;
        private String tlsVersion;
        private boolean pqKex;
        private boolean pqCert;
        private String trustVerdict = "UNKNOWN";
        private Object chainReason;

        // detailed-stage accumulators
        private List<String> localProtocols;
        private final List<String> supportedVersions = new ArrayList<String>();
        private final List<String> suiteNames = new ArrayList<String>();

        Pipeline(NIOSocket nioSocket, InetSocketAddress remote, long timeoutMillis,
                 boolean detailed, Consumer<NVGenericMap> callback) {
            this.nioSocket = nioSocket;
            this.remote = remote;
            this.timeout = timeoutMillis > 0 ? timeoutMillis : DEFAULT_SESSION_TIMEOUT_MILLIS;
            this.detailed = detailed;
            this.callback = callback;
            this.executor = nioSocket.getExecutor() != null
                    ? nioSocket.getExecutor() : TaskUtil.defaultTaskProcessor();
        }

        void start() {
            try {
                out.build("host", remote.getHostString())
                        .build(new NVInt("port", remote.getPort()))
                        .build("scan-id", UUID.randomUUID().toString())
                        // hosted check-qdz shape: the hosting endpoint sets total-scanned on the
                        // finished report (((NVLong) r.getNV("total-scanned")).setValue(counter));
                        // scan-time-in-ms is a placeholder filled at delivery so both sit up front
                        .build(new NVLong("total-scanned", 0))
                        .build(new NVLong("scan-time-in-ms", 0));
                // the hosted service's client shape: hybrids first; fall back to the default offer
                launch(PQCSweep.PQ_PREFERRED_GROUPS, null, null, new Consumer<TCPPQCProtocol>() {
                    @Override
                    public void accept(TCPPQCProtocol session) {
                        onPrimary(session);
                    }
                });
            } catch (Throwable t) {
                fail(t);
            }
        }

        // -- session launch and stage handoff --

        /**
         * One session with the given pinned offers. {@code next} runs on the executor once the
         * session has closed — handshake completion, failure, or the session timeout closing it
         * — and never before. Never throws: a session that cannot be built reaches {@code next}
         * as null, one that cannot be opened reaches it closed with no facts.
         */
        private void launch(String[] groups, String[] protocols, String[] ciphers,
                            final Consumer<TCPPQCProtocol> next) {
            final TCPPQCProtocol session;
            try {
                session = new TCPPQCProtocol(null, remote, groups, protocols, ciphers);
            } catch (Exception e) {
                hop(new Runnable() {
                    @Override
                    public void run() {
                        next.accept(null);
                    }
                });
                return;
            }
            final AtomicReference<ScheduledFuture<?>> deadline = new AtomicReference<ScheduledFuture<?>>();
            // hook first: whichever thread closes the session — including this one, should the
            // open fail synchronously — hands the closed session to the next stage exactly once
            session.onClose(new Consumer<TCPPQCProtocol>() {
                @Override
                public void accept(final TCPPQCProtocol closed) {
                    ScheduledFuture<?> d = deadline.get();
                    if (d != null)
                        d.cancel(false);
                    hop(new Runnable() {
                        @Override
                        public void run() {
                            next.accept(closed);
                        }
                    });
                }
            });
            try {
                // the session timeout: a hang closes the session, which fires the hook with no facts
                deadline.set(nioSocket.getScheduler().schedule(new Runnable() {
                    @Override
                    public void run() {
                        SharedIOUtil.close(session);
                    }
                }, timeout, TimeUnit.MILLISECONDS));
                nioSocket.addClientSocket(session);
            } catch (Exception e) {
                // open failed: close (a no-op if the failure already closed it) so the hook fires
                SharedIOUtil.close(session);
            }
        }

        /**
         * Runs a stage on the executor. Hooks fire inside a session's close path — on a worker
         * mid-handshake, on the selector thread, or on the scheduler — so the stage is always
         * queued, never run in place. A stage that throws fails the check; an executor that
         * refuses the task does too.
         */
        private void hop(final Runnable stage) {
            Runnable guarded = new Runnable() {
                @Override
                public void run() {
                    try {
                        stage.run();
                    } catch (Throwable t) {
                        fail(t);
                    }
                }
            };
            try {
                executor.execute(guarded);
            } catch (RuntimeException e) {
                fail(e);
            }
        }

        private static NVGenericMap results(TCPPQCProtocol session) {
            return session != null ? session.getResults() : new NVGenericMap("results");
        }

        // -- summary stages --

        private void onPrimary(TCPPQCProtocol session) {
            if (results(session).getValue("tls_protocol") == null) {
                // fall back only when the failure might be offer-dependent (a TLS-level
                // rejection, or a hang with no cause); a transport failure (refused,
                // unreachable) would just repeat identically and double the wait
                Throwable cause = session != null ? session.getCloseCause() : null;
                boolean transportFailure = cause != null && !(cause instanceof SSLException);
                if (!transportFailure) {
                    launch(null, null, null, new Consumer<TCPPQCProtocol>() {
                        @Override
                        public void accept(TCPPQCProtocol fallback) {
                            onBaseline(fallback);
                        }
                    });
                    return;
                }
            }
            onBaseline(session);
        }

        private void onBaseline(TCPPQCProtocol session) {
            NVGenericMap results = results(session);
            if (results.getValue("tls_protocol") == null) {
                // the service's scan-level error shape: no success key, error-message + UNKNOWN verdict
                Object error = results.getValue("error");
                String message = friendlyError(error != null ? String.valueOf(error) : null);
                out.build("secure", "no")
                        .build("error-message", message)
                        .build("trust-verdict", "UNKNOWN")
                        .build("trust-reason", message)
                        .build("overall-status", "ERROR");
                deliver();
                return;
            }
            baseline = session;
            summarize(session, results);
            if (!detailed) {
                finish();
                return;
            }
            localProtocols = PQCSweep.localProtocols();
            probeVersion(0);
        }

        private void summarize(TCPPQCProtocol session, NVGenericMap results) {
            out.build(new NVBoolean("success", true))
                    .build("secure", "yes");

            Object tlsProtocol = results.getValue("tls_protocol");
            Object cipher = results.getValue("tls_cipher");
            Object group = results.getValue("tls_kex_group");
            tlsVersion = String.valueOf(tlsProtocol);
            pqKex = Boolean.TRUE.equals(results.getValue("pqc_kex"));
            out.build("tls-version", tlsVersion)
                    .build(new NVBoolean("tls-version-pqc-capable", "TLSv1.3".equals(tlsVersion)))
                    .build("key-exchange-type", kexType(group != null ? group.toString() : null))
                    .build("key-exchange-algorithm", group != null ? group.toString() : "UNKNOWN")
                    .build(new NVBoolean("key-exchange-pqc-ready", pqKex))
                    .build("cipher-suite", String.valueOf(cipher));

            X509Certificate[] chain = session.getCapturedChain();
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
            chainReason = results.getValue("chain_reason");
        }

        // -- detailed stages: versions -> suites -> revocation --

        /** Supported protocol versions, one session per candidate the local provider can express. */
        private void probeVersion(int index) {
            while (index < PQCSweep.VERSION_CANDIDATES.length
                    && !localProtocols.isEmpty()
                    && !localProtocols.contains(PQCSweep.VERSION_CANDIDATES[index]))
                index++;
            if (index >= PQCSweep.VERSION_CANDIDATES.length) {
                // versions land in the report after the suites array (hosted check-qdz key order)
                startCiphers();
                return;
            }
            final int current = index;
            final String version = PQCSweep.VERSION_CANDIDATES[index];
            launch(null, new String[]{version}, null, new Consumer<TCPPQCProtocol>() {
                @Override
                public void accept(TCPPQCProtocol probe) {
                    if (results(probe).getValue("tls_protocol") != null)
                        supportedVersions.add(version);
                    probeVersion(current + 1);
                }
            });
        }

        private void startCiphers() {
            discover("TLSv1.3", PQCSweep.providerSuites(true), new Runnable() {
                @Override
                public void run() {
                    discover("TLSv1.2", PQCSweep.providerSuites(false), new Runnable() {
                        @Override
                        public void run() {
                            afterCiphers();
                        }
                    });
                }
            });
        }

        /**
         * One version's discovery loop (the sweep's enumeration, META-TCP-PQC.md §7): offer the
         * remaining candidates, record the server's pick, drop it, repeat until the server
         * refuses or fails to complete.
         */
        private void discover(final String version, final List<String> remaining, final Runnable then) {
            if (remaining.isEmpty()) {
                then.run();
                return;
            }
            launch(null, new String[]{version}, remaining.toArray(new String[0]),
                    new Consumer<TCPPQCProtocol>() {
                        @Override
                        public void accept(TCPPQCProtocol probe) {
                            NVGenericMap r = results(probe);
                            Object negotiated = r.getValue("tls_protocol") != null ? r.getValue("tls_cipher") : null;
                            // the pinned offer guarantees membership; a foreign pick would loop forever
                            if (negotiated instanceof String && remaining.remove(negotiated)) {
                                suiteNames.add((String) negotiated);
                                discover(version, remaining, then);
                            } else {
                                then.run();
                            }
                        }
                    });
        }

        private void afterCiphers() {
            // hosted check-qdz key order: revocation, then the suites array, then the versions list

            // revocation: stapled OCSP -> active OCSP -> CRL, soft-fail (blocking HTTP, on this worker)
            X509Certificate[] chain = baseline.getCapturedChain();
            if (chain != null && chain.length > 0) {
                NVGenericMap revocation = PQCRevocation.check(chain[0],
                        chain.length > 1 ? chain[1] : null, baseline.getStapledOCSP(), timeout);
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
            if ("REVOKED".equals(out.getValue("revocation-status")))
                trustVerdict = "REVOKED";

            NVGenericMapList suites = new NVGenericMapList("supported-cipher-suites");
            for (String name : suiteNames) {
                NVGenericMap suite = new NVGenericMap();
                suite.build("name", name)
                        .build("strength", suiteStrength(name))
                        .build("key-exchange", suiteKex(name))
                        .build(new NVBoolean("forward-secrecy", forwardSecrecy(name)));
                suites.add(suite);
            }
            out.add(suites);
            out.add(new NVStringList("supported-protocol-versions", supportedVersions));
            finish();
        }

        // -- verdict and delivery --

        private void finish() {
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
            deliver();
        }

        /** A stage failed: the report keeps whatever it has, the missing verdict keys say ERROR. */
        private void fail(Throwable e) {
            String message = friendlyError(e.getMessage() != null ? e.getMessage() : e.toString());
            if (out.getNV("secure") == null)
                out.build("secure", "no");
            if (out.getNV("error-message") == null)
                out.build("error-message", message);
            if (out.getNV("trust-verdict") == null)
                out.build("trust-verdict", "UNKNOWN").build("trust-reason", message);
            if (out.getNV("overall-status") == null)
                out.build("overall-status", "ERROR");
            deliver();
        }

        private void deliver() {
            if (delivered.compareAndSet(false, true)) {
                ((NVLong) out.getNV("scan-time-in-ms")).setValue(System.currentTimeMillis() - startMillis);
                callback.accept(out);
            }
        }
    }

    // ---- shaping ----

    private static NVGenericMapList chainMap(X509Certificate[] chain) {
        NVGenericMapList chainMap = new NVGenericMapList("cert-chain");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];
            boolean selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
            NVGenericMap entry = new NVGenericMap();
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
     * Runs a warm-up check first and prints the second (steady-state) report, then the same
     * check once more through {@code asyncCheck} — the report arrives on a callback, which
     * closes the socket and exits the process. Exit codes: 0 success, 1 a report said ERROR,
     * 64 usage/setup failure.
     */
    public static void main(String[] args) {
        NIOSocket nioSocket = null;
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
            nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            // warm-up pass: first-use costs (provider load, JIT, selector/pool spin-up) land
            // here so the reported scan-time-in-ms reflects a steady-state check
            check(nioSocket, new InetSocketAddress(host, port), DEFAULT_SESSION_TIMEOUT_MILLIS, detailed);
            NVGenericMap report = check(nioSocket, new InetSocketAddress(host, port),
                    DEFAULT_SESSION_TIMEOUT_MILLIS, detailed);
            System.out.println("report: " + report);
            final int syncExit = "ERROR".equals(report.getValue("overall-status")) ? 1 : 0;
            // the same check through the async form: main returns, the JVM stays up on the
            // NIO/pool threads, and the callback closes the socket and exits the process
            final NIOSocket socket = nioSocket;
            asyncCheck(socket, new InetSocketAddress(host, port), DEFAULT_SESSION_TIMEOUT_MILLIS, detailed, (r) -> {
                System.out.println("async report: " + r);
                SharedIOUtil.close(socket);
                System.exit("ERROR".equals(r.getValue("overall-status")) ? 1 : syncExit);
            });
        } catch (Exception e) {
            // no async callback is pending on this path: close and exit here or the JVM hangs
            System.err.println("check error: " + e);
            SharedIOUtil.close(nioSocket);
            System.exit(64);
        }
    }
}
