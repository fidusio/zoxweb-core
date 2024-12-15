package org.zoxweb.shared.security;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class JWTToken
    extends SetNameDescriptionDAO
    implements CredentialInfo
{


  public enum Param
      implements GetNVConfig {
    JWT(NVConfigManager
        .createNVConfigEntity("jwt", "JWT object", "JWT", true, false, JWT.class, null)),
    TOKEN(NVConfigManager
        .createNVConfig("token", "Original token", "Token", true, false, String.class)),

    ;

    private final NVConfig nvc;

    Param(NVConfig nvc) {
      this.nvc = nvc;
    }

    public NVConfig getNVConfig() {
      return nvc;
    }
  }

  public static final NVConfigEntity NVC_JWT_TOKEN = new NVConfigEntityLocal("jwt_token",
      null,
      "JWTToken",
      true,
      false,
      false,
      false,
      JWTToken.class,
      SharedUtil.extractNVConfigs(Param.values()),
      null,
      false,
      SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);


  public JWTToken() {
    super(NVC_JWT_TOKEN);
  }

  public JWTToken(JWT jwt, String token) {
    this();
    setJWT(jwt);
    setToken(token);
  }


  public JWT getJWT() {
    return lookupValue(Param.JWT);
  }

  public void setJWT(JWT jwt) {
    setValue(Param.JWT, jwt);
  }

  public String getToken() {
    return lookupValue(Param.TOKEN);
  }

  public void setToken(String token) {
    setValue(Param.TOKEN, token);
  }


  /**
   * @return
   */
  @Override
  public CredentialType getCredentialType() {
    return CredentialType.TOKEN;
  }

}
