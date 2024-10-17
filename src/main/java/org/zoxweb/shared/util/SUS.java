package org.zoxweb.shared.util;

import java.util.Collection;

/**
 * Sus(Shared Util Shortcut) basically to minimise typing
 */
public class SUS
{
    private SUS(){}

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
     * Check if an array is empty meaning null or length = 0
     * @param array to be checked
     * @return true if the array exists and not empty
     */
    public static boolean isEmpty(Object[] array)
    {
        return (array != null && array.length == 0);
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
        SharedUtil.checkIfNulls("enum can't be null", en);
        if (en instanceof GetName)
            return ((GetName) en).getName();
        return en.name();
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
}
