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
package org.zoxweb.server.io;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileWatcherTest
{

    /**
     * Drives a {@link FileWatcher} via {@link TaskUtil#defaultTaskScheduler()} (a
     * {@link java.util.concurrent.ScheduledExecutorService}) and verifies it fires
     * exactly once, and only when the watched file's mtime advances.
     */
    @Test
    public void detectChangeViaScheduler() throws Exception
    {
        File tmp = File.createTempFile("filewatcher", ".tmp");
        tmp.deleteOnExit();
        // Pin a deterministic baseline well clear of filesystem mtime granularity.
        long base = (System.currentTimeMillis() / 1000L) * 1000L;
        assertTrue(tmp.setLastModified(base), "could not seed mtime");

        AtomicInteger hits = new AtomicInteger();
        AtomicReference<File[]> seen = new AtomicReference<>();
        FileWatcher watcher = new FileWatcher(files -> {
            hits.incrementAndGet();
            seen.set(files);
        }, tmp);

        ScheduledFuture<?> future = TaskUtil.defaultTaskScheduler()
                .scheduleAtFixedRate(watcher, 0, 50, TimeUnit.MILLISECONDS);
        try
        {
            // Poll several times with no change: the consumer must not fire.
            Thread.sleep(300);
            assertEquals(0, hits.get(), "watcher fired without a change");

            // Advance the mtime forward (simulating a cert renewal / file rewrite).
            assertTrue(tmp.setLastModified(base + 5000L), "could not advance mtime");

            long deadline = System.currentTimeMillis() + 2000L;
            while (hits.get() == 0 && System.currentTimeMillis() < deadline)
                Thread.sleep(20);

            assertEquals(1, hits.get(), "change should fire exactly once");
            // Default constructor uses reportAllIfOneFileModified == true; with a single watched
            // file the reported set is just that file.
            assertEquals(1, seen.get().length);
            assertSame(tmp, seen.get()[0]);
            assertSame(tmp, watcher.files()[0]);
            assertTrue(watcher.lastAccess() > 0, "lastAccess not stamped");

            // No further changes -> no further callbacks.
            Thread.sleep(300);
            assertEquals(1, hits.get(), "watcher fired again without a change");
        }
        finally
        {
            future.cancel(true);
            tmp.delete();
        }
    }

    /**
     * With {@code reportAllIfOneFileModified == true} the consumer must receive every watched file
     * (not just the changed one) as soon as any single file changes. Also exercises
     * {@link FileWatcher#addFile(File)} / {@link FileWatcher#removeFile(File)}.
     */
    @Test
    public void reportAllAndDynamicSet() throws Exception
    {
        File a = File.createTempFile("fw-a", ".tmp");
        File b = File.createTempFile("fw-b", ".tmp");
        File c = File.createTempFile("fw-c", ".tmp");
        for (File f : new File[]{a, b, c})
            f.deleteOnExit();

        long base = (System.currentTimeMillis() / 1000L) * 1000L;
        for (File f : new File[]{a, b, c})
            assertTrue(f.setLastModified(base), "could not seed mtime");

        AtomicReference<File[]> seen = new AtomicReference<>();
        // reportAllIfOneFileModified == true -> report the whole set on any change.
        FileWatcher watcher = new FileWatcher(true, seen::set, a, b);

        assertTrue(watcher.addFile(c), "c should be added");
        assertFalse(watcher.addFile(b), "b already watched");
        assertTrue(watcher.removeFile(b), "b should be removed");
        assertFalse(watcher.removeFile(b), "b already removed");
        assertEquals(2, watcher.files().length, "expected a and c");

        // Change only c; consumer should still receive the full set {a, c}.
        assertTrue(c.setLastModified(base + 5000L), "could not advance mtime");
        watcher.run();

        File[] reported = seen.get();
        assertNotNull(reported, "no change reported");
        Set<File> set = new HashSet<>(Arrays.asList(reported));
        assertEquals(new HashSet<>(Arrays.asList(a, c)), set, "report-all should include unchanged files");

        // No further change -> run() must not invoke the consumer again.
        seen.set(null);
        watcher.run();
        assertNull(seen.get(), "watcher fired without a change");
    }

    /**
     * With {@code reportAllIfOneFileModified == false} the consumer must receive only the files that
     * actually changed, not the whole watched set.
     */
    @Test
    public void reportChangedOnly() throws Exception
    {
        File a = File.createTempFile("fw-x", ".tmp");
        File b = File.createTempFile("fw-y", ".tmp");
        a.deleteOnExit();
        b.deleteOnExit();

        long base = (System.currentTimeMillis() / 1000L) * 1000L;
        assertTrue(a.setLastModified(base), "could not seed mtime");
        assertTrue(b.setLastModified(base), "could not seed mtime");

        AtomicReference<File[]> seen = new AtomicReference<>();
        FileWatcher watcher = new FileWatcher(false, seen::set, a, b);

        // Change only a; consumer should receive exactly {a}.
        assertTrue(a.setLastModified(base + 5000L), "could not advance mtime");
        watcher.run();

        File[] reported = seen.get();
        assertNotNull(reported, "no change reported");
        assertEquals(1, reported.length, "report-changed-only should exclude unchanged files");
        assertSame(a, reported[0]);
    }

    /**
     * The consumer is now optional at construction (deferred via {@link FileWatcher#setConsumer}),
     * and a null/empty file array is tolerated. The non-null check lives on {@code setConsumer}.
     */
    @Test
    public void nullHandlingContract() throws Exception
    {
        // Null consumer and null files are both tolerated by the constructors.
        FileWatcher deferred = new FileWatcher(null, new File("does-not-exist"));
        assertNull(deferred.getConsumer());
        FileWatcher noFiles = new FileWatcher(true, null);
        assertEquals(0, noFiles.files().length);

        // setConsumer rejects null but accepts a real consumer.
        assertThrows(NullPointerException.class, () -> deferred.setConsumer(null));
        Consumer<File[]> c = files -> {};
        deferred.setConsumer(c);
        assertSame(c, deferred.getConsumer());
    }

    /**
     * A change observed by a deferred watcher must reach the consumer once it is set, provided the
     * consumer is installed before the watcher is polled.
     */
    @Test
    public void deferredConsumerReceivesChange() throws Exception
    {
        File f = File.createTempFile("fw-deferred", ".tmp");
        f.deleteOnExit();
        long base = (System.currentTimeMillis() / 1000L) * 1000L;
        assertTrue(f.setLastModified(base), "could not seed mtime");

        FileWatcher watcher = new FileWatcher(true, null, f);
        AtomicReference<File[]> seen = new AtomicReference<>();
        watcher.setConsumer(seen::set);

        assertTrue(f.setLastModified(base + 5000L), "could not advance mtime");
        watcher.run();

        assertNotNull(seen.get(), "consumer set before poll should receive the change");
        assertSame(f, seen.get()[0]);
    }
}
