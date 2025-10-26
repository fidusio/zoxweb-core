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

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a default implementation of the TaskExecutor and TaskTerminationListener interface, it is
 * recommended to extend this class and customize the implementation to reflect the requirement of the new implementation
 *
 * @author mnael
 *
 */
public abstract class TaskDefault
        implements TaskExecutor {

    protected AtomicLong executeCount = new AtomicLong();
    protected AtomicLong finishCount = new AtomicLong();

    protected abstract void childExecuteTask(TaskEvent event);

    @Override
    public void finishTask(TaskEvent event) {
        finishCount.incrementAndGet();
    }

    @Override
    public void executeTask(TaskEvent event) {
        try {
            childExecuteTask(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        executeCount.incrementAndGet();
    }

    public long getExecutionCallbackCount() {
        return executeCount.get();
    }

    public long getTerminationCallbackCount() {
        return finishCount.get();
    }

}