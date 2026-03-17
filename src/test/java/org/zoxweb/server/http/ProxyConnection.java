package org.zoxweb.server.http;

import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class ProxyConnection {

  public static void main(String... args) {
    try {
      int index = 0;
      String proxy = args[index++];

      IPAddress proxyAddress = new IPAddress(proxy);

      Map<Long, String> results = new ConcurrentSkipListMap<Long, String>();
      for (int i = 1; i < args.length; i++) {
        HTTPMessageConfigInterface hcc = HTTPMessageConfig
            .createAndInit(args[i], null, HTTPMethod.GET, false);
        hcc.setProxyAddress(proxyAddress);
        HTTPCall hc = new HTTPCall(hcc);
        long ts = System.currentTimeMillis();
        HTTPResponseData rd = hc.sendRequest();
        ts = System.currentTimeMillis() - ts;
        System.out.println(HTTPStatusCode.statusByCode(rd.getStatus()));

        String str = SUS.toCanonicalID(' ', proxy, args[i]);
        SharedUtil.putUnique(results, ts,
            rd.getStatus() + " Command: " + str + " it took " + ts + " millis");
      }
      for (String str : results.values()) {
        System.out.println(str);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
