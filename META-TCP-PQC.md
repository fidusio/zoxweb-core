# META-TCP-PQC — the `TCPPQCProtocol` TLS-posture auditor

**Home:** `org.zoxweb.server.net.protocols.pqc` — the one package in the tree allowed to import
Bouncy Castle (`TCPPQCProtocol` the session, `PQCUtil` the BC seam).
**Status:** the authoritative contract for the implementation.

---

## 1. Purpose and scope

`TCPPQCProtocol` is a **client-side TLS-posture auditor**: it opens a connection to one
operator-supplied endpoint, drives the TLS handshake, introspects everything the handshake
reveals — negotiated protocol and cipher, post-quantum key-exchange posture, the full
certificate chain with validity and expiration — records a structured report, and closes. It
sends no application data and expects none.

It is the **purpose-written-code complement** to the JSON meta-protocol validators
(META-PROTOCOL.md): the meta grammar deliberately cannot branch, compute, or evaluate
certificates; posture auditing needs all three. What the two share is the operational shape —
endpoint in, `NVGenericMap` report out, session closes itself, verdict read after
`waitForClose`.

---

## 2. Architecture — two layers

| Layer | Class | One instance means |
|---|---|---|
| Session | `TCPPQCProtocol extends TCPSessionCallback` | **one connection, one handshake, one observation** |
| Sweep | `PQCSweep` (orchestrator, plain class) | N sessions against the same endpoint with different offers; aggregates the matrix |

The split is forced by TLS itself: one handshake negotiates one protocol version and one
key-exchange group. "Which versions does the server support" and "does it accept a PQ-only
offer" are answerable only by connecting repeatedly with **pinned offers** and observing which
handshakes complete. The session class never loops; the sweep owns repetition and aggregation.

---

## 3. The session — override contract

`TCPPQCProtocol` rides the zoxweb-core client TLS path unchanged
(`NIOSocket.addClientSocket(callback)` → `connected()` → engine driver → completion chain). The
SSL driver (`SSLUtil`, `SSLSessionConfig`, `CustomSSLStateMachine`) is **never modified or
subclassed** — the auditor is a completion target, nothing more.

| Member | Contract |
|---|---|
| constructor | takes the endpoint and an `SSLContextInfo` (§6) — pre-set via `setSSLContextInfo(...)`, so the upgrade happens at connect time (`connected()` → `internalSSLUpgrade`), before any read dispatch |
| `connectedFinished()` | not reached on the pre-set-context path; implement as no-op (defensive) |
| `sslUpgraded(SSLConfigInt sci)` | **the single introspection point** — the designed completion seam (`_finished` → `notifySSLHandshakeFinished` → `sslHandshakeSuccessful` → here). All of §5's capture and evaluation runs here, then the session closes itself |
| `accept(ByteBuffer)` | ignore — the auditor exchanges no application data |
| `exception(Throwable)` | record the failure into the report (handshake refusal is itself a finding, e.g. "TLSv1.0 offer rejected" — see §7), stash the close cause, close once |
| lifecycle | `NVGenericMap` results bag + close latch + `waitForClose(millis)` / `getCloseCause()`, mirroring `TCPMetaProtocol`; `onClose(hook)` is the push-style twin — fired exactly once from the close path, so an event-driven consumer never parks a thread on the session |

A handshake-failure path must distinguish, in the report, *refused offer* (server declined the
pinned protocol/group — a sweep data point) from *transport failure* (connect refused, timeout —
no posture information).

---

## 4. Trust model — observe first, evaluate offline

The auditor's handshake runs **trust-all** so it completes against any endpoint — self-signed,
expired, wrong-host — and always captures the chain. Certificate judgment is then performed
**offline from the captured chain**, and *reported*, never thrown:

- **chain evaluation**: PKIX validation of the captured chain against a caller-supplied trust
  store (default: the JVM trust store) → `chain_trusted` plus the failure reason when false
  (untrusted root / self-signed / expired at validation time / broken chain order);
- **hostname**: the dialed hostname checked against the leaf's SANs/CN → `hostname_match`;
- **temporal**: `notBefore`/`notAfter` per certificate, days-to-expiry for the leaf, and an
  `expires_soon` flag against a caller-supplied threshold (default 30 days).

