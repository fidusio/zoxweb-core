package org.zoxweb.server.task;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.task.CallableConsumer;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.task.ExceptionCallback;
import org.zoxweb.shared.util.SharedUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FutureCallableRunnableTask<V>
    implements TaskExecutor, Future<V>
{

    public final LogWrapper log = new LogWrapper(FunctionalInterface.class).setEnabled(false);
    private final Callable<V> callable;
    private final Runnable runnable;
    final TaskEvent taskEvent;
    private V result = null;
    private final boolean isFuture;
    final AtomicBoolean pendingExecution = new AtomicBoolean(true);

    FutureCallableRunnableTask(TaskEvent te)
    {
        SharedUtil.checkIfNulls("TaskEvent can't be null", te);
        callable = null;
        runnable = null;
        isFuture = false;
        this.taskEvent = te;
    }

    FutureCallableRunnableTask(Callable<V> callable, Object source)
    {
        SharedUtil.checkIfNulls("Callable can't be null", callable);
        this.callable = callable;
        this.runnable = null;
        this.isFuture = true;
        taskEvent = new TaskEvent(source,
                (callable instanceof CallableConsumer) ? ((CallableConsumer<V>) callable).isStackTraceEnabled(): true,
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
        else if(runnable != null)
            runnable.run();
        else
            taskEvent.getTaskExecutor().executeTask(taskEvent);

    }

    /**
     * @param event
     */
    @Override
    public void finishTask(TaskEvent event)
    {
        if (callable != null)
        {
            if (event.getExecutionException() != null)
            {
                // if there was an exception while call
                if (callable instanceof ExceptionCallback)
                {
                    ((ConsumerCallback<V>) callable).exception(event.getExecutionException());
                }
            }
            else if (callable instanceof Consumer) {
                ((Consumer<V>) callable).accept(result);
            }
        }
        pendingExecution.set(false);
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
        // this will cause error
        return taskEvent.execCount() != 0 && !pendingExecution.get();
    }

    /**
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public V get() throws InterruptedException, ExecutionException
    {
        synchronized(this)
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
