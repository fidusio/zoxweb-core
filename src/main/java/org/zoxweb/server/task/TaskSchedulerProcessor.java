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

import org.zoxweb.shared.util.*;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public class TaskSchedulerProcessor
    implements Runnable, DaemonController, GetNVProperties {

	public final class TaskSchedulerAppointment
			implements Appointment
	{

		private final Appointment appointment;
		private final TaskEvent taskEvent;

		private TaskSchedulerAppointment(Appointment appointment, TaskEvent te)
		{
			this.appointment = appointment;
			this.taskEvent = te;
		}

		@Override
		public long getDelayInMillis() {
			return appointment.getDelayInMillis();
		}

		@Override
		public void setDelayInMillis(long delayInMillis) {
			setDelayInNanos(delayInMillis, System.nanoTime());
		}

		@Override
		public long getExpirationInMillis() {
			return appointment.getExpirationInMillis();
		}

		@Override
		public boolean cancel() {
			return remove(this);
		}

		public boolean equals(Object o) {
			return appointment.equals(o);
		}

		@Override
		public void setDelayInNanos(long delayInMillis, long nanoOffset) {
			// TODO Auto-generated method stub
			cancel();
			appointment.setDelayInNanos(delayInMillis, nanoOffset);
			queue(this);
		}

		@Override
		public long getPreciseExpiration() {
			// TODO Auto-generated method stub
			return appointment.getPreciseExpiration();
		}

		public int hashCode() {
			return appointment.hashCode();
		}

		public synchronized void close()
		{
			if (!appointment.isClosed()) {
				cancel();
				appointment.close();
			}
		}

		public synchronized boolean reset(boolean runOnce)
		{
			if (runOnce && taskEvent.execCount() > 0 || isClosed())
				return false;
			// this is the right logic
			setDelayInNanos(appointment.getDelayInMillis(), System.nanoTime());
			return true;

		}


		/**
		 * @return
		 */
		@Override
		public long execCount() {
			return taskEvent.execCount();
		}

		public boolean isClosed() {
			return appointment.isClosed();
		}

	}
	
	private TaskProcessor taskProcessor = null;
	private boolean live = true;
	private static final long DEFAULT_TIMEOUT = Const.TimeInMillis.MILLI.MILLIS*500;
	private static final AtomicLong TSP_COUNTER = new AtomicLong(0);
	private long counterID = TSP_COUNTER.incrementAndGet();
	private volatile ConcurrentSkipListSet<TaskSchedulerAppointment> queue = null;

	private volatile long expiryTimestamp;
	
	public TaskSchedulerProcessor() {
		this(Appointment.EQUAL_COMPARATOR, null);
	}

	public TaskSchedulerProcessor(TaskProcessor tp) {
		this(Appointment.EQUAL_COMPARATOR, tp);
	}

	private TaskSchedulerProcessor(Comparator<Appointment> tsc, TaskProcessor tp) {
		SharedUtil.checkIfNulls("TaskSchedulerComparator can't be null", tsc);
		queue =  new ConcurrentSkipListSet<TaskSchedulerAppointment>(tsc);
		taskProcessor = tp;
		TaskUtil.startRunnable(tp != null?  tp.getThreadGroup() : null, this, "TSP-" + counterID);
	}

	public void close() {
		if (live) {
			synchronized(this) {
				// check to avoid double penetration
				if (live) {
					live = false;
					notify();
				}
			}

			synchronized(queue) {
				queue.notify();
			}
		}
	}

	@Override
	public boolean isClosed() {
		return !live;
	}




	
	public Appointment queue(Object source, Appointment a, TaskExecutor te, Object... params)
	{
		return queue(new TaskSchedulerAppointment(a == null ? new AppointmentDefault() : a, new TaskEvent(source, te, params)));
	}
	
	public Appointment queue(Object source, long timeInMillis, TaskExecutor te, Object... params)
	{
      return queue(new TaskSchedulerAppointment( new AppointmentDefault(timeInMillis), new TaskEvent(source, te, params)));
  	}

	public Appointment queue(Appointment a, TaskEvent te)
	{
		return queue(new TaskSchedulerAppointment(a == null ? new AppointmentDefault(0) : a, te));
	}

	/**
	 * Schedule a task based on the nextDelay() of the rate controller.
	 * @param rateController to be applied with nextDelay()
	 * @param task to be executed
	 * @return Appointment object associated with the task
	 */
	public Appointment queue(RateController rateController, Runnable task)
	{
		if(rateController != null && task != null)
			return queue(rateController.nextWait(), task);
		return null;
	}

	/**
	 * Schedule a task based on the current delay in millis from now
	 * @param delayInMillis if delay is < 0 the task will execute now
	 * @param task to be executed
	 * @return Appointment object associated with the task
	 */
	public Appointment queue(long delayInMillis, Runnable task)
    {
        if (task != null)
			return queue(new TaskSchedulerAppointment(new AppointmentDefault(delayInMillis, System.nanoTime()),
					new TaskEvent(this, new RunnableTaskContainer(task))));

        return null;
    }

	public Appointment queue(Appointment appointment, Runnable command)
	{
		if (command != null)
			return queue(new TaskSchedulerAppointment(appointment == null ? new AppointmentDefault() : appointment,
					new TaskEvent(this, new RunnableTaskContainer(command))));

		return null;
	}



	private TaskSchedulerAppointment queue(TaskSchedulerAppointment te) {
		if (!live) {
			throw new IllegalArgumentException("TaskSchedulerProcessor is dead");
		}
		
		synchronized(queue) {
			while(!queue.add(te)) {
				te.appointment.setDelayInNanos(te.appointment.getDelayInMillis(), System.nanoTime());
			}

			queue.notify();
		}
		
		return te;
	}


	public TaskProcessor getExecutor()
	{
		return taskProcessor;
	}
	
	private TaskSchedulerAppointment dequeue() {
		synchronized(queue) {
			return queue.pollFirst();
		}
	}
	
	public boolean remove(Appointment tsa) {
		synchronized (queue) {
			return queue.remove(tsa);
		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(live) {
			long timeToWait = 0;
			
			do {
				TaskSchedulerAppointment tSchedulerEvent = null;

				synchronized(queue) {
					long delay = internalWaitTime();

					if (delay <= 0) {
						tSchedulerEvent = dequeue();
					} else {
						timeToWait = delay;
					}	
				}
				
				if (tSchedulerEvent != null)
				{
					if (taskProcessor != null)
					{
						taskProcessor.queueTask(tSchedulerEvent.taskEvent);
					}
					else
					{
						// we need to execute task locally
						try
						{
							tSchedulerEvent.taskEvent.getTaskExecutor().executeTask(tSchedulerEvent.taskEvent);
						}
						catch( Throwable e)
						{
							e.printStackTrace();
						}
						
						try
						{
							tSchedulerEvent.taskEvent.getTaskExecutor().finishTask(tSchedulerEvent.taskEvent);
						}
						catch( Throwable e)
						{
							e.printStackTrace();
						}
					}
				}

			} while(timeToWait == 0);

			// wait time for the wakeup
			synchronized(queue) {
				timeToWait = internalWaitTime();
				if (live && timeToWait > 0) {
					
					try {
						queue.wait(timeToWait);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public int pendingTasks()
	{
		return queue.size();
	}


	/**
	 * Made private to avoid external calling
	 * @return time to wait in miilis
	 */
	private long internalWaitTime()
	{
		long delay  = DEFAULT_TIMEOUT;
		try
		{
			TaskSchedulerAppointment tSchedulerEvent = queue.first();
			expiryTimestamp = tSchedulerEvent.getExpirationInMillis();
			delay = expiryTimestamp - System.currentTimeMillis();
		} catch(NoSuchElementException e) {
			expiryTimestamp = System.currentTimeMillis() + delay;
		}
		return delay;
	}

	public long waitTime(){
		return expiryTimestamp - System.currentTimeMillis();
	}


	@Override
	public NVGenericMap getProperties()
	{
		NVGenericMap ret = new NVGenericMap();
		ret.setName("task_scheduler");
		ret.add(new NVLong("instance_id", counterID));
		ret.add(new NVInt("pending_tasks", queue.size()));

		ret.add("current_wait", Const.TimeInMillis.toString(waitTime()));


		return ret;
	}

	public String toString()
	{
		return (getExecutor() != null ? getExecutor().toString() + ", TaskSchedulerProcessor[ " : "TaskSchedulerProcessor[") +
				SharedUtil.toCanonicalID(',', counterID, live, queue.size(), Const.TimeInMillis.toString(waitTime()))+"]";
	}


	public boolean isBusy()
	{
		return pendingTasks() != 0;
	}

//	public static long waitIfBusy(long millisToSleepAndCheck)
//	{
//		if(millisToSleepAndCheck < 1)
//			throw new IllegalArgumentException("wait time must be greater than 0 millis second.");
//		while(isBusy())
//		{
//			TaskUtil.sleep(millisToSleepAndCheck);
//		}
//
//		return System.currentTimeMillis();
//	}
}