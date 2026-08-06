# PENDING

Open work items and deferred findings for zoxweb-core.

Line numbers are as of 2026-08-05 (§5 added 2026-08-06) and drift with edits — treat them as hints,
re-locate by symbol. Nothing in the "Deferred findings" sections has been changed in the codebase;
those sections are assessment only.

---

## 1. Work in progress — FSM-driven NIO session (greenfield)

New design, driven by `org.zoxweb.server.fsm`. The existing `TCPSessionCallback` path stays as-is
and keeps working; this is not a port.

**Files:** `server/net/common/TCPSMCallback.java`

### Design decisions still open

- **`CF` type parameter.** Currently `BaseSessionCallback<StateMachineInt<?>>`. Two consequences:
  the wildcard prevents consumers from getting a typed config out of
  `getStateMachine().getConfig()`, and it structurally blocks the SSL layer, which requires a
  `BaseSessionCallback<SSLSessionConfig>` (see §2). Candidate: hold the protocol machine in its own
  field and leave `CF` for the session config.
- **One machine per connection vs. one shared machine per protocol.** Currently implied
  per-connection by the `auto_closeable` property write (see below). Per-connection gives each
  session its own current-state marker and property bag; shared is far cheaper at 19K conn/sec but
  then per-connection data may never live in state properties (the property bag is shared mutable
  context — see FSM.md). This decision dictates what consumers are allowed to touch.
- **Event vocabulary / payload types.** FSM.md rule: one canonical ID = one payload type, forever.
  - `CLOSED` currently carries a `Throwable` (published from `exception()`). A real close event with
    any other payload will give every consumer a runtime `ClassCastException`. Split into `ERROR`
    (`Throwable`) and `CLOSED` before consumers exist.
  - Decide whether clean peer EOF is its own ID or folded into close.
  - Add `SECURE` / handshake-done ID (see `sslHandshakeSuccessful()` below).

### Bugs to fix

- **`accept(SelectionKey)` exception path leaks and can hot-spin.** `read` is declared outside the
  loop, so when `read()` throws it retains its previous value (`0` on the first iteration, or the
  prior positive count). `read == -1` is therefore false: nothing closes, nothing is published to
  the machine, and `NIOSocket` re-arms `OP_READ` on a channel that throws again immediately — a
  stack-trace loop burning a worker. Highest priority item in this section.
- **Clean EOF publishes no event.** `SharedIOUtil.close(this)` now releases resources correctly
  (delegate is wired), but the machine is never told. FSM sees `CONNECTED` → `RAW_IN_DATA`… →
  permanent silence. Publishing `CLOSED` from the close delegate covers this and the item above.
- **`flipMode == false` branch corrupts the buffer.** `compact()` is unconditional, so whichever
  side owns `flip` must also own `compact`. With the flag false the buffer reaches the consumer in
  write mode and `compact()` copies the unwritten range `[N, cap)` over the payload. Currently
  unreachable (flag is `true`, no setter) but becomes live the moment STARTTLS flips it per phase
  (§2). Do **not** make the flag `final` — mode ownership legitimately changes at the upgrade
  boundary.
- **`auto_closeable` stored in `stateMachine.getProperties()`** pins the design to one machine per
  connection: a second `TCPSMCallback` against the same machine collides on that key and one
  session's teardown reaches the other's resources. Also `HashSet` → nondeterministic close order,
  which matters once the SSL engine must be torn down before the channel; prefer `LinkedHashSet`.
- **`sslHandshakeSuccessful()` is empty** — no event, so consumers cannot gate their first write on
  handshake completion.
- **`connected(SelectionKey)` does not call `setChannel(key.channel())` or `setRemoteAddress(...)`.**
  `getChannel()` returns null and consumers have no remote address for logging/SNI. Compare
  `TCPSessionCallback.connected` lines 190-191.

### Invariant to enforce, not just document

`NIOSocket` sets `interestOps(0)` on the key before dispatch and re-arms only after `accept()`
returns; that gate is what serializes reads per connection. A consumer calling async `publish(...)`
escapes the gate → concurrent reads on the same channel and buffer. Because `publishSync` runs
inline regardless of machine mode, a scheduler-mode machine looks fine until a consumer publishes
async. Consider validating in the constructor (`getExecutor() == null && !isScheduledTaskEnabled()`)
so it fails at startup instead of under load.

