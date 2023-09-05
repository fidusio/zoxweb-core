package org.zoxweb.server.flow;

import org.zoxweb.shared.task.SupplierConsumerTask;
import org.zoxweb.server.task.TaskSchedulerProcessor;

import java.util.concurrent.atomic.AtomicLong;


public abstract class DefaultFlowProcessor<F>
    implements FlowProcessor<F>
{



    protected TaskSchedulerProcessor tsp;
    protected AtomicLong sequence = new AtomicLong(0);

    protected DefaultFlowProcessor(TaskSchedulerProcessor tsp)
    {
        this.tsp = tsp;
    }

    public void publish(FlowEvent<F> event)
    {
        event.setSequence(sequence.getAndIncrement());
        tsp.queue(0, new SupplierConsumerTask(event, this));
    }


}
