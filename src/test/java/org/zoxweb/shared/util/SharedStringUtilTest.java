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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SharedStringUtilTest {

	@Test
	public void testTruncate() {
		String str = "Hello, my name is John Smith.";
		String result = SharedStringUtil.truncate(str, str.length());
		assertNotNull(result);
		assertEquals("Hello, my name is John Smith.", result);

		str = "Hello, my name is John Smith.";
		result = SharedStringUtil.truncate(str, 5);
		assertNotNull(result);
		assertEquals("Hello", result);

		result = SharedStringUtil.truncate(null, 0);
		assertNull(result);
	}
	
	@Test
	public void testSplit()
	{
		System.out.println("After right:" + SharedStringUtil.valueAfterRightToken("john.smith.dickson", "."));
		System.out.println("Before right:" + SharedStringUtil.valueBeforeRightToken("john.smith.dickson", "."));
		System.out.println("Before left:" + SharedStringUtil.valueBeforeLeftToken("john.smith.dickson", "."));
		System.out.println("After left:" + SharedStringUtil.valueAfterLeftToken("john.smith.dickson", "."));
	}
	
	@Test
	public void testByteToHex()
	{
		byte [] buffer = {0,1,2,3,4,5,6,7,0,9,10,100,-1};
		String str = SharedStringUtil.bytesToHex(buffer, "::");
		System.out.println(str + " length:" + str.length());
		str = SharedStringUtil.bytesToHex(buffer);
		System.out.println(str + " length:" + str.length());
	}

	@Test public void parseStrings()
	{
		String[] values = {
			"name=v1",
			"name2=v2",
			"name3=v3"
		};

		List<GetNameValue<String>> parsed = SharedStringUtil.parseStrings('=', values);
		assert(parsed.size() == values.length);
		System.out.println(parsed);
	}

	@Test public void spaceChars()
	{
		String test = "Hello World";
		System.out.println(SharedStringUtil.spaceChars(test, "*"));
	}

	@Test public void repeatSequence()
	{
		String test = "Hello World";
		System.out.println(SharedStringUtil.repeatSequence(test, 3, "*"));
		System.out.println(SharedStringUtil.repeatSequence(test, 3));
	}

}