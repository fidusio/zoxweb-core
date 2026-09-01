# PROTOCOL-INFO — skill: authoring JSON protocol-validator definitions

You are given this skill so you can **generate a JSON definition** for a meta-driven protocol
validator. A definition is a single JSON object. A generic validator runtime consumes it, opens
**one** connection to **one** operator-supplied endpoint, runs the short scripted
request/response dialogue the definition declares, records a pass/fail verdict, and closes.
Your entire job is to produce that JSON object — nothing else. The runtime is a black box; you
never write code, and the definition never names a host.

When prompted like *"based on the meta skill, generate an SMTP protocol validator"*, respond with
one complete, valid JSON definition following this skill, plus (if asked) a short explanation of
each step.

---

## 1. What a definition can and cannot do

A definition is a **conformance check**, not a protocol client:

- **One endpoint.** The operator supplies the address at run time. The definition may declare the
  protocol's well-known port(s) as a *hint*, never a target.
- **Guarded linear script.** The dialogue is a fixed list of steps executed top to bottom with a
  **forward-only** cursor. Limited routing exists (§5.5): an `expect` may declare alternative
  patterns routed to a forward `label`, a `validate` may route a mismatch with `on_mismatch`,
  and `jump` skips forward — reserved targets `done`/`fail`. Routes may never point backward,
  so loops are impossible. There is **no computing**: a `validate` may *capture* text with a
  `regex` into the report (§5.3), but a captured value can never feed a later send or match. If
  the check you are asked for needs to read a value and act on it, say so: it is out of scope
  for a definition.
- **Single-shot.** No retries, no backoff, no credentials, no rate manipulation. The session
  identifies itself normally (e.g. a normal EHLO/client-ident line).
- **Safe TLS by default.** Certificate-chain validation is on unless the definition disables it.
  Keep it on when chain trust is part of the verdict — public/WebPKI endpoints, HTTPS-style
  checks. Set `cert_validation: false` for the very common self-signed case — internal services,
  databases (a stock PostgreSQL install is self-signed), mail servers — and whenever the check is
  "what does this endpoint negotiate?" rather than "is the chain trusted?": with validation on,
  a self-signed peer fails the handshake itself and the posture question never gets answered.

---

## 2. The execution model — what the runtime does with your JSON

Understand this lifecycle; every authoring rule below follows from it.

