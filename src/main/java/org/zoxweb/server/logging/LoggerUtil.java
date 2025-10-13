/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.logging;

import org.zoxweb.server.util.DateUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Logger utility class.
 */
public final class LoggerUtil {

    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
    public static final String PRODUCTION_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s : %2$s %5$s%6$s%n";
    public static final String DEFAULT_FORMAT = "%1$s %4$-6s : %2$s-%5$s%6$s%n";
    public static final String DEFAULT_FORMAT_INTERNAL = "[%1$s][%2$s::%3$s][%4$s]:%5$s: %6$s %n";
    public static final ConsoleHandler PRODUCTION_HANDLER = new ConsoleHandler();

    static {
        PRODUCTION_HANDLER.setFormatter(new SimpleFormatter() {

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(DEFAULT_FORMAT_INTERNAL,
                        DateUtil.DEFAULT_DATE_FORMAT_TZ.format(new Date(lr.getMillis())),
                        lr.getSourceClassName(),
                        lr.getSourceMethodName(),
                        lr.getLevel().getLocalizedName(),
                        Thread.currentThread(),
                        lr.getMessage()
                );
            }
        });
    }

    private LoggerUtil() {
    }

    public static Logger loggerToFile(String loggerName, String filename)
            throws SecurityException, IOException {
        if (loggerName != null && filename != null) {
            return loggerToFile(Logger.getLogger(loggerName), filename);
        }

        return null;
    }


    public static Logger loggerToFile(Logger logger, String filename)
            throws SecurityException, IOException {
        if (logger != null && filename != null) {
            FileHandler fh = new FileHandler(filename, true);
            //Logger rootLogger = Logger.getLogger("");
            //Handler[] handlers = rootLogger.getHandlers();
            //if (handlers[0] instanceof ConsoleHandler) {
            //       rootLogger.removeHandler(handlers[0]);
            //}
            logger.addHandler(fh);
            CustomFormatter formatter = new CustomFormatter(DEFAULT_FORMAT);
            fh.setFormatter(formatter);
            logger.info("file logging started");
        }

        return logger;
    }

    public static PrintWriter createPrintWriter(String filename)
            throws IOException {
        return createPrintWriter(new File(filename));
    }

    public static PrintWriter createPrintWriter(File f)
            throws IOException {
        FileWriter fw = new FileWriter(f, true);
        return new PrintWriter(new BufferedWriter(fw), true);
    }

    public static void logToFile(PrintWriter pw, String msg) {
        if (pw != null) {
            pw.println(SDF.format(new Date()) + msg);
        }
    }

    public static void updateLoggingFormat(String format) {
        System.setProperty("java.util.logging.SimpleFormatter.format", format);
    }

//	public static void enableDefaultLogger(String loggerHeader)
//	{
//		Logger mainLogger = Logger.getLogger(loggerHeader);
//		mainLogger.setUseParentHandlers(false);
//		ConsoleHandler handler = new ConsoleHandler();
//		handler.setFormatter(new SimpleFormatter() {
//
//		  @Override
//		  public synchronized String format(LogRecord lr) {
//			return String.format(DEFAULT_FORMAT_INTERNAL,
//				DateUtil.DEFAULT_DATE_FORMAT_TZ.format(new Date(lr.getMillis())),
//				lr.getSourceClassName(),
//				lr.getSourceMethodName(),
//				lr.getLevel().getLocalizedName(),
//				Thread.currentThread(),
//				lr.getMessage()
//			);
//		  }
//		});
//		mainLogger.addHandler(handler);
//	}

    public static void configureLogger(Logger logger) {
        // Remove existing handlers (if any)
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        // Attach the handler to the logger
        logger.addHandler(PRODUCTION_HANDLER);

        // Set the logger level (optional)
        logger.setLevel(Level.ALL);

        // Disable use of parent handlers
        logger.setUseParentHandlers(false);
    }


}