package org.zoxweb.shared.security;

import org.zoxweb.shared.data.PropertyImpl;
import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class JWTToken
        extends PropertyImpl
        implements CredentialInfo {


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

    public static final NVConfigEntity NVC_JWT_TOKEN = new NVConfigEntityPortable("jwt_token",
            null,
            "JWTToken",
            true,
            false,
            false,
            false,
            JWTToken.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyImpl.NVC_PROPERTY_IMPL);


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
    public Type getCredentialType() {
        return Type.TOKEN;
    }

    @Override
    public SecConst.SecStatus getCredentialStatus() {
        return SecConst.SecStatus.ACTIVE;
    }

    @Override
    public void setCredentialStatus(SecConst.SecStatus status) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
