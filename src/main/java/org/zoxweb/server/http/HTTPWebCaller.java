package org.zoxweb.server.http;

import org.zoxweb.server.net.ssl.SSLCheckDisabler;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class HTTPWebCaller
		implements Runnable
{
	public static AtomicInteger TOTAL_COUNTER = new AtomicInteger();
	public static AtomicInteger SUCCESS_COUNTER = new AtomicInteger();
	public static AtomicInteger FAILED_COUNTER = new AtomicInteger();
	
	private static final AtomicInteger counter = new AtomicInteger();
	
	
	public final int id = counter.incrementAndGet();
	private static final transient Logger log = Logger.getLogger(HTTPWebCaller.class.getName());
	private boolean logError;

	private HTTPMessageConfigInterface hcc;
	private BiConsumer<HTTPMessageConfigInterface, HTTPResponseData> consumer;
	
	public HTTPWebCaller(HTTPMessageConfigInterface hcc,
						 BiConsumer<HTTPMessageConfigInterface, HTTPResponseData> consumer)
	{
		this(hcc, consumer, false);
	}
	
	public HTTPWebCaller(HTTPMessageConfigInterface hcc,
						 BiConsumer<HTTPMessageConfigInterface, HTTPResponseData> consumer,
						 boolean logError)
	{
		this.hcc = hcc;
		this.consumer = consumer;
		this.logError = logError;
	}
	
	

	@SuppressWarnings("unchecked")
	@Override
	public void run()
	{
		// TODO Auto-generated method stub
//		TaskEvent event = attachedEvent();
//		int index = 0;
//		HTTPMessageConfigInterface hcc = (HTTPMessageConfigInterface) event.getTaskExecutorParameters()[index++];
//		BiConsumer<HTTPMessageConfigInterface, HTTPResponseData> consumer = event.getTaskExecutorParameters().length > index ? (BiConsumer<HTTPMessageConfigInterface, HTTPResponseData>)event.getTaskExecutorParameters()[index++] : null;
		try
		{
			
			HTTPResponseData rd = new HTTPCall(hcc, SSLCheckDisabler.SINGLETON).sendRequest();
			log.info("[" + id +"]"+ rd.getStatus() + "," + rd.getData().length);
			if (consumer != null)
				consumer.accept(hcc, rd);
			SUCCESS_COUNTER.incrementAndGet();
		}
		catch(Exception e)
		{
			if (logError)
				e.printStackTrace();
			FAILED_COUNTER.incrementAndGet();
			log.info("[" + id +"] failed " + e);
		}
		
		
		TOTAL_COUNTER.incrementAndGet();
	}
	
	
	
//	public static void waitFor(int count)
//	{
//		do
//		{
//			try
//			{
//				Thread.sleep(100);
//			}
//			catch (InterruptedException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}while (HTTPWebCaller.TOTAL_COUNTER.get() != count);
//	}
	
	
	
	public static String stats()
	{
		return "Total: " +TOTAL_COUNTER.get() + " Successful: " + SUCCESS_COUNTER.get() + " Failed: " + FAILED_COUNTER.get();
	}
}