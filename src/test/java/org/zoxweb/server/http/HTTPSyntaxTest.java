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

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.zoxweb.shared.http.URIScheme;

public class HTTPSyntaxTest {

  public static void testURIParsing() {

    String uris[] =
        {
            "https://yahoo.com",
            "http://zoxweb.com:7070/?name1=val1",
            "http://zoxweb.com/?name1=val1",
            "goole.com:443",
            "xlogistx.com",
            "www.xlogistx.com:8443",
            "http://user:password@zoxweb.com/hello",
            "wss://user:password@zoxweb.com/hello",
            "http://user:password@zoxweb.com/hello@",
            "http://zoxweb.com/hello@",
            "http://ma67-c.analytics.edgesuite.net/9.gif?a=P~b=cfc2b1573641a1482~c=A4F818EC99AEF820AB004F04B78279B8BAE49DDE~d=9B91F5007E3FBFE6D9C27DB5145967C38053E427~e=3~g=0~w=23817~ac=cnn/big/us/2016/05/18/pentagon-chinese-jets-unsafe-intercept-sot-todd-tsr.cnn_530364_,512x288_55,640x360_90,768x432_130,896x504_185,1280x720_350,0k.mp4.csmil/bitrate=0~ag=www.cnn.com~ah=www.cnn.com~ak=Flash_PlugIn~al=Windows%207~am=H~aw=http://cnn-f.akamaihd.net/cnn/big/us/2016/05/18/pentagon-chinese-jets-unsafe-intercept-sot-todd-tsr.cnn_530364_,512x288_55,640x360_90,768x432_130,896x504_185,1280x720_350,0k.mp4.csmil/bitrate=2~ax=O~ay=csma-3.6.5:osmfLoader-1.4.13~bb=96.17.202.7~cm=Akamai~dx=16.74~en=cnn/big/us/2016/05/18/pentagon-chinese-jets-unsafe-intercept-sot-todd-tsr.cnn_530364_,512x288_55,640x360_90,768x432_130,896x504_185,1280x720_350,0k.mp4.csmil/bitrate=~m=E~n=12552977.3729:13~u=2~v=12926~x=6737~y=7503~z=1~aa=cnn-f.akamaihd.net~ap=1929~aq=93901~da=10003~db=388.8465:13~dc=0~dd=0~de=0~dg=0~dh=0~dn=0:33~du=330000:33~dw=1~dy=0:1~fb=0~fd=0~fe=0~ff=0~fi=1300000:0:6737::1~fk=S:6737,Q:0,D:0~fl=8758100000:6737~rs=0:-1;6737:10003~tt=cnn/big/us/2016/05/18/pentagon-chinese-jets-unsafe-intercept-sot-todd-tsr.cnn_530364_,512x288_55,640x360_90,768x432_130,896x504_185,1280x720_350,0k.mp4.csmil/bitrate=~",
        };

    for (String str : uris) {
      try {
        System.out.println("Full URL: " + str);
        System.out.println("Host Info: " + HTTPUtil.parseHost(str, 80));
        System.out.println("URIScheme: " + URIScheme.match(str));
        String uri = HTTPUtil.parseURI(str);
        System.out.println("URI[" + uri.length() + "]: " + uri);
        System.out.println(new URI(str));
        String uriEncoded = URLEncoder.encode(str, "utf-8");
        String uriDecoded = URLDecoder.decode(str, "utf-8");
        System.out.println(str.equals(uriEncoded) + " " + uriEncoded);
        System.out.println(str + "\n" + uriDecoded);
        System.out.println("===================================================");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    testURIParsing();
  }

}