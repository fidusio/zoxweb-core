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
package org.zoxweb.shared.data;


import org.zoxweb.shared.security.SubjectAPIKey;
import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class AppDeviceDAO
    extends SubjectAPIKey
    implements AppID<String>,
        CanonicalID
{

    /**
     * Converts the implementing object in its canonical form.
     *
     * @return text identification of the object
     */
    @Override
    public String toCanonicalID() {
        return SharedUtil.toCanonicalID('-', getDomainID(), getAppID());
    }

    public enum Param
        implements GetNVConfig
    {

        APP_ID(NVConfigManager.createNVConfig("app_id", "App ID","AppID", true, false, String.class)),
        DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "Domain ID","DomainID", true, false, String.class)),
        DEVICE(NVConfigManager.createNVConfigEntity("device", "Device information", "Device", true, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.NOT_ARRAY)),
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

    public static final NVConfigEntity NVC_APP_DEVICE_DAO = new NVConfigEntityLocal(
            "app_device_dao",
            null,
            AppDeviceDAO.class.getSimpleName(),
            true,
            false,
            false,
            false,
            AppDeviceDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SubjectAPIKey.NVC_SUBJECT_API_KEY
    );

    public AppDeviceDAO() {
        super(NVC_APP_DEVICE_DAO);
    }

    /**
     * Returns the domain ID.
     * @return
     */
    public String getDomainID() {
       return lookupValue(Param.DOMAIN_ID);
    }

    /**
     * Sets the domain ID.
     *
     * @param domainID
     */
    @Override
    public void setDomainID(String domainID) {
        setValue(Param.DOMAIN_ID, domainID);
    }

    /**
     * Returns the app ID.
     * @return
     */
    public String getAppID() {
        return lookupValue(Param.APP_ID);
    }

    /**
     * Sets the app ID.
     *
     * @param appID
     */
    @Override
    public void setAppID(String appID)
    {
        setValue(Param.APP_ID, appID);
    }


    /**
     * Returns the device.
     * @return
     */
    public DeviceDAO getDevice() {
        return lookupValue(Param.DEVICE);
    }

    /**
     * Sets the device.
     * @param device
     */
    public void setDevice(DeviceDAO device) {
        setValue(Param.DEVICE, device);
    }

   

}