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
package org.zoxweb.server.net.task;

import org.zoxweb.server.task.ExecutorHolder;
import org.zoxweb.server.task.ExecutorHolderManager;
import org.zoxweb.server.task.TaskProcessor;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class ExecutorHolderTest {

	private static final transient Logger log = Logger.getLogger(ExecutorHolderTest.class.getName());

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		try {
			ExecutorHolderManager.SINGLETON.createCachedThreadPool("marwan");
			log.info("" + ExecutorHolderManager.SINGLETON.size());
			ExecutorHolderManager.SINGLETON.createFixedThreadPool("nael", 5);
			log.info("" + ExecutorHolderManager.SINGLETON.size());
			
			ExecutorHolderManager.SINGLETON.createScheduledThreadPool("imad", 5);
			log.info("" + ExecutorHolderManager.SINGLETON.size());
			
			
			ExecutorHolder<Executor> ret = (ExecutorHolder<Executor>)ExecutorHolderManager.SINGLETON.register(new TaskProcessor(1000), "eerabi");
			
			log.info("" + ExecutorHolderManager.SINGLETON.size());
			ExecutorHolderManager.SINGLETON.terminate(ret.getName());
			log.info("" + ExecutorHolderManager.SINGLETON.size());

			ExecutorHolderManager.SINGLETON.close();
		} catch(Exception e) {
			e.printStackTrace();
			ExecutorHolderManager.SINGLETON.close();
		}
	}
}
