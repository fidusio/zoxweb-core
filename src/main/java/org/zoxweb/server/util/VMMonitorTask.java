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
package org.zoxweb.server.util;

import org.zoxweb.server.task.TaskEvent;
import org.zoxweb.server.task.TaskExecutor;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.data.VMInfoDAO;
import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.AppointmentDefault;
import org.zoxweb.shared.util.Const;

import java.util.logging.Logger;

public class VMMonitorTask
        implements TaskExecutor {

    private Logger log;
    private Appointment appointment;
    private TaskSchedulerProcessor tsp;


    private Const.SizeInBytes sizeInBytes = Const.SizeInBytes.B;

    public VMMonitorTask() {

    }

    public VMMonitorTask(Const.SizeInBytes sizeInBytes) {
        if (sizeInBytes != null) {
            this.sizeInBytes = sizeInBytes;
        }
    }

    @Override
    public void executeTask(TaskEvent event) {
        Object[] params = event.getTaskExecutorParameters();

        if (params != null) {
            int i = 0;

            if (params.length > i) {
                log = (Logger) params[i++];
                if (params.length > i) {
                    appointment = (Appointment) params[i++];

                    if (params.length > i) {
                        tsp = (TaskSchedulerProcessor) params[i++];
                    }
                }
            }
        }

        if (log != null) {
            VMInfoDAO vmid = RuntimeUtil.vmSnapshot(sizeInBytes);

            log.info("Values in:" + sizeInBytes +
                    ", Used mem:" + sizeInBytes.convertBytes(vmid.getUsedMemory()) +
                    ", Max-mem:" + sizeInBytes.convertBytes(vmid.getMaxMemory()) +
                    ", Free-mem:" + sizeInBytes.convertBytes(vmid.getFreeMemory()) +
                    ", Total-mem:" + sizeInBytes.convertBytes(vmid.getTotalMemory()));
        }
    }

    @Override
    public void finishTask(TaskEvent event) {
        if (tsp != null && appointment != null) {
            Appointment apt = new AppointmentDefault(appointment.getDelayInMillis());
            tsp.queue(this, apt, this, log, apt, tsp);
        }
    }

}