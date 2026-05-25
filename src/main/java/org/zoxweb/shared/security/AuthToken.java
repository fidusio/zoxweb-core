package org.zoxweb.shared.security;

public class AuthToken<T> {
    private String url;
    private T token;
    private SecConst.AuthenticationType type;

    public AuthToken(String url, SecConst.AuthenticationType type, T token)
    {
        this.url = url;
        this.type = type;
        this.token = token;
    }

    public String getURL(){return url;}

    public T getToken(){return token;}

    public SecConst.AuthenticationType getType()
    {
        return type;
    }


}
