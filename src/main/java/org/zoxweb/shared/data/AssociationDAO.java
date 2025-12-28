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

import org.zoxweb.shared.util.Const;
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
public class AssociationDAO
	extends SetNameDescriptionDAO	
{
	public enum Param
	    implements GetNVConfig
	{
		ASSOCIATED(NVConfigManager.createNVConfigEntity("associated", "The mapped reference", "Mapped", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		MAPPED(NVConfigManager.createNVConfigEntity("mapped", "The mapped to reference", "MappedTo", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		MAP_TYPE(NVConfigManager.createNVConfig("map_type", "Mapping type", "MapType", false, true, false, String.class, Const.ASSOCIATION_TYPE)),
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
	
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on AddressDAO.
	 */
	public static final NVConfigEntity NVC_ASSOCIATION_DAO = new NVConfigEntityPortable("association_dao", null , "AssociationDAO", true, false, false, false, AssociationDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);//,SharedUtil.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));


	public AssociationDAO()
	{
		super(NVC_ASSOCIATION_DAO);
	}
	
	public NVEntity getAssociated()
	{
		return lookupValue(Param.ASSOCIATED);
	}
	
	public void setAssociated(NVEntity associated)
	{
		setValue(Param.ASSOCIATED, associated);
	}
	

	public NVEntity getMapped()
	{
		return lookupValue(Param.MAPPED);
	}
	
	public void setMapped(NVEntity mapped)
	{
		setValue(Param.MAPPED, mapped);
	}
	
	
	public String getAssociationType()
	{
		return lookupValue(Param.MAP_TYPE);
	}
	
	public void setAssociationType(String mapType)
	{
		setValue(Param.MAP_TYPE, mapType);
	}
	
}
