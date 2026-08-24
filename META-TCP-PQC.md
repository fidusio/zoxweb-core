# META-TCP-PQC — requirements: the `TCPPQCProtocol` TLS-posture auditor

**Home:** a separate project, downstream of `org.zoxweb:zoxweb-core` (this document travels with
it; it is written against zoxweb-core's real seams and names them precisely).
**Status:** requirements — the authoritative contract the implementation must satisfy.

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
| lifecycle | `NVGenericMap` results bag + close latch + `waitForClose(millis)` / `getCloseCause()`, mirroring `TCPMetaProtocol` |

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
   families, name- or OID-matched).
4. **Chain evaluation** per §4.
5. **Timing** — connect-to-introspection duration in milliseconds.

---

## 6. External TLS stack — the injection contract

The auditor never constructs a provider-specific context itself:

- the caller supplies the TLS stack through
  `new SSLContextInfo(sslContext, clientAddress, protocols, ciphers)` (the client-mode
  external-context constructor) — for PQC-capable handshakes that context comes from BCJSSE,
  owned and registered by the **host application**;
- per-session pinning uses that constructor's `protocols` array (version sweep) and
  `SSLContextInfo.setSSLGroupSetter(...)` (group sweep);
- **no compile-time Bouncy Castle dependency** in the auditor or in zoxweb-core: every
  BC-specific read (negotiated group, PQ OID names beyond the JDK's vocabulary) is reflective
  and guarded. On a stock JSSE stack the auditor still runs; the PQC-kex fields are absent and
  the report says so.

---

## 7. The sweep

`PQCSweep` runs sessions sequentially or in parallel (each session is independent; NIOSocket
handles concurrency) and aggregates:

- **version matrix** — one session per candidate (`TLSv1.3`, `TLSv1.2`, `TLSv1.1`, `TLSv1.0`,
  optionally `SSLv3`), each with the version pinned as the only enabled protocol; result per
  candidate: `supported` / `refused` / `transport-error`. A refused legacy version is a *good*
  finding, not an error.
- **group matrix** — one session per candidate group set, at minimum: default offer, PQ-hybrid
  only, classical only. "PQ-only completes" is the strong form of `pqc_kex` readiness; "default
  offer negotiates PQ" is the deployed-preference form. Both are reported.
- **baseline session** — the default offer with full capture (§5); the sweep report embeds it.

The sweep's report is one `NVGenericMap`: the baseline's keys at top level, plus
`versions.<name>` and `groups.<name>` sub-results. Key names are case-insensitive
(NVGenericMap semantics); no two keys may differ only by case.

---

## 8. Results contract

Top level (baseline session; every key best-effort, absent when not determinable):

| Key | Type | Meaning |
|---|---|---|
| `tls_protocol` / `tls_cipher` | text | negotiated session |
| `tls_kex_group` | text | negotiated named group, when the provider exposes it |
| `pqc_kex` | boolean | negotiated group is ML-KEM / hybrid |
| `pqc_cert` | boolean | leaf signature algorithm is post-quantum |
| `chain_trusted` | boolean | offline PKIX outcome against the configured trust store |
| `chain_reason` | text | why `chain_trusted` is false |
| `hostname_match` | boolean | dialed name vs leaf SANs/CN |
| `cert_chain` | list of sub-maps | per certificate: `subject`, `issuer`, `serial`, `sig_alg`, `key_alg`, `key_size`, `not_before`, `not_after` |
| `days_to_expiry` | long | leaf certificate |
| `expires_soon` | boolean | `days_to_expiry` under the configured threshold |
| `latency_ms` | long | connect-to-introspection duration |
| `error` | text | transport/handshake failure cause, when the session did not complete |

Sweep additions: `versions.<candidate>` → `supported`/`refused`/`error`, `groups.<candidate>` →
same, each with the candidate's negotiated parameters when supported.

---

## 9. Non-goals

- No OCSP / CRL / revocation checking.
- No server-side auditing (client sessions only) and no application-layer dialogue — the
  session closes right after introspection.
- No modification, subclassing, or bypassing of the zoxweb-core SSL driver.
- No Bouncy Castle compile-time dependency anywhere; reflection only.
- No scoring or policy verdicts ("grade A") — the auditor reports facts; policy belongs to the
  consumer.
- No host discovery, no port ranging: one caller-supplied endpoint, exactly like the
  meta-validators.

---

## 10. Testing requirements

- **Loopback**: a local TLS server (JDK-generated self-signed keystore) exercises capture,
  offline evaluation (`chain_trusted=false` + reason `self-signed`, `hostname_match`,
  expiration math with a short-lived cert), and the refused-version path (server pinned to
  TLSv1.3, session offering TLSv1.2).
- **Provider matrix**: the suite runs green on stock JSSE (PQC-kex fields absent); a
  BC-registered run (test-scoped dependency in the auditor project only) proves the reflective
  group read and, where the JDK/BC combination supports it, a PQ-hybrid loopback handshake.
- **Live smoke** (manual `main(host[:port])`, ProtoConnect-style report printing): a public
  PQ-enabled endpoint reports `pqc_kex=true` under the default offer.
- zoxweb-core's own TLS tests remain the regression gate that the auditor changed nothing
  underneath.
