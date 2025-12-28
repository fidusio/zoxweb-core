package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public abstract class TriggerConsumer<T>
        implements TriggerConsumerInt<T> {
    public final static LogWrapper log = new LogWrapper(TriggerConsumer.class).setEnabled(false);

    private final String[] canonicalIDs;
    private StateInt<?> state;
    protected AtomicLong execCounter = new AtomicLong();
    private Function<T, ?> function;
    private NVGenericMap nvgmConfig = null;


    public TriggerConsumer(Function<T, ?> f, String... canonicalIDs) {
        this(canonicalIDs);
        function = f;
    }

    public TriggerConsumer(Function<T, ?> f, Enum<?>... canonicalIDs) {
        this(canonicalIDs);
        function = f;
    }

    public TriggerConsumer(String... canonicalIDs) {
        this.canonicalIDs = canonicalIDs;
    }

    public TriggerConsumer(Enum<?>... gnCanonicalIDs) {
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
    public void setSate(StateInt<?> state) {
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

    public void setProperties(NVGenericMap nvgm) {
        this.nvgmConfig = nvgm;
    }

    public NVGenericMap getProperties() {
        return nvgmConfig;
    }
}