This is the deliberate inverse of the meta-validators' `cert_validation: true` fail-fast mode:
an auditor that dies at the handshake reports nothing.

---

## 5. What one session captures

All introspection is **best-effort**: any single extraction failing is recorded as absent, never
fails the session.

1. **Negotiated session** — `sci.getSSLEngine().getSession()`: protocol, cipher suite.
2. **PQC key exchange** — the negotiated named group, obtained reflectively from the engine when
   the provider exposes it (BCJSSE's extended interfaces; stock JSSE does not expose it —
   absent is a legal answer). `pqc_kex` is true when the group is an ML-KEM / hybrid group
   (name-matched against a maintained list: `X25519MLKEM768`, `SecP256r1MLKEM768`, …).
3. **Certificate chain** — `getSession().getPeerCertificates()`, per certificate: subject,
   issuer, serial, signature algorithm, public-key algorithm and size, `notBefore`/`notAfter`.
   `pqc_cert` is true when the leaf's signature algorithm is a PQ algorithm (ML-DSA / SLH-DSA
   families, name- or OID-matched). The reported chain is completed with the trust-store root
   that issued the last sent certificate when it can be identified (servers don't send the
   root); `chain_time_valid` covers every certificate in the completed chain.
4. **Chain evaluation** per §4.
5. **ALPN** — the session offers `h2, http/1.1` (browser-like) and records the negotiated
   protocol when the server selects one.
6. **Stapled OCSP** — the session requests `status_request` (RFC 6066); a stapled response is
   captured (`ocsp_stapled`) and retained for the sweep's revocation check.
7. **Client-certificate demand** — a recording key manager flips `client_cert_requested` when
   the server sends a CertificateRequest; no certificate is offered (no mTLS), and the demand
   is reported even when the server then aborts the session.
8. **Timing** — connect-to-introspection duration in milliseconds.

---

## 6. External TLS stack — the injection contract

Bouncy Castle is a direct dependency of zoxweb-core, **confined to the `pqc` package**: every BC
type — provider construction, BCJSSE contexts, `BCSSLParameters` group pinning, negotiated-group
introspection, PQ algorithm classification — lives in `PQCUtil`; nothing outside the package
imports `org.bouncycastle.*`.

- `PQCUtil.createClientContext(...)` builds the BCJSSE client context against the provider
  **instance** resolved through `SecUtil.loadProviders()` — the same registered BC / BCJSSE /
  BCPQC set the rest of the security stack uses (BC at position 1, BCJSSE at position 2,
  idempotent). First use of the auditor therefore registers the providers process-wide when
  nothing else has; contexts are still created against the instance, never by name lookup;
- **one context per session, deliberately — never cached or shared.** A shared BCJSSE client
  context owns a session cache keyed by peer host:port, and the provider offers no per-context
  way to disable resumption (`setSessionCacheSize(0)` means *unlimited*; the only off switch is
  a JVM-global system property read once at class load). A resumed handshake skips the key
  exchange and certificate steps the audit exists to observe, so sharing a context can silently
  corrupt every fact in the report for repeat targets. The per-session context also carries the
  session's client-cert-request recorder in its key manager. A warm context build costs
  single-digit milliseconds against handshakes that dominate the session — do not "optimize"
  this into a cache;
- the context enters the transport through
  `new SSLContextInfo(sslContext, clientAddress, protocols, ciphers)` (the client-mode
  external-context constructor); per-session pinning uses that constructor's `protocols` array
  (version sweep) and `SSLContextInfo.setSSLGroupSetter(PQCUtil.groupSetter(...))` (group
  sweep, via `BCSSLParameters.setNamedGroups`). The group setter also pins the early
  `key_share` (`BCSSLParameters.setEarlyKeyShares`) to the offer's first group plus the
  `x25519` fallback when offered: the provider's default key_share is classical-only, so
  without it a client-order-honoring server picks `x25519` without a HelloRetryRequest and a
  hybrid-first offer never reveals the endpoint's PQ support;
