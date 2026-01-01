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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVStringList;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Data access object for network connection configuration.
 * Stores connection settings including protocol schemes, socket configuration,
 * and SSL/TLS configuration options.
 *
 * @author mnael
 * @see IPAddress
 */
@SuppressWarnings("serial")
public class ConnectionConfig
        extends SetNameDescriptionDAO {

    /**
     * Configuration parameters for ConnectionConfig.
     */
    public enum Param
            implements GetNVConfig {
        /** Protocol schemes supported by this connection (e.g., HTTP, HTTPS) */
        SCHEMES(NVConfigManager.createNVConfig("schemes", "Protocol schemes", "Schemes", false, true, NVStringList.class)),
        /** Socket configuration including address and port */
        SOCKET_CONFIG(NVConfigManager.createNVConfigEntity("socket_config", "Socket configuration", "SocketConfig", false, true, IPAddress.NVC_IP_ADDRESS)),
        /** SSL/TLS configuration options */
        SSL_CONFIG(NVConfigManager.createNVConfig("ssl_config", "SSL configuration", "SSLConfig", false, true, NVGenericMap.class)),


        ;

        private final NVConfig cType;

        Param(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }


    }

    /** NVConfigEntity definition for ConnectionConfig */
    public final static NVConfigEntity NVC_CONNECTION_CONFIG_DAO = new NVConfigEntityPortable("connection_config", null, "ConnectionConfig", false, true, false, false, ConnectionConfig.class, SharedUtil
            .extractNVConfigs(Param.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);

    /**
     * Default constructor.
     */
    public ConnectionConfig() {
        super(NVC_CONNECTION_CONFIG_DAO);
    }

    /**
     * Returns the protocol schemes for this connection.
     *
     * @return array of scheme strings
     */
    public String[] getSchemes() {
        return ((NVStringList) lookup(Param.SCHEMES)).getValues();
    }

    /**
     * Sets the protocol schemes for this connection.
     *
     * @param schemes the schemes to set
     */
    public void setSchemes(String... schemes) {
        ((NVStringList) lookup(Param.SCHEMES)).setValues(schemes);
    }

    /**
     * Sets the socket configuration.
     *
     * @param sc the IP address configuration
     */
    public void setSocketConfig(IPAddress sc) {
        setValue(Param.SOCKET_CONFIG, sc);
    }

    /**
     * Returns the socket configuration.
     *
     * @return the IP address configuration
     */
    public IPAddress getSocketConfig() {
        return lookupValue(Param.SOCKET_CONFIG);
    }

    /**
     * Returns the SSL/TLS configuration map.
     *
     * @return the SSL configuration as a generic map
     */
    public NVGenericMap getSSLConfig() {
        return (NVGenericMap) lookup(Param.SSL_CONFIG);
    }
}
