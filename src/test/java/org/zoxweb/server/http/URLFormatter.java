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

import java.net.URL;

public class URLFormatter {

  public static void main(String[] args) {

    for (String arg : args) {
      try {
        System.out.println("Trying to parse: " + arg);
        URL url = new URL(arg);
        System.out.println(HTTPUtil.parseHostURL(url));
        System.out.println(HTTPUtil.parseURI(url, false));
        System.out.println(HTTPUtil.parseURI(url, true));
        System.out.println(HTTPUtil.parseURL(url));
        System.out.println(url.getQuery());

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
