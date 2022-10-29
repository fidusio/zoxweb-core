package org.zoxweb.server.logging;

import java.util.Arrays;
import java.util.logging.Logger;

public class LogWrapper {
    private final Logger logger;
    private boolean enabled = true;
    public LogWrapper(Logger logger)
    {
        this.logger = logger;
    }

    public LogWrapper(Class<?> clazz)
    {
        this(clazz.getName());
    }

    public LogWrapper(String loggerName)
    {
        this.logger = Logger.getLogger(loggerName);
    }

    public Logger getLogger()
    {
        return logger;
    }

    public boolean isEnabled()
    {
        return enabled;
    }
    public LogWrapper setEnabled(boolean stat)
    {
        enabled = stat;
        return this;
    }
    public LogWrapper info(Object o)
    {
        if(isEnabled())
        {
            if (o == null)
                logger.info("null");
            else if(o instanceof String)
                logger.info((String) o);
            else
            logger.info(o.toString());
        }
        return this;
    }

    public LogWrapper info(String message, Object ...o)
    {
        if(isEnabled())
        {
            if (o == null)
                logger.info(message + ": " + "null");
            else if(o.length < 2)
                logger.info(message +": " + (o.length == 0 ? "" : o[1]));
            else
                logger.info(message +": " + Arrays.toString(o));
        }
        return this;
    }


}
