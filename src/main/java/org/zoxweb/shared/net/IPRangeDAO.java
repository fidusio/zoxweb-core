package org.zoxweb.shared.net;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;



/**
 * The IPRange class is bean class that contains information regarding an
 * iprange .
 * 
 * @version
 * @author JavaConsigliere
 */
@SuppressWarnings("serial")
public class IPRangeDAO
extends SetNameDescriptionDAO
{

	/**
	 * This enum contains the following variables:
	 * agreement title, agreement content, and 
	 * agreement check title.
	 * @author mzebib
	 *
	 */
	public enum Params
		implements GetNVConfig
	{
		IFACE(NVConfigManager.createNVConfig("interface", "The system interface name", "Interface", true, true, String.class)),
		NET_ADDRESS(NVConfigManager.createNVConfig("address", "Thge ip address", "InetAddress", true, true, String.class)),
		NET_MASK(NVConfigManager.createNVConfig("netmask", "The network mask", "NetorkMask", true, true, String.class)),
		COUNT(NVConfigManager.createNVConfig("count", "Count", "count", false, true, Integer.class)),

		;
		
		private final NVConfig cType;
		
		Params (NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	
	}
	
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on AgreementDAO.
	 */
	public static final NVConfigEntity IP_RANGE_DAO = new NVConfigEntityPortable("ip_range_dao", null , "IPRangeDAO", true, false, false, false, IPRangeDAO.class, SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);
	public IPRangeDAO()
	{
		super(IP_RANGE_DAO);
	}

	public IPRangeDAO(String inet, String startIP, String mask, int count)
	{
		this();
		setNetworkInterface(inet);
		setStartingIP(startIP);
		setNetworkMask(mask);
		setIPCount(count);
	}

	/**
	 * Return the network Interface associated with;
	 */
	public String getNetworkInterface()
	{
		return lookupValue(Params.IFACE);
	}

	/**
	 * Set the network interface to be associated with.
	 */
	public void setNetworkInterface(String iface) {
		setValue(Params.IFACE, iface);
	}

	/**
	 * Return the starting ip
	 */
	public String getStartingIP()
	{
		return lookupValue(Params.NET_ADDRESS);
	}

	/**
	 * Set the starting ip
	 */
	public void setStartingIP(String ip) {
		setValue(Params.NET_ADDRESS, ip);
	}

	/**
	 * Return the network mask
	 */
	public String getNetworkMask() {
		return lookupValue(Params.NET_MASK);
	}

	/**
	 * Set the network mask
	 */
	public void setNetworkMask(String mask) {
		setValue(Params.NET_MASK, mask);
	}

	/**
	 * Return the ip count
	 */
	public int getIPCount()
	{
		return lookupValue(Params.COUNT);
	}

	/**
	 * Set the ip count.
	 */
	public void setIPCount(int count)
	{
		setValue(Params.COUNT, count);
	}

//	public String toString() {
//		return "Interface " + netInterface + " IP " + startingIP + " mask "
//				+ networkMask + " count " + ipCounts;
//	}
//
//	// private static java.io.PrintStream o = System.out;
//	private int ipCounts;
//
//	private String startingIP;
//
//	private String networkMask;
//
//	private String netInterface;
}
