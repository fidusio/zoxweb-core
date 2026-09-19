/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
package org.zoxweb.shared.util;

import org.zoxweb.shared.data.ReferenceIDDAO;
import org.zoxweb.shared.filters.GetValueFilter;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.Const.GNVType;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * SUS (Shared Util Shortcut): the single GWT-safe static utility facade of the shared tree.
 * <p>
 * It absorbed the former {@code SharedUtil} (NV/meta-model, enum, lookup and number helpers)
 * and {@code SharedStringUtil} (string, token, hex and UTF-8 helpers), both since deleted, so
 * callers only need one short prefix. The file is organised in three sections:
 * <ol>
 * <li>core shortcuts (null/empty guards, canonical ids, byte-array helpers);</li>
 * <li>NV / meta-model / enum / collection helpers (ex SharedUtil);</li>
 * <li>string / token / hex helpers (ex SharedStringUtil).</li>
 * </ol>
 * Everything here must stay client-safe: no threads, no java.io.File, no sockets, no JVM-only APIs.
 */
public final class SUS {
    private SUS() {
    }

    // =====================================================================================
    // Section 1: core shortcuts
    // =====================================================================================


    private static final Map<Object, Object> cache = new HashMap<>();


    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * Convert an entire byte array to a hex string.
     *
     * @param bytes the byte array to convert
     * @return hex string representation of the bytes
     */
    public static String fastBytesToHex(byte[] bytes) {
        return fastBytesToHex(bytes, 0, bytes != null ? bytes.length : 0);
    }

