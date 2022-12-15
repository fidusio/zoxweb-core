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
import org.zoxweb.server.net.InetFilterRulesManager;
import org.zoxweb.server.util.ApplicationConfigManager;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.DaemonController;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

public class JHTTPPServer 
    implements Runnable, DaemonController
{

	private static final Logger log = Logger.getLogger(Const.LOGGER_NAME);
	private final String VERSION = "2.0";

	private final String V_SPECIAL = " 2016-3-15";

	private final String HTTP_VERSION = "HTTP/1.1";

	private final String MAIN_LOGFILE = "proxy.log";

	private String DATA_FILE = "proxy.data";

	private String SERVER_PROPERTIES_FILE = "proxy.properties";

	private String http_useragent = "Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)";

	private ServerSocket listen;

	//private BufferedWriter logfile;

	//private BufferedWriter access_logfile;

	private Properties serverproperties = null;

	private long bytesread;

	private long byteswritten;

	private int numconnections;

	private boolean enable_cookies_by_default = true;

	private WildcardDictionary dic = new WildcardDictionary();

	private Vector<Object> urlactions = new Vector<Object>();

	public final int DEFAULT_SERVER_PORT = 8080;

	public final String WEB_CONFIG_FILE = "admin/jp2-config";

	//public int port = DEFAULT_SERVER_PORT;

	public InetAddress proxy;

	private int proxy_port = DEFAULT_SERVER_PORT;

	public long config_auth = 0;

	public long config_session_id = 0;

	public String config_user = "root";

	public String config_password = "geheim";

	public static boolean error;

	public static String error_msg;

	public boolean use_proxy = false;

	public static boolean block_urls = false;

	public boolean filter_http = false;

	private boolean debugEnabled = true;

	public boolean log_access = true;

	public String log_access_filename = "paccess.log";
	
	//private  String defaultLogDir = null;

	public boolean webconfig = true;
	private boolean live = true;

	public boolean www_server = true;
	private InetFilterRulesManager ipfm = null;


	private void init() 
	{
		try 
		{
			//String fileName = MAIN_LOGFILE;
			ApplicationConfigManager.SINGLETON.loadDefault();
			
			//defaultLogDir = ApplicationConfigManager.SINGLETON.concatAsDirName(acd, "var/logs");
		
//			if ( new File(defaultLogDir).exists())
//			{
//				fileName = defaultLogDir + MAIN_LOGFILE;
//			}
			DATA_FILE = ApplicationConfigManager.SINGLETON.concatWithEnvVar("data/"+DATA_FILE);
			SERVER_PROPERTIES_FILE = ApplicationConfigManager.SINGLETON.concatWithEnvVar("conf/"+SERVER_PROPERTIES_FILE);
			log_access_filename = ApplicationConfigManager.SINGLETON.concatWithEnvVar("var/logs/"+log_access_filename);
			///logfile = new BufferedWriter(new FileWriter(fileName, true));
		} catch (Exception e_logfile) {
			log.info("Unable to open the main log file.");
			
			log.info("jHTTPp2 need write permission for the file "
						+ MAIN_LOGFILE);
			error_msg += " " + e_logfile.getMessage();
		}
		log.info("server startup...");

		try {
			restoreSettings();
		} catch (Exception e_load) {
			setErrorMsg("Error while resoring settings: " + e_load.getMessage());
		}
		try {
			listen = new ServerSocket(getProxyPort(),50);
		} catch (BindException e_bind_socket) {
			setErrorMsg("Socket " + getProxyPort()
					+ " is already in use (Another jHTTPp2 proxy running?) "
					+ e_bind_socket.getMessage());
		} catch (IOException e_io_socket) {
			setErrorMsg("IO Exception while creating server socket on port "
					+ getProxyPort() + ". " + e_io_socket.getMessage());
		}

		if (error) {
			log.info(error_msg);
			return;
		}
		
//		try 
//		{
//			ipfm = IPFilterManager.load(SharedUtil.trimOrEmpty(System.getenv("OPT_BASE"))+"/var/proxy/proxy_ip_filter.xml");
//		} 
//		catch (Exception e) 
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		{
			// start the server here
			new Thread(this).start();
			log.info("Running on port " + getProxyPort());
		}
		// if (debug) remote_debug_vector=new Vector();
		// remote_debug=false;
	}

	public JHTTPPServer() 
	{
		this(0, null);
	}

	public JHTTPPServer(int port, InetFilterRulesManager ipfm) 
	{
		this.ipfm = ipfm;
		if ( port > 0)
		{
			proxy_port = port;
		}
			
		log.info("jHTTPp2 HTTP Proxy Server Release "
				+ getServerVersion()
				+ "\r\n"
				+ "Copyright (c) 2008-2014 support <support@zoxweb.com>\r\n"
				+ "This software comes with ABSOLUTELY NO WARRANTY OF ANY KIND.\r\n"
				+ "http://www.zoxweb.com/");
		
		init();
	}

	/**
	 * calls init(), sets up the serverport and starts for each connection new
	 * Jhttpp2Connection
	 */
//	void serve() 
//	{
//		log.info("Server running.");
//		try 
//		{
//			while (true) 
//			{
//				Socket client = listen.accept();
//				if ( ipfm != null)
//				{
//					String clientHostAddress = client.getInetAddress().getHostAddress();
//					SecurityStatus ft = ipfm.checkIP(clientHostAddress);
//					if ( ft == SecurityStatus.DENY)
//					{
//						client.close();
//						if (debug)
//							log.info("ACCESS " + ft + " for ip:" + clientHostAddress);
//						continue;
//					}
//				}
//				new Jhttpp2HTTPSession(this, client);
//			}
//		} 
//		catch (Exception e) 
//		{
//			e.printStackTrace();
//			log.info("Exception in Jhttpp2Server.serve(): " + e.toString());
//		}
//	}

	public void run() 
	{		
		log.info("Proxy started on port:"+getProxyPort());
		try 
		{
			while (live) 
			{
				Socket client = listen.accept();
				InetAddress clientHostAddress  = null;
				try
				{
					
					if (ipfm != null)
					{
						clientHostAddress = client.getInetAddress();
						SecurityStatus ft = ipfm.lookupSecurityStatus(clientHostAddress);
						if ( ft == SecurityStatus.DENY)
						{
							IOUtil.close(client);
							log.info("ACCESS " + ft + " for ip:" + clientHostAddress.getHostAddress());
							continue;
						}
					}
					if (live)
						new JHTTPPSession(this, client);
					else
						IOUtil.close(client);
						
				}
				catch(Exception e)
				{
					IOUtil.close(client);
					e.printStackTrace();
					log.info("Failed to serve :" + clientHostAddress);
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			log.info("Exception in Jhttpp2Server.run(): " + e.toString());
		}
	}

	public void setErrorMsg(String a) {
		error = true;
		error_msg = a;
	}

	/**
	 * Tests what method is used with the reqest
	 * @param d 
	 * 
	 * @return -1 if the server doesn't support the method
	 */
	public int getHttpMethod(String d) {
		if (SharedStringUtil.contains(d, HTTPMethod.GET.name()) || SharedStringUtil.contains(d, HTTPMethod.HEAD.name()))
			return 0;
		if (SharedStringUtil.contains(d, HTTPMethod.POST.name()) || SharedStringUtil.contains(d, HTTPMethod.PUT.name()) || SharedStringUtil.contains(d, HTTPMethod.PATCH.name()))
			return 1;
		if (SharedStringUtil.contains(d, HTTPMethod.CONNECT.name()))
			return 2;
		if (SharedStringUtil.contains(d, HTTPMethod.OPTIONS.name()))
			return 3;

		return -1;/*
					 * No match...
					 * 
					 * Following methods are not implemented: ||
					 * startsWith(d,"TRACE")
					 */
	}

//	public boolean startsWith(String a, String what) 
//	{
//		return SharedStringUtil.contains(a, what, true);
//		
//		int l = what.length();
//		int l2 = a.length();
//		return l2 >= l ? a.substring(0, l).equalsIgnoreCase(what) : false;
//	}

	/**
	 * @return the Server response-header field
	 */
	public String getServerIdentification() {
		return "jHTTPp2/" + getServerVersion();
	}

	public String getServerVersion() {
		return VERSION + V_SPECIAL;
	}

	/**
	 * saves all settings with a ObjectOutputStream into a file
	 * @throws IOException 
	 * 
	 * @since 0.2.10
	 */
	public void saveSettings() throws IOException {
		serverproperties.setProperty("server.http-proxy",
				Boolean.toString(use_proxy).toString());
		serverproperties.setProperty("server.http-proxy.hostname", proxy
				.getHostAddress());
		serverproperties.setProperty("server.http-proxy.port", Integer.toString(
				proxy_port).toString());
		serverproperties.setProperty("server.filter.http", Boolean.toString(
				filter_http).toString());
		serverproperties.setProperty("server.filter.url", Boolean.toString(
				block_urls).toString());
		serverproperties.setProperty("server.filter.http.useragent",
				http_useragent);
		serverproperties.setProperty("server.enable-cookies-by-default",
				 Boolean.toString(enable_cookies_by_default).toString());
		serverproperties.setProperty("server.debug-logging", Boolean.toString(isDebugEnabled())
				.toString());
		serverproperties.setProperty("server.port", Integer.toString(getProxyPort())
				.toString());
		serverproperties.setProperty("server.access.log", Boolean.toString(
				log_access).toString());
		serverproperties.setProperty("server.access.log.filename",
				log_access_filename);
		serverproperties.setProperty("server.webconfig", Boolean.toString(webconfig)
				.toString());
		serverproperties.setProperty("server.www", Boolean.toString(www_server)
				.toString());
		serverproperties.setProperty("server.webconfig.username", config_user);
		serverproperties.setProperty("server.webconfig.password",
				config_password);
		storeServerProperties();

		ObjectOutputStream file = new ObjectOutputStream(new FileOutputStream(
				DATA_FILE));
		file.writeObject(dic);
		file.writeObject(urlactions);
		file.close();
	}

	/**
	 * restores all Jhttpp2 options from "settings.dat"
	 * 
	 * @since 0.2.10
	 */
	@SuppressWarnings("unchecked")
	public void restoreSettings()// throws Exception
	{
		getServerProperties();
		use_proxy = Boolean.valueOf(serverproperties.getProperty(
				"server.http-proxy", "false")).booleanValue();
		try {
			proxy = InetAddress.getByName(serverproperties.getProperty(
					"server.http-proxy.hostname", "127.0.0.1"));
		} catch (UnknownHostException e) {
		}
//		proxy_port = new Integer(serverproperties.getProperty(
//				"server.http-proxy.port", ""+getProxyPort())).intValue();
		block_urls = Boolean.valueOf(serverproperties.getProperty(
				"server.filter.url", "false")).booleanValue();
		http_useragent = serverproperties.getProperty(
				"server.filter.http.useragent",
				"Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
		filter_http = Boolean.valueOf(serverproperties.getProperty(
				"server.filter.http", "false")).booleanValue();
		enable_cookies_by_default = Boolean.valueOf(serverproperties.getProperty(
				"server.enable-cookies-by-default", "true")).booleanValue();
		setDebugEnabled(Boolean.valueOf(serverproperties.getProperty(
				"server.debug-logging", "false")).booleanValue());
//		port = new Integer(serverproperties.getProperty("server.port", "8080"))
//				.intValue();
		log_access = Boolean.valueOf(serverproperties.getProperty(
				"server.access.log", "true")).booleanValue();
		log_access_filename = serverproperties.getProperty(
				"server.access.log.filename", "paccess.log");
		webconfig = Boolean.valueOf(serverproperties.getProperty(
				"server.webconfig", "true")).booleanValue();
		www_server = Boolean.valueOf(serverproperties.getProperty("server.www",
				"true")).booleanValue();
		config_user = serverproperties.getProperty("server.webconfig.username",
				"root");
		config_password = serverproperties.getProperty(
				"server.webconfig.password", "geheim");

		try {
			
//			String fileName = log_access_filename;
//			if ( new File(defaultLogDir).exists())
//			{
//				fileName = defaultLogDir +log_access_filename;
//			}
			
//			access_logfile = new BufferedWriter(new FileWriter(
//					fileName, true));
			// Restore the WildcardDioctionary and the URLActions with the
			// ObjectInputStream (settings.dat)...
			ObjectInputStream obj_in;
			File file = new File(DATA_FILE);
			if (!file.exists()) {
				if (!file.createNewFile() || !file.canWrite()) {
					setErrorMsg("Can't create or write to file "
							+ file.toString());
				} else
					saveSettings();
			}

			obj_in = new ObjectInputStream(new FileInputStream(file));
			dic = (WildcardDictionary) obj_in.readObject();
			urlactions = (Vector<Object>)obj_in.readObject();
			obj_in.close();
		} catch (IOException e) {
			setErrorMsg("restoreSettings(): " + e.getMessage());
		} catch (ClassNotFoundException e_class_not_found) {
		}
	}

	/**
	 * @return the HTTP version used by jHTTPp2
	 */
	public String getHttpVersion() {
		return HTTP_VERSION;
	}

	/**
	 * the User-Agent header field
	 * 
	 * @since 0.2.17
	 * @return User-Agent String
	 */
	public String getUserAgent() {
		return http_useragent;
	}

	public void setUserAgent(String ua) {
		http_useragent = ua;
	}

	/**
	 * writes into the server log file and adds a new line
	 * 
	 * @since 0.2.21
	 */
//	public void writeLog(String s) {
//		writeLog(s, true);
//	}

	
//	public void writeLog(String s, boolean b) 
//	{
//		if ( b && logfile != null)
//		{
//			try 
//			{
//				s = new Date().toString() + " " + s;
//				logfile.write(s, 0, s.length());
//				if (b)
//					logfile.newLine();
//				logfile.flush();
//				if (debug)
//					System.out.println(s);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	public void closeLog()
//	{
//		if ( logfile != null)
//		{
//			try {
//				writeLog("Server shutdown.");
//				logfile.flush();
//				logfile.close();
//				access_logfile.close();
//			} catch (Exception e) {
//			}
//		}
//	}

	/**
	 * writes to the server log file
	 * @param read 
	 * 
	 * @since 0.2.21
	 */
	public void addBytesRead(long read) {
		bytesread += read;
	}

	/**
	 * Functions for the jHTTPp2 statistics: How many connections Bytes
	 * read/written
	 * @param written 
	 * 
	 * @since 0.3.0
	 */
	public void addBytesWritten(int written) {
		byteswritten += written;
	}

	public int getServerConnections() {
		return numconnections;
	}

	public long getBytesRead() {
		return bytesread;
	}

	public long getBytesWritten() {
		return byteswritten;
	}

	public synchronized void increaseNumConnections() {
		numconnections++;
	}

	public synchronized void decreaseNumConnections() {
		numconnections--;
	}

	public void AuthenticateUser(String u, String p) {
		if (config_user.equals(u) && config_password.equals(p)) {
			config_auth = 1;
		} else
			config_auth = 0;
	}

//	public String getGMTString() {
//		return new Date().toString();
//	}

	public JHTTPPURLMatch findMatch(String url) {
		return (JHTTPPURLMatch) dic.get(url);
	}

	public WildcardDictionary getWildcardDictionary() {
		return dic;
	}

	public Vector<Object> getURLActions() {
		return urlactions;
	}

	public boolean enableCookiesByDefault() {
		return this.enable_cookies_by_default;
	}

	public void enableCookiesByDefault(boolean a) {
		enable_cookies_by_default = a;
	}

	public void resetStat() {
		bytesread = 0;
		byteswritten = 0;
	}

	/**
	 * @return Properties
	 * @since 0.4.10a
	 */
	public Properties getServerProperties() {
		if (serverproperties == null) {
			serverproperties = new Properties();
			try {
				serverproperties.load(new DataInputStream(new FileInputStream(
						SERVER_PROPERTIES_FILE)));
			} catch (IOException e) {
				log.info("getServerProperties(): " + e.getMessage());
			}
		}
		return serverproperties;
	}

	/**
	 * @since 0.4.10a
	 */
	public void storeServerProperties() {
		if (serverproperties == null)
			return;
		try {
			serverproperties
					.store(
							new FileOutputStream(SERVER_PROPERTIES_FILE),
							"Jhttpp2Server main properties. Look at the README file for further documentation.");
		} catch (IOException e) {
			log.info("storeServerProperties(): " + e.getMessage());
		}
	}

	

	/**
	 * Shutdown the proxy object
	 */
	public synchronized void close() 
	{
		// TODO Auto-generated method stub
		if ( live)
		{
			live = false;
			IOUtil.close( listen);
		}
		
	}

	/**
	 * @see org.zoxweb.shared.util.DaemonController#isClosed()
	 */
	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return !live;
	}
	
	
	public InetFilterRulesManager getInetFilterRulesManager()
	{
		return ipfm;
	}
	
	public int getProxyPort()
	{
		return proxy_port;
	}
	
	public void setProxyPort( int port)
	{
		proxy_port = port;
	}

	public boolean isDebugEnabled() 
	{
		return debugEnabled;
	}

	public void setDebugEnabled(boolean debugEnabled)
	{
		this.debugEnabled = debugEnabled;
	}

	
	
}
