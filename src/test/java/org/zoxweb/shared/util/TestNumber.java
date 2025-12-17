package org.zoxweb.shared.util;



//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.UUID;

public class TestNumber {

	static String longMax = "" + Long.MAX_VALUE;
	static String longMin = "" + Long.MAX_VALUE;
	static String intMax = "" + Integer.MAX_VALUE;
	static String intMin = "" + Integer.MIN_VALUE;
	static String maxDouble = "" + Double.MAX_VALUE;
	static String maxIntPlusOne = "2147483648";
	static String d = "0.044198288480142844";
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

	@Test
	public void longBase64Conversion()
	{
		byte[] longArray = BytesValue.LONG.toBytes(Long.MAX_VALUE);
		String result = SharedBase64.encodeAsString(Base64Type.URL, longArray);
		System.out.println(result + ", " + result.length());
	}

	@Test
	public void testDoubleConversion()
	{
		System.out.println("double:" + Double.parseDouble(d));
		System.out.println("float :" + Float.parseFloat(d));
	}


	@Test
	public void base27ToString()
	{
		long[] longSet = {
				0, 10, 100, 1000, 10000, 1000000, Long.MAX_VALUE, 1, 2, 4, 8, 16, 31, 32, 64, 128, 256, 512, 1024
		};

		for(long val : longSet)
		{
			String str = NumSet.Base27.SINGLETON.toString(val);
			String str1 = "" + NumSet.Base27.SINGLETON.getLong(str);
			System.out.println(val +", "  +  str + ", " + str1 + ", " + str1.equals(""+val) + ", converted length: " + str.length());
		}
	}


	@Test
	public void base27ToLong()
	{

		String[] stringSet = {
				"Aaaab",
				NumSet.Base27.SINGLETON.toString(Long.MAX_VALUE),
		};

		for(String val : stringSet)
		{

			System.out.println(val +", "  +  NumSet.Base27.SINGLETON.getLong(val));

		}
	}


	@Test
	public void base27UUID()
	{
		for (int i = 0; i < 100; i++) {
			UUID uuid = UUID.randomUUID();
			String uuidStr = uuid.toString();
			String str = NumSet.Base27.SINGLETON.toString(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
			System.out.println(uuid + ", " + uuidStr.length() + ", " + str + ", " + str.length());
		}
	}

	@Test
	public void parseInt()
	{
		System.out.println(SharedUtil.parseInt("25"));
		System.out.println(SharedUtil.parseInt("0xff"));

		System.out.println(SharedUtil.parseInt("a"));
		System.out.println(SharedUtil.parseInt("0x2000"));
		System.out.println(Integer.parseInt("2000", 16));
	}


	@Test
	public void parseShort()
	{
		System.out.println(SharedUtil.parseShort("25"));
		assert(SharedUtil.parseShort("0Xfaa") == SharedUtil.parseShort("0xfAA"));

	}

	@Test
	public void parseLong()
	{
		System.out.println(SharedUtil.parseLong("25"));
		assert(SharedUtil.parseLong("0Xffaa") == SharedUtil.parseLong("0xffAA"));

	}

	@Test
	public void testIntToBytes()
	{
		int toSend = Integer.MAX_VALUE;
		byte[] array = {(byte) ((toSend >> 24) & 0xff), (byte) ((toSend >> 16) & 0xff), (byte) ((toSend >> 8) & 0xff), (byte) ((toSend) & 0xff)};
		System.out.println(SharedStringUtil.bytesToHex(array));
		System.out.println((byte) ((toSend >> 24) & 0xff));
		System.out.println((byte) ((toSend >> 16) & 0xff));
		System.out.println((byte) ((toSend >> 8) & 0xff));
		System.out.println((byte) ((toSend >> 0) & 0xff));
		System.out.println(BytesValue.INT.toValue(array) + " " + BytesValue.INT.toValue(array).equals(toSend));
	}


	@Test
	public void testShortToBytes()
	{
		short toSend = Short.MAX_VALUE;
		byte[] array = {(byte) ((toSend >> 8) & 0xff), (byte) ((toSend) & 0xff)};
		System.out.println(SharedStringUtil.bytesToHex(array));
		System.out.println((byte) ((toSend >> 8) & 0xff));
		System.out.println((byte) ((toSend >> 0) & 0xff));
		System.out.println(BytesValue.SHORT.toValue(array) + " " + BytesValue.SHORT.toValue(array).equals(toSend));
	}

	@Test
	public void testLongToBytes()
	{
		long toSend = Long.MAX_VALUE;
		byte[] array = {
				(byte) ((toSend >> 56) & 0xff), (byte) ((toSend >> 48) & 0xff), (byte) ((toSend >> 40) & 0xff), (byte) ((toSend>>32) & 0xff),
				(byte) ((toSend >> 24) & 0xff), (byte) ((toSend >> 16) & 0xff), (byte) ((toSend >> 8) & 0xff), (byte) ((toSend) & 0xff)
		};
		System.out.println(SharedStringUtil.bytesToHex(array));
		System.out.println((byte) ((toSend >> 56) & 0xff));
		System.out.println((byte) ((toSend >> 48) & 0xff));
		System.out.println((byte) ((toSend >> 40) & 0xff));
		System.out.println((byte) ((toSend >> 32) & 0xff));
		System.out.println((byte) ((toSend >> 24) & 0xff));
		System.out.println((byte) ((toSend >> 16) & 0xff));
		System.out.println((byte) ((toSend >> 8) & 0xff));
		System.out.println((byte) ((toSend >> 0) & 0xff));
		System.out.println(BytesValue.LONG.toValue(array) + " " + BytesValue.LONG.toValue(array).equals(toSend));
	}

}
