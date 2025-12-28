package org.zoxweb.shared.util;


import org.junit.jupiter.api.Test;

public class StringArrayDecoderTest {

	NVCollectionStringDecoder nvasd = new NVCollectionStringDecoder("=", ",", true);
	@Test
	public void test() 
	{
		
		String values[] = {
			"n=v1,v2", "n=v", "n=,v2,,v4"
		};
		
		for(String v: values)
		{
			System.out.println(nvasd.decode(v));
		}
	}

}
