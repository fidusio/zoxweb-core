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

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SetCanonicalID;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class defines the data content data access object.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class DataContentDAO 
	extends SimpleDocumentDAO
	implements SetCanonicalID
{
	/**
	 * This enum contains data content variables which include:
	 * canonical ID, content type, content, and language.
	 */
	public enum Param
		implements GetNVConfig
	{
		CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "Canonical ID", "CanonicalID", true, true, String.class)),
		CONTENT_TYPE(NVConfigManager.createNVConfig("content_type", "Content type", "ContentType", false, true, String.class)),
		LANGUAGE(NVConfigManager.createNVConfig("language", "language", "Language", true, true, String.class)),
		
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
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
	 */
	public static final NVConfigEntity NVC_DATA_CONTENT_DAO = new NVConfigEntityPortable(
																						"data_content_dao",
																						null,
																						"DataContentDAO", 
																						true,
																						false,
																						false,
																						false,
																						DataContentDAO.class,
																						SharedUtil.extractNVConfigs(Param.values()),
																						null,
																						false,
																						SimpleDocumentDAO.NVC_SIMPLE_DOCUMENT_DAO
																					);
																					
	
	/**
	 * This is the default constructor.
	 */
	public DataContentDAO()
	{
		super(NVC_DATA_CONTENT_DAO);
	}

	/**
	 * Returns string representation of this class.
	 */
	@Override
	public String toCanonicalID() 
	{
		return null;
	}
	
	/**
	 *Returns canonical ID. 
	 */
	@Override
	public String getCanonicalID() 
	{
		return lookupValue(Param.CANONICAL_ID);
	}

	/**
	 * Sets canonical ID.
	 */
	@Override
	public void setCanonicalID(String id) 
	{
		setValue(Param.CANONICAL_ID, id);
	}
	
	/**
	 * Returns content type.
	 * @return content type
	 */
	public String getContentType()
	{
		return lookupValue(Param.CONTENT_TYPE);
	}

	/**
	 * Sets content type.
	 * @param type
	 */
	public void setContentType(String type)
	{
		setValue(Param.CONTENT_TYPE, type);
	}
	
	
	/**
	 * Returns language.
	 * @return language
	 */
	public String getLanguage()
	{
		return lookupValue(Param.LANGUAGE);
	}
	
	/**
	 * Sets language.
	 * @param language
	 */
	public void setLanguage(String language)
	{
		setValue(Param.LANGUAGE, language);
	}
	
}
