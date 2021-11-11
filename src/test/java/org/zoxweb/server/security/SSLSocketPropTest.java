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
package org.zoxweb.server.security;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

public class SSLSocketPropTest {

  public static void main(String[] args) {

    try {
      URL url = new URL("https://management.rubuspi.com:7443/manage");
      URLConnection con = url.openConnection();
      SSLCheckDisabler.SINGLETON.updateURLConnection(con);
      Reader reader = new InputStreamReader(con.getInputStream());
      while (true) {
        int ch = reader.read();
        if (ch == -1) {
          break;
        }
        System.out.print((char) ch);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
