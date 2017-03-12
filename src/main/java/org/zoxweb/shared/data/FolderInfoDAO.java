/*
 * Copyright (c) 2012-2016 ZoxWeb.com LLC.
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

import java.util.List;

import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVEntityContainer;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * 
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class FolderInfoDAO 
	extends DocumentInfoDAO
	implements NVEntityContainer
{
	
	public enum Params
		implements GetNVConfig
	{
		NVC_NAME(NVConfigManager.createNVConfig("name", null,"Name", true, true, String.class)),
		CONTENT(NVConfigManager.createNVConfigEntity("content", "The folder content", "Content", false, true, NVEntity[].class, ArrayType.REFERENCE_ID_MAP)),
		
		;
	
		private final NVConfig cType;
		Params( NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}
	
	public static final NVConfigEntity NVC_FOLDER_INFO_DAO = new NVConfigEntityLocal(
																						"folder_info_dao", 
																						null, 
																						"FolderInfoDAO", 
																						true, 
																						false, 
																						false, 
																						false, 
																						FolderInfoDAO.class, 
																						SharedUtil.extractNVConfigs(Params.values()), 
																						null, 
																						false, 
																						DocumentInfoDAO.NVC_DOCUMENT_INFO_DAO
																					);//,SharedUtil.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));
	
	/**
	 * The default constructor.
	 */
	public FolderInfoDAO()
	{
		super(NVC_FOLDER_INFO_DAO);
	}
	
	/**
	 * Gets the folder content.
	 * @return folder contenet
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getFolderContent()
	{
		return (ArrayValues<NVEntity>) lookup(Params.CONTENT);
	}
	
	/**
	 * Sets the folder content.
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public void setFolderContent(ArrayValues<NVEntity> values)
	{
		ArrayValues<NVEntity> content = (ArrayValues<NVEntity>) lookup(Params.CONTENT);
		content.add(values.values(), true);
	}
	
	/**
	 * Sets the folder content.
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public void setFolderContent(List<NVEntity> values)
	{
		ArrayValues<NVEntity> content = (ArrayValues<NVEntity>) lookup(Params.CONTENT);
		content.add(values.toArray(new NVEntity[0]), true);
	}

	/**
	 * Checks if folder contains NVEntity given reference ID of the object.
	 * Returns object if found.
	 * @param refID
	 * @return the contained nventity or null if not found
	 */
	@Override
	public NVEntity contains(String refID) 
	{
		return SharedDataUtil.searchByRefID(this, refID);
	}

	/**
	 * Checks if folder contains given NVEntity. Returns object if found.
	 * @param nve
	 * @return return the contained nventity or null
	 */
	@Override
	public NVEntity contains(NVEntity nve) 
	{
		if (nve != null)
		{
			return contains(nve.getReferenceID());
		}
		
		return null;
	}
		
}