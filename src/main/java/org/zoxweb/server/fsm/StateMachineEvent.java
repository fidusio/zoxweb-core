package org.zoxweb.server.fsm;

import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.util.GetName;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable observability event emitted by a {@link StateMachine} to its
 * {@link StateMachineListener}s.
 * <p>
 * Carries: a JVM-global sequence {@link #getID() id} (total order for traceability),
 * the creation {@link #getTimeStamp() timestamp} (inherited from {@link BaseEventObject}),
 * the emitting machine ({@link #getSource()} / {@link #machine()}), the {@link Type},
 * and — depending on the type — the trigger ({@link #getData()}), its
 * {@link #getCanonicalID() canonical ID}, the {@link #getConsumer() TriggerConsumer}
 * that processed it, the old/new states, and the consumer count of a publish snapshot.
 * </p>
 * <p>
 * <b>Lifetime contract:</b> an event is valid only for the duration of the
 * {@link StateMachineListener} callback {@code handleEvent(StateMachineEvent)}. Listeners MUST NOT retain
 * it — a retained event pins its states, consumer, trigger, and the trigger's payload,
 * preventing garbage collection (e.g. of deregistered states). For durable history keep
 * the {@link #toLog()} string instead (see {@link StateMachineEventHistory}).
 * </p>
 */
public class StateMachineEvent
        extends BaseEventObject<TriggerInt<?>> {

    /**
     * The observable event types.
     */
    public enum Type
            implements GetName {
        /** The current-state marker actually changed (old state null = pre-init). */
        STATE_CHANGED("state-changed"),
        /** A trigger entered the machine; consumer count is the dispatch snapshot size (0 = unrouted). */
        TRIGGER_PUBLISHED("trigger-published"),
        /** A TriggerConsumer completed processing a trigger — the per-consumer execution record. */
        TRIGGER_CONSUMED("trigger-consumed"),
        /** A state was registered with the machine. */
        STATE_REGISTERED("state-registered"),
        /** A state was deregistered from the machine. */
        STATE_DEREGISTERED("state-deregistered"),
        /** The machine started (fired before the INIT trigger is published). */
        MACHINE_STARTED("machine-started"),
        /** The machine closed (fired exactly once). */
        MACHINE_CLOSED("machine-closed"),
        ;

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final static AtomicLong SEQUENCE = new AtomicLong();

    private final long id = SEQUENCE.incrementAndGet();
    private final Type type;
    private final TriggerConsumerInt<?> consumer;
    private final StateInt<?> oldState;
    private final StateInt<?> newState;
    private final int consumerCount;

    StateMachineEvent(StateMachineInt<?> source,
                      Type type,
                      TriggerInt<?> trigger,
                      TriggerConsumerInt<?> consumer,
                      StateInt<?> oldState,
                      StateInt<?> newState,
                      int consumerCount) {
        super(source, trigger);
        this.type = type;
        this.consumer = consumer;
        this.oldState = oldState;
        this.newState = newState;
        this.consumerCount = consumerCount;
    }

    /**
     * @return JVM-global monotonically increasing event id — total order for history reconstruction
     */
    public long getID() {
        return id;
    }

    public Type getType() {
        return type;
    }

    /**
     * @return the trigger's canonical ID (routing key) for TRIGGER_PUBLISHED/TRIGGER_CONSUMED, null otherwise
     */
    public String getCanonicalID() {
        return getData() != null ? getData().getCanonicalID() : null;
    }

    /**
     * @return the TriggerConsumer that processed the trigger (TRIGGER_CONSUMED only) — its
     * {@code getState()} identifies the owning state
     */
    public TriggerConsumerInt<?> getConsumer() {
        return consumer;
    }

    /**
     * @return the previous state for STATE_CHANGED (null = pre-init), null otherwise
     */
    public StateInt<?> getOldState() {
        return oldState;
    }

    /**
     * @return the new state for STATE_CHANGED (null = marker cleared), or the subject state
     * for STATE_REGISTERED/STATE_DEREGISTERED
     */
    public StateInt<?> getNewState() {
        return newState;
    }

    /**
     * @return the dispatch snapshot size for TRIGGER_PUBLISHED (0 = no consumer routed), 0 otherwise
     */
    public int getConsumerCount() {
        return consumerCount;
    }

    /**
     * @return the emitting state machine
     */
    public StateMachineInt<?> machine() {
        return (StateMachineInt<?>) getSource();
    }

    /**
     * Renders this event as a single log line for history traceability:
     * sequence id, formatted timestamp, machine name, event type, and type-specific details.
     *
     * @return the log line
     */
    public String toLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(id)
                .append(' ').append(DateUtil.DEFAULT_DATE_FORMAT_TZ.format(getTimeStamp()))
                .append(' ').append(machine().getName())
                .append(' ').append(type.getName());
        switch (type) {
            case STATE_CHANGED:
                sb.append(' ').append(oldState).append(" -> ").append(newState);
                break;
            case TRIGGER_PUBLISHED:
                sb.append(" canID=").append(getCanonicalID())
                        .append(" trigger#").append(getData().getID())
                        .append(" consumers=").append(consumerCount);
                break;
            case TRIGGER_CONSUMED:
                sb.append(" canID=").append(getCanonicalID())
                        .append(" trigger#").append(getData().getID())
                        .append(" consumer=").append(consumer);
                break;
            case STATE_REGISTERED:
            case STATE_DEREGISTERED:
                sb.append(' ').append(newState);
                break;
            default:
                break;
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toLog();
    }
}