### Consumer contract to write down

- `RAW_IN_DATA` payload arrives in **read** mode (already flipped). A consumer must **not** call
  `flip()` on it — that sets `lim=0` and discards everything. Given the field name `flipMode`, this
  is a plausible first mistake.
- The buffer is pool-owned and valid only for the duration of the consumer call. Anything retaining
  data must copy.
- Partial consumption is supported and is the framing mechanism: leave the remainder, `compact()`
  slides it forward, the next read appends. Consuming nothing is safe (data preserved) but a
  consumer that never makes progress eventually fills the buffer → `read()` returns 0 → connection
  stalls silently with no error. Decide whether that is a documented consumer obligation or
  something the callback detects (matters for protocols with frames > 16K).

---

## 2. Planned — `SSLStateMachineV2`

Motivation: model STARTTLS probing and related protocol-phase logic, which the current machine
cannot express because its canonical IDs *are* the `SSLEngineResult.HandshakeStatus` values — no
room for `PROBING` / `PLAINTEXT` / `UPGRADE_REQUESTED` / `UPGRADING` / `SECURE` / `DOWNGRADED`.
That is a vocabulary change, not a logic change.

Additive by design — keep `SSLStateMachine` and `CustomSSLStateMachine` untouched (load-proven,
fragile).

### The seam

`SSLConnectionHelper` is three methods:

```java
void publish(HandshakeStatus status, BaseSessionCallback<SSLSessionConfig> callback);
void createRemoteConnection();
SSLSessionConfig getConfig();
```

`SSLSessionConfig.sslConnectionHelper` is typed to that interface, so a V2 is selectable per session
with zero edits to `SSLSessionConfig`, `SSLUtil`, or the existing machines.

### Scope discipline

V2 owns **orchestration only** — keep calling the same `SSLUtil` handlers (`_needWrap`,
`_needUnwrap`, `_needTask`, `_finished`, `_notHandshaking`) for engine steps. STARTTLS changes *when*
the handshake cycle is entered, not what happens inside it.

Note the payload constraint: `SSLStateMachine.publish` puts the **callback itself** in the trigger
(`Trigger<BaseSessionCallback<SSLSessionConfig>>`), and every `SSLUtil` handler takes
`(SSLSessionConfig, BaseSessionCallback<SSLSessionConfig>)`. So the session callback must be a
`BaseSessionCallback<SSLSessionConfig>` to participate at all — this is what §1's `CF` decision
gates.

### Security requirement — buffer state at the upgrade edge

After the server's STARTTLS go-ahead (e.g. `220 Ready`), any bytes already in the plaintext read
buffer **beyond** that response line were injected by whoever controls the wire, not by the
authenticated peer. Processing them inside the TLS session is the STARTTLS plaintext-injection class
(Postfix / Exim / most MTAs). Correct behavior: treat residue after the go-ahead as **fatal** — drop
the connection, do not clear-and-continue. A well-behaved peer has nothing pending there.

The FSM shape makes this enforceable in one named edge instead of implicitly in a read loop. If
`rawReadBuffer` becomes `inSSLNetData` via `beginHandshake(inRawBuffer, ...)`, it must be verified
**empty**, not merely compacted, at the moment of upgrade.

### Other design notes

- **Buffer ownership at handoff.** `SSLSessionConfig.close()` calls
  `ByteBufferUtil.cache(inSSLNetData, inAppData, outSSLNetData, inRemoteData)` and the §1 close
  delegate also caches `rawReadBuffer`. If they are the same buffer it is returned to the pool
  twice; `cache0`'s `contains()` dedup happens to absorb this today but that is incidental, not a
  contract. Pick one owner.
- **Downgrade policy as an explicit state.** When a peer does not advertise STARTTLS: terminal
  failure or plaintext downgrade? Making `DOWNGRADED` explicit allows per-destination "TLS required"
  enforcement later without touching flow (submission must enforce; MTA-to-MTA opportunistic
  cannot — see §3.2).
- **Close path.** If V2 owns teardown, fix close_notify there rather than inheriting the current
  behavior (§3.3).

---

## 3. Deferred findings — `org.zoxweb.server.net` review

