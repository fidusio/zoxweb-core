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
package org.zoxweb.server.net.security;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.zoxweb.server.io.FileMonitor;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.RuntimeUtil;
import org.zoxweb.shared.app.AppCreatorDefault;
import org.zoxweb.shared.data.events.*;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.security.IPBlockerConfig;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.app.AppCreator;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

public class IPBlockerListener
        implements EventHandlerListener<BaseEventObject<?>>, Closeable
{
	public static final String RESOURCE_NAME = "IPBlocker";



	class RemoteIPInfo
	{
		@Override
		public String toString() {
			return "RemoteIPInfo [remoteHost=" + remoteHost + ", detectionStartTime=" + detectionStartTime
					+ ", lastTimeDetected=" + lastTimeDetected + ", attackCount=" + attackCount + ", blocked=" + blocked
					+ ", attackRate=" + attackRate + " per min]";
		}
		InetSocketAddressDAO remoteHost = null;
		long detectionStartTime = 0;
		long lastTimeDetected = 0;
		long attackCount = 0;
		boolean blocked = false;
		float attackRate = 0;
	
		
	}
	
	
	
	public static class Creator
		extends AppCreatorDefault<IPBlockerListener, IPBlockerConfig>
	{

		@Override
		public String getName() {
			return RESOURCE_NAME;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public IPBlockerListener createApp() throws NullPointerException, IllegalArgumentException, IOException {
			// TODO Auto-generated method stub
			EventListenerManager tlm = TaskUtil.getDefaultEventManager();
			
			IPBlockerListener ipbl = new IPBlockerListener(getAppConfig(), TaskUtil.getDefaultTaskScheduler());
			tlm.addEventListener(ipbl);
			ipbl.fileMonitor = new FileMonitor(getAppConfig().getAuthFile(), tlm, true);
			TaskUtil.getDefaultTaskScheduler().queue(TimeInMillis.MINUTE.MILLIS, new Runnable(){
				public void run()
				{

					// Every one minute clear the timeout list
					ipbl.clearTimeouts();
					ipbl.getScheduler().queue(TimeInMillis.MINUTE.MILLIS, this);
				}
			});
			return ipbl;
		}


		@Override
		public void close() {

		}
	}
	
	private static final transient Logger log = Logger.getLogger(IPBlockerListener.class.getName());
	
	
	private TaskSchedulerProcessor tsp = null;
	protected transient volatile FileMonitor fileMonitor = null;	
	private Map<InetSocketAddressDAO, RemoteIPInfo> ripiMap = new LinkedHashMap<InetSocketAddressDAO, RemoteIPInfo>();
	private IPBlockerConfig ipbc;
	private Lock lock = new ReentrantLock();
	public IPBlockerListener(IPBlockerConfig ipbc,TaskSchedulerProcessor tsp)
	{
		this.ipbc = ipbc;
		this.tsp = tsp;
	}
	
	private TaskSchedulerProcessor getScheduler()
	{
		if (tsp != null)
			return tsp;
		
		return TaskUtil.getDefaultTaskScheduler();
	}
	
	
	public void clearTimeouts()
	{
		RemoteIPInfo all[] = ripiMap.values().toArray(new RemoteIPInfo[0]);
		for(RemoteIPInfo toCheck: all)
		{
			if (System.currentTimeMillis() - toCheck.detectionStartTime >= ipbc.getResetTimeInMillis())
			{
				ripiMap.remove(toCheck.remoteHost);
				log.info(toCheck + " was removed from check list");
			}
		}
	}
	

	private void processStringTokenEvent(StringTokenEvent ste)
	{
		String token = ste.getData();
		//long timeStamp = ste.getTimeStamp();
		
		
		if (SharedStringUtil.contains(token, ipbc.getAuthToken(), true))
		{
			List<NVPair> results = SharedUtil.toNVPairs(token, "=", " ");
			NVPair parameter = SharedUtil.lookup(results, ipbc.getAuthValue());
			String value = SharedUtil.getValue(parameter);
			if (!SharedStringUtil.isEmpty(value))
			{
				value = value.toLowerCase();
				reportBadAddress(new InetSocketAddressEvent(ste.getSource(), new InetSocketAddressDAO(value, 22)));
				//log.info(timeStamp + " we have a match:" + value );
//				RemoteIPInfo ripi = ripiMap.get(value);
//				if (ripi == null)
//				{
//					ripi = new RemoteIPInfo();
//					ripi.remoteHost = value;
//					ripi.detectionStartTime = timeStamp;
//					ripiMap.put(value, ripi);
//				}
//				ripi.lastTimeDetected = timeStamp;
//				ripi.attackCount++;
//
//
//				ripi.attackRate = ripi.lastTimeDetected > ripi.detectionStartTime ? ((ripi.attackCount*TimeInMillis.MINUTE.MILLIS) / ((ripi.lastTimeDetected - ripi.detectionStartTime))) : 0;
//
//				log.info(ripi + " minCount: " + ipbc.getTriggerCounter() + " rate: " + ipbc.getRate());
//
//				if (ripi.attackCount >= ipbc.getTriggerCounter() && ripi.attackRate >= ipbc.getRate())
//				{
//					log.info("we must block:" + ripi);
//					String command = SharedStringUtil.embedText(ipbc.getCommand(), ipbc.getCommandToken(), value);
//					log.info("we will execute:" + command);
//					try
//					{
//						synchronized(this)
//						{
//							if (!ripi.blocked)
//							{
//								RuntimeUtil.runAndFinish(command);
//								ripi.blocked = true;
//							}
//						}
//					}
//					catch (Exception e)
//					{
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
				
				
			}
		}

	}


	private void reportBadAddress(InetSocketAddressEvent isae) {
		if(isae != null)
		{
			long timeStamp = isae.getTimeStamp();
			InetSocketAddressDAO isad = isae.getData();
			if(isad != null)
			{
				RemoteIPInfo ripi = ripiMap.get(isad);
				if (ripi == null)
				{
					ripi = new RemoteIPInfo();
					ripi.remoteHost = isad;
					ripi.detectionStartTime = timeStamp;
					ripiMap.put(isad, ripi);
				}
				ripi.lastTimeDetected = timeStamp;
				ripi.attackCount++;


				ripi.attackRate = ripi.lastTimeDetected > ripi.detectionStartTime ? ((ripi.attackCount * TimeInMillis.MINUTE.MILLIS) / ((ripi.lastTimeDetected - ripi.detectionStartTime))) : 0;

				log.info(ripi + " minCount: " + ipbc.getTriggerCounter() + " rate: " + ipbc.getRate());

				if (ripi.attackCount >= ipbc.getTriggerCounter() && ripi.attackRate >= ipbc.getRate()) {
					log.info("we must block:" + ripi);
					String command = SharedStringUtil.embedText(ipbc.getCommand(), ipbc.getCommandToken(), isad.getInetAddress());
					if (!SharedStringUtil.isEmpty(ipbc.getPortToken()) && isad.getPort() > 0)
					{
						command = SharedStringUtil.embedText(command, ipbc.getPortToken(), ""+isad.getPort());
					}
					log.info("we will execute:" + command);
					try {

						lock.lock();
						if (!ripi.blocked) {
							RuntimeUtil.runAndFinish(command);
							ripi.blocked = true;
						}
						log.info(ripi.remoteHost + " blocked count of pending ips " + ripiMap.size());

					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally
					{
						lock.unlock();
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(BaseEventObject<?> event) {
		if(event instanceof InetSocketAddressEvent)
		{
			reportBadAddress((InetSocketAddressEvent) event);
		}
		else if (event instanceof StringTokenEvent)
		{
			processStringTokenEvent((StringTokenEvent) event);
		}
	}


	public IPBlockerConfig getIPBlockerConfig()
	{
		return ipbc;
	}



//	@Override
//	public void executeTask(TaskEvent event)
//	{
//		// TODO Auto-generated method stub
//		clearTimeouts();
//		getScheduler().queue(new AppointmentDefault(TimeInMillis.MINUTE.MILLIS), event);
//
//	}
//
//
//	@Override
//	public void finishTask(TaskEvent event) {
//		// TODO Auto-generated method stub
//
//	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		IOUtil.close(fileMonitor);
	}

	
	public static void main(String ...args)
	{
		try
		{
			int index = 0;
		
			
			
			IPBlockerConfig ipbc = new IPBlockerConfig();
			ipbc.setAuthFile(args[index++]);
			ipbc.setAuthToken(args[index++]);
			ipbc.setAuthValue(args[index++]);
			ipbc.setCommand(args[index++]);
			ipbc.setCommandToken(args[index++]);
			ipbc.setTriggerCounter(Long.parseLong(args[index++]));
			ipbc.setRate(Float.parseFloat(args[index++]));
			Creator c = new Creator();
			c.setAppConfig(ipbc);
			c.createApp();
			
			
//			TokenListenerManager tlm = new TokenListenerManager();
//			
//			IPBlockerListener ipbl = new IPBlockerListener(args[index++],  args[index++],  args[index++],  args[index++], 
//										  Long.parseLong(args[index++]), Float.parseFloat(args[index++]), TaskUtil.getDefaultTaskScheduler());
//			tlm.addEventListener(ipbl);
//
//			TaskUtil.getDefaultTaskScheduler().queue(new FileMonitor(file, tlm, true), new AppointmentDefault(TimeInMillis.MINUTE.MILLIS), ipbl);
			
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("IPBlocker fileToMonitor tokenMatch parameterName executionCommand commandToken");
		}
	}


	
}
