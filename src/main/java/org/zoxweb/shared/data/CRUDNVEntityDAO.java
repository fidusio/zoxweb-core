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

import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.CRUDNVEntity;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * 
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class CRUDNVEntityDAO
	extends SetNameDAO
	implements CRUDNVEntity
{

	public enum Param
		implements GetNVConfig
	{
		CRUD_OP(NVConfigManager.createNVConfig("crud_op", "CRUD operation", "CRUDOperation", true, false, CRUD.class)),
		NVE(NVConfigManager.createNVConfigEntity("nve", "NVEntity", "NVEntity", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		REVISION_TRACKING(NVConfigManager.createNVConfig("revision_tracking", "Revision Tracking", "RevisionTracking", false, false, Boolean.class))

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
	
	public static final NVConfigEntity NVC_CRUD_NVENTITY_DAO = new NVConfigEntityPortable("crud_nventity_dao", null , "CRUDNVEntityDAO", true, false, false, false, CRUDNVEntityDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, SetNameDAO.NVC_NAME_DAO);

	public CRUDNVEntityDAO()
	{
		super(NVC_CRUD_NVENTITY_DAO);
	}
	
	
	
	public CRUDNVEntityDAO(CRUD crud, NVEntity nve)
	{
		this(crud, nve, false);
	}
	
	public CRUDNVEntityDAO(CRUD crud, NVEntity nve, boolean revisionTracking)
	{
		this();
		setCRUD(crud);
		setNVEntity(nve);
		setRevisionTrackingEnabled(revisionTracking);
	}
	
	@Override
	public CRUD getCRUD() 
	{
		return lookupValue(Param.CRUD_OP);
	}
	
	public void setCRUD(CRUD crud)
	{
		setValue(Param.CRUD_OP, crud);
	}
	
	@Override
	public NVEntity getNVEntity() 
	{
		return lookupValue(Param.NVE);
	}
	
	public void setNVEntity(NVEntity nve)
	{
		setValue(Param.NVE, nve);
	}

	@Override
	public boolean isRevisionTrackingEnabled()
	{
		return lookupValue(Param.REVISION_TRACKING);
	}
	
	public void setRevisionTrackingEnabled(boolean revTracking)
	{
		setValue(Param.REVISION_TRACKING, revTracking);
	}

}