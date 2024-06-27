package org.zoxweb.shared.util;

public class CanonicalIDSetter
{

    private final String[] attrNames;
    private final char sep;


    public CanonicalIDSetter(char sep, GetNVConfig...attrGetNVConfig)
    {
        this.sep = sep;
        this.attrNames = new String[attrGetNVConfig.length];
        for (int i = 0; i < attrGetNVConfig.length; i++)
        {
            this.attrNames[i] = attrGetNVConfig[i].getNVConfig().getName();
        }
    }


    public CanonicalIDSetter(char sep, NVConfig...attrNVConfigs)
    {
        this.sep = sep;
        this.attrNames = new String[attrNVConfigs.length];
        for (int i = 0; i < attrNVConfigs.length; i++)
        {
            this.attrNames[i] = attrNVConfigs[i].getName();
        }
    }


    public CanonicalIDSetter(char sep, GetName...attrGetNames)
    {
        this.sep = sep;
        this.attrNames = new String[attrGetNames.length];
        for (int i = 0; i < attrGetNames.length; i++)
        {
            this.attrNames[i] = attrGetNames[i].getName();
        }
    }

    public CanonicalIDSetter(char sep, String ...attrNames)
    {
        this.sep = sep;
        this.attrNames = attrNames;
    }


//    public <V> void setValue(NVEntity nve, GetNVConfig getNVConfig, V value)
//    {
//        setValue(nve, getNVConfig.getNVConfig().getName(), value);
//    }
//
//    public <V> void setValue(NVEntity nve, NVConfig nvc, V value)
//    {
//        setValue(nve, nvc.getName(), value);
//    }
//
//    public <V> void setValue(NVEntity nve, GetName getName, V value)
//    {
//        setValue(nve, getName.getName(), value);
//    }
//    public <V> void setValue(NVEntity nve, String attrName, V value)
//    {
//        synchronized (nve)
//        {
//            nve.setValue(attrName, value);
//            setCanonicalID(nve, attrName);
//        }
//    }


    private boolean matchFound(String attrName)
    {

        for(String name : attrNames)
        {
            if(attrName.equals(name))
            {
                return true;
            }
        }
        return false;
    }


    public void setCanonicalID(NVEntity nve, String attrName)
    {
        if(nve instanceof SetCanonicalID && matchFound(attrName))
        {
            Object[] values = new Object[attrName.length()];
            for(int i = 0; i < attrNames.length; i++)
            {
                values[i] = nve.lookupValue(attrNames[i]);
            }
            ((SetCanonicalID) nve).setCanonicalID(SharedUtil.toCanonicalID(true, sep, values));
        }
    }
}
