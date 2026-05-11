package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.zoxweb.shared.filters.TokenMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Microbenchmark for {@link TokenMatcher#matches(String)}.
 * <p>
 * Disabled by default. Run with:
 * <pre>
 * mvn -DskipTests=false -Dbench=true -Dtest=TokenMatcherBenchmark test
 * </pre>
 *
 * <p>This is a hand-rolled bench, not JMH — accuracy is best-effort. Each scenario
 * warms up the JIT for {@link #WARMUP_MS} ms then measures for {@link #MEASURE_MS} ms.
 */
@EnabledIfSystemProperty(named = "bench", matches = "true")
public class TokenMatcherBenchmark {

    private static final long WARMUP_MS = 2000L;
    private static final long MEASURE_MS = 5000L;
    private static final int CONCURRENT_THREADS = Runtime.getRuntime().availableProcessors();

    // ==================== Single-rule scenarios ====================

    @Test
    public void benchExactHit() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.google.com");
        runSingleThread("exact hit", tm, new String[]{"api.google.com"});
    }

    @Test
    public void benchExactMiss() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.google.com");
        runSingleThread("exact miss", tm, new String[]{"api.notmatching.com"});
    }

    @Test
    public void benchSuffixHit() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("*.xlogistx.io");
        runSingleThread("suffix hit (single rule)", tm, new String[]{"tracker.ads.xlogistx.io"});
    }

    @Test
    public void benchGlobInteriorHit() {
        TokenMatcher tm = new TokenMatcher();
        tm.addPattern("api.*.xlogistx.io");
        runSingleThread("glob interior hit", tm, new String[]{"api.v2.xlogistx.io"});
    }

    // ==================== Realistic DNS blocklist scenarios ====================

    @Test
    public void benchBlocklist1000SuffixHit() {
        TokenMatcher tm = buildSuffixBlocklist(1000);
        // Hits a randomly-chosen rule from the list
        String[] queries = {"sub.blocked042.example", "x.blocked500.example", "a.b.blocked999.example"};
        runSingleThread("blocklist 1000 suffix HIT (round-robin)", tm, queries);
    }

    @Test
    public void benchBlocklist1000SuffixMiss() {
        TokenMatcher tm = buildSuffixBlocklist(1000);
        String[] queries = {"www.google.com", "api.github.io", "store.apple.com", "cdn.cloudflare.net"};
        runSingleThread("blocklist 1000 suffix MISS", tm, queries);
    }

    @Test
    public void benchBlocklist1000Mixed() {
        TokenMatcher tm = buildSuffixBlocklist(1000);
        // 80% miss / 20% hit — typical DNS traffic against an ad-blocker
        String[] queries = {
                "www.google.com", "api.github.io", "store.apple.com", "cdn.cloudflare.net",
                "news.ycombinator.com", "en.wikipedia.org", "raw.githubusercontent.com", "fonts.googleapis.com",
                "sub.blocked042.example", "x.blocked500.example"
        };
        runSingleThread("blocklist 1000 mixed (80% miss / 20% hit)", tm, queries);
    }

    // ==================== Concurrent ====================

    @Test
    public void benchConcurrentBlocklist1000Mixed() throws Exception {
        TokenMatcher tm = buildSuffixBlocklist(1000);
        String[] queries = {
                "www.google.com", "api.github.io", "store.apple.com", "cdn.cloudflare.net",
                "news.ycombinator.com", "en.wikipedia.org", "raw.githubusercontent.com", "fonts.googleapis.com",
                "sub.blocked042.example", "x.blocked500.example"
        };
        runConcurrent("CONCURRENT (" + CONCURRENT_THREADS + " threads) blocklist 1000 mixed", tm, queries);
    }

    // ==================== Helpers ====================

    private static TokenMatcher buildSuffixBlocklist(int n) {
        TokenMatcher tm = new TokenMatcher(true);
        Random rnd = new Random(42);
        String[] tlds = {"com", "net", "org", "io", "co", "example"};
        for (int i = 0; i < n; i++) {
            String tld = tlds[rnd.nextInt(tlds.length)];
            tm.addPattern("*.blocked" + String.format("%03d", i) + "." + tld);
        }
        return tm;
    }

    private static void runSingleThread(String label, TokenMatcher tm, String[] queries) {
        // Warmup
        long warmupOps = loopFor(tm, queries, WARMUP_MS);

        // Measurement
        long startNs = System.nanoTime();
        long ops = loopFor(tm, queries, MEASURE_MS);
        long elapsedNs = System.nanoTime() - startNs;

        report(label, ops, elapsedNs, 1, warmupOps);
    }

    private static long loopFor(TokenMatcher tm, String[] queries, long durationMs) {
        long deadline = System.nanoTime() + durationMs * 1_000_000L;
        long ops = 0;
        // Unroll the array to reduce loop overhead per measurement iteration
        while ((ops & 0xFFFF) != 0 || System.nanoTime() < deadline) {
            for (int i = 0; i < queries.length; i++) {
                if (tm.matches(queries[i])) ops++; else ops++;
            }
        }
        return ops;
    }

    private static void runConcurrent(String label, TokenMatcher tm, String[] queries) throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        AtomicLong totalOps = new AtomicLong();

        // Warmup
        runConcurrentPhase(exec, tm, queries, WARMUP_MS, new AtomicLong());

        // Measurement
        long startNs = System.nanoTime();
        runConcurrentPhase(exec, tm, queries, MEASURE_MS, totalOps);
        long elapsedNs = System.nanoTime() - startNs;

        exec.shutdown();
        report(label, totalOps.get(), elapsedNs, CONCURRENT_THREADS, 0L);
    }

    private static void runConcurrentPhase(ExecutorService exec, TokenMatcher tm, String[] queries,
                                           long durationMs, AtomicLong total) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_THREADS);
        List<Runnable> tasks = new ArrayList<Runnable>(CONCURRENT_THREADS);
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            tasks.add(() -> {
                try {
                    start.await();
                    long localOps = loopFor(tm, queries, durationMs);
                    total.addAndGet(localOps);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        for (Runnable r : tasks) exec.submit(r);
        start.countDown();
        done.await(durationMs * 3, TimeUnit.MILLISECONDS);
    }

    private static void report(String label, long ops, long elapsedNs, int threads, long warmupOps) {
        double seconds = elapsedNs / 1e9;
        double opsPerSec = ops / seconds;
        double nsPerOp = (double) elapsedNs / ops;
        double nsPerOpPerThread = nsPerOp * threads;
        System.out.printf(
                "%-55s  ops=%,12d  %,10.0f ops/s  %7.1f ns/op%s  (warmup=%,d)%n",
                label, ops, opsPerSec, nsPerOp,
                threads > 1 ? String.format("  [%.1f ns/op/thread]", nsPerOpPerThread) : "",
                warmupOps);
    }
}
