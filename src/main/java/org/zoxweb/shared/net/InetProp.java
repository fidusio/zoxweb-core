/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

public class InetProp {

    public enum IPVersion {
        V4,
        V6
    }


    public enum InetProto
            implements GetName {
        NONE,
        MANUAL,
        LOOPBACK,
        STATIC,
        DHCP,
        PPPOE;

        public String toString() {
            return getName();
        }


        public String getName() {
            return name().toLowerCase();
        }
    }

    /**
     * The network interface category
     * MAIN this is the main interface for WAN OR LAN
     * AUXILIARY this is an AUXILIARY interface for the WAN OR LAN
     * NONE not applicable
     */
    public enum NICategory {
        MAIN,
        AUXILIARY,
        NONE
    }

    /**
     * The network interface type
     * LAN internal interface
     * WAN external interface
     * BRIDGE interface
     */
    public enum NIType {
        LAN,
        WAN,
        BRIDGE
    }

    /**
     * This enum represent the network connection status,
     * @author mnael
     *
     */
    public enum NIStatus {
        OK, // the connection is ok
        REMOTE_PING_FAILED, // remote internet ping failed
        ROUTER_PING_FAILED,// router ping failed
        CONNECTION_DOWN, // the connection is down
        DONT_USE // the connection is ok but don't use
    }


    public enum BondingMode {
        NONE,
        AGGREGATE,
        FAILOVER;

        public static BondingMode lookup(String val) {
            BondingMode ret = (BondingMode) SharedUtil.lookupEnum(val, values());

            if (ret != null) {
                ret = NONE;
            }

            return ret;
        }
    }

}