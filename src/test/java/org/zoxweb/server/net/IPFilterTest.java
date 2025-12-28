/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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

import java.net.InetAddress;
import java.util.List;

import org.zoxweb.server.net.InetFilterRulesManager.InetFilterRule;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.InetFilterDAO;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

public class IPFilterTest {

	public static void main(String[] args) {

//		String localhost = null;
//		
//		try 
//		{
//			localhost = InetAddress.getByName("localhost").getHostAddress();
//		} catch (UnknownHostException e1) {
//			e1.printStackTrace();
//		}

		String[] ipArray = new String[] {
			"10.0.0.1",
			"10.0.1.185",
			"10.0.2.1",
			"44.44.34.44",
			"192.168.1.1",
			"55.55.55.55",
			"localhost",
			"204.110.9.200",
			"management.rubuspi.com"
		};

		InetFilterRulesManager ipfm = new InetFilterRulesManager();
		
		try {
			InetFilterRule ifr = null;
			
			checkTest( ipfm, ipArray);
			
			ipfm.addInetFilterProp(new InetFilterDAO("44.44.34.44", "255.255.255.255"), SecurityStatus.DENY);
			
			System.out.println();
			checkTest( ipfm, ipArray);
			
			ipfm.addInetFilterProp(new InetFilterDAO("10.0.0.1", "255.255.0.0"), SecurityStatus.ALLOW);
			System.out.println();
			checkTest( ipfm, ipArray);
		 	
			ipfm.addInetFilterProp(new InetFilterDAO("localhost", "255.255.255.255"), SecurityStatus.ALLOW);
			System.out.println();
			checkTest( ipfm, ipArray);
			
			ipfm = new InetFilterRulesManager();
			ipfm.addInetFilterProp(new InetFilterDAO("localhost", "255.255.255.255"),  SecurityStatus.ALLOW);
			//ipfm.addIPFilterProp(new InetFilterDAO("10.0.0.1", "255.255.0.0"),  SecurityStatus.ALLOW);
			//ipfm.addIPFilterProp(new InetFilterDAO("192.168.0.1", "255.255.0.0"),  SecurityStatus.ALLOW);

			System.out.println();
			checkTest( ipfm, ipArray);

			// this example demonstrate all 
		
			ipfm.addInetFilterProp(new InetFilterDAO("44.44.34.44", null), SecurityStatus.ALLOW);
			ipfm.addInetFilterProp(new InetFilterDAO("10.0.1.1", "255.255.255.0"), SecurityStatus.ALLOW);
			//ifr = new InetFilterRule(new InetFilterDAO("0.0.0.0", "0.0.0.0"),  SecurityStatus.DENY);
			//ifr.getInetFilterDAO().setName("Deny All");
			ipfm.addInetFilterProp(ifr);
			 
			 
			System.out.println();
			checkTest( ipfm, ipArray);
			
			System.out.println("new test");
			checkTest( ipfm, ipArray);
			
			String json = GSONUtil.toJSONs(ipfm.getAll(), true, false, null);
			//System.out.println(ipfm.getAll().get(0));
			System.out.println(json);
			List<InetFilterRule> test = GSONUtil.fromJSONs(json, Base64Type.DEFAULT, InetFilterRule.class);
			System.out.println(test);
			
			ipfm = new InetFilterRulesManager();
			ipfm.setAll(test);
			checkTest( ipfm, ipArray);
			System.out.println("Same test twice");
			ipfm.addInetFilterProp("management.rubuspi.com-255.255.255.255-ALLOW");
			checkTest( ipfm, ipArray);
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	public static void checkTest(InetFilterRulesManager ipfm, String[] ipArray) {
		for (String ip : ipArray) {
			
			try {
				InetAddress inet = InetAddress.getByName(ip);
				byte[] address = inet.getAddress();
				long ts = System.nanoTime();
				SecurityStatus status = ipfm.checkIPSecurityStatus(address);
				ts = System.nanoTime() - ts;
				System.out.println("ip " + ip + " status " + status + ":" + ts + " nanos");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
