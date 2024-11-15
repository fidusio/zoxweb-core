package org.zoxweb.shared.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Sus(Shared Util Shortcut) basically to minimise typing
 */
public class SUS
{
    private SUS(){}

    private static final Map<Object,Object> cache = new HashMap<>();


    /**
     * 
     * @param collection to be checked
     * @return true col != null and col not empty
     */
    public static boolean isNotEmpty(Collection<?> collection)
    {
        return collection != null && !collection.isEmpty();
    }



    /**
     * 
     * @param str to checked
     * @return true str !=null and str.trim() not empty
     */
    public static boolean isNotEmpty(String str)
    {
        return str != null && !str.trim().isEmpty();
    }


    /**
     * Check if an array is empty not empty meaning not null and length > 0
     * @param array to be checked
     * @return true if the array exists and not empty
     */
    public static boolean isNotEmpty(Object[] array)
    {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(byte[] array)
    {
        return (array != null && array.length != 0);
    }

    /**
     * Convert a NVEntity to NVGenericMap
     * @param nve to be converted
     * @return the nve as NVGenericMao
     */
    public static NVGenericMap toNVGenericMap(NVEntity nve)
    {
        NVGenericMap ret = new NVGenericMap(nve.getName());
        NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
        for(NVConfig nvc : nvce.getAttributes())
        {
            ret.add(nve.lookup(nvc));
        }
        return ret;
    }

    /**
     * Return the enum name if it implements GetName if not enum.name()
     * @param en to be checked
     * @return the name
     */
    public static String enumName(Enum<?> en)
    {
        checkIfNulls("enum can't be null", en);
        if (en instanceof GetName)
            return ((GetName) en).getName();
        return en.name();
    }
    public static String[] enumNames(Enum<?> ...enums)
    {
        String[] ret = new String[enums.length];
        for(int i = 0; i < enums.length; i++)
            ret[i] = enumName(enums[i]);
        return ret;
    }

    /**
     * Checks all the objs if any of them is null it will throw a NullPointerException.
     * @param msg NullPointerException message
     * @param objs to be checked
     * @throws NullPointerException if any obj is null
     */
    public static void checkIfNulls(String msg, Object... objs)
            throws NullPointerException
    {
        if (objs == null)
            // error in invoking the check
            throw new NullPointerException("Null Array Object");

        for (Object o : objs)
            if (o == null)
                throw new NullPointerException(msg);
    }

    public static NVGenericMap updateGetNVProperties(GetNVProperties toUpdate, NVGenericMap value)
    {
        return updateNVGenericMap(toUpdate.getProperties(), value);
    }

    @SuppressWarnings("unchecked")
    public static NVGenericMap updateNVGenericMap(NVGenericMap toUpdate, NVGenericMap value)
    {
        for(GetNameValue<?> gnv : value.values())
        {
            GetNameValue<?> gnvToUpdate = toUpdate.get(gnv);
            if (gnvToUpdate != null)
            {
                ((NVBase<Object>)gnvToUpdate).setValue(gnv.getValue());
            }
            else
            {
                toUpdate.add(gnv);
            }
        }
        
        return toUpdate;
    }

    public static <V> GetNameValue<V> buildNV(String name, V value)
    {
        return new NamedValue<>(name, value);
    }

    /**
     * Returns true if str is null or str.trim().length() == 0.
     * @param str to be checked
     * @return true if str is empty
     */
    public static boolean isEmpty(String str)
    {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isPrimitiveGNV(GetNameValue<?> nvb)
    {
        checkIfNulls("NameValue null", nvb);
        if (cache.get(nvb.getClass()) != null ||
                (nvb.getValue() != null && cache.get(nvb.getValue().getClass()) !=null) )
            return true;

        if (nvb instanceof NVPair ||
                nvb instanceof NVInt ||
                nvb instanceof NVLong ||
                nvb instanceof NVFloat ||
                nvb instanceof NVDouble)
        {
            synchronized (cache)
            {
                cache.put(nvb.getClass(), nvb.getClass());
                return true;
            }
        }

        if (nvb.getValue() != null)
        {
            Object value = nvb.getValue();
            if (value instanceof String ||
                    value instanceof Integer ||
                    value instanceof Long ||
                    value instanceof Float ||
                    value instanceof Double ||
                    value instanceof Short)
            {
                synchronized (cache)
                {
                    cache.put(value.getClass(), value.getClass());
                    return true;
                }
            }
        }

        return false;

    }

    private static StringBuilder _toCanonicalID(int pos, StringBuilder sb, boolean ignoreNull, char sep, Object val)
    {
        if ( val == null && ignoreNull)
        {
            return sb;
        }

        if (ignoreNull && sb.length() != 0)
        {
            sb.append(sep);
        }
        else if (pos != 0)
        {
            sb.append(sep);
        }

        if (val != null)
        {
            sb.append(val);
        }

        return sb;
    }

    public static String toCanonicalID(char sep, Object ...datas)
    {
        return toCanonicalID(false, sep, datas);
    }

    public static String toCanonicalID(boolean ignoreNulls, char sep,  Object... objArray)
    {
        StringBuilder sb = new StringBuilder();

        if (objArray != null)
        {

            for (int i = 0; i < objArray.length; i++)
            {
                _toCanonicalID(i, sb, ignoreNulls, sep, objArray[i]);
            }
        }

        return sb.toString();
    }

    public static String toCanonicalID(boolean ignoreNulls, char sep, Enumeration<?> enumer)
    {
        StringBuilder sb = new StringBuilder();

        if (enumer != null)
        {
            int pos = 0;
            while (enumer.hasMoreElements())
            {
                _toCanonicalID(pos++, sb, ignoreNulls, sep, enumer.nextElement());
            }
        }

        return sb.toString();
    }

    public static String trimOrNull(String str)
    {
        str = (str != null ? str.trim() : null);
        return str != null ? (!str.isEmpty() ? str : null) : null;
    }

    public static String trimOrEmpty(String str)
    {
        return str != null ? str.trim() : "";
    }
}
