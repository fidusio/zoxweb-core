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
package org.zoxweb;

import org.zoxweb.server.http.HTTPCall;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPAuthScheme;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.GetNameValue;

import java.util.ArrayList;
import java.util.List;

public class WebAuthorizationTest {

    public static void main(String[] args) {

        try {
            HTTPMessageConfig hcc = new HTTPMessageConfig();

            int index = 0;
            hcc.setURL(args[index++]);
            hcc.setURI(args[index++]);

            hcc.setMethod(HTTPMethod.POST);

            List<GetNameValue<String>> tokens = new ArrayList<GetNameValue<String>>();
            tokens.add(HTTPAuthScheme.BASIC.toHTTPHeader("userName", ":passwordValue"));

            tokens.add(HTTPAuthScheme.BEARER.toHTTPHeader("tokenValue"));
            tokens.add(HTTPAuthScheme.BASIC.toHTTPHeader("userName", null));
            tokens.add(HTTPAuthScheme.BASIC.toHTTPHeader(null, null));

            for (GetNameValue<String> token : tokens) {
                System.out.println(token + " " + HTTPAuthScheme.parse(token));
                hcc.setAuthorization(HTTPAuthScheme.parse(token));
                System.out.println(GSONUtil.toJSON(hcc, true, false, true));
                HTTPCall hc = new HTTPCall(hcc);
                System.out.println(hc.sendRequest());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}