1. **Connect.** The runtime dials the operator's endpoint. Nothing in the script has run yet.
2. **Session start.**
   - If the definition declares `tls.mode = "immediate"`, the TLS handshake runs **first**; the
     script starts only once the link is encrypted (so even step 0's `send` goes out encrypted).
   - Otherwise the script starts immediately on the plaintext link.
   Starting the script means: execute steps from the top until a step **must wait** — an
   `expect`/`validate` with nothing to match yet, or a `start_tls` handshake. Leading `send`
   steps therefore go out immediately on connect (e.g. a UDP probe's first datagram, an SSH
   client-ident line).
3. **Data arrives.** Received bytes are appended to an internal **assembly buffer**. The
   definition's **boundary strategy** (§4) decides when accumulated bytes form a complete
   message. Chunking is invisible: a reply may arrive in one piece or byte-by-byte and the script
   behaves identically — a waiting step simply re-evaluates whenever new data lands, and waits
   again if its match is still incomplete.
4. **Steps advance.** Each time a message is framed (or, for the `stream` boundary, whenever the
   accumulation grows), the waiting step re-checks. A matched `expect` advances the cursor and
   execution continues to the next step (further sends go out, the next expect waits, ...).
5. **TLS upgrade (`start_tls` step).** The script pauses, the handshake runs on the same
   connection, and the script **resumes at the next step encrypted**. Before upgrading, the
   runtime enforces the STARTTLS anti-injection rule: any unconsumed plaintext left over past the
   last match **fails the session** (never clear-and-continue) — see §5.4.
6. **Completion.** The step list finishing = the check passed. The runtime records the verdict,
   marks the session ready, and (if `close_on_ready`) closes it. Later bytes are ignored.
7. **Failure.** A validation mismatch, an oversized message, an unresolved variable, an I/O
   error, or (UDP) an ICMP port-unreachable fails the session immediately with a recorded reason.
8. **Peer close.** A clean disconnect before the script completes ends the session with **no
   verdict** — the absent `ready` flag tells the operator the dialogue never finished (for UDP, a
   lost datagram surfaces as a timeout, not a failure).

**The result** is a flat report the operator reads after the session closes:

| Key | Meaning |
|---|---|
| `validated` | `true`/`false` — present on every run that completed or failed |
| `reason` | the failure cause, or `"script completed"` when no validate step ran |
| `ready` | `true` — the script ran to completion (absent on failure/incomplete) |
| *your `report` keys* | the matched text or regex capture a `validate` step stored, or the `true`/`false` outcome of an `optional` probe (§5.3) |
| *your `record` keys* | the constants a `record` step merged — typically which branch of a routed script ran (§5.5) |
| `tls_protocol`, `tls_cipher` | the negotiated TLS session, when TLS ran |
| `latency_ms` | connect-to-verdict duration in milliseconds (TLS handshake and dialogue included), recorded on every completed or failed run |

A run with no `validate` step still yields `validated: true, reason: "script completed"` —
reaching the end of the dialogue is itself the check.

---

## 3. Top-level schema

```json
{
  "name": "smtp-starttls",
  "transport": "tcp",
  "port": [25, 587],
  "timeout_sec": 5,
  "close_on_ready": true,
  "tls": { "mode": "on_demand", "cert_validation": true },
  "assembler": { "boundary": "stream" },
  "vars": { "helo": "probe.local" },
  "exchange": [ ...steps... ]
}
```

| Key | Type | Default | Rules |
|---|---|---|---|
| `name` | string | generic | label for reports/logs |
| `transport` | `"tcp"` \| `"udp"` | `tcp` | UDP forbids `tls` and `start_tls` (no DTLS) |
| `port` | int or int array | none | **hint only, never an endpoint**. An array lists every well-known port (`[25, 587]` for SMTP STARTTLS); the **first** entry is the default used when the operator omits a port. Each value must be 1–65535; an empty array is invalid |
| `timeout_sec` | int | 5 | connect-timeout hint |
| `close_on_ready` | boolean | `true` for udp, `false` for tcp | close the session as soon as the script completes — set `true` for probes |
| `tls` | object | absent = plaintext | `mode`: `"immediate"` (handshake before the script) or `"on_demand"` (upgrade at the script's `start_tls` step). `cert_validation` defaults `true` (chain trust is part of the check — public HTTPS/WebPKI); set `false` for self-signed/internal endpoints — the common case outside public HTTPS — or the handshake itself fails before the posture is measured |
| `assembler` | object | by transport | message framing, §4 |
| `vars` | object of strings | — | defaults for `${name}` placeholders, §6 |
| `exchange` | array of steps | — | the guarded linear script, §5 |

Invalid combinations are rejected before any connection: unknown transport/mode/boundary/op,
`udp` + `tls`, `start_tls` without an `on_demand` tls block, `start_tls` with `immediate` (the
link is already secure), malformed data literals, out-of-range ports. Routing and capture:
duplicate/empty/reserved labels, unknown or backward route targets, an expect block without
`match`, an empty `alt` list or an alt entry missing `match`/`goto`, `on_timeout` (reserved),
`optional`+`on_mismatch`, a malformed regex or `${var}` inside one, `group` without a regex or
out of the pattern's range, an empty `record` block or a `record` key naming a verdict key.

**Key names are case-insensitive** at every nesting level — `"Port"`, `"PORT"`, and `"port"` are
the same key (write lowercase anyway, as every example does). Two consequences: never give two
`report` keys names differing only by case (they collide into one results entry), and note the
tolerance applies to *keys* only — data literals (§6) are matched byte-exact unless a `validate`
opts into `ignore_case` (§5.3).

---

## 4. Assembly — how raw bytes become messages

The `assembler` block declares the framing. One strategy is active at a time (a `boundary` step
can switch it mid-script, §5.6).

```json
"assembler": { "boundary": "delimited", "terminator": "txt:\n", "strip_cr": true, "max_message": 255 }
```

| `boundary` | One message is... | Default for | Extra keys |
|---|---|---|---|
| `datagram` | one received datagram, as-is (no accumulation) | **UDP** | — |
| `delimited` | the bytes up to (excluding) a terminator sequence, detected across chunks | — | `terminator` (data literal, default `txt:\r\n`), `strip_cr` (drop one trailing CR before the terminator — CR-tolerant line protocols) |
| `length_prefixed` | a whole binary frame whose header carries the payload length | — | `length: { "offset": 0, "size": 1\|2\|3\|4, "endian": "big"\|"little", "adjust": 0 }` — the frame is `offset + size + parsed-length + adjust` bytes, delivered whole (header included); `size: 3` covers 24-bit headers (the HTTP/2 frame shape) |
| `stream` | there is no framing: the unconsumed accumulation itself is matched against, and each match **consumes through its end** | **TCP** | — |

`max_message` (default 65536) caps the accumulation/message size — a breach fails the session, so
a hostile peer cannot exhaust memory. `max_skip` (default 65536) bounds framed-message skipping
(§5.2).

**Choosing a boundary:** line protocols (SMTP, POP3, FTP banners, SSH ident) → `delimited` when
you need clean whole-line messages, or `stream` when consume-through-match phrasing is simpler;
binary packet protocols (SSH transport, many TLVs) → `length_prefixed`; UDP → `datagram` (leave
the default).

---

## 5. The `exchange` script — step semantics

Each step is a one-key object. Steps run strictly in order.

### 5.1 `{"send": "<literal>"}`

Writes the decoded literal to the session (encrypted automatically once the link is secure).
`${vars}` resolve at send time. A send also **opens a new request/response round**: whatever
message an earlier step matched is forgotten, so a following `validate` always examines the
*reply to this send*, never stale data.

### 5.2 `{"expect": "<literal>"}` — the wait-and-match step

Waits until the expected byte sequence appears, with boundary-specific semantics you must get
right:

- **`stream`**: contains-match against the unconsumed accumulation; on match, everything from the
  start of the accumulation **through the end of the match is consumed**, and that consumed span
  becomes the "current message" for a following `validate`.
  **Authoring rule:** always expect **through the end of the unit you care about** — usually the
  line terminator. `{"expect": "txt:220 "}` leaves the rest of the greeting line unconsumed,
  which (a) pollutes a following `validate`'s view and (b) trips the `start_tls` residue check.
  Prefer `{"expect": "txt:220 ready\r\n"}`, or the two-step idiom
  `{"expect": "txt:220 "}` then `{"expect": "txt:\r\n"}` to consume through an
  unpredictable line's end.
- **`delimited` / `length_prefixed` / `datagram`**: per-message contains-match. A complete
  message that does **not** contain the sequence is *skipped* and the step keeps waiting — this
  is how multi-line continuations work (SMTP `250-...` lines skipped until the `250 ` final
  line). Total skipped bytes are bounded by `max_skip`; exceeding it fails the session. The
  matched message becomes the "current message".

### 5.3 `{"validate": { ... }}` — the verdict step

Examines the **current message** — the one the last `expect` matched; if no expect preceded it in
this round, the next complete framed message (or, on `stream`, a non-consuming snapshot of the
accumulation). Meta keys, all optional, checked in order with short-circuit:

```json
{ "validate": { "prefix": "txt:SSH-2.0-", "contains": "txt:OpenSSH", "exact": null,
                "report": "banner",
                "extract": { "offset": 22, "size": 4, "endian": "big", "adjust": 0 } } }
```

- `prefix` / `contains` / `exact` — byte-level matches (data literals, `${vars}` allowed).
- `ignore_case` — `true` ASCII-folds (`A–Z`/`a–z` only) **both sides** of the
  `prefix`/`contains`/`exact` matches. Use it only when the protocol itself defines the token as
  case-insensitive — HTTP header names (`Server:` vs `server:`), SMTP verbs and capability
  keywords — never for binary literals or case-significant payloads. `report` still stores the
  message in its original case. `expect` steps stay byte-exact: anchor them on case-neutral
  bytes (line terminators, digits, framing) and leave case tolerance to the validate.
- `report` — on success, store the matched text in the results under this key (this is how a
  probe *captures* a banner, a capability line, an algorithm list).
- `optional` — `true` turns the step from an **assertion** into a **probe**: the match outcome
  is recorded as `true`/`false` under the `report` key (mandatory with `optional` — rejected at
  compile without it), the script continues on match and mismatch alike, and
  `validated`/`reason` are never touched. This is the *flexible* mode: an assertion says "the
  endpoint must", a probe says "tell me whether the endpoint does".
- `extract` — for **binary** messages: first narrow the message to a **length-prefixed field at a
  fixed offset** (read a 1/2/3/4-byte length at `offset`, take the `length + adjust` bytes that
  follow it); the matches and `report` then apply to the extracted field. The offset must be
  fixed by the protocol specification — extraction is declarative framing, not computation.
  Out-of-bounds extraction is a validation failure.
- `regex` — checked **last**, after `prefix`/`contains`/`exact`: a **plain Java regex** (the
  `txt:`/`hex:` prefix rules do *not* apply, and `${var}`s are rejected — the pattern compiles
  fail-fast at load). It runs contains-style (`find()`) against the post-`extract` message
  decoded ISO-8859-1, so binary messages match safely; `ignore_case` never folds a regex — use
  an inline `(?i)`. `group` picks the reported capture (default: group 1 when the pattern
  captures, else the whole match; range-checked at load). On a match, the capture is what
  `report` stores. The capture is **report-only** — it can never feed a send or a later match.
  With `optional`, the boolean contract holds: the probe records match presence, never the
  capture (want both capture and tolerance? use a non-optional regex with `on_mismatch`).

  ```json
  { "validate": { "regex": "HTTP/1\\.[01] (\\d{3})", "report": "status" } }
  ```
- `on_mismatch` — route the mismatch to a forward `label` (or `done`/`fail`) instead of failing
  the session (§5.5): the verdict and the current message stay untouched, so the routed path
  can re-examine the same reply. `on_mismatch: "fail"` keeps the normal failure verdict.
  Conflicts with `optional` (a probe never mismatches) — rejected at load.

A mismatch **fails the session** (unless `optional`, or routed by `on_mismatch`):
`validated: false` plus a `reason`, and the connection closes. A pass records
`validated: true`. Multiple validate steps are allowed; typically one per captured artifact.

**Pattern — the capability/version matrix.** Because a validate does not clear the current
message, several probes can examine the *same* reply: mandatory conformance as an assertion,
per-version support as probes. One run then reports V1 only, V2 only, both, or neither — none of
which fails the session; if every step completes, the verdict is still
`validated: true, reason: "script completed"` and the booleans carry the answer:

```json
{ "expect":   "txt:\r\n" },
{ "validate": { "prefix":   "txt:CAP",  "report": "capabilities" } },
{ "validate": { "contains": "txt:V1", "optional": true, "report": "v1_supported" } },
{ "validate": { "contains": "txt:V2", "optional": true, "report": "v2_supported" } }
```

**Pattern:** to validate a whole reply line on `stream`, expect through the terminator first,
then validate — the current message is then the entire consumed line:

```json
{ "send":     "txt:EHLO ${helo}\r\n" },
{ "expect":   "txt:\r\n" },
{ "validate": { "contains": "txt:250", "report": "ehlo_reply" } }
```

### 5.4 `{"start_tls": true}` — mid-session TLS upgrade

Requires `tls: { "mode": "on_demand" }`. The runtime first checks that **zero unconsumed bytes**
remain (accumulation residue or an unconsumed framed message): leftover plaintext behind the
server's go-ahead is the classic STARTTLS command-injection shape, and the session is failed
rather than continued. Then the handshake runs; the script resumes at the next step with every
subsequent send/expect encrypted, and `tls_protocol`/`tls_cipher` recorded.

**Authoring rule:** the steps before `start_tls` must consume the go-ahead line completely
(§5.2's expect-through-the-line rule) — that is what makes the residue check pass on a
well-behaved peer.

**Ask-then-upgrade — never `start_tls` blind.** The step itself performs the handshake
unconditionally; it does not probe willingness. Whether the peer *offers* the upgrade is a
protocol question, so the script must ask it in the protocol's own vocabulary and reach
`start_tls` only after matching — and fully consuming — the affirmative answer:

- *Text protocols* verify the capability is advertised, then request the upgrade and consume the
  go-ahead line (the SMTP shape):

  ```json
  { "send":   "txt:EHLO ${helo}\r\n" },
  { "expect": "txt:STARTTLS" },
  { "expect": "txt:\r\n" },
  { "send":   "txt:STARTTLS\r\n" },
  { "expect": "txt:220 " },
  { "expect": "txt:\r\n" },
  { "start_tls": true }
  ```

  The first expect pair proves the upgrade is *offered*; the second pair proves it is *granted*.

- *Binary protocols* send the protocol's upgrade request and match its affirmative reply — the
  PostgreSQL shape (8-byte `SSLRequest`, one-byte answer, `S` = willing / `N` = refused):

  ```json
  { "send":   "hex:00000008 04D2162F" },
  { "expect": "txt:S" },
  { "start_tls": true }
  ```

Because these expects consume exactly the affirmative bytes, the residue check is satisfied as a
side effect — the go-ahead rule and the residue rule are the same discipline.

**When the peer refuses.** Two ways to author it. As an *assertion*, a refusal (a `454`, an
`N`, a capability line that never mentions the upgrade) simply never matches, and the session
ends by timeout or peer close with no `ready` flag — the operator reads "script did not
complete" as "upgrade not available"; to assert the opposite posture, author a separate
definition that expects the refusal bytes instead. As an *observation*, route the refusal with
an expect alternative (§5.5) — match the affirmative on the main path, `goto` a branch on the
refusal bytes, and `record` which posture was seen; both paths complete `validated: true` and
the `record` key carries the answer.

**Trap: a `validate` right after `start_tls` sees pre-TLS bytes.** `start_tls` does not open a
new request/response round — only a `send` does — so the current message crossing the upgrade is
still whatever the *plaintext* go-ahead expect matched. A matcher-less
`{"validate": {"report": "tls_negotiated"}}` placed after the upgrade therefore passes vacuously
and reports the stale go-ahead bytes under a misleading key; it never examines the TLS session.
The negotiated parameters need no validate step at all — `tls_protocol`/`tls_cipher` are recorded
automatically when the handshake completes. Capture the go-ahead (if wanted) with a validate
*before* `start_tls` under an honest key, and after the upgrade only validate replies to a new
encrypted `send`.

### 5.5 Routing — `label`, `jump`, expect alternatives, `record`

The guarded-linear routing vocabulary. Every route target is a `label` name (case-insensitive,
unique), or the reserved `done` (complete the script now) / `fail` (fail the session). **Routes
are forward-only** — a backward or self target is rejected at load, so loops are impossible.

- `{"label": "name"}` — a no-op jump target.
- `{"jump": "target"}` — unconditional forward jump. `{"jump": "done"}` ends the main path
  before the branch tail.
- `{"record": {"key": value, ...}}` — merge constant strings/booleans/numbers into the results;
  the way each branch marks which path ran. The verdict keys (`validated`, `reason`, `ready`,
  `latency_ms`) are rejected at load. Keys are case-insensitive — a `record` key colliding with
  a `report` key merges into one entry, last writer wins.
- **Expect alternatives** — the block form of `expect`. Steps stay single-key objects, so the
  alternatives live *inside* the value (a byte literal that genuinely starts with `{` must be
  written with the `txt:` prefix):

  ```json
  { "expect": { "match": "txt:STARTTLS",
                "alt": [ { "match": "txt:250 ", "goto": "plaintext_only" } ] } }
  ```

  The main `match` behaves exactly like the string form and **always wins over the
  alternatives** (regardless of byte position on `stream`); alts are tried in declaration
  order. On `stream`, the matching pattern (main or alt) consumes through its own end and
  becomes the current message — an alt feeding a later `start_tls` must consume through the
  line terminator, or the residue check fails the upgrade (§5.4, by design). On framed
  boundaries, a message matching *neither* main nor any alt is **skipped** as usual
  (`max_skip`-bounded) — the `250-` continuation idiom survives, so author alt patterns that
  cannot occur in continuation lines.

The one-definition-one-posture rule (§5.4) softens accordingly: a definition may now *observe*
both postures in one run — match the affirmative on the main path, route the refusal to a
branch, and `record` which one happened — with both paths ending `validated: true` and the
`record` key carrying the answer. Reserve hard assertions (an unrouted `expect`/`validate`) for
what must be true on every path. `on_timeout` is reserved for a future phase and rejected at
load — a peer that answers nothing still ends as timeout/no-`ready`.

### 5.6 `{"boundary": { ...assembler block... }}` — switch framing mid-script

Replaces the active framing from this step on; residue in the accumulation reframes under the new
rule. TCP only. Use it when a protocol changes shape mid-dialogue — e.g. a text banner line
followed by binary packets:

```json
{ "boundary": { "boundary": "length_prefixed", "max_message": 35000,
                "length": { "offset": 0, "size": 4 } } }
```

---

## 6. Data literals and variables

Every literal (`send`, `expect`, `terminator`, `validate` matches) is a string with an optional
one-word encoding prefix, split at the **first colon**:

- `txt:` — UTF-8 verbatim (control bytes via JSON escapes: `"txt:PING\r\n"`).
- `hex:` — hex digits, whitespace ignored: `"hex:1234 0100 0001"` → bytes `12 34 01 00 00 01`.
- `base64:` — Base64 body.
- **No recognized prefix** — the whole string, colon included, is UTF-8 text (`"USER: bob"` is
  just text).

Bodies may contain `${name}` placeholders resolved from `vars` (or operator injection) when the
step runs; a missing or empty variable fails the session. The two layers never mix: the prefix is
identified first and substitution happens only in the body — a variable's *value* can never
become an encoding directive. Use variables for anything caller-specific (HELO names, tokens),
so the definition stays generic.

---

## 7. UDP specifics

- The socket is *connected* to the single peer: stray-source datagrams are dropped, and an ICMP
  port-unreachable fails the session immediately — the fast "nothing is listening" verdict.
- One datagram = one message (`datagram` boundary, the default). Design one request → one reply:
  UDP gives the script no ordering or loss recovery; a lost datagram is a timeout, not a failure.
- Bake a recognizable token into the probe and match its echo (e.g. a fixed DNS transaction id).
- No TLS, no `start_tls`, no `boundary` switch over UDP.

---

## 8. Authoring checklist

1. Pick the transport; declare the protocol's well-known `port` hint(s) (array if several, most
   common first).
2. Pick the boundary from the protocol's shape (§4); set a realistic `max_message`.
3. Script the shortest dialogue that proves conformance: greeting → identify → (optional
   capability check) → (optional `start_tls`) → validate → done. Shorter is better.
4. On `stream`, every `expect` consumes through the end of its line/unit (§5.2) — mandatory
   before `start_tls` and before any `validate` that should see the whole line.
5. Never `start_tls` blind: match the peer's affirmative go-ahead first (ask-then-upgrade,
   §5.4) so the handshake only runs against a peer that agreed to it.
6. Capture what the operator wants to see with `report` keys (banner, capability line,
   negotiated list).
7. Set `close_on_ready: true` for probes (UDP already defaults to it).
8. Pick `cert_validation` by what the check asserts: `true` when chain trust is part of the
   verdict (public HTTPS/WebPKI endpoints); `false` for self-signed/internal endpoints — the
   common case outside public HTTPS — and for negotiation-posture checks, where a failed chain
   would mask the answer.
9. Route only what the check *observes*; assert what must hold on every path. Every branch
   should `record` which path ran, alt patterns must not occur in framed continuation lines
   (§5.5), and a stream alt feeding `start_tls` must consume through the line terminator.
   Capture with `regex` only what the operator wants to *read* — a capture can never be acted
   on.
10. Never: hostnames/IPs in the definition, backward routes or loops, computed values,
    credentials, retries, more than one connection's worth of dialogue.

---

## 9. Worked examples

### 9.1 SMTP STARTTLS check (TCP, mid-session upgrade, RFC 3207 shape)

```json
{
  "name": "smtp-starttls",
  "port": [25, 587],
  "timeout_sec": 5,
  "close_on_ready": true,
  "tls": { "mode": "on_demand", "cert_validation": false },
  "vars": { "helo": "probe.local" },
  "exchange": [
    { "expect": "txt:\r\n" },
    { "validate": { "prefix": "txt:220", "report": "banner" } },
    { "send": "txt:EHLO ${helo}\r\n" },
    { "expect": "txt:STARTTLS" },
    { "expect": "txt:\r\n" },
    { "send": "txt:STARTTLS\r\n" },
    { "expect": "txt:220 " },
    { "expect": "txt:\r\n" },
    { "start_tls": true },
    { "send": "txt:EHLO ${helo}\r\n" },
    { "expect": "txt:\r\n" },
    { "validate": { "contains": "txt:250", "report": "post_tls_ehlo" } }
  ]
}
```

Reading it: consume the greeting through its terminator and capture it as `banner` (validating
the `220` prefix); EHLO; wait until a capability line mentions STARTTLS and consume through its
end; request the upgrade; consume the go-ahead line completely (residue check passes);
handshake; re-EHLO encrypted per RFC 3207; validate and capture the encrypted reply.
`cert_validation` is off because mail servers are routinely self-signed and the check is
STARTTLS posture, not chain trust (§1).

### 9.2 SSH banner + key-exchange capture (TCP, framing switch + binary extract)

```json
{
  "name": "ssh-banner",
  "port": 22,
  "timeout_sec": 5,
  "close_on_ready": true,
  "assembler": { "boundary": "delimited", "terminator": "txt:\n", "strip_cr": true, "max_message": 255 },
  "exchange": [
    { "expect": "txt:SSH-" },
    { "validate": { "prefix": "txt:SSH-2.0-", "report": "banner" } },
    { "send": "txt:SSH-2.0-generic_probe\r\n" },
    { "boundary": { "boundary": "length_prefixed", "max_message": 35000,
                    "length": { "offset": 0, "size": 4 } } },
    { "validate": { "extract": { "offset": 22, "size": 4 },
                    "contains": "txt:sha2", "report": "kex_algorithms" } }
  ]
}
```

Reading it: the ident line is CR-tolerant `\n`-delimited text — validate and capture it; send our
own ident (the protocol requires both sides to send; some servers wait for it); switch to the
protocol's binary packets (4-byte big-endian length prefix); the first packet is the
key-exchange announcement, whose algorithm name-list is a length-prefixed ASCII field at fixed
offset 22 — extract it, sanity-match, and report it. (A post-quantum policy check is one edit:
`"contains": "txt:sntrup761"`.)

### 9.3 DNS probe (UDP, single round trip, token echo)

```json
{
  "name": "dns-probe",
  "transport": "udp",
  "port": 53,
  "timeout_sec": 3,
  "exchange": [
    { "send": "hex:1234 0100 0001 0000 0000 0000 07 6578616d706c65 03 636f6d 00 0001 0001" },
    { "expect": "hex:1234" },
    { "validate": { "contains": "hex:1234", "report": "dns" } }
  ]
}
```

Reading it: one canned A-record query for `example.com` with transaction id `0x1234`; the reply
datagram must echo the id; capture it. `close_on_ready` and the `datagram` boundary are the UDP
defaults.

### 9.4 SMTP posture probe (guarded-linear routing + record)

```json
{
  "name": "smtp-posture",
  "port": [25, 587],
  "timeout_sec": 5,
  "close_on_ready": true,
  "vars": { "helo": "probe.local" },
  "exchange": [
    { "expect": "txt:\r\n" },
    { "validate": { "prefix": "txt:220", "report": "banner" } },
    { "send": "txt:EHLO ${helo}\r\n" },
    { "expect": { "match": "txt:STARTTLS",
                  "alt": [ { "match": "txt:250 ", "goto": "plaintext_only" } ] } },
    { "expect": "txt:\r\n" },
    { "record": { "starttls_offered": true } },
    { "jump": "done" },
    { "label": "plaintext_only" },
    { "expect": "txt:\r\n" },
    { "record": { "starttls_offered": false } }
  ]
}
```

Reading it: consume the greeting and capture it as `banner`; EHLO; wait on the capability reply
— a line mentioning
STARTTLS takes the main path, while the final `250 ` line without it routes to
`plaintext_only` (the `250-` continuations match neither and keep waiting). Each branch
consumes through its line's end, `record`s the posture, and completes — **both** postures end
`validated: true, ready: true`; the operator reads the answer from `starttls_offered`. Note
`jump: "done"` ending the main path before the branch tail, and that this probe only
*observes* the posture — it never upgrades, so no `tls` block is needed.

### 9.5 HTTP status capture (regex + on_mismatch)

```json
{
  "name": "http-status-regex",
  "port": [80, 8080],
  "timeout_sec": 5,
  "close_on_ready": true,
  "vars": { "host": "example.com" },
  "exchange": [
    { "send": "txt:GET / HTTP/1.1\r\nHost: ${host}\r\nConnection: close\r\n\r\n" },
    { "expect": "txt:\r\n\r\n" },
    { "validate": { "regex": "HTTP/1\\.[01] (\\d{3})", "report": "status", "on_mismatch": "not_http" } },
    { "jump": "done" },
    { "label": "not_http" },
    { "record": { "http": false } }
  ]
}
```

Reading it: one canned request; consume through the header block; the regex captures the status
code into `status` (group 1 is the default when the pattern captures). A non-HTTP reply routes
to `not_http` and `record`s `http: false` instead of failing the run.

### 9.6 Minimal reachability check (no validate step)

```json
{
  "name": "greeting-check",
  "port": 21,
  "close_on_ready": true,
  "exchange": [ { "expect": "txt:220 " } ]
}
```

Completing the script *is* the verdict: the report reads `validated: true, reason: "script
completed", ready: true`.
