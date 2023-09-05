package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.RateController;

public class AsyncHTTPCall<C> {

    private DataDecoder<byte[], C> dataDecoder = null;
    private ConsumerCallback<C> consumerCallback = null;
    private TaskSchedulerProcessor tsp = null;


    public AsyncHTTPCall(DataDecoder<byte[], C> dataDecoder, ConsumerCallback<C> consumerCallback)
    {
        this(dataDecoder, consumerCallback, TaskUtil.getDefaultTaskScheduler());
    }

    public AsyncHTTPCall(DataDecoder<byte[], C> dataDecoder,  ConsumerCallback<C> consumerCallback, TaskSchedulerProcessor tsp)
    {
        this.tsp = tsp;
        this.dataDecoder = dataDecoder;
        this.consumerCallback = consumerCallback;
    }


    public void asyncSend(HTTPMessageConfigInterface hmci, RateController rc)
    {
        asyncSend(hmci, rc.nextDelay());
    }

    public void asyncSend(HTTPMessageConfigInterface hmci, long delay)
    {
        tsp.queue(delay, ()->{
            try
            {
                if(consumerCallback != null)
                    consumerCallback.accept(dataDecoder.decode(HTTPCall.send(hmci).getData()));
                else
                    HTTPCall.send(hmci);
            }
            catch(Exception e)
            {
                if(consumerCallback != null)
                    consumerCallback.exception(e);
            }
        });
    }
}
