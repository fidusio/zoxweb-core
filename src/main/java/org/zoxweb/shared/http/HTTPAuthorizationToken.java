package org.zoxweb.shared.http;

import org.zoxweb.shared.util.*;

public class HTTPAuthorizationToken
        extends HTTPAuthorization
{


    public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null,"Token", false, true, String.class);

    public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_TOKEN = new NVConfigEntityLocal("http_authorization_token", null , null, true, false, false, false, HTTPAuthorizationToken.class, SharedUtil.toNVConfigList(NVC_TOKEN), null, false, HTTPAuthorizationBearer.NVC_HTTP_AUTHORIZATION);



    //private String token;
    public HTTPAuthorizationToken()
    {
        super(NVC_HTTP_AUTHORIZATION_TOKEN);
    }

    public HTTPAuthorizationToken(HTTPAuthScheme authScheme, String token)
    {
        this();
        SharedUtil.checkIfNulls("Null name or token", authScheme, token);
        setValue(NVC_AUTH_SCHEME, authScheme);
        switch (getAuthScheme())
        {
            case SSWS:
                setToken(token);
                break;
            case GENERIC:
                String[] parsed = SharedStringUtil.parseString(token, " ", true);
                if (parsed.length != 2)
                {
                    throw new IllegalArgumentException("Invalid token: " + token);
                }
                setName(parsed[0]);
                setToken(parsed[1]);
                break;
            default:
                throw new IllegalArgumentException("Invalid generic token: " + token);
        }
    }


    public HTTPAuthorizationToken(HTTPAuthScheme authScheme, String name, String token)
    {
        this();
        if (authScheme != HTTPAuthScheme.GENERIC)
        {
            throw new IllegalArgumentException("Must be generic type");
        }
        SharedUtil.checkIfNulls("Null name or token", name, token);
        setValue(NVC_AUTH_SCHEME, authScheme);
        setName(name);
        setToken(token);

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
        if (getAuthScheme() == HTTPAuthScheme.GENERIC)
            return getAuthScheme().toHTTPHeader(getName(), getToken());

        return getAuthScheme().toHTTPHeader(getToken());
    }

}