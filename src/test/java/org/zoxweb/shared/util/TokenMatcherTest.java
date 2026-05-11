package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.filters.TokenMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TokenMatcherTest {

    // ==================== Tier classification ====================

    @Test
    public void tierExact() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("xlogistx.io");
        assertEquals(TokenMatcher.Tier.EXACT, tm.tierOf("xlogistx.io"));
    }

    @Test
    public void tierPrefix() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.*");
        assertEquals(TokenMatcher.Tier.PREFIX, tm.tierOf("api.*"));
    }

    @Test
    public void tierSuffix() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*.xlogistx.io");
        assertEquals(TokenMatcher.Tier.SUFFIX, tm.tierOf("*.xlogistx.io"));
    }

    @Test
    public void tierContains() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*xlogistx*");
        assertEquals(TokenMatcher.Tier.CONTAINS, tm.tierOf("*xlogistx*"));
    }

    @Test
    public void tierGlobInteriorStar() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.*.xlogistx.io");
        assertEquals(TokenMatcher.Tier.GLOB, tm.tierOf("api.*.xlogistx.io"));
    }

    @Test
    public void tierGlobQuestion() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("/?file.?xt");
        assertEquals(TokenMatcher.Tier.GLOB, tm.tierOf("/?file.?xt"));
    }

    @Test
    public void doubleStarsCollapse() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a**b");
        // normalized to "a*b", which has single star, not at ends → falls through to GLOB
        assertEquals(TokenMatcher.Tier.GLOB, tm.tierOf("a*b"));
        assertTrue(tm.containsRule("a*b"));
    }

    // ==================== Matching: each tier ====================

    @Test
    public void exactMatches() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("xlogistx.io");
        assertTrue(tm.matches("xlogistx.io"));
        assertFalse(tm.matches("api.xlogistx.io"));
        assertFalse(tm.matches("xlogistx.i"));
    }

    @Test
    public void exactLiteralWithDots() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.google.com");
        assertEquals(TokenMatcher.Tier.EXACT, tm.tierOf("api.google.com"));
        assertTrue(tm.matches("api.google.com"));
        assertFalse(tm.matches("api.google.co"));
        assertFalse(tm.matches("api.google.comm"));
        assertFalse(tm.matches("xapi.google.com"));
        assertFalse(tm.matches("google.com"));
    }

    @Test
    public void prefixMatches() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.*");
        assertTrue(tm.matches("api."));
        assertTrue(tm.matches("api.v2.xlogistx.io"));
        assertFalse(tm.matches("ap"));
        assertFalse(tm.matches("xapi.v2"));
    }

    @Test
    public void suffixMatches() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*.xlogistx.io");
        assertTrue(tm.matches("a.xlogistx.io"));
        assertTrue(tm.matches("a.b.c.xlogistx.io"));
        assertFalse(tm.matches("xlogistx.io"));
        assertFalse(tm.matches("xlogistx.ion"));
    }

    @Test
    public void containsMatches() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*xlogistx*");
        assertTrue(tm.matches("xlogistx"));
        assertTrue(tm.matches("ax.xlogistx.io"));
        assertTrue(tm.matches("---xlogistx---"));
        assertFalse(tm.matches("xlogist"));
    }

    @Test
    public void globInteriorStar() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.*.xlogistx.io");
        assertTrue(tm.matches("api.v1.xlogistx.io"));
        assertTrue(tm.matches("api.deeply.nested.xlogistx.io"));
        assertFalse(tm.matches("api.xlogistx.io"));   // no middle segment between the dots
        assertFalse(tm.matches("apiv1.xlogistx.io"));
    }

    @Test
    public void suffixWithQuestionMark() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*.xlogistx.?o");
        // Because it has BOTH '*' AND '?', it falls to the GLOB tier (not pure SUFFIX).
        assertEquals(TokenMatcher.Tier.GLOB, tm.tierOf("*.xlogistx.?o"));
        assertTrue(tm.matches("a.xlogistx.io"));
        assertTrue(tm.matches("a.xlogistx.co"));
        assertTrue(tm.matches("sub.api.xlogistx.io"));
        assertFalse(tm.matches("xlogistx.io"));            // missing the "*." prefix segment
        assertFalse(tm.matches("a.xlogistx.iom"));         // ?o requires exactly one char + 'o' at end
        assertFalse(tm.matches("a.xlogistx.iio"));         // ? matches exactly one char, not two
        assertFalse(tm.matches("a.xlogistx.o"));           // missing the ? character before 'o'
    }

    @Test
    public void globQuestionMark() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("/?file.?xt");
        assertTrue(tm.matches("/afile.txt"));
        assertTrue(tm.matches("/0file.4xt"));
        assertFalse(tm.matches("/file.txt"));     // missing the char before "file"
        assertFalse(tm.matches("/abfile.txt"));   // two chars where ? expects one
        assertFalse(tm.matches("/afile.tttxt"));
    }

    @Test
    public void matchAllStar() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*");
        assertTrue(tm.matches("anything"));
        assertTrue(tm.matches(""));
        assertTrue(tm.matches("a.b.c.d"));
    }

    // ==================== Case sensitivity ====================

    @Test
    public void caseSensitiveByDefault() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("Xlogistx.IO");
        assertTrue(tm.matches("Xlogistx.IO"));
        assertFalse(tm.matches("xlogistx.io"));
    }

    @Test
    public void caseInsensitiveMatches() {
        TokenMatcher tm = new TokenMatcher(true);
        tm.addPattern("*.XlogistX.io");
        assertTrue(tm.matches("API.XLOGISTX.IO"));
        assertTrue(tm.matches("api.xlogistx.io"));
    }

    @Test
    public void caseInsensitiveContains() {
        TokenMatcher tm = new TokenMatcher(true);
        tm.addPattern("*XlogistX*");
        assertTrue(tm.matches("X.XLOGISTX.Y"));
    }

    // ==================== CRUD ====================

    @Test
    public void addReturnsFalseOnDuplicate() {
        TokenMatcher tm = new TokenMatcher();
        assertTrue(tm.addPattern("foo"));
        assertFalse(tm.addPattern("foo"));
        assertEquals(1, tm.size());
    }

    @Test
    public void removeExisting() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("xlogistx.io");
        tm.addPattern("*.xlogistx.io");
        tm.addPattern("api.*.xlogistx.io");
        assertTrue(tm.removePattern("*.xlogistx.io"));
        assertFalse(tm.matches("api.xlogistx.io"));
        assertTrue(tm.matches("api.v1.xlogistx.io")); // glob still there
        assertTrue(tm.matches("xlogistx.io"));        // exact still there
        assertEquals(2, tm.size());
    }

    @Test
    public void removeMissing() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("foo");
        assertFalse(tm.removePattern("bar"));
        assertEquals(1, tm.size());
    }

    @Test
    public void updateReplacesAndReclassifies() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("foo");
        assertTrue(tm.updatePattern("foo", "*.foo"));
        assertEquals(TokenMatcher.Tier.SUFFIX, tm.tierOf("*.foo"));
        assertFalse(tm.matches("foo"));
        assertTrue(tm.matches("a.foo"));
    }

    @Test
    public void updateMissingReturnsFalse() {
        TokenMatcher tm = new TokenMatcher();
        assertFalse(tm.updatePattern("missing", "new"));
    }

    @Test
    public void updateCollisionReturnsFalse() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a");
        tm.addPattern("b");
        assertFalse(tm.updatePattern("a", "b"));
        assertEquals(2, tm.size());
        assertTrue(tm.containsRule("a"));
        assertTrue(tm.containsRule("b"));
    }

    @Test
    public void containsRule() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*.x");
        assertTrue(tm.containsRule("*.x"));
        assertFalse(tm.containsRule("*.y"));
    }

    @Test
    public void getAllReturnsArray() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a");
        tm.addPattern("b*");
        tm.addPattern("*c");
        String[] all = tm.getAll();
        assertEquals(3, all.length);
        List<String> asList = Arrays.asList(all);
        assertTrue(asList.containsAll(Arrays.asList("a", "b*", "*c")));
    }

    @Test
    public void getAllEmpty() {
        TokenMatcher tm = new TokenMatcher();
        assertEquals(0, tm.getAll().length);
    }

    @Test
    public void getRulesSnapshot() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a");
        tm.addPattern("b*");
        tm.addPattern("*c");
        assertEquals(3, tm.getRules().size());
        assertTrue(tm.getRules().containsAll(Arrays.asList("a", "b*", "*c")));
    }

    @Test
    public void clearResetsEverything() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("foo");
        tm.addPattern("*.bar");
        tm.addPattern("baz*");
        tm.addPattern("*qux*");
        tm.addPattern("a?b*c");
        tm.clear();
        assertEquals(0, tm.size());
        assertFalse(tm.matches("foo"));
        assertFalse(tm.matches("a.bar"));
    }

    @Test
    public void nullRuleThrows() {
        TokenMatcher tm = new TokenMatcher();
        assertThrows(NullPointerException.class, () -> tm.addPattern(null));
    }

    @Test
    public void emptyRuleThrows() {
        TokenMatcher tm = new TokenMatcher();
        assertThrows(IllegalArgumentException.class, () -> tm.addPattern(""));
    }

    @Test
    public void nullTokenIsNoMatch() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*");
        assertFalse(tm.matches(null));
        assertNull(tm.matchFirst(null));
        assertTrue(tm.matchAll(null).isEmpty());
    }

    // ==================== matchFirst / matchAll ====================

    @Test
    public void matchFirstReturnsFirstTier() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("xlogistx.io");
        tm.addPattern("*.io");
        tm.addPattern("*");
        assertEquals("xlogistx.io", tm.matchFirst("xlogistx.io"));
    }

    @Test
    public void matchAllCollectsAcrossTiers() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("xlogistx.io");
        tm.addPattern("*.io");
        tm.addPattern("xlog*");
        tm.addPattern("*xlogistx*");
        tm.addPattern("*");
        List<String> all = tm.matchAll("xlogistx.io");
        assertTrue(all.contains("xlogistx.io"));
        assertTrue(all.contains("*.io"));
        assertTrue(all.contains("xlog*"));
        assertTrue(all.contains("*xlogistx*"));
        assertTrue(all.contains("*"));
        assertEquals(5, all.size());
    }

    @Test
    public void matchFirstNullWhenNoMatch() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("foo");
        assertNull(tm.matchFirst("bar"));
    }

    // ==================== Adversarial globs ====================

    @Test
    public void adversarialGlobLinearTime() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a*a*a*a*a*a*b");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append('a');
        long start = System.nanoTime();
        assertFalse(tm.matches(sb.toString()));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        // Should be well under a second even on a slow machine. Allow generous bound.
        assertTrue(elapsedMs < 1000, "adversarial match took " + elapsedMs + "ms");
    }

    @Test
    public void minLenPrunesShortTokens() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("a?b?c");          // minLen = 5
        assertFalse(tm.matches("abc")); // too short
        assertTrue(tm.matches("a1b2c"));
    }

    // ==================== matchInLine (ad-hoc rule) ====================

    @Test
    public void matchInLineSuffixRule() {
        List<String> hits = TokenMatcher.matchInLine("*.xlogistx.io",
                "api.xlogistx.io", "www.xlogistx.io", "other.com", "xlogistx.io");
        assertEquals(Arrays.asList("api.xlogistx.io", "www.xlogistx.io"), hits);
    }

    @Test
    public void matchInLineGlobInterior() {
        List<String> hits = TokenMatcher.matchInLine("api.*.xlogistx.io",
                "api.v1.xlogistx.io", "api.v2.xlogistx.io", "api.xlogistx.io", "web.v1.xlogistx.io");
        assertEquals(Arrays.asList("api.v1.xlogistx.io", "api.v2.xlogistx.io"), hits);
    }

    @Test
    public void matchInLineQuestionMark() {
        List<String> hits = TokenMatcher.matchInLine("/?file.?xt",
                "/afile.txt", "/bfile.4xt", "/file.txt", "/abfile.txt");
        assertEquals(Arrays.asList("/afile.txt", "/bfile.4xt"), hits);
    }

    @Test
    public void matchInLineDoesNotMutateInstance() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("foo");
        TokenMatcher.matchInLine("*.bar", "a.bar", "b.bar");
        assertEquals(1, tm.size());
        assertFalse(tm.containsRule("*.bar"));
    }

    @Test
    public void matchInLineEmptyAndNullInputs() {
        assertTrue(TokenMatcher.matchInLine("*.io").isEmpty());
        assertTrue(TokenMatcher.matchInLine("*.io", (String[]) null).isEmpty());
        List<String> hits = TokenMatcher.matchInLine("*.io", "a.io", null, "b.io");
        assertEquals(Arrays.asList("a.io", "b.io"), hits);
    }

    @Test
    public void matchInLineNullRuleThrows() {
        assertThrows(NullPointerException.class, () -> TokenMatcher.matchInLine(null, "x"));
    }

    @Test
    public void matchInLineEmptyRuleThrows() {
        assertThrows(IllegalArgumentException.class, () -> TokenMatcher.matchInLine("", "x"));
    }

    @Test
    public void matchInLineStarMatchesAll() {
        List<String> hits = TokenMatcher.matchInLine("*", "a", "b", "c");
        assertEquals(Arrays.asList("a", "b", "c"), hits);
    }

    // ==================== Constructor with initial rules ====================

    @Test
    public void constructorWithInitialRules() {
        TokenMatcher tm = new TokenMatcher(true,
                Arrays.asList("api.*.xlogistx.io", "*.xlogistx.io", "/?file.?xt"));
        assertEquals(3, tm.size());
        assertTrue(tm.matches("API.V1.XLOGISTX.IO"));
        assertTrue(tm.matches("www.XLOGISTX.IO"));
        assertTrue(tm.matches("/Afile.Txt"));
    }

    // ==================== Concurrency ====================

    @Test
    public void concurrentReadWriteStress() throws Exception {
        final TokenMatcher tm = new TokenMatcher(true);
        for (int i = 0; i < 100; i++) tm.addPattern("rule" + i + ".*");

        final int readers = 6;
        final int writers = 2;
        final int iterations = 2000;
        ExecutorService exec = Executors.newFixedThreadPool(readers + writers);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(readers + writers);
        final AtomicInteger errors = new AtomicInteger();

        for (int r = 0; r < readers; r++) {
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        tm.matches("rule" + (i % 100) + ".something");
                    }
                } catch (Throwable t) { errors.incrementAndGet(); }
                finally { done.countDown(); }
            });
        }
        for (int w = 0; w < writers; w++) {
            final int wid = w;
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations / 4; i++) {
                        String r = "writer" + wid + "_" + i + ".*";
                        tm.addPattern(r);
                        tm.removePattern(r);
                    }
                } catch (Throwable t) { errors.incrementAndGet(); }
                finally { done.countDown(); }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "stress did not complete in time");
        exec.shutdown();
        assertEquals(0, errors.get(), "concurrent errors: " + errors.get());
        assertEquals(100, tm.size());
    }
}
