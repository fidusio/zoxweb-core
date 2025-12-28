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

import java.util.Date;

import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.Const.ResourceType;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class UUIDInfoDAO 
	extends TimeStampDAO 
{
	
	public enum Param
        implements GetNVConfig
	{
		ACCESS_COUNTER(NVConfigManager.createNVConfig("access_counter", "The access count(-1) never expire, 0 expired, > 0 still valid","AccessCounter", false, false, false, false, Long.class, null)),
		EXPIRATION_TS(NVConfigManager.createNVConfig("expiration_ts", "Time in millis when the UUID will expire","Expiration", false, false, false, false, Date.class, null)),
		RESOURCE_ID(NVConfigManager.createNVConfig("resource_id", "Resource identifier usually a reference_id","ResourceID", true, false, false, false, String.class, null)),
		RESOURCE_CANONICAL_ID(NVConfigManager.createNVConfig("resource_canonical_id", "The resource canonical ID","ResourceCanonicalID", false, false, false, false, String.class, null)),
		CONTENT(NVConfigManager.createNVConfigEntity("content", "The folder content", "Content", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		// till we support persistence maybe change to class name or NVEntity(might be a problem with resource real-time synchronization ??!)
		RESOURCE_TYPE(NVConfigManager.createNVConfig("resource_type", "Resource type","ResourceType", true, false, false, false, ResourceType.class, null)),
		RESOURCE_CRUD(NVConfigManager.createNVConfig("resource_crud", "CRUD permission for UUID","ResourceCRUD", true, false, false, false, CRUD.class, null)),
		SESSION_ID(NVConfigManager.createNVConfig("session_id", "The session id","SessionID", true, false, true, false, String.class, null)),
		
		UUID(NVConfigManager.createNVConfig("uuid", "UUID ","UUID", true, false, true, false, String.class, null)),
		
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
	
	public static final NVConfigEntity NVC_UUID_INFO_DAO = new NVConfigEntityPortable(
	        "uuid_info_dao",
            null,
            UUIDInfoDAO.class.getSimpleName(),
            true,
            false,
            false,
            false,
            UUIDInfoDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );
	
	
	/**
	 * The default constructor.
	 */
	public UUIDInfoDAO() 
	{
		super(NVC_UUID_INFO_DAO);
	}
	
	/**
	 * Returns expiration time (in milliseconds, -1 for never expires).
	 * @return expiration time
	 */
	public long getExpirationTime() 
	{
		return lookupValue(Param.EXPIRATION_TS);
	}
	
	/**
	 * Sets expiration time (in milliseconds, -1 for never expires).
	 * @param ts to be set
	 */
	public void setExpirationTime(long ts) 
	{
		setValue(Param.EXPIRATION_TS, ts);
	}
	
	/**
	 * Returns the access count (in milliseconds, -1 for never expires).
	 * @return access count
	 */
	public long getAccessCount()
	{
		return lookupValue(Param.ACCESS_COUNTER);
	}
	
	/**
	 * Sets the access count (in milliseconds, -1 for never expires).
	 * @param ac access time to be set
	 */
	public synchronized void setAccessCount(long ac) 
	{
		setValue(Param.ACCESS_COUNTER, ac);
	}
	
	/**
	 * Returns the UUID.
	 * @return uuid value
	 */
	public String getUUID() 
	{
		return lookupValue(Param.UUID);
	}
	
	/**
	 * Sets the UUID.
	 * @param uuid to be set
	 */
	public void setUUID(String uuid) 
	{
		setValue(Param.UUID, uuid);
	}
	
	/**
	 * Returns the resource ID.
	 * @return resource id
	 */
	public String getResourceID()
	{
		return lookupValue(Param.RESOURCE_ID);
	}
	
	/**
	 * Sets the resource ID.
	 * @param resourceID to be set
	 */
	public void setResourceID(String resourceID) 
	{
		setValue(Param.RESOURCE_ID, resourceID);
	}

	/**
	 * Returns the resource canonical ID.
	 * @return resource canonical id
	 */
	public String getResourceCanonicalID()
	{
		return lookupValue(Param.RESOURCE_CANONICAL_ID);
	}
	
	/**
	 * Sets the resource canonical ID.
	 * @param resourceID to be set
	 */
	public void setResourceCanonicalID(String resourceID) 
	{
		setValue(Param.RESOURCE_CANONICAL_ID, resourceID);
	}

	/**
	 * Returns the content.
	 * @return content 
	 */
	public NVEntity getContent()
	{
		return lookupValue(Param.CONTENT);
	}
	
	/**
	 * Sets the content.
	 * @param content
	 */
	public void setContent(NVEntity content) 
	{
		setValue(Param.CONTENT, content);
	}
	
	/**
	 * Returns the resource type.
	 * @return resource type 
	 */
	public ResourceType getResourceType()
	{
		return lookupValue(Param.RESOURCE_TYPE);
	}
	
	/**
	 * Sets the resource type.
	 * @param resourceType
	 */
	public void setResourceType(ResourceType resourceType) 
	{
		setValue(Param.RESOURCE_TYPE, resourceType);
	}
	
	/**
	 * Returns the resource CRUD.
	 * @return crud
	 */
	public CRUD getResourceCRUD()
	{
		return lookupValue(Param.RESOURCE_CRUD);
	}
	
	/**
	 * Sets the resource CRUD.
	 * @param crud
	 */
	public void setResourceCRUD(CRUD crud) 
	{
		setValue(Param.RESOURCE_CRUD, crud);
	}
	
	/**
	 * Returns the session ID.
	 * @return session id
	 */
	public String getSessionID()
	{
		return lookupValue(Param.SESSION_ID);
	}
	
	/**
	 * Sets the session ID.
	 * @param sessionID
	 */
	public void setSessionID(String sessionID) 
	{
		setValue(Param.SESSION_ID, sessionID);
	}

}