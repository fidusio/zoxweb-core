# META-SM-PROTO-DESIGN — the Composable Protocol Validator

**Package:** `org.zoxweb.server.net.common.sm` · **Updated:** 2026-08-15
**Status:** **v2 implemented** (steps 1–6 of §13 done 2026-08-15, uncommitted, 16 test classes /
92 tests green): the state catalog (`assembler`/`controller`/`responder`/`validator`/`ssl`) is
live, the phase SPI (`ConnectionPhase`, `SSHBannerPhase`, `DataExchangePhase`, `SSLClientPhase`)
is **deleted**, the factory composes machines from JSON (explicit `states` + sugar), and the
READY gate is the controller's completion rule.

---

## 0. Purpose & scope (read first)

This subsystem is a **client-side service-conformance checker** for network endpoints the operator
runs or is authorized to check. It answers one operational question: *does the service at this
address still speak the protocol it is supposed to speak, and is its TLS configuration what we
expect?* It is the config-driven equivalent of running `dig`, `openssl s_client`, or a scripted
SMTP health check — the tooling behind uptime monitoring, deployment smoke tests, TLS-expiry and
cipher-posture checks, and protocol regression tests in CI.

**What a session does, in full:** open **one** connection to **one** caller-supplied address, run
a short scripted request/response dialogue declared in JSON, compare the reply against expected
values, record a pass/fail verdict, and close. That is the entire capability surface.

**Structural limits (by design, not by policy):**
- **No discovery.** The caller supplies a single `InetSocketAddress`. There is no host
  enumeration, no port ranging, no sweeping or parallel-target machinery anywhere in the package —
  the config cannot even name a host (Rule 8).
- **No branching, parsing, or computation.** The `exchange` script is deliberately linear: it can
  send a constant and check that a reply contains an expected sequence, nothing more (§8). It
  cannot read a value out of a response and act on it, which is what keeps it a conformance
  checker rather than a general protocol client. Anything richer is explicitly out of scope and
  requires purpose-written code.
- **No credential or authentication logic**, no retry/backoff loops, no rate manipulation, no
  traffic obfuscation. Sessions are single-shot and identify themselves normally.

**The security-relevant behavior in this document is protective, and defaults are the safe ones:**
certificate-chain validation *and* hostname verification are **on by default**
(`cert_validation: true`); disabling them exists for internal endpoints using self-signed
certificates and for reading what a TLS endpoint negotiates, and doing so is called out as unsafe
wherever it appears. The "STARTTLS injection" checks (§8, §12) are a **client-side defense** — the
machine refuses to upgrade if unexpected plaintext arrived before the TLS handshake, which is the
documented mitigation for that vulnerability class, not an attempt to exercise it. Buffer caps
(`max_message`, accumulation limits) exist so a hostile or broken peer cannot exhaust memory.

Terminology note: this document uses "probe" for *the single scripted request a checker sends*,
and "fingerprint" for *identifying which protocol/TLS parameters an endpoint reports* — both in
the ordinary monitoring sense, applied to endpoints the operator is entitled to query.

---

**This is the single authoritative document for this subsystem.** It carries the operational
premise, the rules that must never be violated, the current implementation baseline, and the v2
target design. Read §2 (Iron Rules) **first** — every rule exists because it was violated during
development and had to be corrected by the maintainer (§15 logs each correction).

**Resuming work in a fresh session?** Read §2 (Iron Rules), then **§17 — the class-by-class
inventory of what exists on disk today and the uncommitted working state**. §1–§4 describe the
implemented v1 behavior; §5–§13 are the v2 target that is designed but **not built**.

---

## 1. Operational premise (maintainer-defined, authoritative)

The subsystem is a **network protocol validator** built on a configurable per-connection state
machine.

- `UDPSMCallback` / `TCPSMCallback` are **configured by the composed state machine**. They run
  inside the JVM; every interaction with the remote service goes through **NIOSocket**.
- NIOSocket triggers the callback in exactly two ways:
  1. remote connection established → `connected(SelectionKey)` → **`CONNECTED`**;
  2. data arrival while the callback waits → `accept(data)` → **`RAW_IN_DATA`**.
- `RAW_IN_DATA` payloads are **partial or complete**, and may be **plain, handshake, or
  encrypted** bytes.
- The machine — not any helper, not any caller — performs the entire session: react to
  `CONNECTED`, assemble incoming data into protocol messages, decide and send responses,
  validate the protocol, and end its own life.
- End of life (remote disconnect, or validation finished — success **or** failure): close the
  channel, clean up resources, and **push the report to the machine's report listener**.
- **The state/consumer vocabulary is open-ended**: the machine is composed by JSON config —
  states and TriggerConsumers can be added or modified later without changing the assembly
  machinery.

**The complete usage contract — nothing else is involved:**

1. Build a `ClientConSM` from a JSON protocol definition (`ClientSMFactory.fromJSON`) or
   programmatically (`ClientConSMBuilder`).
2. Mint the session callback from the machine (`sm.newSessionCallback()` for TCP,
   `sm.newSessionCallback(remote)` for UDP) and hand endpoint + callback to NIOSocket:
   `addClientSocket(InetSocketAddress, TCPSMCallback, timeout, resolver)` or
   `addDatagramSocket(InetSocketAddress localBind, UDPSMCallback)`.
3. NIOSocket invokes `connected(SelectionKey)` — the single kickoff for both transports.
4. The machine runs the whole session and closes itself.
5. The caller reads the **validation report** from the machine's results bag
   (`SMProtoUtil.results(sm)` + `Params.EXCEPTION`), pushed on `MACHINE_CLOSED`.

JSON definition + address in → autonomous machine run → validation report out.

### The canonical pipeline

