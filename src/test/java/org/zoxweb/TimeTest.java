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

import org.zoxweb.server.util.DateUtil;
import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.util.Const;

import java.util.Date;

public class TimeTest {

	public static void main(String[] args) {
		for (String time: args) {
			try
			{
				System.out.println(time);
				
				System.out.println(Const.TimeInMillis.toMillis(time)+" millis");
			}
			catch( Exception e)
			{
				e.printStackTrace();
			}
		}
		
		long[] time = {Const.TimeInMillis.toNanos("1millis"), 1500000, 5000, 480000, Const.TimeInMillis.toNanos("4 millis"), 200000, 450000, Const.TimeInMillis.toNanos("7millis")};
		
		for (int j = 0; j < 10; j++){
			for (int i = 0; i < time.length; i++) {
				long ts = ServerUtil.delay(time[i]);
				System.out.println(j + ":Expected Time: " + time[i] + "  Actual Time: " + Const.TimeInMillis.nanosToString(ts));
			}
		}

		Date date = new Date();
		System.out.println(DateUtil.DEFAULT_GMT_MILLIS.format(date));
		System.out.println(DateUtil.DEFAULT_ZULU_MILLIS.format(date));


		long millis = System.currentTimeMillis();
		long nanos = System.nanoTime();
		System.out.println(millis + " millis, " + nanos + " nanos");
		long newValue = millis *1000_000 + nanos%1000_000;
		System.out.println(newValue);
		newValue = millis *1000 + (nanos%1000_000)/1000;
		System.out.println(newValue);
		newValue = 1000 + (0%1000);
		System.out.println(newValue);
	
	}
}
