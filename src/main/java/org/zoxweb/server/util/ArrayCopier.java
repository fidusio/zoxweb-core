package org.zoxweb.server.util;

import java.lang.reflect.Array;

public final class ArrayCopier {

    private ArrayCopier() {
    }

    public static Object copy(Class<?> c, Object[] objs) {
        if (int[].class.equals(c)) {
            return copyAsInt(objs);
        }
        if (long[].class.equals(c)) {
            return copyAsLong(objs);
        }
        if (float[].class.equals(c)) {
            return copyAsFloat(objs);
        }
        if (Double[].class.equals(c)) {
            return copyAsDouble(objs);
        }
        if (short[].class.equals(c)) {
            return copyAsShort(objs);
        }

        if (Object[].class.isAssignableFrom(c)) {
            Object[] ret = (Object[]) Array.newInstance(c.getComponentType(), objs.length);
            for (int i = 0; i < objs.length; i++) {
                ret[i] = objs[i];
            }
            return ret;

        }

        throw new IllegalArgumentException("Unsupported type " + c);

    }


    public static int[] copyAsInt(Object[] objs) {
        int[] ret = new int[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (int) objs[0];
        }
        return ret;
    }

    public static long[] copyAsLong(Object[] objs) {
        long[] ret = new long[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (long) objs[0];
        }
        return ret;
    }

    public static float[] copyAsFloat(Object[] objs) {
        float[] ret = new float[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (float) objs[0];
        }
        return ret;
    }

    public static double[] copyAsDouble(Object[] objs) {
        double[] ret = new double[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (double) objs[0];
        }
        return ret;
    }

    public static short[] copyAsShort(Object[] objs) {
        short[] ret = new short[objs.length];
        for (int i = 0; i < objs.length; i++) {
            ret[i] = (short) objs[0];
        }
        return ret;
    }
}
