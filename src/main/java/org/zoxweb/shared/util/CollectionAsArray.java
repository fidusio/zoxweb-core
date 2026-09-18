package org.zoxweb.shared.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Wraps a {@link Collection} and maintains a cached array snapshot of its contents,
 * following the copy-on-write pattern: all mutations ({@link #add(Object[])},
 * {@link #remove(Object[])}, {@link #clear()}) are synchronized and refresh the
 * snapshot, while {@link #asArray()} is lock-free and returns the last published
 * snapshot via a volatile read.
 * <p>
 * This makes it suited for read-mostly use cases where iteration must be fast and
 * must never throw {@code ConcurrentModificationException}, such as dispatching
 * events to a set of listeners. The returned array must be treated as read-only:
 * it is shared by all callers until the next mutation replaces it.
 *
 * @param <T> the element type
 */
public class CollectionAsArray<T> {
    private final Collection<T> collection;
    private final T[] empty;
    private volatile T[] vals;


    /**
     * Creates a wrapper around the given collection and takes the initial snapshot.
     *
     * @param col   the backing collection; all mutations are applied to it
     * @param empty a zero-length array of the element type, used as the type token
     *              for {@link Collection#toArray(Object[])} and returned as-is when
     *              the collection is empty
     * @throws NullPointerException     if col or empty is null
     * @throws IllegalArgumentException if empty is not of length 0
     */
    public CollectionAsArray(Collection<T> col, T[] empty) {
        SUS.checkIfNulls("list", col, empty);
        if (empty.length != 0)
            throw new IllegalArgumentException("array must be of size 0");
        this.collection = col;
        this.empty = empty;
        this.vals = collection.toArray(empty);
    }

    /**
     * Adds the given elements to the backing collection and refreshes the array snapshot.
     *
     * @param t the elements to add
     * @return this instance, for fluent chaining
     */
    public CollectionAsArray<T> add(T... t) {
        synchronized (this) {
            Collections.addAll(collection, t);
            this.vals = collection.toArray(empty);
        }
        return this;
    }

    /**
     * Removes the given elements from the backing collection and refreshes the array snapshot.
     *
     * @param t the elements to remove
     * @return this instance, for fluent chaining
     */
    public CollectionAsArray<T> remove(T... t) {
        synchronized (this) {
            for (T r : t)
                collection.remove(r);
            this.vals = collection.toArray(empty);
        }
        return this;
    }


    /**
     * Removes all elements from the backing collection and resets the snapshot to the empty array.
     */
    public void clear() {
        synchronized (this) {
            collection.clear();
            this.vals = collection.toArray(empty);
        }
    }

    /**
     * Returns whether the current snapshot contains an element equal to {@code toCheck},
     * as determined by {@link Objects#equals(Object, Object)}.
     * <p>
     * Lock-free: the check runs against the snapshot published by the last completed
     * mutation, so an element being added or removed concurrently may or may not be seen.
     * A null argument is allowed and matches a null element.
     *
     * @param toCheck the element to look for, may be null
     * @return true if an equal element is present in the snapshot
     */
    public boolean contains(T toCheck) {
        return contains(toCheck, null);
    }

    /**
     * Returns whether the current snapshot contains an element that the given matcher
     * accepts against {@code toCheck}.
     * <p>
     * For every element {@code v} of the snapshot the matcher is invoked as
     * {@code matcher.matches(v, toCheck)}: the stored element is the {@code ref}
     * argument and the probe is the {@code to} argument. The matcher is responsible
     * for handling null elements. Same snapshot semantics as {@link #contains(Object)}.
     *
     * @param toCheck the element to look for, may be null
     * @param matcher the matching rule; if null, plain {@link Objects#equals(Object, Object)} is used
     * @return true if the matcher accepts at least one element of the snapshot
     */
    public boolean contains(T toCheck, RefMatcher<T, T> matcher) {
        for (T v : asArray()) {
            if (matcher != null) {
                if (matcher.matches(v, toCheck)) return true;
            } else {
                if (Objects.equals(v, toCheck)) return true;
            }
        }
        return false;
    }

    /**
     * Returns the current array snapshot without locking.
     * <p>
     * The array reflects the collection as of the last completed mutation and is
     * shared by all callers: it must not be modified. Safe to call concurrently
     * with mutations; never returns null.
     *
     * @return the cached snapshot of the collection's contents
     */
    public T[] asArray() {
        return vals;
    }
}
