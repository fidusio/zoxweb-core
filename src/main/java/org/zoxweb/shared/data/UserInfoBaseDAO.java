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

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public abstract class UserInfoBaseDAO 
	extends SetNameDescriptionDAO
	implements GetNVProperties,
		       SetCanonicalID
{
	
	public enum Param
		implements GetNVConfig
	{
		TITLE(NVConfigManager.createNVConfig("title", "Personal name title", "Title", false, true, false, String.class, DataConst.PERSONAL_TITLES)),
		FIRST_NAME(NVConfigManager.createNVConfig("first_name", "First name", "FirstName", false, true, String.class)),
		MIDDLE_NAME(NVConfigManager.createNVConfig("middle_name", "Middle name", "MiddleName", false, true, String.class)),
		LAST_NAME(NVConfigManager.createNVConfig("last_name", "Last name", "LastName", false, true, String.class)),
		DOB(NVConfigManager.createNVConfig("date_of_birth", "Date of birth", "DateOfBirth", true, true, Date.class)),
		LIST_OF_ADDRESSES(NVConfigManager.createNVConfigEntity("list_of_addresses", "List of addresses", "ListOfAddresses", false, true, AddressDAO.NVC_ADDRESS_DAO, ArrayType.LIST)),
		LIST_OF_PHONES(NVConfigManager.createNVConfigEntity("list_of_phones", "List of phones", "ListOfPhones", false, true, PhoneDAO.NVC_PHONE_DAO, ArrayType.LIST)),
		LIST_OF_EMAILS(NVConfigManager.createNVConfig("list_of_emails", "List of emails", "ListOfEmails", false, true, false, String[].class, FilterType.EMAIL)),
		CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "CanonicalID map", "CanonicalID", true, false, String.class)),
		PROPERTIES(NVConfigManager.createNVConfig("additional_info", "Additional information", "AdditionalInfo", false, true, NVGenericMap.class)),
		
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

	public static final NVConfigEntity NVC_USER_INFO_BASE_DAO = new NVConfigEntityPortable(
																							"user_info_base_dao", 
																							null, 
																							"UserInfoBaseDAO",
																							true, 
																							false, 
																							false, 
																							false, 
																							UserInfoBaseDAO.class, 
																							SharedUtil.extractNVConfigs(Param.values()),
																							null, 
																							false, 
																							SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																					 	);
	
	/**
	 * 
	 * @param nvce
	 */
	protected UserInfoBaseDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Gets the personal title.
	 * @return title 
	 */
	public String getTitle() 
	{
		return lookupValue(Param.TITLE);
	}
	
	/**
	 * Sets the personal title.
	 * @param title
	 */
	public void setTitle(String title) 
	{
		setValue(Param.TITLE, title);
	}
	
	/**
	 * Gets the first name.
	 * @return first name
	 */
	public String getFirstName() 
	{
		return lookupValue(Param.FIRST_NAME);
	}
	
	/**
	 * Sets the first name.
	 * @param firstName
	 */
	public void setFirstName(String firstName) 
	{
		setValue(Param.FIRST_NAME, firstName);
	}
	
	/**
	 * Gets the middle name.
	 * @return middle name
	 */
	public String getMiddleName() 
	{
		return lookupValue(Param.MIDDLE_NAME);
	}
	
	/**
	 * Sets the middle name.
	 * @param middleName
	 */
	public void setMiddleName(String middleName) 
	{
		setValue(Param.MIDDLE_NAME, middleName);
	}
	
	/**
	 * Gets the last name.
	 * @return last name
	 */
	public String getLastName() 
	{
		return lookupValue(Param.LAST_NAME);
	}
	
	/**
	 * Sets the last name.
	 * @param lastName
	 */
	public void setLastName(String lastName) 
	{
		setValue(Param.LAST_NAME, lastName);
	}
	
	/**
	 * Gets the date of birth.
	 * @return date of birth
	 */
	public long getDOB() 
	{
		return lookupValue(Param.DOB);
	}
	
	/**
	 * Sets the date of birth.
	 * @param dob
	 */
	public void setDOB(long dob) 
	{
		setValue(Param.DOB, dob);
	}
	
	/**
	 * Gets the list of addresses.
	 * @return list of addresses
	 */
	public List<AddressDAO> getListOfAddresses() 
	{
		return lookupValue(Param.LIST_OF_ADDRESSES);
	}

	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getAddressesAsArrayValues() {
	    return lookup(Param.LIST_OF_ADDRESSES);
    }
	
	/**
	 * Sets the list of addresses.
	 * @param list
	 */
	public void setListOfAddresses(List<AddressDAO> list) 
	{
		setValue(Param.LIST_OF_ADDRESSES, list);
	}
	
	/**
	 * Gets the list of phones.
	 * @return list of phones
	 */
	public List<PhoneDAO> getListOfPhones() 
	{
		return lookupValue(Param.LIST_OF_PHONES);
	}

    @SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getPhonesAsArrayValues()
    {
        return (ArrayValues<NVEntity>) lookup(Param.LIST_OF_PHONES);
    }

	/**
	 * Sets the list of phones.
	 * @param list
	 */
	public void setListOfPhones(List<PhoneDAO> list)
	{
		setValue(Param.LIST_OF_PHONES, list);
	}

	
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getEmails()
	{
		return (ArrayValues<NVPair>) lookup(Param.LIST_OF_EMAILS);
	}
	
	
	/**
	 * Gets the list of emails.
	 * @return list of emails
	 */
	public List<NVPair> getListOfEmails() 
	{
		return lookupValue(Param.LIST_OF_EMAILS);
	}
	
	/**
	 * Sets the list of emails.
	 * @param list
	 */
	public void setListOfEmails(List<NVPair> list)
	{
		setValue(Param.LIST_OF_EMAILS, list);
	}


	public NVGenericMap getProperties()
	{
		return (NVGenericMap) lookup(Param.PROPERTIES);
	}

	public String toCanonicalID()
	{
		return getCanonicalID();
	}

	public String getCanonicalID()
	{
		return lookupValue(Param.CANONICAL_ID);
	}

	public void setCanonicalID(String canonicalID)
	{
		setValue(Param.CANONICAL_ID, canonicalID);
	}
}