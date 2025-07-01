package org.zoxweb.shared.security;

import org.zoxweb.server.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Interface to invoke method with security access check
 */
public interface SecureInvoker {

    <V> V invoke(boolean strict, Object bean, Method method, Object... parameters)
            throws InvocationTargetException, IllegalAccessException;

    <V> V invoke(boolean strict, Object bean, ReflectionUtil.MethodAnnotations methodAnnotations, Map<String, Object> parameters)
            throws InvocationTargetException, IllegalAccessException;
}
