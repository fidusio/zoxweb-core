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
package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.security.SSLCheckDisabler;
import org.zoxweb.shared.http.HTTPHeaderName;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.Const.DeviceType;
import org.zoxweb.shared.util.SharedUtil;

public class HTTPProxyTest {

  public static void main(String... args) {

    String messages[] =
        {
            "connect zoxweb.com:443 HTTP/1.1\r\n" +
                "accept: */*\r\n" +
                "\r\n",
            "connect zoxweb.com:443 HTTP/1.1\r\n" +
                "accept: */*\r\n",
            "post zoxweb.com:443 HTTP/1.1\r\n" +
                "accept: */*\r\n" +
                "\r\n" +
                "simple body",
            "patch zoxweb.com:443 HTTP/1.1\r\n" +
                "accept: */*\r\n" +
                "content-length: 25\r\n" +
                "user-agent: Ipad from 1970\r\n" +
                "\r\n" +
                "simple body",

        };

    for (String message : messages) {
      try {
        UByteArrayOutputStream ubaos = new UByteArrayOutputStream(message);

        HTTPMessageConfigInterface hcc = HTTPUtil.parseRawHTTPRequest(ubaos, null, true);
        System.out.println(hcc);
				if (hcc != null) {
					System.out.println((hcc.getContent() != null ? new String(hcc.getContent()) : "") + ":" +
							hcc.getContentLength());
				}
        System.out.println("Is Request complete " + HTTPUtil.checkRequestStatus(hcc));
        ubaos.write(" Shou ya khara");

        hcc = HTTPUtil.parseRawHTTPRequest(ubaos, hcc, false);
        System.out.println(hcc);
        if (hcc != null) {
          System.out.println(
              (hcc.getContent() != null ? new String(hcc.getContent()) : "") + ":" + hcc
                  .getContentLength());
          System.out.println(DeviceType.lookup(SharedUtil
              .lookupValue(hcc.getHeaderParameters(), HTTPHeaderName.USER_AGENT.getName())));
        }
        System.out.println("Is Request complete " + HTTPUtil.checkRequestStatus(hcc));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      int index = 0;
      InetSocketAddressDAO proxys[] = {new InetSocketAddressDAO(args[index++]),
          new InetSocketAddressDAO(args[index++])};
      HTTPMessageConfigInterface hcc = HTTPMessageConfig
          .createAndInit(args[index++], null, HTTPMethod.GET);
      int loopCount = Integer.parseInt(args[index++]);
      //hcc.getParameters().add(new NVPair("param", "value"));

      HTTPCall hc = new HTTPCall(hcc, SSLCheckDisabler.SINGLETON);

      long delta;
      byte[][] contents = new byte[2][];
      long deltas[] = new long[2];
      long deltaTotal = 0;

      for (int i = 0; i < loopCount; i++) {
        for (int j = 0; j < proxys.length; j++) {

          InetSocketAddressDAO p = proxys[j];
          contents[j] = null;
          try {
            hcc.setProxyAddress(p);
            delta = System.currentTimeMillis();

            contents[j] = hc.sendRequest().getData();
            delta = System.currentTimeMillis() - delta;
            deltas[j] = delta;
            System.out.println(p + " delta " + delta);
          } catch (Exception e) {
            e.printStackTrace();
            System.out.println(p + " " + e);
          }
        }

        try {
          if (i > 5) {
            boolean result = SharedUtil.slowEquals(contents[0], contents[1]);
						if (result) {
							deltaTotal += deltas[0] - deltas[1];
						}
            System.out.println("Result " + result + " difference " + (deltas[0] - deltas[1]));
          }
        } catch (Exception e) {

        }
      }

      System.out.println("Total: " + deltaTotal);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
