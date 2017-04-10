/*
 * Copyright (c) 2012-Jul 30, 2014 ZoxWeb.com LLC.
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

import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarTest {

	public static void main(String[] args) {
		Calendar cal = new GregorianCalendar();
		Calendar calToCheck = new GregorianCalendar();
		
		calToCheck.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 13);
		System.out.println(cal.getTime());
		System.out.println(calToCheck.getTime());
	}
	
}