    /**
     * Convert a segment of a byte array to a hex string.
     *
     * @param bytes  the byte array to convert
     * @param offset starting position in the array
     * @param len    number of bytes to convert
     * @return hex string representation of the specified byte range
     * @throws NullPointerException      if bytes is null
     * @throws IndexOutOfBoundsException if offset or len are out of range
     */
    public static String fastBytesToHex(byte[] bytes, int offset, int len) {
        checkIfNull("Null bytes", bytes);
        if (offset < 0 || len < 0 || offset + len > bytes.length) {
            throw new IndexOutOfBoundsException("Offset or length out of range");
        }

        char[] hex = new char[len * 2];
        for (int i = offset; i < offset + len; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

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
     * Check if an array is not empty meaning not null and length > 0
     *
     * @param array to be checked
     * @return true if the array exists and not empty
     */
    public static boolean isNotEmpty(Object[] array) {
        return (array != null && array.length != 0);
    }

    /**
     * Check if an array is empty meaning null or length = 0
     *
     * @param array to be checked
     * @return true if the array is null or empty
     */
    public static boolean isEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }

    /**
     * Check if a byte array is not empty meaning not null and length > 0
     *
     * @param array to check
     * @return true if the array exists and not empty
     */
    public static boolean isNotEmpty(byte[] array) {
        return (array != null && array.length != 0);
    }


    /**
     * Check if all bytes in the specified range are zero.
     *
     * @param buffer the byte array to check
     * @param offset starting position in the buffer
     * @param length number of bytes to check
     * @return true if all bytes in the range are zero
     */
    public static boolean areAllBytesZero(byte[] buffer, int offset, int length) {
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
     * @return the nve as NVGenericMap
     */
    public static NVGenericMap toNVGenericMap(NVEntity nve) {
        NVGenericMap ret = new NVGenericMap(nve.getName());
        NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
        for (NVConfig nvc : nvce.getAttributes()) {
            ret.add(nve.lookup(nvc));
        }
        return ret;
    }


    public static NVStringList enumsToStringList(String name, Enum<?>... enums) {
        NVStringList ret = new NVStringList(name);
        for (Enum<?> e : enums) {
            ret.add(enumToString(e));
        }

        return ret;
    }

    public static String enumToString(Enum<?> enumToConvert) {
        if (enumToConvert instanceof GetName) return ((GetName) enumToConvert).getName();
        if (enumToConvert instanceof GetValue) return ((GetValue<?>) enumToConvert).getValue() + "";
        return enumToConvert != null ? enumToConvert.toString() : null;
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


    /**
     * Check if a single object is null and throw NullPointerException if so.
     *
     * @param str error message for the NullPointerException
     * @param obj the object to check
     * @throws NullPointerException if obj is null
     */
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

    /**
     * Update the properties of a GetNVProperties object with values from an NVGenericMap.
     *
     * @param toUpdate the GetNVProperties whose properties will be updated
     * @param value    the NVGenericMap containing new values
     * @return the updated NVGenericMap
     */
    public static NVGenericMap updateGetNVProperties(GetNVProperties toUpdate, NVGenericMap value) {
        return updateNVGenericMap(toUpdate.getProperties(), value);
    }

    /**
     * Update an NVGenericMap with values from another NVGenericMap.
     * Existing entries are updated in place; new entries are added.
     *
     * @param toUpdate the target NVGenericMap to update
     * @param value    the source NVGenericMap containing new values
     * @return the updated toUpdate NVGenericMap
     */
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

    /**
     * Internal helper to append a value to a canonical ID being built.
     *
     * @param pos        current position index in the sequence
     * @param sb         the StringBuilder being constructed
     * @param ignoreNull if true, null values are skipped
     * @param sep        separator character between values
     * @param val        the value to append
     * @return the StringBuilder with the value appended
     */
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
        if (str != null) {
            str = str.trim();

            if (!str.isEmpty()) {
                return str;
            }
        }

        return null;
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


    /**
     * Return the index of the first occurrence of match[] in buffer[]
     *
     * @param buffer
     * @param match
     * @return index of the match, -1 if no match found
     */
    public static int indexOf(byte[] buffer, byte[] match) {
        return indexOf(buffer, 0, buffer.length, match, 0, match.length);
    }

    /**
     * @param buffer      to look into
     * @param fromIndex   start index of buffer
     * @param toIndex     end index of buffer
     * @param match       array to look for
     * @param matchOffset offset in match buffer
     * @param matchLen    length of data from matchOffset
     * @return matching index inside buffer
     */
    public static int indexOf(byte[] buffer, int fromIndex, int toIndex, byte[] match, int matchOffset, int matchLen) {
        return indexOf(buffer.length, buffer, fromIndex, toIndex, match, matchOffset, matchLen);
    }


    public static int indexOf(int bufferLimit, byte[] buffer, int fromIndex, int toIndex, byte[] match, int matchOffset, int matchLen) {
        if (matchOffset < 0 || matchLen < 1 || (matchOffset + matchLen) > match.length || toIndex > bufferLimit || bufferLimit > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        for (int i = fromIndex; i < toIndex; i++) {
            int j = 0;

            for (; j < matchLen && j + i < toIndex; j++) {
                if (buffer[i + j] != match[matchOffset + j]) {
                    break;
                }
            }

            if (j == matchLen) {
                return i;
            }
        }

        return -1;
    }

    /**
     * @param buffer
     * @param bufferStartIndex
     * @param bufferEndIndex
     * @param cs
     * @param csOffset
     * @param csLen
     * @param ignoreCase
     * @return matching index
     */
    public static int indexOf(byte[] buffer, int bufferStartIndex, int bufferEndIndex, CharSequence cs, int csOffset, int csLen, boolean ignoreCase) {
        if (csOffset < 0 || csLen < 1 || (csOffset + csLen) > cs.length() || bufferEndIndex > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        for (int i = bufferStartIndex; i < bufferEndIndex; i++) {
            int j = 0;

            for (; j < csLen && j + i < bufferEndIndex; j++) {
                if (ignoreCase) {
                    if ((buffer[i + j] != Character.toUpperCase(cs.charAt(csOffset + j)) && buffer[i + j] != Character.toLowerCase(cs.charAt(csOffset + j)))) {
                        break;
                    }
                } else if (buffer[i + j] != cs.charAt(csOffset + j)) {
                    break;
                }
            }

            if (j == csLen) {
                return i;
            }
        }

        return -1;
    }
    /**
     * @param buffer
     * @param str
     * @return matching index
     */
    public static int indexOf(byte[] buffer, String str) {
        return indexOf(buffer, 0, buffer.length, str, 0, str.length(), false);
    }
    /**
     * @param buffer
     * @param str
     * @return matching index
     */
    public static int indexOfIgnoreCase(byte[] buffer, String str) {
        return indexOf(buffer, 0, buffer.length, str, 0, str.length(), true);
    }

    // =====================================================================================
    // Section 2: NV / meta-model / enum / collection helpers (formerly SharedUtil)
    // =====================================================================================



    public static void illegalCondition(String message, boolean... conditions)
            throws IllegalArgumentException {
        for (boolean condition : conditions) {
            if (!condition)
                throw new IllegalArgumentException(message);
        }
    }

    //@SuppressWarnings("unchecked")
    public static <T> T getWrappedValue(T v) {
        if (v instanceof WrappedValue) {
            return (T) ((WrappedValue<?>) v).unwrap();
        }

        return v;
    }


    public static Number parseNumber(String number) {
        try {
            Long ret = Long.valueOf(number);
            if (ret <= Integer.MAX_VALUE && ret >= Integer.MIN_VALUE) {
                return Integer.valueOf(ret.intValue());
            }
            return ret;
        } catch (NumberFormatException e) {
        }

        Double ret = Double.valueOf(number);
        if (ret <= Float.MAX_VALUE && ret >= -Float.MAX_VALUE) {
            // missing check for Float,MIN_VALUE
            return Float.valueOf(ret.floatValue());
        }
        return ret;
    }


    public static int parseInt(String strInt) throws NumberFormatException {
        try {
            return Integer.parseInt(strInt);
        } catch (NumberFormatException e) {
        }

        return hexToInt(strInt);
    }


    public static int hexToInt(String strIn) throws NumberFormatException {
        String str = trimOrNull(strIn);
        if (str == null)
            throw new NumberFormatException("Invalid token to convert " + strIn);

        int index;
        if ((index = str.indexOf("x")) != -1 || (index = str.indexOf("X")) != -1) {
            str = str.substring(index + 1);
        }
        return Integer.parseInt(str, 16);
    }


    public static long parseLong(String strInt) throws NumberFormatException {
        try {
            return Long.parseLong(strInt);
        } catch (NumberFormatException e) {
        }

        int index = -1;
        if ((index = strInt.indexOf("x")) != -1 || (index = strInt.indexOf("X")) != -1) {
            strInt = strInt.substring(index + 1);
        }

        return Long.parseLong(strInt, 16);
    }


    public static short parseShort(String strInt) throws NumberFormatException {
        try {
            return Short.parseShort(strInt);
        } catch (NumberFormatException e) {
        }

        int index = -1;
        if ((index = strInt.indexOf("x")) != -1 || (index = strInt.indexOf("X")) != -1) {
            strInt = strInt.substring(index + 1);
        }

        return Short.parseShort(strInt, 16);
    }

    public static Number[] normalizeNumbers(Number... numbers) {
        Class<?>[] classPriority =
                {
                        Integer.class,
                        Long.class,
                        Float.class,
                        Double.class
                };
        int priorityMatch = -1;
        Class<?> type = null;
        for (Number num : numbers) {
            int classIndex = -1;
            for (int i = 0; i < classPriority.length; i++) {
                if (classPriority[i] == num.getClass()) {
                    classIndex = i;
                    break;
                }
            }
            if (classIndex == -1) {
                throw new IllegalArgumentException("Numbers can't be normalized " + Arrays.toString(numbers));
            }
            if (classIndex > priorityMatch) {
                priorityMatch = classIndex;
                type = classPriority[priorityMatch];
            }
        }

        Number[] retVals = new Number[numbers.length];

        for (int i = 0; i < retVals.length; i++) {
            if (type == Integer.class) {
                retVals[i] = Integer.valueOf(numbers[i].intValue());
            } else if (type == Long.class) {
                retVals[i] = Long.valueOf(numbers[i].longValue());
            } else if (type == Float.class) {
                retVals[i] = Float.valueOf(numbers[i].floatValue());
            } else if (type == Double.class) {
                retVals[i] = Double.valueOf(numbers[i].doubleValue());
            }
        }

        return retVals;
    }

    @SuppressWarnings("unchecked")
    public static <T extends NVBase<?>> T numberToNVBase(String name, Number number) {
        if (number instanceof Integer) {
            return (T) new NVInt(name, (Integer) number);
        }
        if (number instanceof Long) {
            return (T) new NVLong(name, (Long) number);
        }
        if (number instanceof Float) {
            return (T) new NVFloat(name, (Float) number);
        }

        if (number instanceof Double) {
            return (T) new NVDouble(name, (Double) number);
        }

        throw new IllegalArgumentException("Unsupported type " + number.getClass());

    }

    public static <V> void putUnique(Map<Long, V> map, long key, V v) {
        synchronized (map) {
            while (map.get(key) != null) {
                key++;
            }

            map.put(key, v);
        }
    }


    public static boolean equals(ValueFilter<?, ?> vf1, ValueFilter<?, ?> vf2) {
        if (vf1 != null && vf2 != null) {
            if (vf1 == vf2) {
                return true;
            }

            if (vf1.toCanonicalID() != null && vf2.toCanonicalID() != null) {
                return vf1.toCanonicalID().equals(vf2.toCanonicalID());
            }
        }

        return false;
    }

    /**
     * This utility method look for the matching enum in case-insensitive fashion.
     *
     * <br>It will try different matches with the following inventory:
     * <ol>
     * <li> try match enum.name();
     * <li> try match enum.toString();
     * <li> try match enum.getName() if enum instance of GetName
     * <li> try match enum.getValue() if enum instance of GetValue
     * </ol>
     *
     * @param list
     * @param str
     * @return matching enum
     */
    @Deprecated
    public static <V extends Enum<?>> V lookupEnum(Enum<?>[] list, String str) {
        return lookupEnum(str, list);
    }


    /**
     * This utility method look for the matching enum in case-insensitive fashion.
     *
     * <br>It will try different matches with the following inventory:
     * <ol>
     * <li> try match enum.name();
     * <li> try match enum.toString();
     * <li> try match enum.getName() if enum instance of GetName
     * <li> try match enum.getValue() if enum instance of GetValue
     * </ol>
     *
     * @param str
     * @param list
     * @return matching enum
     */
    @SuppressWarnings("unchecked")
    public static <V extends Enum<?>> V lookupEnum(String str, Enum<?>... list) {
        if (str != null) {
            for (Enum<?> e : list) {
                if (str.equalsIgnoreCase(e.name())) {
                    return (V) e;
                }

                if (str.equalsIgnoreCase(e.toString())) {
                    return (V) e;
                }

                if (e instanceof GetName && str.equalsIgnoreCase(((GetName) e).getName())) {
                    return (V) e;
                }

                if (e instanceof GetValue && str.equalsIgnoreCase("" + ((GetValue<?>) e).getValue())) {
                    return (V) e;
                }
            }
        }

        return null;
    }


    public static <V extends Enum<?>> V lookupEnum(int ordinal, Enum<?>... list) {
        for (Enum e : list) {
            if (e.ordinal() == ordinal)
                return (V) e;
        }

        return null;
    }


    public static GetName lookupGetName(GetName[] gNames, String name) {
        if (name != null && gNames != null) {
            for (GetName gn : gNames) {
                if (name.equalsIgnoreCase(gn.getName())) {
                    return gn;
                }
            }
        }

        return null;
    }


    public static <T extends Enum<?>> T lookupTypedEnum(T[] list, String str) {
        if (str != null) {
            for (T e : list) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }

                if (str.equalsIgnoreCase(e.toString())) {
                    return e;
                }

                if (e instanceof GetName && str.equalsIgnoreCase(((GetName) e).getName())) {
                    return e;
                }

                if (e instanceof GetValue && str.equalsIgnoreCase("" + ((GetValue<?>) e).getValue())) {
                    return e;
                }
            }
        }

        return null;
    }

    public static Enum<?> matchingEnumContent(Enum<?>[] list, String str) {
        if (str != null) {
            for (Enum<?> e : list) {
                if (contains(str, e.name(), true)) {
                    return e;
                }

                if (contains(str, e.toString(), true)) {
                    return e;
                }

                if (e instanceof GetName && contains(str, ((GetName) e).getName(), true)) {
                    return e;
                }

                if (e instanceof GetValue && contains(str, "" + ((GetValue<?>) e).getValue(), true)) {
                    return e;
                }
            }
        }

        return null;
    }

    public static Enum<?> matchingEnumContent(String str, Enum<?>[] list) {
        if (str != null) {
            for (Enum<?> e : list) {
                if (contains(e.name(), str, true)) {
                    return e;
                }

                if (contains(e.toString(), str, true)) {
                    return e;
                }

                if (e instanceof GetName && contains(((GetName) e).getName(), str, true)) {
                    return e;
                }

                if (e instanceof GetValue && contains("" + ((GetValue<?>) e).getValue(), str, true)) {
                    return e;
                }
            }
        }

        return null;
    }

    /**
     * Returns enum based on given enum class and value.
     *
     * @param enumClass
     * @param value
     * @return matching enum
     */
    public static <E extends Enum<?>> E enumValue(Class<?> enumClass, String value) {
        if (value != null) {
            if (enumClass.isArray()) {
                enumClass = enumClass.getComponentType();
            }

            if (enumClass.isEnum()) {
                Enum<?>[] all = (Enum<?>[]) enumClass.getEnumConstants();
                return lookupEnum(value, all);
            } else {
                throw new IllegalArgumentException(enumClass + " is an enum class");
            }
        }

        return null;
    }


    public static NVBase<?> toNVBasePrimitive(String name, Object value) {
        NVBase<?> ret = null;
        if (value instanceof String) {
            ret = new NVPair(name, (String) value);
        } else if (value instanceof Boolean) {
            ret = new NVBoolean(name, (Boolean) value);
        } else if (value instanceof Integer) {
            ret = new NVInt(name, (Integer) value);
        } else if (value instanceof Long) {
            ret = new NVLong(name, (Long) value);
        } else if (value instanceof Float) {
            ret = new NVFloat(name, (Float) value);
        } else if (value instanceof Double) {
            ret = new NVDouble(name, (Double) value);
        } else if (value instanceof Enum) {
            ret = new NVEnum(name, (Enum<?>) value);
        } else if (value instanceof byte[]) {
            ret = new NVBlob(name, (byte[]) value);
        } else if (value instanceof BigDecimal) {
            ret = new NVBigDecimal(name, (BigDecimal) value);
        } else if (value instanceof List) {
            List<?> temp = (List<?>) value;
            if (temp.size() > 0) {
                if (temp.get(0) instanceof String) {
                    ret = new NVStringList(name);
                    for (Object v : temp) {
                        ((NVStringList) ret).getValue().add((String) v);
                    }
                } else if (temp.get(0) instanceof Double) {
                    ret = new NVDoubleList(name);
                    for (Object v : temp) {
                        ((NVDoubleList) ret).getValue().add((Double) v);
                    }
                } else if (temp.get(0) instanceof Float) {
                    ret = new NVFloatList(name);
                    for (Object v : temp) {
                        ((NVFloatList) ret).getValue().add((Float) v);
                    }
                } else if (temp.get(0) instanceof Integer) {
                    ret = new NVIntList(name);
                    for (Object v : temp) {
                        ((NVIntList) ret).getValue().add((Integer) v);
                    }
                } else if (temp.get(0) instanceof Long) {
                    ret = new NVLongList(name);
                    for (Object v : temp) {
                        ((NVLongList) ret).getValue().add((Long) v);
                    }
                }
            }
        }


        return ret;
    }

    /**
     * Parses a name = value String and return an NVPair object.
     *
     * @param str
     * @return parse name=value into nvpair
     */
    public static NVPair toNVPair(String str) {
        return toNVPair(str, "=", false);
    }

    /**
     * Converts a string to a NVPair based on the first occurrence of the sep in str.
     *
     * @param str
     * @param sep
     * @return parse name sep value into nvpair
     */
    public static NVPair toNVPair(String str, String sep) {
        return toNVPair(str, sep, false);
    }

    public static NVPair toNVPair(String str, String sep, boolean trim) {
        NVPair ret = null;
        str = trimOrNull(str);

        if (str != null) {
            int index = str.indexOf(sep);

            if (index != -1) {
                String name = trimOrNull(str.substring(0, index));
                String value = str.substring(index + sep.length());

                if (value.isEmpty()) {
                    value = null;
                }

                if (value != null && trim) {
                    value = value.trim();
                }

                if (name != null) {
                    ret = new NVPair(name, value);
                }
            }
        }

        return ret;
    }

    /**
     * Looks up the NVBase on name.
     *
     * @param list
     * @param name
     * @return nvbase that matches getName()
     */
    public static NVBase<?> lookupNVPB(List<NVBase<?>> list, String name) {
        if (name != null) {
            for (NVBase<?> nvpb : list) {
                if (name.equalsIgnoreCase(nvpb.getName())) {
                    return nvpb;
                }
            }
        }

        return null;
    }

    /**
     * Looks up the NVBase based on enum.
     *
     * @param list
     * @param e
     * @return nvbase that matches getName()
     */
    public static NVBase<?> lookupNVPB(List<NVBase<?>> list, Enum<?> e) {
        if (e != null) {
            for (NVBase<?> nvpb : list) {
                if (e.name().equalsIgnoreCase(nvpb.getName())) {
                    return nvpb;
                } else if (e instanceof GetName) {
                    String n = ((GetName) e).getName();

                    if (n.equalsIgnoreCase(nvpb.getName())) {
                        return nvpb;
                    }
                } else if (e instanceof GetNVConfig) {
                    String n = ((GetNVConfig) e).getNVConfig().getName();

                    if (n.equalsIgnoreCase(nvpb.getName())) {
                        return nvpb;
                    } else {
                        n = ((GetNVConfig) e).getNVConfig().getDisplayName();

                        if (n.equalsIgnoreCase(nvpb.getName())) {
                            return nvpb;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Looks NV list based on name.
     *
     * @param list the list to search
     * @param name the name to look for
     * @param <V> the value type
     * @return GetNameValue matching name
     */
    public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> list, String name) {
        return lookupNV(list, name, null);
    }

    /**
     * Looks up NV based on name and canonical separator.
     *
     * @param list the list to search
     * @param name the name to look for
     * @param canonicalSep the canonical separator
     * @param <V> the value type
     * @return GetNameValue matching name
     */
    public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> list, String name, String canonicalSep) {
        if (name != null) {
            if (canonicalSep != null) {
                name = parseNameValue(name, canonicalSep)[0];
            }

            for (GetNameValue<V> nvp : list) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    return nvp;
                }
            }
        }

        return null;
    }

    /**
     * Looks up value based on name.
     *
     * @param list
     * @param name
     * @return return the value that matches name
     */
    public static <V> V lookupValue(List<? extends GetNameValue<V>> list, String name) {
        V ret = null;

        if (name != null) {
            for (GetNameValue<V> nvp : list) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    ret = nvp.getValue();
                    break;
                }
            }
        }

        return ret;
    }

    public static <V> V lookupValue(ArrayValues<GetNameValue<V>> list, String name) {
        V ret = null;

        if (name != null) {
            for (GetNameValue<V> nvp : list.values()) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    ret = nvp.getValue();
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Looks up value based on enum.
     *
     * @param list
     * @param e
     * @return lookup value that matched e name
     */
    public static <V> V lookupValue(List<? extends GetNameValue<V>> list, Enum<?> e) {
        V ret = null;

        if (e != null) {
            for (GetNameValue<V> nvp : list) {
                if (e.name().equalsIgnoreCase(nvp.getName())) {
                    ret = nvp.getValue();
                    break;
                } else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName())) {
                    ret = nvp.getValue();
                    break;
                }
            }
        }

        return ret;
    }


    public static <V> V lookupValue(GetNameValue<V> v) {
        if (v != null) {
            return v.getValue();
        }

        return null;
    }

    /**
     * Looks up list which GetName based on name.
     *
     * @param list
     * @param name
     * @return return the matching GetName that matches name
     */
    @SuppressWarnings("unchecked")
    public static <V> V lookup(List<? extends GetName> list, String name) {
        V ret = null;

        if (name != null) {
            for (GetName nvp : list) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    ret = (V) nvp;
                    break;
                }
            }
        }

        return ret;
    }

    public static <V> V lookupMap(Map<String, V> map, String key, boolean ignoreCase) {
        if (key != null) {
            if (ignoreCase) {
                Set<Map.Entry<String, V>> set = map.entrySet();
                Iterator<Map.Entry<String, V>> it = set.iterator();
                while (it.hasNext()) {
                    Map.Entry<String, V> entry = it.next();
                    if (key.equalsIgnoreCase(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        }

        return map.get(key);
    }

    public static <K, V> V lookupMapSmart(Map<K, V> map, K key) {
        V ret = map.get(key);
        if (ret == null && key != null && key instanceof String) {
            String keyAsString = (String) key;
            ret = map.get(keyAsString.toLowerCase());
            if (ret == null) {
                ret = map.get(keyAsString.toUpperCase());
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <V> List<V> search(GetName[] list, String... name) {
        List<V> ret = new ArrayList<V>();

        if (name != null && name.length > 0 && name[0] != null) {
            for (GetName nvp : list) {
                if (name[0].equalsIgnoreCase(nvp.getName())) {
                    ret.add((V) nvp);
                }
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <V> List<V> search(List<? extends GetName> list, String... name) {
        List<V> ret = new ArrayList<V>();

        if (name != null && name.length > 0 && name[0] != null) {
            for (GetName nvp : list) {
                if (name[0].equalsIgnoreCase(nvp.getName())) {
                    ret.add((V) nvp);
                }
            }
        }

        return ret;
    }

    public static int signum(int val) {
        if (val > 0)
            return 1;
        if (val < 0)
            return -1;
        return 0;
    }

    public static int signum(long val) {
        if (val > 0)
            return 1;
        if (val < 0)
            return -1;
        return 0;
    }


    /**
     * Looks up list which extends GetName based on enum.
     *
     * @param list
     * @param e
     * @return return the matching GetName that matches e
     */
    @SuppressWarnings("unchecked")
    public static <V> V lookup(List<? extends GetName> list, Enum<?> e) {
        V ret = null;

        if (e != null) {
            for (GetName nvp : list) {
                if (e.name().equalsIgnoreCase(nvp.getName())) {
                    ret = (V) nvp;
                    break;
                } else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName())) {
                    ret = (V) nvp;
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Removes the prefix in a string and returns a substring of the original string
     * less the prefix
     *
     * @param prefix
     * @param str
     * @return str stripped of prefix
     */
    public static String removePrefix(String prefix, String str) {
        if (prefix != null && str.startsWith(prefix)) {
            // tele-sign do not like the +
            str = str.substring(prefix.length());
        }

        return str;
    }

    public static byte[] reverseBytes(byte[] array) {
        byte[] ret = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[array.length - (i + 1)];
        }
        return ret;
    }

    /**
     * Looks up NV list that extends GetNameValue based on enum.
     *
     * @param arrayList the list to search
     * @param e the enum to match
     * @param <V> the value type
     * @return matching GetNameValue
     */
    public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> arrayList, Enum<?> e) {
        if (e != null) {
            for (GetNameValue<V> nvp : arrayList) {
                if (e.name().equalsIgnoreCase(nvp.getName())) {
                    return nvp;
                } else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName())) {
                    return nvp;
                }
            }
        }

        return null;
    }


    public static <T> List<List<T>> combinationsAsList(boolean addEmpty, T... array) {
        List<List<T>> powerSet = new ArrayList<>();
        // Start with the empty subset
        List<T> empty = new ArrayList<>();
        powerSet.add(empty);

        // For each element, add it to all existing subsets to form new subsets
        for (T element : array) {
            // Remember current number of subsets so far
            int n = powerSet.size();
            for (int i = 0; i < n; i++) {
                // Create a new subset from the existing subset
                List<T> subset = new ArrayList<>(powerSet.get(i));
                subset.add(element);
                powerSet.add(subset);
            }
        }

        if (!addEmpty)
            powerSet.remove(empty);
        return powerSet;
    }


    public static <T> Set<Set<T>> combinationsAsSet(boolean addEmpty, T... array) {
        Set<Set<T>> powerSet = new LinkedHashSet<>();
        // Start with the empty subset
        Set<T> empty = new LinkedHashSet<>();
        powerSet.add(empty);

        // For each element, add it to all existing subsets to form new subsets
        for (T element : array) {
            // Remember current number of subsets so far
            Set<T>[] arraySets = powerSet.toArray(new Set[0]);
            for (int i = 0; i < arraySets.length; i++) {
                // Create a new subset from the existing subset
                Set<T> subset = new HashSet<>(arraySets[i]);
                subset.add(element);
                powerSet.add(subset);
            }
        }

        if (!addEmpty)
            powerSet.remove(empty);
        return powerSet;
    }

    /**
     * This method converts an object array into a string.
     * Ex. String str[] = new String[3];
     * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
     * toString(str) returns [0]:Zox [1]:Web [2]:Core
     *
     * @param array
     * @return obj[0] + \n + ob[1] +\n + ...
     */
    public static String toString(Object[] array) {
        return toString(array, "\n");
    }

    /**
     * This method converts an object array into a string which contains
     * a specified string that separates each value of the array.
     * Ex. String str[] = new String[3];
     * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
     * toString(str, "-") returns [0]:Zox-[1]:Web-[2]:Core
     *
     * @param array
     * @param sep
     * @return obj[0] + sep + ob[1] +sep  + ...
     */
    public static String toString(Object[] array, String sep) {
        return toString(array, sep, true);
    }

    /**
     * This method converts an object array into a string which contains
     * a specified string that separates each value of the array. Also, if index
     * is false, returns only string without the value of its location within the
     * object array (in brackets). Otherwise if true, all characters are included.
     * * * Ex. String str[] = new String[3];
     * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
     * toString(str, "-",false) returns Zox-Web-Core
     *
     * @param array
     * @param sep
     * @param index
     * @return formatted string
     */
    public static String toString(Object[] array, String sep, boolean index) {
        StringBuilder sb = new StringBuilder();

        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (index) {
                    sb.append("[");
                    sb.append(i);
                    sb.append("]:");
                }

                sb.append(array[i]);

                if (i + 1 != array.length) {
                    sb.append(sep);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Converts NVEntity to debug string.
     *
     * @param nve
     * @return debug string
     */
    public static String toDebugString(NVEntity nve) {
        StringBuilder sb = new StringBuilder();

        if (nve != null) {
            sb.append("[" + nve.getClass().getName() + "]\n");

            for (NVBase<?> nvb : nve.getAttributes().values()) {
                sb.append("\t" + nvb.getClass().getName() + ",");

                if (nvb instanceof NVEntityReference) {
                    sb.append(nvb.getName() + ":" + toDebugString((NVEntity) nvb.getValue()) + "\n");
                }

                if (nvb instanceof NVEntityReferenceList) {
                    NVEntityReferenceList tempList = (NVEntityReferenceList) nvb;

                    for (NVEntity nveTemp : tempList.getValue()) {
                        sb.append(nvb.getName() + ":" + toDebugString(nveTemp) + "\n");
                    }
                } else {
                    sb.append(nvb.getName() + ":" + nvb.getValue() + "\n");
                }
            }
        } else {
            sb.append("null");
        }

        return sb.toString();
    }


    /**
     * Looks up array values based on given String.
     *
     * @param arrayValues
     * @param name
     * @return list that matches name
     */
    public static <V> List<? extends GetNameValue<V>> lookupArrayValues(ArrayValues<? extends GetNameValue<V>> arrayValues, String name) {
        ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>();

        if (name != null) {
            for (GetNameValue<V> nvp : arrayValues.values()) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                }
            }
        }

        return ret;
    }

    /**
     * Looks up array values based on given enum.
     *
     * @param arrayValues
     * @param e
     * @return list that matches e
     */
    public static <V> List<? extends GetNameValue<V>> lookupArrayValues(ArrayValues<? extends GetNameValue<V>> arrayValues, Enum<?> e) {
        ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>();

        if (e != null) {
            for (GetNameValue<V> nvp : arrayValues.values()) {
                if (e.name().equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                } else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                }
            }
        }

        return ret;
    }

    /**
     * Looks up all NV list that extends GetNameValue based on enum.
     *
     * @param arrayList
     * @param e
     * @return list that matches e
     */
    public static <V> List<? extends GetNameValue<V>> lookupAllNV(List<GetNameValue<V>> arrayList, Enum<?> e) {
        ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>();

        if (e != null) {
            for (GetNameValue<V> nvp : arrayList) {
                if (e.name().equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                } else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                }
            }
        }

        return ret;
    }

    /**
     * @param arrayList
     * @param name
     * @return list that matches name
     */
    public static <V> List<? extends GetNameValue<V>> lookupAllNV(List<? extends GetNameValue<V>> arrayList, String name) {
        return lookupAllNV(arrayList, name, null);
    }

    /**
     * @param arrayList
     * @param name
     * @param canonicalSep
     * @return list that matches name
     */
    public static <V> List<? extends GetNameValue<V>> lookupAllNV(List<? extends GetNameValue<V>> arrayList, String name, String canonicalSep) {
        ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>();

        if (name != null) {
            if (canonicalSep != null) {
                name = parseNameValue(name, canonicalSep)[0];
            }

            for (GetNameValue<V> nvp : arrayList) {
                if (name.equalsIgnoreCase(nvp.getName())) {
                    ret.add(nvp);
                }
            }
        }

        return ret;
    }


    /**
     * @param nvMap
     * @return convert map to list nvpairs
     */
    public static <V> List<? extends GetNameValue<String>> toNVPairs(Map<String, String[]> nvMap) {
        return toNVPairs(nvMap, false);
    }

    /**
     * @param nvMap
     * @param nullAllowed
     * @return convert map to list nvpairs
     */
    public static <V> List<? extends GetNameValue<String>> toNVPairs(Map<String, String[]> nvMap, boolean nullAllowed) {
        List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();

        for (Map.Entry<String, String[]> nvp : nvMap.entrySet()) {
            if (nvp != null) {
                String[] values = nvp.getValue();

                if (values != null) {
                    for (String value : values) {
                        if (!nullAllowed) {
                            value = trimOrNull(value);
                        }

                        if ((nullAllowed || value != null) && nvp.getKey() != null) {
                            ret.add(new NVPair(nvp.getKey(), value));
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * @param enums
     * @return convert enum to list nvpairs
     */
    @SuppressWarnings("unchecked")
    public static <V> List<? extends GetNameValue<String>> toNVPairs(Enum<?>... enums) {
        List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();

        for (Enum<?> e : enums) {
            NVPair nvp = new NVPair();

            if (e instanceof GetName) {
                nvp.setName(((GetName) e).getName());
            } else {
                nvp.setName(e.name());
            }

            if (e instanceof GetValue) {
                nvp.setValue(((GetValue<String>) e).getValue());
            }

            if (e instanceof GetValueFilter) {
                ValueFilter<String, String> vf = ((GetValueFilter<String, String>) e).getValueFilter();

                //nvp.setValueFilter(((GetValueFilter<String, String>)e).getValueFilter());
                if (vf instanceof DynamicEnumMap) {
                    vf = DynamicEnumMapManager.SINGLETON.lookup(((DynamicEnumMap) vf).getName());
                }

                nvp.setValueFilter(vf);
            }

            ret.add(nvp);
        }

        return ret;
    }

    /**
     * @param nvMap
     * @return convert map to list nvpairs
     */
    public static ArrayList<? extends GetNameValue<String>> listToNVPairs(Map<String, List<String>> nvMap) {
        ArrayList<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();

        for (Map.Entry<String, List<String>> nvp : nvMap.entrySet()) {
            if (nvp != null) {
                List<String> values = nvp.getValue();

                if (values != null) {
                    for (String value : values) {
                        ret.add(new NVPair(nvp.getKey(), value));
                    }
                }
            }
        }

        return ret;
    }

    /**
     * @param paramList
     * @param configList
     * @return true if all mandatory parameters are set
     */
    public static boolean areAllMandatorySet(List<NVPair> paramList, GetNVConfig[] configList) {
        return (firstMissingMandatory(paramList, configList) == null);
    }

    /**
     * @param paramList
     * @param configList
     * @return the first not set mandatory nvconfig
     */
    public static NVConfig firstMissingMandatory(List<NVPair> paramList, GetNVConfig[] configList) {
        for (GetNVConfig con : configList) {
            NVConfig config = con.getNVConfig();

            if (config.isMandatory()) {
                if (lookupNV(paramList, config.getName()) == null) {
                    if (lookupNV(paramList, (Enum<?>) con) != null) {
                        continue;
                    }

                    return config;
                }
            }
        }

        return null;
    }

    public static NVBase<?> classToNVBase(Class<?> c, String name, String value) {
        checkIfNulls("Class or name can't be null", c, name);
        c = Const.wrap(c);
        NVBase<?> nvbArray = null;
        if (c.isArray()) {
            //enum must be checked first
            if (c.getComponentType().isEnum()) {
                nvbArray = new NVEnumList(name, new ArrayList<Enum<?>>());
            } else if (String[].class.equals(c)) {
                nvbArray = new NVStringList(name);
            } else if (Long[].class.equals(c)) {
                nvbArray = new NVLongList(name, new ArrayList<Long>());
            } else if (byte[].class.equals(c)) {
                nvbArray = new NVBlob(name, null);
            } else if (Integer[].class.equals(c)) {
                nvbArray = new NVIntList(name);
            } else if (Float[].class.equals(c)) {
                nvbArray = new NVFloatList(name);
            } else if (Double[].class.equals(c)) {
                nvbArray = new NVDoubleList(name);
            } else if (Date[].class.equals(c)) {
                nvbArray = new NVLongList(name);
            } else if (BigDecimal[].class.equals(c)) {
                nvbArray = new NVBigDecimalList(name);
            } else
                throw new IllegalArgumentException("Unsupported class:" + c);
        } else
            return internalClassToNVBase(c, name, value);

        if (value != null) {
            String[] values = value.split(",");
            List<Object> arrayValue = (List<Object>) nvbArray.getValue();
            for (String v : values) {
                NVBase<?> result = internalClassToNVBase(c.getComponentType(), name, v);
                arrayValue.add(result.getValue());
            }

            return nvbArray;
        }

        return null;


    }


    private static NVBase<?> internalClassToNVBase(Class<?> c, String name, String value) {
        if (c.isEnum()) {

            Enum<?> enumValue = null;
            if (value != null) {
                enumValue = lookupEnum(value, (Enum<?>[]) c.getEnumConstants());
                if (enumValue == null)
                    throw new IllegalArgumentException(value + " is not a valid enum");
            }

            return new NVEnum(name, enumValue);
        } else if (String.class.equals(c)) {
            NVPair nvp = new NVPair(name, value);
            return nvp;
        } else if (Long.class.equals(c)) {
            return new NVLong(name, value != null ? Long.parseLong(value) : 0);
        } else if (Integer.class.equals(c)) {
            return new NVInt(name, value != null ? Integer.parseInt(value) : 0);
        } else if (Boolean.class.equals(c)) {
            if (name.equalsIgnoreCase(value)) {
                return new NVBoolean(name, true);
            }

            return new NVBoolean(name, value != null ? Const.Bool.lookupValue(value) : false);
        } else if (Float.class.equals(c)) {
            return new NVFloat(name, value != null ? Float.parseFloat(value) : 0);
        } else if (Double.class.equals(c)) {
            return new NVDouble(name, value != null ? Double.parseDouble(value) : 0);
        } else if (Date.class.equals(c)) {
            return new NVLong(name, 0);
        } else if (BigDecimal.class.equals(c)) {
            return new NVBigDecimal(name, new BigDecimal(value));
        } else if (Number.class.equals(c)) {
            return new NVNumber(name, null);
        }

        throw new IllegalArgumentException("Unsupported class:" + c);
    }


    @SuppressWarnings("unchecked")
    public static <T> T parsePrimitiveValue(GNVType type, Number n) {
        switch (type) {

            case NVDOUBLE:
                return (T) Double.valueOf(n.doubleValue());

            case NVFLOAT:
                return (T) Float.valueOf(n.floatValue());
            case NVINT:
                return (T) Integer.valueOf(n.intValue());
            case NVLONG:
                return (T) Long.valueOf(n.longValue());
            default:
                throw new IllegalArgumentException("Invalid type " + type);

        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T parsePrimitiveValue(GNVType type, String v) {
        switch (type) {

            case NVDOUBLE:
                return (T) Double.valueOf(v);

            case NVFLOAT:
                return (T) Float.valueOf(v);
            case NVINT:
                return (T) Integer.valueOf(v);
            case NVLONG:
                return (T) Long.valueOf(v);
            default:
                throw new IllegalArgumentException("Invalid type " + type);

        }
    }

    public static GetNameValueComment<String> parseGetNameStringComment(String line, String nvSeparator, String... commentTags) {
        line = trimOrNull(line);
        if (line != null) {
            if (!isComment(line)) {
                if (nvSeparator != null) {
                    int nvSepIndex = line.indexOf(nvSeparator);
                    int commentIndex = indexOf(line, commentTags);

                    if (commentIndex != -1 && nvSepIndex != -1 && commentIndex <= nvSepIndex)
                        return null;

                    String name = null;
                    String value = null;
                    String comment = null;
                    if (nvSepIndex != -1) {
                        name = line.substring(0, nvSepIndex);
                        if (commentIndex != -1) {
                            value = line.substring(nvSepIndex + nvSeparator.length(), commentIndex).trim();
                            comment = line.substring(commentIndex).trim();
                        } else {
                            value = line.substring(nvSepIndex + nvSeparator.length()).trim();
                        }

                        return new GetNameValueComment<String>(new NVPair(name, value), comment);
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param config
     * @param value
     * @return convert string to value dictated by nvconfig
     */
    public static Object stringToValue(NVConfig config, String value) {
        Class<?> c = config.getMetaType();

        if (c.isArray()) {
            throw new IllegalArgumentException(config + " Cannot be converted, is of type array.");
        } else {
            // Not array
            if (c.isEnum()) {
                return lookupEnum(value, (Enum<?>[]) c.getEnumConstants());
            } else if (String.class.equals(c)) {
                return value;
            } else if (Long.class.equals(c)) {
                if (!isEmpty(value)) {
                    return Long.valueOf(value);
                } else if (!config.isMandatory()) {
                    return Long.valueOf(0);
                }
            } else if (Integer.class.equals(c)) {
                if (!isEmpty(value)) {
                    return Integer.valueOf(value);
                } else if (!config.isMandatory()) {
                    return Integer.valueOf(0);
                }
            } else if (Boolean.class.equals(c)) {
                return Boolean.valueOf(value);
            } else if (Float.class.equals(c)) {
                if (!isEmpty(value)) {
                    return Float.valueOf(value);
                } else if (!config.isMandatory()) {
                    return Float.valueOf((float) 0.0);
                }
            } else if (Double.class.equals(c)) {
                if (!isEmpty(value)) {
                    return Double.valueOf(value);
                } else if (!config.isMandatory()) {
                    return Double.valueOf(0.0);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported type " + config);
    }

    /**
     * First checks whether the class is of array type. Then checks the primitive data type of
     * class c and returns true if class type is primitive, otherwise returns false.
     *
     * @param c
     * @return true if primitive note string is considered primitive
     */
    public static boolean isPrimitive(Class<?> c) {
        checkIfNulls("Class is null.", c);

        if (c.isArray()) {
            c = c.getComponentType();
        }

        if (c.isPrimitive() || c.equals(String.class) ||
                c.equals(Long.class) || c.equals(Integer.class) || c.equals(Float.class) || c.equals(Double.class)) {
            return true;
        }

        return false;
    }


    /**
     * @param array
     * @return extract the nvconfig from the GetNVConfig array
     */
    public static List<NVConfig> extractNVConfigs(GetNVConfig... array) {
        ArrayList<NVConfig> ret = null;

        if (array != null) {
            ret = new ArrayList<NVConfig>();

            for (int i = 0; i < array.length; i++) {
                ret.add(array[i].getNVConfig());
            }

        }

        return ret;
    }


    @SuppressWarnings("unchecked")
    public static <V> List<V> addTo(List<V> list, V... toAdd) {
        if (list != null && toAdd != null) {
            for (V v : toAdd) {
                list.add(v);
            }
        }
        return list;
    }

    /**
     * @param array
     * @return convert NVConfig array to List
     */
    public static ArrayList<NVConfig> toNVConfigList(NVConfig... array) {
        ArrayList<NVConfig> ret = null;

        if (array != null) {
            ret = new ArrayList<NVConfig>();

            for (int i = 0; i < array.length; i++) {
                ret.add(array[i]);
            }
        }

        return ret;
    }

    /**
     * @param list
     * @param toAdd
     * @return merged list + toAdd
     */
    public static List<NVConfigEntity> merge(List<NVConfigEntity> list, NVConfigEntity... toAdd) {
        if (list == null) {
            list = new ArrayList<NVConfigEntity>();
        }

        for (NVConfigEntity nvce : toAdd) {
            list.add(nvce);
        }

        return list;
    }


    @SafeVarargs
    public static List<NVConfig> mergeMeta(List<NVConfig>... cList) {
        return mergeMeta(true, cList);
    }

    @SafeVarargs
    public static List<NVConfig> mergeMeta(boolean deepCopy, List<NVConfig>... cList) {
        List<NVConfig> first = null;

        for (int i = 0; i < cList.length; i++) {
            List<NVConfig> c = cList[i];

            if (first == null) {
                first = c;
                continue;
            }

            first.addAll(0, c);
        }

        return first;
    }


    /**
     * Converts NVPair list to GetNameValue list.
     * @param list the list to convert
     * @return converted list of GetNameValue
     */
    public static List<GetNameValue<String>> toNVList(List<NVPair> list) {
        List<GetNameValue<String>> ret = null;

        if (list != null) {
            ret = new ArrayList<GetNameValue<String>>();

            for (NVPair nvp : list) {
                ret.add(nvp);
            }
        }

        return ret;
    }

    /**
     * Parse a line that contains a list of name=value separated by ampersand, carriage return or newline.
     *
     * @param str the string to parse
     * @param nvpSep the name nvpSep value
     * @param regExp the separator nvp1 rexExp nvp2
     * @return list nvpair (name sep value regExp)+
     */
    public static List<NVPair> toNVPairs(String str, String nvpSep, String regExp) {
        String[] pairs = parseString(str, regExp, (CharSequence[]) null);
        ArrayList<NVPair> ret = new ArrayList<NVPair>();

        for (String p : pairs) {
            NVPair nv = toNVPair(p, nvpSep, false);

            if (nv != null) {
                ret.add(nv);
            }
        }
        return ret;
    }


    public static NVGenericMap toNVGenericMap(NVGenericMap nvgm, String str, String nvpSep, String regExp, boolean trim) {
        String[] pairs = parseString(str, regExp, (CharSequence[]) null);
        NVGenericMap ret = nvgm != null ? nvgm : new NVGenericMap();

        for (String p : pairs) {
            NVPair nv = toNVPair(p, nvpSep, trim);

            if (nv != null) {
                ret.add(nv);
            }
        }
        return ret;
    }


    public static NVGenericMap toNVGenericMap(NVGenericMap nvgm, Collection<String> collection, String nvSep, boolean trim) {
        NVGenericMap ret = nvgm != null ? nvgm : new NVGenericMap();
        for (String toParse : collection) {
            NVPair nv = toNVPair(toParse, nvSep, trim);

            if (nv != null) {
                ret.build(nv);
            }
        }
        return ret;
    }

    /**
     * @param list
     * @param nameOfNVToBeFiltered
     * @return something
     */
    public static List<GetNameValue<String>> filterNV(List<GetNameValue<String>> list, String nameOfNVToBeFiltered) {
        List<GetNameValue<String>> ret = null;

        if (nameOfNVToBeFiltered != null) {
            for (GetNameValue<String> nvp : list) {
                if (!nameOfNVToBeFiltered.equalsIgnoreCase(nvp.getName())) {
                    if (ret == null) {
                        ret = new ArrayList<GetNameValue<String>>();
                    }

                    ret.add(nvp);
                }
            }
        }

        if (ret == null) {
            ret = list;
        }

        return ret;
    }



    /**
     * @param gnv
     * @return value
     */
    public static <V> V getValue(GetNameValue<V> gnv) {
        if (gnv != null) {
            return gnv.getValue();
        }

        return null;
    }








    /**
     * @param name
     * @param value
     * @param nameValueSep
     * @param quotedValue
     * @return formatted string name sep quote value quote
     */
    public static <V> String format(String name, V value, String nameValueSep, boolean quotedValue) {
        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append(name);
            sb.append(nameValueSep);
        }

        if (value != null) {
            if (quotedValue) {
                sb.append('\"');
            }

            sb.append(value);

            if (quotedValue) {
                sb.append('\"');
            }
        }

        return sb.toString();
    }

    /**
     * This method will check if value is null then return defaultValue otherwise it will return value.
     * The value and defaultValue can not be null simultaneously.
     *
     * @param value the value to check
     * @param defaultValue the default value if value is null
     * @param <V> the value type
     * @return override null with default value
     * @throws NullPointerException if both defaultValue and value are null
     */
    public static <V extends Object> V nullToDefault(V value, V defaultValue)
            throws NullPointerException {

        if (value == null && defaultValue == null) {
            throw new NullPointerException("value and defaultValue can not be set to null simultaneously");
        }

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static long referenceIDToLong(ReferenceID<?> refID) {
        if (refID != null && refID.getReferenceID() != null) {
            return Long.parseLong("" + refID.getReferenceID());
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    public static <V> void validate(NVConfig nvc, NVBase<V> nvb, boolean setValue) {
        if (nvb != null) {
            if (nvb.getValue() == null && nvc.isMandatory()) {
                throw new NullPointerException("attribute " + nvc + " is a required value can't be null");
            }

            ValueFilter<Object, Object> vf = (ValueFilter<Object, Object>) nvc.getValueFilter();

            if (vf != null) {
                if (setValue) {
                    V v = nvb.getValue();

                    if (nvc.isArray() && v instanceof List) {
                        List<Object> list = (List<Object>) v;

                        for (int i = 0; i < list.size(); i++) {
                            Object value = list.get(i);

                            if (value instanceof NVPair) {
                                ((NVPair) value).setValue((String) vf.validate(((NVPair) value).getValue()));
                            } else {
                                value = vf.validate(value);
                            }

                            list.set(i, value);
                        }
                    } else {
                        nvb.setValue((V) vf.validate(nvb.getValue()));
                    }
                } else if (!nvc.isArray()) {
                    vf.validate(nvb.getValue());
                }
            }
        }
    }

    public static void validate(NVEntity nve, boolean setValue, boolean validateRecursive) {
        NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();

        for (NVConfig nvc : nvce.getAttributes()) {
            //Logic needs to be changed, NVEntiy must have reference ID implementation.
            if (nvc != ReferenceIDDAO.NVC_GUID && nvc != ReferenceIDDAO.NVC_SUBJECT_GUID) {

                if (validateRecursive && nvc instanceof NVConfigEntity) {
                    NVConfigEntity nvcetemp = (NVConfigEntity) nvc;

                    if (!nvcetemp.isArray()) {
                        NVEntityReference nver = (NVEntityReference) nve.lookup(nvc.getName());
                        validate(nver.getValue(), setValue, validateRecursive);
                    }
                } else {
                    validate(nvc, nve.lookup(nvc.getName()), setValue);
                }
            }
        }
    }

    /**
     * Returns a copy of the given NVPair list.
     *
     * @param list NVPair list to copy
     * @return copied NVPair list
     */
    public static List<NVPair> copy(List<NVPair> list) {
        List<NVPair> ret = new ArrayList<NVPair>();

        for (NVPair nvp : list) {
            ret.add(copy(nvp));
        }

        return ret;
    }

    /**
     * Returns a copy of the given NVPair.
     *
     * @param nvp NVPair to copy
     * @return copied NVPair
     */
    public static NVPair copy(NVPair nvp) {
        NVPair ret = new NVPair();

        ret.setName(nvp.getName());
        ret.setValue(nvp.getValue());
        ret.setValueFilter(nvp.getValueFilter());

        return ret;
    }

    public static boolean doesNameExistNVList(List<NVPair> list, String name) {
        return lookup(list, name) != null;
    }

    public static <V extends TimeStampInterface> V touch(V ts, CRUD... ops) {
        checkIfNulls("Document info is null.", ts);

        if (ts.getCreationTime() == 0) {
            ts.setCreationTime(System.currentTimeMillis());
        }

        if (ops == null || ops.length == 0) {
            ts.setLastTimeUpdated(System.currentTimeMillis());
            ts.setLastTimeRead(System.currentTimeMillis());
        } else {
            for (CRUD op : ops) {
                if (op != null) {
                    switch (op) {
                        case CREATE:
                            if (ts.getCreationTime() == 0) {
                                ts.setCreationTime(System.currentTimeMillis());
                            }
                            break;
                        case READ:
                            ts.setLastTimeRead(System.currentTimeMillis());
                            break;
                        case UPDATE:
                            ts.setLastTimeUpdated(System.currentTimeMillis());
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return ts;
    }

    public static boolean doesNVEntityExist(List<? extends NVEntity> list, String referenceID) {
        if (list != null && referenceID != null) {
            for (NVEntity nve : list) {
                if (nve.getReferenceID() != null && nve.getReferenceID().equals(referenceID)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void close(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {

            }
        }
    }


    public static boolean equals(IsValid validator, byte[] a1, int a1From, int a1To, byte[] a2, int a2From, int a2To) {
        if (a1 == a2)
            return true;

        if (a1 == null || a2 == null)
            return false;

        int len = a1To - a1From;
        if (a1From < 0 ||
                a1To < 0 ||
                a2From < 0 ||
                a2To < 0 ||
                len < 0 ||
                (a2To - a2From) < 0 ||
                len != a2To - a2From)
            return false;

        for (int i = 0; i < len; i++) {
            if ((validator != null && !validator.isValid()) ||
                    a1[a1From + i] != a2[a2From + i])
                return false;
        }

        return true;
    }


    public static int hashCode(byte[] a, int offset, int length) {
        if (a == null)
            return 0;

        int result = 1;
        for (int i = 0; i < length; i++)
            result = 31 * result + a[i + offset];

        return result;
    }


    public static boolean equals(NVEntity nve1, NVEntity nve2) {
        if (nve1 != null && nve2 != null
                && nve1.getNVConfig().getMetaType() != null
                && nve2.getNVConfig().getMetaType() != null
                && nve1.getNVConfig().getMetaType().equals(nve2.getNVConfig().getMetaType())
                && nve1.getReferenceID() != null
                && nve2.getReferenceID() != null
                && nve1.getReferenceID().equals(nve2.getReferenceID())
        ) {
            return true;
        }

        return false;
    }


    public static <V> boolean genContains(V match, V... vals) {
        if (match != null) {
            for (V toFind : vals) {
                if (toFind != null) {
                    if (match == toFind)
                        return true;

                    if (toFind instanceof String && match instanceof String) {
                        if (((String) toFind).equalsIgnoreCase((String) match))
                            return true;
                    }
                }
            }
        }

        return false;
    }


    public static NVStringList toNVStringList(String name, String[] values, boolean skipEmptyOrNull) {
        NVStringList ret = new NVStringList(name);
        if (values != null) {
            for (String val : values) {
                if (skipEmptyOrNull) {
                    val = trimOrNull(val);
                    if (val == null)
                        continue;
                }
                ret.getValue().add(val);
            }
        }


        return ret;


    }


    public static int toUnsignedInt(byte b) {
        int ret = b;
        if (ret < 0)
            ret += 256;
        return ret;
    }


    // =====================================================================================
    // Section 3: string / token / hex helpers (formerly SharedStringUtil)
    // =====================================================================================

    /**
     * Result of a token search: the token, the index of the first match, the number of
     * matches and the index of the last match (referenceIndex).
     */
    public static class MatchToken {
        MatchToken(CharSequence t) {
            token = t;
            index = -1;
            count = 0;
            referenceIndex = -1;
        }

        CharSequence token;
        int index;
        int count;
        int referenceIndex;

        public String toString() {
            return toCanonicalID(':', token, referenceIndex, index, count);
        }

        public int getIndex() {
            return index;
        }

        public int getCount() {
            return count;
        }
    }

    public static boolean isIncrementOk(String[] args, int index) {
        return isIncrementOk(args, index, 1);
    }

    public static boolean isIncrementOk(String[] args, int index, int increment) {
        return index + increment <= args.length;
    }

    /**
     * Finds and returns the next token match within the string sequence.
     *
     * @param token
     * @param fromIndex
     * @param tokens
     * @return
     */
    private static MatchToken nextMatch(CharSequence token, int fromIndex, MatchToken... tokens) {
        MatchToken ret = null;

        for (int i = fromIndex; i < token.length(); i++) {
            for (MatchToken mt : tokens) {
                boolean match = false;

                int j = 0;

                for (; j < mt.token.length() && (i + j) < token.length(); j++) {
                    if (token.charAt(i + j) != mt.token.charAt(j)) {
                        break;
                    }
                }

                if (j == mt.token.length()) {
                    match = true;
                }

                if (match) {
                    mt.index = i;
                    mt.count++;
                    ret = mt;
                    break;
                }
            }

            if (ret != null) {
                break;
            }
        }

        return ret;
    }


    public static String getTokenByIndex(String path, String sep, int index, boolean ignoreCase) {
        String params[] = parseStringLenient(path, sep);
        if (index < params.length) {
            return ignoreCase ? params[index].toLowerCase() : params[index];
        }

        return null;
    }

    public static int indexOf(String str, String strLookingFor, int startIndex, boolean ignoreCase) {
        if (ignoreCase) {
            str = toLowerCase(str);
            strLookingFor = toLowerCase(strLookingFor);
        }

        return str.indexOf(strLookingFor, startIndex);
    }

    public static boolean contains(String str, String lookingFor) {
        return contains(str, lookingFor, true);
    }

    public static boolean contains(String str, String lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null) {
            return indexOf(str, lookingFor, 0, ignoreCase) > -1;
        }

        return false;
    }


    public static boolean contains(String str, GetName lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null && lookingFor.getName() != null) {
            return indexOf(str, lookingFor.getName(), 0, ignoreCase) > -1;
        }

        return false;
    }

    public static boolean contains(String str, GetValue<String> lookingFor, boolean ignoreCase) {
        if (str != null && lookingFor != null && lookingFor.getValue() != null) {
            return indexOf(str, lookingFor.getValue(), 0, ignoreCase) > -1;
        }

        return false;
    }

    public static boolean equals(String str1, String str2, boolean ignoreCase) {
        if (str1 == str2) {
            return true;
        }

        if (str1 != null && str2 != null) {
            if (ignoreCase) {
                str1 = toLowerCase(str1);
                str2 = toLowerCase(str2);
            }

            return str1.equals(str2);
        }

        return false;
    }

    public static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // Remove the surrounding quotes
            value = value.substring(1, value.length() - 1);
            // Unescape any escaped quotes
            //value = value.replace("\\\"", "\"");
        }
        return value;
    }

    public static String removeCharFromEnd(char toRemove, String str) {
        int indexToRemove = -1;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != toRemove)
                break;
            indexToRemove = i;
        }
        if (indexToRemove != -1)
            return str.substring(0, indexToRemove);
        return str;
    }

    /**
     * Reverses the given token(s).
     *
     * @param reverse
     * @param tokens
     * @return
     */
    private static MatchToken[] reverseTokens(boolean reverse, MatchToken... tokens) {
        if (reverse) {
            if (tokens.length > 1) {
                MatchToken[] ret = new MatchToken[tokens.length];

                for (int i = 0; i < ret.length; i++) {
                    ret[i] = tokens[tokens.length - 1 - i];
                }

                return ret;
            }
        }

        return tokens;
    }

    /**
     * Parses the token and return matching region between startDelimiter and endDelimiter.
     * If the token contains sub groups the outer group will return.
     * Ex: token = {{hello}}there{stranger},  if sd={ and ed=} the result is 2 matches {{hello}},{stranger}
     *
     * @param token
     * @param sd    startDelimiter
     * @param ed    endDelimiter
     * @return List of CharSequence
     * @throws NullPointerException if any of the parameters is null
     */
    public static List<CharSequence> parseGroup(CharSequence token, CharSequence sd, CharSequence ed, boolean includeDelimiters)
            throws NullPointerException {

        List<CharSequence> ret = new ArrayList<CharSequence>();
        MatchToken startToken = new MatchToken(sd);
        MatchToken endToken = new MatchToken(ed);
        MatchToken currentMatch = nextMatch(token, 0, startToken, endToken);
        if (currentMatch == null)
            return ret;
        currentMatch.referenceIndex = currentMatch.index;
        boolean sameToken = sd.equals(ed);

        MatchToken tokenList[] = {
                startToken,
                endToken
        };
        do {
            tokenList = reverseTokens(sameToken, tokenList);
            currentMatch = nextMatch(token, currentMatch.index + currentMatch.token.length(), tokenList);

            if (currentMatch != null && startToken.count == endToken.count) {
                endToken.referenceIndex = endToken.index + endToken.token.length();

                if (includeDelimiters) {
                    ret.add(token.subSequence(startToken.referenceIndex, endToken.referenceIndex));
                } else {
                    ret.add(token.subSequence(startToken.referenceIndex + sd.length(), endToken.referenceIndex - ed.length()));
                }

                tokenList = reverseTokens(sameToken, tokenList);
                currentMatch = nextMatch(token, currentMatch.index + currentMatch.token.length(), tokenList);

                if (currentMatch != null) {
                    currentMatch.referenceIndex = currentMatch.index;
                }
            }

        } while (currentMatch != null && (endToken.referenceIndex != token.length()));

        return ret;
    }

    /**
     * Returns a substring to the right of the specified index if both val and token are not null,
     * otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring to the right of the specified index if both val and token are not null,
     */
    public static String valueAfterRightToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.lastIndexOf(token);

            if (lastIndex != -1) {
                return val.substring(lastIndex + token.length());
            }
        }

        return val;
    }

    /**
     * Returns a substring starting at the beginning of the string and ending to the left of the specified index
     * if both val and token are not null, otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring starting at the beginning of the string and ending to the left of the specified index
     */
    public static String valueBeforeRightToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.lastIndexOf(token);

            if (lastIndex != -1) {
                return val.substring(0, lastIndex);
            }
        }

        return val;
    }

    public static String[] parseToken(String token, int matchCount, boolean ignoreCase, String... toMatches) {
        List<String> matches = new ArrayList<>();
        boolean moreToMatch;
        int lastIndex = 0;
        String tokenToProcess = ignoreCase ? token.toUpperCase() : token;
        boolean found;
        do {
            found = false;
            moreToMatch = false;
            for (String toMatch : toMatches) {

                int matchIndex = tokenToProcess.indexOf(ignoreCase ? toMatch.toUpperCase() : toMatch, lastIndex);
                if (matchIndex != -1) {
                    // we have a match
                    String toAdd = token.substring(lastIndex, matchIndex);
                    if (isNotEmpty(toAdd))
                        matches.add(toAdd);
                    found = true;
                    lastIndex = matchIndex + toMatch.length();
                    // break loop
                    break;
                }
            }
            if (matchCount > 0 && matches.size() < matchCount) {
                moreToMatch = true;
            } else if (matchCount < 1 && found) {
                moreToMatch = true;
            }

        } while (moreToMatch && found);

        String toAdd = token.substring(lastIndex);
        if (isNotEmpty(toAdd))
            matches.add(toAdd);


        return matches.toArray(new String[0]);
    }

    /**
     * Extracts the value between the prefix and postfix string.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @param ignoreCase
     * @return the value between the prefix and postfix string.
     */
    public static StringToken valueBetween(String str, String prefix, String postfix, boolean ignoreCase) {
        if (str != null) {
            String val = str;

            if (ignoreCase) {
                val = str.toLowerCase();
                prefix = prefix.toLowerCase();
                postfix = postfix.toLowerCase();
            }

            int preIndex = val.indexOf(prefix);

            if (preIndex == -1) {
                return null;
            }

            int postIndex = val.indexOf(postfix, preIndex + prefix.length());

            if (postIndex == -1) {
                return null;
            }

            return new StringToken(str.substring(preIndex + prefix.length(), postIndex), preIndex + prefix.length(), postIndex);

        }

        return null;
    }

    /**
     * Returns a substring to the right of the specified index if both val and token are not null,
     * otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring to the right of the specified index if both val and token are not null,
     */
    public static String valueAfterLeftToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.indexOf(token);

            if (lastIndex != -1) {
                return val.substring(lastIndex + token.length());
            }
        }

        return val;
    }

    /**
     * Returns a substring starting at the beginning of the string and ending to the left of the specified index
     * if both val and token are not null, otherwise will return empty string.
     *
     * @param val
     * @param token
     * @return a substring starting at the beginning of the string and ending to the left of the specified index
     */
    public static String valueBeforeLeftToken(String val, String token) {
        if (val != null && token != null) {
            int lastIndex = val.indexOf(token);

            if (lastIndex != -1) {
                return val.substring(0, lastIndex);
            }
        }

        return val;
    }

    /**
     * Checks if str is not null then return uppercase version, otherwise return null.
     *
     * @param str
     * @return if str is not null then return uppercase version, otherwise return null.
     */
    public static String toUpperCase(String str) {
        if (str != null) {
            return str.toUpperCase();
        }

        return null;
    }


    /**
     * Covert a string to bytes array using UTF-8 format
     *
     * @param str to be converted
     * @return byte array
     */
    public static byte[] toBytes(String str) {
        return getBytes(str);
    }

    /**
     * Return the byte array by converting the string to byte using UTF-8.
     *
     * @param str
     * @return the byte array by converting the string to byte using UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static byte[] getBytes(String str)
            throws NullPointerException, IllegalArgumentException {
        checkIfNulls("Null String", str);

        try {
            return str.getBytes(Const.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Returns the bytes array by converting the strings to bytes using UTF-8.
     *
     * @param strs
     * @return the bytes array by converting the strings to bytes using UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static byte[][] getBytesArray(String... strs)
            throws NullPointerException, IllegalArgumentException {

        checkIfNulls("Null String", (Object[]) strs);

        byte[][] ret = new byte[strs.length][];

        for (int i = 0; i < strs.length; i++) {
            ret[i] = getBytes(strs[i]);
        }

        return ret;
    }

    /**
     * Return the String based on the byte array  using encoding UTF-8.
     *
     * @param array
     * @return the String based on the byte array  using encoding UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static String toString(byte[] array)
            throws NullPointerException, IllegalArgumentException {
        return toString(array, 0, array.length);
    }

    public static String toString(char[] array) {
        return new String(array);
    }


    /**
     * Return the String based on the byte array  using encoding UTF-8.
     *
     * @param array
     * @param offset
     * @param length
     * @return the String based on the byte array  using encoding UTF-8.
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static String toString(byte[] array, int offset, int length)
            throws NullPointerException, IllegalArgumentException {

        checkIfNull("Null String", array);

        try {
            return new String(array, offset, length, Const.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static String toString(byte b) {
        int ret = b;
        if (ret < 0)
            ret += 256;
        return Integer.toString(ret);
    }

    /**
     * Checks if str is not null then return lowercase version, otherwise return null.
     *
     * @param str
     * @return if str is not null then return lowercase version, otherwise return null.
     */
    public static String toLowerCase(String str) {
        if (str != null) {
            return str.toLowerCase();
        }

        return null;
    }

    /**
     * Trims the str and checks if its length = > minLength.
     *
     * @param str
     * @param minLength
     * @return true if str match the min length criteria
     */
    public static boolean isMinimumLengthMet(String str, int minLength) {
        str = trimOrNull(str);

        if (str == null) {
            if (minLength == 0) {
                return true;
            } else {
                return false;
            }
        }

        return (str.length() >= minLength);
    }

    /**
     * Filters the given string based on given filters and returns the filtered string.
     *
     * @param str
     * @param filters
     * @return the given string based on given filters and returns the filtered string.
     */
    public static String filterString(String str, CharSequence... filters) {
        if (str != null) {
            for (CharSequence cs : filters) {
                str = str.replace(cs, "");
            }
        }

        return str;
    }

    /**
     * Parses specified string based on regex and filter.
     *
     * @param str
     * @param regex
     * @param filters
     * @return parsed strings
     */
    public static String[] parseString(String str, String regex, CharSequence... filters) {
        return parseString(str, regex, false, filters);
    }

    /**
     * Parses specified string based on regex and filter.
     *
     * @param str     to parse
     * @param regex   reg expression
     * @param noEmpty if true empty string are filtered
     * @param filters filter to the str
     * @return parsed strings
     */
    public static String[] parseString(String str, String regex, boolean noEmpty, CharSequence... filters) {
        if (filters != null) {
            str = filterString(str, filters);
        }
        String[] results = str.split(regex);
        if (!noEmpty)
            return results;

        List<String> ret = new ArrayList<String>();
        for (String tmp : results) {
            if (!isEmpty(tmp))
                ret.add(tmp);
        }
        return ret.toArray(new String[0]);
    }

    public static String[] parseStringLenient(String str, String regex, CharSequence... filters) {
        if (str != null)
            return parseString(str, regex, filters);

        return Const.EMPTY_STRING_ARRAY;
    }


    public static List<GetNameValue<String>> parseStrings(char sep, String... tokens) {
        List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();
        String stringSep = "" + sep;
        for (String token : tokens) {
            String[] toks = token.split(stringSep);
            if (toks.length == 2) {
                ret.add(new NVPair(toks[0], toks[1]));
            }
        }
        return ret;
    }


    /**
     * Returns a string array based on the strings str and sep.
     *
     * @param str
     * @param sep
     * @return {name, value}
     */
    public static String[] parseNameValue(String str, String sep) {
        int index = str.indexOf(sep);

        if (index != -1) {
            return new String[]{str.substring(0, index), str.substring(index + sep.length(), str.length())};
        }

        return new String[]{str};
    }

    public static boolean isComment(String line) {
        return isComment(line, Const.COMMENT_TAGS);
    }


    public static boolean isComment(String line, String... startTokenMarkers) {

        line = trimOrNull(line);
        if (line != null) {
            for (String token : startTokenMarkers) {
                if (line.startsWith(token))
                    return true;
            }
        }
        return false;
    }

    public static int indexOf(String str, String... tags) {
        int firstIndex = -1;

        for (String tag : tags) {
            int index = str.indexOf(tag);
            if (index != -1) {
                if (firstIndex == -1) {
                    firstIndex = index;
                } else if (index < firstIndex) {
                    firstIndex = index;
                }
            }
        }

        return firstIndex;
    }


    public static String concat(String sep, int length, String... tokens) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(sep);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    /**
     * Concatenates s1 + sep + s2 = total, sep will not be added if s1 ends with sep or s2 starts with sep.
     *
     * @param s1
     * @param s2
     * @param sep
     * @return s1 + sep + s2
     */
    public static String concat(String s1, String s2, String sep) {
        s1 = trimOrEmpty(s1);
        s2 = trimOrEmpty(s2);
        sep = trimOrEmpty(sep);

        if ((s1.endsWith(sep) && !s2.startsWith(sep)) || (!s1.endsWith(sep) && s2.startsWith(sep))) {
            return s1 + s2;
        } else if (s1.endsWith(sep) && s2.startsWith(sep)) {
            return s1 + s2.substring(sep.length());
        }

        if (s2.isEmpty()) {
            return s1;
        }

        return s1 + sep + s2;
    }

    /**
     * Format a list of NPairs.
     *
     * @param list the list of name-value pairs
     * @param <V> the value type
     * @return formatted string like n1=v1&amp;n2=v2...
     */
    public static <V> String format(ArrayValues<GetNameValue<V>> list) {
        return format(list, "=", false, "&");
    }

    /**
     * This method will convert an ArrayList of NVPair into a single string
     *
     * @param list         all the NVPair
     * @param nameValueSep the name value separator
     * @param quotedValue  if the value should be quoted
     * @param nvPairSep    the separator between the nvpairs
     * @return the formated string
     */
    public static <V> String format(ArrayValues<GetNameValue<V>> list, String nameValueSep, boolean quotedValue, String nvPairSep) {
        StringBuilder sb = new StringBuilder();
        GetNameValue<V>[] all = list.values();

        for (int i = 0; i < all.length; i++) {
            GetNameValue<V> nvp = all[i];
            sb.append(format(nvp, nameValueSep, quotedValue));

            if (i + 1 < all.length) {
                sb.append(nvPairSep);
            }
        }

        return sb.toString();
    }

    /**
     * @param pair
     * @param nameValueSep
     * @param quotedValue
     * @return formatted string
     */
    public static <V> String format(GetNameValue<V> pair, String nameValueSep, boolean quotedValue) {
        return format(pair.getName(), pair.getValue(), nameValueSep, quotedValue);
    }


    public static String formatStringValues(String sep, String... values) {
        checkIfNulls("Null Parameter", sep, values);
        StringBuilder ret = new StringBuilder();

        for (String value : values) {
            value = trimOrNull(value);

            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }


    /**
     * This method add chars between the text characters
     *
     * @param text
     * @param chars
     * @return
     */
    public static String spaceChars(String text, String chars) {
        StringBuilder ret = new StringBuilder();
        char[] stringChars = text.toCharArray();
        for (int i = 0; i < stringChars.length; i++) {
            ret.append(stringChars[i]);
            if (i + 1 < stringChars.length)
                ret.append(chars);
        }
        return ret.toString();
    }


    public static String repeatSequence(String sc, int count) {
        return repeatSequence(sc, count, null);
    }

    public static String repeatSequence(String sc, int count, String sep) {
        if (count < 1) {
            return sc;
        }
        if (sep != null && sep.length() == 0)
            sep = null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(sc);
            if (sep != null && i + 1 < count)
                sb.append(sep);
        }
        return sb.toString();
    }

    public static String formatStringToByteArray(String str, boolean hex) {
        byte data[] = getBytes(str);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (byte b : data) {
            if (sb.length() > 1)
                sb.append(',');
            if (hex)
                byteToHex(sb, "0x", b);
            else
                sb.append(b);
        }
        sb.append("}");

        return sb.toString();
    }

    public static String formatStringValues(String sep, GetName... gns) {
        checkIfNulls("Null Parameter", sep, gns);
        StringBuilder ret = new StringBuilder();

        for (GetName gn : gns) {
            String value = trimOrNull(gn.getName());
            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }

    public static String formatStringValues(String sep, GetValue<?>... values) {
        checkIfNulls("Null Parameter", sep, values);
        StringBuilder ret = new StringBuilder();

        for (GetValue<?> gnv : values) {
            String value = trimOrNull(gnv.getValue() != null ? "" + gnv.getValue() : null);

            if (value != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }

                ret.append(value);
            }
        }

        return ret.toString();
    }

    public static <V> String format(String name, V value) {
        return format(name, value, "=", false);
    }

    public static String tag(String str) {
        return tag(Const.TAG_ENVELOPE, str, Const.TAG_ENVELOPE);
    }

    public static String tag(String leftRightTag, String str) {
        return tag(leftRightTag, str, leftRightTag);
    }

    public static String tag(String leftTag, String str, String rightTag) {
        return (leftTag != null ? leftTag : "") + str + (rightTag != null ? rightTag : "");
    }

    /**
     * Converts a hex string into a byte array
     *
     * @param str
     * @return a hex string into a byte array
     * @throws IllegalArgumentException
     * @throws NullPointerException
     */
    public static byte[] hexToBytes(String str)
            throws IllegalArgumentException, NullPointerException {
        str = str.toUpperCase().trim();
        str = str.replaceAll("\\s", "");

        if (str.startsWith("0X")) {
            str = valueAfterLeftToken(str, "0X");
        }

        int len = str.length();

        if (len % 2 != 0) {
            throw new IllegalArgumentException("Not a valid hex format " + str);
        }

        byte[] byteRet = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            // take the first byte shift to the left a nible
            // then or it with right char value
            byteRet[i / 2] = (byte) ((hexToInt(str.charAt(i)) << 4) | hexToInt(str.charAt(i + 1)));
        }

        return byteRet;

    }

    /**
     * Convert a hex char 0-F to a integer value 0-15.
     *
     * @param c to be converted
     * @return value 0-15
     * @throws IllegalArgumentException if c is different than 0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f
     */
    public static int hexToInt(char c)
            throws IllegalArgumentException {

//        c = Character.toUpperCase(c);
//        for (int j = 0; j < Const.HEX_TOKENS.length; j++) {
//            if (c == Const.HEX_TOKENS[j]) {
//                return j;
//            }
//        }

        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;


        throw new IllegalArgumentException("Invalid character not a hex type 0-F:" + c);
    }


    public static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i))) return false;
        return !s.isEmpty();
    }

    /**
     * Replaces the textMarker inside text with value.
     * ex: text="Hello my name is $TEXTMARKER$", textMarker="$TEXTMARKER$", value="Earl", result="Hello my name is Earl".
     *
     * @param text
     * @param textMarker
     * @param value
     * @return text embedded with value
     */
    public static String embedText(String text, String textMarker, String value) {

        checkIfNulls("Invalid text", text);

        if (textMarker != null) {
            if (value == null) {
                value = "";
            }

            String textMarkerUpper = textMarker.toUpperCase();
            text = text.replace(textMarker, textMarkerUpper);

            return text.replace(textMarkerUpper, value);
        }

        return null;
    }

    public static byte[] embedTextAsBytes(String text, String textMarker, String value) {
        String embedded = embedText(text, textMarker, value);
        return getBytes(embedded);
    }

    /**
     * Replaces the textMarker inside text with value.
     * ex: text="Hello my name is $TEXTMARKER$", textMarker="$TEXTMARKER$", value="Earl", result="Hello my name is Earl".
     *
     * @param text
     * @param textMarker
     * @param value
     * @return text embedded with value
     */
    @SuppressWarnings("unchecked")
    public static String embedText(String text, Enum<?> textMarker, String value) {
        checkIfNulls("Invalid text", text);

        if (textMarker != null) {
            if (value == null) {
                value = "";
            }

            String textMarkerToken = null;

            if (textMarker instanceof GetValue) {
                textMarkerToken = ((GetValue<String>) textMarker).getValue();
            }

            if (textMarkerToken == null && textMarker instanceof GetName) {
                textMarkerToken = ((GetName) textMarker).getName();
            }

            if (textMarkerToken == null) {
                textMarkerToken = textMarker.name();
            }

            String textMarkerUpper = textMarkerToken.toUpperCase();
            text = text.replace(textMarkerToken, textMarkerUpper);

            return text.replace(textMarkerUpper, value);
        }

        return null;
    }

    /**
     * Returns the string after the last occurrence of delimiter if no match is found return token as is.
     *
     * @param token
     * @param delimiter
     * @return the string after the last occurrence of delimiter if no match is found return token as is.
     */
    public static String getTokenAfterDelimiter(String token, char delimiter) {
        int index = token.lastIndexOf(delimiter);

        if (index == -1) {
            return token;
        }

        return token.substring(index + 1);
    }

    /**
     * Checks if token occurs at least once in text.
     *
     * @param text
     * @param token
     * @return true if token occurs at least once in text.
     */
    public static boolean hasToken(String text, String token) {
        return !(text.indexOf(token) == -1);
    }

    /**
     * @param preToken
     * @param buffer
     * @param offset
     * @param len
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer, int offset, int len, String postToken) {
        StringBuilder sb = null;

        if (buffer != null) {
            //sb = new StringBuilder(buffer.length);
            for (int i = offset; i < offset + len; i++) {
                if (postToken != null && sb != null) {
                    sb.append(postToken);
                }
                if (sb == null) {
                    sb = new StringBuilder();
//                    // try to predict the size
//                    int size = buffer.length * (2);
//                    size += preToken != null && preToken.length() > 0 ? (preToken.length() * buffer.length) : 0;
//                    size += postToken != null && postToken.length() > 0 && (buffer.length - 1) > 0 ? postToken.length() * (buffer.length - 1) : 0;
//                    sb = new StringBuilder(size);
                }
                sb = byteToHex(sb, preToken, buffer[i]);
            }
        }

        return sb != null ? sb.toString() : null;
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param buffer
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer, String postToken) {
        return bytesToHex(null, buffer, 0, buffer.length, postToken);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param preToken
     * @param buffer
     * @param postToken
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer, String postToken) {
        return bytesToHex(preToken, buffer, 0, buffer.length, postToken);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation
     *
     * @param preToken
     * @param buffer
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(String preToken, byte[] buffer) {
        return bytesToHex(preToken, buffer, 0, buffer.length, null);
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation, based on the offset and len.
     *
     * @param buffer
     * @param offset
     * @param len
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer, int offset, int len) {
        return bytesToHex(null, buffer, offset, len, null);
    }

    /**
     * Converts a byte to hex StringBuilder.
     *
     * @param sb       if null this method will create a new string builder , if not null will use the one being passed
     * @param preToken to be added before the converted byte if null it will be skipped
     * @param b        byte to convert
     * @return stringbuilder of hex that represent the buffer content
     */
    public static StringBuilder byteToHex(StringBuilder sb, String preToken, byte b) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (preToken != null) {
            sb.append(preToken);
        }

        sb.append(Const.HEX_TOKENS[(b >> 4) & (0X0F)]);
        sb.append(Const.HEX_TOKENS[b & (0X0F)]);

        return sb;
    }

    /**
     * Converts a byte buffer into a hexadecimal string representation.
     *
     * @param buffer
     * @return string of hex that represent the buffer content
     */
    public static String bytesToHex(byte[] buffer) {
        return bytesToHex(null, buffer, 0, buffer.length, null);
    }

    /**
     * Converts a string into a hexadecimal string representation.
     *
     * @param preToken
     * @param str
     * @return string of hex that represent the buffer content
     */
    public static String stringToHex(String preToken, String str) {
        return bytesToHex(preToken, getBytes(str));
    }

    /**
     * Truncates a string based on the specified length.
     *
     * @param str
     * @param length
     * @return Truncates a string based on the specified length.
     */
    public static String truncate(String str, int length) {
        if (str == null) {
            return null;
        }

        if (str.length() > length) {
            return str.substring(0, length);
        }

        return str;
    }

    /**
     * Compresses the given string given the maximum length, separator, and separator length.
     *
     * @param sep
     * @param str
     * @param maxLength
     * @param sepLength
     * @return compresses the given string given the maximum length, separator, and separator length.
     */
    public static String toShortHand(char sep, String str, int maxLength, int sepLength) {
        if (str != null) {
            if (str.length() < maxLength) {
                return str;
            } else {
                int midpoint = maxLength / 2 - sepLength / 2;
                StringBuilder sb = new StringBuilder();
                sb.append(str.substring(0, midpoint));

                for (int i = 0; i < sepLength; i++) {
                    sb.append(sep);
                }

                sb.append(str.substring(str.length() - midpoint, str.length()));

                return sb.toString();
            }
        }

        return null;
    }

    public static MatchToken matchToken(String str, String token, boolean ignoreCase) {
        MatchToken ret = null;

        int firstIndex = indexOf(str, token, 0, ignoreCase);
        int counter = 0;

        if (firstIndex != -1) {
            ret = new MatchToken(token);
            ret.index = firstIndex;
            ret.referenceIndex = firstIndex;

            int currentIndex = firstIndex;

            while (currentIndex != -1) {
                counter++;
                ret.referenceIndex = currentIndex;
                currentIndex = indexOf(str, token, currentIndex + token.length(), ignoreCase);
            }

            ret.count = counter;
        }

        return ret;
    }

}
