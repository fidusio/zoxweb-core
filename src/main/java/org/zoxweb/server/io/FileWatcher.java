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

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.SUS;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * A lightweight, poll-based watcher for one or more {@link File}s.
 * <p>
 * Each invocation of {@link #run()} samples every watched file's last-modified timestamp and,
 * when at least one has advanced since the previous sample, notifies the supplied consumer once
 * with the full set of files. The class holds no thread of its own: it is meant to be driven
 * externally, typically by a scheduler or timer that calls {@link #run()} at a fixed interval.
 * <p>
 * The set of watched files may be changed at runtime via {@link #addFile(File)} /
 * {@link #removeFile(File)}; those mutators and the per-poll {@link #checkFiles()} sweep are
 * {@code synchronized} on this instance, so the file set is consistent under concurrent access.
 * The consumer is invoked outside the lock.
 * <p>
 * {@link #lastAccess} is {@code volatile} so it may be read safely from a thread other than the
 * one running the poll.
 */
public class FileWatcher
        implements Runnable {


    public static final LogWrapper log = new LogWrapper(FileWatcher.class);
    /** Callback invoked, with the files to report, whenever a change is detected. */
    private volatile Consumer<File[]> consumer;
    /**
     * Watched files mapped to their last-modified baseline (epoch millis). A {@link LinkedHashMap}
     * so iteration—and therefore the order of files handed to the consumer—follows insertion order.
     */
    private final Map<File, Long> filesMap = new LinkedHashMap<>();
    /** Wall-clock time of the most recent detected change (epoch millis). */
    private volatile long lastAccess;
    /**
     * Selects which files are passed to the consumer when a change is detected:
     * {@code true} delivers every watched file as long as at least one changed, {@code false}
     * delivers only the files that actually changed this poll.
     */
    private final boolean reportAllIfOneFileModified;

    public FileWatcher(boolean reportAllIfOneFileModified) {
        this(reportAllIfOneFileModified, null, (File[]) null);
    }
    /**
     * Creates a watcher with no files yet; add them later via {@link #addFile(File)}.
     *
     * @param reportAllIfOneFileModified when {@code true}, the consumer receives every watched file
     *                                   as long as at least one changed; when {@code false}, it
     *                                   receives only the files that changed on a given poll
     * @param action                     callback invoked each time a change is detected; may be null
     *                                   and supplied later via {@link #setConsumer(Consumer)}
     */
    public FileWatcher(boolean reportAllIfOneFileModified, Consumer<File[]> action) {
        this(reportAllIfOneFileModified, action, (File[]) null);
    }


    /**
     * Creates a watcher that reports every watched file whenever any one of them changes
     * (i.e. {@code reportAllIfOneFileModified == true}).
     *
     * @param consumer callback invoked with the watched files; may be null and supplied later via
     *                 {@link #setConsumer(Consumer)}
     * @param files    the files to monitor (null array and null elements are skipped)
     */
    public FileWatcher(Consumer<File[]> consumer, File... files) {
        this(true, consumer, files);
    }

    /**
     * Creates a watcher for the given files.
     * <p>
     * The consumer may be supplied later via {@link #setConsumer(Consumer)}; set it before the
     * watcher is scheduled, since a change observed by {@link #run()} while no consumer is set is
     * consumed (its baseline advances) and not re-reported later.
     *
     * @param reportAllIfOneFileModified when {@code true}, the consumer receives every watched file
     *                                   as long as at least one changed; when {@code false}, it
     *                                   receives only the files that changed on a given poll
     * @param action                     callback invoked each time a change is detected; may be null
     *                                   (see above)
     * @param files                      the files to monitor (null array and null elements are skipped)
     */
    public FileWatcher(boolean reportAllIfOneFileModified, Consumer<File[]> action, File... files) {


        this.reportAllIfOneFileModified = reportAllIfOneFileModified;
        if (action != null) {
            setConsumer(action);
        }

        // Seed the baseline so the first real modification, not the initial state, triggers the callback.
        if (files != null) {
            for (File file : files) {
                if (file != null)
                    filesMap.put(file, file.lastModified());
            }
        }
        lastAccess = System.currentTimeMillis();
    }

    /**
     * Sets (or replaces) the callback invoked when a change is detected.
     *
     * @param consumer the callback; must be non-null
     * @throws NullPointerException if {@code consumer} is null
     */
    public synchronized void setConsumer(Consumer<File[]> consumer) {
        SUS.checkIfNulls("consumer can't be null", consumer);
        this.consumer = consumer;
    }

    /**
     * @return the current callback, or null if none has been set yet
     */
    public synchronized Consumer<File[]> getConsumer() {
        return consumer;
    }


    /**
     * Samples every watched file and advances the baseline for any whose last-modified timestamp
     * has moved forward. Using {@code >} (rather than {@code !=}) means a file replaced with an
     * older timestamp does not trigger, and a momentarily-missing file (e.g. {@code lastModified()}
     * returning {@code 0} mid-rename) is ignored rather than reported as a change.
     * <p>
     * This is the change-detection step used by {@link #run()}, exposed so a caller can perform an
     * on-demand, real-time check (e.g. immediately before using a watched resource) instead of
     * waiting for the next scheduled poll. The caller acts on the returned files directly. Note
     * that a detected change advances that file's baseline and is therefore consumed by this call:
     * a subsequent {@link #run()} or {@code checkFiles()} will not report it again, and the
     * {@code consumer} is not invoked for a change observed here—so when mixing on-demand checks
     * with a scheduled {@link #run()}, whichever fires first handles the change.
     *
     * @return the files that should be reported—all watched files when
     *         {@link #reportAllIfOneFileModified} is {@code true}, otherwise only the changed ones—or
     *         {@code null} if nothing changed since the previous sample
     */
    public synchronized File[] checkFiles() {
        List<File> modifiedList = new ArrayList<>();
        for (File file : filesMap.keySet()) {
            long tempLastModified = file.lastModified();
            if(log.isEnabled()) log.getLogger().info("Checking " + file.getAbsolutePath() + " last modified: " + new Date(tempLastModified));
            if (tempLastModified > filesMap.get(file)) {
                filesMap.put(file, tempLastModified);
                modifiedList.add(file);
            }
        }

        return modifiedList.isEmpty() ? null : (reportAllIfOneFileModified ? filesMap.keySet().toArray(new File[0]) : modifiedList.toArray(new File[0]));
    }

    /**
     * Adds a file to the watched set, seeding its baseline to the file's current last-modified time
     * so only subsequent changes trigger the consumer.
     *
     * @param file the file to watch
     * @return {@code true} if the file was added; {@code false} if it was null or already watched
     */
    public synchronized boolean addFile(File file) {
        if(log.isEnabled()) log.getLogger().info("Will try ot add file: " + file);
        if (file != null && !filesMap.containsKey(file)) {
            filesMap.put(file, file.lastModified());
            if(log.isEnabled()) log.getLogger().info("File: " + file + " ADDED");
            return true;
        }
        return false;
    }

    /**
     * Removes a file from the watched set.
     *
     * @param file the file to stop watching
     * @return {@code true} if the file was being watched and is now removed
     */
    public synchronized boolean removeFile(File file) {
        if(log.isEnabled()) log.getLogger().info("Removing file");
        return filesMap.remove(file) != null;
    }


    /**
     * Performs a single poll. If any watched file's last-modified timestamp has advanced, the
     * baseline is updated, the access time is stamped, and the consumer is invoked once with the
     * files selected by {@link #reportAllIfOneFileModified}. Intended to be called repeatedly by an
     * external scheduler.
     */
    public void run() {
        if(log.isEnabled()) log.getLogger().info("Executing run");
        File[] filesToReport = checkFiles();
        if (filesToReport != null) {
            if(log.isEnabled()) log.getLogger().info("Files to report: " +  Arrays.toString(filesToReport));

            lastAccess = System.currentTimeMillis();
            if (consumer != null) {
                consumer.accept(filesToReport);
            }

        }
    }

    /**
     * @return the wall-clock time (epoch millis) of the last detected change, or 0 if none yet
     */
    public long lastAccess() {
        return lastAccess;
    }


    /**
     * @return a snapshot of the currently watched files, in insertion order
     */
    public File[] files() {
        return filesMap.keySet().toArray(new File[0]);
    }

}
