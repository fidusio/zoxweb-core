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

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Container class for network-related property enumerations.
 * Provides definitions for IP versions, internet protocols,
 * network interface categories, types, statuses, and bonding modes.
 *
 * @author mnael
 */
public class InetProp {

    /**
     * IP version enumeration.
     */
    public enum IPVersion {
        /** IPv4 protocol */
        V4,
        /** IPv6 protocol */
        V6
    }

    /**
     * Internet protocol configuration types.
     */
    public enum InetProto
            implements GetName {
        /** No protocol configured */
        NONE,
        /** Manual configuration */
        MANUAL,
        /** Loopback interface */
        LOOPBACK,
        /** Static IP configuration */
        STATIC,
        /** Dynamic Host Configuration Protocol */
        DHCP,
        /** Point-to-Point Protocol over Ethernet */
        PPPOE;

        public String toString() {
            return getName();
        }

        public String getName() {
            return name().toLowerCase();
        }
    }

    /**
     * Network interface category enumeration.
     * <ul>
     *   <li>MAIN - primary interface for WAN or LAN</li>
     *   <li>AUXILIARY - secondary interface for WAN or LAN</li>
     *   <li>NONE - not applicable</li>
     * </ul>
     */
    public enum NICategory {
        /** Primary network interface */
        MAIN,
        /** Secondary/auxiliary network interface */
        AUXILIARY,
        /** Not applicable */
        NONE
    }

    /**
     * Network interface type enumeration.
     * <ul>
     *   <li>LAN - internal/local network interface</li>
     *   <li>WAN - external/wide area network interface</li>
     *   <li>BRIDGE - bridged network interface</li>
     * </ul>
     */
    public enum NIType {
        /** Local Area Network interface */
        LAN,
        /** Wide Area Network interface */
        WAN,
        /** Bridge interface */
        BRIDGE
    }

    /**
     * Network interface status enumeration.
     * Represents the current operational state of a network connection.
     *
     * @author mnael
     */
    public enum NIStatus {
        /** Connection is operational */
        OK,
        /** Remote internet ping failed */
        REMOTE_PING_FAILED,
        /** Router ping failed */
        ROUTER_PING_FAILED,
        /** Connection is down */
        CONNECTION_DOWN,
        /** Connection is OK but should not be used */
        DONT_USE
    }

    /**
     * Network interface bonding mode enumeration.
     * Defines how multiple network interfaces are combined.
     */
    public enum BondingMode {
        /** No bonding */
        NONE,
        /** Aggregate bandwidth across interfaces */
        AGGREGATE,
        /** Failover to backup interface on failure */
        FAILOVER;

        /**
         * Looks up a bonding mode by string value.
         *
         * @param val the string value to look up
         * @return the matching BondingMode, or NONE if not found
         */
        public static BondingMode lookup(String val) {
            BondingMode ret = (BondingMode) SharedUtil.lookupEnum(val, values());

            if (ret != null) {
                ret = NONE;
            }

            return ret;
        }
    }

}