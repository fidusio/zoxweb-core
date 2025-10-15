package org.zoxweb.server.logging;

import java.util.logging.Logger;

public class LogWrapper {
    /**
     * Exposed for ease of access
     */
    public final Logger logger;
    private volatile boolean enabled = true;

    public LogWrapper(Logger logger) {
        this.logger = logger;
    }

    public LogWrapper(Class<?> clazz) {
        this(clazz.getName());
    }

    public LogWrapper(String loggerName) {
        this(Logger.getLogger(loggerName));
        LoggerUtil.configureLogger(logger);
    }

    /**
     * Kept for backward compatibility
     * @return logger
     */
    public Logger getLogger() {
        return logger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LogWrapper setEnabled(boolean stat) {
        this.enabled = stat;
        return this;
    }

//    public void info(Object o) {
//        if (isEnabled()) {
//            if (o == null)
//                getLogger().info("null");
//            else if (o instanceof String)
//                getLogger().info((String) o);
//            else
//                getLogger().info(o.toString());
//        }
//          return this;
//    }
//
//    public LogWrapper info(String message, Object... o) {
//        if (isEnabled()) {
//            if (o == null || o.length == 0)
//                logger.info(message + ": " + "null");
//            else if (o.length == 1)
//                logger.info(message + ": " + o[0]);
//            else
//                logger.info(message + ": " + Arrays.toString(o));
//        }
//        return this;
//    }


}
