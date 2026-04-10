package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPAPIManager;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.security.KeyMakerProvider;
import org.zoxweb.shared.util.SUS;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectionTest {

    public static class ToTest {
        public boolean params8(String str, int intV, float floatV, String[] array, boolean bool, OutputStream os, String s1, String s2) {
            System.out.println(SUS.toCanonicalID(',', str, intV, floatV, Arrays.toString(array), bool, os, s1, s2));
            return true;
        }

        public void param1(String str) {
            System.out.println(str);
        }

        public void param0() {
        }

    }

    @Test
    public void testFields() throws IllegalAccessException {
        Field wrapLogger = ReflectionUtil.findField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        LogWrapper log = ReflectionUtil.getValueFromField(NIOSocket.class, LogWrapper.class, JMod.FINAL, JMod.PUBLIC, JMod.STATIC);
        System.out.println(wrapLogger);
        if (log.isEnabled()) log.getLogger().info("pre-hello");
        log.setEnabled(true);
        if (log.isEnabled()) log.getLogger().info("hello");
        // System.out.println(JMod.toString(wrapLogger.getModifiers()));
    }


    @Test
    public void testParameterIndex() throws NoSuchMethodException {


        Method m = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);

        Class<?>[] toLookFor =
                {
                        String.class,
                        boolean.class,
                        float.class,
                        Boolean.class,
                        UByteArrayOutputStream.class

                };
        for (Class<?> c : toLookFor) {
            System.out.println(c + " index  " + ReflectionUtil.parameterIndex(m, 0, c));
        }
    }

    @Test
    public void invokeParameters() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        Method m = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);
        ToTest tt = new ToTest();

        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        baos.write("DATA in the BAOS object");

        boolean v = ReflectionUtil.invokeMethod(false, tt, m, 5, (float) 3.5, "marwan", true, baos, "batata");
        System.out.println(v);
    }


    @Test
    public void methodParameterMatch() throws NoSuchMethodException {
        Method method8 = ToTest.class.getMethod("params8", String.class, int.class, float.class, String[].class, boolean.class, OutputStream.class, String.class, String.class);
        Method method1 = ToTest.class.getMethod("param1", String.class);
        Method method0 = ToTest.class.getMethod("param0");

        assert ReflectionUtil.doesMethodSupportParameters(true, method8, String.class, int.class, float.class, String[].class, boolean.class, String.class, String.class, OutputStream.class);
        assert ReflectionUtil.doesMethodSupportParameters(true, method0);
        assert ReflectionUtil.doesMethodSupportParameters(false, method0);
        assert ReflectionUtil.doesMethodSupportParameters(true, method1, String.class);
        assert !ReflectionUtil.doesMethodSupportParameters(false, method1, int.class);
        assert ReflectionUtil.doesMethodSupportParameters(false, method8, int.class);
        assert !ReflectionUtil.doesMethodSupportParameters(false, method8, double.class);

        assert ReflectionUtil.doesMethodSupportParameters(false, method1, String.class, int.class);

        assert ReflectionUtil.doesMethodSupportParameters(true, method1, 1, String.class);
        assert ReflectionUtil.doesMethodSupportParameters(false, method8, 4, String.class, int.class, float.class, String[].class, boolean.class, String.class, String.class, OutputStream.class);

    }


    // ---------------------------------------------------------------------
    // Fixtures and tests for ReflectionUtil.createBean(Class) / createBean(String)
    // ---------------------------------------------------------------------

    /** Plain bean with an implicit public no-arg constructor. */
    public static class WithDefaultCtor {
        public boolean constructed = true;
    }

    /** Has a no-arg constructor, but it's private — newInstance should fail with IllegalAccessException. */
    public static class WithPrivateDefaultCtor {
        private WithPrivateDefaultCtor() {
        }
    }

    /** No no-arg constructor; exposes itself via a public static final field of the same type. */
    public static class FieldSingleton {
        public static final FieldSingleton INSTANCE = new FieldSingleton("via-field");
        public final String tag;

        private FieldSingleton(String tag) {
            this.tag = tag;
        }
    }

    /** No no-arg constructor; exposes itself via a static {@code singleton()} accessor. */
    public static class MethodSingleton {
        private static final MethodSingleton ONE = new MethodSingleton("init");
        public final String tag;

        private MethodSingleton(String tag) {
            this.tag = tag;
        }

        public static MethodSingleton singleton() {
            return ONE;
        }
    }

    /** No no-arg constructor; exposes itself via a static {@code onlyInstance()} accessor (the fallback name). */
    public static class OnlyInstanceSingleton {
        private static final OnlyInstanceSingleton ONE = new OnlyInstanceSingleton("init");
        public final String tag;

        private OnlyInstanceSingleton(String tag) {
            this.tag = tag;
        }

        public static OnlyInstanceSingleton onlyInstance() {
            return ONE;
        }
    }

    /** No no-arg ctor, no field, no singleton/onlyInstance accessor — createBean must throw. */
    public static class NoConstructorNoSingleton {
        private NoConstructorNoSingleton(String required) {
        }
    }

    /** Has a {@code singleton()} method, but it's an instance method, not static — must be rejected. */
    public static class NonStaticSingletonMethod {
        private NonStaticSingletonMethod(String required) {
        }

        public NonStaticSingletonMethod singleton() {
            return null;
        }
    }

    /** Has a {@code singleton()} method whose return type is not the class itself — must be rejected. */
    public static class WrongReturnTypeSingleton {
        private WrongReturnTypeSingleton(String required) {
        }

        public static String singleton() {
            return "not-the-right-type";
        }
    }

    /** Has both a public-static-final field and a singleton() method — the field path runs first and wins. */
    public static class FieldAndMethodSingleton {
        public static final FieldAndMethodSingleton INSTANCE = new FieldAndMethodSingleton("from-field");
        private static final FieldAndMethodSingleton FROM_METHOD = new FieldAndMethodSingleton("from-method");
        public final String tag;

        private FieldAndMethodSingleton(String tag) {
            this.tag = tag;
        }

        public static FieldAndMethodSingleton singleton() {
            return FROM_METHOD;
        }
    }

    /** Singleton accessor that returns null — must surface as InstantiationException, not a silent null. */
    public static class NullReturningSingleton {
        private NullReturningSingleton(String required) {
        }

        public static NullReturningSingleton singleton() {
            return null;
        }
    }

    /**
     * Field is public static but NOT final. Documents the current strict modifier check
     * in findField — without final, the field path doesn't match and createBean falls through
     * to the singleton-method path (and fails here, since there's no singleton()).
     */
    public static class FieldNonFinalSingleton {
        public static FieldNonFinalSingleton INSTANCE = new FieldNonFinalSingleton("init");

        private FieldNonFinalSingleton(String tag) {
        }
    }


    @Test
    public void createBean_defaultConstructor() throws Exception {
        WithDefaultCtor bean = ReflectionUtil.createBean(WithDefaultCtor.class);
        assertNotNull(bean);
        assertTrue(bean.constructed);
    }

    @Test
    public void createBean_byClassName_defaultConstructor() throws Exception {
        WithDefaultCtor bean = ReflectionUtil.createBean(WithDefaultCtor.class.getName());
        assertNotNull(bean);
        assertTrue(bean.constructed);
    }

    @Test
    public void createBean_byClassName_unknownClass_throwsCNFE() {
        assertThrows(ClassNotFoundException.class,
                () -> ReflectionUtil.createBean("org.zoxweb.does.not.Exist"));
    }


    @Test
    public void createBean_publicStaticFinalField() throws Exception {
        FieldSingleton bean = ReflectionUtil.createBean(FieldSingleton.class);
        assertSame(FieldSingleton.INSTANCE, bean);
        assertEquals("via-field", bean.tag);
    }

    @Test
    public void createBean_singletonMethod() throws Exception {
        MethodSingleton bean = ReflectionUtil.createBean(MethodSingleton.class);
        assertSame(MethodSingleton.singleton(), bean);
        assertEquals("init", bean.tag);
    }

    @Test
    public void createBean_onlyInstanceMethod() throws Exception {
        OnlyInstanceSingleton bean = ReflectionUtil.createBean(OnlyInstanceSingleton.class);
        assertSame(OnlyInstanceSingleton.onlyInstance(), bean);
        assertEquals("init", bean.tag);
    }

    @Test
    public void createBean_fieldTakesPrecedenceOverMethod() throws Exception {
        FieldAndMethodSingleton bean = ReflectionUtil.createBean(FieldAndMethodSingleton.class);
        assertSame(FieldAndMethodSingleton.INSTANCE, bean);
        assertEquals("from-field", bean.tag);
    }

    @Test
    public void createBean_noConstructorNoSingleton_throws() {
        assertThrows(InstantiationException.class,
                () -> ReflectionUtil.createBean(NoConstructorNoSingleton.class));
    }

    @Test
    public void createBean_nonStaticSingletonMethod_throws() {
        assertThrows(InstantiationException.class,
                () -> ReflectionUtil.createBean(NonStaticSingletonMethod.class));
    }

    @Test
    public void createBean_wrongReturnTypeSingleton_throws() {
        assertThrows(InstantiationException.class,
                () -> ReflectionUtil.createBean(WrongReturnTypeSingleton.class));
    }

    @Test
    public void createBean_singletonReturnsNull_throws() {
        assertThrows(InstantiationException.class,
                () -> ReflectionUtil.createBean(NullReturningSingleton.class));
    }

    /**
     * Documents the current strict modifier check in findField:
     * a non-final public static field is NOT recognized as a singleton field, so createBean
     * falls through to the singleton-method path. With no singleton() either, it throws.
     * If findField is later relaxed to a bitmask check, this test will need to be updated.
     */
    @Test
    public void createBean_nonFinalSingletonField_currentlyThrows() {
        assertThrows(InstantiationException.class,
                () -> ReflectionUtil.createBean(FieldNonFinalSingleton.class));
    }

    @Test
    public void createBeanOnActualClasses() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        KeyMakerProvider provider = ReflectionUtil.createBean(KeyMakerProvider.class);
        assert provider != null;

        HTTPAPIManager ham = ReflectionUtil.createBean(HTTPAPIManager.class);
        assert ham != null;

    }
}
