package org.zoxweb.server.http;


import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPMediaType;


public class HTTPCallSockTest {

  public static void main(String... args) {
    try {
      int index = 0;
      String url = args[index++];
      String uri = args[index++];
      HTTPMethod httpMethod =
          args.length > index ? HTTPMethod.lookup(args[index++]) : HTTPMethod.GET;

      //InetSocketAddressDAO proxy = new InetSocketAddressDAO("10.0.0.20", 2375, ProxyType.SOCKS);
      HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, uri, httpMethod);
      //hmci.setProxyAddress(proxy);
      hmci.setContentType(HTTPMediaType.APPLICATION_JSON);
      HTTPCall hc = new HTTPCall(hmci);
      System.out.println(hc.sendRequest());

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Usage: url uri [http method]");
    }
  }
}
