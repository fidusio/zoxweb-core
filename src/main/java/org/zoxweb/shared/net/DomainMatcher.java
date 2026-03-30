package org.zoxweb.shared.net;

import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.SUS;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Thread-safe domain matcher with tiered lookup strategy.
 * <p>
 * Patterns are classified into three tiers for optimal matching performance:
 * <ul>
 *   <li><b>Tier 1 — Exact:</b> literal domains with no wildcards, matched via O(1) hash lookup.</li>
 *   <li><b>Tier 2 — Suffix:</b> patterns of the form {@code *.suffix} (e.g. {@code *.xlogistx.io}),
 *       matched by walking parent domain labels against a hash set.</li>
 *   <li><b>Tier 3 — Glob:</b> complex patterns containing {@code *} or {@code ?} in non-prefix
 *       positions, matched via iterative glob scan.</li>
 * </ul>
 * All pattern and domain comparisons are case-insensitive.
 */
public class DomainMatcher {

    // Tier 1: exact match — O(1)
    private final Set<String> exactSet = ConcurrentHashMap.newKeySet();
    // Tier 2: suffix match for "*.suffix" patterns — O(label count)
    private final Set<String> suffixSet = ConcurrentHashMap.newKeySet();
    // Tier 3: complex globs (?, or * not at prefix) — linear scan
    private final List<String> globPatterns = new CopyOnWriteArrayList<>();

    /**
     * Adds a domain pattern to the matcher.
     * <p>
     * The pattern is automatically classified into the appropriate tier:
     * <ul>
     *   <li>No wildcards → exact set</li>
     *   <li>{@code *.suffix} with no further wildcards → suffix set</li>
     *   <li>All other wildcard patterns → glob list</li>
     * </ul>
     *
     * @param pattern the domain pattern (e.g. {@code "xlogistx.io"}, {@code "*.xlogistx.io"}, {@code "api-?.example.com"})
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public void addPattern(String pattern) {
        if(SUS.isEmpty(pattern)) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        pattern = DataEncoder.StringLower.encode(pattern);


        if (pattern.indexOf('*') == -1 && pattern.indexOf('?') == -1) {
            // no wildcards → exact
            exactSet.add(pattern);
        } else if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(2);
            if (suffix.indexOf('*') == -1 && suffix.indexOf('?') == -1) {
                // pure suffix wildcard like *.xlogistx.io
                suffixSet.add(suffix);
            } else {
                globPatterns.add(pattern);
            }
        } else {
            globPatterns.add(pattern);
        }
    }

    /**
     * Removes a previously added pattern from the matcher.
     *
     * @param pattern the pattern to remove
     * @return {@code true} if the pattern was found and removed
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public boolean removePattern(String pattern) {
        if(SUS.isEmpty(pattern)) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        pattern = DataEncoder.StringLower.encode(pattern);
        return exactSet.remove(pattern)
                || suffixSet.remove(pattern.startsWith("*.") ? pattern.substring(2) : pattern)
                || globPatterns.remove(pattern);
    }

    /**
     * Tests whether a domain matches any of the registered patterns.
     * <p>
     * Matching proceeds through tiers in order (exact → suffix → glob)
     * and returns on the first hit.
     *
     * @param domain the domain name to test (e.g. {@code "api.xlogistx.io"})
     * @return {@code true} if the domain matches at least one pattern
     * @throws NullPointerException if domain is null
     */
    public boolean matches(String domain) {
        SUS.checkIfNull("Null pattern", domain);
        domain = DataEncoder.StringLower.encode(domain);

        // Tier 1: exact
        if (exactSet.contains(domain))
            return true;

        // Tier 2: suffix set — also match the bare domain itself
        // "a.b.xlogistx.io" checks "a.b.xlogistx.io", "b.xlogistx.io", then "xlogistx.io"
        if (suffixSet.contains(domain))
            return true;
        int dot = domain.indexOf('.');
        while (dot != -1) {
            if (suffixSet.contains(domain.substring(dot + 1)))
                return true;
            dot = domain.indexOf('.', dot + 1);
        }

        // Tier 3: glob patterns
        for (String pattern : globPatterns) {
            if (globMatch(pattern, domain))
                return true;
        }

        return false;
    }

    /**
     * Iterative glob matching — no regex, no recursion.
     * <ul>
     *   <li>{@code *} matches zero or more of any character</li>
     *   <li>{@code ?} matches exactly one character</li>
     * </ul>
     *
     * @param pattern the glob pattern
     * @param text    the text to match against
     * @return {@code true} if the text matches the pattern
     */
    private static boolean globMatch(String pattern, String text) {
        int pi = 0, ti = 0;
        int starPi = -1, starTi = -1;
        int pLen = pattern.length(), tLen = text.length();

        while (ti < tLen) {
            if (pi < pLen && (pattern.charAt(pi) == '?' || pattern.charAt(pi) == text.charAt(ti))) {
                pi++;
                ti++;
            } else if (pi < pLen && pattern.charAt(pi) == '*') {
                // record backtrack anchor
                starPi = pi;
                starTi = ti;
                pi++;
            } else if (starPi != -1) {
                // backtrack: let * consume one more char
                pi = starPi + 1;
                starTi++;
                ti = starTi;
            } else {
                return false;
            }
        }
        // consume trailing *'s in pattern
        while (pi < pLen && pattern.charAt(pi) == '*')
            pi++;

        return pi == pLen;
    }

    /**
     * Returns the total number of registered patterns across all tiers.
     *
     * @return the pattern count
     */
    public int size() {
        return exactSet.size() + suffixSet.size() + globPatterns.size();
    }
}