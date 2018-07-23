package org.zoxweb.shared.data;


import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.NVPairGetNameMap;

public class DeviceDAOTest {

	@Test
	public void testJSON()
			 throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		DeviceDAO device = new DeviceDAO();
		device.setName("Computer");
		device.setDeviceID(UUID.randomUUID().toString());
		device.getProperties().add(new NVPair("name", "value"));
		//device.getProperties().add(new NVPair("name", "value2"));
		device.getProperties().add(new NVPair("AAA", "triple"));
		String json = GSONUtil.toJSON(device, true);
		System.out.println(json);
		Class<?> propC = device.getProperties().getClass();
		DeviceDAO deviceReverse = GSONUtil.fromJSON(json);
		Class<?> propC1 = deviceReverse.getProperties().getClass();
		assert propC == propC1;
		NVPairGetNameMap val = (NVPairGetNameMap)((Object)deviceReverse.getProperties());
		System.out.println(propC1 + " " + val.getValue().getClass());
		
		
		
	}
}
