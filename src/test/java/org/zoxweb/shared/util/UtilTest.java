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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.zoxweb.server.http.HTTPMultiPartUtil;


public class UtilTest {

	public static void main(String[] args) {
		byte buffer[] = SharedStringUtil.hexToBytes("bede0001");
		byte buffer1[] = SharedStringUtil.hexToBytes("0xbede0001");
		System.out.println( SharedStringUtil.bytesToHex(buffer1) + ":" + SharedStringUtil.bytesToHex(buffer));
		long ts1 = System.nanoTime();
		long ts2 = System.nanoTime();
		System.out.println(":delta " +(ts2 -ts1));
		
		long array[] = new long[100];

		for (int i=0; i < array.length; i++) {
			array[i] = System.nanoTime();
		}
		
		for (int i=0; i < array.length; i++) {
			if (i > 0) {
			    System.out.println(i + ":delta " + (array[i] - array[i - 1]));
            }
		}
		
		TimeUnit tut = TimeUnit.NANOSECONDS;
		long ts = System.nanoTime();
		System.out.println(HTTPMultiPartUtil.generateBoundary( tut));
		ts = System.nanoTime() -ts;
		
		System.out.println(tut  + ":" + ts);

		long total = 0;

		for (int i =0; i < 10; i++) {
			if (i%2 == 0) {
				tut = TimeUnit.MILLISECONDS;
			} else {
                tut = TimeUnit.NANOSECONDS;
            }
			
			ts = System.nanoTime();
			String boundary  = HTTPMultiPartUtil.generateBoundary(tut);
			
			ts =  System.nanoTime() -ts;
			total+=ts;
			System.out.println(  i + ":" + ts + ":" +(total/(i+1)) + ":" + boundary +":" + tut);
		}
		
		try {
			System.getProperties().store(System.out, "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<NVPair> result =  SharedUtil.toNVPairs("n1=v&n2=v", "=", "&");

		System.out.println(result);
		
		result =  SharedUtil.toNVPairs("pam_unix(sshd:auth): authentication failure; logname= uid=0 euid=0 tty=ssh ruser= rhost=196.43.230.86  user=root", "=", " ");
		System.out.println(result);

		System.out.println(SharedUtil.toCanonicalID('-', null, "a", null));
		System.out.println(SharedUtil.toCanonicalID(true, '-', "a", null));
		System.out.println(SharedUtil.toCanonicalID(false, '-', "a", null, "b"));
		String concat = SharedUtil.toCanonicalID(true, '-', "a", null, "b");
		System.out.println(concat);
		String parsedContact[] = SharedStringUtil.parseString(concat, "-");
		System.out.println(Arrays.toString(parsedContact));
	}

}