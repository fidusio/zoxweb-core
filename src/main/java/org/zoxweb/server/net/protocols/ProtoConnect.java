package org.zoxweb.server.net.protocols;

import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The operational meta protocol runner (META-PROTOCOL.md §7):
 * <pre>
 *   ProtoConnect &lt;definition.json&gt; &lt;host[:port]&gt; [name=value ...]
 * </pre>
 * Loads the JSON definition, builds the matching protocol (TCP or UDP per the definition's
 * {@code transport}), injects the {@code name=value} variables, drives it through NIOSocket, and
 * prints the final report. The port defaults to the definition's {@code port} hint when the
 * endpoint omits it. Pure observer — the protocol runs and closes the session itself.
 * <p>
 * The static factories ({@link #createTCPProtocol(InetSocketAddress, String)} and friends) are
 * the programmatic equivalent: endpoint + definition in, ready-to-connect protocol out — hand
 * the TCP one to {@code NIOSocket.addClientSocket(protocol)} (remote and timeout ride on the
 * callback) and the UDP one to {@code NIOSocket.addDatagramSocket(new InetSocketAddress(0), protocol)}.
 * Every factory also has a {@code Consumer<NVGenericMap>} form that delivers the final results
 * bag exactly once when the session closes (the {@code onClose} hook) — the event-driven
 * alternative to {@code waitForClose}.
 * <p>
 * Exit codes: {@code 0} validated (clean close), {@code 1} session failed (closed with a cause /
 * verdict false), {@code 2} no completion within the wait, {@code 64} usage or definition error.
 */
public final class ProtoConnect {

    private ProtoConnect() {
    }

    // ---- protocol factories: endpoint + definition (JSON text or parsed NVGenericMap) in,
    // ---- bound protocol out ----

    /**
     * Builds a TCP protocol from a parsed definition, bound to {@code remote} — ready for
     * {@code NIOSocket.addClientSocket(protocol)}.
     */
    public static TCPMetaProtocol createTCPProtocol(InetSocketAddress remote, NVGenericMap config) {
        SUS.checkIfNulls("remote and config can't be null", remote, config);
        TCPMetaProtocol ret = new TCPMetaProtocol(null, config);
        ret.setRemoteAddress(remote);
        ret.getScript().recordEndpoint(remote); // host/port in the results even if connect fails
        return ret;
    }

    /**
     * Builds a TCP protocol from a parsed definition, bound to {@code address} — an
     * {@code "host[:port]"} string; a missing port falls back to the definition's {@code port}
     * hint.
     */
    public static TCPMetaProtocol createTCPProtocol(String address, NVGenericMap config) {
        SUS.checkIfNulls("address and config can't be null", address, config);
        TCPMetaProtocol ret = new TCPMetaProtocol(null, config);
        InetSocketAddress remote = requireEndpoint(parseEndpoint(address, ret.getScript().getPort()), address);
        ret.setRemoteAddress(remote);
        ret.getScript().recordEndpoint(remote);
        return ret;
    }

    /**
     * Builds a TCP protocol from a parsed definition, bound to {@code address}; a missing/zero
     * port falls back to the definition's {@code port} hint.
     */
    public static TCPMetaProtocol createTCPProtocol(IPAddress address, NVGenericMap config) {
        SUS.checkIfNulls("address and config can't be null", address, config);
        TCPMetaProtocol ret = new TCPMetaProtocol(null, config);
        int port = address.getPort() > 0 ? address.getPort() : ret.getScript().getPort();
        InetSocketAddress remote = requireEndpoint(
                port > 0 ? new InetSocketAddress(address.getInetAddress(), port) : null,
                "" + address);
        ret.setRemoteAddress(remote);
        ret.getScript().recordEndpoint(remote);
        return ret;
    }

    /**
     * Builds a UDP protocol from a parsed definition, targeting {@code remote} — ready for
     * {@code NIOSocket.addDatagramSocket(new InetSocketAddress(0), protocol)}.
     */
    public static UDPMetaProtocol createUDPProtocol(InetSocketAddress remote, NVGenericMap config) {
        SUS.checkIfNulls("remote and config can't be null", remote, config);
        return new UDPMetaProtocol(null, config, remote);
    }

    /**
     * Builds a UDP protocol from a parsed definition, targeting {@code address} — an
     * {@code "host[:port]"} string; a missing port falls back to the definition's {@code port}
     * hint.
     */
    public static UDPMetaProtocol createUDPProtocol(String address, NVGenericMap config) {
        SUS.checkIfNulls("address and config can't be null", address, config);
        InetSocketAddress remote = requireEndpoint(
                parseEndpoint(address, defaultPort(config)), address);
        return new UDPMetaProtocol(null, config, remote);
    }

    /**
     * Builds a UDP protocol from a parsed definition, targeting {@code address}; a missing/zero
     * port falls back to the definition's {@code port} hint.
     */
    public static UDPMetaProtocol createUDPProtocol(IPAddress address, NVGenericMap config) {
        SUS.checkIfNulls("address and config can't be null", address, config);
        int port = address.getPort() > 0 ? address.getPort() : defaultPort(config);
        InetSocketAddress remote = requireEndpoint(
                port > 0 ? new InetSocketAddress(address.getInetAddress(), port) : null, "" + address);
        return new UDPMetaProtocol(null, config, remote);
    }

    // JSON-text forms — parse once, then delegate to the NVGenericMap equivalents

    /** JSON-text form of {@link #createTCPProtocol(InetSocketAddress, NVGenericMap)}. */
    public static TCPMetaProtocol createTCPProtocol(InetSocketAddress remote, String json) {
        return createTCPProtocol(remote, parse(json));
    }

    /** JSON-text form of {@link #createTCPProtocol(String, NVGenericMap)}. */
    public static TCPMetaProtocol createTCPProtocol(String address, String json) {
        return createTCPProtocol(address, parse(json));
    }

    /** JSON-text form of {@link #createTCPProtocol(IPAddress, NVGenericMap)}. */
    public static TCPMetaProtocol createTCPProtocol(IPAddress address, String json) {
        return createTCPProtocol(address, parse(json));
    }

    /** JSON-text form of {@link #createUDPProtocol(InetSocketAddress, NVGenericMap)}. */
    public static UDPMetaProtocol createUDPProtocol(InetSocketAddress remote, String json) {
        return createUDPProtocol(remote, parse(json));
    }

    /** JSON-text form of {@link #createUDPProtocol(String, NVGenericMap)}. */
    public static UDPMetaProtocol createUDPProtocol(String address, String json) {
        return createUDPProtocol(address, parse(json));
    }

    /** JSON-text form of {@link #createUDPProtocol(IPAddress, NVGenericMap)}. */
    public static UDPMetaProtocol createUDPProtocol(IPAddress address, String json) {
        return createUDPProtocol(address, parse(json));
    }

    // ---- callback forms: the same factories plus a results consumer, fired once on close ----
    // The consumer receives the final verdict bag (getResults()) exactly once when the session
    // closes — completion, failure, or remote EOF alike — the event-driven alternative to
    // waitForClose. It occupies the protocol's onClose hook and runs inside the close path:
    // hand real work to an executor rather than block in place.

    /** {@link #createTCPProtocol(InetSocketAddress, NVGenericMap)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(InetSocketAddress remote, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(remote, config), callback);
    }

    /** {@link #createTCPProtocol(String, NVGenericMap)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(String address, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(address, config), callback);
    }

    /** {@link #createTCPProtocol(IPAddress, NVGenericMap)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(IPAddress address, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(address, config), callback);
    }

    /** {@link #createTCPProtocol(InetSocketAddress, String)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(InetSocketAddress remote, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(remote, json), callback);
    }

    /** {@link #createTCPProtocol(String, String)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(String address, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(address, json), callback);
    }

    /** {@link #createTCPProtocol(IPAddress, String)} with a results callback. */
    public static TCPMetaProtocol createTCPProtocol(IPAddress address, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createTCPProtocol(address, json), callback);
    }

    /** {@link #createUDPProtocol(InetSocketAddress, NVGenericMap)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(InetSocketAddress remote, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(remote, config), callback);
    }

    /** {@link #createUDPProtocol(String, NVGenericMap)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(String address, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(address, config), callback);
    }

    /** {@link #createUDPProtocol(IPAddress, NVGenericMap)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(IPAddress address, NVGenericMap config,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(address, config), callback);
    }

    /** {@link #createUDPProtocol(InetSocketAddress, String)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(InetSocketAddress remote, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(remote, json), callback);
    }

    /** {@link #createUDPProtocol(String, String)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(String address, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(address, json), callback);
    }

    /** {@link #createUDPProtocol(IPAddress, String)} with a results callback. */
    public static UDPMetaProtocol createUDPProtocol(IPAddress address, String json,
                                                    Consumer<NVGenericMap> callback) {
        return withCallback(createUDPProtocol(address, json), callback);
    }

    private static TCPMetaProtocol withCallback(TCPMetaProtocol protocol, Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback can't be null", callback);
        return protocol.onClose(closed -> callback.accept(closed.getResults()));
    }

    private static UDPMetaProtocol withCallback(UDPMetaProtocol protocol, Consumer<NVGenericMap> callback) {
        SUS.checkIfNulls("callback can't be null", callback);
        return protocol.onClose(closed -> callback.accept(closed.getResults()));
    }

    private static NVGenericMap parse(String json) {
        SUS.checkIfNulls("json can't be null", json);
        return GSONUtil.fromJSONDefault(json, NVGenericMap.class);
    }

    // ---- the CLI ----

    public static void main(String[] args) {
        int exit = run(args);
        System.exit(exit);
    }

    static int run(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: ProtoConnect <definition.json> <host[:port]> [name=value ...]");
            return 64;
        }

        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("cannot load definition " + args[0] + ": " + e);
            return 64;
        }

        NIOSocket nioSocket = null;
        try {
            NVGenericMap config = GSONUtil.fromJSONDefault(json, NVGenericMap.class);
            boolean udp = "udp".equalsIgnoreCase(ProtoUtil.stringValue(config, "transport", "tcp"));
            TCPMetaProtocol tcp = udp ? null : createTCPProtocol(args[1], json);
            UDPMetaProtocol udpProtocol = udp ? createUDPProtocol(args[1], json) : null;

            ExchangeScript script = udp ? udpProtocol.getScript() : tcp.getScript();
            for (int i = 2; i < args.length; i++) {
                int eq = args[i].indexOf('=');
                if (eq <= 0) {
                    System.err.println("bad variable (want name=value): " + args[i]);
                    return 64;
                }
                script.setVar(args[i].substring(0, eq), args[i].substring(eq + 1));
            }

            System.out.println("CONNECTING " + args[1] + " (" + script.getName() + ", "
                    + (udp ? "udp" : "tcp") + ")");
            nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
            long waitMillis = TimeUnit.SECONDS.toMillis(Math.max(script.getTimeoutSec(), 1) * 2L);

            int exit;
            if (udp) {
                nioSocket.addDatagramSocket(new InetSocketAddress(0), udpProtocol);
                exit = outcome(udpProtocol.waitForClose(waitMillis), udpProtocol.getCloseCause(),
                        udpProtocol.getResults());
                SharedIOUtil.close(udpProtocol);
            } else {
                nioSocket.addClientSocket(tcp); // remote, timeout and resolver ride on the protocol
                exit = outcome(tcp.waitForClose(waitMillis), tcp.getCloseCause(), tcp.getResults());
                SharedIOUtil.close(tcp);
            }
            return exit;
        } catch (Exception e) {
            System.err.println("probe error: " + e);
            return 64;
        } finally {
            SharedIOUtil.close(nioSocket);
        }
    }

    private static int outcome(boolean closed, Throwable cause, NVGenericMap results) {
        System.out.println("results: " + results);
        if (cause != null)
            System.out.println("cause: " + cause);
        if (!closed) {
            System.out.println("verdict: NO COMPLETION (exit 2)");
            return 2;
        }
        boolean validated = Boolean.TRUE.equals(results.getValue(ProtoUtil.ResKey.VALIDATED)) && cause == null;
        System.out.println("verdict: " + (validated ? "VALIDATED (exit 0)" : "FAILED (exit 1)"));
        return validated ? 0 : 1;
    }

    /** The definition's default-port hint: the first declared port, -1 when none. */
    private static int defaultPort(NVGenericMap config) {
        int[] ports = ProtoUtil.ports(config);
        return ports.length > 0 ? ports[0] : -1;
    }

    private static InetSocketAddress requireEndpoint(InetSocketAddress endpoint, String source) {
        if (endpoint == null)
            throw new IllegalArgumentException(
                    "endpoint needs a port (none in '" + source + "', no port hint in the definition)");
        return endpoint;
    }

    /**
     * Parses {@code host[:port]}; falls back to {@code hintPort} when the endpoint omits the
     * port. Returns null when no port is available from either source.
     */
    static InetSocketAddress parseEndpoint(String endpoint, int hintPort) {
        int c = endpoint.lastIndexOf(':');
        if (c > 0)
            return new InetSocketAddress(endpoint.substring(0, c), Integer.parseInt(endpoint.substring(c + 1)));
        return hintPort > 0 ? new InetSocketAddress(endpoint, hintPort) : null;
    }
}
