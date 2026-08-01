package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.task.SupplierConsumerTask;
import org.zoxweb.shared.util.SUS;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
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
        implements StateMachineInt<C> {

    public final static LogWrapper log = new LogWrapper(StateMachine.class).setEnabled(false);

    private final String name;
    private final TaskSchedulerProcessor tsp;
    private final Map<String, Set<TriggerConsumerInt<?>>> tcMap = new ConcurrentHashMap<String, Set<TriggerConsumerInt<?>>>();
    private final Map<String, StateInt<?>> states = new ConcurrentHashMap<String, StateInt<?>>();
    private C config;
    private final Executor executor;
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicReference<StateInt> currentState = new AtomicReference<>();


    public StateMachine(String name) {
        this(name, TaskUtil.defaultTaskScheduler());
    }

    public StateMachine(String name, TaskSchedulerProcessor taskSchedulerProcessor)
            throws NullPointerException {
        SUS.checkIfNulls("Name or TaskScheduler can't be null.", name, taskSchedulerProcessor);
        this.name = name;
        this.tsp = taskSchedulerProcessor;
        //this.schedulerOnly = schedulerOnly;
        executor = tsp.getExecutor();
    }

    public StateMachine(String name, Executor executor)
            throws NullPointerException {
        if (log.isEnabled()) log.getLogger().info(name + ":" + executor);
        SUS.checkIfNulls("Name or Executor can't be null.", name);
        this.name = name;
        this.tsp = null;
        this.executor = executor;
    }

    @Override
    public synchronized StateMachineInt<C> register(StateInt<?> state) {
        if (state != null) {
            TriggerConsumerInt<?>[] triggers = state.triggers();
            if (triggers != null) {
                for (TriggerConsumerInt<?> tc : triggers) {
                    mapTriggerConsumer(tc);
                }
            }
            state.setStateMachine(this);
            states.put(state.getName(), state);
        }
        return this;
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
        currentState.compareAndSet(state, null);
        return true;
    }

//    @Override
//    public boolean deregister(String name) {
//        return deregister(lookupState(name));
//    }
//
//    @Override
//    public boolean deregister(Enum<?> name) {
//        return deregister(lookupState(name));
//    }

    synchronized void mapTriggerConsumer(TriggerConsumerInt<?> tc) {
        String[] canonicalIDs = tc.canonicalIDs();
        for (String canID : canonicalIDs) {

            Set<TriggerConsumerInt<?>> tcSet = tcMap.get(canID);
            if (tcSet == null) {
                tcSet = new LinkedHashSet<TriggerConsumerInt<?>>();
                tcMap.put(canID, tcSet);
            }
            tcSet.add(tc);
        }
    }


    @Override
    public StateMachineInt<C> publish(TriggerInt<?> trigger) {
        if (isClosed())
            throw new IllegalStateException("State machine closed");

        if (log.isEnabled()) log.getLogger().info("" + trigger);
        if (isScheduledTaskEnabled()) {
            TriggerConsumerInt<?>[] tcis = lookupTriggerConsumers(trigger);
            if (tcis != null) {
                for (TriggerConsumerInt<?> c : tcis) {
                    tsp.queue(0, new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(c)));
                }
            }

        } else if (executor != null) {
            TriggerConsumerInt<?>[] tcis = lookupTriggerConsumers(trigger);
            if (tcis != null) {
                for (TriggerConsumerInt<?> c : tcis) {
                    executor.execute(new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(c)));
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

        TriggerConsumerInt<?>[] tcis = lookupTriggerConsumers(trigger);

        if (log.isEnabled()) log.getLogger().info("" + trigger);

        if (tcis != null) {
            for (TriggerConsumerInt<?> c : tcis) {
                SupplierConsumerTask<?> sct = new SupplierConsumerTask<>(trigger, new TriggerConsumerHolder<>(c));
                sct.run();
            }
        }

        return this;
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
//                SupplierConsumerTask<?> sct = new SupplierConsumerTask(trigger, new TriggerConsumerHolder<>(tci));
//                if (isScheduledTaskEnabled())
//                    tsp.queue(0, sct);
//                else if (executor != null)
//                    executor.execute(sct);
//                else
//                    sct.run();

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

    public TaskSchedulerProcessor getScheduler() {
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

//    @Override
//    public StateInt<?> lookupState(Enum<?> name) {
//        return lookupState(SUS.enumName(name));
//    }

    @Override
    public StateInt<?> getCurrentState() {
        return currentState.get();
    }

    @Override
    public void setCurrentState(StateInt<?> stateInt) {
        currentState.set(stateInt);
    }


    @Override
    public void close() {
        isClosed.getAndSet(true);

    }

    public boolean isClosed() {
        return isClosed.get();
    }


}
