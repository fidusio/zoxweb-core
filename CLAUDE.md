# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

zoxweb-core is an enterprise Java utility library (Maven artifact `org.zoxweb:zoxweb-core`, published to Maven Central) by XlogistX. Single Maven module, ~670 classes, compiled to **Java 8** (`jdk.version=1.8` in the pom) even though the local JDK is newer — do not use post-Java-8 language features or APIs in `src/main`.

## Build and test commands

```
mvn compile                            # compile
mvn package                            # jar + sources + javadoc (gpg sign runs at verify)
mvn test -DskipTests=false             # run tests — REQUIRED flag, see below
mvn test -DskipTests=false -Dtest=UserInfoDAOTest             # single test class
mvn test -DskipTests=false -Dtest=UserInfoDAOTest#methodName  # single test method
```

**Tests are skipped by default**: the pom sets `<skipTests>true</skipTests>` as a property. `mvn test` alone does nothing; always pass `-DskipTests=false`.

Other build quirks:
- `src/main/java` is also declared as a resource directory, so non-`.java` files there (e.g. `org/zoxweb/conf/*.json`) ship inside the jar.
- The `maven-gpg-plugin` signs at the `verify` phase; `mvn install`/`mvn verify` will fail without the GPG key. Use `mvn package`/`mvn test` for normal development.
- Tests are JUnit 5 (Jupiter). A minority of older files still use JUnit 4-style imports.

## Architecture

### The `shared` vs `server` split (the one rule that governs the whole tree)

- `org.zoxweb.shared.*` — GWT/client-safe code: no threads, no `java.io.File`, no sockets, no `javax.net.ssl`, no JVM-only APIs. This is a design discipline (there is no `.gwt.xml` in this repo), but it must be preserved: **never add a server-only import to `shared`**.
- `org.zoxweb.server.*` — full-JVM code: NIO, TLS, crypto providers, thread pools, file I/O, HTTP execution.

### The NVEntity meta-model (the spine of everything)

Nearly every data object — DAOs, security identities, HTTP message configs, API configs — is an `NVEntity` (`org.zoxweb.shared.util`). An entity holds its state in a `Map<String, NVBase<?>>` and declares its schema statically:

- a nested `enum Param implements GetNVConfig` builds one `NVConfig` per field via `NVConfigManager.createNVConfig(...)`;
- a `public static final NVConfigEntity NVC_<NAME>` aggregates them (parent's `NVC_*` passed in for meta-inheritance);
- the constructor calls `super(NVC_<NAME>)`.

That schema drives JSON round-tripping (`GSONUtil.toJSON/fromJSON`, meta-aware) and persistence (`APIDataStore.search/insert/update` take an `NVConfigEntity` as the collection schema). Follow this idiom exactly when adding entity fields.

### Persistence contract, not implementation

`org.zoxweb.shared.api.APIDataStore` defines the datastore contract (CRUD, meta-driven search, sequences, dynamic enum maps). The only implementation in this repo is `org.zoxweb.server.util.MockAPIDataStore` (in-memory, for tests). Real database-backed implementations live in sibling repos — and they implement real `beginTransaction`/`abortTransaction`; the no-op defaults in the interface are fallbacks only. Don't flag transactional delete-then-insert patterns as unsafe on the grounds of the no-op defaults.

### NIO / TLS stack (`org.zoxweb.server.net`, `.net.ssl`)

- `NIOSocket` is the selector-loop engine: one instance handles server sockets, client sockets, and datagrams. Protocols plug in by subclassing `ProtocolFactoryBase` (mints per-connection `ProtocolHandler` instances) and registering via `NIOSocket.addServerSocket(port, backlog, factory)`. Reference implementation: `net/protocols/EchoProtocol`.
- TLS lives in `net.ssl`: `SSLNIOSocketHandlerFactory` + `SSLNIOSocketHandler` drive a non-blocking `SSLEngine` via an explicit state machine (`SSLStateMachine`, `SSLHandshakingState`, `SSLDataReadyState`).
- Layering is strict: `NIOSocket` = pure transport, the SSL layer = pure crypto, session callbacks (`BaseSessionCallback` and friends) = protocol logic. One `NIOSocket` + one thread pool drives HTTPS serving, tunnels, proxies, and client TLS simultaneously.
- **The SSL/TLS code paths are tuned and fragile. Do not refactor or "fix" them unprompted** — flag suspected issues and wait for an explicit ask. The handshake is serialized per session on one worker by design; `_needTask` does not block the selector.

### Threading (`org.zoxweb.server.task`)

`TaskUtil` is the app-wide facade: `TaskUtil.defaultTaskProcessor()` / default task scheduler back everything (NIO, HTTP, timers). The pool is bounded (16–128 workers, ~1500–2000 deep queue with hysteresis backpressure at the dispatch layer). Configuration setters only work before first initialization.

Design philosophy (confirmed by the maintainer — check before flagging "defects"):
1. **The app must survive**: workers catch `Throwable` to stay alive; this is intentional, not sloppy error handling.
2. **No babysitting callers**: no hung-task protection or per-caller timeouts; callers are responsible for their own behavior.

### Security (`org.zoxweb.shared.security` + `org.zoxweb.server.security`)

Identity model: `PrincipalIdentifier` (login handle — username/email) resolves to a `SubjectIdentifier` (the subject, keyed by GUID). Linkage is by `subject_guid` across principals, credentials (`CredentialInfo`/`CIPassword`), and RBAC grants (`PermissionGrant`, `RoleGrant`, `RoleGroupGrant` referencing `PermissionInfo`/`RoleInfo`/`RoleGroupInfo` catalogs). `DomainSecurityManagerDefault` (server) implements the `DomainSecurityManager` contract on top of any `APIDataStore`; `SecUtil` is the crypto facade (password hashing, JWT). A subject's last `PrincipalIdentifier` can never be deleted.

### HTTP

`org.zoxweb.shared.http` holds protocol value objects (`HTTPMessageConfig`, headers, status codes, WebSocket frames); `org.zoxweb.server.http` executes them (`OkHTTPCall`/`HTTPCall` clients, `HTTPNIOSocket` server on the NIO stack, `HTTPAPIBuilder`/`HTTPAPIEndPoint` REST toolkit).

### Utility naming conventions

- `SharedUtil`, `SharedStringUtil`, `SharedBase64`, `SharedMetaUtil` — stateless static helpers safe for `shared`.
- `SUS` — terse null/empty guards (`SUS.isEmpty`, `SUS.checkIfNulls`), used everywhere.
- `Const` — central constants/enums bag; `MetaToken` — well-known field names (`SUBJECT_GUID`, `GUID`, `NAME`) used in datastore queries.
- `GSONUtil` (server) — the JSON hub for both generic and NVEntity-aware serialization.

## Tests layout

Real tests live in `src/test/java/org/zoxweb/{shared,server}/...` mirroring main packages, named `*Test.java`. Loose classes directly under `src/test/java/org/zoxweb/` with informal names (`OddTest`, `LambdaTest`, `QuickLZTest`, ...) are scratch/experiment code, not the suite. Some `server/net`, `server/http`, and TLS tests open real sockets and are not hermetic; meta-model and security tests use `MockAPIDataStore` and are self-contained.
