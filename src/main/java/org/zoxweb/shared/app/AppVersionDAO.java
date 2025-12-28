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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;

import java.util.Date;

/**
 * The ApplicationInfoDAO class defines information about a system application.
 *
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class AppVersionDAO
        extends SetNameDescriptionDAO
        implements CanonicalID {

    public enum Param
            implements GetNVConfig {
        MAJOR(NVConfigManager.createNVConfig("major", "The major number of the version", "Minor", false, true, int.class)),
        MINOR(NVConfigManager.createNVConfig("minor", "The minor number of the version", "Major", true, true, int.class)),
        NANO(NVConfigManager.createNVConfig("nano", "The nano number of the version", "Nano", true, true, int.class)),
        RELEASE_DATE(NVConfigManager.createNVConfig("release_date", "Last update timestamp (in millis).", "LastUpdateTS", true, false, false, true, Date.class, null));

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_APPLICATION_VERSION_DAO = new NVConfigEntityPortable(
            "application_version_dao",
            null,
            "ApplicationVersionDAO",
            true,
            false,
            false,
            false,
            AppVersionDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );


    public AppVersionDAO(String nameDescriptionVersion) {
        this();
        String[] parsed = nameDescriptionVersion.split("::");
        int index = 0;
        switch (parsed.length) {

            case 1:
                // major.minor.nano
                parseVersion(parsed[index]);
                break;
            case 2:
                // we have name::major.minor.nano
                setName(parsed[index++]);
                parseVersion(parsed[index]);
                break;

            case 3:
                // we have name::description::major.minor.nano
                setName(parsed[index++]);
                setDescription(parsed[index++]);
                parseVersion(parsed[index]);
                break;
        }
    }


    private void parseVersion(String version) {
        String[] versions = version.split("\\.");
        if (versions.length != 3) throw new IllegalArgumentException("Invalid version format " + version);
        setMajor(Integer.parseInt(versions[0]));
        setMinor(Integer.parseInt(versions[1]));
        setNano(Integer.parseInt(versions[2]));
    }

    /**
     * The default constructor.
     */
    public AppVersionDAO() {
        super(NVC_APPLICATION_VERSION_DAO);
    }


    /**
     * Returns the major number of the version.
     *
     * @return the major value
     */
    public int getMajor() {
        return lookupValue(Param.MAJOR);
    }

    /**
     * Sets the major number of the version.
     *
     * @param major
     */
    public void setMajor(int major) {
        setValue(Param.MAJOR, major);
    }

    /**
     * Returns the minor number of the version.
     *
     * @return the minor value
     */
    public int getMinor() {
        return lookupValue(Param.MINOR);
    }

    /**
     * Sets the minor number of the version.
     *
     * @param minor
     */
    public void setMinor(int minor) {
        if (minor < 0 || minor > 9) {
            throw new IllegalArgumentException(minor + " invalid minor value valid [0-9]");
        }

        setValue(Param.MINOR, minor);
    }

    /**
     * Gets the nano number of the version.
     *
     * @return the nano value
     */
    public int getNano() {
        return lookupValue(Param.NANO);
    }

    /**
     * Sets the nano number of the version.
     *
     * @param nano
     */
    public void setNano(int nano) {
        if (nano < 0 || nano > 99) {
            throw new IllegalArgumentException(nano + " invalid nano value valid [0-9]");
        }
        setValue(Param.NANO, nano);
    }

    /**
     * @see org.zoxweb.shared.util.CanonicalID#toCanonicalID()
     */
    @Override
    public String toCanonicalID() {
        return getName() + "-" + getMajor() + "." + getMinor() + "." + getNano();
    }


    public String toString() {
        return toCanonicalID();
    }

    public Date getReleaseDate() {
        long ts = lookupValue(Param.RELEASE_DATE);
        return new Date(ts);
    }


    public String version()
    {
        return  getMajor() + "." + getMinor() + "." + getNano();
    }


    public GetNameValue<String> toNVVersion() {
        return GetNameValue.create("VERSION", toCanonicalID());
    }

    public void setReleaseDate(Date date) {
        setValue(Param.RELEASE_DATE, date.getTime());
    }
}