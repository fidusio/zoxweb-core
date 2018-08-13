package org.zoxweb.shared;



import org.junit.Test;
import org.zoxweb.shared.util.SharedUtil;

public class TestNumber {

	static String longMax = "" + Long.MAX_VALUE;
	static String longMin = "" + Long.MAX_VALUE;
	static String intMax = "" + Integer.MAX_VALUE;
	static String intMin = "" + Integer.MIN_VALUE;
	static String maxDouble = "" + Double.MAX_VALUE;
	static String maxIntPlusOne = "2147483648"; 
	@Test
	public void testLong() 
	{
		assert(SharedUtil.parseNumber(maxIntPlusOne) instanceof Long);
		assert(SharedUtil.parseNumber(longMax) instanceof Long);
		assert(SharedUtil.parseNumber(longMin) instanceof Long);
	}
	
	
	@Test
	public void testInt() 
	{
		
		assert(SharedUtil.parseNumber(intMax) instanceof Integer);
		assert(SharedUtil.parseNumber(intMin) instanceof Integer);
	}
	
	
	@Test
	public void testFloat()
	{
		assert(SharedUtil.parseNumber("23343.4534") instanceof Float);
	}
	
	@Test
	public void testDouble()
	{
		assert(SharedUtil.parseNumber(maxDouble) instanceof Double);
	}

}