# META-SSL-ENGINE-DESIGN — how the TLS engine driver actually works

**Packages:** `org.zoxweb.server.net.ssl` (engine driver + server machines) ·
`org.zoxweb.server.net.common.sm` (client machine)
**Status:** load-proven in production (≈19K TLS-terminated HTTPS req/s, 50–60K WS-TLS msg/s on an
8-core SBC). The code is tuned and fragile. Flag suspicions, do not patch unprompted.

---

## 0. Read this first

This document exists because every fresh reading of `SSLUtil` re-derives the same handful of wrong
conclusions about `NEED_WRAP` / `NEED_UNWRAP` — "app data is dropped", "BUFFER_OVERFLOW is
reachable", "the selector is blocked", "this loop spins". **§9 lists those claims and why each one
is false.** Read §9 before writing a single word of review about this subsystem.

The subsystem is a **non-blocking driver for `javax.net.ssl.SSLEngine`**. It does not invent a
protocol state machine. The *engine* owns the TLS state; this code only services whatever
`HandshakeStatus` the engine currently reports, performs exactly **one** engine step per call, and
re-publishes the status the engine handed back. There is no work queue, no timer, no retry policy,
and no per-session lock.

---

## 1. The whole model in one paragraph

`publish(status, callback)` **is** the loop. It is a status → handler dispatch table. Each handler
performs one engine step (`wrap`, `unwrap`, run delegated tasks, or the post-handshake hook) and
then calls `publish` again with the status the engine returned. That is a **synchronous, re-entrant
call chain on the same worker thread** — not a scheduler, not an event queue. The chain ends when a
handler returns *without* publishing, which happens on exactly three conditions:

1. `BUFFER_UNDERFLOW` — the engine needs more wire bytes; the worker re-arms `OP_READ` and returns
   to the pool. The selector will re-dispatch when the peer sends more.
2. the session closed (`CLOSED` result, EOF, or an exception path), or
3. the handshake finished and the net in-buffer holds nothing further to process.

Nothing else stops it, and nothing else drives it. If you are looking for "the thing that schedules
the next handshake step", it is the `publish` call at the bottom of the previous handler.

---

## 2. Cast

| Piece | Class | Role |
|---|---|---|
| Engine step handlers | `SSLUtil._needWrap` / `_needUnwrap` / `_needTask` / `_finished` / `_notHandshaking` | The five handlers. One engine step each. Shared by **every** driver, server and client |
| Per-session state | `SSLSessionConfig` (implements `SSLConfigInt`) | Engine, channel, the three buffers, close logic |
| Dispatcher contract | `SSLConnectionHelper<C>` | `publish(status, callback)` + `notifySSLHandshakeFinished()` |
| Completion target | `SSLHandshakeFinished` | `sslHandshakeSuccessful(SSLConfigInt)` — where `_finished` lands: `SSLNIOSocketHandler` (tunnel hook), `TCPSessionCallback` (`connectedFinished`); the validator machine reaches `SSLClientBridge` via `ClientSSLHelper` instead |
| Default dispatcher (server + generic client) | `CustomSSLStateMachine` | `MonoStateMachine` — a plain status→lambda table, `synchronous=false`. One constructor `(SSLConfigInt, SSLHandshakeFinished)` — self-installs the helper; the target receives handshake completion (see §9.9) |
| Server dispatcher (full-FSM path) | `SSLStateMachine` + `SSLHandshakingState` + `SSLDataReadyState` | Same five handlers behind the `org.zoxweb.server.fsm` framework. Selected by `simpleStateMachine=false` |
| Server transport | `SSLNIOSocketHandler` | Owns the channel; `accept(SelectionKey)` publishes the current status |
| Client transport (plain client) | `TCPSessionCallback` | `sslUpgrade()` installs a `CustomSSLStateMachine`, then publishes to start |
| Client machine (validator SM) | `ClientSSLHelper` + `SSLClientHandshakeState` + `SSLClientDataState` | Router-fed variants — see §7 |
| Net buffer pair | `IOBuffers` (`org.zoxweb.server.io`) | in/out ciphertext pair under one lifecycle; recached to the pool on close |

---

## 3. Threading — why there are no locks

