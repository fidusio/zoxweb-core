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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import java.util.logging.Logger;


import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.security.SSLSessionDataFactory;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.util.NVGenericMap;

import java.nio.channels.SocketChannel;

public class EchoProcessor 
    extends ProtocolSessionProcessor
{
	
	private static final transient Logger log = Logger.getLogger(EchoProcessor.class.getName());

	private ByteBuffer bBuffer = ByteBuffer.allocate(1024);
	private static long counter = 0;
	
	public static final ProtocolSessionFactory<EchoProcessor> FACTORY = new ProtocolSessionFactory<EchoProcessor>()
			{

				@Override
				public EchoProcessor newInstance() {
					// TODO Auto-generated method stub
					return new EchoProcessor();
				}
				public boolean isBlocking()
				{
					return false;
				}
				@Override
				public SSLSessionDataFactory getIncomingSSLSessionDataFactory() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public InetFilterRulesManager getIncomingInetFilterRulesManager() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public void setIncomingInetFilterRulesManager(InetFilterRulesManager incomingIFRM) {
					// TODO Auto-generated method stub
					
				}
				@Override
				public InetFilterRulesManager getOutgoingInetFilterRulesManager() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public void setOutgoingInetFilterRulesManager(InetFilterRulesManager incomingIFRM) {
					// TODO Auto-generated method stub
					
				}
				@Override
				public void setIncomingSSLSessionDataFactory(SSLSessionDataFactory sslSessionDataFactory) {
					// TODO Auto-generated method stub
					
				}
				@Override
				public Logger getLogger() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public void setLogger(Logger logger) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public NVGenericMap getSessionProperties() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public String getName() {
					// TODO Auto-generated method stub
					return "EchoFactory";
				}
				
				
				
		
			};
	
	
	private EchoProcessor()
	{
		counter++;
		log.info("Session:" + counter);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "echo";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Echo";
	}

	

	@Override
	public void accept(SelectionKey key)
	{
    	try
    	{
    		int read;
    		bBuffer.clear();
    		do
    		{
    			
    			read = ((SocketChannel)key.channel()).read(bBuffer);
    			
    			
    		}while(read > 0);
    		
    		
    		if (bBuffer.position() > 0)
			{
    			log.info("read size:" + read);
    			ByteBuffer wBuffer = ByteBuffer.allocate(1024);
    			wBuffer.put("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=iso-8859-1\r\n\r\n<html><body><h1>Hello, World!</h1></body></html>\r\n\r\n".getBytes("UTF-8"));
    			
    			wBuffer.flip();
    			((SocketChannel)key.channel()).write(wBuffer);
    			
    			IOUtil.close(key.channel());
    			
			}
    		
	    	if (read == -1)
	    	{
	    		key.cancel();
	    		IOUtil.close(key.channel());
	    	}
    	}
    	catch(IOException e)
    	{
  
    		key.cancel();
    		IOUtil.close(key.channel());
    	}
    	
    	setSelectable(true);
		
	}
	
	
	
	
	
	@SuppressWarnings("resource")
	public static void main(String ...args)
	{
		try
		{
			new NIOSocket(new InetSocketAddress(Integer.parseInt(args[0])), 128, EchoProcessor.FACTORY, TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public boolean isClosed() {
//		// TODO Auto-generated method stub
//		return false;
//	}
	
	public void postOp() 
	{
		// TODO Auto-generated method stub
		//ByteBufferUtil.cache(bBuffer);
	}

}
