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

import java.util.EventListener;

/**
 * This interface must be implemented by any class that uses the TaskProcessor
 * Object 
 * @author mnael
 *
 */
public interface TaskExecutor
		extends EventListener
{

	/**
	 * This method is called by the TaskProcessor ExecutorThread to execute a task
	 * This method must not throw any exception and in case it want to share a response
	 * the TaskEvent.setExecutionResult() can be used 
	 * @param event task event to be executed
	 */
	void executeTask(TaskEvent event);
	
	/**
	 * Called after returning from TaskExecutor.executeTask();
	 * @param event
	 */
	void finishTask(TaskEvent event);

}