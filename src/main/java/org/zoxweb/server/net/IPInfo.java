/*
 * Copyright 2012 ZoxWeb.com LLC.
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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import java.util.List;
import java.util.Properties;

import org.zoxweb.server.io.IOUtil;

import org.zoxweb.shared.net.InetProp.IPVersion;
import org.zoxweb.shared.net.InetProp.InetProto;
import org.zoxweb.shared.util.SharedUtil;






/**
 * This class contains all the IP Information associated with a network connection.
 * This class will work with Linux for now windows support might come soon.
 * <br> This class will return the following info:
 * <ul>
 * <li> IP address.
 * <li> Network mask.
 * <li> Network address.
 * <li> Default gateway or router.
 * <li> Broadcast ip address.
 * </ul>
 * @author mnael
 *
 */

public class IPInfo 
{
	
	
	private IPInfo( NetworkInterface ni, IPVersion ipv)
	{
		SharedUtil.checkIfNulls("NetworkInterface or ipv can't be null", ni, ipv);
		if ( ipv == IPVersion.V6)
		{
			throw new IllegalArgumentException("IPV6 is not supported");
		}
		this.ni = ni;
		this.ipv = ipv;
	}
	
	
	public InetAddress getIPAddress() 
		throws IOException
	{
		InetAddress ret = null;
		switch( ipv)
		{
		case V4:
			ret = NetUtil.getIPV4MainAddress(ni);
			break;
		case V6:
			break;
		}
		
		
		return ret;
	}
	
	public InetAddress getNetworkMask() throws IOException
	{
		InetAddress ret = null;
		switch( ipv)
		{
		case V4:
			List<InterfaceAddress> lia = ni.getInterfaceAddresses();
			InetAddress main = getIPAddress();
			for ( InterfaceAddress ia : lia)
			{
			
				if ( main.equals(ia.getAddress()))
				{
					ret = NetUtil.toNetmaskIPV4(ia.getNetworkPrefixLength());
					break;
				}
			}
			break;
		case V6:
			break;
		}
		
		
		return ret;
	}
	
	public InetAddress getNetwork() throws IOException
	{
		InetAddress ret = null;
		switch( ipv)
		{
		case V4:
			List<InterfaceAddress> lia = ni.getInterfaceAddresses();
			InetAddress main = getIPAddress();
			for ( InterfaceAddress ia : lia)
			{
				
				if ( main.equals(ia.getAddress()))
				{
					ret = NetUtil.getNetwork(ia);
					break;
				}
			}
			break;
		case V6:
			break;
		}
		
		return ret;
	}
	
	public InetAddress getBroadcast() throws IOException
	{
		InetAddress ret = null;
		switch( ipv)
		{
		case V4:
			List<InterfaceAddress> lia = ni.getInterfaceAddresses();
			InetAddress main = getIPAddress();
			for ( InterfaceAddress ia : lia)
			{
			
				if ( main.equals(ia.getAddress()))
				{
					ret = ia.getBroadcast();
					break;
				}
			}
			break;
		case V6:
			break;
		}
		
		return ret;
	}
	
