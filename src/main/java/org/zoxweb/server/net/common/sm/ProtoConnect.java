package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.SharedIOUtil;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Command-line runner for a protocol description: connects to a remote endpoint and
 * drives the {@link ClientConSM} the config declares, printing the connection lifecycle to
 * stdout.
 * <p>
 * Usage:
 * <pre>
 *   ProtoConnect host:port path/to/proto-config.json [var=value ...]
 * </pre>
 * <ul>
 * <li>{@code host:port} — the endpoint to dial. The config never names it (it describes the
 * protocol, not the endpoint); the port may be omitted if the config carries a {@code port}
 * default-port hint.</li>
 * <li>{@code path/to/proto-config.json} — the protocol description (schema:
 * {@code META-SM-PROTO-DESIGN.md}).</li>
 * <li>{@code var=value} — optional {@code exchange} variables injected into the session (resolve
 * {@code ${var}} placeholders in send/expect literals).</li>
 * </ul>
 * The run prints {@code CONNECTED}, {@code BANNER_RECEIVED}, {@code SECURE} (with the negotiated
 * TLS protocol/cipher), {@code IN_DATA}, {@code READY}, and {@code CLOSED} events as they fire, then
 * exits when the session closes. A {@code "transport": "udp"} config dials a connected datagram
 * socket instead; a probe config's machine closes the session itself once the pipeline completes
 * ({@code close_on_ready}) — this runner never drives the session, it only observes. Exit code:
 * {@code 0} clean close, {@code 1} closed with a cause (failed check), {@code 2} timed out without
 * closing, {@code 64} usage error.
 */
public final class ProtoConnect {

    private ProtoConnect() {
    }

