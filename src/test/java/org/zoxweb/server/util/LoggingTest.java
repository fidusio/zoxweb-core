package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.logging.LogWrapper;

public class LoggingTest {
  // static final Logger log = Logger.getLogger(LoggingTest.class.getName());
  @Test
  public void logTest()
  {
//    LoggerUtil.enableDefaultLogger("org.zoxweb");
//    //LoggerUtil.updateLoggingFormat(LoggerUtil.PRODUCTION_FORMAT);
//    Logger log = Logger.getLogger(LoggingTest.class.getName());
//    log.info("hello");
//
//    LoggerUtil.updateLoggingFormat(LoggerUtil.PRODUCTION_FORMAT);
//    Logger log1 = Logger.getLogger(LoggerUtil.class.getName());

    LogWrapper log = new LogWrapper(LoggingTest.class);

    log.info("hello");
    log.getLogger().info("helloz");
  }
}
