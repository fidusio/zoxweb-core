package org.zoxweb.shared.http;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.io.IOException;
import java.util.Arrays;

public class HTTPServerConfigTest {

  private static String ENV_VAR;
  @BeforeAll
  public static void init()
  {
    ENV_VAR = System.getenv("ENV_VAR");
  }

  @Test
  public void valueChecks() throws IOException {
    System.out.println(System.getProperties());
    HTTPServerConfig hsc = GSONUtil.fromJSON(IOUtil.inputStreamToString(ENV_VAR), HTTPServerConfig.class);
    System.out.println(hsc);
    System.out.println(hsc.getConnectionConfigs());


    for (HTTPEndPoint ep : hsc.getEndPoints())
    {
      System.out.println(ep.getMethods().getClass() + " " + Arrays.toString(ep.getMethods()));
      System.out.println(SharedUtil.toCanonicalID(',', ep.getName(), ep.getBean()));
      System.out.println(ep);
    }

  }

  @Test
  public void readSystemProperties()
  {
    String[] propNames = {
            "http.keepAlive",
            "http.maxConnections",
            "sun.net.http.keepAliveTimeout",
    };
    for (String propName: propNames)
      System.out.println(propName + "=" + System.getProperty(propName));
    System.getProperties().list(System.out);
  }

}
