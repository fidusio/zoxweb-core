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

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.data.events.EventListenerManager;
import org.zoxweb.shared.data.events.InetSocketAddressEvent;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * NIO Socket 
 * @author mnael
 */
public class NIOSocket
    implements Runnable, DaemonController, Closeable
{
	public static final LogWrapper logger = new LogWrapper(NIOSocket.class).setEnabled(false);
	//public static boolean debug = false;
	private boolean live = true;
	private final SelectorController selectorController;
	private final Executor executor;
	private  long connectionCount = 0;


	private long selectedCountTotal = 0;
	private long statLogCounter = 0;
	private long attackTotalCount = 0;
	private final long startTime = System.currentTimeMillis();
	private EventListenerManager<BaseEventObject<?>,?> eventListenerManager = null;
	private final RateCounter callsCounter = new RateCounter("nio-calls-counter");

	

	
	
	
	public NIOSocket(Executor tsp) throws IOException
	{
		this(null, 0, null, tsp);
	}
	
	
	
	public NIOSocket(InetSocketAddress sa, int backlog, ProtocolFactory<?> psf, Executor exec) throws IOException
	{
		logger.getLogger().info("Executor: " + exec);
		selectorController = new SelectorController(Selector.open());
		this.executor = exec;
		if (sa != null)
			addServerSocket(sa, backlog, psf);

		TaskUtil.startRunnable(this, "NIO-SOCKET");
	}
	
	public SelectionKey addServerSocket(InetSocketAddress sa, int backlog, ProtocolFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", sa, psf);
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(sa, backlog);
		return addServerSocket(ssc, psf);
	}
	
	public SelectionKey addServerSocket(ServerSocketChannel ssc,  ProtocolFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", ssc, psf);
		
		SelectionKey sk = selectorController.register(ssc, SelectionKey.OP_ACCEPT, psf, false);
		logger.getLogger().info(ssc + " added");
		
		return sk;
	}

	public SelectionKey addDatagramChannel(InetSocketAddress sa,  ProtocolFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", sa, psf);
		DatagramChannel dc = DatagramChannel.open();
		dc.socket().bind(sa);
		
		return addDatagramChannel(dc, psf);
	}


	public SelectionKey addClientSocket(InetSocketAddress sa, ProtocolFactory<?> psf) throws IOException
	{
		return addClientSocket(sa, psf, null);
	}

	public SelectionKey addClientSocket(InetSocketAddress sa, ProtocolFactory<?> psf, RateController rateController) throws IOException
	{
		SocketChannel sc = SocketChannel.open();

		SelectionKey ret = selectorController.register(sc, SelectionKey.OP_CONNECT, psf, false);
		sc.connect(sa);
		return ret;
	}



	public SelectionKey addDatagramChannel(DatagramChannel dc,  ProtocolFactory<?> psf) throws IOException
	{
		SharedUtil.checkIfNulls("Null values", dc, psf);
		SelectionKey sk = selectorController.register(dc, SelectionKey.OP_ACCEPT, psf, false);
		logger.getLogger().info(dc + " added");
		
		return sk;
	}
	
	public SelectionKey addServerSocket(InetSocketAddressDAO sa, int backlog, ProtocolFactory<?> psf) throws IOException
	{
		return addServerSocket(new InetSocketAddress(sa.getPort()), backlog, psf);
	}
	
	public SelectionKey addServerSocket(int port, int backlog, ProtocolFactory<?> psf) throws IOException
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
				if (selectorController.isOpen())
				{
					selectedCount = selectorController.select();
					long delta = System.nanoTime();
					if (selectedCount > 0)
					{
						Set<SelectionKey> selectedKeys = selectorController.selectedKeys();
						Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
						selectedCountTotal += selectedCount;

						while(keyIterator.hasNext())
						{	    
						    SelectionKey key = keyIterator.next();
						    keyIterator.remove();

						    try
						    {


						    	if (key.isReadable()
									&& key.isValid()
									&& key.channel().isOpen())
							    {
									// channel has data to read
									// this is the reading part of the process
							    	ProtocolHandler currentPP = (ProtocolHandler) key.attachment();
									if (currentPP != null)
							    	{
							    		// very,very,very crucial setup prior to processing
										// we are disabling the key operations by the selector
										// for the current selection key
										int keyOPs = key.interestOps();
										key.interestOps(0);

							    		// a channel is ready for reading
								    	if (executor != null)
								    	{
								    		executor.execute(()->
											{
								    			try
												{
													currentPP.accept(key);
												}
								    			catch (Exception e)
												{
								    				e.printStackTrace();
												}
								    			// very crucial step
												if(key.isValid())
												{
													// restoring selection ops for the selection key
													key.interestOps(keyOPs);
													selectorController.wakeup();
												}
											});
								    	}
								    	else
										{
											// no executor set so the current thread must process the incoming data
											try
											{
												currentPP.accept(key);
											}
											catch (Exception e)
											{
												e.printStackTrace();
											}
											// very crucial step
											if(key.isValid())
											{
												key.interestOps(keyOPs);
												selectorController.wakeup();
											}
								    	}
							    	}

							    } 
						    	else if(key.isAcceptable()
										&& key.isValid()
										&& key.channel().isOpen())
							    {
							        // a connection was accepted by a ServerSocketChannel.
							    	
							    	SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
									ProtocolFactory<?> protocolFactory = (ProtocolFactory<?>) key.attachment();
									if(logger.isEnabled()) logger.getLogger().info("Accepted: " + sc + " psf:" + protocolFactory);
							    	// check if the incoming connection is allowed


							    	if (NetUtil.checkSecurityStatus(protocolFactory.getIncomingInetFilterRulesManager(), sc, null) !=  SecurityStatus.ALLOW)
							    	{
							    		try
							    		{ 	
							    			long currentAttackCount = ++attackTotalCount;
							    			if (attackTimestamp == 0)
							    			{
							    				attackTimestamp = System.currentTimeMillis();
							    			}

							    			InetSocketAddress isa = (InetSocketAddress) ((ServerSocketChannel)key.channel()).getLocalAddress();
							    			// in try block with catch exception since logger can point to file log
							    			logger.getLogger().info( "@ port:" + isa.getPort() + " access denied for:" + sc.getRemoteAddress());
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
							    				float overAllRate = ((float)currentAttackCount/(float)(System.currentTimeMillis() - startTime))*TimeInMillis.SECOND.MILLIS;
							    				logger.getLogger().info(" Burst Attacks:" + burstRate+ " a/s" + " Total Attacks:" + overAllRate + " a/s" + " total:" + attackTotalCount + " in " + TimeInMillis.toString(System.currentTimeMillis() - startTime));
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
							    		// create a protocol instance
								    	ProtocolHandler protocolHandler = protocolFactory.newInstance();
								    	
								    	protocolHandler.setSelectorController(selectorController);
								    	protocolHandler.setExecutor(executor);
								    	protocolHandler.setOutgoingInetFilterRulesManager(protocolFactory.getOutgoingInetFilterRulesManager());

										// if we have an executor
										// accept the new connection

										if (executor != null && protocolFactory.isComplexSetup())
										{
											executor.execute(() ->
											{
												try
												{
													protocolHandler.setupConnection(sc, protocolFactory.isBlocking());
												}
												catch (IOException e)
												{
													e.printStackTrace();
													IOUtil.close(protocolHandler);
												}
											});
										}
										else
										{
											protocolHandler.setupConnection(sc, protocolFactory.isBlocking());
										}
								    	connectionCount++;
								    
							    	}
	
							    } 
							    else if (key.isValid()
										&& key.channel().isOpen()
										&&  key.isConnectable())
							    {

									connectionCount++;
									ProtocolFactory<?> protocolFactory = (ProtocolFactory<?>) key.attachment();
									ProtocolHandler ph = protocolFactory.newInstance();
									ph.setSelectorController(selectorController);
									ph.setupConnection((AbstractSelectableChannel) key.channel(), false);
									key.attach(ph);


									int keyOPs = key.interestOps();
									key.interestOps(0);

									// a channel is ready for reading
									if (executor != null)
									{
										executor.execute(()->
										{
											try
											{
												ph.accept(key);
											}
											catch (Exception e)
											{
												e.printStackTrace();
											}
											// very crucial step
											if(key.isValid())
											{
												key.interestOps(keyOPs);
												selectorController.wakeup();
											}
										});
									}
									else
									{
										// no executor set so the current thread must process the incoming data
										try
										{
											ph.accept(key);
										}
										catch (Exception e)
										{
											e.printStackTrace();
										}
										// very crucial step
										if(key.isValid())
										{
											key.interestOps(keyOPs);
											selectorController.wakeup();
										}
									}



//									key.attachment()
//									if (((SocketChannel) key.channel()).isConnectionPending()) {
//										((SocketChannel) key.channel()).finishConnect();
//									}
//									logger.getLogger().info("connectable:" + key);
//									selectorController.cancelSelectionKey(key);
//									IOUtil.close(key.channel());



							    }
//							    else if (key.isValid()
//										&& key.channel().isOpen()
//										&& key.isWritable())
//							    {
//							         a channel is ready for writing
//							    }
							   
						    }
						    catch(Exception e)
						    {
								if(!(e instanceof CancelledKeyException))
						    		e.printStackTrace();
						    }

						    try
						    {
						    	// key clean up
						    	if (!key.isValid()
									|| !key.channel().isOpen())
						    	{
									key.cancel();
									key.channel().close();
						    	}
						    }
						    catch(Exception e)
						    {
						    	e.printStackTrace();
						    }
						    
						}
						delta = System.nanoTime() - delta;
						callsCounter.register(delta);
					}
				}

				// stats
				if(getStatLogCounter() > 0 && (callsCounter.getCounts()%getStatLogCounter() == 0 || (System.currentTimeMillis() - snapTime) > getStatLogCounter()))
				{
					snapTime = System.currentTimeMillis();
					logger.getLogger().info("Average dispatch processing " + TimeInMillis.nanosToString(averageProcessingTime()) +
							" total time:" + TimeInMillis.nanosToString(callsCounter.getDeltas()) +
							" total dispatches:" + callsCounter.getCounts() + " total select calls:" + selectedCountTotal +
							" last select count:" + selectedCount + " total select keys:" +selectorController.keysCount() +
						   " available workers:" +  TaskUtil.availableThreads(executor) + "," + TaskUtil.pendingTasks(executor));
				}
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

	
	public long averageProcessingTime()
	{
		return (long) callsCounter.average();
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
		
		Set<SelectionKey>  keys = selectorController.keys();
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

	}
	
	
	public long totalConnections()
	{
		return connectionCount;
	}
	
	public long getStatLogCounter()
	{
		return statLogCounter;
	}

	public void setStatLogCounter(long statLogCounter) 
	{
		this.statLogCounter = statLogCounter;
	}

	public NVGenericMap getStats()
	{
		NVGenericMap ret = new NVGenericMap("nio_socket");
		ret.add("time_stamp", DateUtil.DEFAULT_JAVA_FORMAT.format(new Date()));
		ret.build(new NVLong("connection_counts", totalConnections())).
				build(new NVLong("select_calls_counts", selectedCountTotal)).
				build(new NVLong("attack_counts", attackTotalCount)).
				build(new NVLong("selection_key_registration_count", selectorController.registrationCount())).
				build(new NVLong("total_selection_keys", selectorController.selectionKeysCount())).
				build(callsCounter);
		return ret;
	}
	
}
