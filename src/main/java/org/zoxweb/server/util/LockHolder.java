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
package org.zoxweb.server.util;

import org.zoxweb.shared.util.SUS;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A utility class that wraps a {@link Lock} and provides conditional lock/unlock operations.
 * <p>
 * This class is useful in scenarios where locking behavior needs to be controlled by a
 * boolean flag, allowing the same code path to work with or without synchronization
 * based on runtime conditions.
 * </p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *     <li>Optional thread-safety: Enable/disable locking based on configuration</li>
 *     <li>Performance optimization: Skip locking in single-threaded contexts</li>
 *     <li>Conditional synchronization: Lock only when certain conditions are met</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * LockHolder lockHolder = new LockHolder();
 * boolean needsLocking = isMultiThreaded();
 *
 * lockHolder.lock(needsLocking);
 * try {
 *     // critical section - only synchronized if needsLocking is true
 *     performOperation();
 * } finally {
 *     lockHolder.unlock(needsLocking);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe when the underlying {@link Lock} is thread-safe
 * (which is the case for the default {@link ReentrantLock}).
 * </p>
 *
 * @see Lock
 * @see ReentrantLock
 */
public class LockHolder {

    /** The underlying lock instance */
    private final Lock lock;

    /**
     * Constructs a new LockHolder with a default {@link ReentrantLock}.
     */
    public LockHolder() {
        this(new ReentrantLock());
    }

    /**
     * Constructs a new LockHolder with the specified lock.
     *
     * @param lock the lock to use for synchronization
     * @throws NullPointerException if lock is null
     */
    public LockHolder(Lock lock) {
        SUS.checkIfNull("null lock", lock);
        this.lock = lock;
    }

    /**
     * Conditionally acquires the lock.
     * <p>
     * If the parameter is {@code true}, the lock is acquired (blocking if necessary).
     * If {@code false}, this method does nothing.
     * </p>
     *
     * @param lock if true, acquire the lock; if false, do nothing
     */
    public void lock(boolean lock) {
        if (lock)
            this.lock.lock();
    }

    /**
     * Conditionally releases the lock.
     * <p>
     * If the parameter is {@code true}, the lock is released.
     * If {@code false}, this method does nothing.
     * </p>
     * <p>
     * <b>Important:</b> The parameter value should match the value used in the
     * corresponding {@link #lock(boolean)} call to maintain proper lock balance.
     * </p>
     *
     * @param unlock if true, release the lock; if false, do nothing
     */
    public void unlock(boolean unlock) {
        if (unlock)
            this.lock.unlock();
    }

}
