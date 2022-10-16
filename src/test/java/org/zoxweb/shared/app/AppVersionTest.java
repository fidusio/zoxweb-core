package org.zoxweb.shared.app;

import java.util.Date;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;

public class AppVersionTest {

  @Test
  public void build()
  {
    AppVersionDAO appVersionDAO = new AppVersionDAO();
    appVersionDAO.setNano(1);
    appVersionDAO.setReleaseDate(new Date());

    Date dt = appVersionDAO.getReleaseDate();
    assert dt!= null;

    System.out.println(appVersionDAO.getReleaseDate());
    String json = GSONUtil.toJSONDefault(appVersionDAO);
    System.out.println(json);
    AppVersionDAO temp = GSONUtil.fromJSON(json, AppVersionDAO.class);
    System.out.println(temp.getReleaseDate());
  }

}
