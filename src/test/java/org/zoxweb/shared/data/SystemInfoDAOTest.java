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
package org.zoxweb.shared.data;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.NVEntity;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SystemInfoDAOTest {

	public static void main(String[] args) {
		try {
			SystemInfoDAO sys = ServerUtil.loadSystemInfoDAO();
			GsonBuilder builder = new GsonBuilder();
			builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
			builder.setPrettyPrinting();
			Gson gson = builder.create();
			String str = gson.toJson(sys);
			System.out.println( str);
			System.out.println( sys.getSystemProperties());
			
			
			for (NVEntity nve : sys.getNetworkInterfaces().values()) {
				NetworkInterfaceDAO nid = (NetworkInterfaceDAO) nve;
				System.out.println( nid.getName() + " " + nid.getMACAddress());
			}
			
			sys.getApplicationDAOs().add( new AgreementDAO("english", "I agree to", "Terms and Conditions" ,"I agree to term and condition"));
			String zwJson = GSONUtil.toJSON(sys, true);
			System.out.println( zwJson);
			
			SystemInfoDAO sysFromJson = GSONUtil.fromJSON(zwJson, SystemInfoDAO.class);
			
			String zwJson1 = GSONUtil.toJSON(sysFromJson, true);
			System.out.println("Equals:" + zwJson1.equals(zwJson));
			System.out.println("ApplicationDAOs:" + sysFromJson.getApplicationDAOs());
			System.out.println("ApplicationDAOs:" + sys.getApplicationDAOs());
			System.out.println(zwJson1);
			System.out.println(sys.getNetworkInterfaces().size() + "," +sysFromJson.getNetworkInterfaces().size() );

//			Iterator<NetworkInterfaceDAO> it = sys.getNetworkInterfaces().values().iterator();
			
//			while(it.hasNext())
//			{
//				NetworkInterfaceDAO niDAO = it.next();
//				
//				NetworkInterface ni = NetworkInterface.getByName( niDAO.getName());
//				byte address[] =  ni.getHardwareAddress();
//				long zTS=0, tTS=0, zhTS=0, thTS=0;
//				byte[] tAddress=null, zAddress=null;
//				String tBA=null, zBA=null;
//				for ( int i= 0 ;i <2; i++)
//				{
//					zTS = System.nanoTime();
//					zBA = SharedUtil.bytesToHex(address);
//					zTS = System.nanoTime() - zTS;
//					
//					
//					zhTS = System.nanoTime();
//					zAddress = SharedUtil.hexToBytes(zBA);
//					zhTS = System.nanoTime() - zhTS;
//					
//					
//					tTS = System.nanoTime();
//					tBA = PasswordHashUtil.toHex( address);
//					tTS = System.nanoTime() - tTS;
//					
//					thTS = System.nanoTime();
//					tAddress =  PasswordHashUtil.fromHex(tBA);
//					thTS = System.nanoTime() - thTS;
//				}
//				
//				System.out.println( SharedUtil.toCanonicalID(',', niDAO.getName(), niDAO.getHardwareAddress(), "zoxweb array to hex string", zTS, zBA, "third", tTS, tBA, "delta", (tTS-zTS)));
//				System.out.println( SharedUtil.toCanonicalID(',', niDAO.getName(), niDAO.getHardwareAddress(), "zoxweb hex string to byte array", zhTS, "third", thTS, tBA, "delta", (thTS-zhTS), SharedUtil.slowEquals(tAddress, zAddress), SharedUtil.slowEquals(tAddress, address)));
//				System.out.println( SharedUtil.bytesToHex(zAddress));
//				
//				
//				
//				
//			}
			
//			
//		List<NetworkInterfaceDAO> nis = sysFromJson.getNetworkInterfaces();
//		NVEntityMap nvem = new NVEntityMap();
//		
//		for (NetworkInterfaceDAO nid : nis)
//		{
//			nvem.add(nid);
//			
//		}

		//sysFromJson = 	sys;	
		System.out.println("size of nvem:" + sysFromJson.getNetworkInterfaces().size());
		ArrayValues<NVEntity> nvem = (ArrayValues<NVEntity>) sysFromJson.getNetworkInterfaces();//lookup(SystemInfoDAO.NVC_NETWORK_INTERFACES.getName());
		int counter = 0;
		
		System.out.println("Eth4:" + nvem.get("eth4"));
		//NetworkInterfaceDAO array[] = sysFromJson.getNetworkInterfaces().values();
		for (NVEntity nve : sys.getNetworkInterfaces().values()) {
			NetworkInterfaceDAO nid = (NetworkInterfaceDAO) nve;
			System.out.println(nvem.get(nid.getName()));
			if (counter %2 == 0)
				nvem.remove( nvem.get(nid.getName()));
			else
				nvem.remove(nid);
			System.out.println(sysFromJson.getNetworkInterfaces().size());
			
			counter++;
		}
		
		System.out.println(sys.getNetworkInterfaces().size() + "," +sysFromJson.getNetworkInterfaces().size() );
		System.out.println(sys.getNetworkInterfaces().size() + "," +sys.getNetworkInterfaces().search("eth4") );
		IPAddress addr1 = new IPAddress("zoxweb.com", 80);
		IPAddress addr2 = new IPAddress("zoxweb.com", 80);
		System.out.println(addr1.equals(addr2));
		
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}