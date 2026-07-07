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
package org.zoxweb.shared.app;

import org.zoxweb.shared.data.AppIDResource;
import org.zoxweb.shared.util.*;

/**
 * Created on 7/3/17
 */
@SuppressWarnings("serial")
public class AppIDDefault
        extends AppIDResource {
    public static final NVConfigEntity NVC_APP_ID_DEFAULT = new NVConfigEntityPortable(
            "app_id_default",
            "AppIDDefault",
            AppIDDefault.class.getSimpleName(),
            true, false,
            false, false,
            AppIDDefault.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AppIDResource.NVC_APP_ID_RESOURCE
    );

    /**
     * The default constructor.
     */
    public AppIDDefault() {
        super(NVC_APP_ID_DEFAULT);
    }

    protected AppIDDefault(NVConfigEntity nvce) {
        super(nvce);
    }

    public AppIDDefault(String domainID, String appID) {
        this();
        setDomainAppID(domainID, appID);
    }


    public static AppIDDefault create(String domainID, String appID) {
        return new AppIDDefault(domainID, appID);
    }


    public static AppIDDefault create(String domainAppID) {
        int index = domainAppID.indexOf(AppID.CAN_ID_SEP);
        if (index == -1) {
            throw new IllegalArgumentException(AppID.CAN_ID_SEP + " separator " + domainAppID + " missing");
        }
        return new AppIDDefault(domainAppID.substring(0, index), domainAppID.substring(index + 1));

    }


}