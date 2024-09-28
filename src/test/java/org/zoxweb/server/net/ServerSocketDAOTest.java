package org.zoxweb.server.net;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.IPAddress;

public class ServerSocketDAOTest {


  public static void main(String ...args)
  {
    try {
      IPAddress isa = new IPAddress();
      isa.setBacklog(250);
      isa.setPort(80);
      String json = GSONUtil.toJSON(isa, false, false, false);
      System.out.println(json);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}
