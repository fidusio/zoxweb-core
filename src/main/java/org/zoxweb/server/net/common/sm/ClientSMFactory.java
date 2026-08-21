package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVGenericMapList;
import org.zoxweb.shared.util.NVInt;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Declarative composition of a {@link ClientConSM} from a JSON object (via
 * {@link GSONUtil#fromJSONDefault}) or an {@link NVGenericMap} directly — <b>a state
 * catalog, not a protocol switch</b>: the JSON declares predefined catalog states and each
 * one's {@code config} block; the factory registers them in declared order between the
 * mandatory lifecycle states, seeding each config into that state's properties bag. Adding a
 * state type later is one {@link #registerState catalog entry} — zero factory-core change.
 * <p>
 * The config describes a <b>protocol</b>, never an endpoint: it carries no remote host — the
 * caller supplies the {@code InetSocketAddress} when the connection is created
 * ({@code NIOSocket.addClientSocket}). An ssl state binds SNI and hostname verification to that
 * connected address at upgrade time. An optional top-level {@code port} is only a default-port
 * hint the caller may read via {@link #port(NVGenericMap, int)}.
 * <p>
 * <b>v2 shape</b> — explicit states:
 * <pre>
 * { "name": "dns-probe", "transport": "udp", "port": 53, "close_on_ready": true,
 *   "states": [
 *     { "state": "assembler",  "config": { "boundary": "datagram", "max_message": 65536 } },
 *     { "state": "controller", "config": { "exchange": [
 *         { "send":     "hex:1234 0100 ..." },
 *         { "validate": { "contains": "hex:1234", "report": "dns" } } ] } }
 *   ] }
 * </pre>
 * <b>Sugar forms</b> (expand to the equivalent states): {@code protocol: "ssh"} (+ optional
 * {@code ssh} block) → delimited assembler + validating controller; a bare top-level
 * {@code exchange} → default-boundary assembler + controller; a {@code tls} block /
 * {@code protocol: "tls"} → the ssl state. A {@code responder} and {@code validator} are
 * auto-composed whenever a controller is present. {@code transport: "udp"} is always plaintext
 * (no DTLS). Unknown keys are ignored; contradictory or unsafe combinations fail fast
 * ({@link IllegalArgumentException}).
 */
public final class ClientSMFactory {

    private ClientSMFactory() {
    }

    /**
     * The state catalog: name → builder function ({@code config} block → configured state).
     * Extensible — register new entries via {@link #registerState}.
     */
    private static final Map<String, Function<NVGenericMap, State<?>>> CATALOG =
            new LinkedHashMap<String, Function<NVGenericMap, State<?>>>();

    static {
        CATALOG.put(MessageAssemblerState.NAME, MessageAssemblerState::new);
        CATALOG.put(ProtocolControllerState.NAME, ProtocolControllerState::new);
        CATALOG.put(ResponseControllerState.NAME, config -> new ResponseControllerState());
        CATALOG.put(ProtocolTypeValidatorState.NAME, config -> new ProtocolTypeValidatorState());
        CATALOG.put(SSLClientState.NAME, ClientSMFactory::sslState);
        CATALOG.put(SSHKexState.NAME, SSHKexState::new);
    }

    private static State<?> sslState(NVGenericMap config) {
        boolean certValidation = SMProtoUtil.booleanValue(config, "cert_validation", true);
        String modeName = SMProtoUtil.stringValue(config, "mode", "on_demand");
        SSLClientState.TLSMode mode;
        if ("immediate".equalsIgnoreCase(modeName))
            mode = SSLClientState.TLSMode.IMMEDIATE;
        else if ("on_demand".equalsIgnoreCase(modeName))
            mode = SSLClientState.TLSMode.ON_DEMAND;
        else
            throw new IllegalArgumentException("unknown tls mode: " + modeName);
        return new SSLClientState(mode, certValidation);
    }

    /**
     * Registers (or replaces) a catalog state builder — the open-vocabulary seam: new state
     * types compose without any factory-core change.
     */
    public static void registerState(String name, Function<NVGenericMap, State<?>> builder) {
        SUS.checkIfNulls("name or builder null", name, builder);
        synchronized (CATALOG) {
            CATALOG.put(name, builder);
        }
    }

    public static ClientConSM fromJSON(String json) {
        SUS.checkIfNull("json null", json);
        return fromConfig(GSONUtil.fromJSONDefault(json, NVGenericMap.class));
    }

    public static ClientConSM fromConfig(NVGenericMap cfg) {
        SUS.checkIfNull("config null", cfg);
        String name = SMProtoUtil.stringValue(cfg, "name", "client-sm");
        String protocol = SMProtoUtil.stringValue(cfg, "protocol", "plain").toLowerCase();
        String transportName = SMProtoUtil.stringValue(cfg, "transport", "tcp").toLowerCase();

        ClientSessionContext.Transport transport;
        if ("tcp".equals(transportName))
            transport = ClientSessionContext.Transport.TCP;
        else if ("udp".equals(transportName))
            transport = ClientSessionContext.Transport.UDP;
        else
            throw new IllegalArgumentException("unknown transport: " + transportName);

        if (transport == ClientSessionContext.Transport.UDP) {
            // UDP is always plaintext (no DTLS) and datagram-based: only protocol 'plain'
            // (optionally with an exchange script) makes sense over it
            if (!"plain".equals(protocol))
                throw new IllegalArgumentException(
                        "protocol '" + protocol + "' requires a TCP stream; transport 'udp' supports protocol 'plain'");
            if (subMap(cfg, "tls") != null)
                throw new IllegalArgumentException("tls block over transport 'udp': no DTLS in this stack");
        }

        ClientConSMBuilder builder = ClientConSMBuilder.create(name).settings(cfg).transport(transport);
        boolean controllerComposed;

        Object statesNV = cfg.getNV("states");
        if (statesNV instanceof NVGenericMapList) {
            // v2 explicit shape: the JSON composes the machine state by state
            controllerComposed = composeStates(builder, (NVGenericMapList) statesNV);
        } else {
            controllerComposed = composeSugar(builder, cfg, protocol, transport);
        }

        // machine-dictated session end: a UDP config with a script is a probe — the pipeline
        // completing IS the session (UDP has no EOF), so the machine closes it on READY unless
        // the config says otherwise; TCP and open-ended UDP sessions default to staying open
        boolean closeOnReadyDefault = transport == ClientSessionContext.Transport.UDP && controllerComposed;
        builder.closeOnReady(SMProtoUtil.booleanValue(cfg, "close_on_ready", closeOnReadyDefault));

        ClientConSM sm = builder.build();
        // seed default exchange variables from a config "vars" block; the caller may add/override
        // via ctx.setVar before connecting (the endpoint-specific / environment values stay out of
        // the generic protocol description)
        NVGenericMap vars = subMap(cfg, "vars");
        if (vars != null) {
            for (GetNameValue<?> nv : vars.values()) {
                Object v = nv.getValue();
                if (v != null)
                    sm.getContext().setVar(nv.getName(), v.toString());
            }
        }
        return sm;
    }

    /**
     * The v2 explicit-states path: each entry is {@code {state: <catalog name>, config: {...}}}.
     *
     * @return true if a controller was composed (drives the UDP close_on_ready default)
     */
    private static boolean composeStates(ClientConSMBuilder builder, NVGenericMapList states) {
        boolean controller = false, responder = false, validator = false;
        for (NVGenericMap entry : states.getValue()) {
            String stateName = SMProtoUtil.stringValue(entry, "state", null);
            if (stateName == null)
                throw new IllegalArgumentException("states entry without a 'state' name: " + entry);
            Function<NVGenericMap, State<?>> stateBuilder;
            synchronized (CATALOG) {
                stateBuilder = CATALOG.get(stateName);
            }
            if (stateBuilder == null)
                throw new IllegalArgumentException("unknown catalog state: " + stateName);
            builder.state(stateBuilder.apply(subMap(entry, "config")));
            controller |= ProtocolControllerState.NAME.equals(stateName);
            responder |= ResponseControllerState.NAME.equals(stateName);
            validator |= ProtocolTypeValidatorState.NAME.equals(stateName);
        }
        // a controller publishes OUT_MESSAGE/VALIDATE — the consuming states are part of the
        // deal, auto-composed unless the config declared them explicitly
        if (controller && !responder)
            builder.state(new ResponseControllerState());
        if (controller && !validator)
            builder.state(new ProtocolTypeValidatorState());
        return controller;
    }

    /**
     * The sugar path: {@code protocol}/{@code tls}/{@code exchange} keys expand to the
     * equivalent catalog states (v1 configs compose unchanged).
     *
     * @return true if a controller was composed
     */
    private static boolean composeSugar(ClientConSMBuilder builder, NVGenericMap cfg,
                                        String protocol, ClientSessionContext.Transport transport) {
        boolean ssh = "ssh".equals(protocol);
        boolean tlsProtocol = "tls".equals(protocol);
        if (!ssh && !tlsProtocol && !"plain".equals(protocol))
            throw new IllegalArgumentException("unknown protocol: " + protocol);

        // ssl state first: broadcast order is declaration order, an IMMEDIATE auto-start must
        // precede the controller's CONNECTED consumer
        NVGenericMap tls = subMap(cfg, "tls");
        SSLClientState.TLSMode tlsMode = null;
        if (tlsProtocol || tls != null) {
            NVGenericMap sslConfig = tls != null ? tls : new NVGenericMap();
            if (tlsProtocol && SMProtoUtil.stringValue(sslConfig, "mode", null) == null)
                sslConfig.build("mode", "immediate");
            SSLClientState ssl = (SSLClientState) sslState(sslConfig);
            tlsMode = ssl.getMode();
            if (tlsProtocol && tlsMode == SSLClientState.TLSMode.ON_DEMAND)
                throw new IllegalArgumentException(
                        "protocol 'tls' means the link is secured before READY — tls mode 'on_demand' would" +
                        " silently leave it plaintext; use protocol 'plain' with an on_demand tls block for STARTTLS");
            builder.state(ssl);
        }

        Object exchange = cfg.getNV("exchange");
        if (ssh) {
            if (exchange != null)
                throw new IllegalArgumentException(
                        "'exchange' is not supported with protocol 'ssh': compose explicit states instead" +
                        " (both scripts would drive one controller)");
            // protocol "ssh" is factory sugar (META-SM-PROTO-DESIGN.md §11): a CR-tolerant
            // delimited assembler + a controller that awaits the identification line and
            // validates it, reporting results.banner
            NVGenericMap sshCfg = subMap(cfg, "ssh");

            // optional KEXINIT capture — the PQ-readiness check: kex_check records the server's
            // kex_algorithms + pq_kex verdict; pq_required (implies the check) fails the session
            // when no post-quantum key exchange is offered. Declared BEFORE the assembler:
            // broadcast order is declaration order, and the wire-buffer handoff (the assembler
            // owns IN_DATA until the script completes, ssh_kex after) depends on the kex state
            // seeing each buffer first and skipping it until the assembly is finished
            boolean pqRequired = SMProtoUtil.booleanValue(sshCfg, "pq_required", false);
            boolean kexComposed = pqRequired || SMProtoUtil.booleanValue(sshCfg, "kex_check", false);
            if (kexComposed) {
                NVGenericMap kexCfg = new NVGenericMap();
                if (pqRequired)
                    kexCfg.build(new NVBoolean("pq_required", true));
                String pqAlgorithms = SMProtoUtil.stringValue(sshCfg, "pq_algorithms", null);
                if (pqAlgorithms != null)
                    kexCfg.build("pq_algorithms", pqAlgorithms);
                builder.state(new SSHKexState(kexCfg));
            }

            NVGenericMap assemblerCfg = new NVGenericMap();
            assemblerCfg.build("boundary", "delimited")
                    .build("terminator", "txt:\n")
                    .build(new NVBoolean("strip_cr", true))
                    .build(new NVInt("max_message", SMProtoUtil.intValue(sshCfg, "banner_max_line", 255)));
            builder.state(new MessageAssemblerState(assemblerCfg));

            NVGenericMap validateMeta = new NVGenericMap();
            validateMeta.build("prefix", SMProtoUtil.stringValue(sshCfg, "banner_prefix", "SSH-2.0-"));
            String contains = SMProtoUtil.stringValue(sshCfg, "banner_contains", null);
            if (contains != null)
                validateMeta.build("contains", contains);
            String exact = SMProtoUtil.stringValue(sshCfg, "banner_exact", null);
            if (exact != null)
                validateMeta.build("exact", exact);
            validateMeta.build("report", "banner");
            List<GetNameValue<?>> steps = new ArrayList<GetNameValue<?>>();
            if (kexComposed && SMProtoUtil.booleanValue(sshCfg, "send_ident", true)) {
                // the KEXINIT capture needs the server to proceed to key exchange, and some
                // servers (e.g. GitHub's) wait for the client's identification line before
                // sending theirs (RFC 4253 §4.2: both sides send independently) — so the probe
                // identifies itself normally (§0). Banner-only checks stay fully passive.
                steps.add(new NVPair(ProtocolControllerState.OP_SEND,
                        "txt:" + SMProtoUtil.stringValue(sshCfg, "client_ident", "SSH-2.0-zoxweb_probe") + "\r\n"));
            }
            steps.add(new NVPair(ProtocolControllerState.OP_EXPECT, "txt:SSH-"));
            steps.add(new NamedValue<NVGenericMap>(ProtocolControllerState.OP_VALIDATE, validateMeta));
            NVGenericMap controllerCfg = new NVGenericMap();
            controllerCfg.add(new NamedValue<List<GetNameValue<?>>>("exchange", steps));
            controllerCfg.build(new NVInt("max_skip", SMProtoUtil.intValue(sshCfg, "pre_banner_cap", 4096)));
            builder.state(new ProtocolControllerState(controllerCfg));
        } else if (exchange != null) {
            // bare top-level exchange: default-boundary assembler + the scripted controller
            builder.state(new MessageAssemblerState());
            NVGenericMap controllerCfg = new NVGenericMap();
            controllerCfg.add((GetNameValue<?>) exchange);
            ProtocolControllerState controller = new ProtocolControllerState(controllerCfg);
            if (controller.hasStartTLS() && tlsMode != SSLClientState.TLSMode.ON_DEMAND)
                throw new IllegalArgumentException(
                        "'start_tls' step requires a tls block with mode 'on_demand'" +
                        (tlsMode == null ? " (no tls block configured)" : " (mode is immediate)"));
            builder.state(controller);
        }

        boolean controllerComposed = ssh || exchange != null;
        if (controllerComposed) {
            builder.state(new ResponseControllerState());
            builder.state(new ProtocolTypeValidatorState());
        }
        return controllerComposed;
    }

    /**
     * The optional default-port hint. The protocol config carries no endpoint; a top-level
     * {@code port} is only a suggested default (e.g. the protocol's well-known port) the caller
     * may fold into the {@code InetSocketAddress} it supplies to
     * {@code NIOSocket.addClientSocket}. The host always comes from the caller.
     *
     * @param fallback returned when the config has no {@code port}
     * @return the configured {@code port}, or {@code fallback}
     */
    public static int port(NVGenericMap cfg, int fallback) {
        return SMProtoUtil.intValue(cfg, "port", fallback);
    }

    /**
     * @return the configured connect timeout in seconds, default 5
     */
    public static int timeoutSec(NVGenericMap cfg) {
        return SMProtoUtil.intValue(cfg, "timeout_sec", 5);
    }

    private static NVGenericMap subMap(NVGenericMap cfg, String name) {
        if (cfg == null)
            return null;
        Object nv = cfg.getNV(name);
        return nv instanceof NVGenericMap ? (NVGenericMap) nv : null;
    }
}
