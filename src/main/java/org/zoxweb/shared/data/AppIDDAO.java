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

import org.zoxweb.shared.util.*;

/**
 * Created on 7/3/17
 */
@SuppressWarnings("serial")
public class AppIDDAO
    extends AppIDResource
{
    public static final NVConfigEntity NVC_APP_ID_DAO = new NVConfigEntityPortable(
            "app_id_dao",
            "AppIDDAO" ,
            AppIDDAO.class.getSimpleName(),
            true, false,
            false, false,
            AppIDDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AppIDResource.NVC_APP_ID_RESOURCE
    );

    /**
     * The default constructor.
     */
    public AppIDDAO()
    {
        super(NVC_APP_ID_DAO);
    }
    
    protected AppIDDAO(NVConfigEntity nvce)
    {
    	 super(nvce);
    }

    public AppIDDAO(String domainID, String appID)
    {
        this();
        setDomainAppID(domainID, appID);
    }

}