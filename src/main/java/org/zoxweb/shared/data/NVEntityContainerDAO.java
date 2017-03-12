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
public class NVEntityContainerDAO 
	extends CanonicalIDDAO
	implements NVEntityContainer
{

	public enum Params
		implements GetNVConfig
	{
		
		CONTENT(NVConfigManager.createNVConfigEntity("content", "Content", "Content", false, true, NVEntity[].class, ArrayType.LIST)),
		
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}

	public static final NVConfigEntity NVC_NVENTITY_CONTAINER_DAO = new NVConfigEntityLocal(
																								"nventity_container_dao", 
																								null, 
																								"Dossier", 
																								true, 
																								false, 
																								false, 
																								false, 
																								NVEntityContainerDAO.class, 
																								SharedUtil.extractNVConfigs(Params.values()), 
																								null, 
																								false, 
																								CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																							);
	
	/**
	 * The default constructor.
	 */
	public NVEntityContainerDAO()
	{
		super(NVC_NVENTITY_CONTAINER_DAO);
	}
	
	/**
	 * This constructor instantiates NVEntityContainerDAO based on given NVConfigEntity parameter.
	 * @param nvce
	 */
	protected NVEntityContainerDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Returns content as an array values of NVEntity.
	 * @return content
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getContent()
	{
		return (ArrayValues<NVEntity>) lookup(Params.CONTENT);
	}
	
	/**
	 * Sets content given an array values of NVEntity.
	 * @param values
	 */
	public void setContent(ArrayValues<NVEntity> values)
	{
		getContent().add(values.values(), true);
	}
	
	/**
	 * Sets content given a list of NVEntity.
	 * @param values
	 */
	public void setContent(List<NVEntity> values)
	{
		getContent().add(values.toArray(new NVEntity[0]), true);
	}
	
	/**
	 * Returns content as a list of NVEntity.
	 * @return content as list
	 */
	public List<NVEntity> getContentAsList()
	{
		return lookupValue(Params.CONTENT);
	}

	/**
	 * Checks if content contains NVEntity given the reference ID of the object.
	 * Returns object if found.
	 * @param refID
	 * @return contained nventity
	 */
	@Override
	public NVEntity contains(String refID)
	{
		if (refID != null)
		{
			for (NVEntity nve : getContent().values())
			{
				if (refID.equals(nve.getReferenceID()))
				{
					return nve;
				}
			}
		}
		
		return null;
	}

	/**
	 * Checks if content contains given NVEntity. Returns object if found.
	 * @param nve
	 * @return contained nventity
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