```
NIOSocket ─ connected(SK) ──► CONNECTED state                (mandatory #1)
NIOSocket ─ accept(data) ───► RAW_IN_DATA / waiting state    (mandatory #2)
        data: partial|complete · plain|handshake|encrypted
                     │
                     │  (handshake/encrypted → TLS record assembly = the SSL states,
                     │   decrypted output re-enters toward the protocol assembler)
                     ▼
          ┌─ MessageAssembler state ─────────────────────────┐
          │ protocol-specific; accumulates in a pooled       │
          │ UByteArrayOutputStream; parse position tracked   │
          │ in ITS STATE BAG across dispatches               │
          │   partial  ⇒ wait (return, resume next dispatch) │
          │   complete ⇒ publish IN_MESSAGE                  │
          └───────────────────────┬──────────────────────────┘
                                  ▼ IN_MESSAGE
          ┌─ ProtocolController state ───────────────────────┐
          │ driven by its META CONFIGURATION; decides what   │
          │ to invoke next, by publishing events:            │
          │   ├─ OUT_MESSAGE ─► ResponseController state     │
          │   ├─ VALIDATE ────► ProtocolTypeValidator state  │
          │   ├─ START_TLS ───► SSL upgrade (unchanged)      │
          │   └─ … (future states, config-inserted)          │
          └───────────────────────┬──────────────────────────┘
                                  ▼
          CLOSED state                                       (mandatory #3)
          – remote disconnect (TCP), fatal error, or machine
            end-of-life (validation passed OR failed)
          – channel closed + resources cleaned (callback
            teardown delegate)
          – report pushed to the REPORT LISTENER
```

---

## 2. Iron Rules (each was violated during development — do not repeat)

1. **The machine dictates the entire session.** Post-`connected(SK)`: send per configuration,
   wait for NIOSocket triggers, validate the response, **and close** — all machine actions.
   Nothing external drives any part of it.
2. **`connected(SelectionKey)` is the only kickoff.** `setChannel(Channel)` is channel setup only
   (bind, connect, teardown registration) and publishes **no events**. Nothing happens on the
   machine before `connected()` is invoked. This holds identically for both callbacks — NIOSocket
   registers the datagram channel with **zero** interest ops, invokes `connected(sk)`, then
   installs the returned ops, so `CONNECTED` always precedes any read dispatch.
3. **Helpers and tests are pure observers.** `ProtoConnect`, JUnit tests, any application:
   register consumers/listeners, await completion, read the report. They never send, never close,
   never orchestrate. Tests drive `NIOSocket` + callback **directly** — never through a helper's
   run method.
4. **Results accumulate in the machine's properties** — a `NamedValue<NVGenericMap>` under
   `SMProtoUtil.RESULTS`, reachable by every state and TriggerConsumer via
   `getStateMachine().getProperties()`. Never accumulate outcomes in caller-side
   `AtomicReference`s or ad-hoc observer states.
5. **Use the framework's existing facilities.** Completion is already signaled by
   `StateMachine.close()` firing `MACHINE_CLOSED` to `StateMachineListener`s, and queryable via
   `isClosed()`; the codebase wait idiom is `TaskUtil.waitIfBusy(millis, exitCondition)`. Do not
   hand-roll `CountDownLatch` + observer-`State` contraptions.
6. **One state machine per session callback**, enforced fail-fast (the `AUTO_CLOSEABLE` marker in
   machine properties). The machine is session-owned; teardown order is sacred: close
   `AUTO_CLOSEABLE` resources → recache read buffer → publish `CLOSED` (payload = stashed
   `Params.EXCEPTION` or null) → **close the machine last** (a closed machine rejects publishes).
7. **The transport router owns the wire event exclusively.** `RAW_IN_DATA` (TCP) / `DATAGRAM`
   (UDP) are consumed only by the router state; phases/catalog states consume `IN_DATA`. Every
   buffer is a detached pooled copy with exactly **one active owner**, who recaches it via
   `ByteBufferUtil.cache` when done. Double-cache = pool corruption.
8. **The config describes a protocol, never an endpoint.** No host in the JSON; an optional
   top-level `port` is a default-port hint only. TLS SNI/hostname verification bind to the address
   the caller dialed (deferred `SSLContextInfo` at upgrade time).
9. **Layer separation is strict.** The `${name}` mechanism is protocol *value* injection, confined
   to the data-literal body after the `txt:`/`hex:`/`base64:` encoding prefix. The encoding prefix
   is byte-conversion syntax — the two layers never mix (a resolved variable value can never
   become an encoding directive; prefix-position placeholders are a non-concept). New
   encoders/decoders/common utilities go in `SMProtoUtil` — no new helper classes.
10. **Zero changes to `net.ssl`.** The machine orchestrates the handshake (SSLStateMachineV2) by
    routing `HandshakeStatus` publishes to the load-proven `SSLUtil` handlers. The engine steps
    are untouchable. `ClientSSLHelper.close()` is a no-op **by contract** (closing the machine
    there would silently lose `CLOSED`).
11. **Client-side only.** No server-mode states, no server-side upgrade counterpart until
    explicitly requested.
12. **Compose first, bind last.** Property-bag injection/sharing happens during assembly; once a
    session callback binds a machine, the bag is live session state (teardown registry, exception
    relay, report).

---

## 3. The FSM engine (`org.zoxweb.server.fsm`) — how execution actually works

