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
package org.zoxweb.shared.data;


import org.zoxweb.shared.security.SubjectAPIKey;
import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class AppDeviceInfo
        extends SubjectAPIKey
        implements CanonicalID {

    /**
     * Converts the implementing object in its canonical form.
     *
     * @return text identification of the object
     */
    @Override
    public String toCanonicalID() {
        return SUS.toCanonicalID('-', getAppID().getDomainID(), getAppID().getAppID());
    }

    public enum Param
            implements GetNVConfig {


        DEVICE(NVConfigManager.createNVConfigEntity("device", "Device information", "Device", true, false, DeviceInfo.NVC_DEVICE_INFO, NVConfigEntity.ArrayType.NOT_ARRAY)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_APP_DEVICE_INFO = new NVConfigEntityPortable(
            "app_device_info",
            null,
            AppDeviceInfo.class.getSimpleName(),
            true,
            false,
            false,
            false,
            AppDeviceInfo.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            SubjectAPIKey.NVC_SUBJECT_API_KEY
    );

    public AppDeviceInfo() {
        super(NVC_APP_DEVICE_INFO);
    }


    /**
     * Returns the device.
     *
     * @return
     */
    public DeviceInfo getDevice() {
        return lookupValue(Param.DEVICE);
    }

    /**
     * Sets the device.
     *
     * @param device
     */
    public void setDevice(DeviceInfo device) {
        setValue(Param.DEVICE, device);
    }

    public String getDomainID() {
        return getAppID().getDomainID();
    }
}