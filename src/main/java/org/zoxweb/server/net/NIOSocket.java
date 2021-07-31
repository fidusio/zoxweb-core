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
package org.zoxweb.server.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.security.SSLSessionDataFactory;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.data.events.EventListenerManager;
import org.zoxweb.shared.data.events.InetSocketAddressEvent;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.DaemonController;
import org.zoxweb.shared.util.SharedUtil;

/**
 * NIO Socket 
 * @author mnael
 */
public class NIOSocket
    implements Runnable, DaemonController, Closeable
{
	private static final transient Logger logger = Logger.getLogger(NIOSocket.class.getName());
	private static boolean debug = false;
	private boolean live = true;
	private final SelectorController selectorController;
	private final Executor tsp;
	private AtomicLong connectionCount = new AtomicLong();

	private long totalDuration = 0;
	private long dispatchCounter = 0;
	private long selectedCountTotal = 0;
	private long statLogCounter = 0;
	private AtomicLong attackTotalCount = new AtomicLong();
	private final long startTime = System.currentTimeMillis();
	private EventListenerManager<BaseEventObject<?>,?> eventListenerManager = null;
	

	
	
	
	public NIOSocket(Executor tsp) throws IOException
	{
		this(null, 0, null, tsp);
	}
	
	
	
	public NIOSocket(InetSocketAddress sa, int backlog, ProtocolSessionFactory<?> psf, Executor tsp) throws IOException
	{
		//SharedUtil.checkIfNulls("Null value", psf, sa);
		selectorController = new SelectorController(Selector.open());
		this.tsp = tsp;
		if (sa != null)
			addServerSocket(sa, backlog, psf);
		

		
		new Thread(this).start();
	}
	
	public SelectionKey addServerSocket(InetSocketAddress sa, int backlog, ProtocolSessionFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", sa, psf);
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(sa, backlog);
		logger.info(ssc + " added");
		return addServerSocket(ssc, psf);
	}
	
	public SelectionKey addServerSocket(ServerSocketChannel ssc,  ProtocolSessionFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", ssc, psf);
		
		SelectionKey sk = selectorController.register(ssc, SelectionKey.OP_ACCEPT);
		sk.attach(psf);
		logger.info(ssc + " added");
		
		return sk;
	}

	public SelectionKey addDatagramChannel(InetSocketAddress sa,  ProtocolSessionFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", sa, psf);
		DatagramChannel dc = DatagramChannel.open();
		dc.socket().bind(sa);
		
		return addDatagramChannel(dc, psf);
	}
	
	public SelectionKey addDatagramChannel(DatagramChannel dc,  ProtocolSessionFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", dc, psf);
		SelectionKey sk = selectorController.register(dc, SelectionKey.OP_ACCEPT);
		sk.attach(psf);
		logger.info(dc + " added");
		
		return sk;
	}
	
	public SelectionKey addSeverSocket(InetSocketAddressDAO sa, int backlog, ProtocolSessionFactory<?> psf) throws IOException
	{
		return addServerSocket(new InetSocketAddress(sa.getPort()), backlog, psf);
	}
	
	public SelectionKey addSeverSocket(int port, int backlog, ProtocolSessionFactory<?> psf) throws IOException
	{
		return addServerSocket(new InetSocketAddress(port), backlog, psf);
	}
	
	public void setEventManager(EventListenerManager<BaseEventObject<?>, ?> eventListenerManager)
	{
		this.eventListenerManager = eventListenerManager;
	}

	public EventListenerManager<BaseEventObject<?>, ?> getEventManager()
	{
		return eventListenerManager;
	}
	

	@Override
	public void run()
	{	
		long snapTime = System.currentTimeMillis();
		long attackTimestamp = 0;
		
		
		while(live)
		{
			try 
			{
				int selectedCount = 0;
				if (selectorController.getSelector().isOpen())
				{
					//log.info("total keys:" + selector.keys().size());
					//log.info("PreSelect:" + selectorController.getSelector().keys().size() + "," +TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
					selectedCount = selectorController.select();
					long detla = System.nanoTime();
					if (selectedCount > 0)
					{
						//log.info("PostSelect:" + selectorController.getSelector().keys().size() + "," + selectedCount + "," +TaskUtil.getDefaultTaskProcessor().availableExecutorThreads() + "," + totalConnections());
						//log.info("Selected count:"+selectedCount);
						Set<SelectionKey> selectedKeys = selectorController.getSelector().selectedKeys();
						Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
						selectedCountTotal += selectedCount;

						while(keyIterator.hasNext())
						{	    
						    SelectionKey key = keyIterator.next();
						    keyIterator.remove();
						    try
						    {	    	
						    	if (key.isValid() && SharedUtil.getWrappedValue(key.channel()).isOpen() && key.isReadable())
							    {
							    	ProtocolSessionProcessor currentPSP = (ProtocolSessionProcessor)key.attachment();
							    	if (currentPSP != null && currentPSP.isSelectable())
							    	{
							    		// very very crucial setup prior to processing
							    		currentPSP.setSelectable(false);
							    		currentPSP.attach(key);
							    		// a channel is ready for reading
								    	if (tsp != null)
								    	{
								    		
								    		//tsp.queueTask(new TaskEvent(this, currentPSP, key));
								    		tsp.execute(currentPSP);
								    	}
								    	else
								    	{
								    		currentPSP.accept(key);
								    	}
							    	}
							    } 
						    	else if(key.isValid() && SharedUtil.getWrappedValue(key.channel()).isOpen() && key.isAcceptable()) 
							    {
							        // a connection was accepted by a ServerSocketChannel.
							    	
							    	SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
							    	if (debug) logger.info("Accepted:" + sc);
							    	ProtocolSessionFactory<?> psf = (ProtocolSessionFactory<?>) key.attachment();
							    	
							    	// check if the incoming connection is allowed
							    	if (NetUtil.checkSecurityStatus(psf.getIncomingInetFilterRulesManager(), sc.getRemoteAddress(), null) !=  SecurityStatus.ALLOW)
							    	{
							    		try
							    		{ 	
							    			long currentAttackCount = attackTotalCount.incrementAndGet();
							    			if (attackTimestamp == 0)
							    			{
							    				attackTimestamp = System.currentTimeMillis();
							    			}
							    			
//							    			Logger log = psf.getLogger();
//							    			if(log == null)
//							    			{
//							    				log = logger;
//							    			}
							    			InetSocketAddress isa = (InetSocketAddress) ((ServerSocketChannel)key.channel()).getLocalAddress();
							    			
							    			
							    			
							    			// in try block with catch exception since logger can point to file log
							    			
							    			logger.info( "@ port:" + isa.getPort() + " access denied for:" + sc.getRemoteAddress());
							    			if(eventListenerManager != null)
											{
												if (sc.getRemoteAddress() instanceof InetSocketAddress)
												{
													InetSocketAddress remoteISA = (InetSocketAddress) sc.getRemoteAddress();
													if(remoteISA.getAddress() instanceof Inet4Address) {
														String remoteIPAddress = SharedNetUtil.toV4Address(remoteISA.getAddress().getAddress());
														InetSocketAddressEvent event = new InetSocketAddressEvent(this, new InetSocketAddressDAO(remoteIPAddress, isa.getPort()));
														eventListenerManager.dispatch(event, true);
													}
												}
											}
							    			
							    			if (currentAttackCount % 500 == 0)
							    			{
							    				float burstRate = (float) ((500.00/(float)(System.currentTimeMillis() - attackTimestamp))*TimeInMillis.SECOND.MILLIS);
							    				float overAllRate = (float)((float)currentAttackCount/(float)(System.currentTimeMillis() - startTime))*TimeInMillis.SECOND.MILLIS;
							    				logger.info(" Burst Attacks:" + burstRate+ " a/s" + " Total Attacks:" + overAllRate + " a/s" + " total:" + attackTotalCount + " in " + TimeInMillis.toString(System.currentTimeMillis() - startTime));
							    				attackTimestamp = 0;
							    			}
							    		}
							    		catch(Exception e)
							    		{
							    			e.printStackTrace();
							    		}
							    		finally
							    		{
							    			// had to close after log otherwise we have an open socket
							    			IOUtil.close(sc);
							    		}
							    		
							    	}
							    	else
							    	{
							    			    		
							    		//ProtocolSessionFactory<?> psf = (ProtocolSessionFactory<?>) key.attachment();
							    		// connection allowed lets set it up
							    		
								    	ProtocolSessionProcessor psp = psf.newInstance();
								    	
								    	psp.setSelectorController(selectorController);
								    	psp.setOutgoingInetFilterRulesManager(psf.getOutgoingInetFilterRulesManager());
								    	
								    	
								    	// secure socket
								    	SSLSessionDataFactory sslUtil = psf.getIncomingSSLSessionDataFactory();
								    	if (sslUtil != null)
								    	{
								    		if (debug) logger.info("we have ssl socket");
								    		psp.setInputSSLSessionData(sslUtil.create(false));
								    		
								    	}
								    	
								    	
								    	selectorController.register(NIOChannelCleaner.DEFAULT, sc, SelectionKey.OP_READ, psp, psf.isBlocking());
								    	
								    	connectionCount.incrementAndGet();
								    	//log.info("Connected:" + sc.getRemoteAddress() + " " + connectionCount.get());
								    
							    	}
	
							    } 
							    else if (key.isValid() && SharedUtil.getWrappedValue(key.channel()).isOpen() && key.isConnectable())
							    {
							        // a connection was established with a remote server.
							    } 
							    else if (key.isValid() && SharedUtil.getWrappedValue(key.channel()).isOpen() && key.isWritable())
							    {
							        // a channel is ready for writing
							    }
							   
						    }
						    catch(Exception e)
						    {
						    	e.printStackTrace();
						    }
						    
						    
						    try
						    {
						    	if (!key.isValid()|| !SharedUtil.getWrappedValue(key.channel()).isOpen())
						    	{
						    		selectorController.cancelSelectionKey(key);
						    	}
						    }
						    catch(Exception e)
						    {
						    	e.printStackTrace();
						    }
						    
						}
						
						detla = System.nanoTime() - detla;
						totalDuration += detla;
						dispatchCounter++;
					}
					
				}
				
				
				if(getStatLogCounter() > 0 && (dispatchCounter%getStatLogCounter() == 0 || (System.currentTimeMillis() - snapTime) > getStatLogCounter()))
				{
					snapTime = System.currentTimeMillis();
					logger.info("Average dispatch processing " + TimeInMillis.nanosToString(averageProcessingTime()) + 
							 " total time:" + TimeInMillis.nanosToString(totalDuration) +
							 " total dispatches:" + dispatchCounter + " total selectors:" + selectedCountTotal + 
							 " last select count:" + selectedCount + " total keys:" +selectorController.getSelector().keys().size() + 
							 " available workers:" + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads() + "," + TaskUtil.getDefaultTaskProcessor().pendingTasks())
					;
				}
			}
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
	}

	
	public long averageProcessingTime()
	{
		if (dispatchCounter > 0)
			return totalDuration/dispatchCounter;
		
		return -1;
		
	}
	
	@Override
	public boolean isClosed()
	{
		// TODO Auto-generated method stub
		return live;
	}

	@Override
	public void close() throws IOException 
	{
		// TODO Auto-generated method stub
		live = false;
		
		Set<SelectionKey>  keys = selectorController.getSelector().keys();
		for (SelectionKey sk : keys)
		{
			if (sk.channel() != null)
				IOUtil.close(sk.channel());
			try
			{
				selectorController.cancelSelectionKey(sk);
			}
			catch(Exception e)
			{
				
			}
		}
		
		//IOUtil.close(pw);
	
	}
	
	
	public long totalConnections()
	{
		return connectionCount.get();
	}
	
	public long getStatLogCounter()
	{
		return statLogCounter;
	}

	public void setStatLogCounter(long statLogCounter) 
	{
		this.statLogCounter = statLogCounter;
	}

//	public InetFilterRulesManager getIncomingInetFilterRulesManager()
//	{
//		return ifrm;
//	}
//	
//	public InetFilterRulesManager getOutgoingInetFilterRulesManager()
//	{
//		return outgoingIFRM;
//	}
	
	
}
