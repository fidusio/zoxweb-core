/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
package org.zoxweb.server.filters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatterTest {
	
	public static void main(String[] args) throws ParseException {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
			System.out.println("value  " + sdf.format(new Date()) + new Date());
		} catch(Exception e) {
			e.printStackTrace();
		}

		SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println( new Date());

		String[] dates = {"2014-05-28", "1960-5-20", "1900-8-4", "0000-01-01", "-2500-01-01"};
	
		for (int i = 0; i < dates.length; i++) {
			Date date = DEFAULT_DATE_FORMAT.parse(dates[i]);
			System.out.println(date.getTime() + " " + date);
		}

		@SuppressWarnings("deprecation")
		Date date = new Date(114, 4, 29, 11, 14);
		System.out.println(date);
		
		long v = Long.parseLong("1434499245449");
		System.out.println( "date from db " + new Date(v));
		
		
		String patterns[] =
			{
				"yyyy-MM-dd 'Etc/'zzz",
				
				"yyyy-MM-dd 'Etc/'z",
				"yyyy-MM-dd 'Etc/'zz",
				"yyyy-MM-dd 'Etc/'zzzz",
				"yyyy-MM-dd",
				
			};
		
		
		String toParse ="2015-7-17 Etc/GMT+7";
		
		for (String pattern : patterns) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(pattern);
				
				sdf.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
				System.out.println("pattern:" + pattern);
				System.out.println( sdf.parse(toParse));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		DateTimeValueFilter dts = new DateTimeValueFilter("yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC");
		
		System.out.println(new Date(dts.validate("2015-06-24T22:15:19Z")));

		System.out.println(TimeZone.getDefault().getID());
	}
}
