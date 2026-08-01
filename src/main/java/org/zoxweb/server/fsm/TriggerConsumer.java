package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.NVGMProperties;
import org.zoxweb.shared.util.SUS;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Base {@link TriggerConsumerInt} implementation. Subclasses implement
 * {@link #accept(Object)} with the processing logic; the base class holds the canonical IDs,
 * the owning-state back-reference, an execution counter, and the publish helpers that
 * delegate to the owning state's machine.
 * <p>
 * The String-varargs constructor rejects an empty canonical ID list — a consumer with no
 * IDs could never be dispatched.
 * </p>
 *
 * @param <T> the type of trigger data this consumer accepts
 */
public abstract class TriggerConsumer<T>
        extends NVGMProperties
        implements TriggerConsumerInt<T> {
    public final static LogWrapper log = new LogWrapper(TriggerConsumer.class).setEnabled(false);

    private final String[] canonicalIDs;
    private StateInt<?> state;
    protected AtomicLong execCounter = new AtomicLong();
    private Function<T, ?> function;


    public TriggerConsumer(Function<T, ?> f, String... canonicalIDs) {
        this(canonicalIDs);
        function = f;
    }

    public TriggerConsumer(Function<T, ?> f, Enum<?>... canonicalIDs) {
        this(canonicalIDs);
        function = f;
    }

    public TriggerConsumer(String... canonicalIDs) {
        super(false);
        if (SUS.isEmpty(canonicalIDs)) {
            throw new IllegalArgumentException("Argument 'canonicalIDs' must not be empty.");
        }
        this.canonicalIDs = canonicalIDs;
    }

    public TriggerConsumer(Enum<?>... gnCanonicalIDs) {
        super(false);
        canonicalIDs = SUS.enumNames(gnCanonicalIDs);
    }

    @Override
    public String[] canonicalIDs() {
        return canonicalIDs;
    }

    @Override
    public StateInt<?> getState() {
        return state;
    }

    @Override
    public void setState(StateInt<?> state) {
        this.state = state;
    }

    @Override
    public <R> TriggerConsumerInt<T> setFunction(Function<?, R> function) {
        this.function = (Function<T, ?>) function;
        return this;
    }

    @Override
    public <R> Function getFunction() {
        return function;
    }


    @Override
    public String toString() {
        return "TriggerConsumer{" +
                "canonicalIDs=" + Arrays.toString(canonicalIDs) +
                ", state=" + state + ", exec-counter=" + execCounter.get() +
                '}';
    }

    @Override
    public void publish(TriggerInt<?> triggerInt) {
        if (triggerInt != null)
            getState().getStateMachine().publish(triggerInt);
    }

    @Override
    public <D> void publish(String canID, D data) {
        if (canID != null)
            getState().getStateMachine().publish(new Trigger(getState(), canID, data));
    }

    public <D> void publish(Enum<?> canID, D data) {
        if (canID != null)
            getState().getStateMachine().publish(new Trigger(getState(), SUS.enumName(canID), data));
    }

    @Override
    public void publishSync(TriggerInt<?> triggerInt) {
        if (triggerInt != null)
            getState().getStateMachine().publishSync(triggerInt);
    }

    public <D> void publishSync(String canID, D data) {
        if (canID != null)
            getState().getStateMachine().publishSync(new Trigger(getState(), canID, data));
    }

    public <D> void publishSync(Enum<?> canID, D data) {
        if (canID != null)
            getState().getStateMachine().publishSync(new Trigger(getState(), SUS.enumName(canID), data));
    }

    @Override
    public StateMachineInt<?> getStateMachine() {
        return state.getStateMachine();
    }
}
