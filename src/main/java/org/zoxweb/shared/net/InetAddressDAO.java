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

/**
 * Data access object for internet address information.
 * Stores an IP address along with its version (IPv4 or IPv6).
 *
 * @author mnael
 * @see InetProp.IPVersion
 */
@SuppressWarnings("serial")
public class InetAddressDAO
        extends SetNameDAO {

    private static final NVConfig INET_ADDRESS = NVConfigManager.createNVConfig("inet_address", "The ip address", "InetAddress", true, false, String.class);
    private static final NVConfig IP_VERSION = NVConfigManager.createNVConfig("ip_version", "The ip version V4 or V6", "IPVersion", true, false, IPVersion.class);

    /** NVConfigEntity definition for InetAddressDAO */
    public static final NVConfigEntity NVC_INET_ADDRESS_DAO = new NVConfigEntityPortable("inet_address_dao", null, "InetAddressDAO", true, false, false, false, InetAddressDAO.class, SharedUtil.toNVConfigList(INET_ADDRESS, IP_VERSION), null, false, SetNameDAO.NVC_NAME_DAO);

    /**
     * Default constructor.
     */
    public InetAddressDAO() {
        super(NVC_INET_ADDRESS_DAO);
    }

    /**
     * Returns the internet address.
     *
     * @return the IP address string
     */
    public String getInetAddress() {
        return lookupValue(INET_ADDRESS);
    }

    /**
     * Sets the internet address.
     *
     * @param address the IP address to set
     */
    public void setInetAddress(String address) {
        setValue(INET_ADDRESS, address);
    }

    /**
     * Returns the IP version.
     *
     * @return the IP version (V4 or V6)
     */
    public IPVersion getIPVersion() {
        return lookupValue(IP_VERSION);
    }

    /**
     * Sets the IP version.
     *
     * @param ipType the IP version to set
     */
    public void setIPVersion(IPVersion ipType) {
        setValue(IP_VERSION, ipType);
    }

    /**
     * Returns a string representation in format "version:address".
     *
     * @return the canonical string representation
     */
    public String toString() {
        return SharedUtil.toCanonicalID(':', getIPVersion(), getInetAddress());
    }

}