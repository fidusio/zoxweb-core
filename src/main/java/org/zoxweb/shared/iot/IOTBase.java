package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

public abstract class IOTBase
extends PropertyDAO
{

    public static final NVConfigEntity NVC_IOT_BASE = new NVConfigEntityLocal("iot_base",
            null,
            "IOTBase",
            true,
            false,
            false,
            false,
            IOTBase.class,
            SharedUtil.toNVConfigList(DataConst.DataParam.UNIQUE_CANONICAL_ID.getNVConfig()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO);




    protected IOTBase(NVConfigEntity nvce, CanonicalIDSetter cids)
    {
        super(nvce, cids);
    }


}
