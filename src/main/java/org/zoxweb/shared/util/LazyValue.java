/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.util;

import java.util.function.Supplier;

/**
 * A thread-safe, lazily initialized value holder.
 *
 * <p>The value is not created until the first call to {@link #get()}; that call
 * invokes the supplied creator exactly once and every subsequent call returns the
 * cached result. Initialization uses double-checked locking on a {@code volatile}
 * field, so the fast path (value already present) is lock-free and only the very
 * first access pays for synchronization.
 *
 * <p>The monitor to synchronize on is supplied by the caller rather than created
 * internally. This lets an owner share its own lock across several lazy fields
 * (typically by passing {@code this}) so that initialization is serialized with
 * the owner's other synchronized state and no extra lock objects are allocated
 * per field. A {@code LazyValue} is therefore only as cheap as its creator: it is
 * meant for members that are expensive or rarely needed, for example a property
 * bag or a listener collection that most instances never touch.
 *
 * <pre>{@code
 * private final LazyValue<NVGenericMap> properties =
 *         new LazyValue<>(this, () -> new NVGenericMap("properties"));
 *
 * public NVGenericMap getProperties() {
 *     return properties.get();   // allocated on first use only
 * }
 * }</pre>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The creator must return a non-{@code null} value. A {@code null} result
 *       is not cached and the creator will be invoked again on the next
 *       {@link #get()}, because {@code null} is the "not yet initialized" marker.</li>
 *   <li>If the creator throws, nothing is cached and the exception propagates to
 *       the caller of {@link #get()}; a later call retries.</li>
 *   <li>The creator must not call {@link #get()} on the same instance (it would
 *       re-enter the lock and recurse).</li>
 *   <li>The value, once created, is never replaced; there is no reset.</li>
 * </ul>
 *
 * <p>Being in {@code shared}, the class uses only {@code synchronized} and
 * {@code volatile}; it has no dependency on {@code java.util.concurrent}.
 *
 * @param <V> the type of the held value
 * @see Supplier
 */
public class LazyValue<V>
        implements Supplier<V> {

    private final Object lock;
    private volatile V value;
    private final Supplier<V> valueCreator;

    /**
     * Creates an uninitialized holder.
     *
     * @param lock         the monitor to synchronize on during first
     *                     initialization; usually the owning object ({@code this})
     *                     so that several lazy fields share one lock
     * @param valueCreator the factory invoked once, on first {@link #get()}, to
     *                     produce the value; must return a non-{@code null} result
     * @throws NullPointerException if {@code lock} or {@code valueCreator} is {@code null}
     */
    public LazyValue(Object lock, Supplier<V> valueCreator) {
        SUS.checkIfNulls("lock and creator can't be null.", lock, valueCreator);
        this.lock = lock;
        this.valueCreator = valueCreator;

    }

    /**
     * Returns the value, creating it on the first call.
     *
     * <p>Concurrent first-time callers block on the shared lock; exactly one of
     * them runs the creator and all of them observe the same instance. Once the
     * value exists this method is a plain volatile read.
     *
     * @return the lazily created value
     * @throws RuntimeException whatever the creator throws, in which case the
     *         value stays uninitialized
     */
    public V get() {
        if (value == null) {
            synchronized (lock) {
                if (value == null)
                    value = valueCreator.get();
            }
        }
        return value;
    }

    /**
     * Tells whether the value has already been created, without triggering
     * creation. Useful for cleanup paths that should not allocate the value just
     * to tear it down (for example, "close the listeners only if there are any").
     *
     * @return {@code true} if {@link #get()} has completed successfully at least once
     */
    public boolean isInitialized() {
        return value != null;
    }
}
