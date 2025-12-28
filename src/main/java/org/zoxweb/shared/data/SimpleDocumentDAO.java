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

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * 
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class SimpleDocumentDAO
	extends TimeStampDAO
{

	public enum Param
		implements GetNVConfig
	{
		CONTENT(NVConfigManager.createNVConfig("content", "Content of the document", "Content", false, true, false, false, String.class, FilterType.CLEAR)),
		
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
	
	public static final NVConfigEntity NVC_SIMPLE_DOCUMENT_DAO = new NVConfigEntityPortable(
            "simple_document_dao",
            "Simple note",
            "SimpleNote",
            true,
            false,
            false,
            false,
            SimpleDocumentDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );
	
	/**
	 * The default constructor.
	 */
	public SimpleDocumentDAO() 
	{
		super(NVC_SIMPLE_DOCUMENT_DAO);
	}
	
	/**
	 * This constructor instantiates SimpleDocumentDAO based on given NVConfigEntity parameter.
	 * @param nvce
	 */
	protected SimpleDocumentDAO(NVConfigEntity nvce) 
	{
		super(nvce);
	}
	
	/**
	 * Returns content.
	 * @return content
	 */
	public String getContent()
	{
		return lookupValue(Param.CONTENT);
	}
	
	/**
	 * Sets content.
	 * @param content
	 */
	public void setContent(String content)
	{
		setValue(Param.CONTENT, content);
	}
	
}