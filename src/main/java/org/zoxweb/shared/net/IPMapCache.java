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


import org.zoxweb.shared.util.KVMapStoreDefault;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.HashSet;
import java.util.LinkedHashMap;


public class IPMapCache
        extends KVMapStoreDefault<String, String> {

    public IPMapCache() {
        super(new LinkedHashMap<>(), new HashSet<>());
    }

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

    public void addExclusion(String exclusion) {
        super.addExclusion(SharedStringUtil.toTrimmedLowerCase(exclusion));
    }


}