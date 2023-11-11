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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;


import org.zoxweb.server.task.*;
import org.zoxweb.shared.task.SupplierConsumerTask;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.AppointmentDefault;

public class TaskSchedulerProcessorTest {


	private static final Logger log = Logger.getLogger(TaskSchedulerProcessorTest.class.getName());
	private static final Lock lock = new ReentrantLock();
	private static Object test = null;

	static class TaskLockTest
			extends TaskDefault {

		/**
		 * @see org.zoxweb.server.task.TaskDefault#childExecuteTask(org.zoxweb.server.task.TaskEvent)
		 */
		@Override
		protected void childExecuteTask(TaskEvent event) {
			Integer index = (Integer) event.getTaskExecutorParameters()[0];

			if (test == null) {
			    try {
					System.out.println("["+ index +"]:" + "Will try to lock:" + Thread.currentThread());
					lock.lock();
					System.out.println("["+ index +"]:" + "Lock Acquired:" + Thread.currentThread());

					if (test == null) {
						Object temp = new Object();
						synchronized(temp) {
							try {
								long delta = System.nanoTime();
								temp.wait(Const.TimeInMillis.SECOND.MILLIS*20);
								delta = System.nanoTime() - delta;
								System.out.println("["+ index +"]:" + "Waited " + Const.TimeInMillis.nanosToString(delta) + ":" + Thread.currentThread());
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						test = temp;
					}
				} finally {
					lock.unlock();
					System.out.println("["+ index +"]:" + "Unlocked:" + Thread.currentThread());
				}
			} else {
				System.out.println("["+ index +"]:" + "Never locked:" + Thread.currentThread());
			}
			
		}
	}

	static class TaskExecutorImpl
		implements TaskExecutor {

		private long index = 0;
		private static AtomicLong counter = new AtomicLong();

		TaskExecutorImpl() {
			index = counter.incrementAndGet();
		}

		/**
		 * @see org.zoxweb.server.task.TaskExecutor#executeTask(org.zoxweb.server.task.TaskEvent)
		 */
		public void executeTask(TaskEvent event) {
			long ts = System.currentTimeMillis();
			Appointment tScheduler = (Appointment) event.getTaskExecutorParameters()[0];
			System.out.println(index+":Called at:" + ts + " expiration:" + tScheduler.getExpirationInMillis() + " delta:" + (ts-tScheduler.getExpirationInMillis()) + " delay:" +tScheduler.getDelayInMillis() + " " + Thread.currentThread().getName());
			
		}

		/**
		 * @see org.zoxweb.server.task.TaskExecutor#finishTask(org.zoxweb.server.task.TaskEvent)
		 */
		public void finishTask(TaskEvent event) {

		}
	}
	
	public static void main(String[] args)
    {
		TaskProcessor tp = new  TaskProcessor(50, Runtime.getRuntime().availableProcessors()*5, Thread.NORM_PRIORITY, true);
		TaskSchedulerProcessor tsp = new TaskSchedulerProcessor(tp);
		
		Appointment tsa = null;
		
		for(int i = 0; i < 10; i++) {
			Appointment ts = new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS);
			TaskExecutorImpl tei = new TaskExecutorImpl();
			
			if (i == 0) {
				tsa = tsp.queue(tsp, ts, tei, ts);
			} else {
                tsp.queue(tsp, ts, tei, ts);
            }

			if (i > 5) {
				for (int j = 0; j < i; j++) {
					ts = new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS);
					tei = new TaskExecutorImpl();
					tsp.queue(tsp, ts, tei, ts);
				}
			}
		}

		Appointment tsold = new AppointmentDefault(-Const.TimeInMillis.SECOND.MILLIS);
		TaskExecutorImpl teiold = new TaskExecutorImpl();
		tsp.queue(tsp, tsold, teiold, tsold);
		
		for (int i = 0; i < 50; i++) {
			Appointment ts = new AppointmentDefault(-Const.TimeInMillis.SECOND.MILLIS, System.nanoTime());
			TaskExecutorImpl tei = new TaskExecutorImpl();
			tsp.queue(tsp, ts, tei, ts);
		}

		while(tsp.pendingTasks() != 0) {
			try {
				Thread.sleep( Const.TimeInMillis.SECOND.MILLIS );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("TaskSchedulerProcessor:" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
		//tsp.terminate();
		System.out.println("TaskSchedulerProcessor AAAAA   AAAAAA :" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
		
		//tsp = new TaskSchedulerProcessor( Appointment.EQUAL_MORE_COMPARATOR);
		Appointment last = null;
		TaskExecutorImpl tei = null;
		Appointment tsaToCancel = null;

		for (int i = 0; i < 10; i++) {
			Appointment ts = new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS);
			tei = new TaskExecutorImpl();
			tsp.queue(tsp, ts, tei, ts);
			//System.out.println(i + " wait for " + (Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS) );
			if ( i > 5) {
				for (int j = 0; j < i; j++) {
					ts = new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS);
					tei = new TaskExecutorImpl( );
					
					last = tsp.queue(tsp, ts, tei, ts);
					if (tei.index == 99) {
						tsaToCancel = last;
					}
					//System.out.println(j + " wait for " + (Const.TimeInMillis.SECOND.MILLIS*1 + i*Const.TimeInMillis.SECOND.MILLIS) );
				}
			}
		}

		System.out.println("TaskSchedulerProcessor:" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
		if (tsaToCancel != null) {
		    tsaToCancel.cancel();
        }

		while( tsp.pendingTasks() != 0) {
			try {
				Thread.sleep(Const.TimeInMillis.SECOND.MILLIS);
				if (last != null)
				{
					System.out.println( last.cancel() + " " + tei.index);
					last = null;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (tsa != null) {
			tsa.setDelayInMillis(2000);
		}
		
		int index = 0;

		for (index = 0; index < 20; index++) {
			tsp.queue(new AppointmentDefault(), new TaskEvent(tsp, new TaskLockTest(), index));
		}
		tsp.queue(new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*21), new TaskEvent(tsp, new TaskLockTest(), index++));
		
		
		for (;index < 1000; index++)
		{
		  tsp.queue(new AppointmentDefault(Const.TimeInMillis.SECOND.MILLIS*21, System.nanoTime()), new TaskEvent(tsp, new TaskLockTest(), index++));
		}
		

//		while( tsp.pendingTasks() != 0) {
//			try {
//				Thread.sleep( Const.TimeInMillis.SECOND.MILLIS);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}

//		TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, new Runnable() {
//			long ts = System.currentTimeMillis();
//			@Override
//			public void run() {
//				ts = System.currentTimeMillis() - ts;
//				System.out.println("************************* It took: "  + Const.TimeInMillis.toString(ts));
//			}
//		});


		TaskUtil.simpleTaskScheduler().queue(0, new SupplierConsumerTask(new Supplier<String>()
				{
					String str;
					Supplier<String> init(String str)
					{
						this.str = str;
						return this;
					}
					public String get(){
					return str;
				}}.init("toto"),
				new Consumer<String>() {
				@Override
				public void accept(String s) {
					log.info(Thread.currentThread() + " " + s);
				}
		})
		);



		TaskUtil.waitIfBusyThenClose(23, tp, tsp);
		TaskUtil.waitIfBusyThenClose(25);

		System.out.println("TaskSchedulerProcessor 1 :" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
		//tp.close();
		System.out.println("TaskSchedulerProcessor 2 :" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
		//tsp.close();
		System.out.println("TaskSchedulerProcessor 3 :" + tsp.pendingTasks() + " TaskProcessor:" + tp.pendingTasks());
	}
}