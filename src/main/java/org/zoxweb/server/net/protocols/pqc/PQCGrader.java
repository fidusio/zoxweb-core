package org.zoxweb.server.net.protocols.pqc;

import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVStringList;

import java.util.ArrayList;
import java.util.List;

/**
 * The bundled report consumer that derives a letter grade from a finished sweep report
 * (META-TCP-PQC.md §8). Deliberately layered <b>on top of</b> the report: the auditor and the
 * sweep report facts only (§9) — the grade is derived, never a fact, and every grade carries
 * the {@code reasons} that produced it so it stays explainable from the report. Consumers that
 * want different policy grade the same report themselves.
 * <p>
 * Scale, worst condition wins:
 * <ul>
 * <li><b>E</b> — no posture information (the baseline session never completed);</li>
 * <li><b>F</b> — trust failure, outranks everything: untrusted chain, hostname mismatch,
 * time-invalid chain, or a revoked leaf;</li>
 * <li><b>C</b> — a deprecated protocol (SSLv3 / TLS 1.0 / TLS 1.1) or a weak cipher suite
 * (RC4 / DES / 3DES / NULL / export / anon) is accepted;</li>
 * <li><b>B</b> — trusted but no PQ key exchange, or PQ-ready with minor wear (CBC suites
 * accepted, certificate expiring soon);</li>
 * <li><b>A</b> — PQ negotiable and clean; <b>A+</b> when the default offer already negotiates
 * PQ ({@code kex_default}).</li>
 * </ul>
 * Only facts present in the report are judged — an absent fact (a matrix that did not run)
 * neither helps nor hurts the grade.
 */
public final class PQCGrader {

    private static final String[] DEPRECATED_VERSIONS = {"SSLv3", "TLSv1.1", "TLSv1"};
    private static final String[] WEAK_TOKENS = {"_RC4", "_3DES", "_DES_", "DES40", "_NULL", "EXPORT", "ANON"};

    private PQCGrader() {
    }

    /**
     * @param report a finished sweep (or single-session) report
     * @return a {@code grade} sub-map: {@code letter} and the {@code reasons} that produced it
     */
    public static NVGenericMap grade(NVGenericMap report) {
        NVGenericMap grade = new NVGenericMap("grade");
        List<String> reasons = new ArrayList<String>();
        String letter;

        if (report == null || report.getValue("tls_protocol") == null) {
            reasons.add("baseline session never completed - no posture information");
            letter = "E";
        } else if (trustFailures(report, reasons)) {
            letter = "F";
        } else if (acceptanceFailures(report, reasons)) {
            letter = "C";
        } else {
            boolean wear = wear(report, reasons);
            boolean ready = Boolean.TRUE.equals(subValue(report, "pqc_ready", "ready"));
            boolean kexDefault = Boolean.TRUE.equals(subValue(report, "pqc_ready", "kex_default"));
            if (!ready) {
                reasons.add("no post-quantum key exchange");
                letter = "B";
            } else if (wear) {
                letter = "B";
            } else if (kexDefault) {
                letter = "A+";
            } else {
                reasons.add("PQ negotiated only for hybrid-preferring clients (kex_default=false)");
                letter = "A";
            }
        }

        grade.build("letter", letter);
        grade.add(new NVStringList("reasons", reasons));
        return grade;
    }

    /** F conditions — a connection that cannot be trusted outranks its PQ posture. */
    private static boolean trustFailures(NVGenericMap report, List<String> reasons) {
        if (isFalse(report, "chain_trusted")) {
            Object why = report.getValue("chain_reason");
            reasons.add("certificate chain not trusted" + (why != null ? ": " + why : ""));
        }
        if (isFalse(report, "hostname_match"))
            reasons.add("hostname does not match the certificate");
        if (isFalse(report, "chain_time_valid"))
            reasons.add("chain contains an expired or not-yet-valid certificate");
        if ("revoked".equals(subValue(report, "revocation", "status")))
            reasons.add("leaf certificate is revoked");
        return !reasons.isEmpty();
    }

    /** C conditions — the endpoint accepts what should be refused. */
    private static boolean acceptanceFailures(NVGenericMap report, List<String> reasons) {
        Object versionsNV = report.getNV("versions");
        if (versionsNV instanceof NVGenericMap) {
            for (String version : DEPRECATED_VERSIONS) {
                if ("supported".equals(subValue((NVGenericMap) versionsNV, version, "status")))
                    reasons.add("deprecated protocol supported: " + version);
            }
        }
        for (String suite : allSuites(report)) {
            String upper = suite.toUpperCase();
            for (String token : WEAK_TOKENS) {
                if (upper.contains(token)) {
                    reasons.add("weak cipher suite accepted: " + suite);
                    break;
                }
            }
        }
        return !reasons.isEmpty();
    }

    /** Minor wear — not a failure, but keeps the grade at B. */
    private static boolean wear(NVGenericMap report, List<String> reasons) {
        boolean wear = false;
        for (String suite : allSuites(report)) {
            if (suite.toUpperCase().contains("_CBC_")) {
                reasons.add("CBC cipher suites accepted");
                wear = true;
                break;
            }
        }
        if (Boolean.TRUE.equals(report.getValue("expires_soon"))) {
            Object days = report.getValue("days_to_expiry");
            reasons.add("certificate expires soon" + (days != null ? " (days_to_expiry=" + days + ")" : ""));
            wear = true;
        }
        return wear;
    }

    // ---- report access ----

    private static List<String> allSuites(NVGenericMap report) {
        List<String> suites = new ArrayList<String>();
        Object ciphersNV = report.getNV("ciphers");
        if (ciphersNV instanceof NVGenericMap) {
            for (String key : new String[]{"tls13", "tls12"}) {
                Object listNV = ((NVGenericMap) ciphersNV).getNV(key);
                if (listNV instanceof NVStringList && ((NVStringList) listNV).getValue() != null)
                    suites.addAll(((NVStringList) listNV).getValue());
            }
        }
        return suites;
    }

    /** Explicit false only — an absent fact never fails a grade. */
    private static boolean isFalse(NVGenericMap report, String key) {
        return Boolean.FALSE.equals(report.getValue(key));
    }

    private static Object subValue(NVGenericMap report, String subMapName, String key) {
        Object subMap = report.getNV(subMapName);
        return subMap instanceof NVGenericMap ? ((NVGenericMap) subMap).getValue(key) : null;
    }
}