- The **selector thread never touches the engine**. It selects, accepts, and dispatches.
- **Key-interest gating**: before dispatching a readable channel the engine sets the key's interest
  ops to `0`; the worker re-arms `OP_READ` only after its full cycle completes. While a worker owns
  a session, that session **cannot** be dispatched to a second worker. Per-session state therefore
  needs no synchronization — the gate is the serialization mechanism, not any lock.
- Different sessions run fully in parallel on different workers.
- `_needTask` runs the engine's delegated tasks **inline on the worker** (cert-chain validation,
  OCSP, HSM signature). That stalls *that worker only*; the selector keeps dispatching other
  sessions. Handshake work being serialized on one worker per session is the design, not a defect.

---

## 4. Buffers — three per session, all heap, all pooled

Allocated once by `SSLSessionConfig.beginHandshake(IOBuffers)` (idempotent via `hasBegan`), which
also sets client/server mode and calls `SSLEngine.beginHandshake()`:

| Buffer | Accessor | Size | Holds |
|---|---|---|---|
| net in | `getSSLIOBuffers().getInBuffer()` | ≥ `SSLSession.getPacketBufferSize()` (~16 KB+) | inbound **ciphertext** — the `unwrap` source |
| net out | `getSSLIOBuffers().getOutBuffer()` | ≥ `getPacketBufferSize()` | outbound **ciphertext** — the `wrap` destination |
| decryption | `getInDecryptedBuffer()` | `SSLSession.getApplicationBufferSize()` | inbound **plaintext** — the `unwrap` destination |

A caller may **donate** a pre-allocated `IOBuffers` pair; anything missing or undersized is
replaced. Undersized donated buffers are dropped to GC on purpose — only correctly sized buffers go
back to the pool. `close()` recaches all three (`IOBuffers.close()` for the pair,
`ByteBufferUtil.cache(...)` for the decryption buffer); `cache0` clears each buffer before pooling,
so no plaintext survives into the next session.

### Buffer-mode contract (get this wrong and everything below reads as a bug)

`SSLUtil.smartSSLWrap/smartSSLUnwrap(engine, source, destination, flipSource, compactSource)`:

- the **source** is flipped before the engine call and compacted after ⇒ it always ends in
  **write-mode**, position = unconsumed bytes. This is why `net.position() > 0` means "leftover
  ciphertext" and why `_finished` tests exactly that;
- the **destination** is never touched by these helpers ⇒ it stays in **write-mode**, position =
  *accumulated* plaintext across however many unwraps happened since the last drain;
- the consumer (`callback.accept(buffer)`) therefore receives a **write-mode** buffer and is
  required to fully drain it (`ByteBufferUtil.smartWrite(..., flip=true)` flips, drains, compacts).

Accumulation in the destination is the mechanism that keeps stream order across handler boundaries.
It is not a leak.

---

## 5. The five handlers — exact contract

| Handler | Reads the channel? | Source | Destination | On `OK` |
|---|---|---|---|---|
| `_needUnwrap` | **yes** (`channel.read(netIn)`) | net in | **decryption buffer** | `publish(result.getHandshakeStatus())` |
| `_needWrap` | no (writes) | `ByteBufferUtil.EMPTY` | net out → `smartWrite` to channel | `publish(result.getHandshakeStatus())` |
| `_needTask` | no | — | — | runs all delegated tasks, then `publish(engine.getHandshakeStatus())` |
| `_finished` | no | — | — | `helper.notifySSLHandshakeFinished()` → the session's `SSLHandshakeFinished` target (unconditional; a thrown exception is terminal — session closed, no continuation), then `publish(current status)` **iff `netIn.position() > 0`** |
| `_notHandshaking` | **yes** | net in | decryption buffer | loops unwrapping; `callback.accept(decryptionBuffer)` whenever `bytesProduced > 0`; exits on `BUFFER_UNDERFLOW` |

Status guards: `_needUnwrap`, `_needWrap` and `_notHandshaking` each re-check
`config.getHandshakeStatus()` on entry and return silently if it no longer matches — a stale publish
must never drive an engine step. `_notHandshaking`'s `else` branch re-publishes the *current* status
instead, which is how a post-handshake status change (TLS 1.3 KeyUpdate, `NEED_UNWRAP_AGAIN`) gets
back into the handshake handlers.

