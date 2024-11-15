package org.zoxweb.server.net;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * This a very simple and basic Datagram relay single threaded definitely not performant must be upgraded
 * how to use it
 * <code>
 * DatagramRelay dr = new DatagramRelay(53, "8.8.8.8", 53, 512);
 * // or
 * DatagramRelay dr = DatagramRelay.ConfigParam.toDatagramRelay(jsonConfig);
 * //create a local dns relay to the google dns
 * new Thread(dr).start();
 * </code>
 * json config file format example for a dns relay 
 * <code>
 * {
	"port" : 53,
	"remote_host" : "8.8.8.8",
	"remote_port" : 53,
	"packet_size" : 512
}
 * </code>
 * @author javaconsigliere
 *
 */
public class DatagramRelay
implements Runnable, DaemonController
{
	
	
	public enum ConfigParam
		implements GetName
	{
		
		PORT("port"),
		REMOTE_HOST("remote_host"),
		REMOTE_PORT("remote_port"),
		PACKET_SIZE("packet_size"),
		COMMANDS("commands"),
		;

		private final String name;
		
		ConfigParam(String name)
		{
			this.name = name;
		}
		
		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return name;
		}
		
		public static DatagramRelay toDatagramRelay(String json) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException
		{
			return toDatagramRelay(GSONUtil.fromJSONGenericMap(json, null, Base64Type.URL));
		}
		
		public static DatagramRelay toDatagramRelay(NVGenericMap config)
			throws IOException
		{
			return new DatagramRelay(config.getValue((GetName)PORT), (String)config.getValue((GetName)REMOTE_HOST), config.getValue((GetName)REMOTE_PORT), config.getValue((GetName)PACKET_SIZE));
		}
		
	}
	
	
	
	private static volatile Logger log = Logger.getLogger(DatagramRelay.class.getName());
	
	private InetAddress remoteHost;
	private int remotePort;
	private int packetSize;
	private DatagramSocket dsServer;
	private DatagramSocket dsRemote;
	private long requestCount = 0;
	
	/**
	 * Create a DatagramRelay.
	 * @param localPort local port. 
	 * @param remoteHost hostname of the server.
	 * @param remotePort port of the remote host
	 * @param packetSize in byte of the data buffer
	 * @throws IOException in case of error
	 */
	public DatagramRelay(int localPort, String remoteHost, int remotePort, int packetSize)
			throws IOException
	{
		this(localPort, InetAddress.getByName(remoteHost), remotePort, packetSize);
	}
	
	/**
	 * Create a DatagramRelay.
	 * @param localPort local port. 
	 * @param remoteHost hostname of the server.
	 * @param remotePort port of the remote host
	 * @param packetSize in byte of the data buffer
	 * @throws IOException in case of error
	 */
	public DatagramRelay(int localPort, InetAddress remoteHost, int remotePort, int packetSize)
			throws IOException
	{

		SUS.checkIfNulls("null host", remoteHost);
		SharedUtil.illegalCondition("Invalid port values", (localPort > 0 && localPort < 65535 ), (remotePort > 0 && remotePort < 65535));
		SharedUtil.illegalCondition("Invalid packet size " + packetSize, (packetSize > 0 && packetSize < 65535 ));
		
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.packetSize = packetSize;
		dsServer = new DatagramSocket(localPort);
		dsRemote = new DatagramSocket();
		log.info("created@" + localPort +"<->" + remoteHost.getHostAddress() + ":" + remotePort +".Buffer Size:" + packetSize);
		
	}
	
	@Override
	public void run() 
	{
		log.info("started@" + dsServer.getLocalPort() +"<->" + remoteHost.getHostAddress() + ":" + remotePort);
		byte[] inBuffer = new byte[packetSize];
		byte[] outBuffer = new byte[packetSize];
		DatagramPacket inDP = new DatagramPacket(inBuffer, inBuffer.length);
		while(!dsServer.isClosed())
		{
			try
			{
				
				dsServer.receive(inDP);
				DatagramPacket received = sendToRemote(dsRemote, inDP, outBuffer, remoteHost, remotePort);
				received.setAddress(inDP.getAddress());
				received.setPort(inDP.getPort());
				dsServer.send(received);
				
				
				requestCount++;
			}
			catch (InterruptedIOException e) {
				continue;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				log.info("Error" + dsServer.getLocalPort() +"<->" + remoteHost.getHostAddress() + ":" + remotePort + " total requests:" + requestCount) ;
				
			}
		}
		
	}

	@Override
	/**
	 * close and stop the relay
	 */
	public void close() throws IOException {
		// TODO Auto-generated method stub
		log.info("Closing@" + dsServer.getLocalPort() +"<->" + remoteHost.getHostAddress() + ":" + remotePort + " total requests:" + requestCount) ;
		dsServer.close();
	}

	/**
	 * Return the status
	 */
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return dsServer.isClosed();
	}
	
	private DatagramPacket sendToRemote(DatagramSocket ds, DatagramPacket incoming, byte buffer[], InetAddress remoteHost, int remotePort)
			throws IOException
	{
		

		
		DatagramPacket toSend = new DatagramPacket(incoming.getData(), incoming.getLength(), remoteHost, remotePort);
		ds.send(toSend);
		//ds.setSoTimeout(5000);
		Arrays.fill(buffer, (byte)0);
		DatagramPacket received = new DatagramPacket(buffer, buffer.length);
		//wait for the response
		ds.receive(received);
		return received;

	}
	
	public static void main(String ...args)
	{
		try
		{
			int index = 0;
			File file = null;
			
			
			// maybe a json config file
			if (args.length == 1)
				file = new File(args[index]);
			
			DatagramRelay dr = null;		
			if (file != null && file.exists())
				dr = ConfigParam.toDatagramRelay(IOUtil.inputStreamToString(file));
			else
				dr = new DatagramRelay(Integer.parseInt(args[index++]), args[index++], Integer.parseInt(args[index++]), Integer.parseInt(args[index++]));
			
			// start the relay
			new Thread(dr).start();
		}
		catch(Exception e)
		{
			System.err.println("usage: <localPort> <remoteHostName> <remotePort> <packet buffer size>");
			System.err.println("or");
			System.err.println("usage: <jsonConfigFile>");
			e.printStackTrace();
		}
	}

}
