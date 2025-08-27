/*
 * Copyright (c) 2012-2020 ZoxWeb.com LLC.
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
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class SimpleMessage
	extends PropertyDAO
{
	public enum Param
		implements GetNVConfig
	{
		MESSAGE(NVConfigManager.createNVConfig("message", "Content of the message", "Message", false, true, false, false, String.class, FilterType.CLEAR)),
		ERROR(NVConfigManager.createNVConfig("error", "Error message if available", "ErrorMessage", false, true, false, false, String.class, FilterType.CLEAR)),
		CATEGORY(NVConfigManager.createNVConfig("category", "Message category", "Category", false, true, false, false, String.class, FilterType.CLEAR)),
		TYPE(NVConfigManager.createNVConfig("type", "Message type", "Type", false, true, false, false, String.class, FilterType.CLEAR)),
		STATUS(NVConfigManager.createNVConfig("status", "Message status code", "Status", false, true, int.class)),
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
	
	public static final NVConfigEntity NVC_SIMPLE_MESSAGE = new NVConfigEntityPortable(
            "simple_message",
            "Simple Message",
            "SimpleMessage",
            true,
            false,
            false,
            false,
            SimpleMessage.class,
            SharedUtil.extractNVConfigs(Param.MESSAGE,Param.ERROR,Param.CATEGORY,Param.TYPE, Param.STATUS, PropertyDAO.Param.PROPERTIES),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );
	
	/**
	 * The default constructor.
	 */
	public SimpleMessage() 
	{
		super(NVC_SIMPLE_MESSAGE);
		setCreationTime(System.currentTimeMillis());
	}

	public SimpleMessage(String message, int status)
	{
		this(message, status, null);
	}
	public SimpleMessage(String message, int status, String error)
	{
		this();
		setMessage(message);
		setStatus(status);
		setError(error);
	}
	
	/**
	 * This constructor instantiates SimpleDocumentDAO based on given NVConfigEntity parameter.
	 * @param nvce
	 */
	protected SimpleMessage(NVConfigEntity nvce) 
	{
		super(nvce);
	}
	
	/**
	 * Returns message.
	 * @return message
	 */
	public String getMessage()
	{
		return lookupValue(Param.MESSAGE);
	}
	
	/**
	 * Sets message.
	 * @param message
	 */
	public void setMessage(String message)
	{
		setValue(Param.MESSAGE, message);
	}


	/**
	 * Returns error message.
	 * @return error
	 */
	public String getError()
	{
		return lookupValue(Param.ERROR);
	}

	/**
	 * Sets error message.
	 * @param error
	 */
	public void setError(String error)
	{
		setValue(Param.ERROR, error);
	}


	/**
	 * Returns message code.
	 * @return code.
	 */
	public int getStatus()
	{
		return lookupValue(Param.STATUS);
	}

	/**
	 * Sets message code
	 * @param status
	 */
	public void setStatus(int status) { setValue(Param.STATUS, status); }


	public String getCategory()
	{
		return lookupValue(Param.CATEGORY);
	}

	public void setCategory(String category)
	{
		setValue(Param.CATEGORY, category);
	}
	public String getType()
	{
		return lookupValue(Param.TYPE);
	}

	public void setType(String type)
	{
		setValue(Param.TYPE, type);
	}
	
}