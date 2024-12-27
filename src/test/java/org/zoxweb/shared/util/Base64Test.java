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

import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.UUID;

//import org.apache.commons.codec.binary.Base64;

public class Base64Test {

	public static void main(String[] arg) {
		byte[] num = {0, 1, 2};

		String toConvert = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";

		System.out.println(toConvert.length());
		//System.out.println(SharedBase64.computeBase64ArraySize(num));
		System.out.println(new String(SharedBase64.encode(num)));

		long tsOld = System.nanoTime();
		byte[] oldBase64 = SharedBase64.encode(toConvert.getBytes());

		byte[] decodedBase64 = SharedBase64.decode(oldBase64);

		tsOld = System.nanoTime() - tsOld;
		System.out.println(oldBase64.length);
		System.out.println(new String(oldBase64));
		System.out.println(new String(decodedBase64));
		System.out.println(tsOld + "nanos");

		String[] testArray = {"1", "12", "123", "1234", "12345", "123456", "1234567", "12345678", "123456789", "1234567890",
				"a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg", "abcdefgh", "abcdefghi", "abcdefghij", toConvert, "asure.", "sure.", "any carnal pleasure"};

		String partial = "naelmarwan";


		System.out.println("Length of Array: " + testArray.length);
		long tsJ, tsZW;
		byte[] sharedEncodedeBase64, sharedDecodedBase64,java64Encoded,java64Decodded;
		Encoder javaEncoder = java.util.Base64.getEncoder();
		Decoder javaDecoder = java.util.Base64.getDecoder();
		for (int i = 0; i < testArray.length; i++) {
			System.out.println(i + 1 + "." + "Original Value: " + testArray[i]);

			byte toEncode[] = testArray[i].getBytes();
			tsZW = System.nanoTime();
			sharedEncodedeBase64 = SharedBase64.encode(Base64Type.DEFAULT, toEncode, 0, toEncode.length);
			sharedDecodedBase64 = SharedBase64.decode(Base64Type.DEFAULT, sharedEncodedeBase64, 0, sharedEncodedeBase64.length);
			tsZW = System.nanoTime() - tsZW;

			tsZW = System.nanoTime();
			sharedEncodedeBase64 = SharedBase64.encode(Base64Type.DEFAULT, toEncode, 0, toEncode.length);
			sharedDecodedBase64 = SharedBase64.decode(Base64Type.DEFAULT, sharedEncodedeBase64, 0, sharedEncodedeBase64.length);
			tsZW = System.nanoTime() - tsZW;


			tsJ = System.nanoTime();
			java64Encoded = javaEncoder.encode(toEncode);//.encodeBase64(toEncode);
			java64Decodded = javaDecoder.decode(java64Encoded);
			tsJ = System.nanoTime() - tsJ;

			tsJ = System.nanoTime();
			java64Encoded = javaEncoder.encode(toEncode);//.encodeBase64(toEncode);
			java64Decodded = javaDecoder.decode(java64Encoded);
			tsJ = System.nanoTime() - tsJ;

			System.out.println(i + 1 + "." + "Encode Base64: " + new String(sharedEncodedeBase64) + ":\t\t" +
					Arrays.equals(sharedEncodedeBase64, java64Encoded));
			System.out.println(i + 1 + "." + "Decoded Base64: " + new String(sharedDecodedBase64) + ":\t\t" +
					 Arrays.equals(sharedDecodedBase64, java64Decodded));
			System.out.println("zoxweb:" + tsZW + " java:" + tsJ + " delta:" + (tsJ - tsZW) + " factor:" + ((float) tsJ / (float) tsZW));
			System.out.println(tsZW + " nanos: " + testArray[i].equals(new String(sharedDecodedBase64)));
		}

		byte[] byteArray = new byte[256];

		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = (byte) i;
		}

		byte[] byteArrayBase64 = SharedBase64.encode(byteArray);
		System.out.println(new String(byteArrayBase64));
		System.out.println("Original Array length:" + byteArray.length + " base 64 length:" + byteArrayBase64.length);
		byte[] byteArrayOriginal = SharedBase64.decode(byteArrayBase64);
		System.out.println(Arrays.equals(byteArray, byteArrayOriginal));

		System.out.println("BASE_64 length:" + SharedBase64.BASE_64.length + " REVERSE_BASE_64 length:" + SharedBase64.REVERSE_BASE_64.length);

		System.out.println("Partial test:" + new String(SharedBase64.encode(partial.getBytes(), 4, 6)) + "," + new String(SharedBase64.encode(partial.getBytes(), 1, 1)));
		byte fullname[] = SharedBase64.encode(partial.getBytes());
		System.out.println("Marwan " + new String(fullname));
		
		

		System.out.println("Equals:" + SUS.equals("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(), SharedBase64.BASE_64));
		
		try
		{
			byte[] b64 = SharedBase64.encode("1234567890");
			String str = SharedStringUtil.toString(b64);
			System.out.println("1234567890 b64:" + str);
			System.out.println(SharedBase64.decodeAsString(null, "MTIzNDU2Nzg5MA"));
			System.out.println(SharedBase64.decodeAsString(null, "MTIzNDU2Nzg5MA="));
			str+="^";
			System.out.println(SharedBase64.validate(str));
			b64 = SharedBase64.decode(str);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		UUID uuid = UUID.randomUUID();
		
		byte[] uuidBytes = BytesValue.LONG.toBytes(null, 0, uuid.getMostSignificantBits(), uuid.getMostSignificantBits());
		String str = SharedStringUtil.toString(SharedBase64.encode(Base64Type.URL, uuidBytes));
		String str1 = Base64.getUrlEncoder().encodeToString(uuidBytes); 
		System.out.println(str + " " + str1 + " " + str1.equals(str));
		
		byte[] b  = SharedBase64.decode(Base64Type.URL, str);
		byte[] b1 = Base64.getUrlDecoder().decode(str);
		System.out.println(Arrays.equals(uuidBytes, b) + " " + Arrays.equals(uuidBytes, b1));
		str = SharedStringUtil.toString(SharedBase64.encode(Base64Type.URL, b));
		System.out.println(str);

		String base64URL = "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ";
		String plain = SharedBase64.decodeAsString(Base64Type.URL, base64URL);
		System.out.println(plain);
		System.out.println(SharedBase64.encodeAsString(Base64Type.URL, plain));


		System.out.println(SharedBase64.REVERSE_BASE_64.length + " " + SharedBase64.URL_REVERSE_BASE_64.length);
	}

}