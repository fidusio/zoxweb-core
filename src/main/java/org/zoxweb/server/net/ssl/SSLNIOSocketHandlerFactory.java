package org.zoxweb.server.net.ssl;


import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ProtocolFactoryBase;
import org.zoxweb.shared.data.ConfigDAO;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.InstanceFactory;

public class SSLNIOSocketHandlerFactory
        extends ProtocolFactoryBase<SSLNIOSocketHandler>
{

    private IPAddress remoteConnection;
    private SSLContextInfo sslContext;
    private Class<? extends BaseSessionCallback> scClass;
    private InstanceFactory.InstanceCreator<SSLSessionCallback> instanceCreator;

    public SSLNIOSocketHandlerFactory()
    {
        complexSetup = false;
    }
    public SSLNIOSocketHandlerFactory(SSLContextInfo sslContext, InstanceFactory.InstanceCreator<SSLSessionCallback> instanceCreator)
    {
        this();
        this.sslContext = sslContext;
        this.instanceCreator = instanceCreator;
    }

    public SSLNIOSocketHandlerFactory(SSLContextInfo sslContext, Class<? extends BaseSessionCallback> scClass)
    {
        this();
        this.sslContext = sslContext;
        this.scClass = scClass;
    }


    public SSLNIOSocketHandlerFactory(SSLContextInfo sslContext, IPAddress rc)
    {
        this();
        this.sslContext = sslContext;
        remoteConnection = rc;
    }

    public SSLContextInfo getSSLContext()
    {
        return sslContext;
    }


    @Override
    public SSLNIOSocketHandler newInstance()
    {
        SSLSessionCallback sc = null;
        try
        {
            if(instanceCreator != null)
            {
                sc = instanceCreator.newInstance();;
            }
            else if(scClass != null)
                sc = (SSLSessionCallback) scClass.getDeclaredConstructor().newInstance();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return new SSLNIOSocketHandler(sslContext, sc, getProperties().getValue("simple_state_machine", true), remoteConnection);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "SSLNIOSocketFactory";
    }

    public void init()
    {
        if(getProperties().getValue("remote_host") != null)
            setRemoteConnection(new IPAddress(getProperties().getValue("remote_host")));
        sslContext = (SSLContextInfo) ((ConfigDAO)getProperties().getValue("ssl_engine")).attachment();
        try
        {
            if(getProperties().getValue("session_callback") != null)
            {
                scClass = (Class<SSLSessionCallback>) Class.forName(getProperties().getValue("session_callback"));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public void setRemoteConnection(IPAddress rConnection)
    {
        remoteConnection = rConnection;
    }

    public IPAddress getRemoteConnection(){ return remoteConnection; }


//    @Override
//    public NIOChannelCleaner getNIOChannelCleaner() {
//        return NIOChannelCleaner.DEFAULT;
//    }

}