`NEED_UNWRAP_AGAIN` is registered as a **string** trigger next to `NEED_UNWRAP` because the enum
constant does not exist on Java 8 — the codebase compiles to Java 8. `SUS.enumName(...)` is the
comparison idiom.

---

## 6. The transition graph, including the drain chain

```
selector: readable ─▶ interestOps=0 ─▶ worker ─▶ publish(engine.getHandshakeStatus())
                                                      │
        ┌─────────────────────────────────────────────┼──────────────────────────────┐
        ▼                    ▼                        ▼                              ▼
   NEED_UNWRAP          NEED_WRAP                 NEED_TASK                     NOT_HANDSHAKING
   read+unwrap          wrap+write                run tasks                     read+unwrap loop
        │                    │                        │                         accept(plaintext)
        └────────┬───────────┴────────────┬───────────┘                              │
                 ▼                        ▼                                          │
            publish(next)            publish(next)                                   │
                 │                                                                   │
                 ▼                                                                   │
             FINISHED ──▶ notifySSLHandshakeFinished ─▶ sslHandshakeSuccessful        │
                 │                                                                   │
                 └── netIn.position() > 0 ? ──▶ publish(NOT_HANDSHAKING) ─────────────┘
                                  │
                                  no ──▶ return; worker re-arms OP_READ
```

**The drain chain is the load-bearing part.** A handshake handler never dispatches application data
itself. If an `unwrap` inside `_needUnwrap` produces plaintext — the case where a peer's flight
interleaves app data with the handshake finish — those bytes sit in the decryption buffer, the
published status carries the session forward, and `_finished` → `NOT_HANDSHAKING` →
`_notHandshaking` delivers the buffer to the callback in stream order. `_finished`'s own comment
draws it:

```
||-----DATA BUFFER------ ||
||Handshake data|App data||
```

Delivery belongs to the state transition. Reading any single handler in isolation and concluding
"this handler drops data" is the single most common misreading of this file.

---

## 7. Two drivers, one handler set

**Server / plain client** — `CustomSSLStateMachine` (default) or `SSLStateMachine` (full FSM,
`simpleStateMachine=false`). Both are thin: register the five statuses, delegate to the `SSLUtil`
handlers, count rates. `SSLNIOSocketHandler.accept(key)` publishes `config.getHandshakeStatus()` on
every readable event, which is the *only* thing needed to drive a whole session.

**Validator client machine** (`net.common.sm`) — `ClientSSLHelper` routes publishes into
`ClientConSM`, where `SSLClientHandshakeState` / `SSLClientDataState` consume them.
`NEED_WRAP` / `NEED_TASK` / `FINISHED` delegate straight to `SSLUtil`; on completion,
`ClientSSLHelper.notifySSLHandshakeFinished()` delegates to the session's `SSLClientBridge`
(output stream flipped to encrypted writes, `TLS_SECURE`, results bag, `SECURE` publish, the
ssl state's READY gate — the bridge is set by `SSLClientState.upgrade` before the first
publish, so it is always present at `FINISHED`). The two **unwrap** paths are
re-implemented locally for one reason:

> In that stack `TCPSMCallback` drains the socket and the transport router feeds the net in-buffer.
> A handler that also called `channel.read()` would compete with the router: when a packet is fed in
> chunks, a handler-side read between chunks pulls newer socket bytes ahead of the still-unfed older
> bytes and corrupts ciphertext order. `_notHandshaking`'s own read would additionally see `-1` when
> the peer's final data and FIN arrive together, closing the session with records still buffered.
> **EOF detection belongs to `TCPSMCallback`'s read loop.**

`SSLClientHandshakeState` unwraps into `ByteBufferUtil.EMPTY` rather than the decryption buffer.
That is deliberate and client-side-exact: while a *client* engine is genuinely `NEED_UNWRAP`, only
server handshake records can arrive (no app data before the server Finished, and consuming that
Finished flips the engine to `NEED_WRAP`), so `bytesProduced` is provably 0. A peer that violates it
earns `BUFFER_OVERFLOW → SSLException → ctx.fail`, which is the correct strict verdict for a
conformance checker. `_needUnwrap` is shared with server sessions where a producing unwrap is real,
which is why *it* targets the decryption buffer instead.

