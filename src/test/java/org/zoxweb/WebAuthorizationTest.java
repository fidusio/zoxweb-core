/*
 * Copyright (c) 2012-Oct 16, 2015 ZoxWeb.com LLC.
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

import java.util.ArrayList;
import java.util.List;

import org.zoxweb.server.http.HTTPCall;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPAuthorizationType;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.GetNameValue;

public class WebAuthorizationTest {

	public static void main(String[] args) {

		try {
			HTTPMessageConfig hcc = new HTTPMessageConfig();
			
			int index = 0;
			hcc.setURL(args[index++]);
			hcc.setURI(args[index++]);
			
			hcc.setMethod(HTTPMethod.POST);
			
			List<GetNameValue<String>> tokens = new ArrayList<GetNameValue<String>>();
			tokens.add(HTTPAuthorizationType.BASIC.toHTTPHeader("userName", ":passwordValue"));
			
			tokens.add(HTTPAuthorizationType.BEARER.toHTTPHeader("tokenValue"));
			tokens.add(HTTPAuthorizationType.BASIC.toHTTPHeader("userName", null));
			tokens.add(HTTPAuthorizationType.BASIC.toHTTPHeader(null, null));
			
			for (GetNameValue<String> token : tokens) {
				System.out.println(token + " " + HTTPAuthorizationType.parse(token));
				hcc.setAuthentication(HTTPAuthorizationType.parse(token));
				System.out.println(GSONUtil.toJSON(hcc, true, false, true));
				HTTPCall hc = new HTTPCall(hcc);
				System.out.println(hc.sendRequest());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}