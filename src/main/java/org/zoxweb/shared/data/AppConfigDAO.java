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


import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;



/**
 * Created on 8/11/17
 */
@SuppressWarnings("serial")
public class AppConfigDAO
        extends PropertyDAO {

    public enum Param
        implements GetNVConfig
    {
        APP_ID(NVConfigManager.createNVConfigEntity("app_id", "App ID", "AppID", true, false, AppIDDAO.NVC_APP_ID_DAO, NVConfigEntity.ArrayType.NOT_ARRAY)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_APP_CONFIG_DAO = new NVConfigEntityPortable(
            "app_config_dao",
            null,
            "AppConfigDAO",
            true,
            false,
            false,
            false,
            AppConfigDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    public AppConfigDAO()
    {
        super(NVC_APP_CONFIG_DAO);
    }

    public AppConfigDAO(AppIDDAO appIDDAO)
    {
        this();
        setAppIDDAO(appIDDAO);
    }

    public AppIDDAO getAppIDDAO() {
        return lookupValue(Param.APP_ID);
    }

    public void setAppIDDAO(AppIDDAO appID) {
        setValue(Param.APP_ID, appID);
    }
}