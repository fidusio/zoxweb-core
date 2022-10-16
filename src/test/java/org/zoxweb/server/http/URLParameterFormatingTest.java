package org.zoxweb.server.http;

//import org.junit.Before;
//import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.http.HTTPEncoder;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.util.NVPair;

public class URLParameterFormatingTest {

  static HTTPMessageConfig hcc = new HTTPMessageConfig();

  @BeforeAll
  public static void setUp() {
    hcc.getParameters().add(new NVPair("address", "www.yahoo.com"));
    hcc.getParameters().add(new NVPair("port", "343"));
    hcc.getParameters().add(new NVPair("widget", "tata"));
    hcc.getParameters().add(new NVPair("a", "a"));
    hcc.getParameters().add(new NVPair("z", "z"));
    hcc.getParameters().add(new NVPair("k", "k"));
    //hcc.getParameters().add(new NVPair((String) null, "v"));
  }

  @Test
  public void parametersURL() {

    StringBuilder sb = new StringBuilder("batata&");

    System.out.println(HTTPEncoder.URL_ENCODED.format(sb, hcc.getParameters().asArrayValuesString().values()));

  }

  @Test
  public void parametersURI() {

    StringBuilder sb = new StringBuilder("batata/");

    System.out.println(HTTPEncoder.URI_REST_ENCODED.format(sb, hcc.getParameters().asArrayValuesString().values()));

  }

  @Test
  public void httpHeaders() {
    System.out.println(HTTPEncoder.HEADER.format("application/json", "charset=utf-8"));
    System.out.println(HTTPEncoder.HEADER.format("application/json; ", "charset=utf-8", "hello-1"));
    System.out.println(HTTPEncoder.HEADER.format("application/json; ", "charset=utf-8; ", "hello-1"));
  }


  @Test
  public void all() {
    for (HTTPEncoder hpe : HTTPEncoder.values()) {
      System.out.println(hpe.format(null, hcc.getParameters().asArrayValuesString().values()));
    }
    System.out
        .println("===============================================================================");
  }
}
