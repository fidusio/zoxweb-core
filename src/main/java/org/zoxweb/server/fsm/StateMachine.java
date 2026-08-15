package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.task.SupplierConsumerTask;
import org.zoxweb.shared.util.NVGMProperties;
import org.zoxweb.shared.util.SUS;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Default {@link StateMachineInt} implementation: the single dispatch funnel for all triggers.
 * <p>
 * Routing is by canonical ID via an internal {@code canonicalID -> Set<TriggerConsumerInt>}
 * index built at registration. All publishing paths funnel through {@link #publish(TriggerInt)}
 * or {@link #publishSync(TriggerInt)}, which snapshot the consumer set for the trigger's
 * canonical ID and dispatch each consumer wrapped in a {@link TriggerConsumerHolder} (which
 * maintains the exec counter and the current-state marker).
 * </p>
 * <h2>Concurrency</h2>
 * Registration writers ({@code register}, {@code mapTriggerConsumer}) and the dispatch reader
 * ({@code lookupTriggerConsumers}) synchronize on this machine, so states and consumers may be
 * registered while the machine is operational: every publish sees a consistent before-or-after
 * view of any concurrent registration, and the snapshot array is dispatched outside the lock.
 * <h2>Execution mode</h2>
 * Chosen by constructor: TaskScheduler mode ({@link TaskSchedulerProcessor}), Executor mode,
 * or — with a null executor — synchronous inline execution. {@link #publishSync(TriggerInt)}
 * always runs inline regardless of mode.
 *
 * @param <C> the configuration type shared with all consumers via {@link #getConfig()}
 */
public class StateMachine<C>
        extends NVGMProperties
        implements StateMachineInt<C> {

    public final static LogWrapper log = new LogWrapper(StateMachine.class).setEnabled(false);

    private final String name;
    private final ScheduledExecutorService tsp;
    private final Map<String, Set<TriggerConsumerInt<?>>> tcMap = new ConcurrentHashMap<>();
    private final Map<String, StateInt<?>> states = new ConcurrentHashMap<>();
    private C config;
    private final Executor executor;
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicBoolean isEventLogEnabled = new AtomicBoolean(true);
    //private final NVGenericMap properties = new NVGenericMap("sm_properties");

    private final AtomicReference<StateInt<?>> currentState = new AtomicReference<>();
    private final Set<StateMachineListener> listeners = new CopyOnWriteArraySet<>();


    public StateMachine(String name) {
        this(name, TaskUtil.defaultTaskScheduler());
    }

    public StateMachine(String name, ScheduledExecutorService taskSchedulerProcessor)
            throws NullPointerException {
        super(true, "sm-properties");
        SUS.checkIfNulls("Name or TaskScheduler can't be null.", name, taskSchedulerProcessor);
        this.name = name;
        this.tsp = taskSchedulerProcessor;
        //this.schedulerOnly = schedulerOnly;
        executor = (taskSchedulerProcessor instanceof TaskSchedulerProcessor)  ? ((TaskSchedulerProcessor)taskSchedulerProcessor).getExecutor() : null;
    }

    public StateMachine(String name, Executor executor)
            throws NullPointerException {
        super(true, "sm-properties");
        if (log.isEnabled()) log.getLogger().info(name + ":" + executor);
        SUS.checkIfNulls("Name or Executor can't be null.", name);
        this.name = name;
        this.tsp = null;
        this.executor = executor;
    }

    @Override
    public StateMachineInt<C> register(StateInt<?> state) {
        if (state != null) {
            synchronized (this) {
                TriggerConsumerInt<?>[] triggers = state.triggers();
                if (triggers != null) {
                    for (TriggerConsumerInt<?> tc : triggers) {
                        mapTriggerConsumer(tc);
                    }
                }
                state.setStateMachine(this);
                states.put(state.getName(), state);
            }
            // fired outside the monitor — a listener must never hold the machine lock
            fire(StateMachineEvent.Type.STATE_REGISTERED, null, null, null, state, 0);
        }
        return this;
    }

    @Override
    public StateMachineInt<C> setEventLogEnabled(boolean enabled) {
        isEventLogEnabled.set(enabled);
        return this;
    }

    @Override
    public boolean isEventLogEnabled() {
        return isEventLogEnabled.get();
    }

    @Override
    public StateMachineInt<C> addListener(StateMachineListener listener) {
        if (listener != null)
            listeners.add(listener);
        return this;
    }

    @Override
    public boolean removeListener(StateMachineListener listener) {
        return listener != null && listeners.remove(listener);
    }

    /**
     * Notifies all listeners of an event; the event is constructed only if listeners
     * exist (zero-allocation fast path) and each listener is exception-isolated.
     */
    private void fire(StateMachineEvent.Type type,
                      TriggerInt<?> trigger,
                      TriggerConsumerInt<?> consumer,
                      StateInt<?> oldState,
                      StateInt<?> newState,
                      int consumerCount) {
        if (isEventLogEnabled.get() && !listeners.isEmpty()) {
            StateMachineEvent event = new StateMachineEvent(this, type, trigger, consumer, oldState, newState, consumerCount);
            for (StateMachineListener l : listeners) {
                try {
                    l.handleEvent(event);
                } catch (Exception e) {
                    if (log.isEnabled()) log.getLogger().info("listener failed: " + e);
                }
            }
        }
    }

    /**
     * Package-private: invoked by {@link TriggerConsumerHolder} after a consumer has
     * successfully processed a trigger.
     */
    void fireTriggerConsumed(TriggerInt<?> trigger, TriggerConsumerInt<?> consumer) {
        fire(StateMachineEvent.Type.TRIGGER_CONSUMED, trigger, consumer, null, null, 0);
    }

    @Override
    public boolean deregister(StateInt<?> state) {
        if (state != null && SUS.enumName(StateInt.States.INIT).equals(state.getName()))
            throw new IllegalArgumentException("INIT state cannot be deregistered");
        if (state == null || states.get(state.getName()) != state)
            return false;

        // detach first: stops State.register(tc) forwarding into tcMap,
        // so no new consumer can slip into the routing index past the snapshot below
        state.setStateMachine(null);
        // State monitor only — never nested with the machine monitor (lock discipline)
        TriggerConsumerInt<?>[] triggers = state.triggers();
        // Machine monitor only
        synchronized (this) {
            if (triggers != null) {
                for (TriggerConsumerInt<?> tc : triggers) {
                    for (String canID : tc.canonicalIDs()) {
                        Set<TriggerConsumerInt<?>> tcSet = tcMap.get(canID);
                        if (tcSet != null) {
                            tcSet.remove(tc);
                            if (tcSet.isEmpty())
                                tcMap.remove(canID);
                        }
                    }
                }
            }
            // value-checked: only remove if the mapping still points at this state
            states.remove(state.getName(), state);
        }
        // clear the current-state marker only if it is the deregistered state
        if (currentState.compareAndSet(state, null))
            fire(StateMachineEvent.Type.STATE_CHANGED, null, null, state, null, 0);
        // completion event, fired last
        fire(StateMachineEvent.Type.STATE_DEREGISTERED, null, null, null, state, 0);
        return true;
    }


    synchronized void mapTriggerConsumer(TriggerConsumerInt<?> tc) {
        String[] canonicalIDs = tc.canonicalIDs();
        for (String canID : canonicalIDs) {
            tcMap.computeIfAbsent(canID, k -> new LinkedHashSet<>()).add(tc);
        }
    }

    @Override
    public StateMachineInt<C> publish(TriggerInt<?> trigger) {
        if (isClosed())
            throw new IllegalStateException("State machine closed");

        if (log.isEnabled()) log.getLogger().info("" + trigger);
        if (isScheduledTaskEnabled()) {
            TriggerConsumerInt<?>[] tcis = lookupTriggerConsumers(trigger);
            fire(StateMachineEvent.Type.TRIGGER_PUBLISHED, trigger, null, null, null, tcis != null ? tcis.length : 0);
            if (tcis != null) {
                for (TriggerConsumerInt<?> c : tcis) {
                    tsp.schedule(new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(trigger, c)), 0, TimeUnit.MILLISECONDS);
                }
            }

        } else if (executor != null) {
            TriggerConsumerInt<?>[] triggerConsumerInts = lookupTriggerConsumers(trigger);
            fire(StateMachineEvent.Type.TRIGGER_PUBLISHED, trigger, null, null, null, triggerConsumerInts != null ? triggerConsumerInts.length : 0);
            if (triggerConsumerInts != null) {
                for (TriggerConsumerInt<?> c : triggerConsumerInts) {
                    executor.execute(new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(trigger, c)));
                }
            }
        } else
            return publishSync(trigger);

        return this;
    }


    private synchronized TriggerConsumerInt<?>[] lookupTriggerConsumers(TriggerInt<?> tc) {
        if (tc != null) {
            Set<TriggerConsumerInt<?>> set = tcMap.get(tc.getCanonicalID());
            if (set != null)
                return set.toArray(new TriggerConsumerInt[0]);
        }
        return null;
    }

    @Override
    public <D> StateMachineInt<C> publish(StateInt<?> state, String canID, D data) {
        if (canID != null)
            return publish(new Trigger<>(this, canID, state, data));
        return this;
    }

    @Override
    public <D> StateMachineInt<C> publish(StateInt<?> state, Enum<?> canID, D data) {
        if (canID != null)
            return publish(new Trigger<>(this, canID, state, data));
        return this;
    }


    @Override
    public StateMachineInt<C> publishSync(TriggerInt<?> trigger) {
        if (isClosed())
            throw new IllegalStateException("State machine closed");

        TriggerConsumerInt<?>[] triggerConsumerInts = lookupTriggerConsumers(trigger);
        fire(StateMachineEvent.Type.TRIGGER_PUBLISHED, trigger, null, null, null, triggerConsumerInts != null ? triggerConsumerInts.length : 0);

        if (log.isEnabled()) log.getLogger().info("" + trigger);

        if (triggerConsumerInts != null) {
            for (TriggerConsumerInt<?> c : triggerConsumerInts) {
                SupplierConsumerTask<?> sct = new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(trigger, c));
                sct.run();
            }
        }

        return this;
    }

    @Override
    public <D> StateMachineInt<C> publishSync(String canID, D data) {
        return publishSync(new Trigger<>(this, canID, getCurrentState(), data));
    }

    @Override
    public <D> StateMachineInt<C> publishSync(Enum<?> canID, D data) {
        return publishSync(new Trigger<>(this, canID, getCurrentState(), data));
    }

    @Override
    public <D> StateMachineInt<C> publishSync(StateInt<?> state, String canID, D data) {
        if (canID != null)
            return publishSync(new Trigger<>(this, canID, state, data));
        return this;
    }

    @Override
    public <D> StateMachineInt<C> publishSync(StateInt<?> state, Enum<?> canID, D data) {
        if (canID != null)
            return publishSync(new Trigger<>(this, canID, state, data));
        return this;
    }

    @Override
    public StateMachineInt<C> publishToCurrentState(TriggerInt<?> trigger) {
        if (isClosed())
            throw new IllegalStateException("State machine closed");

        StateInt<?> current = getCurrentState();
        if (current == null) {
            return publish(trigger);
        } else {
            Consumer<?> tci = current.lookupTriggerConsumer(trigger.getCanonicalID());
            if (tci != null) {
                return publish(trigger);
            }
        }
        return this;
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public StateMachineInt<C> setConfig(C config) {
        this.config = config;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public void start(boolean sync) {
        if (tcMap.get(StateInt.States.INIT.getName()) != null) {
            // listeners see the start before the INIT consumer runs
            fire(StateMachineEvent.Type.MACHINE_STARTED, null, null, null, null, 0);
            if (sync)
                publishSync(new Trigger<Void>(this, StateInt.States.INIT, null, null));
            else
                publish(new Trigger<Void>(this, StateInt.States.INIT, null, null));
        } else
            throw new IllegalArgumentException("Not Init state");
    }


    public String toString() {
        return getName();
    }

    public ScheduledExecutorService getScheduler() {
        return tsp;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public boolean isScheduledTaskEnabled() {
        return tsp != null;
    }

    @Override
    public StateInt<?> lookupState(String name) {
        return states.get(name);
    }


    @Override
    public StateInt<?> getCurrentState() {
        return currentState.get();
    }

    @Override
    public void setCurrentState(StateInt<?> stateInt) {
        StateInt<?> old = currentState.getAndSet(stateInt);
        // fire only on an actual change; same-state dispatches are silent
        if (old != stateInt)
            fire(StateMachineEvent.Type.STATE_CHANGED, null, null, old, stateInt, 0);
    }


    @Override
    public void close() {
        if (!isClosed.getAndSet(true))
            fire(StateMachineEvent.Type.MACHINE_CLOSED, null, null, null, null, 0);
    }

    public boolean isClosed() {
        return isClosed.get();
    }


}
