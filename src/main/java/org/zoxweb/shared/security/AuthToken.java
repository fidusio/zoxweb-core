package org.zoxweb.shared.security;

import org.zoxweb.shared.crypto.CryptoConst;

public class AuthToken<T> {
    private String url;
    private T token;
    private CryptoConst.AuthenticationType type;

    public AuthToken(String url, CryptoConst.AuthenticationType type, T token)
    {
        this.url = url;
        this.type = type;
        this.token = token;
    }

    public String getURL(){return url;}

    public T getToken(){return token;}

    public CryptoConst.AuthenticationType getType()
    {
        return type;
    }


}