- **Structure.** A machine holds: a `config` object `C` (here `ClientSessionContext`, reached via
  `getStateMachine().getConfig()`); a machine-wide properties `NVGenericMap` inherited from
  `NVGMProperties` (`AUTO_CLOSEABLE`, `EXCEPTION`, the `results` bag) — injectable/shareable for
  composition; registered `State`s (each with its **own** `NVGMProperties` bag, shared among that
  state's consumers); and a dispatch map **canonical ID → ordered set of `TriggerConsumer`s**.
  The scheduler is a plain `java.util.concurrent.ScheduledExecutorService` (zoxweb
  `TaskSchedulerProcessor` implements it), used only by the async `publish()` path.
- **Publish = synchronous broadcast.** `publishSync(canID, payload)` runs each registered consumer
  **inline on the calling thread**, in **registration order**. No queue, no thread hop
  (`ClientConSM` is built with `(Executor) null`); a consumer may itself `publishSync` — recursion
  is the design (the TLS handshake flight is recursive inline publication). NIOSocket provides the
  external serialization: per-session dispatches never overlap (interest ops zeroed during
  processing).
- **Registration order is behavior.** Transport router first, catalog states in declared order,
  application states last. A consumer registered *during* a dispatch is not in the in-flight
  snapshot — it sees the next publish. That late-registration idiom is load-bearing: it is how
  applications avoid receiving pre-`READY` negotiation bytes.
- **Payload typing is unchecked.** `TriggerConsumer<T>` casts at dispatch; a mismatch is a runtime
  `ClassCastException` inside the broadcast. `CONNECTED` carries a `SelectionKey` over TCP but an
  `InetSocketAddress` over UDP, so **any consumer of a transport-varying event must be
  payload-agnostic** (`TriggerConsumer<Object>`). This was a live bug.
- **Never `start()`ed.** Machines are driven purely by callback publishes + internal
  republication; states are consumer namespaces, not a transition walk.
- **Closed machine.** `publishSync` on a closed machine throws `IllegalStateException` — hence the
  sacred teardown order and the machine-closed guards before post-completion republishes.
  `close()` is once-only and fires `MACHINE_CLOSED` to `StateMachineListener`s — the native
  completion signal.

### Composition — shared properties at two scopes

Both `StateMachine` and `State` extend `shared.util.NVGMProperties` (public `getProperties()` /
`setProperties(NVGenericMap)`). This is a **deliberate composition mechanism** — machines are
composable and states are machine-agnostic building blocks. The public `setProperties` is the
injection point, not a hazard.

| Scope | Bag | Shared among | Holds |
|---|---|---|---|
| Machine | `sm.getProperties()` (`"sm-properties"`) | every state + TriggerConsumer, and the caller | session contract: `AUTO_CLOSEABLE`, `EXCEPTION`, the `results` report, cross-state context |
| State | `state.getProperties()` (`"state-properties"`) | that state's own TriggerConsumers | working memory: accumulation, cursors, flags |

**TriggerConsumers coordinate through the bags — that is how a consumer knows what to do next.**
On each dispatch a consumer consults its state bag (and the machine bag) and decides: **fire a
follow-up event** (`publishSync`), **wait for more data** (stash the partial input in the bag and
return — the next dispatch resumes from it), complete, or fail the session. Because the memory
lives in the bag rather than instance fields, the same state class composes into any machine
unchanged. A composite can inject **one shared bag** into several machines/states, giving every
consumer a single blackboard with zero extra plumbing. Since the scheduler is a
`ScheduledExecutorService`, composed machines can share one scheduler or run under a host's.

---

## 4. Transport bridges — the two callbacks (v1, unchanged by v2)

Both are the **transport bridge and nothing more**: they convert socket readiness into machine
publishes and own the one-time teardown. Structurally identical by design (`UDPSMCallback`
deliberately does *not* extend `UDPSessionCallback`).

**Construction (both).** Null checks → fail-fast if the machine already carries the
`AUTO_CLOSEABLE` marker (one machine per callback) → `setConfig(machine)` → register the
`AUTO_CLOSEABLE` collection in machine properties → arm the `CloseableTypeDelegate` teardown
lambda (once-only; its flag flips **before** the lambda runs, which the read loops rely on).

**`TCPSMCallback` (stream)**
- `setChannel(Channel)` — bind `SocketChannel`, register for teardown, resolve remote address.
- `connected(SelectionKey)` — kickoff: `setChannel`, create the session output stream
  (`CommonChannelOutputStream`, teardown-registered), `publishSync(CONNECTED, key)`, return
  `OP_READ`.
- `accept(SelectionKey)` — read loop under `readLock`: loop-top `isClosed()` guard, clear the
  pooled 16K `rawReadBuffer`, `read`, flip, mint a **detached pooled HEAP copy**,
  `publishSync(RAW_IN_DATA, copy)`; EOF (`-1`) or `IOException` → close the session. Writes go
  through `ClientSessionContext.write` → `bcos` (encrypts after `sslHandshakeSuccessful`).
- `exception(Throwable)` — stash under `Params.EXCEPTION` (if still open) → close; the delegate
  republishes it as the `CLOSED` payload.

**`UDPSMCallback` (connected datagram client)**
- Constructed with the peer address, stored in the inherited `SessionCallback.remoteAddress`
  (`setRemoteAddress` in the constructor, read back via `getRemoteAddress()`); the bind address is
  the caller's (`addDatagramSocket(localBind, cb)`, ephemeral `new InetSocketAddress(0)`).
- `setChannel(Channel)` — **silent setup only**: bind, teardown-register,
  `connect(getRemoteAddress())` (guarded by `isConnected()`; failure routes through `exception()`
  then rethrows).
- `connected(SelectionKey)` — kickoff: idempotent `setChannel`, once-only CAS
  `publishSync(CONNECTED, getRemoteAddress())`, return `OP_READ`. NIOSocket registers with **zero ops** and
  installs the returned ops only after this completes, so the machine's `CONNECTED` actions (first
  datagram) always precede any read dispatch; an early reply waits in the OS buffer.
- `accept(SelectionKey)` — receive loop under `readLock`, same loop-top guard: `receive` → flip →
  detached copy → `publishSync(DATAGRAM, DataPacket)`; drain until empty. Any receive
  `IOException` — including ICMP port-unreachable, which a *connected* channel surfaces — is
  **fatal for a client**: `exception(e)` → `CLOSED` with cause (the fast "nothing listening"
  verdict). Stray-source datagrams are dropped by the OS.
- No TLS: `sslHandshakeSuccessful` throws `UnsupportedOperationException` (no DTLS in this stack).

**Teardown (identical, the delegate lambda):** close every `AUTO_CLOSEABLE` in registration order
(channel first — so in-flight reads exit; SSL session state after — so its drain sees a dead
channel) → recache the raw read buffer under `readLock` → publish `CLOSED` with the stashed
`Params.EXCEPTION` or null → `machine.close()` **last**.

**NIOSocket seam:** `addDatagramSocket(InetSocketAddress, ConnectionCallback<?>)` — bind →
`setChannel` → register **0 ops** → `connected(sk)` → install returned ops. The legacy
`UDPSessionCallback` overload delegates to it.

---

## 5. The states

### Mandatory lifecycle states (always registered by the runtime, never by config)

