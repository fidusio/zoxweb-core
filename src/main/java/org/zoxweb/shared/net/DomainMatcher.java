package org.zoxweb.shared.net;

import org.zoxweb.shared.filters.TokenMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, case-insensitive domain matcher.
 * <p>
 * Thin facade over {@link TokenMatcher} configured for case-insensitive matching, plus DNS-style
 * apex auto-registration: when a pure suffix pattern like {@code *.foo.com} is added, the bare
 * apex {@code foo.com} is automatically also matched. This mirrors the X.509 / cookie convention
 * where {@code *.foo.com} covers both subdomains and the apex itself.
 * <p>
 * Apex auto-registration is reference-counted: removing {@code *.foo.com} also removes the
 * implicit apex, unless the user has explicitly added {@code foo.com} as its own pattern (in
 * which case the apex stays).
 * <p>
 * Supported wildcards: {@code *} (zero or more) and {@code ?} (exactly one).
 *
 * @see TokenMatcher
 */
public class DomainMatcher {

    private final TokenMatcher tokenMatcher = new TokenMatcher(true);
    // The patterns the user explicitly added (lowercased, stars-collapsed). This is the
    // public-facing rule set and the source of truth for size() and dedup.
    private final Set<String> userPatterns = ConcurrentHashMap.newKeySet();
    // Refcount of "*.<apex>" patterns per apex literal. While count > 0 we keep <apex>
    // registered in tokenMatcher so the bare apex matches too. Guarded by writeMutex.
    private final Map<String, Integer> apexRefcount = new HashMap<String, Integer>();
    private final Object writeMutex = new Object();

    /**
     * Adds a domain pattern.
     *
     * @param pattern e.g. {@code "xlogistx.io"}, {@code "*.xlogistx.io"}, {@code "api-?.example.com"}
     * @return {@code true} if newly added; {@code false} if already present
     * @throws NullPointerException     if pattern is null
     * @throws IllegalArgumentException if pattern is empty
     */
    public boolean addPattern(String pattern) {
        String norm = normalize(pattern);
        synchronized (writeMutex) {
            if (!userPatterns.add(norm)) return false;
            // May return false if the apex auto-added this exact literal earlier; that's fine.
            tokenMatcher.addPattern(norm);
            String apex = pureSuffixApex(norm);
            if (apex != null) {
                Integer cur = apexRefcount.get(apex);
                int next = (cur == null ? 0 : cur) + 1;
                apexRefcount.put(apex, next);
                if (next == 1 && !userPatterns.contains(apex)) {
                    tokenMatcher.addPattern(apex);
                }
            }
            return true;
        }
    }

    /**
     * Removes a previously added pattern.
     *
     * @return {@code true} if the pattern was present and removed
     * @throws NullPointerException     if pattern is null
     * @throws IllegalArgumentException if pattern is empty
     */
    public boolean removePattern(String pattern) {
        String norm = normalize(pattern);
        synchronized (writeMutex) {
            if (!userPatterns.remove(norm)) return false;

            String apex = pureSuffixApex(norm);
            if (apex != null) {
                int next = apexRefcount.get(apex) - 1;
                if (next == 0) {
                    apexRefcount.remove(apex);
                    // Drop the implicit apex unless the user also explicitly added it.
                    if (!userPatterns.contains(apex)) {
                        tokenMatcher.removePattern(apex);
                    }
                } else {
                    apexRefcount.put(apex, next);
                }
            }

            // Remove the user's pattern from the engine — unless it's an exact literal still
            // referenced as an apex by some surviving "*.<that-literal>" pattern.
            if (isExactLiteral(norm) && apexRefcount.containsKey(norm)) {
                // leave it in tokenMatcher; an outstanding *.X still needs it
            } else {
                tokenMatcher.removePattern(norm);
            }
            return true;
        }
    }

    /**
     * Tests whether a domain matches any registered pattern.
     *
     * @return {@code true} on match; {@code false} on no match or when {@code domain} is null
     */
    public boolean matches(String domain) {
        return tokenMatcher.matches(domain);
    }

    /** @return number of patterns the user has explicitly added (apex auto-registrations are not counted). */
    public int size() {
        return userPatterns.size();
    }

    /** @return every user-added pattern as a fresh array (apex auto-registrations are not included). */
    public String[] getAll() {
        return userPatterns.toArray(new String[0]);
    }

    // -------- internals --------

    private static String normalize(String pattern) {
        if (pattern == null) throw new NullPointerException("pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern cannot be empty");
        return collapseStars(pattern.toLowerCase());
    }

    /** Returns the apex literal for a pure {@code *.<literal>} pattern (no other wildcards), else null. */
    private static String pureSuffixApex(String norm) {
        if (!norm.startsWith("*.") || norm.length() < 3) return null;
        String rest = norm.substring(2);
        if (rest.indexOf('*') >= 0 || rest.indexOf('?') >= 0) return null;
        return rest;
    }

    private static boolean isExactLiteral(String s) {
        return s.indexOf('*') < 0 && s.indexOf('?') < 0;
    }

    private static String collapseStars(String r) {
        if (r.indexOf("**") < 0) return r;
        StringBuilder sb = new StringBuilder(r.length());
        boolean prev = false;
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if (c == '*') {
                if (!prev) sb.append(c);
                prev = true;
            } else {
                sb.append(c);
                prev = false;
            }
        }
        return sb.toString();
    }
}
