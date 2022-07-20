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

import org.jsoup.Jsoup;
//import org.jsoup.safety.Whitelist;
import org.jsoup.safety.Safelist;

import org.zoxweb.server.http.HTTPCall;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.SharedStringUtil;

public class JsoupTest {

	public static void main(String[] args) {

        try {
			int index = 0;
			HTTPMessageConfigInterface hcc = HTTPMessageConfig.createAndInit(args[index++], null, HTTPMethod.GET);
			String htmlBody = SharedStringUtil.toString(new HTTPCall(hcc).sendRequest().getData());
		
			String filtered = Jsoup.clean(htmlBody, Safelist.relaxed());
			System.out.println("Different:" + htmlBody.equals(filtered));
			System.out.println("Different:" + filtered);
			System.out.println("Different:" + htmlBody);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