---

## 8. Write path and close path

**Write** — `SSLUtil.sslChunkedWrite(config, channel, src, tracker, closeable, flip)`:
payloads under `min(applicationBufferSize, 8 KB)` go through a single `_sslWrite`; larger payloads
are sliced into 8 KB views, each encrypted as one TLS record. The chunk size sits deliberately below
`getApplicationBufferSize()` so every `wrap()` fully consumes its chunk. `_sslWrite` refuses to run
unless the engine is `NOT_HANDSHAKING`. `ByteBufferUtil.smartWrite` loops until every byte is on the
wire — a complete-or-fail write, by design (see §9.4).

**Close** — `SSLSessionConfig.close()` is one-shot (`isClosed` CAS) and does, in order:
`closeOutbound()`; a drain loop that publishes `NEED_WRAP`/`NEED_UNWRAP` **with a null callback**
until the engine's outbound is done, the channel is shut, or `forcedClose` is set; close both
channels; cancel the selection keys (legacy, `@Deprecated`, effectively a `wakeup()`); close the
`SSLConnectionHelper` and the `IOBuffers`; recache the decryption buffer; close the output stream.

The handlers all tolerate `callback == null` precisely because of that drain loop.

In the validator SM, `ClientSSLHelper.close()` is a **deliberate no-op**: `SSLSessionConfig.close()`
closes its helper *before* the machine publishes `CLOSED`, so a helper that closed the machine would
make the `CLOSED` publish throw and the event would be lost. The machine is closed exclusively by
`TCPSMCallback`'s close delegate, last.

---

## 9. Non-findings — claims that are wrong, with the proof

These get "discovered" over and over. Each is false. Do not report them.

1. **"`BUFFER_OVERFLOW` is reachable / the guard is a recovery gap."**
   False. The decryption buffer is allocated at `SSLSession.getApplicationBufferSize()` — the
   engine's own ceiling for the plaintext a single `unwrap` can produce — and the net out-buffer at
   `getPacketBufferSize()` against ≤ 8 KB plaintext chunks. Both overflow branches are
   dead-on-arrival guards. See invariant 3 in the `SSLUtil` class javadoc.

2. **"`_needUnwrap` ignores `result.bytesProduced()` — app data is dropped / the session stalls."**
   False. The handshake handlers deliberately do not dispatch; the state transition owns delivery
   (§6). `_finished` re-publishes `NOT_HANDSHAKING` while the net buffer still holds bytes and
   `_notHandshaking` drains the decryption buffer to the callback, in order, because the destination
   accumulates in write-mode and `accept` flips from position 0.

3. **"`_needTask` blocks the selector."**
   False. Delegated tasks run on the worker, never on the selector thread (§3). Serialized handshake
   work per session is the design.

4. **"`smartWrite` busy-spins; this needs `OP_WRITE` + a partial-write queue."**
   False. Complete-or-fail synchronous write is the contract: when `sslChunkedWrite` returns
   normally, every byte is encrypted and on the wire. There is no "unsent tail" steady state to
   reason about. Never propose the `OP_WRITE` queue rewrite.

5. **"The `EMPTY` destination in `SSLClientHandshakeState` is a bug."**
   False — §7. It is a strict client-side verdict, and the drain chain exists on that machine too if
   it ever needs to change.

6. **"Recaching buffers into a pool is unsafe / copy per dispatch would be safer."**
   False. The allocate/cache pool is load-proven (without it, GC thrash under load). `cache0` clears
   before pooling. Fix lifecycle traps with documentation, never by removing recaching.

7. **"The `publish` recursion could overflow the stack."**
   False in practice: every recursion step either consumes a TLS record from a bounded net buffer or
   returns on `BUFFER_UNDERFLOW`. Depth is bounded by the records in one ~16 KB buffer.

8. **"`SSLSessionConfig.close()`'s `while` loop is an infinite spin."**
   False. It is the close_notify drain and exits on `isOutboundDone()`, a closed channel, or
   `forcedClose`; the `default` branch closes the channel, which terminates it.

