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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import org.zoxweb.server.io.IOUtil;

import org.zoxweb.shared.util.IsClosed;

public class NetworkTunnel
    implements IsClosed
{
	
	
	
	private static transient final Logger log = Logger.getLogger(NetworkTunnel.class.getName());
	public static boolean debug = true;
	

	static class StreamRelay
		implements Runnable, IsClosed
	{
		private InputStream is;
		private OutputStream os;
	    IsClosed[] counterParts;
		private boolean closedStat = false;
		StreamRelay(InputStream is, OutputStream os)
		{
			this.is = is;
			this.os = os;
		}
		
		private void start()
		{
			//TaskUtil.getDefaultTaskScheduler().queue(0,this);
			new Thread(this).start();
		}
		@Override
		public  void close() throws IOException
		{
			if (!closedStat)
			{
				if(debug)log.info("closed");
				closedStat = true;
				IOUtil.close(is);
				IOUtil.close(os);
			
				if (counterParts != null)
				{
					for (IsClosed ic : counterParts)
					{
						if(ic != null && !ic.isClosed())
							IOUtil.close(ic);
					}
				}
			}
			
		}
		@Override
		public void run() 
		{
			if(debug) log.info("started");
			byte[] buffer = new byte[2048];
			int read = 0;

			while(!isClosed() && read != -1)
			{
				try
				{
					read = is.read(buffer);
					if(debug) log.info("Read:" + read);
					if (read > 0)
					{
						os.write(buffer, 0, read);
					}
				}
				catch(IOException e)
				{
					//IOUtil.close(this);
					break;
				}
			}

			IOUtil.close(this);

		}
		@Override
		public boolean isClosed() 
		{
			// TODO Auto-generated method stub
			return closedStat;
		}
	}
	
	
	private StreamRelay sr1;
	private StreamRelay sr2;
	private Socket s1;
	private Socket s2;
	
	private boolean closedStat = false;
	
	public NetworkTunnel(Socket socket1, Socket socket2) throws IOException
	{
		s1 = socket1;
		s2 = socket2;
		sr1 = new StreamRelay(s1.getInputStream(), s2.getOutputStream());
		sr2 = new StreamRelay(s2.getInputStream(), s1.getOutputStream());
		
		sr1.counterParts = new IsClosed[] {sr2, this};
		sr2.counterParts = new IsClosed[] {sr1};
		sr1.start();
		sr2.start();
	}

	@Override
	public synchronized void close() 
			throws IOException 
	{
		if (!closedStat)
		{
			
			closedStat = true;
			IOUtil.close(sr1);
			IOUtil.close(sr2);
			IOUtil.close(s1);
			IOUtil.close(s2);
			
		}
		
		
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return closedStat;
	}
	
	
}
