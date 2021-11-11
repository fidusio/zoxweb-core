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
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;


/**
 * The AddressDAO class defines the address data access object used to create an address object.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class  AddressDAO
	extends SetNameDescriptionDAO {

	public enum Param
		implements GetNVConfig
	{
		STREET(NVConfigManager.createNVConfig("street", "Street address", "Street", true, true, String.class)),
		CITY(NVConfigManager.createNVConfig("city", "City", "City", true, true, String.class)),
		STATE_PROVINCE(NVConfigManager.createNVConfig("state_province", "State or province", "State/Province", false, true, String.class)),
		COUNTRY(NVConfigManager.createNVConfig("country", "Country", "Country", true, true, false, String.class, DataConst.COUNTRIES)),
		ZIP_POSTAL_CODE(NVConfigManager.createNVConfig("zip_postal_code", "ZIP/Postal code", "Zip/PostalCode", false, true, String.class)),
		
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
	
	public static final NVConfigEntity NVC_ADDRESS_DAO = new NVConfigEntityLocal(
            "address_dao",
            null ,
            "Address",
            true,
            false,
            false,
            false,
            AddressDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
	
	/**
	 * The default constructor creates an AddressDAO instance.
	 */
	public AddressDAO() {
		super(NVC_ADDRESS_DAO);
	}
	
	/**
	 * Returns the street.
	 * @return the street
	 */
	public String getStreet() {
		return lookupValue(Param.STREET);
	}
	
	/**
	 * Sets the street.
	 * @param street
	 */
	public void setStreet(String street) {
		setValue(Param.STREET, street);
	}
	
	/**
	 * Returns the city.
	 * @return the city
	 */
	public String getCity() {
		return lookupValue(Param.CITY);
	}
	
	/**
	 * Sets the city.
	 * @param city
	 */
	public void setCity(String city) {
		setValue(Param.CITY, city);
	}
	
	/**
	 * Returns the state or province.
	 * @return the province
	 */
	public String getStateOrProvince() {
		return lookupValue(Param.STATE_PROVINCE);
	}
	
	/**
	 * Sets the state or province.
	 * @param state
	 */
	public void setStateOrProvince(String state) {
		setValue(Param.STATE_PROVINCE, state);
	}
	
	/**
	 * Returns the country.
	 * @return the country
	 */
	public String getCountry() {
		return lookupValue(Param.COUNTRY);
	}
	
	/**
	 * Sets the country.
	 * @param country
	 */
	public void setCountry(String country) {
		setValue(Param.COUNTRY, country);
	}
	
	/**
	 * Returns the ZIP or postal code.
	 * @return the zip or postal code depending on the country
	 */
	public String getZIPOrPostalCode() {
		return lookupValue(Param.ZIP_POSTAL_CODE);
	}
	
	/**
	 * Sets the ZIP or postal code.
	 * @param code
	 */
	public void setZIPOrPostalCode(String code) {
		setValue(Param.ZIP_POSTAL_CODE, code);
	}
	
}