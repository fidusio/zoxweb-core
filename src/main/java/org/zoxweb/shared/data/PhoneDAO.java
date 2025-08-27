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

import org.zoxweb.shared.filters.NumberFilter;
import org.zoxweb.shared.filters.PhoneNumberFilter;
import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class defines the phone data access object used to create phone numbers 
 * associated with an account.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class PhoneDAO 
	extends SetNameDescriptionDAO 
	implements CanonicalID
{
	
	/**
	 * This enum includes the following parameters:
	 * phone type, country code, area code, number, and
	 * the extension.
	 * @author mzebib
	 *
	 */
	public enum Param
		implements GetNVConfig
	{
		PHONE_TYPE(NVConfigManager.createNVConfig("phone_type", "Phone_type", "PhoneType", false, true, false, String.class, DataConst.PHONE_TYPES)),
		COUNTRY_CODE(NVConfigManager.createNVConfig("country_code", "Country code", "CountryCode", true, true, false, String.class, DataConst.COUNTRY_CODES)),
		AREA_CODE(NVConfigManager.createNVConfig("area_code", "Area code", "AreaCode", true, true, false, String.class, NumberFilter.SINGLETON)),
		NUMBER(NVConfigManager.createNVConfig("number", "Local number", "Number", true, true, false, String.class, PhoneNumberFilter.SINGLETON)),
		EXTENSION(NVConfigManager.createNVConfig("extension", "Extension", "Extension", false, true, String.class)),
		
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
	
	public static final NVConfigEntity NVC_PHONE_DAO = new NVConfigEntityPortable(
            "phone_dao",
            null,
            "Phone",
            true,
            false,
            false,
            false,
            PhoneDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
	
	/**
	 * The default constructor.
	 */
	public PhoneDAO()
	{
		super(NVC_PHONE_DAO);
	}
	
	/**
	 * Returns the canonical ID.
	 */
	public String toCanonicalID()
	{
		return '+' + getCountryCode() + getAreaCode() + getNumber();
	}
	
	/**
	 * Returns the phone type.
	 * @return phone type
	 */
	public String getPhoneType() 
	{
		return lookupValue(Param.PHONE_TYPE);
	}
	
	/**
	 * Sets the phone type.
	 * @param type
	 */
	public void setPhoneType(String type) 
	{
		setValue(Param.PHONE_TYPE, type);
	}
	
	/**
	 * Returns the country code.
	 * @return country code
	 */
	public String getCountryCode() 
	{
		return lookupValue(Param.COUNTRY_CODE);
	}
	
	/**
	 * Sets the country code.
	 * @param code
	 */
	public void setCountryCode(String code) 
	{
		setValue(Param.COUNTRY_CODE, code);
	}
	
	/**
	 * Returns the area code.
	 * @return area code
	 */
	public String getAreaCode() 
	{
		return lookupValue(Param.AREA_CODE);
	}
	
	/**
	 * Sets the area code.
	 * @param area
	 */
	public void setAreaCode(String area) 
	{
		setValue(Param.AREA_CODE, area);
	}
	
	/**
	 * Returns the number.
	 * @return phone number
	 */
	public String getNumber() 
	{
		return lookupValue(Param.NUMBER);
	}
	
	/**
	 * Sets the number.
	 * @param number
	 */
	public void setNumber(String number) 
	{
		setValue(Param.NUMBER, number);
	}
	
	/**
	 * Returns the extension.
	 * @return extension
	 */
	public String getExtension() 
	{
		return lookupValue(Param.EXTENSION);
	}
	
	/**
	 * Sets the extension.
	 * @param ext
	 */
	public void setExtension(String ext) 
	{
		setValue(Param.EXTENSION, ext);
	}

}