Assessment only; user deferred action ("don't worry about those issues for now"). Nothing changed.
Items marked **[verified]** were confirmed by reading the source directly; others came from review
agents and still need confirmation before acting.

### 3.1 Security / correctness, outside the fragile TLS paths

1. **[verified] IP filter fails open on mixed rule sets** — `InetFilterRulesManager.java:288-332`.
   In `checkIPSecurityStatus`, `ret` is reassigned every iteration before the match test
   (`ret = DENY` in the ALLOW case, `ret = ALLOW` in the DENY case), so an address matching no rule
   is decided by the **type of the last rule in the list**. Add `10.0.0.0/8 ALLOW` then any DENY
   rule → unmatched 8.8.8.8 returns ALLOW. A whitelist becomes fail-open the moment a deny rule is
   appended; reversing insertion order flips the verdict.

2. **[verified] Client TLS never verifies server identity** — `SSLContextInfo.java:309-324`,
   `:389-416`. `setEndpointIdentificationAlgorithm` appears **nowhere** in the repo (grep
   confirmed). The `certValidationEnabled = true` path — documented as production mode — validates
   the chain but never checks the name. Passing host/port to `createSSLEngine` only enables SNI and
   session caching; raw `SSLEngine` does no identity check by default (unlike `HttpsURLConnection`,
   which layers a `HostnameVerifier` on top). A MITM with any CA-issued cert for any domain is
   accepted. Affects `HTTPURLCallback.java:145` (all HTTPS), `TCPSessionCallback.java:55`,
   `SSLNIOSocketHandler.java:224`. Contrast `OkHTTPCall.createOkHttpBuilder:199-203`, which is
   correct in both modes — same library, two clients, same flag name, different guarantees.
   **Fix is not a hardcoded constant:** `"HTTPS"` is the right ruleset for mail ports too (SunJSSE
   only recognizes `"HTTPS"` / `"LDAPS"`; an unrecognized string throws), but MTA-to-MTA port 25
   opportunistic STARTTLS (RFC 7435) must **not** enforce it or delivery breaks. Make it a
   configurable field defaulting to `"HTTPS"` in client mode, nullable for opportunistic links.
   Also: `getSSLParameters()` returns a copy — read/modify/`setSSLParameters` round trip required;
   sequence against the `SSLGroupSetterInt` call at `:407-413`, which may rebuild parameters.

3. **[verified] `NIOConfig` secure-tunnel path always throws** — `NIOConfig.java:120`. `createApp()`
   casts the `ssl_engine` attachment to `SSLContext`, but `parse()` (`:188`) attaches an
   `SSLContextInfo`, which only implements `InstanceFactory.Creator<SSLEngine>`. Any config with a
   TLS-enabled `NIOTunnelFactory` throws `ClassCastException` out of `createApp()`, so that service
   and every service after it in the loop never starts. Related: the type test at `:162` is inverted
   (`clazz.isAssignableFrom(SSLContext.class)`), working only because configs name exactly
   `javax.net.ssl.SSLContext`.

4. **[verified] `DatagramRelay` — four separate defects** —
   - `:157-161` `close()` closes only `dsServer`; `dsRemote` leaks its fd and a thread parked in
     `receive()` is never unblocked (loop guard is never re-checked).
   - `:177-181` `ds.setSoTimeout(5000)` is commented out → one lost UDP reply wedges the
     single-threaded loop permanently; every later query is silently ignored.
   - `:181` `dsRemote` is unconnected and the reply source is never validated → any host that finds
     the ephemeral port can inject a forged response, forwarded to the client as authoritative
     (documented use case is DNS). Also delivers a late reply for request N to request N+1's client.
   - `:113` port validation `localPort < 65535` rejects the legal port 65535.

5. **`SecureNetworkTunnel.java:76-90`** — any single exception in the accept loop closes the
   **server** socket and exits the thread, leaking the just-accepted client socket. One transient
   backend `ConnectException` takes down the listener until restart, unlogged. Also `:161` `main`
   exits with status 0 on startup failure.

