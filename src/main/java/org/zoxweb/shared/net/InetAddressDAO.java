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
package org.zoxweb.shared.net;

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.net.InetProp.IPVersion;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class InetAddressDAO
        extends SetNameDAO {

    private static final NVConfig INET_ADDRESS = NVConfigManager.createNVConfig("inet_address", "The ip address", "InetAddress", true, false, String.class);
    private static final NVConfig IP_VERSION = NVConfigManager.createNVConfig("ip_version", "The ip version V4 or V6", "IPVersion", true, false, IPVersion.class);
    public static final NVConfigEntity NVC_INET_ADDRESS_DAO = new NVConfigEntityPortable("inet_address_dao", null, "InetAddressDAO", true, false, false, false, InetAddressDAO.class, SharedUtil.toNVConfigList(INET_ADDRESS, IP_VERSION), null, false, SetNameDAO.NVC_NAME_DAO);

    public InetAddressDAO() {
        super(NVC_INET_ADDRESS_DAO);
    }

    public String getInetAddress() {
        return lookupValue(INET_ADDRESS);
    }

    public void setInetAddress(String address) {
        setValue(INET_ADDRESS, address);
    }

    public IPVersion getIPVersion() {
        return lookupValue(IP_VERSION);
    }

    public void setIPVersion(IPVersion ipType) {
        setValue(IP_VERSION, ipType);
    }

    public String toString() {
        return SharedUtil.toCanonicalID(':', getIPVersion(), getInetAddress());
    }

}