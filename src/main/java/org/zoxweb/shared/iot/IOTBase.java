package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVGenericMap;

public abstract class IOTBase
extends PropertyDAO
{
    protected IOTBase(NVConfigEntity nvce)
    {
        super(nvce);
    }

    protected NVGenericMap lookupSubNVGM(GetName gn)
    {
        return lookupSubNVGM(gn.getName());
    }
    protected synchronized NVGenericMap lookupSubNVGM(String name)
    {
        NVGenericMap ret = getProperties().lookup(name);
        if (ret == null)
        {
            ret = new NVGenericMap(name);
            getProperties().build(ret);
        }

        return ret;
    }
}
