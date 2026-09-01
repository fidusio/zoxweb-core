# META-PROTOCOL — the JSON meta-driven protocol validator

**Package:** `org.zoxweb.server.net.protocols`
**This is the single authoritative document for this subsystem.**

---

## 0. Purpose & scope

The subsystem is a **client-side service-conformance checker** for network endpoints the operator
runs or is authorized to check. It answers one operational question: *does the service at this
address still speak the protocol it is supposed to speak, and is its TLS configuration what we
expect?* It is the config-driven equivalent of running `dig`, `openssl s_client`, or a scripted
SMTP health check — the tooling behind uptime monitoring, deployment smoke tests, TLS-posture
checks, and protocol regression tests in CI.

**What a session does, in full:** open **one** connection to **one** caller-supplied address, run
a short scripted request/response dialogue declared in JSON, compare the reply against expected
values, record a pass/fail verdict, and close. That is the entire capability surface.

**Structural limits (by design, not by policy):**
- **No discovery.** The caller supplies a single `InetSocketAddress`. There is no host
  enumeration, no port ranging, no parallel-target machinery. The config cannot name a host — an
  optional top-level `port` is a default-port *hint* only.
- **A guarded linear script — no loops, no computation.** The `exchange` script is a linear step
  list with a **forward-only** cursor. Labeled steps and outcome routes (an expect block's
  `alt`, a validate's `on_mismatch`, `jump`; reserved targets `done`/`fail`) may only jump
  forward — enforced at compile, so loops are impossible by construction. A `regex` capture
  *reads* a value into the report, but the script can never compute with it: no captured value
  ever feeds a send. Anything richer (loops, computed values, multi-connection logic) is out of
  scope and requires purpose-written code.
- **No credential logic, no retry/backoff, no rate manipulation.** Sessions are single-shot and
  identify themselves normally.
- **Protocols live in JSON, never in code.** There is no protocol-specific sugar in the engine:
  an SSH banner check, an SMTP STARTTLS check, and a DNS probe are all plain definition files
  consumed by the same generic engine (§8).

**Safe TLS defaults:** certificate-chain validation *and* hostname verification are on by default
(`cert_validation: true`); disabling them exists for internal endpoints with self-signed
certificates and for reading what an endpoint negotiates, and is unsafe anywhere else. The
STARTTLS residue check (§4.3) is a client-side *defense* against the STARTTLS command-injection
vulnerability class: the session is failed — never continued — if unexpected plaintext arrived
before the upgrade.

---

## 1. Architecture

Four classes, layered on the existing transport and TLS plumbing. Step dispatch is a
`MonoStateMachine` op→handler table (the `CustomSSLStateMachine` pattern: one registered
handler per op, publish-chained inline on the read worker) — not the full
`org.zoxweb.server.fsm.StateMachine` framework: no states, no triggers, no event vocabulary,
no property bags:

```
                    JSON definition (file or string)
                              │
                              ▼  compile, fail-fast
                     ┌─ ExchangeScript ─────────────┐
                     │ assembler (framing)          │
                     │ step pump (send/expect/      │
                     │   validate/start_tls/label/  │
                     │   jump/record)               │
                     │ results (NVGenericMap)       │
                     └──────┬───────────────────────┘
              host seam: write() · startTLS() · fail() · complete()
                     ┌──────┴───────────────────────┐
        TCPMetaProtocol                   UDPMetaProtocol
        extends TCPSessionCallback        extends UDPSessionCallback
                     │                              │
        CommonChannelOutputStream          connected DatagramChannel
        CustomSSLStateMachine + SSLUtil
                     │                              │
                     └────────── NIOSocket ─────────┘
```

| Class | Role |
|---|---|
| `ProtoUtil` | Stateless statics: literal decoding (`txt:`/`hex:`/`base64:`), `${var}` resolution, binary-safe `indexOf`, `NVGenericMap` config readers. |
| `ExchangeScript` | The transport-agnostic engine: compiles the JSON, frames incoming bytes, runs the linear step script, owns the results. Talks to its host only through the host seam. |
| `TCPMetaProtocol` | TCP host: `TCPSessionCallback` subclass wiring the engine to the socket, the session output stream, and both TLS modes. |
| `UDPMetaProtocol` | UDP host: `UDPSessionCallback` subclass on a *connected* datagram channel; plaintext only. |

**Threading.** Everything runs inline on the NIOSocket read worker. NIOSocket zeroes a key's
interest ops for the duration of each dispatch, so per-session dispatches never overlap — the
engine needs no locks. A session only ever closes on its own worker (from the read loop's
EOF/error path or from a script decision); observers never drive or close a session.

**The complete usage contract:**

1. Build a validator from a JSON definition: `new TCPMetaProtocol(id, json)` /
   `new UDPMetaProtocol(id, json, remoteAddress)` — or the `ProtoConnect.createTCPValidator` /
   `createUDPProtocol` factories (endpoint + definition in, bound validator out).
2. Hand endpoint + validator to NIOSocket:
   `addClientSocket(InetSocketAddress, ConnectionCallback<?>, timeoutInSec, resolver)` (TCP) or
   `addDatagramSocket(InetSocketAddress localBind, ConnectionCallback<?>)` (UDP).
3. NIOSocket invokes `connected(SelectionKey)`; from that instant the validator performs the
   entire session — send per configuration, assemble replies, validate, upgrade to TLS if
   scripted, close itself.
4. The caller waits via `waitForClose(millis)` (or polls `isClosed()`) and reads the verdict from
   `getResults()` + `getCloseCause()`.

JSON definition + address in → autonomous session → validation report out.

---

## 2. JSON schema

Top-level keys:

| Key | Type / values | Default | Meaning |
|---|---|---|---|
| `name` | string | `"protocol-validator"` | label for logs/reports |
| `transport` | `"tcp"` \| `"udp"` | `tcp` | which validator class may run this definition |
| `port` | int, or int array | — | the protocol's well-known port(s), **hint** only — never an endpoint. An array (`"port": [25, 587]` for SMTP STARTTLS) documents every port the protocol is expected on; the **first** entry is the default used when the caller's endpoint omits a port |
| `timeout_sec` | int | 5 | connect timeout hint for the caller |
| `close_on_ready` | boolean | `true` for udp, else `false` | session closes itself when the script completes |
| `tls` | `{mode, cert_validation}` | — | TLS behavior (§4.2, §4.3); TCP only |
| `assembler` | `{boundary, …}` | by transport | message framing (§3.1) |
| `vars` | object | — | defaults for `${name}` injection |
| `exchange` | array of steps | — | the guarded linear script (§3.2) |

`tls` block: `mode` is `"immediate"` (handshake right after connect) or `"on_demand"` (upgrade
when the script executes `start_tls`); `cert_validation` defaults to `true` — validate the chain
against the JVM trust store **and** verify the certificate identity against the dialed host. The
two always move together: chain validation without hostname verification would accept a valid
certificate issued for a different host.

Fail-fast validation at compile: unknown transport, tls mode, boundary, or exchange op;
`udp` × {`tls`, `start_tls`}; `start_tls` without an `on_demand` tls block; malformed
`hex:`/`base64:` literals. Routing and capture (§3.2.1, §3.3): duplicate/empty/reserved
labels; unknown, backward, or self route targets; an expect block without `match`, an empty
`alt` list, an alt entry missing `match` or `goto`; `on_timeout` (reserved for phase 2);
`optional` + `on_mismatch`; a malformed regex, `${var}` in a regex, `group` without a regex or
out of the pattern's range; an empty `record` block or a `record` key naming a reserved verdict
key. The retired sm-era keys (`protocol`, `ssh`, `states`) are rejected with a clear error —
protocol shapes are definition files now (§8).

**Key names are case-insensitive.** The definition is parsed into an `NVGenericMap`, whose entry
names match regardless of case — `"Port"`, `"PORT"`, and `"port"` are the same key, at every
nesting level (top-level, `tls`, `assembler`, step blocks). This also means two `report` keys
differing only by case collide into one results entry — don't author that. Key-case tolerance is
a *config* property: data literals (§2.1) stay byte-exact unless a `validate` opts into
`ignore_case` (§3.3).

### 2.1 Data literals and variables

Every data literal (`send`, `expect`, `terminator`, `validate.prefix/contains/exact`) carries an
optional one-word encoding prefix, split at the **first `:`**:

- `txt:` — body is UTF-8 verbatim.
- `hex:` — body is hex digits, whitespace ignored (`"hex:1234 0100"` → `12 34 01 00`).
- `base64:` — body is Base64.
- **No recognized prefix** — the whole string, colon and all, is UTF-8 (`"USER: bob"` is text,
  not a directive).

Bodies may contain `${name}` placeholders resolved from `vars` / caller injection
(`setVar(name, value)`) at send time; a missing or empty variable fails the step. **The two
layers never mix:** the prefix is identified first, then variables are resolved in the body only.
A variable's *value* can never become an encoding directive — a var expanding to a string that
starts with `hex:` is data. Literals without variables are decoded at compile time (fail-fast);
literals with variables keep the raw body and decode at send time.

---

## 3. The engine — `ExchangeScript`

### 3.1 Assembler (message framing)

Incoming bytes accumulate in an internal buffer; a **boundary strategy** decides when a complete
message exists. `max_message` (default 65536) caps the accumulation — a breach fails the session
(a hostile or broken peer cannot exhaust memory).

| `boundary` | Rule | Default for | Meta keys |
|---|---|---|---|
| `datagram` | one datagram = one message; no accumulation | **UDP** | — |
| `delimited` | message ends at a terminator, binary-safe, detected across chunk boundaries | — | `terminator` (default `txt:\r\n`), `strip_cr` (drop a trailing `\r` before the terminator) |
| `length_prefixed` | a header field carries the payload length | — | `length: {offset, size (1/2/3/4), endian (big/little, default big), adjust}` — `size 3` covers 24-bit headers (the HTTP/2 frame shape) |
| `stream` | the accumulation-so-far *is* the current message; the pump consumes through each match | **TCP** | — |

Defaults by transport mean most definitions write only `"boundary"`, or nothing.

A definition whose protocol changes framing mid-dialogue (e.g. an SSH text banner followed by
binary packets) switches strategy with a **`boundary` exchange step** (§3.2): the step carries a
full `assembler`-shaped block, validated at compile; from that step on, the accumulation residue
reframes under the new rule. Not available over UDP (one datagram = one message).

### 3.2 Step pump

The `exchange` script is a **guarded linear step list**: a forward-only cursor advances through
it as the session progresses, and outcome routes (§3.2.1) may only jump forward. The pump runs
after every state change (start, new data, TLS secured) until it blocks waiting for data, fails,
or completes. Dispatch is the inherited `MonoStateMachine` op→handler table — publish is the
loop, one handler per op, inline on the read worker.

| Op | Action | Semantics |
|---|---|---|
| `{"send": "<literal>"}` | write to the session | `${vars}` resolved at send time; bytes go through the host's write path (encrypted when the session is secure) |
| `{"expect": "<literal>"}` | wait for a match | per-message contains-match. Framed boundaries: a non-matching complete message is skipped, bounded by `max_skip` (default 65536) — the SMTP `250-` continuation idiom. `stream`: match against the accumulation, consume through the end of the match |
| `{"expect": {"match": …, "alt": […]}}` | wait, with routes | block form of `expect`: the same main match plus alternative patterns, each routed to a forward label — §3.2.1 |
| `{"validate": {…}}` | apply the verdict match | see §3.3; a mismatch fails the session — unless the step routes it with `on_mismatch` (§3.2.1) |
| `{"start_tls": true}` | upgrade to TLS | residue check, then the host seam's `startTLS()`; the pump resumes when the handshake completes (§4.3) |
| `{"boundary": {…}}` | switch framing | replaces the active framing with the step's `assembler`-shaped block (§3.1); the accumulation residue reframes under the new rule. TCP only |
| `{"label": "<name>"}` | no-op jump target | names a forward route target; labels are case-insensitive and unique; `done`/`fail` are reserved |
| `{"jump": "<target>"}` | unconditional route | forward jump to a label, or the reserved `done` (complete now) / `fail` (fail the session) |
| `{"record": {…}}` | merge constants | writes the block's literal keys into the results — the way a branch marks which path ran. The engine-owned keys (`ProtoUtil.ResKey`: `validated`, `reason`, `ready`, `latency_ms`, `guid`, `proto-name`, `transport`, `host`, `port`, `open_ts`, `close_ts`, `tls_protocol`, `tls_cipher`, `tls_kex_group`, `error`, `scan-id`, `scan-time-in-ms`, `total-scanned`) are rejected at compile |

**Completion rule:** the step list finishing = session done → `ready=true` in results →
`close_on_ready` honored. **A run that completes with no `validate` step records
`validated=true, reason="script completed"`** — every run yields a verdict; on a branched
script the `record` keys carry which path produced it.

#### 3.2.1 Routing — the guarded-linear rules

Every route target is a `label` name, or the reserved `done` (= the end of the step list) /
`fail` (= fail the session). Every resolved route must point **strictly forward** of the
routing step — enforced at compile, so loops are impossible by construction.

**Steps stay single-key objects.** The JSON→`NVGenericMap` mapping keeps only the first key of
a multi-key step object, so route metadata lives *inside* the step's value: the `expect` block
form, and `on_mismatch` inside the `validate` meta. An `expect` value is treated as a block when
it is an object (or a string that starts with `{` — a byte literal genuinely starting with `{`
must be written with the `txt:` prefix).

```json
{ "expect": { "match": "txt:STARTTLS",
              "alt": [ { "match": "txt:250 ", "goto": "plaintext_only" } ] } }
```

Alt evaluation, by boundary:
- **`stream`**: the main `match` is tried first and **wins regardless of byte position**; then
  each alt in declaration order. The matching pattern (main or alt) consumes through its own
  match and becomes the current message. An alt that matches mid-line leaves residue — a branch
  that feeds `start_tls` must still consume through the line terminator, or the residue check
  fails the upgrade (by design).
- **Framed** (`delimited`/`length_prefixed`/`datagram`): per delivered message, main first, then
  the alts in order; a message matching *neither* is skipped exactly as today (`max_skip`
  bounded) — the `250-` continuation idiom survives. Author alt patterns that cannot occur in
  continuation lines, or the alt fires where a skip was intended.

`{"validate": {…, "on_mismatch": "<target>"}}` routes a mismatch forward instead of failing:
the verdict and the current message stay untouched, so the routed path can re-examine the same
reply. `on_mismatch: "fail"` keeps the legacy failure verdict verbatim; `optional` and
`on_mismatch` conflict (a probe never mismatches) and are rejected at compile.

`on_timeout` is **reserved for phase 2** and rejected at compile: no inactivity timer exists on
client sessions today (the `NIOChannelMonitor` appointment is connect-only), and firing a route
from the scheduler thread would race the lock-free engine. The phase-2 shape is a
selector-worker wake that calls a future `ExchangeScript.timeout()` inline on the session's own
worker.

### 3.3 Validation

`validate` meta: `prefix` / `contains` / `exact` in any combination (short-circuit in that
order), plus an optional `report` key.

```json
{ "validate": { "prefix": "txt:SSH-2.0-", "contains": "txt:OpenSSH", "report": "banner" } }
```

An optional `ignore_case` (default `false`) ASCII-folds **both sides** of the
`prefix`/`contains`/`exact` comparisons — for tokens the protocol itself defines as
case-insensitive (HTTP header names, SMTP verbs/keywords). Folding is `A–Z`→`a–z` only, so
matching stays binary-safe; the `report` text keeps the message's original case. `expect`
matching is always byte-exact — case tolerance is a validation-only option.

An optional **`extract`** block first narrows the message to a **length-prefixed field at a fixed
offset** — `{offset, size (1/2/3/4), endian (default big), adjust}` reads the field length at
`offset` inside the message and takes the bytes that follow it; the matches and the `report` then
apply to the extracted field. Out-of-bounds extraction is a validation failure. This is
declarative framing, not computation — the offset is fixed by the protocol, never derived from a
reply. It is how the SSH definition reports the server's key-exchange name-list out of the binary
`SSH_MSG_KEXINIT` packet:

```json
{ "validate": { "extract": { "offset": 22, "size": 4 }, "contains": "txt:sha2", "report": "kex_algorithms" } }
```

Pass → `validated=true`; with `report` → the matched message text is stored in results under that
key. Fail → `validated=false` + `reason`, and the session is failed with the same cause — the
report and the close cause always agree. The report is complete on both the pass and fail path.

An **`optional`** flag turns the step from assertion into probe: the match outcome is recorded as
a boolean under the `report` key (mandatory with `optional` — compile rejects the combination
without it), the script continues on match and mismatch alike, and `validated`/`reason` are
untouched. Since a validate never clears the current message, several probes can examine the same
reply — the capability/version-matrix idiom: one run reports which protocol variants the endpoint
supports (V1 only, V2 only, both, neither) without any of them failing the session.

An optional **`regex`** is checked **last**, after `prefix`/`contains`/`exact`:

```json
{ "validate": { "regex": "HTTP/1\\.[01] (\\d{3})", "report": "status" } }
```

- The value is a **plain Java regex** — the data-literal prefix rules do *not* apply (regex is
  its own escaping language; a `hex:`/`base64:` pattern is meaningless against decoded text),
  and `${var}`s are rejected, which is what makes `Pattern.compile` a compile-time fail-fast.
- The subject is the (post-`extract`) message decoded **ISO-8859-1** — a 1:1 byte↔char mapping,
  so binary messages match safely — via `Matcher.find()` (contains-style). `ignore_case` never
  folds a regex; use an inline `(?i)`.
- **`group`** selects the reported capture: default group 1 when the pattern captures, else the
  whole match (group 0); an explicit `group` is range-checked at compile. On a match, the
  capture is what `report` stores (it wins over the whole-message text; note the charset
  asymmetry — captures are ISO-8859-1 text, legacy reports UTF-8). A regex mismatch is a normal
  validation mismatch: it fails the session, or routes via `on_mismatch`. With `optional`, the
  boolean contract holds — the probe records match presence, never the capture; to get the
  capture *and* tolerance, use a non-optional regex step with `on_mismatch`.

The capture is **report-only**: it can never feed a send or a later match — reading a value into
the report is observation, not computation, and the no-computation charter stands.

### 3.4 The host seam

The engine's only coupling to transport is a four-method interface implemented by both
validators:

| Method | TCP host | UDP host |
|---|---|---|
| `write(byte[])` | session output stream (plain, or encrypted once secure) | `send` on the connected datagram channel |
| `startTLS()` | inherited `TCPSessionCallback.startTLS(cert_validation)` | rejected at compile — never called |
| `fail(Throwable)` | stash cause, close the session | same |
| `complete()` | `ready=true`; close if `close_on_ready` | same |

---

## 4. Runtime flows

### 4.1 Plain TCP

`connected(SelectionKey)` (final, in `TCPSessionCallback`) installs the plain
`CommonChannelOutputStream` and calls `connectedFinished()` → the validator starts the script
(initial sends go out). Each read dispatch delivers a flipped read-mode buffer to
`accept(ByteBuffer)` → bytes are copied out and fed to the engine. Peer EOF or an I/O error
closes the session; a clean EOF after completion is a normal end.

### 4.2 Immediate TLS

`tls.mode: "immediate"`: `connectedFinished()` immediately calls the inherited
`startTLS(cert_validation)` — the remote address is known at that point, and no read dispatch has
happened yet. `startTLS` builds a client-mode `SSLContextInfo` from the dialed address, swaps the
session output stream for an SSL-armed one, and starts the handshake through
`CustomSSLStateMachine` + the `SSLUtil` handlers (the load-proven `net.ssl` engine — this
subsystem changes nothing there). On completion `sslUpgraded(...)` records
`tls_protocol`/`tls_cipher` in results and starts the script; all script traffic is encrypted.

### 4.3 STARTTLS upgrade

`tls.mode: "on_demand"` + a `start_tls` step: the script runs its plaintext dialogue first
(e.g. `EHLO` → `expect 220`). At the `start_tls` step the engine performs the **injection residue
check**: if any unconsumed plaintext remains past the last expect match (assembler accumulation
or unconsumed inbox), the session is failed — the standard mitigation for STARTTLS command
injection; never clear-and-continue. With zero residue the host's `startTLS()` runs the same
upgrade as §4.2 mid-session; the pump pauses and resumes on `sslUpgraded(...)`, with every
subsequent step encrypted. A `start_tls` step in a definition without an `on_demand` tls block is
a compile-time error.

### 4.4 UDP

The validator is constructed with the peer address; `setChannel` **connects** the datagram
channel to it — stray-source datagrams are dropped by the OS, and ICMP port-unreachable surfaces
as a receive `IOException`, which is **fatal for a client**: the session closes with that cause
(the fast "nothing listening" verdict). `connected(SelectionKey)` starts the script (first
datagram out) before any read dispatch — NIOSocket registers the channel with zero interest ops,
invokes `connected`, then installs the returned ops, so an early reply waits in the OS buffer.
Each received datagram is one message (`datagram` boundary). UDP idioms: one request/response per
probe (no ordering or loss recovery — a lost datagram surfaces as a timeout, not a failure), bake
a recognizable token into the probe and match its echo. No TLS over UDP (no DTLS in this stack).

---

## 5. Buffer ownership & delivery modes

`TCPMetaProtocol` shares its session accumulator with the engine: the pooled `dataAssembler`
(a `UByteArrayOutputStream`) is handed to the `ExchangeScript` constructor,
`accept(ByteBuffer)` appends every delivery straight into it via
`ByteBufferUtil.write(buffer, dataAssembler, true)`, and `script.parse()` frames tokens off the
accumulation — consumed in place via `shiftLeft` — with no intermediate chunk copy. The only
per-message copy is the framed message itself (`copyBytes`), which must survive the next
compaction. The session recaches the accumulator once at `close()`.

`accept(ByteBuffer)` receives data in two delivery modes, normalized by one mechanism — the
session sets `implWillFlipBuffer = true`, so the plain read loop skips its own flip and both
paths deliver **write-mode** buffers:

- **Plain path**: the read loop's own `dataBuffer` — append only; the loop owns and clears it
  per iteration.
- **TLS path**: the session's decrypted buffer, reused across dispatches.

`ByteBufferUtil.write(..., flip = true)` flips the source, drains it fully, and compacts it, so
either reused buffer is ready for its next fill.

`UDPMetaProtocol` allocates one pooled `rawReadBuffer` at construction and runs its own client
receive loop in `accept(SelectionKey)`: drain the channel, hand the engine one detached copy per
datagram via `feed()`, recache the buffer once at `close()`.

Writes are one-shot complete: the session output stream drains the buffer fully (looping via
`smartWrite` / the SSL chunked write) before returning — there is no partial-write queue by
design.

---

## 6. Results & completion contract

The verdict lives in an `NVGenericMap` owned by the engine, exposed via `getResults()`. The
reserved keys below are published as the `ProtoUtil.ResKey` enum (implements `GetName`) —
the engine writes and readers query the bag through those values, never restrung literals:

| Key | Writer | Meaning |
|---|---|---|
| `guid` | construction | run identity: a time-ordered (v7) UUID minted per validator — present in every results bag |
| `proto-name` | construction | the definition's protocol name (`name` key, default `protocol-validator`) |
| `transport` | construction | `tcp` or `udp` |
| `host` / `port` | `recordEndpoint(...)` — the factories, the UDP constructor, TCP connect | the dialed endpoint: host as given (name or IP literal, never a reverse lookup) and port; first value wins |
| `open_ts` | `markOpen()` (connect) | epoch millis when the transport connected (absent if the session never opened) |
| `close_ts` | verdict freeze | epoch millis when the verdict was frozen (completion or failure); first measurement wins |
| `validated` | validate step, or the completion rule | the verdict — present on every completed run |
| `reason` | validate step / failure path | mismatch cause, or `"script completed"` |
| `ready` | completion | the script finished |
| `<report>` | validate step's `report` key | matched message text (e.g. `banner`), a regex capture (§3.3), or the boolean outcome of an `optional` probe |
| `<record>` | a `record` step | the step's constants — typically which branch of a routed script ran (§3.2.1). Case-insensitive names: `record`, `report`, and capture keys differing only by case collide into one entry, last writer wins |
| `tls_protocol` / `tls_cipher` | TLS completion | negotiated session parameters |
| `latency_ms` | completion or failure | connect-to-verdict duration in milliseconds; the clock starts at the host's `markOpen()` (connect — so an immediate handshake is measured) or at script start when never called, and the first measurement wins |

The failure cause is additionally available as `getCloseCause()` (the `Throwable` stashed by
`exception(...)`/`fail(...)`), and always agrees with `validated`/`reason`.

Completion is observed three ways: pull-style with `waitForClose(millis)` (latch released when
the session closes) or by polling `isClosed()`; or event-driven with `onClose(Consumer)` — the
`PQCCheck` idiom — a one-shot hook fired exactly once when the session closes, with the results
bag and close cause final when it fires. Register the hook before handing the validator to
NIOSocket and no thread ever parks on the session; a hook registered on an already-closed
session runs immediately, and a throwing hook never unwinds the closer. The hook runs inside
the close path — hand real work to an executor rather than block in place. Sessions end in
exactly one of three ways: script completion (with `close_on_ready`, self-close), remote
disconnect/EOF, or failure (validation mismatch, residue, I/O error, port-unreachable) — the
results bag and close cause are final before the close completes.

Teardown (both transports): close the channel and session streams, recache session buffers, and
release the completion latch — once only; later closes are no-ops.

---

## 7. `ProtoConnect` — factories & CLI

The programmatic entry points are the `createTCPProtocol` / `createUDPProtocol` factories:
endpoint + definition in (endpoint as `InetSocketAddress`, `"host[:port]"` string, or
`IPAddress`; definition as JSON text or parsed `NVGenericMap`), bound validator out; a missing
port falls back to the definition's `port` hint. The TCP validator carries its remote address
and timeout, so it goes to `NIOSocket.addClientSocket(validator)`; the UDP one to
`addDatagramSocket(new InetSocketAddress(0), validator)`. Every factory also has a
`Consumer<NVGenericMap>` form: the consumer receives the final results bag exactly once when
the session closes — completion, failure, or remote EOF alike — wired through the validator's
`onClose` hook (§6), so it occupies that hook and follows its rules (fires immediately if
already closed, runs inside the close path — hand real work to an executor).

The CLI: `ProtoConnect <definition.json> <host[:port]> [name=value ...]` — loads the
definition, builds the matching validator per its `transport`, injects each `name=value` as a
`${name}` variable, drives it through NIOSocket, and prints the results and the verdict. It is
a pure observer — the validator performs and closes the session itself.

Exit codes: `0` validated; `1` failed (validation mismatch, STARTTLS residue, connection or I/O
error); `2` no completion within the wait window (about twice the definition's `timeout_sec`);
`64` usage error or invalid definition.

---

## 8. Shipped definitions

Protocol shapes are definition files, not code. Shipped under `src/test/resources/protocols/`:

| File | Shape |
|---|---|
| `ssh-banner.json` | delimited banner-only check: `expect "txt:SSH-"` + `validate {prefix "txt:SSH-2.0-", report "banner"}` |
| `ssh-banner-kex.json` | the banner phase of `ssh-banner.json`, then the client ident `send` (RFC 4253 — some servers wait for it), a `boundary` switch to RFC 4253 binary packets (`length_prefixed {offset 0, size 4}`), a `validate` whose `extract {offset 22, size 4}` asserts `sha2` and reports the server's key-exchange name-list from `SSH_MSG_KEXINIT` as `kex_algorithms`, and an `optional` probe on the same extract reporting post-quantum kex support (`sntrup761`) as `pq_kex` |
| `smtp-starttls.json` | `tls {mode on_demand}`; greeting captured as `banner` → EHLO dialogue → `expect` the STARTTLS capability → `start_tls` → post-upgrade EHLO round-trip (RFC 3207 shape) |
| `dns-probe.json` | `transport udp`; one `hex:` DNS query datagram with a fixed transaction id → `validate {contains "hex:<txn id>", report "dns"}` |
| `postgres-ssl-probe.json` | `tls {mode on_demand, cert_validation false}`; the 8-byte `SSLRequest` `send` → `expect "txt:S"` (ask-then-upgrade) → `start_tls` → `boundary` switch to backend messages (`length_prefixed {offset 1, size 4, adjust -4}`) → StartupMessage `send` → `validate {prefix "txt:R"}` reporting the AuthenticationRequest as `auth_request` |
| `https-server.json` | `tls {mode immediate, cert_validation true}`; encrypted `GET` with `${host}` → `expect` the end of the header block → `validate {prefix "txt:HTTP/"}` capturing `response_headers` → `optional` probe `{contains "txt:Server:", ignore_case}` reporting `server` |
| `smtp-posture.json` | the guarded-linear posture probe: greeting captured as `banner` → EHLO dialogue → `expect` block whose main match is the STARTTLS capability with an `alt {match "txt:250 ", goto plaintext_only}` → each branch `record`s `starttls_offered` true/false; both postures complete `validated: true` |
| `ssh-pq-check.json` | the PQ-readiness upgrade of `ssh-banner-kex.json`: banner + kex-list capture, per-algorithm `optional` probes (`pq_sntrup761`, `pq_mlkem`), and a guarded-linear branch folding them into one `pq_supported` boolean — every posture completes `validated: true` |
| `http-status-regex.json` | regex capture: `GET` with `${host}` → `validate {regex "HTTP/1\\.[01] (\\d{3})", report "status", on_mismatch "not_http"}` — the routed path `record`s `http: false` |

A new protocol check is a new JSON file. Code is only ever added for behavior the schema cannot
express (loops, computed values, multi-connection logic) — and such a check belongs in
purpose-written classes, not in this engine.

---

## 9. Build & test

```
mvn compile
mvn test -DskipTests=false -Dtest="org.zoxweb.server.net.protocols.*Test" -DfailIfNoTests=false
java -cp target\test-classes;target\classes;<gson>;<uuid-creator> \
     org.zoxweb.server.net.protocols.DNSProbeTest 8.8.8.8 53   # live check vs a public resolver
```

Tests are skipped by default (`skipTests=true` in the pom) — `-DskipTests=false` is required.
GPG signs at `verify`; use `package`/`test` for development. Test keystore for the TLS suites:
`src/test/resources/test.zoxweb.org.jks` — PKCS12, storepass `password`, alias `selfsigned`.

Test coverage: hermetic engine tests (no sockets: framing, pump, fail-fast matrix, encodings,
routing — alt/jump/on_mismatch/record incl. the residue check on routed paths — and regex
capture); loopback integration for plain exchange, immediate TLS (against
`SSLNIOSocketHandlerFactory`), STARTTLS positive + injection negative, the branched SMTP
posture probe (`SMTPPostureLoopbackTest`, one definition against both server postures), UDP
exchange, and the DNS probe (hermetic responder plus
a live `main`).
