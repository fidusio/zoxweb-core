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
package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * Abstract base class for all IoT (Internet of Things) entities.
 * Provides common properties like canonical ID and user-defined alias
 * that are shared across all IoT-related data objects.
 *
 * @author mnael
 * @see PropertyDAO
 * @see DeviceInfo
 * @see PortInfo
 * @see ProtocolInfo
 */
public abstract class IOTBase
extends PropertyDAO
{

    /** Configuration for the user-defined alias property */
    public static final NVConfig ALIAS = NVConfigManager.createNVConfig("alias", "User defined alias", "Alias", false, true, String.class);

    /** NVConfigEntity definition for IOTBase */
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




    /**
     * Protected constructor for subclasses.
     *
     * @param nvce the NVConfigEntity for this IoT entity
     * @param cids the canonical ID setter for generating unique identifiers
     */
    protected IOTBase(NVConfigEntity nvce, CanonicalIDSetter cids)
    {
        super(nvce, cids);
    }

    /**
     * Returns the user-defined alias for this IoT entity.
     *
     * @return the alias string, or null if not set
     */
    public String getAlias()
    {
        return lookupValue(ALIAS);
    }

    /**
     * Sets the user-defined alias for this IoT entity.
     *
     * @param alias the alias to set
     * @param <V> the concrete type of this IoT entity
     * @return this instance for method chaining
     */
    public <V extends IOTBase> V setAlias(String alias)
    {
        setValue(ALIAS, alias);
        return (V) this;
    }

}
