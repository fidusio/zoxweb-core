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
package org.zoxweb;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.SystemInfoDAO;
import org.zoxweb.shared.net.InetAddressDAO;
import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import com.google.gson.Gson;

public class JSONTest {


	static class EnumTester
	{
		TimeInMillis[] tims;
	}
	static class Toto
	{
		Date date;
		SystemInfoDAO sys_dao;
		NVGenericMap nvgm;
		
	}
	private static Logger log = Logger.getLogger(Const.LOGGER_NAME);

	public static void main (String[] args) {

	    try {
			SystemInfoDAO sysDAO = ServerUtil.loadSystemInfoDAO();
			sysDAO.setGlobalID(UUID.randomUUID().toString());
			
			for (NVEntity nve : sysDAO.getNetworkInterfaces().values()) {
				NetworkInterfaceDAO niDAO = (NetworkInterfaceDAO) nve;

				for (InetAddressDAO iaDAO : niDAO.getInetAddresses()) {
					String json = GSONUtil.toJSON( iaDAO, false);
					System.out.println(json);
					System.out.println("" + GSONUtil.fromJSON(json, InetAddressDAO.class));
				}

				long ts1 = System.nanoTime();
				String json1 = new Gson().toJson(niDAO);
				ts1 = System.nanoTime() - ts1;
				
				String json = null;
				long ts = System.nanoTime();
				json = GSONUtil.toJSON( niDAO, false);
				ts = System.nanoTime() -ts;

				System.out.println( json1);
				System.out.println( json);
				System.out.println( "******* json conversion Gson:" + ts1 + " it took zoxweb json:" + ts + " delta " +((ts1-ts)) + " zoxweb is "  + ((float)ts1/(float)ts) + " faster");
				
				ts1 = System.nanoTime();
				NetworkInterfaceDAO niTemp = new Gson().fromJson(json1, NetworkInterfaceDAO.class);
				ts1 = System.nanoTime() - ts1;

				ts = System.nanoTime();
				NetworkInterfaceDAO zbNI = GSONUtil.fromJSON(json, NetworkInterfaceDAO.class);
				ts = System.nanoTime() -ts;
				System.out.println( "Gson obj:"+niTemp);
				System.out.println( "ZW   obj:"+zbNI);
				System.out.println( "NI   DAO:"+niDAO);
				System.out.println( "******* Object conversion Gson:" + ts1 + " it took zoxweb json:" + ts + " delta " +((ts1-ts)) + " zoxweb is "  + ((float)ts1/(float)ts) + " faster");
			}

			String temp =  GSONUtil.toJSON(sysDAO, true);
			System.out.println(temp);
			SystemInfoDAO newSysDAO =  GSONUtil.fromJSON(temp, SystemInfoDAO.class);
			System.out.println( "Json Equals : " + temp.equals(GSONUtil.toJSON( newSysDAO, true)));
			System.out.println( sysDAO);
			log.info("java logger"+ newSysDAO);
			
			AddressDAO address = new AddressDAO();
			address.setStreet("P.O Box 251906");
			address.setCity("Los Angeles");
			address.setStateOrProvince("CA");
			address.setZIPOrPostalCode("90025");
			address.setCountry("USA");
			
			System.out.println(GSONUtil.toJSON(address, true, false, false, null));
			System.out.println(GSONUtil.toJSONWrapper("address", address, true, false, false, null));
			
			String jsonValues = GSONUtil.toJSONValues(sysDAO.getNetworkInterfaces().values(), true, false, true, null);
			System.out.println(jsonValues);
			List<NVEntity> values = GSONUtil.fromJSONValues(jsonValues, Base64Type.DEFAULT);
			System.out.println(values);
			String json  = GSONUtil.toJSONArray(values, true, false, Base64Type.DEFAULT);
			System.out.println(json);
			List<NVEntity> nves = GSONUtil.fromJSONArray(json, Base64Type.DEFAULT);
			System.out.println(nves);

			NVGenericMap nvgm = new NVGenericMap();
			nvgm.setName("name");
			nvgm.add(new NVLong("longValue", 67));
			nvgm.add(new NVDouble("doubleValue", 567.45));
			nvgm.add(new NVBoolean("booleanValue", true));
			nvgm.add(new NVPair("aname", "mario"));
			Toto t = new Toto();
			t.sys_dao = sysDAO;
			t.nvgm = nvgm;
			t.date = new Date();

			//System.out.println(GGSONUtil.DEFAULT_GSON.toJson(t)SONUtil.DEFAULT_GSON.toJson(sysDAO));
			String ts = GSONUtil.DEFAULT_GSON.toJson(t);
			System.out.println(ts);
			System.out.println(GSONUtil.toJSON(sysDAO, false, false,true));
			Toto t2 = GSONUtil.DEFAULT_GSON.fromJson(ts, Toto.class);
			System.out.println(t.date.equals(t2.date));
			EnumTester et = new EnumTester();
			String testET = GSONUtil.DEFAULT_GSON.toJson(et);
			System.out.println(testET);
			et.tims = new TimeInMillis[]{TimeInMillis.SECOND, TimeInMillis.DAY};
			testET = GSONUtil.DEFAULT_GSON.toJson(et);
			System.out.println(testET);
			testET = "{\"tims\":[\"SECOND\",\"day\"]}";
			et = GSONUtil.DEFAULT_GSON.fromJson(testET, EnumTester.class);
			testET = GSONUtil.DEFAULT_GSON.toJson(et);
			System.out.println(testET);


			NVStringList nvsl = new NVStringList("tataName");
			nvsl.setValues("toto", "titi", "tata");
			nvgm = new NVGenericMap();
			nvgm.add(nvsl);
			nvgm.add(new NVInt("i0", 0));
			nvgm.add(new NVInt("i1", 1));
			nvgm.add(new NVInt("i-1", -1));
			nvgm.add(new NVLong("l0", 0));
			nvgm.add(new NVLong("l1", 1));
			nvgm.add(new NVLong("l-1", -1));

			nvgm.add(new NVFloat("f0", (float) 0));
			nvgm.add(new NVFloat("f", (float) 0.9));
			nvgm.add(new NVFloat("f1", (float) 1));
			nvgm.add(new NVDouble("d0", (double) 0));
			nvgm.add(new NVDouble("d", (double) 0.9));
			nvgm.add(new NVDouble("d1", (double) 1));

			String jsonNVSL = GSONUtil.toJSONGenericMap(nvgm, false, false, false);
			System.out.println(jsonNVSL);
			nvsl = (NVStringList) GSONUtil.fromJSONGenericMap(jsonNVSL, null, null).get("tataName");
			System.out.println(nvsl);




		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}