9. **"The helper is installed twice / the wiring is asymmetric between server and client."**
   False. `CustomSSLStateMachine` has a single constructor
   `(SSLConfigInt, SSLHandshakeFinished)` that **self-installs** the helper into the session
   config (`setSSLConnectionHelper(this)`); server transport and plain client differ only in
   the completion target they pass (`SSLNIOSocketHandler` / `TCPSessionCallback`). That
   invariant (helper installed before any publish) is why `_finished` calls
   `helper.notifySSLHandshakeFinished()` unconditionally. `sslUpgrade()`'s outer
   `setSSLConnectionHelper(new CustomSSLStateMachine(config, this))` re-sets the same
   instance — redundant but idempotent, not a defect. **Review rule: `_finished`'s completion
   chain (`helper.notifySSLHandshakeFinished()` → target `sslHandshakeSuccessful`) carries
   BOTH post-handshake side-effects — the server tunnel hook
   (`SSLNIOSocketHandler.sslHandshakeSuccessful`, that method's only call chain, never
   exercised by plain-TLS tests since `remoteConnection == null`) and the validator machine's
   SECURE/READY gate (`ClientSSLHelper` → `SSLClientBridge`, covered by the sm TLS loopback
   tests). Check both explicitly in any dispatcher change.**

Two more from the surrounding stack that land on the same code: workers catching `Throwable` is
Rule 1 ("the app must survive"), and there is no hung-task protection anywhere by choice — Rule 2
("no babysitting callers").

---

## 10. Genuinely open / known drift

- `SSLUtil`'s class javadoc, invariant 1, still says the `synchronized` keyword on
  `smartWrap`/`smartUnwrap` is "belt-and-suspenders". Those synchronized copies lived on
  `SSLSessionConfig` and have been deleted; the static `SSLUtil.smartSSLWrap`/`smartSSLUnwrap` are
  **not** synchronized. The claim it makes (key-interest gating is the real serialization) is still
  correct — only the parenthetical about `synchronized` is stale.
- `SSLSessionConfig.selectorController` is `@Deprecated` with a written removal plan: closing a
  channel already cancels its keys, so the two `cancelSelectionKey` calls only force a prompt
  `wakeup()`.
- `SSLConfigInt.getSSLSession()` is a `default` accessor with no callers in the repo —
  kept for external/API use.

---

## 11. File map

```
org/zoxweb/server/net/ssl/
  SSLUtil.java                  the five handlers + smartSSLWrap/Unwrap + sslChunkedWrite/_sslWrite
  SSLConfigInt.java             per-session contract (engine, channel, buffers, beginHandshake)
  SSLSessionConfig.java         the implementation: buffers, SNI, close/drain
  SSLConnectionHelper.java      publish(status, callback) + notifySSLHandshakeFinished
  SSLHandshakeFinished.java     completion target: sslHandshakeSuccessful(SSLConfigInt)
  CustomSSLStateMachine.java    default server/client dispatcher (MonoStateMachine)
  SSLStateMachine.java          full-FSM dispatcher  ---+
  SSLHandshakingState.java      NEED_* triggers         +-- simpleStateMachine=false path
  SSLDataReadyState.java        NOT_HANDSHAKING trigger-+
  SSLNIOSocketHandler.java      server transport, setupConnection, tunnel callback
  SSLContextInfo.java           engine factory (keystore / client mode / SNI)

org/zoxweb/server/net/common/
  TCPSessionCallback.java       client sslUpgrade() + accept(key) publish

org/zoxweb/server/net/common/sm/     (see META-SM-PROTO-DESIGN.md for the machine itself)
  ClientSSLHelper.java          publishes into ClientConSM; close() is a no-op by contract;
                                notifySSLHandshakeFinished delegates to the SSLClientBridge
  SSLClientHandshakeState.java  router-fed NEED_UNWRAP (EMPTY dest); others delegate to SSLUtil
  SSLClientDataState.java       router-fed NOT_HANDSHAKING loop, delivers plaintext

org/zoxweb/server/io/
  IOBuffers.java                the ciphertext in/out pair + pooled lifecycle
  ByteBufferUtil.java           allocate/cache pool, smartWrite, EMPTY
```

Related docs: `NET.md` (how to *build* services on this stack), `META-SM-PROTO-DESIGN.md` (the
validator state machine), `FSM.md` (the `org.zoxweb.server.fsm` engine).
