package org.zoxweb.shared.data;





import java.io.IOException;
import java.util.concurrent.TimeUnit;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;


public class CurrentTimestampTest 
{

	@Test
	public void timeInMillis() throws IOException
	{
		CurrentTimestamp ct = new CurrentTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		String json = GSONUtil.toJSON(ct, true, false, false);
		System.out.println(json);
		CurrentTimestamp newCT = GSONUtil.fromJSON(json, CurrentTimestamp.class);
		String newJson= GSONUtil.toJSON(newCT, true, false, false);
		System.out.println(newJson);
		Assertions.assertEquals(json, newJson);
		
		
		ct = new CurrentTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS, "localhost");
		json = GSONUtil.toJSON(ct, true, false, true);
		System.out.println(json);
		newCT = GSONUtil.fromJSON(json, CurrentTimestamp.class);
		newJson= GSONUtil.toJSON(newCT, true, false, true);
		System.out.println(newJson);
		Assertions.assertEquals(json, newJson);
	}
	
	
	@Test
	public void timeInNanos() throws IOException
	{
		CurrentTimestamp ct = new CurrentTimestamp(System.nanoTime(), TimeUnit.NANOSECONDS);
		String json = GSONUtil.toJSON(ct, true, false, false);
		System.out.println(json);
		CurrentTimestamp newCT = GSONUtil.fromJSON(json, CurrentTimestamp.class);
		String newJson= GSONUtil.toJSON(newCT, true, false, false);
		System.out.println(newJson);
		Assertions.assertEquals(json, newJson);
		
		
		ct = new CurrentTimestamp(System.nanoTime(), TimeUnit.NANOSECONDS, "localhost");
		json = GSONUtil.toJSON(ct, true, false, true);
		System.out.println(json);
		newCT = GSONUtil.fromJSON(json, CurrentTimestamp.class);
		newJson= GSONUtil.toJSON(newCT, true, false, true);
		System.out.println(newJson);
		Assertions.assertEquals(json, newJson);
	}

}
