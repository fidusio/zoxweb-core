package org.zoxweb.shared.http;


import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.net.ConnectionConfig;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;


@SuppressWarnings("serial")
public class HTTPServerConfig
extends PropertyDAO
implements AppConfig
{
  public enum Param
      implements GetNVConfig
  {
    APPLICATION_CONFIG_VAR(NVConfigManager.createNVConfig("application_conf_var", "Application Config Variable", "ApplicationConfVar", false, true, String.class)),
    BASE_URI(NVConfigManager.createNVConfig("base_uri", "Base pre uri token", "BaseURI", false, true, String.class)),
    THREAD_POOL_SIZE(NVConfigManager.createNVConfig("thread_pool_size", "Thread pool size", "TreadPoolSize", false, true, int.class)),
    CONNECTIONS(NVConfigManager.createNVConfigEntity("connections", "Connections configurations", "Connections", false, true, ConnectionConfig.class, ArrayType.GET_NAME_MAP)),
    ENDPOINTS(NVConfigManager.createNVConfigEntity("endpoints", "Endpoints", "Endpoints", false, true, HTTPEndPoint.class, ArrayType.GET_NAME_MAP)),

    ;
    private final NVConfig nvc;

    Param(NVConfig nvc)
    {
      this.nvc = nvc;
    }

    public NVConfig getNVConfig()
    {
      return nvc;
    }
  }

  public static final NVConfigEntity NVC_HTTP_SERVER_CONFIG = new NVConfigEntityLocal("HTTP_SERVER_CONFIG",
      null,
      "HTTPServerConfig", 
      true,
      false,
      false,
      false,
      HTTPServerConfig.class,
      SharedUtil.extractNVConfigs(Param.values()),
      null,
      false,
      PropertyDAO.NVC_PROPERTY_DAO);


  NVEntityGetNameMap con;

  public HTTPServerConfig()
  {
    super(NVC_HTTP_SERVER_CONFIG);
  }


  @SuppressWarnings("unchecked")
  public ConnectionConfig[] getConnectionConfigs()
  {
    ArrayValues<NVEntity> av = (ArrayValues<NVEntity>) lookup(Param.CONNECTIONS);
    return (ConnectionConfig[])av.values(new ConnectionConfig[0]);
  }

  public HTTPEndPoint[] getEndPoints()
  {
    ArrayValues<NVEntity> av = (ArrayValues<NVEntity>) lookup(Param.ENDPOINTS);
    return (HTTPEndPoint[])av.values(new HTTPEndPoint[0]);
  }

  public String getApplicationConfVar()
  {
    return lookupValue(Param.APPLICATION_CONFIG_VAR);
  }

  public void setApplicationConfVar(String appConVar)
  {
    setValue(Param.APPLICATION_CONFIG_VAR, appConVar);
  }

  public String getBaseURI()
  {
    return lookupValue(Param.BASE_URI);
  }

  public void setBaseURI(String baseURI)
  {
    setValue(Param.BASE_URI, baseURI);
  }

  public int getThreadPoolSize()
  {
    return lookupValue(Param.THREAD_POOL_SIZE);
  }

  public void setThreadPoolSize(int poolSize)
  {
    if(poolSize < 0 || poolSize > 384)
    {
      throw new IllegalArgumentException("Invalid pool size " + poolSize);
    }
    setValue(Param.THREAD_POOL_SIZE, poolSize);
  }



}
