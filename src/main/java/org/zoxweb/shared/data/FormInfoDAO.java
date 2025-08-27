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

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SetName;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 *
 */
@SuppressWarnings("serial")
public class FormInfoDAO
	extends DocumentInfoDAO
{
	public enum Param
        implements GetNVConfig
	{
		FORM_REFERENCE(NVConfigManager.createNVConfigEntity("form_reference", "The form reference", "FormReference", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		
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

	public static final NVConfigEntity NVC_FORM_INFO_DAO = new NVConfigEntityPortable(
																					"form_info_dao", 
																					null, 
																					"FormInfoDAO", 
																					true, 
																					false, 
																					false, 
																					false, 
																					FormInfoDAO.class, 
																					SharedUtil.extractNVConfigs(Param.values()),
																					null, 
																					false, 
																					DocumentInfoDAO.NVC_DOCUMENT_INFO_DAO
																				);	//,SharedUtil.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));

	/**
	 * The default constructor.
	 */
	public FormInfoDAO()
	{
		super(NVC_FORM_INFO_DAO);
	}

	/**
	 * This constructor instantiates FormInfoDAO based on given NVConfigEntity.
	 * @param nvce
	 */
	protected FormInfoDAO(NVConfigEntity nvce) 
	{
		super(nvce);
	}
	
	/**
     * Returns the referenced NVEntity.
	 * @return the form reference
	 */
	public NVEntity getFormReference()
	{
		return lookupValue(Param.FORM_REFERENCE);
	}
	
	/**
	 * Set the form reference based on given NVEntity.
	 * @param nve
	 */
	public void setFormReference(NVEntity nve)
	{
		setValue(Param.FORM_REFERENCE, nve);
	}
	
//	public String getName()
//	{
//		NVEntity nve = getFormReference();
//		if (nve != null)
//		{
//			return nve.getName();
//		}
//		
//		return null;
//	}
	
	/**
	 * Sets the form name.
	 * @param name
	 */
	public synchronized void setName(String name)
	{
		NVEntity nve = getFormReference();
		
		if (nve != null && nve instanceof SetName)
		{
			((SetName) nve).setName(name);
		}
		
		super.setName(name);
	}

}