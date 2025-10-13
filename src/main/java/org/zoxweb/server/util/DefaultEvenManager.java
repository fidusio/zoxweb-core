package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.data.events.EventHandlerListener;
import org.zoxweb.shared.data.events.EventListenerManager;


public class DefaultEvenManager
        extends EventListenerManager<BaseEventObject<?>, EventHandlerListener<BaseEventObject<?>>> {

    protected final TaskSchedulerProcessor tsp;

    public DefaultEvenManager() {
        this(TaskUtil.defaultTaskScheduler());
    }

    public DefaultEvenManager(TaskSchedulerProcessor tsp) {
        this.tsp = tsp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void dispatch(BaseEventObject<?> event, boolean async) {
        EventHandlerListener<BaseEventObject<?>>[] all = getAllListeners();

        for (EventHandlerListener<?> el : all) {
            if (async) {
                tsp.queue(0, new Runnable() {
                    @Override
                    public void run() {
                        ((EventHandlerListener<BaseEventObject<?>>) el).handleEvent(event);
                    }
                });
            } else {
                ((EventHandlerListener<BaseEventObject<?>>) el).handleEvent(event);
            }
        }
    }

    @Override
    public void close() {
        IOUtil.close(tsp);
    }
}
