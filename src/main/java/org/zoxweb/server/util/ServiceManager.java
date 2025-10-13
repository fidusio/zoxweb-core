package org.zoxweb.server.util;


import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.NIOConfig;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.security.IPBlockerListener;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.ApplicationConfigDAO;
import org.zoxweb.shared.data.ApplicationConfigDAO.ApplicationDefaultParam;
import org.zoxweb.shared.data.ConfigDAO;
import org.zoxweb.shared.security.IPBlockerConfig;
import org.zoxweb.shared.util.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class ServiceManager
        implements AutoCloseable {
    public static final ServiceManager SINGLETON = new ServiceManager();

    private ServiceManager() {

    }

    private static final Logger log = Logger.getLogger(ServiceManager.class.getName());


    public static void close(Object obj) {
        if (obj instanceof AutoCloseable)
            IOUtil.close((AutoCloseable) obj);
    }


    public synchronized NIOSocket loadNIOSocket(ApplicationConfigDAO acd) {
        String filename = acd.lookupValue(ApplicationDefaultParam.NIO_CONFIG);
        if (filename != null) {
            log.info("creating NIO_CONFIG");
            if (ResourceManager.SINGLETON.lookup(NIOConfig.RESOURCE_NAME) != null) {
                close(ResourceManager.SINGLETON.lookup(NIOConfig.RESOURCE_NAME));
            }
            try {
                String fullFileName = ApplicationConfigManager.SINGLETON.concatWithEnvVar("conf", filename);
                ConfigDAO configDAO = GSONUtil.fromJSON(IOUtil.inputStreamToString(fullFileName));
                log.info("" + configDAO);
                NIOConfig nioConfig = new NIOConfig(configDAO);
                NIOSocket nioSocket = nioConfig.createApp();
                ResourceManager.SINGLETON.register(NIOConfig.RESOURCE_NAME, nioConfig);

                return nioSocket;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public synchronized void loadServices() throws NullPointerException, IOException {
        ApplicationConfigDAO acd = ApplicationConfigManager.SINGLETON.loadDefault();
        if (acd != null) {
            ResourceManager.SINGLETON.register(ApplicationConfigDAO.RESOURCE_NAME, acd);
        }

        IPBlockerListener ipBlocker = null;
        String filename = acd.lookupValue(ApplicationDefaultParam.IP_BLOCKER_CONFIG);

        if (filename != null)
            log.info(ApplicationDefaultParam.IP_BLOCKER_CONFIG + ": " + filename);
        else
            log.info(ApplicationDefaultParam.IP_BLOCKER_CONFIG + " NOT FOUND");
        if (filename != null) {
            log.info("creating IP_BLOCKER");
            try {
                if (ResourceManager.SINGLETON.lookup(IPBlockerListener.RESOURCE_NAME) != null) {
                    close(ResourceManager.SINGLETON.lookup(IPBlockerListener.RESOURCE_NAME));
                }
                File file = ApplicationConfigManager.SINGLETON.locateFile(acd, filename);
                IPBlockerConfig appConfig = GSONUtil.fromJSON(IOUtil.inputStreamToString(new FileInputStream(file), true), IPBlockerConfig.class);
                IPBlockerListener.Creator c = new IPBlockerListener.Creator();
                //log.info("\n" + GSONUtil.toJSON(appConfig, true, false, false));
                c.setAppConfig(appConfig);
                ipBlocker = c.createApp();
                ResourceManager.SINGLETON.register(IPBlockerListener.RESOURCE_NAME, ipBlocker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        filename = acd.lookupValue(ApplicationDefaultParam.NIO_CONFIG);
        if (filename != null) {
            log.info("creating NIO_CONFIG");
            if (ResourceManager.SINGLETON.lookup(NIOConfig.RESOURCE_NAME) != null) {
                close(ResourceManager.SINGLETON.lookup(NIOConfig.RESOURCE_NAME));
            }
            try {
                File file = ApplicationConfigManager.SINGLETON.locateFile(acd, filename);
                String configDAOContent = IOUtil.inputStreamToString(file);
                ConfigDAO configDAO = GSONUtil.fromJSON(configDAOContent);
                log.info("NIO_CONFIG:\n" + configDAOContent);
                NIOConfig nioConfig = new NIOConfig(configDAO);
                NIOSocket nioSocket = nioConfig.createApp();
                if (ipBlocker != null)
                    nioSocket.setEventManager(TaskUtil.defaultEventManager());
                ResourceManager.SINGLETON.register(NIOConfig.RESOURCE_NAME, nioConfig);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        log.info("Finished");
    }


    public void close() {
        for (Object res : ResourceManager.SINGLETON.resources()) {
            close(res);
        }
    }
}
