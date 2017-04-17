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
import org.zoxweb.shared.util.DomainID;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * 
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class DomainInfoDAO
	extends SetNameDescriptionDAO
	implements DomainID<String>
{
	
	public static final NVConfig NVC_DOMAIN_ID =  NVConfigManager.createNVConfig("domain_id", "The domain url identifier", "Domain/AccountID", true, true, true, String.class, FilterType.DOMAIN);
	public static final NVConfigEntity NVC_DOMAIN_INFO_DAO = new NVConfigEntityLocal("domain_info_dao", "DomainInfoDAO Object" , "DomainInfoDAO", true, false, false, false, DomainInfoDAO.class, SharedUtil.toNVConfigList(NVC_DOMAIN_ID), null, false, SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);
	
	/**
	 * The default constructor.
	 */
	public DomainInfoDAO()
	{
		super(NVC_DOMAIN_INFO_DAO);
	}
	
	/**
	 * 
	 * @param nvce
	 */
	protected DomainInfoDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}

	/**
	 * Returns the domain ID;
	 * @return the domain id
	 */
	@Override
	public String getDomainID()
	{
		return lookupValue(NVC_DOMAIN_ID);
	}

	/**
	 * Sets the domain ID.
	 * @param domainID
	 */
	@Override
	public void setDomainID(String domainID)
	{
		setValue(NVC_DOMAIN_ID, domainID);
	}

}