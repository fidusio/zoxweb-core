package org.zoxweb.shared.util;

/**
 * A two-argument predicate that decides whether a candidate value {@code to} matches
 * a reference value {@code ref}. The two-argument counterpart of {@link Validator};
 * used for example by {@link CollectionAsArray#contains(Object, RefMatcher)}.
 *
 * @param <R> the reference type
 * @param <T> the candidate type
 */
public interface RefMatcher<R, T> {

    /**
     * Case-insensitive string equality. Returns false when {@code ref} is null;
     * a null {@code to} is handled by {@link String#equalsIgnoreCase(String)} and
     * yields false.
     */
    RefMatcher<String, String> IgnoreCase = (ref, to) -> {
        if (ref != null)
            return ref.equalsIgnoreCase(to);

        return false;
    };

    /**
     * Case-insensitive string equality after trimming both sides with
     * {@link SUS#trimOrNull(String)}. A null, empty or whitespace-only {@code ref}
     * yields false, so two blank strings do not match each other; a blank {@code to}
     * never matches a non-blank {@code ref}.
     */
    RefMatcher<String, String> TrimIgnoreCase = (ref, to) -> {
        ref = SUS.trimOrNull(ref);
        to = SUS.trimOrNull(to);
        if (ref != null)
            return ref.equalsIgnoreCase(to);

        return false;
    };

    /**
     * @param ref the reference value
     * @param to  the candidate value to match against {@code ref}
     * @return true if {@code to} matches {@code ref}
     */
    boolean matches(R ref, T to);
}