| State | Role |
|---|---|
| **CONNECTED** | Transport initialization + the machine's kickoff actions. Entered via `connected(SelectionKey)` — the only entry point. Payload-agnostic. |
| **Waiting / RAW_IN_DATA** | The data-routing state (v1 transport router, kept). Plain bytes → `IN_DATA` toward the assembler; handshake/encrypted bytes → the SSL states (the TLS assembler), whose decrypted output re-enters as `IN_DATA`. Owns `RAW_IN_DATA`/`DATAGRAM` exclusively. |
| **CLOSED** | End-of-life: remote disconnect, fatal error, or the run completing (validation pass or fail). **Physical cleanup stays in the callback teardown delegate.** The machine-side job is finalizing the report; the push itself is `StateMachine.close()` firing `MACHINE_CLOSED` (§10). |

### Catalog states (JSON-composed building blocks; open vocabulary)

Registered by config in declared order between the mandatory states. Each carries its JSON
`config` block seeded into **its own state bag** at assembly; consumers read behavior from the bag.
New catalog states are added by registering a builder — no change to the assembly machinery.

| State (catalog name) | Consumes | Publishes | Working memory (state bag) |
|---|---|---|---|
| **`assembler`** (MessageAssembler) | `IN_DATA` | `IN_MESSAGE` | pooled `UByteArrayOutputStream`, parse/consumed offset, skip counters |
| **`controller`** (ProtocolController) | `IN_MESSAGE`, `SECURE`, `CONNECTED` | `OUT_MESSAGE`, `VALIDATE`, `START_TLS` | step cursor, waiting-for-secure flag |
| **`responder`** (ResponseController) | `OUT_MESSAGE` | — (writes via `ClientSessionContext.write`) | — (stateless) |
| **`validator`** (ProtocolTypeValidator) | `VALIDATE` | — (verdict → results bag; mismatch → `ctx.fail`) | — (match meta from its bag) |

The SSL states, the transport router and the TLS phase remain as in v1 — already building blocks;
the composition work does not touch `net.ssl` (Rule 10).

---

## 6. Event vocabulary (the composition seams)

Bare enum-name canonical IDs. Convention: **facts are nouns on the inbound data ladder; commands
are imperatives.**

```
RAW_IN_DATA → (router) → IN_DATA → (assembler) → IN_MESSAGE → (controller)
(controller) → OUT_MESSAGE → (responder) → session write
(controller) → VALIDATE    → (validator) → verdict into the results bag   (no event back)
(controller) → START_TLS   → (SSL control) → upgrade                       (unchanged)
```

| Event | Status | Payload | Publisher → Consumer |
|---|---|---|---|
| `IN_MESSAGE` | **new** | one complete framed message | assembler → controller |
| `OUT_MESSAGE` | **new** | message to transmit (decoded bytes) | controller → responder |
| `VALIDATE` | **new** | the current message + validate meta | controller → validator |
| `IN_DATA` | kept | application bytes (plain or post-decrypt) | router / SSL bridge → assembler |
| `SECURE` | kept | `SSLConfigInt` | SSL bridge → controller (deferred send) |
| `READY` | kept | null | READY gate → auto-close / application |
| `START_TLS` | kept | null | controller / SSL AutoStart → SSL control |
| `RAW_IN_DATA` · `DATAGRAM` · `CONNECTED` · `CLOSED` | kept | as v1 | callback → router |
| `NEED_WRAP` · `NEED_UNWRAP` · `NEED_UNWRAP_AGAIN` · `NEED_TASK` · `FINISHED` · `NOT_HANDSHAKING` | kept | `SSLClientBridge`, or null during the config-close drain | `ClientSSLHelper` → SSL states |
| **`BANNER_RECEIVED`** | **RETIRED** | — | removed from `ClientEvent`; applications read `results.banner` |

`VALIDATED` was considered and **rejected**: the validator's only output is the report — no event
round-trip. What happens after a verdict is the controller's meta decision or end-of-life.

---

## 7. MessageAssembler — boundary strategies

Accumulates `IN_DATA` in a pooled `UByteArrayOutputStream`; parse/consumed position in the state
bag; a `max_message` cap (default 64K) whose breach fails the session; completion publishes
`IN_MESSAGE`. The boundary rule is a **named strategy from a small registry** (extensible like the
state catalog — a new framer is one registry entry plus its meta keys).

| `boundary` | Rule | Default for | Meta keys |
|---|---|---|---|
| `datagram` | one datagram = one message; pass-through, no accumulation | **UDP** | — |
| `delimited` | message ends at a terminator (`SMProtoUtil` literal, binary-safe); cross-chunk detection via the accumulation | — | `terminator` (default `txt:\r\n`) |
| `length_prefixed` | a header field carries the payload length | — | `length: {offset, size (1/2/4), endian (default big), adjust}` |
| `stream` | emit the accumulation-so-far as the current message; consumed offset advances when the controller consumes through a match — **byte-for-byte the v1 `expect` contains-match semantics** | **TCP** | — |

Defaults by transport mean most configs write only `"boundary"`, or nothing. The SSH
identification line's CR-tolerance quirks become `delimited` configuration, not a bespoke phase.

---

## 8. ProtocolController — meta grammar

Meta is a **linear step list** (JSON key stays `exchange` — v1 configs migrate unchanged). Cursor
and skip counts live in the controller's state bag. **Hard limit preserved: a linear script — no
branching, no field parsing, no computing a value from a reply. A richer controller is a future
catalog state, not a growth of this grammar.**

| Op | Controller action | Semantics |
|---|---|---|
| `{"send": "<literal>"}` | publish `OUT_MESSAGE` (decoded bytes) | the responder writes to the session; `${vars}` resolved at send time |
| `{"expect": "<literal>"}` | wait on `IN_MESSAGE` | per-message contains-match. Framed boundaries: a non-matching complete message is skipped, bounded by `max_message` (the SMTP `250-` continuation idiom). `stream`: snapshot + consume-through-match (v1 semantics) |
| `{"validate": {…}}` | publish `VALIDATE` (meta + current message) | the validator matches and writes the verdict; a mismatch fails the session |
| `{"start_tls": true}` | publish `START_TLS` | unchanged. The client-side safety check becomes "assembler accumulation must be empty" — if any unexpected plaintext arrived before the upgrade the session is failed rather than continued (the standard mitigation for the STARTTLS command-injection vulnerability; never clear-and-continue) |

