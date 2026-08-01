# FSM — Building State Machines with `org.zoxweb.server.fsm`

This document is a build guide. Given a description of a workflow, protocol, or event-driven
process, use it to construct a working state machine with the `org.zoxweb.server.fsm` package
(zoxweb-core, Java 8 — no post-Java-8 language features or APIs).

Source code: [https://github.com/fidusio/zoxweb-core](https://github.com/fidusio/zoxweb-core)
(package: [`src/main/java/org/zoxweb/server/fsm`](https://github.com/fidusio/zoxweb-core/tree/master/src/main/java/org/zoxweb/server/fsm))

## The model

This is **not a classical finite state machine**. There is no transition table and no
transition validation. It is a **canonical-ID routed publish/subscribe engine** with a
current-state marker:

| Concept | Class | Role |
|---|---|---|
| Machine | `StateMachine<C>` / `StateMachineInt<C>` | Single dispatch funnel for all triggers; holds shared config `C`, the state registry, and the current-state marker |
| State | `State<P>` / `StateInt<P>` | Named identifier + mutable property bag (`NVGenericMap`); a registry of consumers. States hold context, **not behavior** |
| Trigger | `Trigger<D>` / `TriggerInt<D>` | Immutable event: canonical ID (routing key) + typed payload `D` + issuing state + unique ID + timestamp |
| Consumer | `TriggerConsumer<T>` / `TriggerConsumerInt<T>` | **Where all logic lives.** Bound to one or more canonical IDs; processes the payload, handles its own errors, publishes the next trigger(s) |

The "transition graph" is emergent: it is encoded in which triggers each consumer publishes,
not declared anywhere.

## Semantics contract (the rules the machine obeys)

1. **Single funnel.** Every trigger is delivered through `publish(...)` or `publishSync(...)`
   on the machine. Consumer publish helpers and all convenience overloads delegate there.
2. **Broadcast by design.** Multiple consumers — in the same or different states — may
   register the same canonical ID. Publishing that ID reaches **all** of them.
3. **Unknown IDs are silent no-ops.** Publishing an ID nobody registered delivers nothing and
   throws nothing.
4. **Snapshot semantics.** Each publish captures the consumer set for its canonical ID at
   publish time. Consumers registered later receive only triggers published after their
   registration completes — late joiners see the future, not the past.
5. **Consumers receive the payload, not the Trigger.** `accept(T data)` gets `trigger.get()`.
   Canonical ID, timestamp, and issuing state are routing/diagnostic metadata.
6. **Runtime registration is legal.** States can be registered while the machine is
   publishing; consumers added to an already-registered state are forwarded to the machine
   automatically. Registration and dispatch are synchronized — every publish sees a clean
   before-or-after view.
7. **Events can arrive at any time, from any thread.** The machine never rejects a trigger
   because of the current state.
8. **Consumers own everything**: business logic, error handling (an error is just another
   trigger you publish), and flow control (publish the next trigger, or publish nothing to
   end the flow).
9. **`close()` gates future publishes only.** Subsequent publish calls throw
   `IllegalStateException`; triggers already queued still execute.
10. **Current state is bookkeeping.** It is set to the owning state of whichever consumer
    dispatched last. `publishToCurrentState(trigger)` is a relevance-gated publish: pre-init
    (no current state) → broadcast; current state has a consumer for the ID → broadcast;
    otherwise → drop.
11. **States can be deregistered — except INIT.** `sm.deregister(state | name | enum)`
    removes a state's consumers from routing (consumers of other states sharing the same
    canonical IDs are unaffected). Deregistration affects only publishes that start
    afterward — already-snapshotted triggers still deliver. The state object stays intact
    (suspend/resume): a later `register` fully restores it. Deregistering the current
    state clears the current-state marker (pre-init delivery policy applies).
    Deregistering the INIT state throws `IllegalArgumentException` — the bootstrap anchor
    is permanent. Unknown/null states return `false`.

## Build recipe

### Step 1 — Choose the execution mode (constructor)

```java
new StateMachine<MyConfig>("name")                    // TaskScheduler mode (TaskUtil default) — async, pooled
new StateMachine<MyConfig>("name", taskScheduler)     // TaskScheduler mode, explicit scheduler
new StateMachine<MyConfig>("name", executor)          // Executor mode — async on that executor
new StateMachine<MyConfig>("name", (Executor) null)   // Synchronous — consumers run inline in the publishing thread
```

- Use **synchronous** for deterministic, serialized processing (protocol engines driven by an
  outer event loop, and anything that must not spawn work — the TLS engine does this).
- Use **TaskScheduler/Executor** for fire-and-forget pipelines where consumers may run on
  pool threads. `publishSync(...)` always runs inline regardless of mode.

### Step 2 — Define canonical IDs

Canonical IDs are the routing keys. Use an enum (preferred) or string constants:

```java
enum Ops implements GetName {          // GetName optional; plain enums use name()
    VALIDATE("validate"), PERSIST("persist"), FAILED("failed");
    private final String name;
    Ops(String n) { name = n; }
    public String getName() { return name; }
}
```

Enum IDs resolve through `SUS.enumName(...)`: `GetName.getName()` if implemented, else
`Enum.name()`. Ensure publishers and registrations resolve to the same string.

### Step 3 — Define states and consumers

A state groups the consumers that are logically active in one phase and carries shared
mutable context in its properties. All logic goes in `TriggerConsumer.accept`:

```java
State<Object> processing = new State<Object>("processing");

processing.register(new TriggerConsumer<Order>(Ops.VALIDATE) {
    @Override
    public void accept(Order order) {
        try {
            validate(order, (MyConfig) getStateMachine().getConfig());
            publish(Ops.PERSIST, order);          // continue the flow
        } catch (Exception e) {
            publish(Ops.FAILED, e);               // errors are triggers too
        }
    }
});

processing.register(new TriggerConsumer<Order>(Ops.PERSIST) {
    @Override
    public void accept(Order order) { store(order); /* no publish = flow ends */ }
});
```

Simple handlers can be lambdas — they are adapted into a `TriggerConsumer` automatically:

```java
processing.register((Consumer<Exception>) e -> log(e), Ops.FAILED);
```

Consumer construction rules:
- One or more canonical IDs required — an empty ID list throws `IllegalArgumentException`.
- One consumer may handle several IDs: `new TriggerConsumer<T>("A", "B")`.
- Inside `accept`, use `getStateMachine().getConfig()` for shared config,
  `getState().getProperties()` for the state's mutable context, and the inherited
  `publish` / `publishSync` helpers for flow control.

### Step 4 — Define the INIT state

`start(...)` publishes the well-known `StateInt.States.INIT` ("init") trigger and **throws if
no consumer is registered for it**. Always provide one; use it to bootstrap (load resources,
publish the first real trigger):

```java
State<Object> init = new State<Object>(StateInt.States.INIT);
init.register(new TriggerConsumer<Void>(StateInt.States.INIT) {
    @Override
    public void accept(Void v) {
        // bootstrap; optionally publish the first domain trigger here
    }
});
```

### Step 5 — Assemble and start

```java
StateMachine<MyConfig> sm = new StateMachine<MyConfig>("order-flow", (Executor) null);
sm.setConfig(myConfig);          // shared object visible to every consumer
sm.register(init);
sm.register(processing);
sm.start(true);                  // true = publish INIT synchronously, false = async
```

### Step 6 — Feed it events

```java
sm.publish(null, Ops.VALIDATE, someOrder);        // async (per machine mode)
sm.publishSync(null, Ops.VALIDATE, someOrder);    // inline, blocks until all consumers ran
sm.close();                                       // reject all future publishes
```

The first argument of the convenience overloads is the issuing state (metadata; null is fine
for external events).

## Design mapping — description to machine

When translating a described process:

1. **Each distinct event/command/signal** in the description → a canonical ID.
2. **Each processing step** → a `TriggerConsumer` (logic + error handling + what to publish
   next). Keep each consumer focused on one event type.
3. **Each phase of the lifecycle** → a `State` grouping its consumers; put phase-shared
   mutable data in the state's properties (`NVBase` entries passed to the `State`
   constructor or added via `getProperties()`).
4. **Shared immutable/session context** (connections, engine objects, settings) → the config
   object `C` on the machine.
5. **Fan-out** ("when X happens, A and B must both run") → register two consumers under the
   same canonical ID; broadcast delivers to both.
6. **Error paths** → dedicated canonical IDs (e.g. `FAILED`) with their own consumers; never
   let exceptions escape `accept`.
7. **Terminal steps** → consumers that publish nothing. `StateInt.States.FINAL` ("final") is
   available as a conventional terminal ID; nothing enforces reaching it.
8. **External stimuli** (network events, timers, user actions) → whatever thread observes
   them calls `sm.publish(...)`; no marshalling needed.
9. **Phases that come and go** (feature toggles, mode switches, plugin lifecycles) →
   suspend/resume with `sm.deregister(state)` / `sm.register(state)` at runtime; the
   state keeps its consumers and properties while detached. INIT can never be
   deregistered.

## Type discipline (important)

Payload typing is not compile-time checked across the publish/consume boundary. A trigger
published under ID `X` with payload type `P` reaches every consumer of `X` as a raw cast —
a mismatch is a runtime `ClassCastException` in the consumer. **Convention: one canonical ID
= one payload type, everywhere.** Document the payload type next to each ID definition.

## Reference implementation in this repo

`org.zoxweb.server.net.ssl.SSLStateMachine` — synchronous mode (`null` executor), states
`INIT` / `SSLHandshakingState` / `SSLDataReadyState`, canonical IDs =
`SSLEngineResult.HandshakeStatus` enum names, config = `SSLSessionConfig`. The JDK's own
handshake status values drive the machine via `publishSync`.

## Pitfalls checklist

- Registering zero canonical IDs on a consumer → `IllegalArgumentException` at construction.
- Calling `start(...)` without an INIT consumer → `IllegalArgumentException`.
- Publishing after `close()` → `IllegalStateException`; anything already queued still runs.
- Passing a `TriggerConsumer` object through the raw-`Consumer` register overload works but
  runs it as a plain consumer under the adapter's IDs (its own exec counter is not used);
  prefer `register(TriggerConsumerInt)` for typed consumers.
- Mutating one state's properties from consumers running concurrently (async modes) needs
  caller-side coordination — the property bag is shared mutable context.
- Ordering: async modes guarantee no cross-trigger ordering. For strict ordering use
  synchronous mode or `publishSync` chains.

---

## Appendix: the design, framework-agnostic

To implement this architecture outside Java/zoxweb, build these pieces:

1. **Router**: a map `canonicalID -> ordered set of consumers`. Publishing looks up the ID,
   snapshots the consumer list, and invokes each consumer with the event payload. Missing ID
   = no-op. Multiple consumers per ID = all invoked (broadcast).
2. **Event**: immutable record of `(canonicalID, payload, issuingState, uniqueID, timestamp)`.
   Consumers get the payload only.
3. **Consumer**: a callback bound to ≥1 canonical IDs. Contract: process the payload, handle
   own errors (emit an error event rather than throwing), then emit the next event(s) or
   stop. All flow control lives here.
4. **State**: a named context object (ID + mutable key/value properties) that groups
   consumers and is recorded as "current" whenever one of its consumers runs. It is
   informational bookkeeping, not an enforcement mechanism.
5. **Execution modes**: same publish API dispatches inline (deterministic) or via a
   queue/pool (async). Synchronous publish must exist for callers that need completion
   before continuing.
6. **Concurrency rules**: registration (writers) and dispatch lookup (readers) synchronize
   on the router; dispatch iterates a snapshot outside the lock; late registrations affect
   only later publishes; a close flag gates new publishes but does not cancel queued work.
7. **No transition validation anywhere** — any event is accepted at any time; the process
   graph emerges from what consumers choose to emit.
