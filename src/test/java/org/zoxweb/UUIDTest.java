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
package org.zoxweb;

import org.zoxweb.shared.util.BytesValue;
import org.zoxweb.shared.util.SharedBase64;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDTest {


	public static void main(String[] args) {
		long most  = System.nanoTime();
		long least = System.nanoTime();
		int size = 10;
		long listNanos[] = new long[size];
		
		for (int i = 0; i < size; i++) {
			listNanos[i] =System.nanoTime();
		}
		
		UUID uuid = new UUID(most, least);
		
		System.out.println( uuid );
		System.out.println("most\t:" + most);
		System.out.println("least\t:" + least);

		long last = 0L;

		for (long n : listNanos) {
			System.out.println( n + " last equals " + ( last == n) + " " + UUID.randomUUID() + " " + Long.toHexString(n));
			last = n;
		}

		 uuid = UUID.fromString("94ecdf50-cdb9-4668-941b-6005108b4840");
		 System.out.println( uuid + " most " + uuid.getMostSignificantBits() + " least " + uuid.getLeastSignificantBits());


		byte result[] = SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, "x3JJHMbDL1EzLkh9GBhXDw==");

		System.out.println(convertBytesToUUID(result));

		System.out.println("" + result.length);
		uuid = new UUID(BytesValue.LONG.toValue(result, 0), BytesValue.LONG.toValue(result, 8));
		System.out.println(uuid);

		uuid = UUID.fromString("94ecdf50-cdb9-4668-941b-6005108b4840");
		System.out.println("\n" + uuid);
		result = BytesValue.LONG.toBytes(new byte[16], 0,  uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
		byte[] leastBytes = BytesValue.LONG.toBytes(uuid.getLeastSignificantBits());
		System.out.println(uuid.getLeastSignificantBits() + " " + BytesValue.LONG.toValue(leastBytes));

		byte[] mostBytes = BytesValue.LONG.toBytes(uuid.getMostSignificantBits());
		System.out.println(uuid.getMostSignificantBits() + " " + BytesValue.LONG.toValue(mostBytes));
		uuid = new UUID(BytesValue.LONG.toValue(mostBytes), BytesValue.LONG.toValue(leastBytes)) ;
		System.out.println(uuid);

		String webSocketTag =  "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	}


	public static UUID convertBytesToUUID(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		long high = byteBuffer.getLong();
		long low = byteBuffer.getLong();
		return new UUID(high, low);
	}
}