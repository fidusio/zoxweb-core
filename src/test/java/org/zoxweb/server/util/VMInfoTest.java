/*
 * Copyright (c) 2012-2014 ZoxWeb.com LLC.
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

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.AppointmentDefault;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.util.logging.Logger;

public class VMInfoTest {

	private static Logger log = Logger.getLogger("");

	public static void main(String ...args) {
		Runtime run = Runtime.getRuntime();
		System.out.println("Total memory:" + run.totalMemory()/Const.SizeInBytes.M.SIZE);
		System.out.println("Free  memory:" + run.freeMemory()/Const.SizeInBytes.M.SIZE);
		System.out.println("Max   memory:" + run.maxMemory()/Const.SizeInBytes.M.SIZE);
		long memory = run.totalMemory() - run.freeMemory();
		System.out.println("Used memory:" + memory);

		try {
			System.out.println(GSONUtil.toJSON(RuntimeUtil.vmSnapshot(), true, false, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TaskSchedulerProcessor tsp = new TaskSchedulerProcessor();
		VMMonitorTask vmmt = new VMMonitorTask(Const.SizeInBytes.K);
		Appointment apt = new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*5);
		tsp.queue( log, apt, vmmt, log, apt, tsp);
		System.out.println("Stack trace:" + RuntimeUtil.stackTrace());
		for (int i = 0; i < 10; i++) {
			tsp.queue( log, new AppointmentDefault(Const.TimeInMillis.MILLI.MILLIS*(5+i)), new VMMonitorTask(Const.SizeInBytes.M), log);
		}
		
	}

}