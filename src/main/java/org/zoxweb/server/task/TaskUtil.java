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

import org.zoxweb.server.util.DefaultEvenManager;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.data.events.EventListenerManager;
import org.zoxweb.shared.util.SharedUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskUtil
{
	private static TaskProcessor TASK_PROCESSOR = null;
	private static TaskSchedulerProcessor TASK_SCHEDULER = null;
	private static TaskSchedulerProcessor TASK_SIMPLE_SCHEDULER = null;
	private static EventListenerManager<BaseEventObject<?>,?> EV_MANAGER = null;

	private static Thread mainThread;
	private static final Lock LOCK = new ReentrantLock();
	
	private static int maxTasks = 1000;
	private static int threadMultiplier = 4;
	private static int minTPThreadCount = 16;
	private static int tpThreadCount = -1;
	public static final long START_TIME_MILLIS = System.currentTimeMillis();

	
	private TaskUtil() {
	}
	
	public static void setMaxTasksQueue(int taskQueueMaxSize) {
		if (TASK_PROCESSOR == null) {
			try {
				LOCK.lock();
				if (TASK_PROCESSOR == null && taskQueueMaxSize > 50) {
					maxTasks = taskQueueMaxSize;
				}
			} finally {
				LOCK.unlock();
			}
		}
	}

	public static void setThreadMultiplier(int multiplier) {
		if (TASK_PROCESSOR == null) {
			try {
				LOCK.lock();
				if (TASK_PROCESSOR == null && multiplier > 2) {
					threadMultiplier = multiplier;
				}
			} finally {
				LOCK.unlock();
			}
		}
	}

	public static void setTaskProcessorThreadCount(int threadCount)
	{
		if (TASK_PROCESSOR == null) {
			try {
				LOCK.lock();
				if (TASK_PROCESSOR == null && threadCount > 2) {
					tpThreadCount = threadCount;
				}
			} finally {
				LOCK.unlock();
			}
		}
	}
	public static void setMinTaskProcessorThreadCount(int minThreadCount){
		if (TASK_PROCESSOR == null) {
			try {
				LOCK.lock();
				if (TASK_PROCESSOR == null && minThreadCount > 2) {
					minTPThreadCount = minThreadCount;
				}
			} finally {
				LOCK.unlock();
			}
		}
	}

	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}


	public static TaskProcessor defaultTaskProcessor() {
		if (TASK_PROCESSOR == null) {
			try {
				LOCK.lock();
				if (TASK_PROCESSOR == null) {
					int threadCount = tpThreadCount;
					if (threadCount < 2 )
					{
						threadCount = Runtime.getRuntime().availableProcessors() * threadMultiplier;
						if (threadCount < minTPThreadCount)
						{
							threadCount = minTPThreadCount;
						}
					}
					TASK_PROCESSOR = new TaskProcessor("DE",maxTasks, threadCount, Thread.NORM_PRIORITY, false);
				}
			} finally {
				LOCK.unlock();
			}
		}
		
		return TASK_PROCESSOR;
	}
	
	public static TaskSchedulerProcessor defaultTaskScheduler() {
		if (TASK_SCHEDULER == null) {
			try {
				LOCK.lock();
				
				if (TASK_SCHEDULER == null) {
					TASK_SCHEDULER = new TaskSchedulerProcessor(defaultTaskProcessor());
				}
			} finally {
				LOCK.unlock();
			}
		}
		
		return TASK_SCHEDULER;
	}

	public static EventListenerManager<BaseEventObject<?>,?> defaultEventManager()
	{
		if (EV_MANAGER == null) {
			try {
				LOCK.lock();

				if (EV_MANAGER == null) {
					EV_MANAGER = new DefaultEvenManager();
				}
			} finally {
				LOCK.unlock();
			}
		}

		return EV_MANAGER;
	}

	/**
	 * Return the default single threaded task scheduler
	 * @return
	 */
	public static TaskSchedulerProcessor simpleTaskScheduler() {
		if (TASK_SIMPLE_SCHEDULER == null) {
			try {
				LOCK.lock();

				if (TASK_SIMPLE_SCHEDULER == null) {
					TASK_SIMPLE_SCHEDULER = new TaskSchedulerProcessor();
				}
			} finally {
				LOCK.unlock();
			}
		}

		return TASK_SIMPLE_SCHEDULER;
	}
	
	public static boolean isBusy()
	{
	    return isBusy(defaultTaskProcessor(), defaultTaskScheduler());
		// getDefaultTaskScheduler().pendingTasks() != 0 || getDefaultTaskProcessor().isBusy();
	}

	public static boolean isBusy(TaskProcessor tp, TaskSchedulerProcessor tsp)
	{
		if (tp == null && tsp == null)
			throw new NullPointerException("TaskProcessor and TaskSchedulerProcessor null");
		return (tp == null ? false : tp.isBusy()) || (tsp == null ? false : tsp.isBusy());
	}

	public static long waitIfBusy()
	{
		return waitIfBusy(100, defaultTaskProcessor(), defaultTaskScheduler());
	}

	public static long waitIfBusy(long durationInMillis)
	{
		return waitIfBusy(durationInMillis, defaultTaskProcessor(), defaultTaskScheduler());
	}

	public static long waitIfBusy(long durationInMillis, TaskProcessor tp, TaskSchedulerProcessor tsp)
	{
		if (tp == null && tsp == null)
			throw new NullPointerException("TaskProcessor and TaskSchedulerProcessor null");
		if(durationInMillis < 1)
			throw new IllegalArgumentException("wait time must be greater than 0 millis second.");
		do
		{
			sleep(durationInMillis);
			// check after wait for reason
			// sometimes the tp and tsp needs some time to start working
			// the con is the caller will always have to wait at least one duration
		}while(isBusy(tp, tsp));

		return System.currentTimeMillis();
	}


	public static long waitIfBusyThenClose(long durationInMillis)
	{
		if(durationInMillis < 1)
			throw new IllegalArgumentException("wait time must be greater than 0 millis second.");
		if (TASK_SIMPLE_SCHEDULER != null)
		{
			do {
				try {
					Thread.sleep(durationInMillis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (TASK_SIMPLE_SCHEDULER.isBusy());//pendingTasks() != 0 );
			TASK_SIMPLE_SCHEDULER.close();
		}
		return waitIfBusyThenClose(durationInMillis, defaultTaskProcessor(), defaultTaskScheduler());
	}





	public static long waitIfBusyThenClose(long millisToSleepAndCheck, TaskProcessor tp, TaskSchedulerProcessor tsp)
	{
//		if(millisToSleepAndCheck < 1)
//			throw new IllegalArgumentException("wait time must be greater than 0 second.");
//		do
//		{
//			try
//			{
//				Thread.sleep(millisToSleepAndCheck);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}while(tsp.pendingTasks() != 0 || tp.isBusy());

		long timestamp = waitIfBusy(millisToSleepAndCheck, tp, tsp);
		tsp.close();
		tp.close();
		return timestamp;
	}


	public static Thread startRunnable(ThreadGroup tg, Runnable run, String name) {
		Thread ret = tg != null ? new Thread(tg, run) : new Thread(run);
		if(name != null)
			ret.setName(name);
		ret.start();
		return ret;
	}

	public static Thread startRunnable(Runnable run, String name) {
		return startRunnable(null, run, name);
	}


	public static int availableThreads(Executor executor)
	{
		if(executor != null && executor instanceof TaskProcessor)
			return ((TaskProcessor)executor).availableExecutorThreads();
		return -1;
	}

	public static int pendingTasks(Executor executor)
	{
		if(executor != null && executor instanceof TaskProcessor)
			return ((TaskProcessor)executor).pendingTasks();
		return -1;
	}




	public static void close()
	{
		defaultTaskScheduler().close();
		defaultTaskProcessor().close();
		simpleTaskScheduler().close();
		defaultEventManager().close();
	}

	public static String info()
	{
		return defaultTaskScheduler().toString();
	}


	/**
	 * This method check if the current thread is the registered main thread
	 * if it hasn't been set it will throw IllegalStateException
	 * @return true if the main Thread.currentThread == registered mainThread
	 * @throws IllegalStateException if the main thread is not registered yet
	 */
	public static boolean isMainThread()
		throws IllegalStateException
	{
		if (mainThread == null)
			throw new IllegalStateException("Main thread haven't been registered");
		return Thread.currentThread() == mainThread;
	}

	/**
	 * This method will try to register the Thread.currentThread() as the main thread
	 * if it is successful it is register and return the registered thread, if unsuccessful
	 * it returns the actual registered main thread meaning once this function can be called
	 * once any subsequent calls are ignored.
	 * @return the successfully registered main thread
	 */
	public static Thread registerMainThread()
	{
		return registerMainThread(Thread.currentThread());
	}

	/**
	 * This method will try to register the Thread.currentThread() as the main thread
	 * if it is successful it is register and return the registered thread, if unsuccessful
	 * it returns the actual registered main thread meaning once this function can be called
	 * once any subsequent calls are ignored.
	 * @param thread to be registered as the main thread
	 * @return he successfully registered main thread
	 */
	public static Thread registerMainThread(Thread thread)
	{
		if (mainThread == null)
		{
			LOCK.lock();
			try
			{
				if (mainThread == null)
					mainThread = thread;
			}
			finally
			{
				LOCK.unlock();
			}
		}

		return mainThread;
	}


	public static long completeTermination(ExecutorService execService, long timeToWait)
	{
		SharedUtil.checkIfNulls("ExecService is null", execService);
		if (timeToWait < 25)
		{
			throw new IllegalArgumentException("Invalid timeToWait < 25 millis " + timeToWait);
		}
		execService.shutdown();
		boolean stat = false;
		while(!stat)
		{
            try
			{
                stat = execService.awaitTermination(timeToWait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

		return System.currentTimeMillis();
	}
	
}