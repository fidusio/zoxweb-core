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
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.util.Const;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class JHTTPPSession 
    implements Runnable
{

	private static final Logger log = Logger.getLogger(Const.LOGGER_NAME);

	public static final int SC_OK=0;
	public static final int SC_CONNECTING_TO_HOST=1;
	public static final int SC_HOST_NOT_FOUND=2;
	public static final int SC_URL_BLOCKED=3;
	public static final int SC_CLIENT_ERROR=4;
	public static final int SC_INTERNAL_SERVER_ERROR=5;
	public static final int SC_NOT_SUPPORTED=6;
	public static final int SC_REMOTE_DEBUG_MODE=7;
	public static final int SC_CONNECTION_CLOSED=8;
	public static final int SC_HTTP_OPTIONS_THIS=9;
	public static final int SC_FILE_REQUEST=10;
	public static final int SC_MOVED_PERMANENTLY=11;
	public static final int SC_CONFIG_RQ = 12;
	public static final AtomicLong TOTAL_COUNTER = new AtomicLong();
	

	private  JHTTPPServer server;

	/** downstream connections */
	private Socket client_socket;
	private BufferedOutputStream client_out;
	private JHTTPPClientInputStream client_in;

	/** upstream connections */
	private Socket remote_socket;
	private BufferedOutputStream remote_out;
	//private Jhttpp2ServerInputStream HTTP_in;
	private BufferedInputStream remote_in;
	private long conCounter;

	public JHTTPPSession(JHTTPPServer server, Socket client)
	{
		try 
		{
			conCounter = TOTAL_COUNTER.incrementAndGet();
			client_in = new JHTTPPClientInputStream(server,this,client.getInputStream());//,true);
			client_out = new BufferedOutputStream(client.getOutputStream());
			this.server=server;
			this.client_socket=client;
			
			if ( server.isDebugEnabled())
				log.info( "Incoming Connetion from " +client.getRemoteSocketAddress());
		}
		catch (IOException e_io) 
		{
			IOUtil.close(client);				
			log.info("Error while creating IO-Streams: " + e_io);
			return;
		}
		//server.writeLog( "Incoming Connetion from " +client.getRemoteSocketAddress());
		new Thread(this).start();
	}
	
	public Socket getLocalSocket() 
	{
		return client_socket;
	}
	
	public Socket getRemoteSocket() 
	{
		return remote_socket;
	}
	
	public boolean isTunnel() 
	{
		return client_in.isTunnel();
	}
	
	public boolean notConnected() 
	{
		return getRemoteSocket()==null;
	}
	
//	public void sendHeader(int a,boolean b)throws IOException 
//	{
//		sendHeader(a);
//		endHeader();
//		client_out.flush();
//	}
	
	public void sendHeader(int status, String content_type, long content_length) 
		throws IOException 
	{
		sendHeader(status);
		sendLine("Content-Length", String.valueOf(content_length));
		sendLine("Content-Type", content_type );
	}
	public void sendLine(String s) 
		throws IOException 
	{
		write(client_out,s + "\r\n");
	}
	
	public void sendLine(String header, String s) throws IOException 
	{
		write(client_out,header + ": " + s + "\r\n");
	}
	
	public void endHeader() throws IOException 
	{
		write(client_out,"\r\n");
	}
	public void run() 
	{
		
		if (server.isDebugEnabled())
			log.info("begin http session");
		
		server.increaseNumConnections();
		
		try 
		{
			handleRequest();
		}
		catch (IOException e_handleRequest) {
			if (server.isDebugEnabled()) System.out.println(e_handleRequest.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			log.info("Jhttpp2HTTPSession.run(); " + e.getMessage());
		}
		
		// close downstream connections
		IOUtil.close(client_in);
		
		IOUtil.close(client_out);
		IOUtil.close(client_socket);
		
		// close upstream connections (webserver or other proxy)
		
		IOUtil.close(remote_in);
		
		IOUtil.close(remote_out);
		IOUtil.close(getRemoteSocket());
			
		
		
		server.decreaseNumConnections();
		if (server.isDebugEnabled())
			log.info("end http session");
	}
	/** 
	 * sends a message to the user 
	 * @param a 
	 * @param info 
	 * @throws IOException 
	 */
	public void sendErrorMSG(int a,String info)throws IOException {
		String statuscode = sendHeader(a);
		String localhost = "localhost";
		try {
			localhost = InetAddress.getLocalHost().getHostName() + ":" + server.getProxyPort();
		}
		catch(UnknownHostException e_unknown_host ) {}
		String msg = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html>\r"
		+ "<!-- jHTTPp2 error message --><HEAD>\r"
		+ "<TITLE>" + statuscode + "</TITLE>\r"
		+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://" + localhost + "/style.css\"></HEAD>\r"  // use css style sheet in htdocs
		+ "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\" LINK=\"#000080\" VLINK=\"#000080\" ALINK=\"#000080\">\r"
		+ "<h2 class=\"headline\">HTTP " + statuscode + " </h2>\r"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i30\">Your request for the following URL failed:</p>"
		+ "<p class=\"tiagtext\"><a href=\"" + client_in.getFullURL() + "\">" + client_in.getFullURL() + "</A> </p>\r"
		+ "<P class=\"i25\">Reason: " + info + "</P>"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i25\"><A HREF=\"http://www.zoxweb.com/\">jHTTPp2</A> HTTP Proxy, Version " + server.getServerVersion() + " at " + localhost
		+"</p>\r"
		//+"<p class=\"i25\"><A HREF=\"http://" + localhost + "/\">jHTTPp2 local website</A> <A HREF=\"http://" + localhost + "/" + server.WEB_CONFIG_FILE + "\">Configuration</A></p>"
		+ "</BODY></HTML>";
		sendLine("Content-Length",String.valueOf(msg.length()));
		sendLine("Content-Type","text/html; charset=iso-8859-1");
		endHeader();
		write(client_out,msg);
		client_out.flush();
	}

	public String sendHeader(int a)throws IOException	{
		String stat;
		
		switch(a) {
			case 200:stat="200 OK"; break;
			case 202:stat="202 Accepted"; break;
			case 300:stat="300 Ambiguous"; break;
			case 301:stat="301 Moved Permanently"; break;
			case 400:stat="400 Bad Request"; break;
			case 401:stat="401 Denied"; break;
			case 403:stat="403 Forbidden"; break;
			case 404:stat="404 Not Found"; break;
			case 405:stat="405 Bad Method"; break;
			case 413:stat="413 Request Entity Too Large"; break;
			case 415:stat="415 Unsupported Media"; break;
			case 501:stat="501 Not Implemented"; break;
			case 502:stat="502 Bad Gateway"; break;
			case 504:stat="504 Gateway Timeout"; break;
			case 505:stat="505 HTTP Version Not Supported"; break;
			default: stat="500 Internal Server Error";
		}
		sendLine(server.getHttpVersion() + " " + stat);
		sendLine("Server",server.getServerIdentification());
		if (a==501) sendLine("Allow","GET, HEAD, POST, PUT, DELETE, CONNECT");
		sendLine("Cache-Control", "no-cache, must-revalidate");
		sendLine("Connection","close");
		return stat;
	}

  /**
   *  the main routine, where it all happens 
   * 
   * @throws Exception 
   */
	public void handleRequest() throws Exception {
		InetAddress remote_host;
		JHTTPPRead remote_read=null;
		int remote_port;
		UByteArrayOutputStream baos = new UByteArrayOutputStream(4096);
		
		
		
		
		// read the client request full request
		int numread = client_in.read(baos);
		//log.info("numread:" + numread + "," + conCounter);
	

		while(!server.isClosed()) 
		{ // with this loop we support persistent connections
			if (numread==-1) 
			{ // -1 signals an error
				if (client_in.getStatusCode()!=SC_CONNECTING_TO_HOST) 
				{
					switch (client_in.getStatusCode()) 
					{
						case SC_CONNECTION_CLOSED: break;
						case SC_CLIENT_ERROR: sendErrorMSG(400,"Your client sent a request that this proxy could not understand. (" + client_in.getErrorDescription() + ")"); break;
						case SC_HOST_NOT_FOUND: sendErrorMSG(504,"Host not found.<BR>jHTTPp2 was unable to resolve the hostname of this request. <BR>Perhaps the hostname was misspelled, the server is down or you have no connection to the internet."); break;
						case SC_INTERNAL_SERVER_ERROR: sendErrorMSG(500,"Server Error! (" + client_in.getErrorDescription() + ")"); break;
						case SC_NOT_SUPPORTED: sendErrorMSG(501,"Your client used a HTTP method that this proxy doesn't support: (" + client_in.getErrorDescription() + ")"); break;
						case SC_URL_BLOCKED: sendErrorMSG(403,(client_in.getErrorDescription()!=null && client_in.getErrorDescription().length()>0?client_in.getErrorDescription():"The request for this URL was denied by the jHTTPp2 URL-Filter.")); break;
						//case SC_REMOTE_DEBUG_MODE: remoteDebug(); break;
						case SC_HTTP_OPTIONS_THIS: sendHeader(200); endHeader(); break;
						case SC_FILE_REQUEST: file_handler(); break;
						case SC_CONFIG_RQ: admin_handler(baos.getInternalBuffer()); break;
						//case SC_HTTP_TRACE:
						case SC_MOVED_PERMANENTLY:
							sendHeader(301);
							write(client_out,"Location: " + client_in.getErrorDescription() + "\r\n");
							endHeader();
							client_out.flush();
						default:
					}
					break; // return from main loop.
				}
				else 
				{ // also an error because we are not connected (or to the wrong host)
					// Creates a new connection to a remote host.
					if (!notConnected()) 
					{
						try 
						{
							if (server.isDebugEnabled())
								log.info(conCounter +":notConnected");
							getRemoteSocket().close();
						}
						catch (IOException e_close_socket) {}
					}
					numread=client_in.getHeaderLength(); // get the header length
					if (!server.use_proxy) {// sets up hostname and port
						remote_host=client_in.getRemoteHost();
						remote_port=client_in.remote_port;
					}
					else {
						remote_host=server.proxy;
						remote_port=server.getProxyPort();
					}
					
					if ( server.isDebugEnabled())
						log.info("Connect: " + remote_host + ":" + remote_port);
					try 
					{
						// the actual connection to the remote server
						connect(remote_host,remote_port);
					}
					catch (IOException e_connect) 
					{
						if (server.isDebugEnabled()) log.info(e_connect.toString());
						sendErrorMSG(502,"Error while creating a TCP connecting to [" +remote_host.getHostName()+ ":" + remote_port + "] <BR>The proxy server cannot connect to the given address or port [" + e_connect.toString() + "]");
						break;
					}
					catch (Exception e) 
					{
						//log.info(e.toString());
						sendErrorMSG(500,"Error: " + e.toString());
						break;
					}
					if (!client_in.isTunnel()  || (client_in.isTunnel() && server.use_proxy))
					{ // no SSL-Tunnel or SSL-Tunnel with another remote proxy: simply forward the request
						//sendLine("Content-Encoding", "gzip");
						
						//log.info(conCounter +":request to be written:\n" + new String(baos.getInternalBuffer(), 0, numread));
						//log.info(conCounter + ":SENDING REQUEST");
						remote_out.write(baos.getInternalBuffer(), 0, numread);
						remote_out.flush();
						
					}
					else
					{ //  SSL-Tunnel with "CONNECT": creates a tunnel connection with the server
						sendLine(server.getHttpVersion() + " 200 Connection established");
						sendLine("Proxy-Agent",server.getServerIdentification());
						endHeader(); client_out.flush();
					}
					// reads data from the remote server
					
					
					remote_read = new JHTTPPRead(server,this, remote_in, client_out); // reads data from the remote server
					//log.info(conCounter +":JHTTPPRead");
					server.addBytesWritten(numread);
				}
			}
			while(!server.isClosed()) { // reads data from the client
				baos.reset();
				numread=client_in.read(baos);
			
				//b = bh.bah;
				//if (server.debug)server.writeLog("Jhttpp2HTTPSession: " + numread + " Bytes read.");
				if (numread!=-1) 
				{
					//log.info(conCounter +":write");
					remote_out.write(baos.getInternalBuffer(), 0, numread);
					remote_out.flush();
					server.addBytesWritten(numread);
				} else break;
			} // end of inner loop
		}// end of main loop
		client_out.flush();
		if (!notConnected() && remote_read != null)
			remote_read.close(); // close Jhttpp2Read thread
		return;
	}
  /** connects to the given host and port 
 * @param host 
 * @param port 
 * @throws IOException */
  public void connect(InetAddress host,int port)
  	throws IOException 
  {
      remote_socket = new Socket(host,port);
      //HTTP_in = new Jhttpp2ServerInputStream(server,this,HTTP_Socket.getInputStream(),false);
      remote_in = new BufferedInputStream(getRemoteSocket().getInputStream());
      remote_out = new BufferedOutputStream(getRemoteSocket().getOutputStream());
  }
  /** converts an String into a Byte-Array to write it with the OutputStream 
 * @param o 
 * @param p 
 * @throws IOException */
  public void write(BufferedOutputStream o,String p)
  	throws IOException 
  {
    o.write(p.getBytes(),0,p.length());
  }

  /**
   * Small webserver for local files in {app}/htdocs
 * @throws IOException 
   * @since 0.4.04
   */
  public void file_handler() throws IOException 
  {
	if (!server.www_server) 
	{
		sendErrorMSG(500, "The jHTTPp2 built-in WWW server module is disabled.");
		return;
	}
    String filename=client_in.url;
    if (filename.equals("/")) filename="index.html"; // convert / to index.html
    else if (filename.startsWith("/")) filename=filename.substring(1);
    if (filename.endsWith("/")) filename+="index.html"; // add index.html, if ending with /
    File file = new File("htdocs/" + filename); // access only files in "htdocs"
    if ( !file.exists() || !file.canRead() // be sure that we can read the file
		|| filename.indexOf("..")!=-1 // don't allow ".." !!!
		|| file.isDirectory() ) { // dont't read if it's a directory
      sendErrorMSG(404,"The requested file /" + filename + " was not found or the path is invalid.");
      return;
    }
    int pos = filename.lastIndexOf("."); // MIME type of the specified file
    String content_type="text/plain"; // all unknown content types will be marked as text/plain
    if (pos != -1) {
    	String extension = filename.substring(pos+1);
    	if (extension.equalsIgnoreCase("htm") || (extension.equalsIgnoreCase("html"))) content_type="text/html; charset=iso-8859-1";
    	else if (extension.equalsIgnoreCase("jpg") || (extension.equalsIgnoreCase("jpeg"))) content_type="image/jpeg";
    	else if (extension.equalsIgnoreCase("gif")) content_type = "image/gif";
    	else if (extension.equalsIgnoreCase("png")) content_type = "image/png";
    	else if (extension.equalsIgnoreCase("css")) content_type = "text/css";
    	else if (extension.equalsIgnoreCase("pdf")) content_type = "application/pdf";
    	else if (extension.equalsIgnoreCase("ps") || extension.equalsIgnoreCase("eps")) content_type = "application/postscript";
    	else if (extension.equalsIgnoreCase("xml")) content_type = "text/xml";
	}
    sendHeader(200,content_type, file.length() );
    sendLine("Content-Encoding", "gzip");
    endHeader();
    BufferedInputStream file_in = new BufferedInputStream(new FileInputStream(file));
    byte[] buffer=new byte[4096];
    int a=file_in.read(buffer);
    GZIPOutputStream zos = new GZIPOutputStream( client_out);
    System.out.println("Will send data gzipped");
    while (a!=-1) { // read until EOF
      zos.write(buffer,0,a);
      a = file_in.read(buffer);
    }
    zos.finish();
    zos.flush();
    file_in.close(); // finished!
  }
  /**
   * @return  status
 * @since 0.4.10b
   */
  public int getStatus()
  {
    return client_in.getStatusCode();
  }
  /**
     * @param b 
 * @throws IOException 
 * @since 0.4.20a
     * admin webpage
   */
  public void admin_handler(byte[] b) throws IOException
  {
	  if (!server.webconfig) {
		  sendErrorMSG(500,"The web-based configuration module is disabled.");
		  return;
	  }
	  JHTTPPAdmin admin = null;
	  String filename = client_in.url;
	  if (client_in.post_data_len > 0) { // if the client used "POST" then append the data to the filename
		  filename = filename + "?" + new String(b,client_in.getHeaderLength()-client_in.post_data_len,client_in.post_data_len);
	  }
	  if (filename.startsWith("/")) filename = filename.substring(1);
      String adminpage = "";
      try {
        admin = new JHTTPPAdmin( filename, server ) ;
		admin.WebAdmin();
		adminpage = admin.HTMLAdmin();
      }
      catch( Exception e) {
		e.printStackTrace();
		//log.info("Jhttpp2Admin Exception: " + e.getMessage());
      }
    	int adminlen = adminpage.length();
    	if ( adminlen < 1 ) {
    	    sendErrorMSG(500,"Error Message from the Web-Admin modul: " + JHTTPPAdmin.error_msg);
    	}
    	else {
    	  	sendHeader(200,"text/html",adminlen);
    	    endHeader();
    	    write(client_out,adminpage);
    	}
    	client_out.flush();
	}
}