	public InetAddress getGateway()
		throws IOException
	{
		return getLinuxRouter();
	}
	
	
	
	
	private InetAddress getLinuxRouter()
		throws IOException
	{
		InetAddress ret = null;
		switch( ipv)
		{
		case V4:
			// go to ifcfg-ni name file 
			// load /etc/sysconfig/network-scripts/ifcfg-niname it as Properties
			// look for BOOTPROTO value 
			// if static
			// - return the GATEWAY value
			// if dhcp
			// - read the last entry in /var/lib/dhclient/dhclient-niname.lease of option routers ip.ip.ip.ip;
			Properties ifcfg = new Properties();
			FileReader fr = new FileReader(LINUX_NI_CFG_PREFIX+ni.getName());
			ifcfg.load(fr);
			fr.close();
			
			String proto = ifcfg.getProperty( LINUX_BOOTPROTO);
			InetProto pt = InetProto.valueOf(proto.toUpperCase());
			
			switch( pt)
			{
			case STATIC:
				String gateway = ifcfg.getProperty(LINUX_GATEWAY);
				
				return gateway != null ? InetAddress.getByName( ifcfg.getProperty(LINUX_GATEWAY)) : null;
				
			case DHCP:
				return InetAddress.getByName(lookupRouterFromDHClientFile());
			
			default:
				throw new IOException("Network " + ni.getName() + " has unsupported BOOTPROTO" + proto );
			}
			
		case V6:
			break;
		}
		
		return ret;
	}
	
	
	private String lookupRouterFromDHClientFile()
		throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader(LINUX_DHCP_NI_CFG_PREFIX + ni.getName() + LINUX_DHCP_NI_CFG_POSTFIX));
		
		String lastRouterLine = null;
		String line = null;
		while ( (line = br.readLine()) != null )
		{
			if ( line.indexOf(LINUX_DHCP_ROUTERS) != -1 )
			{
				lastRouterLine = line;
			}
		}
		IOUtil.close(br);
		
		if ( lastRouterLine != null)
		{
			int beginIndex = lastRouterLine.indexOf(LINUX_DHCP_ROUTERS)+ LINUX_DHCP_ROUTERS.length();
			int endIndex = lastRouterLine.indexOf(";");
			return lastRouterLine.substring(beginIndex, endIndex).trim();
		}
		
		
		return null;
		
	}
	
	public IPVersion getIPVersion()
	{
		return ipv;
	}
	
	
	public NetworkInterface getNetworkInterface()
	{
		return ni;
	}
	
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			sb.append("NetInterf=" + ni.getName() +"\n");
			sb.append("IPAddress=" + (getIPAddress() != null ? getIPAddress().getHostAddress() : "")+"\n");
			sb.append("NETMask  =" + (getNetworkMask() != null ? getNetworkMask().getHostAddress() : "")+"\n");
			sb.append("Gateway  =" + (getGateway() != null ? getGateway().getHostAddress() : "") +"\n");
			sb.append("Network  =" + (getNetwork() != null ?  getNetwork().getHostAddress() : "")+"\n");
			sb.append("Broadcast=" + (getBroadcast() != null ? getBroadcast().getHostAddress() : "")+"\n");
			
		}
		catch( Exception e)
		{
			
		}
		
		return sb.toString();
	}
	
	
	public static IPInfo getV4IPInfo( String niName) throws IOException
	{
		return new IPInfo(NetworkInterface.getByName(niName), IPVersion.V4);
	}
	
	public static IPInfo getV4IPInfo( NetworkInterface ni)
		throws IOException
	{
		return new IPInfo(ni, IPVersion.V4);
	}
	
	
//	public static void main( String... args)
//	{
//		if ( args.length > 0)
//		{
//			for ( String arg : args)
//			{
//				try
//				{
//					System.out.println("Trying to get IP infor for " + arg);
//					System.out.println( getV4IPInfo(arg));
//				}
//				catch( Exception e )
//				{
//					e.printStackTrace();
//				}
//			}
//		}
//		else
//		{
//			try 
//			{
//				for ( Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();)
//				{
//					NetworkInterface ni = e.nextElement();
//					System.out.println("Trying to get IP infor for " + ni.getName());
//					System.out.println( getV4IPInfo(ni));
//					
//				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	
	public static String LINUX_NI_CFG_PREFIX="/etc/sysconfig/network-scripts/ifcfg-";
	public static String LINUX_DHCP_NI_CFG_PREFIX="/var/lib/dhclient/dhclient-";
	public static String LINUX_DHCP_NI_CFG_POSTFIX=".leases";
	public static String LINUX_DHCP_ROUTERS="option routers";
	
	public static final String LINUX_BOOTPROTO="BOOTPROTO";
	public static final String LINUX_GATEWAY="GATEWAY";
	
	
	private IPVersion ipv;
	private NetworkInterface ni;
	
}
