package org.zoxweb.server.logging;

import org.zoxweb.shared.util.GetValue;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enum for java Logger level
 */
public enum LoggerLevel
        implements GetValue<Level> {
    ALL(Level.ALL),
    CONFIG(Level.CONFIG),
    FINE(Level.FINE),
    FINER(Level.FINER),
    FINEST(Level.FINEST),
    INFO(Level.INFO),
    OFF(Level.OFF),
    SEVERE(Level.SEVERE),
    WARNING(Level.WARNING),
    ;

    private final Level level;

    LoggerLevel(Level level) {
        this.level = level;
    }

    @Override
    public Level getValue() {
        return level;
    }

    public static void setLevel(LoggerLevel ll) {
        Logger root = Logger.getLogger("");
        root.setLevel(ll.getValue());
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(ll.getValue());
        }
    }

    public static LoggerLevel currentLevel() {
        return toLoggerLevel(Logger.getLogger("").getLevel());
    }

    public static LoggerLevel toLoggerLevel(Level level) {
        for (LoggerLevel ll : LoggerLevel.values()) {
            if (ll.getValue() == level)
                return ll;
        }

        return null;

    }
}
