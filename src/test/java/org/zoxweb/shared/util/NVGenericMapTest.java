package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.http.HTTPEndPoint;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.io.IOException;
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
	public void composedtest()
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
}
