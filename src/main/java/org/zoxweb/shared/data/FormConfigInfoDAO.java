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

import java.util.List;

import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.DomainID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class FormConfigInfoDAO 
	extends CanonicalIDDAO
	implements DomainID<String>
{
	public enum Param
		implements GetNVConfig
	{
		NVC_NAME(NVConfigManager.createNVConfig("name", null,"Name", true, true, String.class)),
		CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "Canonical ID", "CanonicalID", true, false, true, String.class, null)),
		FORM_CANONICAL_ID(NVConfigManager.createNVConfig("form_canonical_id", "Form class name", "FormCanonicalID", true, true, String.class)),
		DOCUMENT_STORE_REF_ID(NVConfigManager.createNVConfig("document_store_ref_id", "Where to store files associated with the form", "DocumentStoreRefID", false, true, String.class)),
		DEFAULT_LOCATION(NVConfigManager.createNVConfig("default_location", "Where to put the form", "DefaultLocation", false, true, String.class)),
		DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "The domain url identifier", "Domain/AccountID", false, true, false, String.class, null)),
		DEFAULT_VALUES(NVConfigManager.createNVConfig("default_values", "The default values", "DefaultValues", false, true, true, String[].class, null)),
		
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
	
	public static final NVConfigEntity NVC_FORM_CONFIG_INFO_DAO = new NVConfigEntityPortable
																(
																	"form_config_info_dao", 
																	null, 
																	"FormConfig", 
																	true, 
																	false, 
																	false, 
																	false, 
																	FormConfigInfoDAO.class, 
																	SharedUtil.extractNVConfigs(Param.values()),
																	null, 
																	false, 
																	CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																);

	
	public FormConfigInfoDAO()
	{
		super(NVC_FORM_CONFIG_INFO_DAO);
	}
	
	
	@Override
	public String getDomainID()
	{
		return lookupValue(Param.DOMAIN_ID);
	}

	@Override
	public void setDomainID(String domainID)
	{
		setValue(Param.DOMAIN_ID, domainID);
	}
	

	public String getFormCanonicalID() 
	{
		return lookupValue(Param.FORM_CANONICAL_ID);
	}


	public void setFormCanonicalID(String formClassName)
	{
		setValue(Param.FORM_CANONICAL_ID, formClassName);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getDefaultParameters()
	{
		return (ArrayValues<NVPair>) lookup(Param.DEFAULT_VALUES);
	}
	
	public void setDefaultParameters(ArrayValues<NVPair> values)
	{
		getDefaultParameters().add(values.values(), true);
	}
	
	public void setDefaultParameters(List<NVPair> values)
	{
		getDefaultParameters().add(values.toArray(new NVPair[0]), true);
	}
	
	public String getDocumentStoreRefID()
	{
		return lookupValue(Param.DOCUMENT_STORE_REF_ID);
	}
	
	public void  setDocumentStoreRefID(String documentStoreRefID)
	{
		setValue(Param.DOCUMENT_STORE_REF_ID, documentStoreRefID);
	}
	
	public String getDefaultLocation()
	{
		return lookupValue(Param.DEFAULT_LOCATION);
	}
	
	public void  setDefaultLocation(String defaulLocation)
	{
		setValue(Param.DEFAULT_LOCATION, defaulLocation);
	}
	
}