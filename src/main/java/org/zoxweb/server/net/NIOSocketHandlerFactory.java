package org.zoxweb.server.net;


import org.zoxweb.shared.util.InstanceFactory;

public class NIOSocketHandlerFactory
        extends ProtocolFactoryBase<NIOSocketHandler> {

    private Class<? extends BaseSessionCallback> cbClass;
    private InstanceFactory.Creator<PlainSessionCallback> creator;
    private boolean timeout = true;

    public NIOSocketHandlerFactory() {
    }

    public NIOSocketHandlerFactory(Class<? extends BaseSessionCallback> cbClass) {
        this(cbClass, true);

    }

    public NIOSocketHandlerFactory(Class<? extends BaseSessionCallback> cbClass, boolean timeout) {
        this.cbClass = cbClass;
        this.timeout = timeout;

    }

    public NIOSocketHandlerFactory(InstanceFactory.Creator<PlainSessionCallback> creator) {
        this.creator = creator;
    }


    @Override
    public NIOSocketHandler newInstance() {
        PlainSessionCallback sc = null;
        try {
            if (creator != null)
                sc = creator.newInstance();
            else if (cbClass != null)
                sc = (PlainSessionCallback) cbClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NIOSocketHandler(sc, timeout);
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "NIOPlainSocketFactory";
    }

    public void init() {
        try {
            if (getProperties().getValue("session_callback") != null) {
                cbClass = (Class<PlainSessionCallback>) Class.forName(getProperties().getValue("session_callback"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    @Override
//    public NIOChannelCleaner getNIOChannelCleaner() {
//        return NIOChannelCleaner.DEFAULT;
//    }

}