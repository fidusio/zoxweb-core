package org.zoxweb.shared.http;

import org.zoxweb.shared.util.*;

public class HTTPAuthorizationToken
        extends HTTPAuthorization
{


    public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null,"Token", false, true, String.class);

    public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_TOKEN = new NVConfigEntityLocal("http_authorization_token", null , null, true, false, false, false, HTTPAuthorizationToken.class, SharedUtil.toNVConfigList(NVC_TOKEN), null, false, HTTPAuthorizationBearer.NVC_HTTP_AUTHORIZATION);



    //private String token;
    protected HTTPAuthorizationToken()
    {
        super(NVC_HTTP_AUTHORIZATION_TOKEN);
    }

    public HTTPAuthorizationToken(HTTPAuthScheme authScheme, String token)
    {
        this();
        setToken(token);
        setValue(NVC_AUTH_SCHEME, authScheme);
    }
    /**
     * @return the token
     */
    public String getToken()
    {
        return lookupValue(NVC_TOKEN);
    }
    /**
     * @param token the token to set
     */
    public void setToken(String token)
    {
        setValue(NVC_TOKEN, token);
    }



    public String toString()
    {
        return SharedUtil.toCanonicalID(' ', getAuthScheme(), getToken());
    }

    public GetNameValue<String> toHTTPHeader()
    {
        return getAuthScheme().toHTTPHeader(getToken());
    }

}