package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.http.HTTPEndPoint;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

public class NVGenericMapTest
{
	public static void printValue(NVGenericMap nvgm)
	{
		System.out.println(nvgm.getName());
		
		GetNameValue<?>[] avs = nvgm.values();
		for(GetNameValue<?> av: avs)
		{
			System.out.println(av.getClass().getName() + ":" + av);
			
		}
		
	}
	
	static class TestClass
	{
	  String name;
	  NVGenericMap nvgm;
	}

	@Test
	public void readConfiguration() throws IOException {
		try {
			String json = IOUtil.inputStreamToString(NVGenericMapTest.class.getResourceAsStream("/NVGenericMap.json"), true);
			HTTPEndPoint hep = GSONUtil.fromJSON(json, HTTPEndPoint.class);

			System.out.println("" + hep + " " + hep.getClass());
			NVGenericMap nvgm = hep.getProperties();
			System.out.println("" + nvgm + " " + nvgm.getClass());
			nvgm = (NVGenericMap) nvgm.get("gpios-map");
			System.out.println("" + nvgm + " " + nvgm.getClass());
			nvgm = (NVGenericMap) hep.getProperties().get("gpios-init");
			System.out.println("" + nvgm + " " + nvgm.getClass());
			for (GetNameValue<?> nvGenericMap : nvgm.values()) {
				System.out.println("Inner value:" + nvGenericMap + " " + nvGenericMap.getClass());
			}
			nvgm = (NVGenericMap) nvgm.get("modem");
			System.out.println("" + nvgm);
			System.out.println("state:" + nvgm.getValue("state"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	@Test
	public void lookup() throws IOException {
		try {
			String json = IOUtil.inputStreamToString(NVGenericMapTest.class.getResourceAsStream("/NVGenericMap.json"), true);
			NVGenericMap nvgm = GSONUtil.fromJSONDefault(json, NVGenericMap.class);
			System.out.println(nvgm);
			String[] lookups = {
					"properties",
					"properties.gpios-map",
					"properties.gpios-init",
					"properties.gpios-init.modem",
					"properties.batata",
					"bean",
					"bean.batata"
			};

			for (String lookup: lookups)
			{
				System.out.println(lookup + ": " + nvgm.lookup(lookup));
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	@Test
	public void lookupContainer() throws IOException {
		try {
			String json = IOUtil.inputStreamToString(NVGenericMapTest.class.getResourceAsStream("/NVGenericMap.json"), true);
			NVGenericMap nvgm = GSONUtil.fromJSONDefault(json, NVGenericMap.class);
			System.out.println(nvgm);
			String[] lookups = {
					"properties",
					"properties.gpios-map",
					"properties.gpios-init",
					"properties.gpios-init.modem",
					"properties.gpios-init.modem.state",
					"properties.batata",
					"bean",
					"bean.batata.harra"
			};

			for (String lookup: lookups)
			{
				GetNameValue<?> found = nvgm.lookupContainer(lookup);
				System.out.println(lookup + ": " + found);
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	@Test
	public void composedTest()
	{
		try
		{
			NVGenericMap nvgm = new NVGenericMap();
			nvgm.setName("name");
			nvgm.add(new NVLong("longValue", 67));
			nvgm.add(new NVDouble("doubleValue", 567.45));
			nvgm.add(new NVBoolean("booleanValue", true));
			nvgm.add(new NVPair("aname", "value"));
			ValueWithUnit<Float, String> uvw = new ValueWithUnit<Float, String>((float)123.45, ":", "m");
			uvw.setName("value_with_meter");
			nvgm.build(uvw);
			
			
			AddressDAO address = new AddressDAO();
			address.setName("tizi");
			address.setStreet("123 Main St.");
			address.setCity("Los Angeles");
			address.setStateOrProvince("CA");
			address.setCountry("USA");
			address.setZIPOrPostalCode("90025");
			nvgm.add(address);
			nvgm.add(new NVBlob("byteArray", new byte[] {0,1,2,3,5,6,7,8}));
			NVPairList nvp = new NVPairList("nvp", new ArrayList<NVPair>());
			nvp.add(new NVPair("nameNVP", "valueNVP"));
			nvgm.add(nvp);
			
	
			NVLongList nvll = new NVLongList("NVLL", new ArrayList<Long>());
			nvll.getValue().add((long) 1);
			nvll.getValue().add((long) 2);
			nvgm.add(nvll);
			printValue(nvgm);
			String json = GSONUtil.toJSONGenericMap(nvgm, true, false, true);
			System.out.println(json);
			
			nvgm = GSONUtil.fromJSONGenericMap(json, null, Base64Type.URL);
			printValue(nvgm);
			byte array[] = nvgm.getValue("byteArray");
			System.out.println("byteArray" + Arrays.toString(array));
			
			
			System.out.println(Float.parseFloat(""+Double.MAX_VALUE)); 
			System.out.println(Integer.parseInt(""+Integer.MAX_VALUE));
			
			nvgm = new NVGenericMap();
			nvgm.setName("name");
			nvgm.add(new NVPair ("folderRef", "1234325"));
			NVEntityReferenceList nvl = new NVEntityReferenceList("nves");
			nvl.add(address);
			nvgm.add(nvl);
			json = GSONUtil.toJSONDefault(nvgm, true);//GSONUtil.toJSONGenericMap(nvgm, true, false, false);
			System.out.println(json);
			//nvgm = GSONUtil.fromJSONGenericMap(json, null, Base64Type.URL);
			
			
			TestClass tc = new TestClass();
			tc.name = "not set";
			tc.nvgm = nvgm;
			json = GSONUtil.toJSONDefault(tc);
			System.out.println(json);
			tc = GSONUtil.fromJSONDefault(json, TestClass.class);
			
			json = GSONUtil.toJSONDefault(tc);
            System.out.println(json);


			System.out.println();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testBeanValues() {
		try {
			NVGenericMap nvgm = new NVGenericMap();
			nvgm.build(SUS.buildNV("inet-address", InetAddress.getLocalHost()))
					.build("nv", "simple string");

			//String json = GSONUtil.toJSONDefault(nvgm, true);
			System.out.println(nvgm);
			InetAddress address = nvgm.getValue("inet-address");
			System.out.println(address.getHostAddress());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}



	@Test
	public void toJSON()
	{
		NVGenericMap nvgm = new NVGenericMap().build("name", "mario").build("lastname", "taza");
		System.out.println(GSONUtil.toJSONDefault(nvgm, true));
		nvgm.remove("lastname");
		System.out.println(GSONUtil.toJSONDefault(nvgm, true));

	}

	@Test
	public void testAreEquals_NullCases() {
		assert NVGenericMap.areEquals(null, null) : "Both null should be equal";
		assert !NVGenericMap.areEquals(new NVGenericMap(), null) : "Non-null vs null should not be equal";
		assert !NVGenericMap.areEquals(null, new NVGenericMap()) : "Null vs non-null should not be equal";
	}

	@Test
	public void testAreEquals_EmptyMaps() {
		NVGenericMap one = new NVGenericMap();
		NVGenericMap two = new NVGenericMap();
		assert NVGenericMap.areEquals(one, two) : "Two empty maps should be equal";
	}

	@Test
	public void testAreEquals_SameInstance() {
		NVGenericMap nvgm = new NVGenericMap().build("key", "value");
		assert NVGenericMap.areEquals(nvgm, nvgm) : "Same instance should be equal";
	}

	@Test
	public void testAreEquals_PrimitiveValues() {
		NVGenericMap one = new NVGenericMap("test");
		one.add(new NVPair("string", "value"));
		one.add(new NVLong("long", 123L));
		one.add(new NVInt("int", 456));
		one.add(new NVDouble("double", 1.23));
		one.add(new NVFloat("float", 4.56f));
		one.add(new NVBoolean("bool", true));

		NVGenericMap two = new NVGenericMap("test");
		two.add(new NVPair("string", "value"));
		two.add(new NVLong("long", 123L));
		two.add(new NVInt("int", 456));
		two.add(new NVDouble("double", 1.23));
		two.add(new NVFloat("float", 4.56f));
		two.add(new NVBoolean("bool", true));

		assert NVGenericMap.areEquals(one, two) : "Maps with same primitive values should be equal";
	}

	@Test
	public void testAreEquals_DifferentValues() {
		NVGenericMap one = new NVGenericMap().build("name", "mario");
		NVGenericMap two = new NVGenericMap().build("name", "luigi");
		assert !NVGenericMap.areEquals(one, two) : "Maps with different values should not be equal";
	}

	@Test
	public void testAreEquals_DifferentSizes() {
		NVGenericMap one = new NVGenericMap().build("a", "1").build("b", "2");
		NVGenericMap two = new NVGenericMap().build("a", "1");
		assert !NVGenericMap.areEquals(one, two) : "Maps with different sizes should not be equal";
	}

	@Test
	public void testAreEquals_NestedNVGenericMap() {
		NVGenericMap innerOne = new NVGenericMap("inner");
		innerOne.add(new NVPair("nested", "value"));

		NVGenericMap one = new NVGenericMap("outer");
		one.add(innerOne);

		NVGenericMap innerTwo = new NVGenericMap("inner");
		innerTwo.add(new NVPair("nested", "value"));

		NVGenericMap two = new NVGenericMap("outer");
		two.add(innerTwo);

		assert NVGenericMap.areEquals(one, two) : "Maps with nested NVGenericMap should be equal";
	}

	@Test
	public void testAreEquals_NestedNVGenericMapDifferent() {
		NVGenericMap innerOne = new NVGenericMap("inner");
		innerOne.add(new NVPair("nested", "value1"));

		NVGenericMap one = new NVGenericMap("outer");
		one.add(innerOne);

		NVGenericMap innerTwo = new NVGenericMap("inner");
		innerTwo.add(new NVPair("nested", "value2"));

		NVGenericMap two = new NVGenericMap("outer");
		two.add(innerTwo);

		assert !NVGenericMap.areEquals(one, two) : "Maps with different nested values should not be equal";
	}

	@Test
	public void testAreEquals_ByteArrays() {
		NVGenericMap one = new NVGenericMap();
		one.add(new NVBlob("bytes", new byte[]{1, 2, 3, 4, 5}));

		NVGenericMap two = new NVGenericMap();
		two.add(new NVBlob("bytes", new byte[]{1, 2, 3, 4, 5}));

		assert NVGenericMap.areEquals(one, two) : "Maps with same byte arrays should be equal";

		NVGenericMap three = new NVGenericMap();
		three.add(new NVBlob("bytes", new byte[]{1, 2, 3, 4, 6}));

		assert !NVGenericMap.areEquals(one, three) : "Maps with different byte arrays should not be equal";
	}

	@Test
	public void testAreEquals_WithNVEntity() {
		AddressDAO address1 = new AddressDAO();
		address1.setStreet("123 Main St");
		address1.setCity("Los Angeles");

		NVGenericMap one = new NVGenericMap();
		one.add(address1);

		AddressDAO address2 = new AddressDAO();
		address2.setStreet("123 Main St");
		address2.setCity("Los Angeles");

		NVGenericMap two = new NVGenericMap();
		two.add(address2);

		assert NVGenericMap.areEquals(one, two) : "Maps with equivalent NVEntity should be equal";
	}

	@Test
	public void testAreEquals_DeepCopyEquality() {
		NVGenericMap original = new NVGenericMap("original");
		original.add(new NVPair("string", "test"));
		original.add(new NVLong("long", 100L));
		original.add(new NVBoolean("flag", true));

		NVGenericMap innerMap = new NVGenericMap("nested");
		innerMap.add(new NVPair("inner", "value"));
		original.add(innerMap);

		NVGenericMap copy = NVGenericMap.copy(original, true);

		assert NVGenericMap.areEquals(original, copy) : "Deep copy should be equal to original";
	}
}
