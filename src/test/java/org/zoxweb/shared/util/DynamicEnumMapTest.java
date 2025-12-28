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
package org.zoxweb.shared.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.accounting.Currency;

import java.util.ArrayList;
import java.util.List;


public class DynamicEnumMapTest {

	@Test
	public void testGender() {
		DynamicEnumMap dem = new DynamicEnumMap("Gender");

		dem.addEnumValue(new NVPair("M", "Male"));
		dem.addEnumValue(new NVPair("F", "Female"));
		
		System.out.println(dem.getName());
		
		for (NVPair nvp : dem.getValue()) {
			System.out.println(nvp.getName() + " " + nvp.getValue());
		}
	}
	
	@Test
	public void testStates() {
		DynamicEnumMap dem = new DynamicEnumMap("US States");

		dem.addEnumValue(new NVPair("AL", "Alabama"));
		dem.addEnumValue(new NVPair("AK", "Alaska"));
		dem.addEnumValue(new NVPair("AZ", "Arizona"));
		dem.addEnumValue(new NVPair("AR", "Arkansas"));
		dem.addEnumValue(new NVPair("CA", "California"));
		dem.addEnumValue(new NVPair("CO", "Colorado"));
		dem.addEnumValue(new NVPair("CT", "Connecticut"));
		dem.addEnumValue(new NVPair("DE", "Delaware"));
		dem.addEnumValue(new NVPair("FL", "Florida"));
		dem.addEnumValue(new NVPair("GA", "Georgia"));
		dem.addEnumValue(new NVPair("HI", "Hawaii"));
		dem.addEnumValue(new NVPair("ID", "Idaho"));
		dem.addEnumValue(new NVPair("IL", "Illinois"));
		dem.addEnumValue(new NVPair("IN", "Indiana"));
		dem.addEnumValue(new NVPair("IA", "Iowa"));
		dem.addEnumValue(new NVPair("KS", "Kansas"));
		dem.addEnumValue(new NVPair("KY", "Kentucky"));
		dem.addEnumValue(new NVPair("LA", "Louisiana"));
		dem.addEnumValue(new NVPair("ME", "Maine"));
		dem.addEnumValue(new NVPair("MD", "Maryland"));
		dem.addEnumValue(new NVPair("MA", "Massachusetts"));
		dem.addEnumValue(new NVPair("MI", "Michigan"));
		dem.addEnumValue(new NVPair("MN", "Minnesota"));
		dem.addEnumValue(new NVPair("MS", "Mississippi"));
		dem.addEnumValue(new NVPair("MO", "Missouri"));
		dem.addEnumValue(new NVPair("MT", "Montana"));
		dem.addEnumValue(new NVPair("NE", "Nebraska"));
		dem.addEnumValue(new NVPair("NV", "Nevada"));
		dem.addEnumValue(new NVPair("NH", "New Hampshire"));
		dem.addEnumValue(new NVPair("NJ", "New Jersey"));
		dem.addEnumValue(new NVPair("NM", "New Mexico"));
		dem.addEnumValue(new NVPair("NY", "New York"));
		dem.addEnumValue(new NVPair("NC", "North Carolina"));
		dem.addEnumValue(new NVPair("ND", "North Dakota"));
		dem.addEnumValue(new NVPair("OH", "Ohio"));
		dem.addEnumValue(new NVPair("OK", "Oklahoma"));
		dem.addEnumValue(new NVPair("OR", "Oregon"));
		dem.addEnumValue(new NVPair("PA", "Pennsylvania"));
		dem.addEnumValue(new NVPair("RI", "Rhode Island"));
		dem.addEnumValue(new NVPair("SC", "South Carolina"));
		dem.addEnumValue(new NVPair("SD", "South Dakota"));
		dem.addEnumValue(new NVPair("TN", "Tennessee"));
		dem.addEnumValue(new NVPair("TX", "Texas"));
		dem.addEnumValue(new NVPair("UT", "Utah"));
		dem.addEnumValue(new NVPair("VT", "Vermont"));
		dem.addEnumValue(new NVPair("VA", "Virginia"));
		dem.addEnumValue(new NVPair("WA", "Washington"));
		dem.addEnumValue(new NVPair("WV", "West Virginia"));
		dem.addEnumValue(new NVPair("WI", "Wisconsin"));
		dem.addEnumValue(new NVPair("WY", "Wyoming"));

		dem.addEnumValue(new NVPair("DC", "District of Columbia"));
		dem.addEnumValue(new NVPair("GU", "Guam"));
		dem.addEnumValue(new NVPair("PR", "Puerto Rico"));
		dem.addEnumValue(new NVPair("VI", "Virgin Islands"));
		
		System.out.println("\n" + dem.getName());
		
		for (NVPair nvp : dem.getValue()) {
			System.out.println(nvp.getName() + " " + nvp.getValue());
		}
		
	}
	
	@Test
	public void testCountries() {
		DynamicEnumMap dem = new DynamicEnumMap("Countries");

		dem.addEnumValue(new NVPair("US", "United States of America"));
		dem.addEnumValue(new NVPair("CA", "Canada"));
		dem.addEnumValue(new NVPair("UK", "United Kingdom"));
		dem.addEnumValue(new NVPair("FR", "France"));
		dem.addEnumValue(new NVPair("DE", "Germany"));
		dem.addEnumValue(new NVPair("JP", "Japan"));
		dem.addEnumValue(new NVPair("HK", "Hong Kong"));
		dem.addEnumValue(new NVPair("SG", "Singapore"));
		
		System.out.println("\n" + dem.getName());
		
		for (NVPair nvp : dem.getValue()) {
			System.out.println(nvp.getName() + " " + nvp.getValue());
		}
	}
	
	@Test
	public void testCurrency() {
		DynamicEnumMap dem = new DynamicEnumMap(Currency.class);
	
		System.out.println("\n" + dem.toCanonicalID());
		
		for (NVPair nvp : dem.getValue()) {
			System.out.println(nvp.getName() + " " + nvp.getValue());
		}
	}
	
	@Test //(expected = NullPointerException.class)
	public void testNull() {
		DynamicEnumMap test = new DynamicEnumMap("Test");
		Assertions.assertThrows(NullPointerException.class, ()->test.addEnumValue(new NVPair(null)));
	}

	@Test //(expected = IllegalArgumentException.class)
	public void testInvalidStates() {
		DynamicEnumMap dem = new DynamicEnumMap("States");
		
		List<NVPair> list = new ArrayList<NVPair>();
		list.add(new NVPair("CA", "California"));
		list.add(new NVPair("CA", "California"));

		dem.setValue(list);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()->DynamicEnumMapManager.validateDynamicEnumMap(dem));
	}
	
}
