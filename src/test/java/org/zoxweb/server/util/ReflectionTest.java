package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.shared.util.SUS;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionTest
{

    public static class ToTest
    {
        public void params8(String str, int intV, float floatV, String[] array, boolean bool, OutputStream os, String s1, String s2)
        {
            System.out.println(SUS.toCanonicalID(',', str, intV, floatV, Arrays.toString(array), bool, os, s1, s2));
        }

        public void param1(String str)
        {
            System.out.println(str);
        }

        public void param0()
        {
        }

    }

    @Test
    public void testFields() throws IllegalAccessException {
        Field wrapLogger = ReflectionUtil.findField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        LogWrapper log = ReflectionUtil.getValueFromField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        System.out.println(wrapLogger);
        if(log.isEnabled()) log.getLogger().info("pre-hello");
        log.setEnabled(true);
        if(log.isEnabled()) log.getLogger().info("hello");
       // System.out.println(JMod.toString(wrapLogger.getModifiers()));
    }


    @Test
    public void testParameterIndex() throws NoSuchMethodException {


        Method  m = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);

        Class<?>[] toLookFor =
                {
                String.class,
                boolean.class,
                float.class,
                        Boolean.class,
                UByteArrayOutputStream.class

        };
        for(Class<?> c : toLookFor)
        {
            System.out.println(c + " index  " + ReflectionUtil.parameterIndex(m, 0, c));
        }
    }

    @Test
    public void invokeParameters() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        Method  m = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);
        ToTest tt = new ToTest();

        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        baos.write("DATA in the BAOS object");

       ReflectionUtil.invokeMethod(false, tt, m, 5, (float)3.5, "marwan", true, baos, "batata");
    }


    @Test
    public void methodParameterMatch() throws NoSuchMethodException {
        Method params8 = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);
        Method param1 = ToTest.class.getMethod("param1", String.class);
        Method param0 = ToTest.class.getMethod("param0");

        assert ReflectionUtil.doesMethodSupportParameters(true, params8, String.class, int.class, float.class, String[].class, boolean.class, String.class, String.class, OutputStream.class);
        assert ReflectionUtil.doesMethodSupportParameters(true, param0);
        assert ReflectionUtil.doesMethodSupportParameters(false, param0);
        assert ReflectionUtil.doesMethodSupportParameters(true, param1, String.class);
        assert !ReflectionUtil.doesMethodSupportParameters(false, param1, int.class);
        assert ReflectionUtil.doesMethodSupportParameters(false, params8, int.class);
        assert !ReflectionUtil.doesMethodSupportParameters(false, params8, double.class);

        assert ReflectionUtil.doesMethodSupportParameters(false, param1, String.class, int.class);

    }
}
