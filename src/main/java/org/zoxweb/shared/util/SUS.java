package org.zoxweb.shared.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Sus(Shared Util Shortcut) basically to minimise typing
 */
public class SUS {
    private SUS() {
    }

    private static final Map<Object, Object> cache = new HashMap<>();


    /**
     * @param collection to be checked
     * @return true col != null and col not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }


    /**
     * @param str to checked
     * @return true str !=null and str.trim() not empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }


    /**
     * Check if an array is empty not empty meaning not null and length > 0
     *
     * @param array to be checked
     * @return true if the array exists and not empty
     */
    public static boolean isNotEmpty(Object[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * @param array to check
     * @return true if array == null or array.length = 0
     */
    public static boolean isNotEmpty(byte[] array) {
        return (array != null && array.length != 0);
    }


    public static boolean areAllDataZero(byte[] buffer, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            if (buffer[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * Convert a NVEntity to NVGenericMap
     *
     * @param nve to be converted
     * @return the nve as NVGenericMao
     */
    public static NVGenericMap toNVGenericMap(NVEntity nve) {
        NVGenericMap ret = new NVGenericMap(nve.getName());
        NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
        for (NVConfig nvc : nvce.getAttributes()) {
            ret.add(nve.lookup(nvc));
        }
        return ret;
    }

    /**
     * Return the enum name if it implements GetName if not enum.name()
     *
     * @param en to be checked
     * @return the name
     */
    public static String enumName(Enum<?> en) {
        checkIfNulls("enum can't be null", en);
        if (en instanceof GetName)
            return ((GetName) en).getName();
        return en.name();
    }

    /**
     * @param enums to be converted
     * @return string[] of the enums names
     */
    public static String[] enumNames(Enum<?>... enums) {
        String[] ret = new String[enums.length];
        for (int i = 0; i < enums.length; i++)
            ret[i] = enumName(enums[i]);
        return ret;
    }


    public static void checkIfNull(String str, Object obj) {
        checkIfNulls(str, obj);
    }

    /**
     * Checks all the objs if any of them is null it will throw a NullPointerException.
     *
     * @param msg  NullPointerException message
     * @param objs to be checked
     * @throws NullPointerException if any obj is null
     */
    public static void checkIfNulls(String msg, Object... objs)
            throws NullPointerException {
        if (objs == null)
            // error in invoking the check
            throw new NullPointerException("Null Array Object");

        for (Object o : objs)
            if (o == null)
                throw new NullPointerException(msg);
    }

    public static NVGenericMap updateGetNVProperties(GetNVProperties toUpdate, NVGenericMap value) {
        return updateNVGenericMap(toUpdate.getProperties(), value);
    }

    @SuppressWarnings("unchecked")
    public static NVGenericMap updateNVGenericMap(NVGenericMap toUpdate, NVGenericMap value) {
        for (GetNameValue<?> gnv : value.values()) {
            GetNameValue<?> gnvToUpdate = toUpdate.get(gnv);
            if (gnvToUpdate != null) {
                ((NVBase<Object>) gnvToUpdate).setValue(gnv.getValue());
            } else {
                toUpdate.add(gnv);
            }
        }

        return toUpdate;
    }

    /**
     * Build a NamedValue object
     *
     * @param name  of the parameter
     * @param value value of the parameter
     * @param <V>   type of NamedValue
     * @return NamedValue {@link org.zoxweb.shared.util.NamedValue}
     */
    public static <V> GetNameValue<V> buildNV(String name, V value) {
        return new NamedValue<>(name, value);
    }

    /**
     * Returns true if str is null or str.trim().length() == 0.
     *
     * @param str to be checked
     * @return true if str is empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * @param nvb to be checked
     * @return true is gnv is primitive type
     */
    public static boolean isPrimitiveGNV(GetNameValue<?> nvb) {
        checkIfNulls("NameValue null", nvb);
        if (cache.get(nvb.getClass()) != null ||
                (nvb.getValue() != null && cache.get(nvb.getValue().getClass()) != null))
            return true;

        if (nvb instanceof NVPair ||
                nvb instanceof NVInt ||
                nvb instanceof NVLong ||
                nvb instanceof NVFloat ||
                nvb instanceof NVDouble) {
            synchronized (cache) {
                cache.put(nvb.getClass(), nvb.getClass());
                return true;
            }
        }

        if (nvb.getValue() != null) {
            Object value = nvb.getValue();
            if (value instanceof String ||
                    value instanceof Integer ||
                    value instanceof Long ||
                    value instanceof Float ||
                    value instanceof Double ||
                    value instanceof Short) {
                synchronized (cache) {
                    cache.put(value.getClass(), value.getClass());
                    return true;
                }
            }
        }

        return false;

    }

    private static StringBuilder _toCanonicalID(int pos, StringBuilder sb, boolean ignoreNull, char sep, Object val) {
        if (val == null && ignoreNull) {
            return sb;
        }

        if (ignoreNull && sb.length() != 0) {
            sb.append(sep);
        } else if (pos != 0) {
            sb.append(sep);
        }

        if (val != null) {
            sb.append(val);
        }

        return sb;
    }

    /**
     * Produce a canonical id separated by the char separator
     *
     * @param sep      between objArray
     * @param objArray array by invoking objArray[i].toString()
     * @return the canonical identifier
     */
    public static String toCanonicalID(char sep, Object... objArray) {
        return toCanonicalID(false, sep, objArray);
    }

    /**
     * Produce a canonical id separated by the char separator
     *
     * @param ignoreNulls if true null will be removed
     * @param sep         between objArray
     * @param objArray    array by invoking objArray[i].toString()
     * @return the canonical identifier
     */
    public static String toCanonicalID(boolean ignoreNulls, char sep, Object... objArray) {
        StringBuilder sb = new StringBuilder();

        if (objArray != null) {

            for (int i = 0; i < objArray.length; i++) {
                _toCanonicalID(i, sb, ignoreNulls, sep, objArray[i]);
            }
        }

        return sb.toString();
    }

    /**
     * @param ignoreNulls if true null will be removed
     * @param sep         between enumer
     * @param enumer      enumeration
     * @return the canonical identifier
     */
    public static String toCanonicalID(boolean ignoreNulls, char sep, Enumeration<?> enumer) {
        StringBuilder sb = new StringBuilder();

        if (enumer != null) {
            int pos = 0;
            while (enumer.hasMoreElements()) {
                _toCanonicalID(pos++, sb, ignoreNulls, sep, enumer.nextElement());
            }
        }

        return sb.toString();
    }

    /**
     * @param str to be validated
     * @return null or trimmed not empty string
     */
    public static String trimOrNull(String str) {

        return (str == null || str.trim().isEmpty()) ? null : str;
//        str = (str != null ? str.trim() : null);
//        return str != null ? (!str.isEmpty() ? str : null) : null;
    }

    /**
     * @param str to be validated
     * @return empty string null or trimmed string
     */
    public static String trimOrEmpty(String str) {
        return str != null ? str.trim() : "";
    }

    /**
     * Produce an error message
     *
     * @param message description message
     * @param enums   possible values
     * @return error message to be printed or consumed
     */
    public static String errorMessage(String message, Enum<?>... enums) {
        StringBuilder sb = new StringBuilder(message);
        for (Enum<?> e : enums) {
            sb.append('\n');

            if (e instanceof GetName)
                sb.append(((GetName) e).getName());
            else
                sb.append(e.name());
            sb.append(": ");
            if (e instanceof GetDescription)
                sb.append(((GetDescription) e).getDescription());
        }
        return sb.toString();
    }


    /**
     * Compares two byte arrays in length-constant time. This comparison method
     * is used so that password hashes cannot be extracted from an on-line
     * system using a timing attack and then attacked off-line.
     *
     * @param a the first byte array
     * @param b the second byte array
     * @return true if both byte arrays are the same, false if not
     */
    public static boolean slowEquals(byte[] a, byte[] b) {
        checkIfNulls("one of the byte array is null", a, b);
        int diff = a.length ^ b.length;

        for (int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }


    /**
     * Compares two byte arrays
     *
     * @param a the first byte array
     * @param b the second byte array
     * @return true if both byte arrays are the same, false if not
     * @param    length    the length to be compared
     */
    public static boolean equals(byte[] a, byte[] b, int length) {
        checkIfNulls("one of the byte array is null", a, b);
        if (length < 0 || length > a.length || length > b.length) {
            throw new IllegalArgumentException("Invalid length " + length);
        }


        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true a[i] == b[i]
     *
     * @param a array
     * @param b array
     * @return Return true if every a[i] == b[i]
     */
    public static boolean equals(byte[] a, byte[] b) {
        checkIfNulls("one of the byte array is null", a, b);
        if (a.length != b.length)
            return false;
        int length = a.length;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Convert [o.toString()]
     * @param o to be square bracketed
     * @return [o.toString()]
     */
    public static String toSB(Object o) {
        return toSB(o, false);
    }

    /**
     * Convert [o.toString()] or  '[o.toString()] '
     * @param o to be square bracketed
     * @param addSpace if true add space at the end
     * @return [o.toString()] or  '[o.toString()] '
     */
    public static String toSB(Object o, boolean addSpace) {
        return "[" + o + "]" + (addSpace ? " " : "");
    }
}
