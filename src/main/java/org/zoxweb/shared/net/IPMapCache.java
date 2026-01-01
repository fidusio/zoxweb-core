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


import org.zoxweb.shared.util.KVMapStoreDefault;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Cache for mapping IP addresses to MAC addresses.
 * Provides thread-safe mapping with exclusion filtering support.
 * All addresses are stored in lowercase trimmed format.
 *
 * @author mnael
 * @see KVMapStoreDefault
 */
public class IPMapCache
        extends KVMapStoreDefault<String, String> {

    /**
     * Default constructor.
     * Initializes with a LinkedHashMap for ordered storage
     * and a HashSet for exclusion filtering.
     */
    public IPMapCache() {
        super(new LinkedHashMap<>(), new HashSet<>());
    }

    /**
     * Maps an IP address to a MAC address.
     * Both addresses are normalized to lowercase and trimmed.
     * The mapping is skipped if either address is in the exclusion filter.
     *
     * @param ipAddress the IP address to map
     * @param macAddress the MAC address to associate
     * @return true if the mapping was successful, false if excluded or null
     */
    public synchronized boolean map(String ipAddress, String macAddress) {
        ipAddress = SharedStringUtil.toTrimmedLowerCase(ipAddress);
        macAddress = SharedStringUtil.toTrimmedLowerCase(macAddress);
        if (ipAddress != null && macAddress != null) {
            if (!exclusionFilter.contains(ipAddress) && !exclusionFilter.contains(macAddress)) {
                put(ipAddress, macAddress);
                return true;
            }
        }

        return false;
    }

    /**
     * Adds an address to the exclusion filter.
     * Excluded addresses will not be mapped.
     *
     * @param exclusion the address to exclude (IP or MAC)
     */
    public void addExclusion(String exclusion) {
        super.addExclusion(SharedStringUtil.toTrimmedLowerCase(exclusion));
    }


}