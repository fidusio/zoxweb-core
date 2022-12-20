package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;

import java.lang.reflect.Field;

public class ReflectionTest
{

    @Test
    public void testFields() throws IllegalAccessException {
        Field wrapLogger = ReflectionUtil.findField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        LogWrapper log = ReflectionUtil.getValueFromField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        System.out.println(wrapLogger);
        log.info("pre-hello");
        log.setEnabled(true);
        log.info("hello");
       // System.out.println(JMod.toString(wrapLogger.getModifiers()));
    }
}