- negotiated-group introspection goes through the public `BCSSLEngine.getConnection()` surface
  plus **one** guarded reflective hop into the provider-internal `getTlsContext()`, after which
  `SecurityParameters.getNegotiatedGroup()` is public `org.bouncycastle.tls` API. All of it is
  best-effort: on a non-BC engine the PQC-kex fields are absent and the report says so.

---

## 7. The sweep

`PQCSweep` runs sessions sequentially or in parallel (each session is independent; NIOSocket
handles concurrency). One sweep probes one endpoint on one `NIOSocket` — its own, or a
caller-supplied shared one so a fleet of concurrent sweeps rides a single selector; the
`asyncAudit` variants deliver the same report to a callback from a `TaskUtil` worker instead of
blocking the caller. The sweep aggregates:

- **version matrix** — one session per candidate (`TLSv1.3`, `TLSv1.2`, `TLSv1.1`, `TLSv1.0`,
  optionally `SSLv3`), each with the version pinned as the only enabled protocol; result per
  candidate: `supported` / `refused` / `transport-error`. A refused legacy version is a *good*
  finding, not an error. A candidate the local provider cannot enable (disabled by
  `jdk.tls.disabledAlgorithms`, or unsupported) is reported `refused` without opening a
  session — the sweep probes the server only with offers the local stack can express.
- **group matrix** — one session per candidate group set, at minimum: default offer,
  hybrid-first preferred, PQ-hybrid only, classical only. "PQ-only completes" is the strong
  form of `pqc_kex` readiness; "default offer negotiates PQ" is the deployed-preference form;
  "preferred offer negotiates PQ" is the detection form — the hybrids ahead of the classical
  fallback with a hybrid key_share, so a PQ-capable server negotiates PQ even when the
  provider's classical-first default offer hides it (the provider default carries a single
  hybrid, last, with a classical-only key_share). All are reported. The hybrid candidate set is
  `X25519MLKEM768`, `SecP256r1MLKEM768`, `SecP384r1MLKEM1024` — a server supporting only the
  P-256/P-384 hybrids is still detected.
- **cipher matrix** (opt-in) — per version (`TLSv1.3`, `TLSv1.2`, each pinned as the only
  enabled protocol): the sweep offers the local provider's full suite list for that version,
  records the server's pick, drops it from the next offer, and repeats until the server
  refuses — one handshake per discovered suite plus one terminating refusal. The discovery
  order is the server's preference order over the offered set. A reversed two-suite offer pair
  then confirms whether the server enforces its own order (`server_cipher_preference`). Opt-in
  because it multiplies the sweep's handshake count. The offerable universe is what the local
  BCJSSE `SSLEngine` implements: suites the provider does not support cannot be probed.
- **revocation check** (opt-in) — one leaf check on the baseline's captured chain, resolved
  fastest first by `PQCRevocation`: the handshake-stapled OCSP response (zero network), one
  active OCSP POST against the certificate's AIA responder, then one bounded CRL download from
  its distribution point. Soft-fail: any failure reports `status: not_checked` with the reason,
  never an error and never an unbounded wait.
- **baseline session** — the default offer with full capture (§5); the sweep report embeds it.

The sweep's report is one `NVGenericMap`: the baseline's keys at top level, plus
`versions.<name>` and `groups.<name>` sub-results, and — when the cipher matrix ran — a
`ciphers` sub-map. Key names are case-insensitive (NVGenericMap semantics); no two keys may
differ only by case.

---

## 8. Results contract

Top level (baseline session; every key best-effort, absent when not determinable):

