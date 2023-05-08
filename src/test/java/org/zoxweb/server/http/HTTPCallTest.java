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


import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMediaType;
import org.zoxweb.shared.http.HTTPResponseData;


public class HTTPCallTest {

  public static void main(String[] args) {

    try {


      int index = 0;
      String json = IOUtil.inputStreamToString(args[index++]);
      HTTPMessageConfigInterface hcc = GSONUtil.fromJSON(json);



      hcc.setRedirectEnabled(true);


      HTTPCall call = new HTTPCall(hcc);
      long ts = System.currentTimeMillis();
      long delta = 0;

      try {
        HTTPResponseData rd = call.sendRequest();
        delta = System.currentTimeMillis() - ts;

        System.out.println(rd);
      } catch (Exception e) {
        delta = System.currentTimeMillis() - ts;
        e.printStackTrace();
      }

      System.out.println("send request took:" + (delta));
      //hcc.setContent("Jello".getBytes());

      try {
        json = GSONUtil.toJSON((HTTPMessageConfig) hcc, true, true, true);
        System.out.println(json);

        HTTPMessageConfig hccFromJson = GSONUtil.fromJSON(json);
        System.out.println(hccFromJson);
        String json2 = GSONUtil.toJSON(hccFromJson, true, true, true);
        System.out.println("json equals:" + json.equals(json2));
      } catch (Exception e) {
        e.printStackTrace();
      }

      /// multi part test

      HTTPMessageConfig multiHCC = new HTTPMessageConfig();
      System.out.println("Empty:" + multiHCC.isMultiPartEncoding());

      multiHCC.setContentType(HTTPMediaType.TEXT_PLAIN.getValue());
      System.out.println("Text:" + multiHCC.isMultiPartEncoding());
      System.out.println(multiHCC);

      multiHCC.setContentType(HTTPMediaType.MULTIPART_FORM_DATA);
      System.out.println("MULTIPART:" + multiHCC.isMultiPartEncoding());

      System.out.println(HTTPMediaType.lookup("Application/JSon ; charset=utf-8"));
      System.out.println(multiHCC);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

  }

}
