package org.zoxweb.server.http;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.RateController;
import org.zoxweb.shared.util.SharedUtil;

import java.util.concurrent.Executor;

public class AsyncHTTPCall<C> {


    private class ToRun
    implements Runnable
    {
        private final HTTPMessageConfigInterface hmci;
        ToRun(HTTPMessageConfigInterface hmci)
        {
            this.hmci = hmci;
        }

        public void run()
        {
            try {
                HTTPResponseData hrd = HTTPCall.send(hmci);
                if (consumerCallback != null && dataDecoder != null)
                    consumerCallback.accept(dataDecoder.decode(hrd.getData()));
                else if (consumerCallback != null)
                    consumerCallback.accept((C) hrd);

            } catch (Exception e) {
                if (consumerCallback != null)
                    consumerCallback.exception(e);
            }
        }
    }

    private final DataDecoder<byte[], C> dataDecoder;
    private final ConsumerCallback<C> consumerCallback;

    private volatile RateController rc;
    private volatile TaskSchedulerProcessor tsp;

    private volatile Executor exec;



    public AsyncHTTPCall(ConsumerCallback<HTTPResponseData> consumerCallback)
    {
        this((ConsumerCallback<C>) consumerCallback, null);
    }


    public AsyncHTTPCall(ConsumerCallback<C> consumerCallback, DataDecoder<byte[], C> dataDecoder)
    {
        this.dataDecoder = dataDecoder;
        this.consumerCallback = consumerCallback;
    }


    public void asyncSend(HTTPMessageConfigInterface hmci)
    {
        if (exec != null)
            asyncSend(hmci, exec);
        else if (tsp != null)
            asyncSend(hmci, rc.nextDelay(), tsp);
        else
            throw new NullPointerException("No executor or TaskSchedulerProcessor set");
    }

    public void asyncSend(HTTPMessageConfigInterface hmci, Executor exec)
    {
        SharedUtil.checkIfNulls("HTTPMessageConfigInterface or Executor can't be null", hmci, exec);
        exec.execute(new ToRun(hmci));
    }

    public void asyncSend(HTTPMessageConfigInterface hmci, RateController rc, TaskSchedulerProcessor tsp)
    {
        SharedUtil.checkIfNulls("RateController can't be null", rc);
        asyncSend(hmci, rc.nextDelay(), tsp);
    }

    public void asyncSend(HTTPMessageConfigInterface hmci, long delay, TaskSchedulerProcessor tsp)
    {
        SharedUtil.checkIfNulls("HTTPMessageConfigInterface or TaskSchedulerProcessor can't be null ", hmci, tsp);
        tsp.queue(delay, new ToRun(hmci));
    }

    /**
     * This method is mutually exclusive with {@link #setExecutor(Executor)} it will set rc and tsp and set executor = null
     * @param rc rate controller to be used with scheduler
     * @param tsp task scheduler
     * @return this
     * @throws NullPointerException if rc or tsp null
     */
    public synchronized AsyncHTTPCall<C> setTaskScheduler(RateController rc, TaskSchedulerProcessor tsp)
        throws NullPointerException
    {
        SharedUtil.checkIfNulls("RateController or TaskSchedulerProcessor can't be null", rc, tsp);
        this.tsp = tsp;
        this.rc = rc;
        this.exec = null;
        return this;
    }

    /**
     * This method is mutually exclusive with {@link #setTaskScheduler(RateController, TaskSchedulerProcessor)} it will set
     * the executor and nullify rate controller and TaskSchedulerProcessor
     * @param exec thread pool executor to be set
     * @return this
     * @throws NullPointerException if exec null
     */
    public synchronized AsyncHTTPCall<C> setExecutor(Executor exec)
            throws NullPointerException
    {
        SharedUtil.checkIfNulls("Executor can't be null", exec);
        this.tsp = null;
        this.rc = null;
        this.exec = exec;
        return this;
    }
}
