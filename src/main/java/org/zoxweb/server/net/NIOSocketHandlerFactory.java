package org.zoxweb.server.net;


import org.zoxweb.shared.util.InstanceCreator;

public class NIOSocketHandlerFactory
        extends ProtocolFactoryBase<NIOSocketHandler>
{

    private  Class<? extends BaseSessionCallback> cbClass;
    private InstanceCreator<PlainSessionCallback> instanceCreator;

    public NIOSocketHandlerFactory(){}


    public NIOSocketHandlerFactory(Class<? extends BaseSessionCallback> cbClass)
    {
        this.cbClass = cbClass;

    }
    public NIOSocketHandlerFactory(InstanceCreator<PlainSessionCallback> instanceCreator)
    {
        this.instanceCreator = instanceCreator;
    }



    @Override
    public NIOSocketHandler newInstance()
    {
        PlainSessionCallback sc = null;
        try
        {
            if (instanceCreator != null)
                sc = instanceCreator.newInstance();
            else if(cbClass != null)
                sc = (PlainSessionCallback) cbClass.getDeclaredConstructor().newInstance();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return new NIOSocketHandler(sc);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "NIOPlainSocketFactory";
    }
    public void init()
    {
        try
        {
            if(getProperties().getValue("session_callback") != null)
            {
                cbClass = (Class<PlainSessionCallback>) Class.forName(getProperties().getValue("session_callback"));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

//    @Override
//    public NIOChannelCleaner getNIOChannelCleaner() {
//        return NIOChannelCleaner.DEFAULT;
//    }

}