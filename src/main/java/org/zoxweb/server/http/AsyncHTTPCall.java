package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.RateController;

public class AsyncHTTPCall<C> {

    private DataDecoder<byte[], C> dataDecoder = null;
    private ConsumerCallback<C> consumerCallback = null;
    private TaskSchedulerProcessor tsp = null;


    public AsyncHTTPCall(ConsumerCallback<HTTPResponseData> consumerCallback)
    {
        this((ConsumerCallback<C>) consumerCallback, null, TaskUtil.getDefaultTaskScheduler());
    }

    public AsyncHTTPCall(ConsumerCallback<C> consumerCallback, DataDecoder<byte[], C> dataDecoder)
    {
        this(consumerCallback, dataDecoder, TaskUtil.getDefaultTaskScheduler());
    }

    public AsyncHTTPCall(ConsumerCallback<C> consumerCallback, DataDecoder<byte[], C> dataDecoder, TaskSchedulerProcessor tsp)
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
                HTTPResponseData hrd = HTTPCall.send(hmci);
                if(consumerCallback != null && dataDecoder != null)
                    consumerCallback.accept(dataDecoder.decode(hrd.getData()));
                else if (consumerCallback != null)
                    consumerCallback.accept((C) hrd);

            }
            catch(Exception e)
            {
                if(consumerCallback != null)
                    consumerCallback.exception(e);
            }
        });
    }
}
