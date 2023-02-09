package org.zoxweb.server.net.ssl;

import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.InstanceCreator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SSLContextInfo
    implements InstanceCreator<SSLEngine>
{

    public enum Param
        implements GetName
    {
        PROTOCOLS("protocols"),
        CIPHERS("ciphers"),
        ;

        private final String name;
        Param(String name)
        {
            this.name = name;
        }
        @Override
        public String getName() {
            return name;
        }
    }

    private final SSLContext sslContext;
    private final String[] protocols;
    private final String[] ciphers;


    public SSLContextInfo(KeyStore ks, char[] keyStorePassword, String[] protocols, String[] ciphers) throws GeneralSecurityException {
        this(CryptoUtil.initSSLContext("TLS", null, ks, keyStorePassword, keyStorePassword, null),
                protocols,
                ciphers);
    }

    public SSLContextInfo(SSLContext sslContext)
    {
        this(sslContext, null, null);
    }

    public SSLContextInfo(SSLContext sslContext, String[] protocols, String[] ciphers)
    {
        this.sslContext = sslContext;
        this.protocols = protocols;
        this.ciphers = ciphers;
    }


    public SSLEngine newInstance()
    {
        SSLEngine ret = sslContext.createSSLEngine();
        if(protocols != null && protocols.length > 0)
        {
            ret.setEnabledProtocols(protocols);
        }
        if(ciphers != null && ciphers.length > 0)
        {
            ret.setEnabledCipherSuites(ciphers);
        }

        return ret;
    }
}
