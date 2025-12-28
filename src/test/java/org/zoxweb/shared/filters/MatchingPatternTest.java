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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.filters.PatternFilterLiterals;

public class MatchingPatternTest {

	public static void main(String[] args) {
		String REGEX = ".*";
		//String REGEX = "[*]*[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*";
		String[] list = {"*temp", "temp", "*", "/temp/"};
		
		for (int i = 0; i < list.length; i++) {
			if (list[i].matches(REGEX)) {
				System.out.println(i + ":" + list[i]);
			} else {
				System.out.println(i + ":" + "Invalid");
			}
		}

		String REGEX2 = PatternFilterLiterals.SINGLETON.validateWithExclusion("te(*", "*");
		System.out.println(REGEX2);
		REGEX2 = REGEX2.replace("*", ".*");		
		//String REGEX2 = "te\\(.*";
		String[] list2 = {"*temp", "temp", "*", "/temp/", "tempmarwan", "tmustapha", "te(nael", "te (1)"};
		System.out.println(REGEX2);

		for (int i = 0; i < list2.length; i++) {
			if(list2[i].matches(REGEX2)) {
				System.out.println(i + ":" + list2[i]);
			} else {
				System.out.println(i + ":" + "Invalid");
			}
		}
	}
	
}