6. **[verified] IPv6 through a masked IPv4 rule throws past the catch** —
   `SharedNetUtil.getNetwork:118-126` sizes its loop by `address.length` (16) while indexing a
   4-byte netmask → `ArrayIndexOutOfBoundsException`. `InetFilterRulesManager` catches only
   `IOException` (`:303`, `:317`), so it escapes: no verdict rendered, and on the `NIOSocket` accept
   path the just-accepted channel is not closed (only the deny branch's `finally` closes it). Every
   IPv6 connection on a dual-stack listener with masked rules leaks an fd and skips DENY evaluation.

7. **`IPBlockerListener`** — `:209` `attackRate` uses long integer division before storing to a
   float, truncating every fractional rate to 0 (9 attacks / 10 min → 0, not 0.9), so any configured
   rate below 1/min can never trigger a block; `:106`, `:123-131`, `:198-206` `ripiMap` is a plain
   `LinkedHashMap` mutated from event threads and the scheduler thread with the `ReentrantLock`
   guarding only command execution; `:139-145` + `:215-224` a value parsed from a monitored log line
   reaches `Runtime.exec` unvalidated (`IPAddress` does no format checking) — log-injection →
   argument injection, full command injection if the configured command wraps a shell; `:145` port 22
   hardcoded; `:83-90` vs `:271-275` `close()` never cancels the self-rescheduling `clearTimeouts`
   task.

8. **`ClamAVClient`** — `:65-78` streaming failure swallowed, so the wrapped stream keeps delivering
   unscanned data to the consumer; `:80-89` / `:188-208` `close()` ↔ `finishScan()` mutual recursion
   clobbers the real clamd reply with an empty string; `:105`+ the `timeout` parameter is validated
   then discarded (every `setSoTimeout` commented out); `:222-236` `scan(InputStream)` builds with
   `ci = null` → guaranteed NPE per scan, and `close()` closes the caller's stream, contradicting
   its own javadoc.

### 3.2 Pooled-buffer recycling — documentation gaps

`ByteBufferUtil.cache(...)` is called from close paths with no mutual exclusion against a worker
still reading into those buffers (reachable via the idle-timeout scheduler thread or the peer side of
a tunnel). A recycled buffer handed to a new connection while still being filled = cross-session data
bleed. Per project rule, fix by **documenting the invariant** ("close must not recycle while
`accept` may be in flight"), never by removing recaching or copying per dispatch.

Sites: `NIOTunnel.java:106` **[verified]**, `ChannelRelayTunnel.java:94`,
`TCPSessionCallback.java:40-43`, `NIOSocketHandler.java:38`, `SSLSessionConfig.java:121`.

One genuine leak rather than a doc gap: **`ChannelRelayTunnel.java:85-95`** — the
`closeInterface != null` branch never recaches `sBuffer` at all, never cancels `currentSK`, and never
closes `readSource`; since `isClosed` is set before `close_internal()`, a re-entrant close can never
run the else-branch cleanup. Those tunnels silently drop buffers out of the pool.

Also `SSLUtil.java:77-87` vs `:142-151` — the "BUFFER_OVERFLOW unreachable" invariant holds only if
every callback fully drains `inAppData` during `accept()`; that contract is written nowhere, and the
`callback == null` case at `:147` cannot drain at all. Belongs where `BaseSessionCallback.accept`
implementors will see it.

### 3.3 SSL — flag only, do not patch without explicit ask

- **[verified] close_notify generated but never written** — `SSLUtil.java:405-407`. After
  `closeOutbound()`, `wrap()` returns `Status.CLOSED`, not `OK`; only the `OK` branch (`:400`) drains
  `outSSLNetData` to the channel, while `CLOSED` just calls `config.close()` (already a re-entrant
  no-op). Every graceful teardown and every fatal handshake alert drops its final TLS record — peers
  see a bare FIN/RST, strict clients report truncation, handshake-failure alerts never reach the
  client.
- `SSLNIOSocketHandler.java:224` builds client `SSLContextInfo` from an already-resolved
  `getRemoteAddress()`, so `getHostName()` triggers blocking reverse DNS on a worker or yields an IP
  literal → wrong/absent SNI on that path.
- `SSLUtil.java:212-217` `_finished` closes the callback **before** delivering `exception()` to it,
  so the handler runs against torn-down state.
- `SSLSessionConfig.java:96-106` `close()` loop has no guaranteed progress: the `_needWrap`/
  `_needUnwrap` exception paths call `config.close()` (re-entrant no-op) and only the `BUFFER_*`
  branch sets `forcedClose`.
- `SSLUtil.java:160-161` `_notHandshaking` loop condition tests free space, not remaining ciphertext
  (buffer is in write mode after compact) — comment is wrong; termination depends on
  `BUFFER_UNDERFLOW`. Misleading to anyone "optimizing" that path.
- `CustomSSLStateMachine.java:43-48` does not register `NEED_UNWRAP_AGAIN` (`SSLHandshakingState:34`
  does) — unregistered keys are silent no-ops in `MonoStateMachine.publish`. DTLS-only today, but the
  two machines are meant to mirror each other.
- `CustomSSLStateMachine.java:124` prints profanity to stdout for any unrecognized lookup type —
  including `SSL_CONNECTION_COUNT`, which the parallel `SSLStateMachine.lookupType:194-195` supports
  and this one does not.
- `SSLNIOSocketHandler.java:324-327` `upgradeToTLS()` returns `sslConfig != null` with no side
  effects — claims success without doing anything.
- `SSLUtil.java:482-484` `_sslWrite` throws `SSLException` on any transient non-`NOT_HANDSHAKING`
  status (TLS 1.3 KeyUpdate, TLS 1.2 peer-initiated renegotiation), and
  `CommonChannelOutputStream` callers close the stream on it — an established connection dies on a
  legal mid-connection protocol event.

### 3.4 Core NIO / factories

- **[verified] `CloseableTypeDelegate.close():39-41` returns early on a null delegate without
  setting `isClosed`.** Any `BaseSessionCallback` subclass that forgets `setDelegate` gets a
  silent-no-op `close()` and an `isClosed()` that is permanently false, so reapers keyed on it never
  collect. This is what made `TCPSMCallback.close()` a no-op before the delegate was wired (§1).
  Consider making the null-delegate case still flip the flag.
- Factories swallow callback-construction failure and return half-built handlers —
  `NIOSocketHandlerFactory.java:33-44`, `SSLNIOSocketHandlerFactory.java:47-58`. A config typo
  (missing no-arg constructor, bad `session_callback` class name) yields a server that accepts and
  instantly NPEs every connection, one stack trace each, appearing "up". Should fail fast.
- `NIOSocket.java:283` + `NIOChannelMonitor.java:33-43` — connect-timeout monitor and OP_CONNECT
  completion can both deliver `exception()` for the same connection; the `synchronized(key)` in
  `finishConnecting` gives no exclusion because the monitor never synchronizes on the key.
- `NIOSocket.java:533` — `ServerSocketChannel.accept()` may legally return null on a non-blocking
  channel; `:537` then NPEs on `sc.setOption(...)`.
- `NIOSocketHandler.java:113-118` — `setupConnection` registers OP_READ **before** wiring
  `setProtocolHandler`/`setRemoteAddress`, so with a `complexSetup` factory a fast client can drive
  `connected()` on another worker while those are still null.
- `NIOSocket.java:271-272` — DNS resolver result discarded (`resolveIPAddress` called, original `sa`
  still used); `sa.getHostName()` can trigger blocking reverse DNS on the caller thread. Misleading
  dead code unless the resolver has global side effects.
- `NIOSocket.java:696-712` — if `executor.execute` throws after `interestOps(0)` at `:693`, nothing
  restores interest or closes the channel: that connection is permanently deaf but open. Reachable
  only if the dispatch layer rejects rather than blocks.
- `NIOSocket.java:276-281` — `SocketChannel.open()` is outside the try; a throw from `setOption` /
  `configureBlocking` / `cc.setChannel` leaks the fd.
- `NIOSocket.java:293-299` — connect-initiation failure notifies the caller twice (`exception()`
  **and** rethrow).
- `NIOSocket.java:947-957` — `close()` iterates the live `selectorController.keys()` while the
  selector thread removes cancelled keys; `Selector.keys()` is not thread-safe →
  `ConcurrentModificationException` can abort the loop and leak the remaining channels
  (`selector.close()` cancels keys but does not close channels).
- `IPInfo.java:168-179` — `getLinuxRouter` returns `data[1]` of the first 8-column line matching the
  interface without checking that the destination is `0.0.0.0`, so a directly-connected route listed
  first yields gateway `0.0.0.0`. `:79-150` all `case V6:` branches are unreachable (constructor
  throws for V6); `:212-214` NPEs when address/mask/gateway is null.
- `NetUtil.java:441-447` — dangling `else` binds to `if (log.isEnabled())`, not `if (ret)`, making
  the "ping timed out" branch unreachable. `:329-346` `getNetworkIPV4(InterfaceAddress)` uses a
  48-bit mask constant for 128-bit addresses and computes a negative shift for prefixes > 48
  (masked mod 64) → garbage netmask for any IPv6 input.
- `NetworkTunnel.java:40`, `:76`, `:106` — `closedStat` flags are non-volatile but read/written
  across relay threads.
- `TCPSMCallback` (old revision) / `TCPSessionCallback` contrast: the latter correctly sets its close
  delegate in every constructor.
- `NIOConfig.java:225` inner retry catch prints `e` instead of `e1`, hiding the retry's real failure;
  `:234` logs "Adding Incoming rule" inside the **outgoing** loop; `:195-198` SSL context init
  failures are swallowed so a TLS listener can be silently absent at runtime.
- `EchoProtocol.java:27`, `:74` — `ubaos.byteAt(size()-1)` throws on an empty accumulator (reachable
  if a zero-remaining buffer is ever delivered, e.g. an SSL unwrap yielding 0 app bytes); only
  `IOException` is caught. It is the reference protocol implementation, so the pattern gets copied.
- `UDPSessionCallback.java:96-128` — on exception `clientAddr` keeps its previous non-null value, so
  the read loop re-enters immediately; a persistent `receive` failure on an open channel spins.
- `InetFilterRulesManager` non-security items: `:96-109` `getNetworkBytes()` swallows
  `UnknownHostException` leaving `networkBytes` null → later NPE escapes the `IOException` catch and
  makes all subsequent rules unreachable, and the failed DNS lookup is retried on every call while
  holding the manager's monitor (slow DNS stalls accepts); `:86-92` `setInetFilterDAO()` does not
  invalidate cached `networkBytes`/`maskBytes`; `:145-152` `compareTo` violates the `Comparable`
  contract (returns -1 for any distinct object with equal status) and is dead code since the list is
  never sorted; `:249` loopback is unconditionally ALLOW before rules are consulted, so an explicit
  `127.0.0.1 DENY` can never take effect (confirm intent); `:265-267` unreachable "ip v6 deny"
  fallback; `:334` `getAll()` reads the list unsynchronized while mutators hold `this`.
- `ProtocolHandler.java:85-92` — `this` escapes to the scheduler via `PHTimeout` inside the base
  constructor, before subclass fields are assigned (only reachable with a very small custom timeout,
  not the 2.5-minute default).
- `ProtocolFactoryBase.java:63` — `setOutgoingInetFilterRulesManager(InetFilterRulesManager
  incomingIFRM)` misnamed parameter (cosmetic).

---

## 4. Deferred findings — `org.zoxweb.server.http` (spotted in passing, 2026-08-05/06)

1. **`OkHTTPCall.createOkHttpBuilder` calls `builder.setProxy$okhttp(...)`** — `OkHTTPCall.java:214`.
   That is Kotlin's internal name-mangled setter, not public API; the public equivalent is
   `builder.proxy(java.net.Proxy)`. Works against okhttp-jvm 5.4.0 but is not a stable contract —
   any OkHttp upgrade can remove or rename it and the proxy path stops compiling (or worse, resolves
   differently). One-line fix when convenient.

2. **`ParamUtil` silently ignores unrecognized flags** — bit during benchmarking: `-user1`/
   `-password1` (typo for `-user`/`-password`) parsed as unknown keys, so `HTTPCallTool` ran the
   "authenticated" benchmark unauthenticated with no warning; the mistake was only visible by
   diffing the HMCI dump. Consider a warn-on-unconsumed-args pass in tools like `HTTPCallTool`, or
   an optional strict mode in `ParamUtil`.

3. **`HTTPCallTool` phase asymmetry is measurement overhead, not server behavior** — the ENDPOINT
   phase (`HTTPAPIEndPoint.asyncCall` → task-processor dispatch) consistently reports 25–45% lower
   throughput than the RAW phase (direct `OkHTTPCall.send` on pool threads) against the identical
   server. When quoting numbers, use RAW for the server, ENDPOINT for the client-framework path —
   don't average them.

---

## 5. Benchmark baseline — 2026-08-05/06, dbs.xlogistx.io

Recorded so future perf work has a reference point. Setup: server = 8-core ARM SBC, 32 GB RAM,
1 GbE LAN (~15% link utilization at peak — CPU-bound, not network-bound); client = HTTPCallTool,
88 threads, single OkHttp JVM on the dev workstation; **every request passes Shiro authn/authz**;
dynamic responses generated per request via `GSONUtil` (Gson); `Keep-Alive: timeout=5, max=99`.
Zero failed requests in any run (largest: 500K).

| Workload | Plain HTTP (RAW) | TLS-terminated (RAW) |
|---|---|---|
| Dynamic JSON (`/timestamp`) | 43.1K req/s | 25.4K req/s |
| Static JSON (`/start-date`) | 39.6–44.3K req/s | 26.6–27.0K req/s |
| Static + Basic-auth credentials per request | 42.1K req/s | — |

Findings worth keeping:

- **Keep-alive `max` dominates TLS throughput**: at `max=29` TLS collapsed to ~7.6K req/s
  (handshake-bound, full handshake every 29 requests); raising to `max=99` recovered 25–27K.
  Each full handshake costs ~29 ms idle, rising to 200+ ms under 88-thread load (server-side
  queueing). If a benchmark/trusted-client profile ever matters, a larger `max` is the lever.
- **Dynamic-vs-static delta is only 3–6%** → Gson + Shiro per-request cost is sub-microsecond
  amortized; ceiling is HTTP framing + syscalls, and possibly the load generator itself. True
  server ceiling needs a raw-socket client (wrk/bombardier) from a second machine — still untested.
- **Authenticated ≈ anonymous throughput** (42.1K vs 39.6–44.3K): Shiro subject/credential caching
  is doing its job; if it ever re-hashed per request the number would be hundreds/s, so treat that
  cache as load-bearing.
- **Environment trap: Avast Web Shield on the dev workstation caps plain port-80 HTTP at
  ~1000–1250 req/s** (scans plaintext; tunnels Java TLS untouched) — this masqueraded as
  "HTTPS is 4× faster than HTTP" until isolated. Any benchmark from this machine needs Avast
  disabled or the target host/subnet excluded. Verified 2026-08-05: disabling it took HTTP from
  1246/s to 15.2K/s (10K-request run; later 500K runs reached the 39–44K band above).
- Comparison caveat for outside numbers: published nginx/Go/Rust figures are bare routers with no
  security filter and mostly static responses; like-for-like, add a 20–50% security-middleware
  penalty to their side before comparing.

---

## 6. Closed — confirmed intentional, do not re-raise

- **`ByteBufferUtil.write` / `smartWrite` looping until the buffer is drained is correct by design.**
  A write is contractually complete one-shot: the call does not return until every byte has reached
  the channel. Not a busy-spin defect. A slow/zero-window peer occupying its worker until the session
  timeout is accepted behavior (Rule 2: no babysitting callers; backpressure lives at the dispatch
  layer).
- **`OP_WRITE` is never used — anywhere, existing or new code.** Selection interest is read-only:
  `OP_READ`, `OP_ACCEPT`, `OP_CONNECT`. A callback's `interestOps()` returning a constant `OP_READ`
  is final and correct. Never propose write-readiness registration, write-readiness events,
  partial-write bookkeeping, pending-write queues, yields, or sleeps.
- Workers catching `Throwable` to keep the app alive (Rule 1: the app must survive).
- No hung-task protection / per-caller timeouts (Rule 2).
- Backpressure at the `TaskUtil` dispatch layer (16–128 workers, ~2000-deep queue, 1500–2000
  hysteresis), not per-session.
- TLS handshake serialized per session on one worker; `_needTask` does not block the selector.
- `ByteBufferUtil` allocate/cache pooling is load-proven (GC thrash without it) — never remove
  recaching or propose copy-per-dispatch.
- `APIDataStore` no-op `begin/end/abortTransaction` defaults are fallbacks only; real datastores
  implement them. Do not flag transactional delete-then-insert as unsafe.
- `HTTPRequestAttributes` is slated for removal — do not invest in hardening it.
