package org.zoxweb.shared.filters;

import org.zoxweb.shared.net.MACAddressFilter;

public class MACAddressTest 
{
	public static void main(String... args)
	{
		String[] macs = {
			"AA:fd:34:24:34:32","AA-fd-34-24-34-32", "AAfd34243432","01.23.45.67.89.ab", "AA:fd:34:24:34", null, "","AA:fd:34:24:3k"
		};
		
		
		for (String mac: macs)
		{
			try
			{
				System.out.println("Original Address:" + mac);
				byte[] address = MACAddressFilter.SINGLETON.validate(mac);
				System.out.println("Formatted Address:" + MACAddressFilter.toString(address, "-"));
			}
			catch(Exception e)
			{
				System.out.println("Error " +e);
			}
		}
	}
}
