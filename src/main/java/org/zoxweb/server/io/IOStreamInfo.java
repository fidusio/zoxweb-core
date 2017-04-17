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
package org.zoxweb.server.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class IOStreamInfo
	implements AutoCloseable
{

	public volatile InputStream is;
	public volatile OutputStream os;
	public volatile Socket s;
	
	public IOStreamInfo(InputStream is, OutputStream os)
	{
		this.is = is;
		this.os = os;
	}
	
	public IOStreamInfo(Socket socket)
		throws IOException
	{
		this(socket.getInputStream(), socket.getOutputStream());
		s = socket;
	}
	
	public IOStreamInfo(Socket socket, int timeout)
		throws IOException
	{
		this(socket.getInputStream(), socket.getOutputStream());
		socket.setSoTimeout(timeout);
	}

	@Override
	public void close()
		throws IOException
	{
		IOUtil.close(is);
		IOUtil.close(os);
		IOUtil.close(s);
	}

}
