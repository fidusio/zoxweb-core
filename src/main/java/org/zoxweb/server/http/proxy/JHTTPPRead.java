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
package org.zoxweb.server.http.proxy;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.util.Const;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class JHTTPPRead implements Runnable
{
	private static final int BUFFER_SIZE = 4096;
	private static final Logger log = Logger.getLogger(Const.LOGGER_NAME);

	private BufferedInputStream in;

	private OutputStream out;

	private JHTTPPSession connection;

	private JHTTPPServer server;

	public JHTTPPRead(JHTTPPServer server, JHTTPPSession connection,
			BufferedInputStream l_in, BufferedOutputStream l_out) throws IOException {
		in = l_in;
		out = l_out;
		this.connection = connection;
		this.server = server;
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	public void run() {
		read();
	}

	private void read() 
	{
		int read = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		try 
		{
			while (!server.isClosed() && (read = in.read(buf)) != -1) 
			{
			
				if ( !server.isClosed())
				{
					
				
					out.write(buf, 0, read);
					out.flush();
					server.addBytesRead(read);
				}
				
			}
		} 
		catch (IOException e) 
		{
			if ( server.isDebugEnabled())
				log.info(e.getMessage());
		}

		try 
		{
			if (connection.getStatus() != JHTTPPSession.SC_CONNECTING_TO_HOST) // *uaaahhh*:
																					// fixes
																					// a
																					// very
																					// strange
																					// bug
				connection.getLocalSocket().close();
			// why? If we are connecting to a new host (and this thread is
			// already running!) , the upstream
			// socket will be closed. So we get here and close our own
			// downstream socket..... and the browser
			// displays an empty page because jhttpp2
			// closes the connection..... so close the downstream socket only
			// when NOT connecting to a new host....
		} 
		catch (IOException e_socket_close) 
		{
			//log.info(e_socket_close.getMessage());
		}
	}

	public void close() 
	{
		IOUtil.close( in);
	}
}
