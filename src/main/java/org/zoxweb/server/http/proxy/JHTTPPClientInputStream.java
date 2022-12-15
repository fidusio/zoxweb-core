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

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class JHTTPPClientInputStream 
    extends BufferedInputStream
{
	private static final transient Logger log = Logger.getLogger(Const.LOGGER_NAME);
	// private boolean filter = false;
	private String buf;

	private int lread = 0;

	/**
	 * The length of the header (with body, if one)
	 */
	private int header_length = 0;

	/**
	 * The length of the (optional) body of the actual request
	 */
	private int content_len = 0;

	/**
	 * This is set to true with requests with bodies, like "POST"
	 */
	private boolean body = false;

	private JHTTPPServer server;

	private JHTTPPSession connection;

	private InetAddress remote_host;

	private String remote_host_name;

	private boolean ssl = false;

	private String errordescription;

	private int statuscode;

	public String url;

	public String method;

	public int remote_port = 0;

	public int post_data_len = 0;

	public int getHeaderLength() {
		return header_length;
	}

	public InetAddress getRemoteHost() {
		return remote_host;
	}

	public String getRemoteHostName() {
		return remote_host_name;
	}

	public JHTTPPClientInputStream(JHTTPPServer server,
			JHTTPPSession connection, InputStream a) {
		super(a);
		this.server = server;
		this.connection = connection;
	}

	/**
	 * Handler for the actual HTTP request
	 * 
	 * @exception IOException
	 */
	public int read(UByteArrayOutputStream buffer) throws IOException {
		statuscode = JHTTPPSession.SC_OK;
		if (ssl)
		{
			return super.read(buffer.getInternalBuffer());	
		}
		boolean cookies_enabled = server.enableCookiesByDefault();
		StringBuilder rq = new StringBuilder();
		header_length = 0;
		post_data_len = 0;
		content_len = 0;
		boolean start_line = true;
		buf = getLine(); // reads the first line

		while (lread > 2) {
			if (start_line) 
			{
				start_line = false;
				int methodID = server.getHttpMethod(buf);
				switch (methodID) 
				{
				case -1:
					statuscode = JHTTPPSession.SC_NOT_SUPPORTED;
					break;
				case 2:
					ssl = true;
				default:
					InetAddress host = parseRequest(buf, methodID);
					if (statuscode != JHTTPPSession.SC_OK)
						break; // error occured, go on with the next line

					if (!server.use_proxy && !ssl)
					{
						/* creates a new request without the hostname */
						buf = method + " " + url + " "
								+ server.getHttpVersion() + "\r\n";
						lread = buf.length();
					}
					if ((server.use_proxy && connection.notConnected())
							|| !host.equals(remote_host)) {
						if (server.isDebugEnabled())
							log.info("read_f: STATE_CONNECT_TO_NEW_HOST");
						statuscode = JHTTPPSession.SC_CONNECTING_TO_HOST;
						remote_host = host;
					}
					/*
					 * ------------------------- url blocking (only "GET"
					 * method) -------------------------
					 */
					if (JHTTPPServer.block_urls && methodID == 0
							&& statuscode != JHTTPPSession.SC_FILE_REQUEST) {
						if (server.isDebugEnabled())
							System.out.println("Searching match...");
						JHTTPPURLMatch match = server
								.findMatch(this.remote_host_name + url);
						if (match != null) {
							if (server.isDebugEnabled())
								System.out.println("Match found!");
							cookies_enabled = match.getCookiesEnabled();
							if (match.getActionIndex() == -1)
								break;
							OnURLAction action = (OnURLAction) server
									.getURLActions().elementAt(
											match.getActionIndex());
							if (action.onAccessDeny()) {
								statuscode = JHTTPPSession.SC_URL_BLOCKED;
								if (action.onAccessDenyWithCustomText())
									errordescription = action
											.getCustomErrorText();
							} else if (action.onAccessRedirect()) {
								statuscode = JHTTPPSession.SC_MOVED_PERMANENTLY;
								errordescription = action.newLocation();
							}
						}// end if match!=null)
					} // end if (server.block...
				} // end switch
			}// end if(startline)
			else {
				/*-----------------------------------------------
				 * Content-Length parsing
				 *-----------------------------------------------*/
				if (SharedStringUtil.contains(buf.toUpperCase(), "CONTENT-LENGTH")) {
					String clen = buf.substring(16);
					if (clen.indexOf("\r") != -1)
						clen = clen.substring(0, clen.indexOf("\r"));
					else if (clen.indexOf("\n") != -1)
						clen = clen.substring(0, clen.indexOf("\n"));
					try {
						content_len = Integer.parseInt(clen);
					} catch (NumberFormatException e) {
						statuscode = JHTTPPSession.SC_CLIENT_ERROR;
					}
					if (server.isDebugEnabled())
						log.info("read_f: content_len: " + content_len);
					if (!ssl)
						body = true; // Note: in HTTP/1.1 any method can have
										// a body, not only "POST"
				} 
				else if (SharedStringUtil.contains(buf, "Proxy-Connection:", true)) 
				{
					if (!server.use_proxy)
						buf = null;
					else 
					{
						buf = "Proxy-Connection: Keep-Alive\r\n";
						lread = buf.length();
					}
				}
				/*
				 * else if (server.startsWith(buf,"Connection:")) { if
				 * (!server.use_proxy) { buf="Connection: Keep-Alive\r\n"; //use
				 * always keep-alive lread=buf.length(); } else buf=null; }
				 */
				/*-----------------------------------------------
				 * cookie crunch section
				 *-----------------------------------------------*/
				else if (SharedStringUtil.contains(buf, "Cookie:")) {
					if (!cookies_enabled)
						buf = null;
				}
				/*------------------------------------------------
				 * Http-Header filtering section
				 *------------------------------------------------*/
				else if (server.filter_http)
				{
					if (SharedStringUtil.contains(buf, "Referer:")) {// removes
																// "Referer"
						buf = null;
						//log.info("removed referer");
					} else if (SharedStringUtil.contains(buf, "User-Agent")) // changes
																		// User-Agent
					{
						buf = "User-Agent: " + server.getUserAgent() + "\r\n";
						lread = buf.length();
						//log.info("updated user-agent");
					}
				}
			}
			if (buf != null) {
				rq.append( buf);
				if (server.isDebugEnabled())
					log.info(buf);
				header_length += lread;
			}
			buf = getLine();
		}
		rq.append(buf); // adds last line (should be an empty line) to the header
					// String
		header_length += lread;

		if (header_length == 0) {
			if (server.isDebugEnabled())
				log.info("header_length=0, setting status to SC_CONNECTION_CLOSED (buggy request)");
			statuscode = JHTTPPSession.SC_CONNECTION_CLOSED;
		}

		//int newSize = header_length + content_len;
		//if ( buffer.bah.length < newSize)
		//{
		//	buffer.bah = Arrays.copyOf(buffer.bah, newSize+1 );
		//}
		
		
		for (int i = 0; i < header_length; i++)
		{
			//buffer.bah[i] = (byte) rq.charAt(i);
			
			buffer.write((byte)rq.charAt(i));
		}
		//log.info("rq:" + rq);

		if (body) {// read the body, if "Content-Length" given
			post_data_len = 0;
			/*int newSize = header_length + content_len;
			if ( buffer.bah.length < newSize)
			{
				buffer.bah = Arrays.copyOf(buffer.bah, newSize+1 );
			}*/
			while (post_data_len < content_len) {
				//buffer.bah[header_length + post_data_len] = (byte) read(); // writes
																	// data into
																	// the array
				buffer.write((byte) read());
				
				post_data_len++;
			}
			header_length += content_len; // add the body-length to the
											// header-length
			body = false;
		}

		return (statuscode == JHTTPPSession.SC_OK) ? header_length : -1; // return
																				// -1
																				// with
																				// an
																				// error
	}

	/**
	 * reads a line
	 * 
	 * @exception IOException
	 */
	public String getLine() throws IOException {
		int l = 0;
		StringBuilder line = new StringBuilder();
		lread = 0;
		while (l != '\n')
		{
			l = read();
			if (l != -1)
			{
				line.append((char) l);
				lread++;
			}
			else
				break;
		}
		return line.toString();
	}

	/**
	 * Parser for the first (!) line from the HTTP request<BR>
	 * Sets up the URL, method and remote hostname.
	 * 
	 * @return an InetAddress for the hostname, null on errors with a
	 *         statuscode!=SC_OK
	 */
	public InetAddress parseRequest(String a, int method_index) {
		if (server.isDebugEnabled())
			log.info(a);
		String f;
		int pos;
		url = "";
		if (ssl) {
			f = a.substring(8);
		} else {
			method = a.substring(0, a.indexOf(" ")); // first word in the
														// line
			pos = a.indexOf(":"); // locate first :
			if (pos == -1) { // occurs with "GET / HTTP/1.1"
				url = a.substring(a.indexOf(" ") + 1, a.lastIndexOf(" "));
				if (method_index == 0) { // method_index==0 --> GET
					if (url.indexOf(server.WEB_CONFIG_FILE) != -1)
						statuscode = JHTTPPSession.SC_CONFIG_RQ;
					else
						statuscode = JHTTPPSession.SC_FILE_REQUEST;
				} else {
					if (method_index == 1
							&& url.indexOf(server.WEB_CONFIG_FILE) != -1) { // allow
																			// "POST"
																			// for
																			// admin
																			// log
																			// in
						statuscode = JHTTPPSession.SC_CONFIG_RQ;
					} else {
						statuscode = JHTTPPSession.SC_INTERNAL_SERVER_ERROR;
						errordescription = "This WWW proxy supports only the \"GET\" method while acting as webserver.";
					}
				}
				return null;
			}
			f = a.substring(pos + 3); // removes "http://"
		}
		pos = f.indexOf(" "); // locate space, should be the space before
								// "HTTP/1.1"
		if (pos == -1) { // buggy request
			statuscode = JHTTPPSession.SC_CLIENT_ERROR;
			errordescription = "Your browser sent an invalid request: \"" + a
					+ "\"";
			return null;
		}
		f = f.substring(0, pos); // removes all after space
		// if the url contains a space... it's not our mistake...(url's must
		// never contain a space character)
		pos = f.indexOf("/"); // locate the first slash
		if (pos != -1) {
			url = f.substring(pos); // saves path without hostname
			f = f.substring(0, pos); // reduce string to the hostname
		} else
			url = "/"; // occurs with this request: "GET http://localhost
						// HTTP/1.1"
		pos = f.indexOf(":"); // check for the portnumber
		if (pos != -1) {
			String l_port = f.substring(pos + 1);
			l_port = l_port.indexOf(" ") != -1 ? l_port.substring(0, l_port
					.indexOf(" ")) : l_port;
			int i_port = 80;
			try {
				i_port = Integer.parseInt(l_port);
			} catch (NumberFormatException e_get_host) {
				log.info("get_Host :" + e_get_host + " !!!!");
			}
			f = f.substring(0, pos);
			remote_port = i_port;
		} else
			remote_port = 80;
		remote_host_name = f;
		InetAddress address = null;
		if (server.isDebugEnabled())
		log.info(connection.getLocalSocket().getInetAddress()
					.getHostAddress()
					+ " " + method + " " + getFullURL());
		try {
			address = InetAddress.getByName(f);
			if (remote_port == server.getProxyPort()
					&& address.equals(InetAddress.getLocalHost())) {
				if (url.indexOf(server.WEB_CONFIG_FILE) != -1
						&& (method_index == 0 || method_index == 1))
					statuscode = JHTTPPSession.SC_CONFIG_RQ;
				else if (method_index > 0) {
					statuscode = JHTTPPSession.SC_INTERNAL_SERVER_ERROR;
					errordescription = "This WWW proxy supports only the \"GET\" method while acting as webserver.";
				} else
					statuscode = JHTTPPSession.SC_FILE_REQUEST;
			}
		} catch (UnknownHostException e_u_host) {
			if (!server.use_proxy)
				statuscode = JHTTPPSession.SC_HOST_NOT_FOUND;
		}
		return address;
	}

	/**
	 * @return boolean whether the actual connection was established with the
	 *         CONNECT method.
	 * @since 0.2.21
	 */
	public boolean isTunnel() {
		return ssl;
	}

	/**
	 * @return the full qualified URL of the actual request.
	 * @since 0.4.0
	 */
	public String getFullURL() {
		return "http" + (ssl ? "s" : "") + "://" + getRemoteHostName()
				+ (remote_port != 80 ? (":" + remote_port) : "") + url;
	}

	/**
	 * @return status-code for the actual request
	 * @since 0.3.5
	 */
	public int getStatusCode() {
		return statuscode;
	}

	/**
	 * @return the (optional) error-description for this request
	 */
	public String getErrorDescription() {
		return errordescription;
	}
}