| Key | Type | Meaning |
|---|---|---|
| `guid` | text | run identity: a time-ordered (v7) UUID minted per session — the shared `ProtoUtil.ResKey` vocabulary (META-PROTOCOL.md §6), present in every report from construction |
| `proto-name` | text | `"pqc-tls"` (`TCPPQCProtocol.PROTOCOL_NAME`) — no JSON definition names this auditor |
| `transport` | text | `tcp` |
| `host` / `port` | text / int | the dialed endpoint (host as given — name or IP literal) |
| `open_ts` | long | epoch millis at session construction (this class's latency convention) |
| `close_ts` | long | epoch millis when the session closed — stamped once on every close path |
| `tls_protocol` / `tls_cipher` | text | negotiated session |
| `tls_kex_group` | text | negotiated named group, when the provider exposes it |
| `pqc_kex` | boolean | negotiated group is ML-KEM / hybrid |
| `pqc_cert` | boolean | leaf signature algorithm is post-quantum |
| `chain_trusted` | boolean | offline PKIX outcome against the configured trust store |
| `chain_reason` | text | why `chain_trusted` is false |
| `chain_time_valid` | boolean | every certificate of the completed chain is inside its validity window |
| `hostname_match` | boolean | dialed name vs leaf SANs/CN |
| `alpn` | text | negotiated ALPN protocol (offer: `h2, http/1.1`), absent when none |
| `ocsp_stapled` | boolean | present (true) when the server stapled an OCSP response |
| `client_cert_requested` | boolean | present (true) when the server sent a CertificateRequest |
| `cert_chain` | list of sub-maps | per certificate: `subject`, `issuer`, `serial`, `sig_alg`, `key_alg`, `key_size`, `not_before`, `not_after`; completed with the resolved trust-store root when identifiable |
| `days_to_expiry` | long | leaf certificate |
| `expires_soon` | boolean | `days_to_expiry` under the configured threshold |
| `latency_ms` | long | connect-to-introspection duration |
| `error` | text | transport/handshake failure cause, when the session did not complete |

**The PQ-first client strategy** (`PQCUtil.pqFirstAudit`): the client forces the PQ-hybrid
groups (`PQ_STRICT_GROUPS`) as the *only* offer; when that handshake fails or times out it
downgrades to the
provider's default offer and audits with that. The report carries `offer` —
`"pq_only"` (the strict offer succeeded) or `"downgraded_default"` — plus `pq_only_reason` on
the downgrade path. This is the connect-strategy counterpart of the sweep's `pq_only`
candidate: one call, strongest answer first, graceful fallback.

The sweep report embeds the baseline session's results at top level, so it inherits the run
identity above (the baseline session's `guid`/timestamps — `sweep_duration_ms` remains the whole
sweep's clock). Sweep additions: `versions.<candidate>` → `supported`/`refused`/`error`, `groups.<candidate>` →
same, each with the candidate's negotiated parameters when supported. When the cipher matrix
ran, `ciphers` holds `tls13` and `tls12` (string lists — the discovered suites, in discovery
order, which is the server's preference order over the offer; empty when the version yielded
none) and `server_cipher_preference` (boolean — the reversed-pair confirmation, absent when not
determinable). When the revocation check ran, `revocation` holds `status` (`good` / `revoked` /
`unknown` / `not_checked`), `source` (`stapled_ocsp` / `ocsp` / `crl`), and `reason` when not
checked. `sweep_duration_ms` is the whole sweep's wall-clock duration (the per-session
`latency_ms` stays the baseline session's own). And a `pqc_ready` summary sub-map — facts, not
a grade:

| Key | Meaning |
|---|---|
| `kex_default` | the default offer negotiated a PQ group |
| `kex_preferred` | the hybrid-first preferred offer negotiated a PQ group — the detection form |
| `kex_supported` | the PQ-only pinned offer completed with a PQ group — the strong form |
| `cert` | the leaf certificate is PQ-signed (reported separately: CA adoption trails kex) |
| `ready` | `kex_default OR kex_preferred OR kex_supported` — PQ key exchange is negotiable with this endpoint; kex is the operative readiness question because key exchange is exposed to harvest-now-decrypt-later while signatures only need to resist at time of use |

**The bundled grader** (`PQCGrader`): a report *consumer*, not part of the report —
`grade(report)` derives a letter plus the `reasons` that produced it, worst condition winning:
**F** trust failure (untrusted chain, hostname mismatch, time-invalid chain, revoked leaf —
outranks everything), **C** deprecated protocol or weak suite accepted, **B** trusted but no PQ
kex, or PQ-ready with minor wear (CBC suites, certificate expiring soon), **A** PQ negotiable
and clean, **A+** PQ negotiated by the default offer, **E** no posture information. Only facts
present in the report are judged — an absent fact (a matrix that did not run) neither helps
nor hurts. The sweep report itself never carries a grade; the runner's `-grade` flag prints
the verdict alongside it.

**The check facade** (`PQCCheck`): the second bundled consumer — one call, one kebab-case
report shaped like the hosted `check-qdz` service. Summary form = one hybrid-first session
(default-offer fallback); `detailed` adds the supported protocol versions, the enumerated
suites with strength classification (`STRONG` / `WEAK` / `INSECURE`), and the revocation
check. `overall-status` follows the service's semantics (`READY` / `PARTIAL` / `NOT_READY` /
`UNTRUSTED` / `ERROR`, trust failure outranking readiness); facts come from the same sessions
the sweep uses — the facade only reshapes and classifies. Unlike the sweep, the facade never
builds an `NIOSocket` of its own: every `check` / `asyncCheck` runs on a caller-supplied
socket (standing up a selector loop costs a thread and a `Selector`; the caller opens one once
and reuses it across checks, so a fleet of concurrent checks rides a single selector).

The check is event-driven end to end — a continuation pipeline, not a blocking loop. Each
session registers a close hook (`onClose`) before it is opened; when the session closes
(handshake completion, failure, or the per-session deadline — a scheduled close on the
socket's scheduler), the hook queues the next stage on the socket's executor: hybrid-first
session → default-offer fallback → summary shaping → (detailed) one session per protocol
candidate → the cipher-discovery loops → revocation → verdict → callback. A stage reshapes
facts and launches at most one session, then returns; no thread is ever parked on a session,
so a fleet of checks wider than the worker pool cannot starve itself (20 concurrent checks
complete on a two-thread executor). `asyncCheck` returns as soon as the first session is
launched and delivers the report to the callback from the executor; `check` is the same
pipeline with only the calling thread waiting on the final report. The one blocking stage is
the detailed form's revocation exchange (ordinary HTTP on the worker that runs it). The report
is JSON-compatible with the hosted service: `cert-chain` and `supported-cipher-suites` are
`NVGenericMapList` (JSON arrays), and `total-scanned` is a zero placeholder up front that the
hosting endpoint sets on the finished report
(`((NVLong) report.getNV("total-scanned")).setValue(counter)`).

---

## 9. Non-goals

- No revocation network I/O inside a session — a session only captures the stapled response;
  the active OCSP/CRL exchange is the sweep's opt-in `PQCRevocation` stage, outside the
  one-connection contract.
- No server-side auditing (client sessions only) and no application-layer dialogue — the
  session closes right after introspection. ALPN is offered and recorded, never spoken.
- No mTLS — a CertificateRequest is recorded as a finding, never answered with a certificate.
- No modification, subclassing, or bypassing of the zoxweb-core SSL driver.
- No Bouncy Castle import outside the `pqc` package. Provider registration goes through
  `SecUtil.loadProviders()` only — the package never registers or reorders providers itself,
  and contexts are created against the provider instance, never by name lookup.
- No scores inside the report — the auditor and the sweep report facts; policy belongs to the
  consumer. `PQCGrader` is such a consumer, bundled: it derives its letter grade *from* the
  finished report (§8), and its verdict lives outside the report.
- No host discovery, no port ranging: one caller-supplied endpoint, exactly like the
  meta-validators.

---

## 10. Testing requirements

- **Loopback**: a local TLS server (JDK-generated self-signed keystore) exercises capture,
  offline evaluation (`chain_trusted=false` + reason `self-signed`, `hostname_match`,
  expiration math with a short-lived cert), and the refused-version path (server pinned to
  TLSv1.3, session offering TLSv1.2).
- **Provider coverage**: the auditor's own sessions always run on BCJSSE; the reflective group
  read and, where the BC version supports it, a PQ-hybrid handshake are exercised live. The
  non-BC engines elsewhere in the stack stay untouched — the `pqc` package confinement is the
  guarantee.
- **Live smoke** (manual `main(host[:port])`, ProtoConnect-style report printing): a public
  PQ-enabled endpoint reports `pqc_kex=true` under the default offer.
- zoxweb-core's own TLS tests remain the regression gate that the auditor changed nothing
  underneath.
