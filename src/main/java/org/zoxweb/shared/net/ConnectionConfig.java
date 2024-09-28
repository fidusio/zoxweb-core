package org.zoxweb.shared.net;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVStringList;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class ConnectionConfig
  extends SetNameDescriptionDAO{

  public enum Param
      implements GetNVConfig
  {
    SCHEMES(NVConfigManager.createNVConfig("schemes", "Protocol schemes", "Schemes", false, true, NVStringList.class)),
    SOCKET_CONFIG(NVConfigManager.createNVConfigEntity("socket_config", "Socket configuration", "SocketConfig", false, true, IPAddress.NVC_IP_ADDRESS)),
    SSL_CONFIG(NVConfigManager.createNVConfig("ssl_config", "SSL configuration", "SSLConfig", false, true, NVGenericMap.class)),


    ;

    private final NVConfig cType;

    Param(NVConfig c) {
      cType = c;
    }

    public NVConfig getNVConfig() {
      return cType;
    }


  }

  public final static NVConfigEntity NVC_CONNECTION_CONFIG_DAO = new NVConfigEntityLocal("connection_config", null, "ConnectionConfig", false, true, false, false, ConnectionConfig.class, SharedUtil
      .extractNVConfigs(Param.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);

  public ConnectionConfig()
  {
    super(NVC_CONNECTION_CONFIG_DAO);
  }


  public String[] getSchemes()
  {
    return ((NVStringList) lookup(Param.SCHEMES)).getValues();
  }

  public void setSchemes(String ... schemes)
  {
    ((NVStringList) lookup(Param.SCHEMES)).setValues(schemes);
  }


  public void setSocketConfig(IPAddress sc)
  {
    setValue(Param.SOCKET_CONFIG, sc);
  }

  public IPAddress getSocketConfig()
  {
    return lookupValue(Param.SOCKET_CONFIG);
  }

  public NVGenericMap getSSLConfig()
  {
    return (NVGenericMap) lookup(Param.SSL_CONFIG);
  }
}
