package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

public abstract class IOTBase
extends PropertyDAO
{

    public static final NVConfig ALIAS = NVConfigManager.createNVConfig("alias", "User defined alias", "Alias", false, true, String.class);

    public static final NVConfigEntity NVC_IOT_BASE = new NVConfigEntityPortable("iot_base",
            null,
            "IOTBase",
            true,
            false,
            false,
            false,
            IOTBase.class,
            SharedUtil.toNVConfigList(DataConst.DataParam.UNIQUE_CANONICAL_ID.getNVConfig(), ALIAS),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO);




    protected IOTBase(NVConfigEntity nvce, CanonicalIDSetter cids)
    {
        super(nvce, cids);
    }


    public String getAlias()
    {
        return lookupValue(ALIAS);
    }

    public <V extends IOTBase> V setAlias(String alias)
    {
        setValue(ALIAS, alias);
        return (V) this;
    }

}
