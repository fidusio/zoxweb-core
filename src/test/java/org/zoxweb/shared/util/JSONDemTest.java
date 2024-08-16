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
package org.zoxweb.shared.util;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.filters.FilterType;

import java.util.ArrayList;
import java.util.List;

public class JSONDemTest {
	
	public static void main(String[] args) {

		DynamicEnumMap dem = new DynamicEnumMap();
		dem.setReferenceID("123456798");
		dem.setSubjectGUID("48654561");
		dem.setAccountID("564654");
		dem.setName("My Dem");
		dem.setDescription("This is my dem.");
		dem.setFixed(false);
		dem.setIgnoreCase(true);
		dem.setStatic(true);
		
		List<NVPair> values = new ArrayList<NVPair>();
		values.add(new NVPair("Email", "jsmith@zoxweb.com", FilterType.EMAIL));
		values.add(new NVPair("Website", "https://www.zoxweb.com", FilterType.URL));
		
		dem.setValue(values);
		
		try {
			System.out.println("JSON <==> DynamicEnumMap");
			
			// To JSON
			String toJSON = GSONUtil.toJSONDynamicEnumMap(dem);
			System.out.println("ToJSON: \n" + toJSON);
			
			//	From JSON
			DynamicEnumMap fromDEM = GSONUtil.fromJSONDynamicEnumMap((toJSON));
			String fromJSON = GSONUtil.toJSONDynamicEnumMap(fromDEM);
			System.out.println("FromJSON: \n" + "DEM: "+ fromDEM + "\nJSON: " + fromJSON);
			System.out.println("\nAre fromJSON and toJSON equal? " + fromJSON.equals(toJSON));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<DynamicEnumMap> list = new ArrayList<DynamicEnumMap>();
		
		DynamicEnumMap dem2 = new DynamicEnumMap();
		dem2.setReferenceID("123456798");
		dem2.setSubjectGUID("48654561");
		dem2.setAccountID("564654");
		dem2.setName("My Dem");
		dem2.setDescription("This is my dem.");
		dem2.setFixed(true);
		dem2.setIgnoreCase(true);
		dem2.setStatic(true);
		dem2.setValue(values);
		
		list.add(dem2);
		list.add(dem2);
		
		try {
			System.out.println("\nJSON <==> DynamicEnumMap List");
			// To JSON
			String toJSON = GSONUtil.toJSONDynamicEnumMapList(list);
			System.out.println("ToJSON: \n" + toJSON);
			
			//	From JSON
			List<DynamicEnumMap> fromDEMList = GSONUtil.fromJSONDynamicEnumMapList((toJSON));
			String fromJSON = GSONUtil.toJSONDynamicEnumMapList(fromDEMList);
			System.out.println("FromJSON: \n" + "DEM: "+ fromDEMList + "\nJSON: " + fromJSON);
			System.out.println("\nAre fromJSON and toJSON equal? " + fromJSON.equals(toJSON));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}