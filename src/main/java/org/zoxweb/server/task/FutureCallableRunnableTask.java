package org.zoxweb.server.task;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.task.CallableConsumerTask;
import org.zoxweb.shared.util.SharedUtil;

import java.util.concurrent.*;

public class FutureCallableRunnableTask<V>
    implements TaskExecutor, Future<V>
{

    public final LogWrapper log = new LogWrapper(FunctionalInterface.class).setEnabled(false);
    private final Callable<V> callable;
    private final Runnable runnable;
    final TaskEvent taskEvent;
    private V result = null;
    private final boolean isFuture;

    FutureCallableRunnableTask(Callable<V> callable, Object source)
    {
        SharedUtil.checkIfNulls("Callable can't be null", callable);
        this.callable = callable;
        this.runnable = null;
        this.isFuture = true;
        taskEvent = new TaskEvent(source,
                (callable instanceof CallableConsumerTask) ? ((CallableConsumerTask<V>) callable).isExceptionStackTraceEnabled(): true,
                this);
    }

    FutureCallableRunnableTask(Runnable runnable, V result, boolean isFuture, Object source)
    {
        SharedUtil.checkIfNulls("Runnable can't be null", runnable);
        this.callable = null;
        this.runnable = runnable;
        this.result = result;
        this.isFuture = isFuture;
        taskEvent = new TaskEvent(source, true, this);
    }
    /**
     * @param event task event to be executed
     */
    @Override
    public void executeTask(TaskEvent event)
            throws Exception
    {
        if(isFuture) {
            synchronized (this) {
                try {
                    if (runnable != null)
                        runnable.run();
                    else
                        result = callable.call();
                } finally {
                    notifyAll();
                }
            }
        }
        else
            runnable.run();
    }

    /**
     * @param event
     */
    @Override
    public void finishTask(TaskEvent event)
    {
        if (callable != null && callable instanceof CallableConsumerTask)
        {
            if (event.getExecutionException() != null)
                ((CallableConsumerTask<V>) callable).exception(event.getExecutionException());
            else
                ((CallableConsumerTask<V>) callable).accept(result);
        }
    }



    /**
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * @return
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public boolean isDone()
    {
        return taskEvent.execCount() != 0;
    }

    /**
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (log.isEnabled()) log.getLogger().info("get()");


        synchronized (this)
        {
            if (!isDone())
            {
                wait();
            }
        }

        if(taskEvent.getExecutionException() != null)
        {
            throw new ExecutionException(taskEvent.getExecutionException());
        }

        return result;
    }

    /**
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if(log.isEnabled()) log.getLogger().info("get(timeout, timeunit)");

        synchronized (this)
        {
            if(!isDone())
            {
                wait(unit.toMillis(timeout));
            }
            if(!isDone())
                throw new TimeoutException("Result not available yet");
        }


        if(taskEvent.getExecutionException() != null)
        {
            throw new ExecutionException(taskEvent.getExecutionException());
        }

        return result;

    }
}