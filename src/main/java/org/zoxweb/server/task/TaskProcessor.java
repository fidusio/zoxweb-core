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

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.ThresholdQueue;
import org.zoxweb.shared.util.*;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * The task executor object must be used when multiple worker thread are required to execute tasks in parallel
 * The number of worker thread should not exceed 2 times the numbers of cores of hardware thread on which the
 * application is running.
 *
 */
public class TaskProcessor
		implements Runnable, DaemonController, Executor, GetNVProperties
{

	public final static LogWrapper log = new LogWrapper(TaskProcessor.class);

	public static final long WAIT_TIME = TimeUnit.MILLISECONDS.toMillis(500);
	private static final AtomicLong instanceCounter = new AtomicLong();
	private final long counterID = instanceCounter.incrementAndGet();
	private final Thread thread;
	private boolean live = true;
	private final ThresholdQueue<TaskEvent>  tasksQueue;



	/**
	 * This is the worker thread queue is used by the TaskProcessor by dequeuing it and waiting for the queue
	 * to be queued after each the ExecutorThread terminate a task
	 * note it is also used to signal communication between the TaskProcessor thread and ExecutorThread thread.
	 * The size of this queue is set by the constructor of TaskProcessor
	 */
	private final boolean executorNotify;
	private final SimpleQueueInterface<ExecutorThread> workersQueue;
	
	private int executorsCounter = 0;
	private boolean innerLive = true;
	private final ThreadGroup threadGroup;
	private static final AtomicLong TP_COUNTER = new AtomicLong(0);



	/**
	 * This is the worker thread that will execute the TaskExecutor.executeTask
	 */
	protected class ExecutorThread
			implements Runnable {
		
		protected TaskEvent event = null;
		protected final int counter = ++executorsCounter; 
		protected long totalExecutionTime = 0;
		protected long callCounter = 0;
		
		
		protected ExecutorThread(ThreadGroup tg, String parentID, int priority) {
			Thread temp = new Thread(tg, this, parentID +"-ET-" + counter);
			
			temp.setPriority(priority);
			temp.start();
		}

		@Override
		public void run()
		{
			// as long as the TaskProcessor is a live 
			// the ExecutorThread thread will live
			while(innerLive)
			{
				if (event != null)
				{
					// do the work
					TaskExecutor te = event.getTaskExecutor();
					long delta = System.currentTimeMillis();
					
					
					//execute the task;
					if (te != null)
					{
						try {
							te.executeTask(event);
						} catch (Throwable e) {
							e.printStackTrace();
						}

						if (executorNotify) {
							synchronized (te) {
								te.notify();
							}
						}


						// call the task finish task method
						try {
							te.finishTask(event);
						} catch (Throwable e) {
							e.printStackTrace();
						} finally {
							event.incExecCount();
						}
					}


					if (executorNotify)
					{
						synchronized (te)
						{
							te.notify();
						}
					}
					
					delta = System.currentTimeMillis() -  delta;
					totalExecutionTime += delta;
					callCounter++;
					// work is done reset the work event
					queueInternalTask(null);
				}
				
				synchronized(this)
				{
					// if the event is null
					if (event == null)
					{
						try
						{
							// wait to be awaken by the TaskProcessor 
							wait(WAIT_TIME);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		/**
		 * This method will queue one task
		 * @param event if null it will reset the task can only be set to null by the ExecutorThread
		 */
		protected  void queueInternalTask(TaskEvent event)
		{
			synchronized(this)
			{
				this.event = event;
				
				if (event == null) {
					// if the event is null we need 
					// to queue the worker thread to
					// the worker queue 
					workersQueue.queue(this);
					
					// we need to notify the TaskProcessor that
					// we are ready for work
					synchronized(workersQueue)
					{
						// notify the TaskProcessor thread
						workersQueue.notify();
					}
				}
				// notify the ExecutorThread thread to wake up
				// and start working
				notify();
			}
		}
	}
	
	/**
	 * Create a task processor with default count of worker thread if the <code>core count > 1 core count*1.5 if core == 1 then it is 2</code>
	 * @param taskQueueMaxSize task queue maximum size
	 */
	public TaskProcessor(int taskQueueMaxSize)
			throws IllegalArgumentException {
		this(taskQueueMaxSize, Runtime.getRuntime().availableProcessors()*2, Thread.NORM_PRIORITY, true);
	}



	public TaskProcessor(int taskQueueMaxSize,
						 int executorThreadCount,
						 int threadPriority,
						 boolean executorNotify)
		throws IllegalArgumentException
	{
		this(null, taskQueueMaxSize, executorThreadCount, threadPriority, executorNotify);
	}

	/**
	 *  
	 * Create a task processor
	 * @param defaultPrefix thread prefix name tag
	 * @param taskQueueMaxSize task queue max size
	 * @param executorThreadCount number of worker threads
	 * @param threadPriority the thread priority
	 * @param executorNotify notify the task executor
	 * @throws IllegalArgumentException <code>if taskQueueMaxSize < 2 or executorThreadCount < 2, or executorThreadCount > taskQueueMaxSize</code>
	 */
	public TaskProcessor(String defaultPrefix,
						 int taskQueueMaxSize,
						 int executorThreadCount,
						 int threadPriority,
						 boolean executorNotify)
		throws IllegalArgumentException 
	{
		//super("TaskProcessor", "with", false);
		if (taskQueueMaxSize < 2 || executorThreadCount < 2 || executorThreadCount > taskQueueMaxSize)
		{
			throw new IllegalArgumentException("Invalid number of [taskQueueMaxSize,executorThreadCount] " + 
					 "[" + taskQueueMaxSize +"," +executorThreadCount+"]");
		}
		
		tasksQueue = new ThresholdQueue<TaskEvent>((taskQueueMaxSize*75)/100, taskQueueMaxSize);
		if (SharedStringUtil.isEmpty(defaultPrefix))
		{
			defaultPrefix = "TP";
		}
		String tpID = defaultPrefix + "-" + TP_COUNTER.incrementAndGet();
		workersQueue = new ArrayQueue<ExecutorThread>(executorThreadCount);
		threadGroup = new ThreadGroup(tpID);
		for (int i = 0; i < executorThreadCount; i++)
		{
			// create and queue the executor threads
			workersQueue.queue(new ExecutorThread(threadGroup, tpID, threadPriority));
		}
		// start the task processor
		this.executorNotify = executorNotify;
		thread = new Thread(threadGroup, this, tpID +"-TP");


		thread.start();
		log.getLogger().info("Started:" + this);
	}
	
	
	/**
	 * This method will add task to be executed, it will block if the number of awaiting task in the internal queues
	 * equals 2x the number of ExecutorThreads
	 * @param task to be queued, null tasks are ignored
	 * @throws IllegalArgumentException if the TaskProcessor is terminated
	 */
	public void queueTask(TaskEvent task)
		throws IllegalArgumentException
	{	
		if( !live)
		{
			throw new IllegalArgumentException("Can't queue task with a terminated TaskProcessor");
		}
		
		// if the task is not null
		if (task != null)
		{
			// queue the task if it hasn't reached  tasksQueue.getHighMark()
			// if we have reached the tasksQueue.getHighMark() we will block till we reach
			// tasksQueue.getLowMark()
			tasksQueue.queue(task);
			
			synchronized(this)
			{
				// notify the TaskProcessor
				notifyAll();
			}
		}
	}

	
	
	
	
	@Override
	public void run() 
	{
		log.getLogger().info(toString());
		while(live)
		{
			TaskEvent event;
			// check if we have task's to execute
			while((event = tasksQueue.dequeue()) != null)
			{
				
				ExecutorThread et;
				// if we have no executor thread
				// we will wait till we have one
				while ((et = workersQueue.dequeue()) == null)
				{
					// use the workersQueue as inter-thread signal between the ExecutorThreads and TaskProcessor
					synchronized(workersQueue)
					{
						try 
						{
							//dbg("Excecutor queue "+ workersQueue.size());
							workersQueue.wait(WAIT_TIME);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
					}
				}
				
				// we took the first available ExecutorThread
				// and start processing the task
				et.queueInternalTask(event);
				
			}
			
			synchronized(this)
			{
				// if the tasksQueue is empty 
				// and the TaskProcessor is not terminated
				// wait for incoming task to be awaken by the queueTask method
				if (tasksQueue.isEmpty() && live)
				{
					try 
					{
						//dbg("will wait in the outer queue");
						wait(WAIT_TIME);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		// the executor task is terminated
		// notify the executor thread to terminate
		innerLive = false;
		
		ExecutorThread et;
		while ((et = workersQueue.dequeue()) != null)
		{
			synchronized(et)
			{
				et.notify();
			}
		}
		
		
		log.getLogger().info("TaskProcessor[" +executorsCounter + "] terminated");
	}
	
	/**
	 * Return the current number of tasks pending to be executed
	 * @return count of pending tasks
	 */
	public int pendingTasks()
	{
		return tasksQueue.size();
	}

	public long totalQueued()
	{
		return tasksQueue.totalQueued();
	}
	public String toString()
	{
		//return "TaskProcessor[" +executorsCounter+","+ tasksQueue.getHighMark() +"," +tasksQueue.getLowMark()+"]";
		return "TaskProcessor[" + counterID + "," + live + "," + executorsCounter + "," +  availableExecutorThreads() + "," + tasksQueue + "]";
	}
	/**
	 * @return Return true if there is pending tasks or any worker thread is executing a task
	 */
	public boolean isBusy()
	{
		return (tasksQueue.size() > 0 ||  workersQueue.size() < executorsCounter);
	}
	/**
	 * Return the current number of available threads to do the work
	 * @return available threads
	 */
	public int availableExecutorThreads()
	{
		return workersQueue.size();
	}


	public int workersThreadCapacity()
	{
		return workersQueue.capacity();
	}

	
	public void close()
	{
		// TODO Auto-generated method stub
		if (live)
		{
			live = false;
			//log.getLogger().info("TaskProcessor will be terminated.");
			synchronized(this)
			{
				notifyAll();
			}
		}
	}
	
	public boolean isClosed()
	{
		return !live;
	}
	
	public int getQueueMaxSize()
	{
		return tasksQueue.capacity();
	}

	public ThreadGroup getThreadGroup()
	{
		return threadGroup;
	}



	@Override
	public void execute(Runnable command) 
	{
		if (command != null)
			queueTask(new TaskEvent(this, new RunnableTaskContainer(command)));
	}

//	public <T> void execute(Supplier<T> supplier, Consumer<T> consumer)
//	{
//		if(consumer != null)
//			queueTask(new TaskEvent(this, new SupplierConsumerTask<>(), supplier, consumer));
//	}

	@Override
	public NVGenericMap getProperties() {
		NVGenericMap ret = new NVGenericMap();
		ret.setName("task_processor");
		ret.add(new NVLong("instance_id", counterID));
		ret.add(new NVInt("workers_capacity", workersQueue.capacity()));
		ret.add(new NVInt("workers_available", workersQueue.size()));
		ret.add(new NVInt("tasks_capacity", tasksQueue.capacity()));
		ret.add(new NVInt("tasks_capacity_threshold", tasksQueue.getThreshold()));
		ret.add(new NVInt("tasks_pending", tasksQueue.size()));


		return ret;
	}
	
	/**
	 * The tasks queue is used to add task to the task processor
	 */
	
	/**
	 * This is the worker thread queue is used by the TaskProcessor by dequeuing it and waiting for the queue
	 * to be queued after each the ExecutorThread terminate a task
	 * note it is also used to signal communication between the TaskProcessor thread and ExecutorThread thread.
	 * The size of this queue is set by the constructor of TaskProcessor
	 */
	
}
