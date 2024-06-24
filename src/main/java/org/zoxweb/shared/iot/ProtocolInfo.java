package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

public class ProtocolInfo
extends IOTBase
{

    public static final NVConfigEntity NVC_IOT_PROTOCOL = new NVConfigEntityLocal("iot_protocol",
            null,
            "ProtocolInfo",
            true,
            false,
            false,
            false,
            ProtocolInfo.class,
            SharedUtil.toNVConfigList(DataConst.DataParam.UNIQUE_NAME.getNVConfig()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO);

    public ProtocolInfo()
    {
        super(NVC_IOT_PROTOCOL);
    }
}
