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


import org.zoxweb.shared.util.*;

/**
 * This is the base class of all classes and object used via all projects
 * any update or modification must be approved by the architect.
 */
public abstract class ReferenceIDImpl
        extends NVEntity {

    public static final NVConfig NVC_SUBJECT_GUID = NVConfigManager.createNVConfig(MetaToken.SUBJECT_GUID.getName(), "The user id or Subject GUID", "SubjectGUID", true, false, false, true, false, String.class, null);
    public static final NVConfig NVC_GUID = NVConfigManager.createNVConfig(MetaToken.GUID.getName(), "The global id of the Object", "GlobalUID", false, false, false, true, false, String.class, null);
    public static final NVConfig NVC_APP_GUID = NVConfigManager.createNVConfig(MetaToken.APP_GUID.getName(), "The global id of the Object", "GlobalUID", true, false, true, true, false, String.class, null);
    public static final NVConfigEntity NVC_REFERENCE_ID_IMPL = new NVConfigEntityPortable("reference_id_impl", null, "ReferenceIDImpl", true, false, false, false, ReferenceIDImpl.class, SUS.toNVConfigList(NVC_SUBJECT_GUID, NVC_GUID, NVC_APP_GUID), null, false, null);

    protected ReferenceIDImpl(NVConfigEntity nvConfigEntity) {
        super(nvConfigEntity);
    }


    /**
     * Returns the global GUID, the GUID is a portable UUID that can be used to uniquely identify and instance across system it is data store agnostic.
     *
     * @return global guid
     */
    @Override
    public String getGUID() {
        return lookupValue(NVC_GUID);
    }

    /**
     * Sets the global ID.
     *
     * @param globalGUID global uuid
     */
    @Override
    public void setGUID(String globalGUID) {
        setValue(NVC_GUID, globalGUID);
    }

    /**
     * Returns the subject_GUID the uuid of the subject that created this object.
     *
     * @return global_guid
     */
    @Override
    public String getSubjectGUID() {
        return lookupValue(NVC_SUBJECT_GUID);
    }

    /**
     * Sets the Subject GUID.
     *
     * @param subjectGUID of the creator of this object
     */
    @Override
    public void setSubjectGUID(String subjectGUID) {
        setValue(NVC_SUBJECT_GUID, subjectGUID);
    }

    @Override
    public String getAppGUID() {
        return lookupValue(NVC_APP_GUID);
    }

    @Override
    public void setAppGUID(String appGUID) {
        setValue(NVC_APP_GUID, appGUID);
    }
}