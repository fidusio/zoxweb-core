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
package org.zoxweb.shared.util;

import org.zoxweb.shared.http.HTTPMediaType;
import org.zoxweb.shared.http.HTTPAttribute;

import java.util.Arrays;

public class StringFormatTest {

	public static void main(String[] args) {
		try {
			String[][] params = {
					{HTTPMediaType.APPLICATION_JSON.getValue(), HTTPAttribute.CHARSET_UTF8.getValue()},
					null,
					{"", "mara", "fdr", " ", "test"},
			};

			for (String values[] : params) {
				try {
					System.out.println(SharedStringUtil.formatStringValues("; ", values));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			System.out.println(SharedStringUtil.formatStringValues("; ", HTTPMediaType.APPLICATION_JSON, HTTPAttribute.CHARSET_UTF8));

			String toBeParsed[] = {
					"User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:44.0) Gecko/20100101 Firefox/44.0",
					"Proxy-Connection: keep-alive",
					"Connection: keep-alive",
					"Host: comet.yahoo.com:443",
					"Accept:",
				};
			
			for (String str : toBeParsed) {
				System.out.println(SharedUtil.toNVPair(str, ":", true));
				System.out.println(SharedUtil.toNVPair(str, ":", false));
			}
			String roles = "role-1, role-2, role-3,   role-4       ";
			System.out.println(Arrays.toString(SharedStringUtil.parseString(roles, ",",  " ")));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}