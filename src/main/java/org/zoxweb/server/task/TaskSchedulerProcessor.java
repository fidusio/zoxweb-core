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

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TaskSchedulerProcessor
    implements Runnable, DaemonController, GetNVProperties, ScheduledExecutorService  {

	public final class TaskSchedulerAppointment<V>
			extends FutureCallableRunnableTask<V>
			implements Appointment, ScheduledFuture<V>
	{

		private final Appointment appointment;
		private final long autoRepeatDelay;
		private final boolean fixedRate;
		//private final FutureCallableRunnableTask<?> fcrt;

//		private TaskSchedulerAppointment(Appointment appointment, FutureCallableRunnableTask<?> fcrt)
//		{
//			this.appointment = appointment;
//			this.fcrt = fcrt;
//		}

		private TaskSchedulerAppointment(Appointment appointment, TaskEvent te)
		{
			super(te);
			this.appointment = appointment;
			autoRepeatDelay = -1;
			fixedRate = false;
		}

		private TaskSchedulerAppointment(Appointment appointment, Callable<V> callable)
		{
			super(callable, TaskSchedulerProcessor.this);
			this.appointment = appointment;
			autoRepeatDelay = -1;
			fixedRate = false;
		}
		private TaskSchedulerAppointment(Appointment appointment, Runnable runnable, V result, boolean isFuture)
		{
			super(runnable, result, isFuture, TaskSchedulerProcessor.this);
			this.appointment = appointment;
			autoRepeatDelay = -1;
			fixedRate = false;
		}

		private TaskSchedulerAppointment(Appointment appointment, Runnable runnable, long repeatDelay, boolean fixedRate)
		{
			super(runnable, null, false, TaskSchedulerProcessor.this);
			this.appointment = appointment;
			autoRepeatDelay = repeatDelay;
			this.fixedRate = fixedRate;
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
		@Override
		public void finishTask(TaskEvent e)
		{
			super.finishTask(e);
			if(!fixedRate && autoRepeatDelay > 0)
				setDelayInNanos(autoRepeatDelay, 0);
		}

		@Override
		public void executeTask(TaskEvent e) throws Exception {
			if(fixedRate && autoRepeatDelay > 0 )
				setDelayInNanos(autoRepeatDelay, 0);
			super.executeTask(e);
		}

		@Override
		public synchronized boolean reset(boolean runOnce)
		{
			if (runOnce && taskEvent.execCount() > 0 || isClosed())
				return false;
			// this is the right logic
			setDelayInNanos(appointment.getDelayInMillis(), 0);
			return true;

		}

		@Override
		public boolean isClosed() {
			return appointment.isClosed();
		}



		@Override
		public long getDelay(TimeUnit tu)
		{
			return appointment.getDelay(tu);
		}

		@Override
		public int compareTo(Delayed o) {
			// may cause error
			return appointment.compareTo(o);
		}

		/**
		 * Attempts to cancel execution of this task.  This attempt will
		 * fail if the task has already completed, has already been cancelled,
		 * or could not be cancelled for some other reason. If successful,
		 * and this task has not started when {@code cancel} is called,
		 * this task should never run.  If the task has already started,
		 * then the {@code mayInterruptIfRunning} parameter determines
		 * whether the thread executing this task should be interrupted in
		 * an attempt to stop the task.
		 *
		 * <p>After this method returns, subsequent calls to {@link #isDone} will
		 * always return {@code true}.  Subsequent calls to {@link #isCancelled}
		 * will always return {@code true} if this method returned {@code true}.
		 *
		 * @param mayInterruptIfRunning {@code true} if the thread executing this
		 *                              task should be interrupted; otherwise, in-progress tasks are allowed
		 *                              to complete
		 * @return {@code false} if the task could not be cancelled,
		 * typically because it has already completed normally;
		 * {@code true} otherwise
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return cancel();
		}

		/**
		 * Returns {@code true} if this task was cancelled before it completed
		 * normally.
		 *
		 * @return {@code true} if this task was cancelled before it completed
		 */
		@Override
		public boolean isCancelled() {
			return isClosed();
		}


		@Override
		public long execCount()
		{
			return taskEvent.execCount();
		}

//		/**
//		 * Waits if necessary for the computation to complete, and then
//		 * retrieves its result.
//		 *
//		 * @return the computed result
//		 * @throws CancellationException if the computation was cancelled
//		 * @throws ExecutionException    if the computation threw an
//		 *                               exception
//		 * @throws InterruptedException  if the current thread was interrupted
//		 *                               while waiting
//		 */
//		@Override
//		public V get() throws InterruptedException, ExecutionException {
//			return (V)fcrt.get();
//		}

//		/**
//		 * Waits if necessary for at most the given time for the computation
//		 * to complete, and then retrieves its result, if available.
//		 *
//		 * @param timeout the maximum time to wait
//		 * @param unit    the time unit of the timeout argument
//		 * @return the computed result
//		 * @throws CancellationException if the computation was cancelled
//		 * @throws ExecutionException    if the computation threw an
//		 *                               exception
//		 * @throws InterruptedException  if the current thread was interrupted
//		 *                               while waiting
//		 * @throws TimeoutException      if the wait timed out
//		 */
//		@Override
//		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//			return (V)fcrt.get(timeout, unit);
//		}
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
		return queue(a == null ? new AppointmentDefault() : a, new TaskEvent(source, true, te, params));
	}
	
	public Appointment queue(Object source, long timeInMillis, TaskExecutor te, Object... params)
	{
      return queue(new AppointmentDefault(timeInMillis), new TaskEvent(source, true, te, params));
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
			return queue(new TaskSchedulerAppointment(new AppointmentDefault(delayInMillis, System.nanoTime()), task, null, true));
//					new FutureCallableRunnableTask<>(task, null, true, this)));
//					new TaskEvent(this, new RunnableTask(task))));

        return null;
    }

	public Appointment queue(Appointment appointment, Runnable command)
	{
		if (command != null)
			return queue(new TaskSchedulerAppointment(appointment == null ? new AppointmentDefault() : appointment, command, null, true));
//					new TaskEvent(this, new RunnableTask(command))));

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
			TaskSchedulerAppointment<?> tSchedulerEvent = queue.first();
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


	/*
	 The java interface implementation Of ScheduledExecutorService
	 */

	/**
	 * Submits a one-shot task that becomes enabled after the given delay.
	 *
	 * @param command the task to execute
	 * @param delay   the time from now to delay execution
	 * @param unit    the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of
	 * the task and whose {@code get()} method will return
	 * {@code null} upon completion
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if command or unit is null
	 */
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {

		return (ScheduledFuture<?>) queue(TimeUnit.MILLISECONDS.convert(delay, unit), command);
	}

	/**
	 * Submits a value-returning one-shot task that becomes enabled
	 * after the given delay.
	 *
	 * @param callable the function to execute
	 * @param delay    the time from now to delay execution
	 * @param unit     the time unit of the delay parameter
	 * @return a ScheduledFuture that can be used to extract result or cancel
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if callable or unit is null
	 */
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return queue(new TaskSchedulerAppointment<>(new AppointmentDefault(TimeUnit.MILLISECONDS.convert(delay, unit)), callable));
//				new FutureCallableRunnableTask<>(callable, this)));
	}

	/**
	 * Submits a periodic action that becomes enabled first after the
	 * given initial delay, and subsequently with the given period;
	 * that is, executions will commence after
	 * {@code initialDelay}, then {@code initialDelay + period}, then
	 * {@code initialDelay + 2 * period}, and so on.
	 *
	 * <p>The sequence of task executions continues indefinitely until
	 * one of the following exceptional completions occur:
	 * <ul>
	 * <li>The task is {@linkplain Future#cancel explicitly cancelled}
	 * via the returned future.
	 * <li>The executor terminates, also resulting in task cancellation.
	 * <li>An execution of the task throws an exception.  In this case
	 * calling {@link Future#get() get} on the returned future will throw
	 * {@link ExecutionException}, holding the exception as its cause.
	 * </ul>
	 * Subsequent executions are suppressed.  Subsequent calls to
	 * {@link Future#isDone isDone()} on the returned future will
	 * return {@code true}.
	 *
	 * <p>If any execution of this task takes longer than its period, then
	 * subsequent executions may start late, but will not concurrently
	 * execute.
	 *
	 * @param command      the task to execute
	 * @param initialDelay the time to delay first execution
	 * @param period       the period between successive executions
	 * @param unit         the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of
	 * the series of repeated tasks.  The future's {@link
	 * Future#get() get()} method will never return normally,
	 * and will throw an exception upon task cancellation or
	 * abnormal termination of a task execution.
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if command or unit is null
	 * @throws IllegalArgumentException   if period less than or equal to zero
	 */
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return queue(new TaskSchedulerAppointment(new AppointmentDefault(TimeUnit.MILLISECONDS.convert(initialDelay, unit)),
				command,
				TimeUnit.MILLISECONDS.convert(period, unit),
				true));
	}

	/**
	 * Submits a periodic action that becomes enabled first after the
	 * given initial delay, and subsequently with the given delay
	 * between the termination of one execution and the commencement of
	 * the next.
	 *
	 * <p>The sequence of task executions continues indefinitely until
	 * one of the following exceptional completions occur:
	 * <ul>
	 * <li>The task is {@linkplain Future#cancel explicitly cancelled}
	 * via the returned future.
	 * <li>The executor terminates, also resulting in task cancellation.
	 * <li>An execution of the task throws an exception.  In this case
	 * calling {@link Future#get() get} on the returned future will throw
	 * {@link ExecutionException}, holding the exception as its cause.
	 * </ul>
	 * Subsequent executions are suppressed.  Subsequent calls to
	 * {@link Future#isDone isDone()} on the returned future will
	 * return {@code true}.
	 *
	 * @param command      the task to execute
	 * @param initialDelay the time to delay first execution
	 * @param delay        the delay between the termination of one
	 *                     execution and the commencement of the next
	 * @param unit         the time unit of the initialDelay and delay parameters
	 * @return a ScheduledFuture representing pending completion of
	 * the series of repeated tasks.  The future's {@link
	 * Future#get() get()} method will never return normally,
	 * and will throw an exception upon task cancellation or
	 * abnormal termination of a task execution.
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if command or unit is null
	 * @throws IllegalArgumentException   if delay less than or equal to zero
	 */
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
	{
		return queue(new TaskSchedulerAppointment( new AppointmentDefault(TimeUnit.MILLISECONDS.convert(initialDelay, unit)),
				command,
				TimeUnit.MILLISECONDS.convert(delay, unit),
				false));
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted
	 * tasks are executed, but no new tasks will be accepted.
	 * Invocation has no additional effect if already shut down.
	 *
	 * <p>This method does not wait for previously submitted tasks to
	 * complete execution.  Use {@link #awaitTermination awaitTermination}
	 * to do that.
	 *
	 * @throws SecurityException if a security manager exists and
	 *                           shutting down this ExecutorService may manipulate
	 *                           threads that the caller is not permitted to modify
	 *                           because it does not hold {@link
	 *                           RuntimePermission}{@code ("modifyThread")},
	 *                           or the security manager's {@code checkAccess} method
	 *                           denies access.
	 */
	@Override
	public void shutdown() {

	}

	/**
	 * Attempts to stop all actively executing tasks, halts the
	 * processing of waiting tasks, and returns a list of the tasks
	 * that were awaiting execution.
	 *
	 * <p>This method does not wait for actively executing tasks to
	 * terminate.  Use {@link #awaitTermination awaitTermination} to
	 * do that.
	 *
	 * <p>There are no guarantees beyond best-effort attempts to stop
	 * processing actively executing tasks.  For example, typical
	 * implementations will cancel via {@link Thread#interrupt}, so any
	 * task that fails to respond to interrupts may never terminate.
	 *
	 * @return list of tasks that never commenced execution
	 * @throws SecurityException if a security manager exists and
	 *                           shutting down this ExecutorService may manipulate
	 *                           threads that the caller is not permitted to modify
	 *                           because it does not hold {@link
	 *                           RuntimePermission}{@code ("modifyThread")},
	 *                           or the security manager's {@code checkAccess} method
	 *                           denies access.
	 */
	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException("Method not implemented yet");
	}

	/**
	 * Returns {@code true} if this executor has been shut down.
	 *
	 * @return {@code true} if this executor has been shut down
	 */
	@Override
	public boolean isShutdown() {
		return false;
	}

	/**
	 * Returns {@code true} if all tasks have completed following shut down.
	 * Note that {@code isTerminated} is never {@code true} unless
	 * either {@code shutdown} or {@code shutdownNow} was called first.
	 *
	 * @return {@code true} if all tasks have completed following shut down
	 */
	@Override
	public boolean isTerminated() {
		return false;
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown
	 * request, or the timeout occurs, or the current thread is
	 * interrupted, whichever happens first.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout argument
	 * @return {@code true} if this executor terminated and
	 * {@code false} if the timeout elapsed before termination
	 * @throws InterruptedException if interrupted while waiting
	 */
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		synchronized (this) {
			while (!isClosed()) {
				long waitTime = deadline - System.nanoTime();
				if (waitTime <= 0) {
					return false;
				}
				TimeUnit.NANOSECONDS.timedWait(this, waitTime);
			}
			return true;
		}
	}

	/**
	 * Submits a value-returning task for execution and returns a
	 * Future representing the pending results of the task. The
	 * Future's {@code get} method will return the task's result upon
	 * successful completion.
	 *
	 * <p>
	 * If you would like to immediately block waiting
	 * for a task, you can use constructions of the form
	 * {@code result = exec.submit(aCallable).get();}
	 *
	 * <p>Note: The {@link Executors} class includes a set of methods
	 * that can convert some other common closure-like objects,
	 * for example, {@link PrivilegedAction} to
	 * {@link Callable} form so they can be submitted.
	 *
	 * @param task the task to submit
	 * @return a Future representing pending completion of the task
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if the task is null
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task)
	{
		return queue(new TaskSchedulerAppointment<>(new AppointmentDefault(),task));
				//new FutureCallableRunnableTask<>(task, this)));
	}

	/**
	 * Submits a Runnable task for execution and returns a Future
	 * representing that task. The Future's {@code get} method will
	 * return the given result upon successful completion.
	 *
	 * @param task   the task to submit
	 * @param result the result to return
	 * @return a Future representing pending completion of the task
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if the task is null
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result)
	{
		return queue(new TaskSchedulerAppointment<>(new AppointmentDefault(), task, result, true));
				//new FutureCallableRunnableTask<>(task, result, true, this)));
	}

	/**
	 * Submits a Runnable task for execution and returns a Future
	 * representing that task. The Future's {@code get} method will
	 * return {@code null} upon <em>successful</em> completion.
	 *
	 * @param task the task to submit
	 * @return a Future representing pending completion of the task
	 * @throws RejectedExecutionException if the task cannot be
	 *                                    scheduled for execution
	 * @throws NullPointerException       if the task is null
	 */
	@Override
	public Future<?> submit(Runnable task)
	{
		return queue(new TaskSchedulerAppointment<>(new AppointmentDefault(), task, null, false));
				//new FutureCallableRunnableTask<>(task, null, false, this)));
	}

	/**
	 * Executes the given tasks, returning a list of Futures holding
	 * their status and results when all complete.
	 * {@link Future#isDone} is {@code true} for each
	 * element of the returned list.
	 * Note that a <em>completed</em> task could have
	 * terminated either normally or by throwing an exception.
	 * The results of this method are undefined if the given
	 * collection is modified while this operation is in progress.
	 *
	 * @param tasks the collection of tasks
	 * @return a list of Futures representing the tasks, in the same
	 * sequential order as produced by the iterator for the
	 * given task list, each of which has completed
	 * @throws InterruptedException       if interrupted while waiting, in
	 *                                    which case unfinished tasks are cancelled
	 * @throws NullPointerException       if tasks or any of its elements are {@code null}
	 * @throws RejectedExecutionException if any task cannot be
	 *                                    scheduled for execution
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		throw new UnsupportedOperationException("Method not implemented yet");
	}

	/**
	 * Executes the given tasks, returning a list of Futures holding
	 * their status and results
	 * when all complete or the timeout expires, whichever happens first.
	 * {@link Future#isDone} is {@code true} for each
	 * element of the returned list.
	 * Upon return, tasks that have not completed are cancelled.
	 * Note that a <em>completed</em> task could have
	 * terminated either normally or by throwing an exception.
	 * The results of this method are undefined if the given
	 * collection is modified while this operation is in progress.
	 *
	 * @param tasks   the collection of tasks
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout argument
	 * @return a list of Futures representing the tasks, in the same
	 * sequential order as produced by the iterator for the
	 * given task list. If the operation did not time out,
	 * each task will have completed. If it did time out, some
	 * of these tasks will not have completed.
	 * @throws InterruptedException       if interrupted while waiting, in
	 *                                    which case unfinished tasks are cancelled
	 * @throws NullPointerException       if tasks, any of its elements, or
	 *                                    unit are {@code null}
	 * @throws RejectedExecutionException if any task cannot be scheduled
	 *                                    for execution
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("Method not implemented yet");
	}

	/**
	 * Executes the given tasks, returning the result
	 * of one that has completed successfully (i.e., without throwing
	 * an exception), if any do. Upon normal or exceptional return,
	 * tasks that have not completed are cancelled.
	 * The results of this method are undefined if the given
	 * collection is modified while this operation is in progress.
	 *
	 * @param tasks the collection of tasks
	 * @return the result returned by one of the tasks
	 * @throws InterruptedException       if interrupted while waiting
	 * @throws NullPointerException       if tasks or any element task
	 *                                    subject to execution is {@code null}
	 * @throws IllegalArgumentException   if tasks is empty
	 * @throws ExecutionException         if no task successfully completes
	 * @throws RejectedExecutionException if tasks cannot be scheduled
	 *                                    for execution
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException("Method not implemented yet");
	}

	/**
	 * Executes the given tasks, returning the result
	 * of one that has completed successfully (i.e., without throwing
	 * an exception), if any do before the given timeout elapses.
	 * Upon normal or exceptional return, tasks that have not
	 * completed are cancelled.
	 * The results of this method are undefined if the given
	 * collection is modified while this operation is in progress.
	 *
	 * @param tasks   the collection of tasks
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout argument
	 * @return the result returned by one of the tasks
	 * @throws InterruptedException       if interrupted while waiting
	 * @throws NullPointerException       if tasks, or unit, or any element
	 *                                    task subject to execution is {@code null}
	 * @throws TimeoutException           if the given timeout elapses before
	 *                                    any task successfully completes
	 * @throws ExecutionException         if no task successfully completes
	 * @throws RejectedExecutionException if tasks cannot be scheduled
	 *                                    for execution
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("Method not implemented yet");
	}

	/**
	 * Executes the given command at some time in the future.  The command
	 * may execute in a new thread, in a pooled thread, or in the calling
	 * thread, at the discretion of the {@code Executor} implementation.
	 *
	 * @param command the runnable task
	 * @throws RejectedExecutionException if this task cannot be
	 *                                    accepted for execution
	 * @throws NullPointerException       if command is null
	 */
	@Override
	public void execute(Runnable command) {
		submit(command);
	}

}