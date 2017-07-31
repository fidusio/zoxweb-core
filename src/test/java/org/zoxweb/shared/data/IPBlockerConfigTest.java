package org.zoxweb.shared.data;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.security.IPBlockerConfig;




public class IPBlockerConfigTest {

	IPBlockerConfig ipbc;
	
	
	@Before
	public void init()
	{
		ipbc = new IPBlockerConfig();
		ipbc.setAuthFile("/var/log/auth.log");
		ipbc.setAuthToken("authentication failure;");
		ipbc.setAuthValue("rhost");
		ipbc.setCommand("/sbin/iptables -I INPUT 4 -s $$host$$ -p tcp -m tcp --dport 22 -j DROP");
		ipbc.setCommandToken("$$host$$");
		ipbc.setTriggerCount(10);
		ipbc.setRate((float) 12);
	}
	
	

	@Test
	public void testToJSON() throws IOException
	{
		// to guaranty the sequence
		System.out.println(GSONUtil.toJSON(ipbc, true, false, false));
	}
	
	
}
