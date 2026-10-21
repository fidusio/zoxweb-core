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
import org.zoxweb.shared.filters.FQCNFilter;
import org.zoxweb.shared.util.*;

import java.util.List;

/**
 * The persisted record of an app ({@code <domain>-<app>}): its own GUID is its {@code app_guid},
 * and since 2026-10-20 it carries the app's list of registered entity classes
 * ({@link #getResourceTypes()}): the fully qualified class names whose rows, created under a
 * login into the app, the app owns ({@code DomainSecurityManager.addFQCNToApp} /
 * {@code removeFQCNFromApp}). The list belongs to the app record, not to a role.
 * <p>Created on 7/3/17</p>
 */
@SuppressWarnings("serial")
public class AppIDDefault
        extends AppIDResource {

    public enum Param
            implements GetNVConfig {
        /** The registered entity classes (fully qualified names, {@link FQCNFilter}); optional, stored as a string list. */
        RESOURCE_TYPES(NVConfigManager.createNVConfig("resource_types", "Registered entity classes the app owns the rows of", "ResourceTypes", false, true, NVStringList.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_APP_ID_DEFAULT = new NVConfigEntityPortable(
            "app_id_default",
            "AppIDDefault",
            AppIDDefault.class.getSimpleName(),
            true, false,
            false, false,
            AppIDDefault.class,
            SUS.extractNVConfigs(Param.values()),
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


    /**
     * Parses the canonical id {@code <domain>-<app>}. The split is at the <b>last</b> separator: an
     * app name is letters and digits only ({@code AppIDNameFilter}) and can never contain one,
     * while a domain such as {@code my-site.com} can.
     */
    public static AppIDDefault create(String domainAppID) {
        SUS.checkIfNulls("domainAppID can't be null", domainAppID);
        int index = domainAppID.lastIndexOf(AppID.CAN_ID_SEP);
        if (index == -1) {
            throw new IllegalArgumentException(AppID.CAN_ID_SEP + " separator " + domainAppID + " missing");
        }
        return new AppIDDefault(domainAppID.substring(0, index), domainAppID.substring(index + 1));

    }

    @Override
    public void setGUID(String guid) {
        super.setGUID(guid);
        super.setAppGUID(guid);
    }

    @Override
    public void setAppGUID(String appGUID) {
        this.setGUID(appGUID);
    }

    // ------------------------------------------------------------------
    // registered entity classes (2026-10-20): the app's own list, validated by FQCNFilter
    // ------------------------------------------------------------------

    private NVStringList resourceTypesList() {
        NVStringList ret = lookup(Param.RESOURCE_TYPES);
        if (ret == null) {
            ret = new NVStringList(Param.RESOURCE_TYPES.getNVConfig().getName());
            attributes.put(ret.getName(), ret);
        }
        return ret;
    }

    /** @return the registered class names, in registration order; empty when none */
    public String[] getResourceTypes() {
        return resourceTypesList().getValues();
    }

    /**
     * Replaces the registered classes.
     *
     * @param classNames fully qualified class names; null or empty clears the list
     * @throws IllegalArgumentException if a name is not a valid class name (nothing is changed)
     */
    public void setResourceTypes(String... classNames) {
        String[] valid = validateResourceTypes(classNames);
        NVStringList list = resourceTypesList();
        list.getValue().clear();
        for (String name : valid) {
            if (!list.contains(name)) list.add(name);
        }
    }

    /**
     * Registers classes; a name already registered is left as it is.
     *
     * @return the number of names added
     * @throws IllegalArgumentException if a name is not a valid class name (nothing is changed)
     */
    public int addResourceType(String... classNames) {
        String[] valid = validateResourceTypes(classNames);
        NVStringList list = resourceTypesList();
        int added = 0;
        for (String name : valid) {
            if (!list.contains(name)) {
                list.add(name);
                added++;
            }
        }
        return added;
    }

    /**
     * Unregisters classes; a name not registered is no error.
     *
     * @return the number of names removed
     * @throws IllegalArgumentException if a name is not a valid class name (nothing is changed)
     */
    public int removeResourceType(String... classNames) {
        String[] valid = validateResourceTypes(classNames);
        List<String> list = resourceTypesList().getValue();
        int removed = 0;
        for (String name : valid) {
            if (list.remove(name)) removed++;
        }
        return removed;
    }

    /** @return true when {@code className} (or the class's name) is registered on this app */
    public boolean isResourceTypeRegistered(String className) {
        return className != null && resourceTypesList().contains(className.trim());
    }

    public boolean isResourceTypeRegistered(Class<?> clazz) {
        return clazz != null && isResourceTypeRegistered(clazz.getName());
    }

    /** Every name through {@link FQCNFilter}, before anything is written: one bad name fails the whole call. */
    private static String[] validateResourceTypes(String... classNames) {
        if (classNames == null) {
            return new String[0];
        }
        String[] ret = new String[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            ret[i] = FQCNFilter.SINGLETON.validate(classNames[i]);
        }
        return ret;
    }

}