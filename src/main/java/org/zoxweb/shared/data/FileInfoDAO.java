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

import org.zoxweb.shared.filters.FilenameFilter;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.api.APIFileInfoMap;

@SuppressWarnings("serial")
public class FileInfoDAO
        extends DocumentInfoDAO
        implements APIFileInfoMap {

    public enum FileType
            implements GetName {
        FILE("File"),
        FOLDER("Folder");

        private final String name;

        FileType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Param
            implements GetNVConfig {
        CONTENT_TYPE(NVConfigManager.createNVConfig("content_type", "File content type", "ContentType", false, true, String.class)),
        FULL_PATH_NAME(NVConfigManager.createNVConfig("full_path_name", "The file or folder full path name", "FullPathName", true, false, String.class)),
        LENGTH(NVConfigManager.createNVConfig("length", "File length in bytes", "Length", true, false, Long.class)),
        RESOURCE_LOCATOR(NVConfigManager.createNVConfig("resource_locator", "Where the file info content location", "ResourceLocator", true, false, String.class)),
        RESOURCE_ID(NVConfigManager.createNVConfig("resource_id", "The local file info identifier", "ResourceID", true, false, String.class)),
        FILE_TYPE(NVConfigManager.createNVConfig("file_type", "The file type directory or file", "FileType", true, false, FileType.class)),
        REMOTE_FID(NVConfigManager.createNVConfigEntity("remote_file_info_dao", "The remote file info dao", "RemoteFileInfoDAO", true, false, FileInfoDAO.class, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }


    public static final NVConfigEntity NVC_FILE_INFO_DAO = new NVConfigEntityPortable("file_info_dao",
            null,
            "FileInfoDAO",
            true,
            false,
            false,
            false,
            FileInfoDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            NVC_DOCUMENT_INFO_DAO);

    /**
     * The default constructor.
     */
    public FileInfoDAO() {
        super(NVC_FILE_INFO_DAO);
    }

    /**
     * This constructor instantiates FileInfoDAO based on given NVConfigEntity.
     *
     * @param nvce
     */
    protected FileInfoDAO(NVConfigEntity nvce) {
        super(nvce);
    }

    /**
     * Set the name.
     *
     * @param name
     */
    public void setName(String name)
            throws NullPointerException, IllegalArgumentException {
        super.setName(FilenameFilter.SINGLETON.validate(name));
    }

    /**
     * Gets the file content type.
     *
     * @return content type
     */
    public String getContentType() {
        return lookupValue(Param.CONTENT_TYPE);
    }

    /**
     * Sets the file content type.
     *
     * @param ct
     */
    public void setContentType(String ct) {
        setValue(Param.CONTENT_TYPE, ct);
    }

    /**
     * Gets the file length in bytes.
     *
     * @return the length in bytes
     */
    public long getLength() {
        return lookupValue(Param.LENGTH);
    }

    /**
     * Sets the file length in bytes.
     *
     * @param id
     */
    public void setLength(long id) {
        setValue(Param.LENGTH, id);
    }


    /**
     * Gets the file type.
     *
     * @return file type
     */
    public FileType getFileType() {
        return lookupValue(Param.FILE_TYPE);
    }

    /**
     * Sets the file type.
     *
     * @param type
     */
    public void setFileType(FileType type) {
        setValue(Param.FILE_TYPE, type);
    }

    /**
     * Gets the URL location where the file is stored.
     *
     * @return resource location
     */
    public String getResourceLocation() {
        return lookupValue(Param.RESOURCE_LOCATOR);
    }

    /**
     * Sets the URL location where the file is stored.
     *
     * @param location
     */
    public void setResourceLocation(String location) {
        setValue(Param.RESOURCE_LOCATOR, location);
    }

    /**
     * Gets the URL location where the file is stored.
     *
     * @return full path name
     */
    public String getFullPathName() {
        return lookupValue(Param.FULL_PATH_NAME);
    }

    /**
     * Sets the URL location where the file is stored.
     *
     * @param fullPathName
     */
    public void setFullPathName(String fullPathName) {
        setValue(Param.FULL_PATH_NAME, fullPathName);
        setName(fullPathName);
    }


    /**
     * Gets the URL location where the file is stored.
     *
     * @return resource id
     */
    public String getResourceID() {
        return lookupValue(Param.RESOURCE_ID);
    }

    /**
     * Sets the URL location where the file is stored.
     *
     * @param location
     */
    public void setResourceID(String location) {
        setValue(Param.RESOURCE_ID, location);
    }

    /**
     * @see org.zoxweb.shared.api.APIFileInfoMap#getOriginalFileInfo()
     */
    @Override
    public FileInfoDAO getOriginalFileInfo() {
        return this;
    }

    /**
     * @see org.zoxweb.shared.api.APIFileInfoMap#setOriginalFileInfo(org.zoxweb.shared.data.FileInfoDAO)
     */
    @Override
    public void setOriginalFileInfo(FileInfoDAO info)
            throws NullPointerException, IllegalArgumentException {
        throw new IllegalArgumentException("Not supported");
    }

    /**
     * @see org.zoxweb.shared.api.APIFileInfoMap#getRemoteFileInfo()
     */
    @Override
    public FileInfoDAO getRemoteFileInfo() {
        return lookupValue(Param.REMOTE_FID);
    }

    /**
     * @see org.zoxweb.shared.api.APIFileInfoMap#setRemoteFileInfo(org.zoxweb.shared.data.FileInfoDAO)
     */
    @Override
    public void setRemoteFileInfo(FileInfoDAO info)
            throws NullPointerException, IllegalArgumentException {
        if (info != this) {
            setValue(Param.REMOTE_FID, info);
        }
    }

}