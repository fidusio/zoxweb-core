package org.zoxweb.server.http;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.http.HTTPStatusCode;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.SharedUtil;

public class ProxyConnection {

  public static void main(String... args) {
    try {
      int index = 0;
      String proxy = args[index++];

      InetSocketAddressDAO proxyAddress = new InetSocketAddressDAO(proxy);

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

        String str = SharedUtil.toCanonicalID(' ', proxy, args[i]);
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
