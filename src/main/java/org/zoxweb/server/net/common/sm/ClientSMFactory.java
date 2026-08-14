package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

/**
 * Declarative configuration of a {@link ClientConSM} from a JSON object (via
 * {@link GSONUtil#fromJSONGenericMap}) or an {@link NVGenericMap} directly; the parsed map is
 * stored as {@link ClientSessionContext#getSettings()} so phases and future negotiators read
 * their knobs from one bag.
 * <p>
 * The config describes a <b>protocol</b>, never an endpoint: it carries no remote host — the caller
 * supplies the {@code InetSocketAddress} when the connection is created
 * ({@code NIOSocket.addClientSocket}). A TLS phase binds SNI and hostname verification to that
 * connected address at upgrade time. An optional top-level {@code port} is only a default-port hint
 * the caller may read via {@link #port(NVGenericMap, int)}.
 * <p>
 * Schema — {@code protocol} picks the phase set ({@code tls} / {@code ssh} / {@code plain},
 * default {@code plain}); a {@code tls} block may accompany any protocol (STARTTLS-ready):
 * <pre>
 * { "name": "smtps-client", "port": 465,
 *   "protocol": "tls", "tls": {"mode": "immediate", "cert_validation": true}, "timeout_sec": 5 }
 *
 * { "name": "ssh-fingerprint", "port": 22,
 *   "protocol": "ssh", "ssh": {"banner_prefix": "SSH-2.0-", "banner_contains": "OpenSSH"} }
 *
 * { "name": "smtp-starttls", "port": 587,
 *   "protocol": "plain", "tls": {"mode": "on_demand", "cert_validation": true} }
 * </pre>
 * Unknown keys are ignored; a contradictory or unsafe combination fails fast
 * ({@link IllegalArgumentException}).
 */
public final class ClientSMFactory {

    private ClientSMFactory() {
    }

    public static ClientConSM fromJSON(String json) {
        SUS.checkIfNull("json null", json);
        return fromConfig(GSONUtil.fromJSONGenericMap(json, null, Base64Type.DEFAULT));
    }

    public static ClientConSM fromConfig(NVGenericMap cfg) {
        SUS.checkIfNull("config null", cfg);
        String name = stringValue(cfg, "name", "client-sm");
        String protocol = stringValue(cfg, "protocol", "plain").toLowerCase();

        ClientConSMBuilder builder = ClientConSMBuilder.create(name).settings(cfg);

        if ("ssh".equals(protocol)) {
            NVGenericMap ssh = subMap(cfg, "ssh");
            builder.phase(new SSHBannerPhase(
                    stringValue(ssh, "banner_prefix", "SSH-2.0-"),
                    stringValue(ssh, "banner_contains", null),
                    stringValue(ssh, "banner_exact", null),
                    intValue(ssh, "banner_max_line", SSHBannerPhase.DEFAULT_MAX_LINE),
                    intValue(ssh, "pre_banner_cap", SSHBannerPhase.DEFAULT_PRE_BANNER_CAP)));
        } else if (!"tls".equals(protocol) && !"plain".equals(protocol)) {
            throw new IllegalArgumentException("unknown protocol: " + protocol);
        }

        NVGenericMap tls = subMap(cfg, "tls");
        boolean tlsProtocol = "tls".equals(protocol);
        SSLClientPhase.TLSMode tlsMode = null;
        if (tlsProtocol || tls != null) {
            boolean certValidation = booleanValue(tls, "cert_validation", true);
            String modeName = stringValue(tls, "mode", tlsProtocol ? "immediate" : "on_demand");
            if ("immediate".equalsIgnoreCase(modeName))
                tlsMode = SSLClientPhase.TLSMode.IMMEDIATE;
            else if ("on_demand".equalsIgnoreCase(modeName))
                tlsMode = SSLClientPhase.TLSMode.ON_DEMAND;
            else
                throw new IllegalArgumentException("unknown tls mode: " + modeName);
            if (tlsProtocol && tlsMode == SSLClientPhase.TLSMode.ON_DEMAND)
                throw new IllegalArgumentException(
                        "protocol 'tls' means the link is secured before READY — tls mode 'on_demand' would" +
                        " silently leave it plaintext; use protocol 'plain' with an on_demand tls block for STARTTLS");
            // deferred: the TLS context binds to the endpoint the session actually connects to
            // (no remote in the protocol config). Endpoint identification rides cert validation:
            // chain validation without hostname verification accepts any CA-valid cert for any host
            builder.phase(new SSLClientPhase(tlsMode, certValidation));
        }

        java.util.List<org.zoxweb.shared.util.NVPair> steps = DataExchangePhase.stepsFrom(cfg);
        if (steps != null) {
            if ("ssh".equals(protocol))
                throw new IllegalArgumentException(
                        "'exchange' is not supported with protocol 'ssh': the banner phase and the exchange" +
                        " would both consume IN_DATA (one active owner per buffer)");
            DataExchangePhase exchange = new DataExchangePhase(steps);
            if (exchange.hasStartTLS() && tlsMode != SSLClientPhase.TLSMode.ON_DEMAND)
                throw new IllegalArgumentException(
                        "'start_tls' step requires a tls block with mode 'on_demand'" +
                        (tlsMode == null ? " (no tls block configured)" : " (mode is immediate)"));
            builder.phase(exchange);
        }

        ClientConSM sm = builder.build();
        // seed default exchange variables from a config "vars" block; the caller may add/override
        // via ctx.setVar before connecting (the endpoint-specific / environment values stay out of
        // the generic protocol description)
        NVGenericMap vars = subMap(cfg, "vars");
        if (vars != null) {
            for (org.zoxweb.shared.util.GetNameValue<?> nv : vars.values()) {
                Object v = nv.getValue();
                if (v != null)
                    sm.getContext().setVar(nv.getName(), v.toString());
            }
        }
        return sm;
    }

    /**
     * The optional default-port hint. The protocol config carries no endpoint; a top-level
     * {@code port} is only a suggested default (e.g. the protocol's well-known port) the caller may
     * fold into the {@code InetSocketAddress} it supplies to {@code NIOSocket.addClientSocket}. The
     * host always comes from the caller.
     *
     * @param fallback returned when the config has no {@code port}
     * @return the configured {@code port}, or {@code fallback}
     */
    public static int port(NVGenericMap cfg, int fallback) {
        return intValue(cfg, "port", fallback);
    }

    /**
     * @return the configured connect timeout in seconds, default 5
     */
    public static int timeoutSec(NVGenericMap cfg) {
        return intValue(cfg, "timeout_sec", 5);
    }

    private static NVGenericMap subMap(NVGenericMap cfg, String name) {
        Object nv = cfg.getNV(name);
        return nv instanceof NVGenericMap ? (NVGenericMap) nv : null;
    }

    private static String stringValue(NVGenericMap map, String name, String def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof String && !((String) v).isEmpty() ? (String) v : def;
    }

    private static int intValue(NVGenericMap map, String name, int def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static boolean booleanValue(NVGenericMap map, String name, boolean def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof Boolean ? (Boolean) v : def;
    }
}