    /** Extra seconds to wait for the dialogue to complete after the connect timeout elapses. */
    private static final int RUN_GRACE_SEC = 30;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: ProtoConnect host:port proto-config.json [var=value ...]");
            System.exit(64);
            return;
        }

        String hostPort = args[0];
        String configPath = args[1];

        String json = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
        ClientConSM sm = ClientSMFactory.fromJSON(json);

        // optional var=value pairs -> injected exchange variables
        for (int i = 2; i < args.length; i++) {
            int eq = args[i].indexOf('=');
            if (eq <= 0) {
                System.err.println("ignoring malformed var (expected name=value): " + args[i]);
                continue;
            }
            sm.getContext().setVar(args[i].substring(0, eq), args[i].substring(eq + 1));
        }

        InetSocketAddress remote = resolveRemote(hostPort, sm);
        int timeoutSec = ClientSMFactory.timeoutSec(sm.getContext().getSettings());

        NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
        int exit;
        try {
            exit = run(remote, sm, nioSocket, timeoutSec, timeoutSec + RUN_GRACE_SEC);
        } finally {
            SharedIOUtil.close(nioSocket);
            TaskUtil.close();
        }
        System.exit(exit);
    }

    /**
     * Wires the lifecycle listeners, connects the session on {@code nioSocket}, and blocks until the
     * session closes (or {@code maxWaitSec} elapses), printing each event. Does not create or tear
     * down the transport / task pools — the caller owns those.
     *
     * @param remote     the endpoint to dial
     * @param sm         the machine built from the config
     * @param nioSocket  the transport to connect on
     * @param timeoutSec the connect timeout handed to {@code addClientSocket}
     * @param maxWaitSec the overall wait for the session to close
     * @return exit code: 0 clean close, 1 closed with a cause, 2 no completion within the wait
     */
    static int run(InetSocketAddress remote, ClientConSM sm, NIOSocket nioSocket,
                   int timeoutSec, long maxWaitSec) throws java.io.IOException, InterruptedException {
        final CountDownLatch closedLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> closeCause = new AtomicReference<Throwable>();
        final boolean udp = sm.getContext().getTransport() == ClientSessionContext.Transport.UDP;
        final AtomicReference<AutoCloseable> sessionRef = new AtomicReference<AutoCloseable>();

        State<Object> app = new State<Object>("proto-connect");
        // payload-agnostic: CONNECTED carries a SelectionKey over TCP, the remote address over UDP
        app.register((Consumer<Object>) o -> System.out.println("CONNECTED " + remote),
                SMProtoUtil.BasicEvent.CONNECTED);
        app.register((Consumer<String>) banner -> System.out.println("BANNER_RECEIVED " + banner),
                ClientEvent.BANNER_RECEIVED);
        app.register((Consumer<Object>) sci -> System.out.println("SECURE " + describeTLS(sci)),
                ClientEvent.SECURE);
        app.register((Consumer<Object>) o -> {
            System.out.println("READY");
            // register the data consumer only now: pre-READY IN_DATA belongs to the negotiating
            // phases (one active owner per buffer) — a global consumer would double-consume/recache
            app.register((Consumer<ByteBuffer>) bb -> {
                int n = bb.remaining();
                byte[] chunk = new byte[n];
                bb.get(chunk);
                ByteBufferUtil.cache(bb);
                System.out.println("IN_DATA (" + n + " bytes): " + printable(chunk));
            }, ClientEvent.IN_DATA);
        }, ClientEvent.READY);
        app.register((Consumer<Throwable>) cause -> {
            closeCause.set(cause);
            System.out.println("CLOSED" + (cause != null ? " cause: " + cause : " (clean)"));
            closedLatch.countDown();
        }, SMProtoUtil.BasicEvent.CLOSED);
        sm.register(app);

        try {
            if (udp) {
                UDPSMCallback callback = sm.newSessionCallback(remote);
                sessionRef.set(callback);
                System.out.println("connecting (udp) to " + remote + " ...");
                nioSocket.addDatagramSocket(new InetSocketAddress(0), callback);
            } else {
                TCPSMCallback callback = sm.newSessionCallback();
                sessionRef.set(callback);
                System.out.println("connecting to " + remote + " (timeout " + timeoutSec + "s) ...");
                nioSocket.addClientSocket(remote, callback, timeoutSec, null);
            }
            if (!closedLatch.await(maxWaitSec, TimeUnit.SECONDS)) {
                System.out.println("no completion within " + maxWaitSec + "s");
                return 2;
            }
            return closeCause.get() != null ? 1 : 0;
        } finally {
            SharedIOUtil.close(sessionRef.get());
        }
    }

    /**
     * Parses {@code host:port}; if the port is omitted, falls back to the config's {@code port}
     * default-port hint. IPv6 literals must be bracketed ({@code [::1]:443}).
     */
    static InetSocketAddress resolveRemote(String hostPort, ClientConSM sm) {
        String host;
        int port;
        if (hostPort.startsWith("[")) { // [ipv6]:port
            int close = hostPort.indexOf(']');
            if (close < 0)
                throw new IllegalArgumentException("malformed IPv6 host: " + hostPort);
            host = hostPort.substring(1, close);
            String rest = hostPort.substring(close + 1);
            port = rest.startsWith(":") ? Integer.parseInt(rest.substring(1)) : -1;
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon >= 0) {
                host = hostPort.substring(0, colon);
                port = Integer.parseInt(hostPort.substring(colon + 1));
            } else {
                host = hostPort;
                port = -1;
            }
        }
        if (port < 0)
            port = ClientSMFactory.port(sm.getContext().getSettings(), -1);
        if (host.isEmpty() || port < 0)
            throw new IllegalArgumentException(
                    "no port given and config has no 'port' hint: " + hostPort);
        return new InetSocketAddress(host, port);
    }

    /** @return the negotiated TLS protocol + cipher from a {@code SECURE} payload, best-effort. */
    private static String describeTLS(Object sci) {
        if (sci instanceof SSLConfigInt) {
            try {
                SSLSession session = ((SSLConfigInt) sci).getSSLEngine().getSession();
                return session.getProtocol() + " " + session.getCipherSuite();
            } catch (RuntimeException e) {
                return "(session unavailable)";
            }
        }
        return String.valueOf(sci);
    }

    /** Renders bytes for the console: printable ASCII kept, everything else shown as a dot. */
    static String printable(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            int c = b & 0xFF;
            sb.append(c >= 0x20 && c < 0x7F || c == '\r' || c == '\n' || c == '\t' ? (char) c : '.');
        }
        return sb.toString();
    }
}
