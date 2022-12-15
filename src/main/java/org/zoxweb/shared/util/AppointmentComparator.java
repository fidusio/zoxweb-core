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

import java.util.Comparator;


public class AppointmentComparator
    implements Comparator<Appointment>
{


	
	public AppointmentComparator()
    {

	}


	/**
	 * Compares in micros and return sigmun result [-1, 0 , 1]
	 * Note: comparators should always return sing num values otherwise sorting could get funky.
	 * @param o1 to compare
	 * @param o2 compare to
	 * @return
	 * @exception NullPointerException if o1 or o2 are null
	 */
	@Override
	public int compare(Appointment o1, Appointment o2)
    {
		SharedUtil.checkIfNulls("Values can not be null",o1, o2);
		return SharedUtil.signum(o1.getPreciseExpiration() - o2.getPreciseExpiration());
	}

}