**`validate` meta** (generalizes the retired banner phase's match vocabulary):

```json
{ "validate": { "prefix": "txt:SSH-2.0-", "contains": "txt:OpenSSH",
                "exact": null, "report": "banner" } }
```

- `prefix` / `contains` / `exact` — any combination; `SMProtoUtil` literals; `${vars}` allowed.
- `report` — optional key: store the matched message text in the results bag under it.
- Verdict: `validated=true`, or `validated=false` + `reason`. **The report is complete on both the
  pass and the fail path.**

**Completion rule:** the step list finishing = pipeline done → `READY` → `close_on_ready` →
teardown → `MACHINE_CLOSED` report push. **A run that completes with no `validate` step records
`validated=true, reason="script completed"`** — every run yields a verdict.

---

## 9. ProtocolTypeValidator — the verdict

Consumes `VALIDATE`; applies the match to the message; writes into `SMProtoUtil.results(sm)`:

| Key | Value |
|---|---|
| `validated` | `true` / `false` |
| `reason` | on failure: the mismatch cause; on no-validate completion: `"script completed"` |
| `<report>` | on success with a `report` key: the matched message text |

A `false` verdict also fails the session (`ctx.fail`), so `CLOSED` carries the cause and the
report agrees with `Params.EXCEPTION`.

---

## 10. The report and its listener

- **Storage:** the machine results bag — `SMProtoUtil.results(sm)`, an `NVGenericMap` in the
  machine's `NVGMProperties`. Written by states during the run:

| Key | Writer | Meaning |
|---|---|---|
| `validated` / `reason` | validator (or completion rule) | the verdict |
| `ready` | READY gate | pipeline completed |
| `banner` / `<report>` | validator `report` key | matched message text |
| `tls_protocol` / `tls_cipher` | `SSLClientBridge` | negotiated TLS session |
| `Params.EXCEPTION` (machine properties, separate) | teardown | failure cause; also the `CLOSED` payload |

- **Push mechanism: the existing `StateMachineListener` / `MACHINE_CLOSED` hook — NO new
  interface.** `StateMachine.close()` fires `MACHINE_CLOSED`, and teardown closes the machine
  **last** (after `CLOSED` publishes), so the results bag and `EXCEPTION` are final when the
  listener runs. A report listener is a `StateMachineListener` registered via `sm.addListener(...)`
  before connect, reading the report from machine properties.
- **`SMProtoUtil.waitForClose(sm, timeout)`** (pull) is the same mechanism: a latch-arming
  `MACHINE_CLOSED` listener. Push and pull are one implementation.
- **Helpers and tests stay pure observers** (Rule 3).

---

## 11. Factory — from protocol switch to state catalog

v1 `ClientSMFactory` hardcodes `protocol: ssh/tls/plain` → a fixed phase set. v2:

- A **state catalog**: name → state builder. Each builder validates and consumes its own `config`
  block and seeds it into that state's bag.
- The JSON **composes**: it declares the catalog states and each one's config; the factory
  registers them in declared order between the mandatory lifecycle states.
- Adding a state type later = one catalog entry; **zero factory-core change**.
- **`protocol: "ssh"` becomes factory sugar**, expanding to `assembler` (`delimited`, CRLF,
  `max_message` 255) + `controller` `[{"validate": {prefix, contains, exact, report: "banner"}}]`.
  The bespoke `SSHBannerPhase` dissolves entirely.
- Existing `exchange` configs keep working (TCP → `stream`, UDP → `datagram`) with byte-identical
  behavior.

`SSHBannerPhase` and `DataExchangePhase` dissolve into catalog states; `ConnectionPhase` is
**deleted outright** (maintainer ruling, 2026-08-15: the phase SPI was a first-implementation
error — the correct design is the meta-driven state catalog; there is no registrar to preserve).
The factory registers catalog states directly, in declared order, between the mandatory lifecycle
states; `READY` gating is the controller's completion rule (§8), not a phase attribute.

---

## 12. JSON schema

**Current (v1, implemented).** Top-level keys read today: `name`, `transport` (`tcp`/`udp`,
default tcp), `protocol` (`plain`/`ssh`/`tls`, default plain), `port` (default-port **hint** only —
never an endpoint), `timeout_sec` (default 5), `close_on_ready` (machine closes itself on `READY`;
default true for udp+exchange, false otherwise), `tls {mode: immediate|on_demand, cert_validation}`
(**`cert_validation` defaults to `true`** = validate the chain against the JVM trust store **and**
verify the certificate identity against the dialed host; set it to `false` only for internal
endpoints with self-signed certificates or when the check is "what does this endpoint negotiate?" —
chain validation without hostname verification would accept a valid certificate issued for a
different host, so the two always move together),
`ssh {banner_prefix, banner_contains, banner_exact, banner_max_line, pre_banner_cap}`, `vars`
(defaults for `${name}` injection), `exchange[]` (`send` / `expect` / `start_tls` steps).

Data literals carry a one-word encoding prefix: `txt:` (UTF-8 verbatim), `hex:` (whitespace
ignored), `base64:`; no recognized prefix = UTF-8 text. Bodies may contain `${name}` placeholders
resolved from `vars` / caller injection (Rule 9). Fail-fast validation at build: unknown
protocol/transport/tls-mode; `udp` with `ssh`/`tls`/a `tls` block; `protocol: tls` with
`on_demand`; `exchange` with `ssh`; `start_tls` without an `on_demand` tls block; malformed
`hex:`/`base64:` literals; unknown exchange ops.

**Target (v2, illustrative — states become explicit):**

```json
{
  "name": "dns-probe",
  "transport": "udp",
  "port": 53,
  "timeout_sec": 3,
  "close_on_ready": true,

  "states": [
    { "state": "assembler",
      "config": { "boundary": "datagram", "max_message": 65536 } },
    { "state": "controller",
      "config": { "exchange": [
        { "send":     "hex:1234 0100 0001 0000 0000 0000 07 6578616d706c65 03 636f6d 00 0001 0001" },
        { "validate": { "contains": "hex:1234", "report": "dns" } }
      ] } }
  ]
}
```

Sugar forms (`protocol: "ssh"`, a bare top-level `exchange`, a `tls` block) expand to the
equivalent `states` list; defaults by transport keep most `config` blocks near empty.

**UDP idioms that matter:** one request/response per probe (UDP gives the script no ordering or
loss recovery — a lost datagram surfaces as timeout, not failure); bake a recognizable token into
the probe and match its echo; the socket is *connected*, so stray sources are dropped and ICMP
port-unreachable fails the session with a cause.

---

## 13. Implementation breakdown (order; behavior-preserving where possible)

1. **Events** — add `IN_MESSAGE`, `OUT_MESSAGE`, `VALIDATE`; remove `BANNER_RECEIVED` from
   `ClientEvent` and its `ProtoConnect` / test references. **DONE 2026-08-15** (also:
   `SMProtoUtil.BasicEvent` merged into `ClientEvent` — one enum is the whole vocabulary — and
   the wire event renamed `IN_RAW_DATA` → `RAW_IN_DATA` to match this document).
2. **Report listener + `waitForClose`** — one `MACHINE_CLOSED`-based utility; migrate helpers and
   tests off hand-rolled latches. **DONE 2026-08-15**: `SMProtoUtil.waitForClose(sm, millis)`
   (latch-arming `MACHINE_CLOSED` listener, already-closed race covered) +
   `SMProtoUtil.closeCause(sm)` (the canonical `EXCEPTION` machine property, both transports);
   `ProtoConnect` and all completion latches in the sm tests migrated.
3. **`assembler` state + boundary registry** (`datagram` / `delimited` / `length_prefixed` /
   `stream`), working memory in the state bag, `IN_MESSAGE` output. **DONE 2026-08-15**
   (`MessageAssemblerState`; the accumulation holder `Assembly` lives in the machine bag under
   `SMProtoUtil.ASSEMBLY` — the blackboard seam for stream consume-through and the residue
   check; `delimited` gained `strip_cr` for the SSH CR-tolerance quirk).
4. **`controller` / `responder` / `validator` states** — the `exchange` grammar re-expressed
   against the message seams; the `validate` verdict → results. **DONE 2026-08-15**
   (`ProtocolControllerState` — working memory in the state bag, `ready_gate` bag flag, framed
   skip bounded by `max_skip`; `ResponseControllerState`; `ProtocolTypeValidatorState` with the
   `Validation` payload carrier).
5. **Factory catalog rework** + `ssh` / `exchange` / `tls` sugar expansion. **DONE 2026-08-15**
   (`ClientSMFactory.CATALOG` + `registerState`; explicit `states` shape parses via
   `NVGenericMapList`; a `validate` step inside a JSON `exchange` survives the `NVPairList`
   mapping via a `GSONUtil.toNVPair` fix — object values carried as JSON text, re-parsed at
   compile; `responder`/`validator` auto-composed with any controller).
6. **Dissolve** `SSHBannerPhase` / `DataExchangePhase`; migrate their tests to the catalog
   states. **DONE 2026-08-15** — deleted outright along with `ConnectionPhase` (§14.10) and
   `SSLClientPhase` (now the bag-configured `SSLClientState`; the builder registers the engine
   states alongside it). `ClientSessionContext.phaseComplete` renamed `gateComplete`; the
   builder composes with `state(State)` and derives the READY gate from `ready_gate` bag flags.
7. **Docs** — fold the v2 schema into this document's §12 as implemented; move the status line
   from DESIGN to IMPLEMENTED as phases land.

Each step keeps the suite green; existing configs must stay byte-identical in behavior through
step 6.

**Roadmap beyond:** health-check definitions for more service types, written as ordinary configs —
SNMP (a standard `sysDescr` GetRequest with the operator's community string) and OpenVPN (the
protocol's own hello, to confirm the service is up and speaking OpenVPN); an SMTP negotiator
(EHLO/STARTTLS/redo-EHLO, RFC 3207) as a coded catalog state for mail-server monitoring. DHCP is
**out of config scope** — it needs broadcast plus values computed from the reply, so it would be a
purpose-written state, not a config. Server-side counterpart ON HOLD (Rule 11).

---

## 14. Settled decisions (maintainer rulings, 2026-08-15/16)

1. **Operational premise** — the machine drives the whole session end to end; the callbacks are
   machine-configured JVM transport bridges triggered by NIOSocket; the report is pushed at
   end-of-life.
2. **Shared bag + `ScheduledExecutorService` are composition features, not risks** — they enable
   composable machines and states-as-building-blocks. Compose first, bind last.
3. **The state/consumer vocabulary is open-ended**, composed by JSON config; the factory becomes a
   state catalog, not a protocol switch.
4. **Event names**: `IN_MESSAGE`, `OUT_MESSAGE`, `VALIDATE`. `VALIDATED` **rejected**. Convention:
   facts = nouns on the data ladder, commands = imperatives.
5. **Report listener**: built on the existing `StateMachineListener` / `MACHINE_CLOSED` hook — no
   new interface; the report read from machine properties (final because the machine closes last).
6. **Assembler boundaries**: `datagram` / `delimited` / `length_prefixed` / `stream` in a named
   extensible registry; defaults by transport (UDP = `datagram`, TCP = `stream`).
7. **Controller grammar**: the linear `exchange` step list preserved; `validate` op added; the hard
   limit (no branch / parse / compute) kept.
8. **Completion-implies-validated**: a run with no `validate` step records
   `validated=true, reason="script completed"`.
9. **`BANNER_RECEIVED` retired**: applications read `results.banner` from the report.
10. **`ConnectionPhase` is deleted, not shrunk** (2026-08-15) — the phase SPI was a
    first-implementation error from an earlier session, never part of the intended design. The
    meta-driven state catalog replaces it entirely: catalog builders register states directly;
    `READY` gating moves to the controller completion rule. v1 is being phased out in favor of
    v2 — do not preserve v1 structures for their own sake.
11. **`remoteAddress` is standardized on `SessionCallback`, and its rough edges stay open**
    (2026-08-16) — the field and its accessors were pulled up from `BaseSessionCallback` so the
    whole hierarchy shares one implementation. Standardization outranks per-class specificity
    here: proposals to re-specialize it (restoring `UDPSMCallback`'s immutable target via an
    overridden setter or a private final field) are **rejected**. The known consequences — the
    lost `final` on the UDP target, and the pre-connect null window on the TCP client path — are
    deliberately left unpatched pending a larger rework; technical detail in `PENDING.md` §3.4.
    Do not "fix" them opportunistically.

*(Rejected along the way: a Phase A/B/C gap-phasing plan — superseded by the premise above.)*

---

## 15. Failure log (why the Iron Rules exist)

| # | Mistake | Correction |
|---|---|---|
| 1 | `UDPSMCallback` extended `UDPSessionCallback`, inheriting its dispatch/recache/executor model | Rewritten as a pure SM bridge on `SessionCallback` + `ConnectionCallback`, mirroring `TCPSMCallback` (Rules 6, 7) |
| 2 | `CONNECTED` published from `setChannel`; `connected(SK)` was dead code the framework never invoked | `setChannel` = silent setup; `connected(SK)` = the only kickoff; NIOSocket datagram path registers 0-ops then invokes it (Rule 2) |
| 3 | `ProtoConnect` closed the UDP session on `READY` — a helper driving the lifecycle | `close_on_ready` machine action; helpers observe only (Rules 1, 3) |
| 4 | Tests routed through `ProtoConnect.run` instead of driving NIOSocket with the machine | Tests hand the callback to NIOSocket directly (Rule 3) |
| 5 | Outcomes captured in test-side `AtomicReference`s; completion via hand-rolled `CountDownLatch` + ad-hoc observer `State` | Results in the machine's properties bag; completion via `MACHINE_CLOSED` / `isClosed` (Rules 4, 5) |
| 6 | `setProperties` / the SES change flagged as risks | They are the composition mechanism (Rule 12, §3) |
| 7 | `ConnectionPhase` — a hand-rolled phase SPI invented by an earlier session; v1 was built on it | The correct design was always the meta-driven state catalog. v2 exists to phase v1 out; `ConnectionPhase` is deleted, not preserved as a registrar (§11, §14.10) |

Same theme, earlier: the `${var}` layer belongs to protocol value injection, never byte conversion
(Rule 9); a connection is either TCP or UDP and nothing runs before `connected()` — don't guard
against off-contract callers (no babysitting).

---

## 16. Build & test

```
mvn compile
mvn test -DskipTests=false -Dtest="org.zoxweb.server.net.common.sm.*Test" -DfailIfNoTests=false
mvn test -DskipTests=false -Dtest="org.zoxweb.server.fsm.*Test" -DfailIfNoTests=false
java -cp target\test-classes;target\classes;<gson>;<uuid-creator> \
     org.zoxweb.server.net.common.sm.DNSProbeTest 8.8.8.8 53   # live check vs a public resolver
```

Tests are skipped by default (`skipTests=true` in the pom) — `-DskipTests=false` is required. GPG
signs at `verify`; use `package`/`test` for development. Local repo:
`D:/dev/data/java/.m2/repository`.

**Current test coverage (v1):** 16 sm test classes + 3 fsm — full TLS handshake loopback, STARTTLS
upgrade seam (positive + injection negative), UDP exchange loopback, `DNSProbeTest` (hermetic
DNS-shaped responder + a `main(ip [port])` verified live against `8.8.8.8:53` → clean close,
`ready=true`, exit 0; and a dead port → `PortUnreachableException`, exit 1).

---

## 17. Current implementation inventory & working state (v1 — what is on disk today)

Everything below **exists and is green**. The v2 sections (§5–§13) are design only. Where a class
is slated to dissolve in v2, that is noted — but it is live code today.

### Main package — `src/main/java/org/zoxweb/server/net/common/sm/` (20 files)

| Class | Role | v2 fate |
|---|---|---|
| `ClientConSM` | The per-connection machine (`StateMachine<ClientSessionContext>`, always synchronous — `(Executor) null`). Mints + binds `TCPSMCallback` / `UDPSMCallback` with transport guards. | kept |
| `ClientConSMBuilder` | Programmatic composition: `phase()`, `transport()`, `closeOnReady()`, `settings()`. Fail-fast on duplicate phase names, a second SSL phase, banner+exchange together, exchange-before-IMMEDIATE-SSL ordering, and UDP × {SSL, SSH, start_tls}. Registers the transport router first, pre-registers the results bag, appends the `auto-close` READY action. | gains catalog-state registration |
| `ClientSMFactory` | JSON → machine (keys in §12). Fail-fast on contradictory combinations. | becomes the **state catalog** (§11) |
| `ClientSessionContext` | Machine config: `Mode` (PLAIN/TLS_HANDSHAKING/TLS_SECURE), `Transport` (TCP/UDP), the single session binding, settings/vars bags, `write()` routed to stream or datagram (gated during handshake), `fail()`, and the READY gate (`phaseComplete`, records `ready=true`). | kept |
| `TCPSMCallback` | Pure-SM TCP transport bridge (§4). | kept |
| `UDPSMCallback` | Pure-SM UDP client bridge (§4); does **not** extend `UDPSessionCallback`. | kept |
| `ClientTransportState` | TCP router: `CONNECTED` init + READY gate; `RAW_IN_DATA` → PLAIN pass-through as `IN_DATA`, or chunked feed into the SSL engine's inbound buffer in TLS modes. | kept (mandatory state) |
| `UDPClientTransportState` | UDP router: `CONNECTED` init + READY gate; `DATAGRAM` → `IN_DATA` pass-through (UDP is always plaintext). **New 2026-08-14.** | kept (mandatory state) |
| `SSLClientState` | Catalog `ssl` (was `SSLClientPhase`, converted 2026-08-15): bag-configured (`mode`, `cert_validation`, `endpoint_identification`, optional `ssl_context`), `IMMEDIATE` auto-start / `ON_DEMAND` upgrade, `ready_gate` when IMMEDIATE; the builder registers the engine states alongside it. | v2 |
| `SSLClientHandshakeState` | One consumer per `HandshakeStatus`, delegating to `SSLUtil._needWrap/_needUnwrap/_needTask/_finished`. Unwraps are **router-fed, no channel reads**. | kept |
| `SSLClientDataState` | Post-handshake `NOT_HANDSHAKING` unwrap loop, also no channel reads (FIN+data race fix). | kept |
| `SSLClientBridge` | Handler-facing `BaseSessionCallback<SSLSessionConfig>`: decrypted copy → `IN_DATA`, flips the output stream on handshake success, records `tls_protocol`/`tls_cipher`, publishes `SECURE`. | kept |
| `ClientSSLHelper` | The session's `SSLConnectionHelper`; routes statuses into the machine. **`close()` is a no-op by contract** (Rule 10). | kept |
| `MessageAssemblerState` | Catalog `assembler` (§7): boundary strategies `datagram`/`delimited` (+`strip_cr`)/`length_prefixed`/`stream`; `Assembly` accumulation holder in the machine bag (`SMProtoUtil.ASSEMBLY`). **New 2026-08-15.** | v2 |
| `ProtocolControllerState` | Catalog `controller` (§8): the linear `exchange` grammar (`send`/`expect`/`validate`/`start_tls`), working memory + `ready_gate` in the state bag, `max_skip` framed-skip bound, completion rule → READY gate + leftover drain. **New 2026-08-15.** | v2 |
| `ResponseControllerState` | Catalog `responder` (§5): `OUT_MESSAGE` → `ctx.write`; stateless. **New 2026-08-15.** | v2 |
| `ProtocolTypeValidatorState` | Catalog `validator` (§9): `VALIDATE` (payload `Validation` = message + meta) → verdict into results; mismatch fails the session. **New 2026-08-15.** | v2 |
| ~~`SSHBannerPhase`~~ | **DELETED 2026-08-15** — dissolved into `delimited` assembler + `validate` (ssh factory sugar, §11). | — |
| ~~`DataExchangePhase`~~ | **DELETED 2026-08-15** — dissolved into `assembler` + `controller` + `responder` (§8). | — |
| ~~`ConnectionPhase`~~ | **DELETED 2026-08-15** — first-implementation error (§14.10); ownership/broadcast-order contract text lives in `ClientTransportState`/package javadoc. | — |
| `SMProtoUtil` | Package utility home (**all new utilities go here** — maintainer directive): `STRING_TO_DATA`, `STRING_VARS_TO_STRING/DATA`, `hasVars`, `RESULTS` + `results(smi)`. (`BasicEvent` merged into `ClientEvent` 2026-08-15 — one enum is the whole vocabulary.) | gained `waitForClose` + `closeCause` (§13.2, done 2026-08-15) |
| `ClientEvent` | The complete session vocabulary (2026-08-15: absorbed `SMProtoUtil.BasicEvent`): `CONNECTED`, `RAW_IN_DATA`, `DATAGRAM`, `IN_DATA`, `SECURE`, `READY`, `START_TLS`, `IN_MESSAGE`, `OUT_MESSAGE`, `VALIDATE`, `CLOSED`. (`BANNER_RECEIVED` removed — v2 step 1 done.) | kept |
| `ProtoConnect` | Observer CLI: config + `host:port` + `var=value` → prints lifecycle events; exit 0/1/2/64. Never drives the session. | kept |
| `package-info.java` | Package overview javadoc. | kept |

**Outside the package, part of this work:** `NIOSocket.addDatagramSocket(InetSocketAddress,
ConnectionCallback<?>)` (bind → `setChannel` → register **0 ops** → `connected(sk)` → install
returned ops); `UDPSessionCallback.connected()` now calls `setChannel`;
`shared.util.NVGMProperties` gained the named-bag constructor; `remoteAddress` and its accessors
moved up from `BaseSessionCallback` to `SessionCallback` (2026-08-16), so both SM callbacks now
inherit one implementation instead of declaring their own — `TCPSMCallback.setChannel` and the
`UDPSMCallback` constructor call `setRemoteAddress`. Its semantics are pending a larger rework —
open items recorded in `PENDING.md` §3.4, deliberately not patched.

### Test package — `src/test/java/org/zoxweb/server/net/common/sm/` (16 classes)

`ClientConSMBuilderTest`, `ClientSMFactoryTest`, `ClientSSLHelperTest`, `SSLClientBridgeTest`,
`ProtoDataTest`, `ProtoConnectTest`, `SSHBannerPhaseTest`, `SSHBannerLoopbackTest`,
`PlainClientLoopbackTest`, `TLSClientLoopbackTest`, `DataExchangeLoopbackTest`,
`DataExchangeStartTLSTest`, `StartTLSUpgradeSeamTest`, `UDPSMCallbackTest`,
`UDPClientLoopbackTest`, `DNSProbeTest`. Plus `net/common/TCPSMCallbackTest` and three
`org/zoxweb/server/fsm/*Test` classes.

Test keystore: `src/test/resources/test.zoxweb.org.jks` — PKCS12, storepass `password`, alias
`selfsigned`.

### Working state (as of 2026-08-15)

- **The fsm refactor is committed** (`b2f008c5 Updated the state machine properties`):
  `StateMachine`/`State` extend `NVGMProperties`, scheduler generalized to
  `ScheduledExecutorService`. That is the composition foundation (§3).
- **Everything in `net.common.sm` is UNCOMMITTED** — ~20 modified files plus 4 untracked
  (`UDPClientTransportState`, `ClientConSMBuilderTest`, `DNSProbeTest`, `UDPClientLoopbackTest`,
  and this document). Expect a noisy `git status`; it is intentional work in progress, not drift.
- **Intentional deletions/renames in the working tree:** `PROTO-CONFIG.md` deleted (recoverable
  via `git checkout -- PROTO-CONFIG.md`; its content is folded into §12 here);
  `META-SM-STATUS.md` deleted (was untracked — folded into §2/§3/§4/§15 here);
  `ClientConnectionSMBuilderTest` renamed to `ClientConSMBuilderTest`. **Do not "restore" these.**
- **Verify before changing anything:** run both suites in §16. They were green at last run.
- **Environment quirks:** Maven *plugin* downloads from repo.maven.apache.org fail with a PKIX
  error inside an agent shell (TLS interception) — the user's own shell is fine, and all project
  dependencies are already cached, so builds/tests work; only uncached plugin goals fail. Local
  repo is `D:/dev/data/java/.m2/repository`. To run a `main` directly, put `target/classes` +
  `target/test-classes` + `gson` + `uuid-creator` jars on the classpath (§16). Throughput or
  HTTP-benchmark anomalies on this machine are usually Avast Web Shield, not code.
