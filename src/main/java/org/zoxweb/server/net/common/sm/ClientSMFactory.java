package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

/**
 * Declarative configuration of a {@link ClientConnectionSM} from a JSON object (via
 * {@link GSONUtil#fromJSONGenericMap}) or an {@link NVGenericMap} directly; the parsed map is
 * stored as {@link ClientSessionContext#getSettings()} so phases and future negotiators read
 * their knobs from one bag.
 * <p>
 * Schema — {@code protocol} picks the phase set ({@code tls} / {@code ssh} / {@code plain},
 * default {@code plain}); a {@code tls} block may accompany any protocol (STARTTLS-ready):
 * <pre>
 * { "name": "smtps-client", "remote": {"host": "mail.example.com", "port": 465},
 *   "protocol": "tls", "tls": {"mode": "immediate", "cert_validation": true}, "timeout_sec": 5 }
 *
 * { "name": "ssh-fingerprint", "remote": {"host": "git.example.com", "port": 22},
 *   "protocol": "ssh", "ssh": {"banner_prefix": "SSH-2.0-", "banner_contains": "OpenSSH"} }
 *
 * { "name": "smtp-starttls", "remote": {"host": "mx.example.com", "port": 587},
 *   "protocol": "plain", "tls": {"mode": "on_demand", "cert_validation": true} }
 * </pre>
 * Unknown keys are ignored; a missing {@code remote} with a non-plain protocol or a
 * {@code tls} block fails fast ({@link IllegalArgumentException}).
 */
public final class ClientSMFactory {

    private ClientSMFactory() {
    }

    public static ClientConnectionSM fromJSON(String json) {
        SUS.checkIfNull("json null", json);
        return fromConfig(GSONUtil.fromJSONGenericMap(json, null, Base64Type.DEFAULT));
    }

    public static ClientConnectionSM fromConfig(NVGenericMap cfg) {
        SUS.checkIfNull("config null", cfg);
        String name = stringValue(cfg, "name", "client-sm");
        String protocol = stringValue(cfg, "protocol", "plain").toLowerCase();

        ClientConnectionSMBuilder builder = ClientConnectionSMBuilder.create(name).settings(cfg);

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
            InetSocketAddress remote = remoteAddress(cfg);
            if (remote == null)
                throw new IllegalArgumentException("'remote' {host, port} is required for TLS");
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
            try {
                // endpoint identification rides cert validation: chain validation without
                // hostname verification accepts any CA-valid cert for any host (MITM)
                builder.phase(new SSLClientPhase(new SSLContextInfo(remote, certValidation), tlsMode, certValidation));
            } catch (GeneralSecurityException e) {
                throw new IllegalArgumentException("TLS context initialization failed", e);
            }
        } else if (remoteAddress(cfg) == null && !"plain".equals(protocol)) {
            throw new IllegalArgumentException("'remote' {host, port} is required for protocol " + protocol);
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

        return builder.build();
    }

    /**
     * @return the configured remote address for {@code NIOSocket.addClientSocket}, null when
     * the config has no {@code remote} block
     */
    public static InetSocketAddress remoteAddress(NVGenericMap cfg) {
        NVGenericMap remote = subMap(cfg, "remote");
        if (remote == null)
            return null;
        String host = stringValue(remote, "host", null);
        int port = intValue(remote, "port", -1);
        if (host == null || port < 0)
            throw new IllegalArgumentException("'remote' requires host and port");
        return new InetSocketAddress(host, port);
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
