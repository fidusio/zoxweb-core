/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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
package org.zoxweb.server.task;

import org.zoxweb.shared.util.LifeCycleMonitor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceHolder
        extends ExecutorHolder<ExecutorService>
        implements ExecutorService {

    public ExecutorServiceHolder(ExecutorService es, LifeCycleMonitor<ExecutorHolder<?>> lcm) {
        this(es, lcm, null, null);
    }

    public ExecutorServiceHolder(ExecutorService es, LifeCycleMonitor<ExecutorHolder<?>> lcm, String name) {
        this(es, lcm, name, null);
    }

    public ExecutorServiceHolder(ExecutorService es, LifeCycleMonitor<ExecutorHolder<?>> lcm, String name, String description) {
        super(es, lcm, name, description);
    }

    @Override
    public void shutdown() {
        super.close();
        es.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        super.close();
        return es.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return es.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return es.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return es.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return es.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return es.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return es.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return es.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return es.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return es.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return es.invokeAny(tasks, timeout, unit);
    }

    public void close() {
        shutdown();
    }

}