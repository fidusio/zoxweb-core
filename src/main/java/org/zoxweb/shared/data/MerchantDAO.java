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

import org.zoxweb.shared.accounting.PaymentInfoDAO;
import org.zoxweb.shared.data.DataConst.DataParam;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@SuppressWarnings("serial")
public class MerchantDAO 
	extends SetNameDescriptionDAO
	implements AccountID<String>
{
	
	public enum Param
		implements GetNVConfig
	{
		ACCOUNT_ID(NVConfigManager.createNVConfig(MetaToken.ACCOUNT_ID.getName(), "The account id","AccountID", true, false, false, true, true, String.class, null)),
		LIST_OF_DOMAIN_IDS(NVConfigManager.createNVConfigEntity("domain_ids", "Domain ID", "DomainIDs", true, true, DomainInfoDAO.NVC_DOMAIN_INFO_DAO, ArrayType.LIST)),
		COMPANY_TYPE(NVConfigManager.createNVConfig("company_type", "Type of company", "CompanyType", false, true, String.class)),
		LIST_OF_ADDRESSES(NVConfigManager.createNVConfigEntity("addresses", "List of addresses", "Addresses", false, true, AddressDAO.NVC_ADDRESS_DAO, ArrayType.LIST)),
		LIST_OF_PHONES(NVConfigManager.createNVConfigEntity("phones", "List of phones", "ListOfPhones", false, true, PhoneDAO.NVC_PHONE_DAO, ArrayType.LIST)),
		LIST_OF_DOMAIN_EMAILS(NVConfigManager.createNVConfig("domain_emails", "List of domain emails", "DomainEmails", false, true, String[].class)),
		LIST_OF_PAYMENT_INFOS(NVConfigManager.createNVConfigEntity("payment_infos", "List of payment info", "PaymentInfos", false, true, PaymentInfoDAO.NVC_PAYMENT_INFO_DAO, ArrayType.LIST)),
		ADDITIONAL_INFOS(NVConfigManager.createNVConfig("additional_info", "Additional information", "AdditionalInfos", false, true, String[].class)),
		
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
	
	public static final NVConfigEntity NVC_MERCHANT_DAO = new NVConfigEntityPortable(
            "merchant_dao",
            null,
            "MerchantDAO",
            true,
            false,
            false,
            false,
            MerchantDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            true,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
	
	/**
	 * The default constructor.
	 */
	public MerchantDAO()
	{
		super(NVC_MERCHANT_DAO);
	}
	
	/**
	 * Returns the domain ID.
	 * @return list of domain ids
	 */
	public List<DomainID<String>> getDomainIDs() 
	{
		return lookupValue(Param.LIST_OF_DOMAIN_IDS);
	}
	
	/**
	 * Sets the domain ID.
	 * @param domainIDs
	 */
	public void setDomainID(List<DomainID<String>> domainIDs)
	{
		setValue(Param.LIST_OF_DOMAIN_IDS, domainIDs);
	}
	
	/**
	 * Returns the company type.
	 * @return company type
	 */
	public String getCompanyType() 
	{
		return lookupValue(Param.COMPANY_TYPE);
	}
	
	/**
	 * Sets the company type.
	 * @param type
	 */
	public void setCompanyType(String type)
	{
		setValue(Param.COMPANY_TYPE, type);
	}
	
	/**
	 * Returns list of addresses.
	 * @return list of addresses
	 */
	public ArrayList<AddressDAO> getListOfAddresses() 
	{
		return lookupValue(Param.LIST_OF_ADDRESSES);
	}
	
	/**
	 * Sets list of addresses.
	 * @param list
	 */
	public void setListOfAddresses(ArrayList<AddressDAO> list) 
	{
		setValue(Param.LIST_OF_ADDRESSES, list);
	}
	
	/**
	 * Returns list of phones.
	 * @return list of phones
	 */
	public ArrayList<PhoneDAO> getListOfPhones() 
	{
		return lookupValue(Param.LIST_OF_PHONES);
	}
	
	/**
	 * Sets list of phones.
	 * @param list
	 */
	public void setListOfPhones(ArrayList<PhoneDAO> list) 
	{
		setValue(Param.LIST_OF_PHONES, list);
	}
	
	/**
	 * Returns list of domain emails.
	 * @return list of emails 
	 */
	public ArrayList<NVPair> getListOfDomainEmails() 
	{
		return lookupValue(Param.LIST_OF_DOMAIN_EMAILS);
	}
	
	/**
	 * Sets list of domain emails.
	 * @param list
	 */
	public void setListOfDomainEmails(ArrayList<NVPair> list) 
	{
		setValue(Param.LIST_OF_DOMAIN_EMAILS, list);
	}
	
	/**
	 * Returns list of payment information.
	 * @return list of payment infos
	 */
	public ArrayList<PaymentInfoDAO> getListOfPaymentInfos() 
	{
		return lookupValue(Param.LIST_OF_PAYMENT_INFOS);
	}
	
	/**
	 * Sets list of payment information.
	 * @param list
	 */
	public void setListOfPaymentInfos(ArrayList<PaymentInfoDAO> list) 
	{
		setValue(Param.LIST_OF_PAYMENT_INFOS, list);
	}
	
	/**
	 * Returns additional information.
	 * @return list of additional info
	 */
	public ArrayList<NVPair> getAdditonalInfos() 
	{
		return lookupValue(Param.ADDITIONAL_INFOS);
	}
	
	/**
	 * Sets additional information.
	 * @param info
	 */
	public void setAdditionalInfos(ArrayList<NVPair> info)
	{
		setValue(Param.ADDITIONAL_INFOS, info);
	}
	
	/**
	 * Sets the name.
	 * @param name
	 */
	public void setName(String name) 
			throws NullPointerException, IllegalArgumentException
	{
		SUS.checkIfNulls("Name cannot be empty or null.", SUS.trimOrNull(name));
		setValue(DataParam.NAME, name);
	}


	@Override
	public String getAccountID()
	{
		return lookupValue(Param.ACCOUNT_ID);
	}

	/**
	 * Sets the account ID.
	 * @param accountID
	 */
	@Override
	public void setAccountID(String accountID)
	{
		setValue(Param.ACCOUNT_ID, accountID);
	}
}