package org.zoxweb.server.fsm;

import java.util.function.Consumer;

/**
 * Internal dispatch wrapper used by {@link StateMachine} around every consumer it dispatches.
 * Before delegating to the wrapped consumer it performs the per-dispatch bookkeeping:
 * incrementing the consumer's exec counter and updating the machine's current-state marker
 * to the consumer's owning state.
 * <p>
 * Package-private by design — it is the machine's plumbing, ensuring all trigger delivery
 * flows through {@link StateMachine}'s publish methods.
 * </p>
 *
 * @param <T> the consumer's input type
 */
public class TriggerConsumerHolder<T>
        implements Consumer<T> {
    private volatile Consumer<T> inner;


    TriggerConsumerHolder(Consumer<?> inner) {
        this.inner = (Consumer<T>) inner;
    }

    public void accept(T t) {
        if (inner instanceof TriggerConsumer) {
            TriggerConsumer temp = (TriggerConsumer) inner;
            temp.execCounter.incrementAndGet();
            temp.getState().getStateMachine().setCurrentState(temp.getState());
        }

        if (TriggerConsumer.log.isEnabled())
            TriggerConsumer.log.getLogger().info("" + inner);

        inner.accept(t);
    }
}
