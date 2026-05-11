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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.ListAsArray;
import org.zoxweb.shared.util.NamedDescription;
import org.zoxweb.shared.util.SUS;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe token matcher with full CRUD over a set of glob-style rules.
 * <p>
 * Supported wildcards:
 * <ul>
 *   <li>{@code *} — zero or more arbitrary characters</li>
 *   <li>{@code ?} — exactly one arbitrary character</li>
 * </ul>
 * Rules are classified at insertion time into one of five tiers, each with its
 * own optimized matching strategy:
 * <ol>
 *   <li><b>EXACT</b> — no wildcards. O(1) hash lookup.</li>
 *   <li><b>PREFIX</b> — {@code lit*}. Stored in a prefix trie; O(L) walk where L = token length.</li>
 *   <li><b>SUFFIX</b> — {@code *lit}. Stored in a reverse-prefix trie; O(L) walk.</li>
 *   <li><b>CONTAINS</b> — {@code *lit*}. Substring search per entry.</li>
 *   <li><b>GLOB</b> — anything more complex. Iterative two-pointer matcher with
 *       pre-extracted literal head/tail and min-length pruning.</li>
 * </ol>
 * Matching proceeds tier by tier; the first hit short-circuits {@link #matches(String)}
 * and {@link #matchFirst(String)}. {@link #matchAll(String)} collects every match.
 * <p>
 * Examples:
 * <pre>
 * TokenMatcher tm = new TokenMatcher(true);
 * tm.addRule("api.*.xlogistx.io");
 * tm.addRule("*.xlogistx.io");
 * tm.addRule("/?file.?xt");
 * tm.matches("api.v2.xlogistx.io"); // true
 * tm.matches("www.xlogistx.io");    // true
 * tm.matches("/afile.txt");         // true
 * </pre>
 */
public class TokenMatcher
        extends NamedDescription
        implements ValueFilter<String, String> {


    /** Internal tier classification. */
    public enum Tier {EXACT, PREFIX, SUFFIX, CONTAINS, GLOB}

    private static final char STAR = '*';
    private static final char ANY = '?';

    private static final class RuleEntry {
        final String original;
        final String key;
        final Tier tier;
        final CompiledGlob glob;

        RuleEntry(String original, String key, Tier tier, CompiledGlob glob) {
            this.original = original;
            this.key = key;
            this.tier = tier;
            this.glob = glob;
        }
    }

    private static final class TrieNode {
        // ConcurrentHashMap provides safe lock-free reads under concurrent writer mutation.
        final ConcurrentHashMap<Character, TrieNode> children = new ConcurrentHashMap<Character, TrieNode>(4);
        // Non-null means terminal; volatile ensures readers observe a fully-published value.
        volatile String rule;
    }

    private final boolean caseInsensitive;

    // Read-path containers are lock-free. Writers serialize on `this`.
    private final Map<String, RuleEntry> ruleMap = new ConcurrentHashMap<String, RuleEntry>();
    private final Set<String> exactSet = ConcurrentHashMap.newKeySet();
    private final TrieNode prefixTrie = new TrieNode();
    private final TrieNode suffixTrie = new TrieNode();
    // CONTAINS / GLOB tiers — small lists scanned linearly. ListAsArray gives readers a
    // lock-free snapshot via asArray() while writers mutate under their own monitor.
    private final ListAsArray<RuleEntry> containsRules =
            new ListAsArray<RuleEntry>(new ArrayList<RuleEntry>(), new RuleEntry[0]);
    private final ListAsArray<RuleEntry> globRules =
            new ListAsArray<RuleEntry>(new ArrayList<RuleEntry>(), new RuleEntry[0]);

    public TokenMatcher() {
        this(false);
    }

    public TokenMatcher(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public TokenMatcher(boolean caseInsensitive, Iterable<String> initialRules) {
        this.caseInsensitive = caseInsensitive;
        if (initialRules != null) {
            for (String r : initialRules) addPattern(r);
        }
    }

    public TokenMatcher(boolean caseInsensitive, String... initialRules) {
        this.caseInsensitive = caseInsensitive;
        if (initialRules != null) {
            for (String r : initialRules) addPattern(r);
        }
    }

    /**
     * Validate the object
     *
     * @param in value to be validated
     * @return validated acceptable value
     * @throws NullPointerException     if in is null
     * @throws IllegalArgumentException if in is invalid
     */
    @Override
    public String validate(String in) throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("null in value", in);
        in = in.trim();
        if (matches(in)) {
            if (isCaseInsensitive()) {
                return DataEncoder.StringLower.encode(in);
            } else {
                return in;
            }
        }

        throw new IllegalArgumentException("invalid value: " + in);

    }

    /**
     * Check if the value is valid
     *
     * @param in value to be checked
     * @return true if in value valid
     */
    @Override
    public boolean isValid(String in) {
        return matches(in);
    }

    /**
     * Converts the implementing object in its canonical form.
     *
     * @return text identification of the object
     */
    @Override
    public String toCanonicalID() {
        return getName();
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    // ==================== CRUD ====================

    /**
     * Adds a rule.
     *
     * @param pattern glob-style rule with optional {@code *} / {@code ?}
     * @return {@code true} if newly added; {@code false} if an identical rule already existed
     * @throws NullPointerException if rule is null
     * @throws IllegalArgumentException if rule is empty
     */
    public boolean addPattern(String pattern) {
        String norm = normalize(pattern);
        synchronized (this) {
            if (ruleMap.containsKey(norm)) return false;
            RuleEntry e = classify(norm);
            install(e);
            ruleMap.put(norm, e);
            return true;
        }
    }

    /**
     * Removes a previously added rule (compared by canonical form).
     *
     * @return {@code true} if the rule was present and removed
     */
    public boolean removePattern(String pattern) {
        String norm = normalize(pattern);
        synchronized (this) {
            RuleEntry e = ruleMap.remove(norm);
            if (e == null) return false;
            uninstall(e);
            return true;
        }
    }

    /**
     * Replaces {@code oldRule} with {@code newRule} atomically.
     *
     * @return {@code true} on success; {@code false} if {@code oldRule} was not present,
     *         or if {@code newRule} differs from {@code oldRule} and already exists
     */
    public boolean updatePattern(String oldPattern, String newPattern) {
        String oldNorm = normalize(oldPattern);
        String newNorm = normalize(newPattern);
        synchronized (this) {
            if (!ruleMap.containsKey(oldNorm)) return false;
            if (!oldNorm.equals(newNorm) && ruleMap.containsKey(newNorm)) return false;
            RuleEntry old = ruleMap.remove(oldNorm);
            uninstall(old);
            if (oldNorm.equals(newNorm)) {
                install(old);
                ruleMap.put(oldNorm, old);
                return true;
            }
            RuleEntry fresh = classify(newNorm);
            install(fresh);
            ruleMap.put(newNorm, fresh);
            return true;
        }
    }

    /**
     * @return {@code true} if the exact canonical rule string is registered.
     *         This is a rule-lookup, not a match — use {@link #matches(String)} for matching.
     */
    public boolean containsRule(String rule) {
        if (rule == null) return false;
        String norm;
        try {
            norm = normalize(rule);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return ruleMap.containsKey(norm);
    }

    /** @return an unmodifiable snapshot of every registered rule (order is unspecified). */
    public Set<String> getRules() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(ruleMap.keySet()));
    }

    /** @return every registered rule as a fresh array (order is unspecified). */
    public String[] getAll() {
        return ruleMap.keySet().toArray(new String[0]);
    }

    public int size() {
        return ruleMap.size();
    }

    public void clear() {
        synchronized (this) {
            ruleMap.clear();
            exactSet.clear();
            prefixTrie.children.clear();
            prefixTrie.rule = null;
            suffixTrie.children.clear();
            suffixTrie.rule = null;
            containsRules.clear();
            globRules.clear();
        }
    }

    // ==================== Matching ====================

    /** @return {@code true} if at least one rule matches {@code token}. */
    public boolean matches(String token) {
        if (token == null) return false;
        String t = caseInsensitive ? token.toLowerCase() : token;
        if (exactSet.contains(t)) return true;
        if (triePrefixHasMatch(prefixTrie, t, false)) return true;
        if (triePrefixHasMatch(suffixTrie, t, true)) return true;
        for (RuleEntry e : containsRules.asArray()) {
            if (t.contains(e.key)) return true;
        }
        for (RuleEntry e : globRules.asArray()) {
            if (globAccepts(e.glob, t)) return true;
        }
        return false;
    }

    /** @return the first rule (in tier order) that matches {@code token}, or {@code null}. */
    public String matchFirst(String token) {
        if (token == null) return null;
        String t = caseInsensitive ? token.toLowerCase() : token;
        if (exactSet.contains(t)) return t;
        String r = trieFirst(prefixTrie, t, false);
        if (r != null) return r;
        r = trieFirst(suffixTrie, t, true);
        if (r != null) return r;
        for (RuleEntry e : containsRules.asArray()) {
            if (t.contains(e.key)) return e.original;
        }
        for (RuleEntry e : globRules.asArray()) {
            if (globAccepts(e.glob, t)) return e.original;
        }
        return null;
    }

    /** @return every rule that matches {@code token}, in tier order; empty list if none. */
    public List<String> matchAll(String token) {
        List<String> out = new ArrayList<String>();
        if (token == null) return out;
        String t = caseInsensitive ? token.toLowerCase() : token;
        if (exactSet.contains(t)) out.add(t);
        trieAll(prefixTrie, t, false, out);
        trieAll(suffixTrie, t, true, out);
        for (RuleEntry e : containsRules.asArray()) {
            if (t.contains(e.key)) out.add(e.original);
        }
        for (RuleEntry e : globRules.asArray()) {
            if (globAccepts(e.glob, t)) out.add(e.original);
        }
        return out;
    }

    /** Exposed for tests / introspection. */
    public Tier tierOf(String rule) {
        String norm = normalize(rule);
        RuleEntry e = ruleMap.get(norm);
        return e == null ? null : e.tier;
    }

    // ==================== Internals ====================

    private String normalize(String rule) {
        if (rule == null) throw new NullPointerException("rule");
        if (rule.isEmpty()) throw new IllegalArgumentException("rule cannot be empty");
        String r = caseInsensitive ? rule.toLowerCase() : rule;
        return collapseStars(r);
    }

    private static String collapseStars(String r) {
        if (r.indexOf("**") < 0) return r;
        StringBuilder sb = new StringBuilder(r.length());
        boolean prevStar = false;
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if (c == STAR) {
                if (!prevStar) sb.append(c);
                prevStar = true;
            } else {
                sb.append(c);
                prevStar = false;
            }
        }
        return sb.toString();
    }

    /**
     * One-shot ad-hoc match: returns the subset of {@code toMatch} that matches the given
     * inline {@code rule}. The rule is NOT added to any stored rule set — it is used only
     * for this invocation. Supports {@code *} (zero or more chars) and {@code ?} (exactly one).
     * <p>
     * Comparison is case-sensitive; pre-lower both inputs if case-insensitive matching is needed.
     * Null entries inside {@code toMatch} are skipped.
     *
     * @param rule    glob-style rule (e.g., {@code "*.xlogistx.io"}, {@code "api.*.xlogistx.io"})
     * @param toMatch candidate tokens
     * @return subset of {@code toMatch} matching the rule, in input order; never null
     * @throws NullPointerException     if {@code rule} is null
     * @throws IllegalArgumentException if {@code rule} is empty
     */
    public static List<String> matchInLine(String rule, String... toMatch) {
        if (rule == null) throw new NullPointerException("rule");
        if (rule.isEmpty()) throw new IllegalArgumentException("rule cannot be empty");
        List<String> out = new ArrayList<String>();
        if (toMatch == null || toMatch.length == 0) return out;
        String pattern = collapseStars(rule);
        for (int i = 0; i < toMatch.length; i++) {
            String candidate = toMatch[i];
            if (candidate == null) continue;
            if (globMatch(pattern, candidate)) out.add(candidate);
        }
        return out;
    }

    private RuleEntry classify(String rule) {
        int firstStar = rule.indexOf(STAR);
        int lastStar = rule.lastIndexOf(STAR);
        boolean hasStar = firstStar >= 0;
        boolean hasQ = rule.indexOf(ANY) >= 0;

        if (!hasStar && !hasQ) {
            return new RuleEntry(rule, rule, Tier.EXACT, null);
        }
        if (!hasQ && firstStar == lastStar) {
            if (firstStar == rule.length() - 1) {
                return new RuleEntry(rule, rule.substring(0, rule.length() - 1), Tier.PREFIX, null);
            }
            if (firstStar == 0) {
                return new RuleEntry(rule, rule.substring(1), Tier.SUFFIX, null);
            }
        }
        if (!hasQ && rule.length() >= 3
                && rule.charAt(0) == STAR
                && rule.charAt(rule.length() - 1) == STAR
                && countStars(rule) == 2) {
            return new RuleEntry(rule, rule.substring(1, rule.length() - 1), Tier.CONTAINS, null);
        }
        return new RuleEntry(rule, rule, Tier.GLOB, CompiledGlob.compile(rule));
    }

    private static int countStars(String s) {
        int c = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == STAR) c++;
        return c;
    }

    private void install(RuleEntry e) {
        switch (e.tier) {
            case EXACT:    exactSet.add(e.key); break;
            case PREFIX:   trieInsert(prefixTrie, e.key, e.original, false); break;
            case SUFFIX:   trieInsert(suffixTrie, e.key, e.original, true);  break;
            case CONTAINS: containsRules.add(e); break;
            case GLOB:     globRules.add(e); break;
        }
    }

    private void uninstall(RuleEntry e) {
        switch (e.tier) {
            case EXACT:    exactSet.remove(e.key); break;
            case PREFIX:   trieRemove(prefixTrie, e.key, false); break;
            case SUFFIX:   trieRemove(suffixTrie, e.key, true);  break;
            case CONTAINS: containsRules.remove(e); break;
            case GLOB:     globRules.remove(e); break;
        }
    }

    // ----- trie -----

    private static void trieInsert(TrieNode root, String key, String rule, boolean reverse) {
        TrieNode n = root;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char c = reverse ? key.charAt(len - 1 - i) : key.charAt(i);
            TrieNode child = n.children.get(c);
            if (child == null) {
                TrieNode created = new TrieNode();
                TrieNode existing = n.children.putIfAbsent(c, created);
                child = existing != null ? existing : created;
            }
            n = child;
        }
        n.rule = rule; // volatile publish — terminal := (rule != null)
    }

    private static void trieRemove(TrieNode root, String key, boolean reverse) {
        TrieNode n = root;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char c = reverse ? key.charAt(len - 1 - i) : key.charAt(i);
            n = n.children.get(c);
            if (n == null) return;
        }
        n.rule = null;
    }

    private static boolean triePrefixHasMatch(TrieNode root, String token, boolean reverse) {
        TrieNode n = root;
        if (n.rule != null) return true; // empty-key rule (defensive — should not happen)
        int len = token.length();
        for (int i = 0; i < len; i++) {
            char c = reverse ? token.charAt(len - 1 - i) : token.charAt(i);
            n = n.children.get(c);
            if (n == null) return false;
            if (n.rule != null) return true;
        }
        return false;
    }

    private static String trieFirst(TrieNode root, String token, boolean reverse) {
        TrieNode n = root;
        String r = n.rule;
        if (r != null) return r;
        int len = token.length();
        for (int i = 0; i < len; i++) {
            char c = reverse ? token.charAt(len - 1 - i) : token.charAt(i);
            n = n.children.get(c);
            if (n == null) return null;
            r = n.rule;
            if (r != null) return r;
        }
        return null;
    }

    private static void trieAll(TrieNode root, String token, boolean reverse, List<String> out) {
        TrieNode n = root;
        String r = n.rule;
        if (r != null) out.add(r);
        int len = token.length();
        for (int i = 0; i < len; i++) {
            char c = reverse ? token.charAt(len - 1 - i) : token.charAt(i);
            n = n.children.get(c);
            if (n == null) return;
            r = n.rule;
            if (r != null) out.add(r);
        }
    }

    // ----- glob -----

    static final class CompiledGlob {
        final String pattern;
        final String head;
        final String tail;
        final int minLen;

        CompiledGlob(String pattern, String head, String tail, int minLen) {
            this.pattern = pattern;
            this.head = head;
            this.tail = tail;
            this.minLen = minLen;
        }

        static CompiledGlob compile(String rule) {
            int min = 0;
            for (int i = 0; i < rule.length(); i++) {
                if (rule.charAt(i) != STAR) min++;
            }
            int firstWc = -1;
            for (int i = 0; i < rule.length(); i++) {
                char c = rule.charAt(i);
                if (c == STAR || c == ANY) {
                    firstWc = i;
                    break;
                }
            }
            int lastWc = -1;
            for (int i = rule.length() - 1; i >= 0; i--) {
                char c = rule.charAt(i);
                if (c == STAR || c == ANY) {
                    lastWc = i;
                    break;
                }
            }
            String head = "";
            String tail = "";
            if (firstWc > 0) head = rule.substring(0, firstWc);
            if (lastWc >= 0 && lastWc < rule.length() - 1) {
                String maybeTail = rule.substring(lastWc + 1);
                if (maybeTail.indexOf(ANY) < 0) tail = maybeTail;
            }
            return new CompiledGlob(rule, head, tail, min);
        }
    }

    private static boolean globAccepts(CompiledGlob g, String token) {
        if (token.length() < g.minLen) return false;
        if (!g.head.isEmpty() && !token.startsWith(g.head)) return false;
        if (!g.tail.isEmpty() && !token.endsWith(g.tail)) return false;
        return globMatch(g.pattern, token);
    }

    private static boolean globMatch(String pattern, String text) {
        int pi = 0, ti = 0;
        int starPi = -1, starTi = -1;
        int pLen = pattern.length(), tLen = text.length();
        while (ti < tLen) {
            if (pi < pLen && (pattern.charAt(pi) == ANY || pattern.charAt(pi) == text.charAt(ti))) {
                pi++;
                ti++;
            } else if (pi < pLen && pattern.charAt(pi) == STAR) {
                starPi = pi;
                starTi = ti;
                pi++;
            } else if (starPi != -1) {
                pi = starPi + 1;
                starTi++;
                ti = starTi;
            } else {
                return false;
            }
        }
        while (pi < pLen && pattern.charAt(pi) == STAR) pi++;
        return pi == pLen;
    }
}
