# NET — Building Network Services with `org.zoxweb.server.net`

This document is a build guide. Given a description of a network service — TCP server, TLS
server, client connection, UDP service, or tunnel — use it to construct a working
implementation with the `org.zoxweb.server.net` package (zoxweb-core, Java 8 — no
post-Java-8 language features or APIs).

Source code: [https://github.com/fidusio/zoxweb-core](https://github.com/fidusio/zoxweb-core)
(package: [`src/main/java/org/zoxweb/server/net`](https://github.com/fidusio/zoxweb-core/tree/master/src/main/java/org/zoxweb/server/net))

## The model

A single **NIOSocket** instance is a selector-loop engine that simultaneously drives TCP
servers, TCP clients, and UDP sockets. Protocols plug in through factories; per-connection
logic lives in session callbacks. Layering is strict: transport (`NIOSocket`), crypto
(`net.ssl`), and protocol (callbacks) never mix.

| Concept | Class | Role |
|---|---|---|
| Engine | `NIOSocket` | The selector loop (one dedicated thread, "NIO-SOCKET") + worker-pool dispatch. One instance serves any number of ports and protocols |
| Selector guard | `SelectorController` | Thread-safe register/cancel/select against the shared `Selector`; any worker may register channels mid-flight |
| Per-connection handler | `ProtocolHandler` (base), `NIOSocketHandler` (plain TCP), `SSLNIOSocketHandler` (TLS) | Owns the channel, the read buffer, and the optional idle-timeout circuit breaker |
| Factory | `ProtocolFactory` / `ProtocolFactoryBase` | Mints one handler per accepted connection; carries the port's ACLs and setup policy |
| Application callback | `BaseSessionCallback<CF>` (server), `TCPSessionCallback` (client), `UDPSessionCallback` (UDP) | Where your protocol logic goes: `accept(data)`, `exception(e)`, lifecycle hooks |
| Write bridge | `BaseChannelOutputStream` / `CommonChannelOutputStream` | `OutputStream` API over the NIO channel; same object does plaintext and TLS (mode-switchable) |
| TLS config | `SSLContextInfo` (engine factory), `SSLSessionConfig` (per-session state) | Server mode from keystore/`SSLContext`; client mode from target address (+SNI) |
| ACLs | `InetFilterRulesManager` | CIDR allow/deny rules checked inline on every accept |
| Bootstrap | `NIOConfig` | Assemble whole deployments (ports, TLS, ACLs) from a JSON `ConfigDAO` |

## Semantics contract (the rules the engine obeys)

1. **The selector thread is sacred.** It only selects, accepts, and dispatches. All
   protocol work runs on the executor's worker threads. Never block, sleep, or do crypto
   in code that runs on the selector thread.
2. **One session = one thread at a time (key-interest gating).** Before dispatching a
   readable channel, the engine sets the key's interest ops to 0; the worker re-arms
   `OP_READ` only after the handler's full cycle completes. While a worker owns a session,
   that session cannot be dispatched again — per-session state needs no locking. Different
   sessions run fully parallel.
3. **Drain before releasing.** Handlers read in a `do { read } while (read > 0)` loop —
   bytes arriving during processing must be consumed before re-arming, or they wait for
   the next readiness event.
4. **Writes are synchronous and complete-or-fail.** A write call returns only when every
   byte is on the wire (or throws). There is no partial-write state to manage and no
   write-ready (`OP_WRITE`) machinery. A slow peer stalls the writing worker — callers own
   their behavior.
5. **`read == -1` (or any I/O exception) means close the session.** Every handler closes
   itself; `close()` is idempotent everywhere.
6. **Buffers are pooled.** Allocate via `ByteBufferUtil.allocateByteBuffer(...)`, return
   via `ByteBufferUtil.cache(...)` on close. Never remove recaching and never copy-per-
   dispatch — pooling is a load-proven defense against GC thrash. Consequence: a pooled
   buffer handed to a callback is valid **only until the callback returns**; copy the
   bytes if you need them asynchronously.
7. **The idle timeout is a security circuit breaker.** `ProtocolHandler`'s optional
   `PHTimeout` (default ~2.5 min) hard-closes sessions with no I/O exchange — defense
   against idle-connection squatting (slowloris-style fd exhaustion). Enable it for
   request/response protocols; disable (`timeout=false`) only for protocols that
   legitimately idle (tunnels, push channels).
8. **ACLs are checked inline on accept.** Denied connections are counted, logged,
   optionally published as `IPAddressEvent`s (for `IPBlockerListener` auto-blocking), and
   closed. Loopback is always allowed.
9. **Errors (`java.lang.Error`) are out of scope.** JVM-level failures are not handled by
   the socket layer; supervision (process manager) owns recovery. Handlers deal in
   `Exception`s only.

## Buffer-mode contract (critical — two conventions)

The `flip` discipline runs through the whole stack, and the server-side convention is
**dictated by TLS**: `SSLEngine.unwrap` leaves its destination buffer in write-mode (the
JDK engine owns the flip discipline), and the same code path serves TLS 1.2 and 1.3
without version-specific handling. The plain handler adopts the identical convention so
that **one callback implementation runs unchanged over plain or TLS** (see
`EchoProtocol`: `EchoSession` and `SSLEchoSession` are identical). Know which mode your
callback receives:

- **Server-side callbacks** (`BaseSessionCallback.accept(ByteBuffer)` via
  `NIOSocketHandler` or the SSL path): the buffer arrives in **write-mode** (position =
  end of data). Use helpers that flip internally, e.g.
  `ByteBufferUtil.write(byteBuffer, ubaos, true)`.
- **Client-side callbacks** (`TCPSessionCallback.accept(ByteBuffer)`): the buffer arrives
  already **flipped (read-mode)** — consume from `position` to `limit`.
- **Writing**: `BaseChannelOutputStream.write(ByteBuffer, boolean flip)` — pass
  `flip=true` if your buffer is in write-mode, `false` if already read-mode (e.g. from
  `ByteBuffer.wrap(...)`). The `OutputStream` byte/array methods handle this for you.
- **UDP**: `DataPacket.getBuffer()` is read-mode, pool-owned, and reclaimed the moment
  your `accept(DataPacket)` returns — copy if processing asynchronously.

## Build recipes

### Step 0 — One engine per process

```java
NIOSocket nioSocket = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
```

Executor = worker pool for all protocol processing; scheduler = connect timeouts and idle
reapers. Reuse this single instance for every server, client, and UDP socket. `close()`
shuts everything down.

### Recipe A — Plain TCP server

1. Implement the protocol as a `BaseSessionCallback<BaseChannelOutputStream>`:

```java
public class MySession extends BaseSessionCallback<BaseChannelOutputStream> {
    private final UByteArrayOutputStream ubaos = new UByteArrayOutputStream();

    @Override
    public void accept(ByteBuffer byteBuffer) {            // write-mode buffer (server side)
        try {
            ByteBufferUtil.write(byteBuffer, ubaos, true); // accumulate (flips internally)
            if (messageComplete(ubaos))                    // your framing rule
                getOutputStream().write(ubaos, true);      // respond; true = reset after send
        } catch (IOException e) {
            SharedIOUtil.close(getOutputStream());
        }
    }
    @Override public void exception(Throwable e) { /* log/telemetry */ }
    @Override public void close() throws IOException { /* release protocol resources */ }
    @Override public boolean isClosed() { return getOutputStream() != null && getOutputStream().isClosed(); }
}
```

2. Register on a port (backlog, factory with a session creator — lambda preferred):

```java
nioSocket.addServerSocket(8080, 128, new NIOSocketHandlerFactory(MySession::new));
```

The engine handles accept, ACLs, buffer management, idle timeout, and close. Your code is
only `accept` + framing. Reference implementation: `protocols/EchoProtocol.EchoSession`.

### Recipe B — TLS server

Same callback contract, parameterized with `SSLSessionConfig`:

```java
public class MySecureSession extends BaseSessionCallback<SSLSessionConfig> { ...same shape... }

SSLContext sslContext = SecUtil.initSSLContext(keystorePath, "PKCS12", password, null, null, null);
SSLContextInfo serverTLS = new SSLContextInfo(sslContext, new String[]{"TLSv1.3","TLSv1.2"}, null);
nioSocket.addServerSocket(8443, 512, new SSLNIOSocketHandlerFactory(serverTLS, MySecureSession::new));
```

The callback receives **decrypted** application data and writes plaintext to
`getOutputStream()` — encryption is transparent. The handshake is driven internally by
the SSLEngine's own status values; do not touch the SSL state machinery.
`config.getSNIHostName()` exposes the requested SNI after handshake.

### Recipe C — TCP client (plain or TLS)

Extend `TCPSessionCallback`; connect via `addClientSocket`:

```java
TCPSessionCallback client = new TCPSessionCallback(new IPAddress("example.com:443"), true) { // true = validate certs
    @Override protected void connectedFinished() throws IOException {
        // socket up (and TLS handshake done, if TLS) — send the first request
        getOutputStream().write(requestBytes);
    }
    @Override public void accept(ByteBuffer byteBuffer) {  // read-mode buffer (client side)
        // response bytes
    }
    @Override public void exception(Throwable e) { /* connect/timeout/IO failure */ }
};
client.timeoutInSec(10);
nioSocket.addClientSocket(client);
```

- The two-arg `(IPAddress, certValidationEnabled)` constructor makes a **TLS** client
  (`certValidationEnabled=false` = trust-all, dev only). The plain `(IPAddress)`
  constructor makes a **plaintext** client.
- `connectedFinished()` fires when the connection is usable (post-handshake for TLS).
- Connect timeout is enforced by the scheduler; on expiry the channel closes and
  `exception(IOException)` fires. The framework never retries — construct a new callback
  to retry.
- To abandon an in-flight connect (e.g. a superseded probe), call
  `nioSocket.abortClientSocket(selectionKey)` with the key `addClientSocket` returned.
- STARTTLS-style upgrade: a plain client whose `SSLContextInfo` is set later upgrades in
  place — the same output stream switches to TLS mode.

### Recipe D — UDP service

```java
UDPSessionCallback udp = new UDPSessionCallback(TaskUtil.defaultTaskProcessor(), 9090, 2048) {
    @Override public void accept(DataPacket<?> packet) throws IOException {
        // packet.getAddress() = sender; packet.getBuffer() = payload (read-mode)
        // BUFFER IS POOL-OWNED: valid only until this method returns — copy if async
        send(reply, packet.getAddress(), true);            // thread-safe reply
    }
    @Override public void exception(Throwable e) { }
};
nioSocket.addDatagramSocket(udp);
```

One channel, one callback for all peers. With an executor, datagrams process concurrently —
no ordering guarantees across packets.

### Recipe E — Tunnels / port forwarding

- **Plain relay**: `nioSocket.addServerSocket(port, 128, new NIOTunnel.NIOTunnelFactory(new IPAddress("host:port")))` —
  bidirectional byte relay, zero protocol code.
- **TLS-terminating tunnel**: `new SSLNIOSocketHandlerFactory(serverTLS, remoteIPAddress)` —
  decrypts inbound TLS, forwards plaintext to the remote, re-encrypts responses. Zero
  protocol code (a built-in tunnel callback is used when no session callback is given).

### ACLs and auto-blocking (any recipe)

```java
InetFilterRulesManager acl = new InetFilterRulesManager();
acl.addInetFilterProp("10.0.0.0-255.0.0.0-ALLOW");        // format: ip-netmask-[ALLOW|DENY]
factory.setIncomingInetFilterRulesManager(acl);
```

Prefer pure whitelist or pure blacklist rule sets (mixed sets are order-sensitive).
IPv4-oriented. For fail2ban-style automatic blocking, wire
`nioSocket.setEventManager(TaskUtil.defaultEventManager())` and configure an
`IPBlockerListener` — denied connects then feed attack-rate tracking.

### JSON bootstrap (alternative to code)

`NIOConfig` builds an entire deployment from a JSON `ConfigDAO`: per-port factories,
keystore-backed `SSLContext`s (`keystore_file/type/password`, `protocols`, `ciphers`),
`remote_host` for tunnels, and `incoming/outgoing_inet_rule` lists. Use when ports and TLS
material must be operator-configurable without code.

## Design mapping — description to implementation

1. **"Serve protocol X on port N"** → Recipe A: a `BaseSessionCallback` with your framing
   in `accept`, one `addServerSocket` line.
2. **"...over TLS"** → Recipe B: same callback against `SSLSessionConfig`, add an
   `SSLContextInfo` from the keystore.
3. **"Call a remote service"** → Recipe C: `TCPSessionCallback`, first send in
   `connectedFinished`, responses in `accept`.
4. **"Datagram/discovery/telemetry ingest"** → Recipe D.
5. **"Forward/expose/terminate-TLS for another service"** → Recipe E — configuration, not code.
6. **Message framing is yours**: TCP is a byte stream; accumulate in a
   `UByteArrayOutputStream` and detect message boundaries yourself (delimiter, length
   prefix, etc.). One `accept` call ≠ one message.
7. **Multiple services** → one `NIOSocket`, many `addServerSocket`/`addClientSocket`/
   `addDatagramSocket` calls. Plain and TLS ports coexist on the same engine.
8. **Restricted access** → ACL rules on the factory; **hostile environment** → add the
   event manager + `IPBlockerListener`.
9. **Long-idle protocol** → construct the factory/handler with `timeout=false`; otherwise
   keep the default idle circuit breaker.

## Pitfalls checklist

- Not knowing which buffer-mode convention applies: server-side buffers arrive in
  write-mode (a deliberate, TLS-dictated convention — see the buffer-mode contract),
  client-side (`TCPSessionCallback`) arrive read-mode. Use the flip-aware helpers and
  the convention is invisible; guess wrong and you silently read stale bytes.
- Treating a delivered buffer as an owned snapshot. TCP session buffers are
  session-scoped and overwritten on the next read cycle; UDP `DataPacket` buffers are
  returned to the pool the moment `accept` returns. Either way, contents are valid only
  within the callback — copy if you need them afterward. (The pooling is a load-proven
  defense against GC thrash; the copy is the caller's job, by design.)
- Misusing `isComplexSetup`. Inline connection setup on the selector thread is a
  deliberate performance win (20–30% under load — a thread hand-off costs more than
  allocation-only setup; even the TLS factory runs inline, since handshake crypto
  happens later on workers). Set `complexSetup = true` **only** when your factory's
  setup genuinely blocks (I/O, DNS, upstream connect, HSM) — that is the one case where
  inline setup would stall the whole engine. Inside `accept` (worker thread), blocking
  is permitted and is the implementation's own choice — sync or async processing is the
  protocol's decision.
- Assuming one `accept` = one message. The engine deliberately delivers chunks eagerly
  (draining available bytes without releasing the thread — re-dispatching per chunk
  would waste pool resources): the callback assembles (e.g. `UByteArrayOutputStream`),
  parses, and if the request is incomplete simply returns — releasing the worker — and
  continues on the next dispatch. Framing is the callback's job by design; partial
  requests are normal (slow clients, network buffering).
- Forgetting an `exception(Throwable)` implementation on clients — connect failures and
  timeouts arrive there, possibly in addition to a synchronous throw from
  `addClientSocket`; treat them as one event.
- Disabling the idle timeout on an internet-facing request/response port — removes the
  idle-squatting defense.
- Trust-all TLS (`certValidationEnabled=false`, `SSLCheckDisabler`) in production.
- Mixed ALLOW+DENY rule sets — default action becomes insertion-order dependent; keep
  rule sets homogeneous.
- Calling `close()` on the engine expecting in-flight worker tasks to be interrupted —
  close stops the loop and closes channels; running tasks finish on their own.

---

## Appendix: the design, framework-agnostic

To implement this architecture outside Java/zoxweb, build these pieces:

1. **Single event loop + worker pool**: one thread multiplexes readiness across all
   sockets and never executes application work; a bounded pool runs handlers.
2. **Readiness gating for serialization**: on dispatch, remove the socket from the
   interest set; re-add only when the handler's cycle (read → process → respond → drain
   again) completes. This makes each session single-threaded without locks while all
   sessions run in parallel.
3. **Complete-or-fail synchronous writes**: a write returns when fully sent or fails —
   no partial-write resumption state, no write-readiness events.
4. **Pooled I/O buffers** with a borrow/return lifecycle tied to dispatch and session
   close; payloads are valid only within the handler call.
5. **Pluggable protocol factories** per listening port; per-connection handler objects
   own channel + buffers + an optional idle-kill timer (security, not convenience).
6. **TLS as a layer, not a fork**: the same handler/callback shape, with an engine-driven
   handshake state machine between the socket and the callback; the write bridge switches
   from plaintext to record-encrypting mode in place (enables STARTTLS upgrades).
7. **Inline accept-time ACLs** with attack counting and an event hook for automated
   blocking; the app must survive any per-session failure — a dying session closes
   itself and nothing else.
