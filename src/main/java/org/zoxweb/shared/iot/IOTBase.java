package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.NVConfigEntity;

public abstract class IOTBase
extends PropertyDAO
{
    protected IOTBase(NVConfigEntity nvce)
    {
        super(nvce);
    }
}
