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
package org.zoxweb.shared.security;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * This class defines the user id data access object used to create
 * user id for access.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class SubjectIdentifier
	extends PropertyDAO
	implements SubjectID<String>
{

	/**
	 * This enum includes the following parameters:
	 * primary email and user information.
	 */
	public enum Param
		implements GetNVConfig
	{
		SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject identifier", "SubjectID", true, true, true, String.class, CryptoConst.SubjectIDFilter.SINGLETON)),
		SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "Subject Type", "SubjectType", true, true, SubjectType.class)),
		SUBJECT_STATUS(NVConfigManager.createNVConfig("subject_status", "Subject status", "SubjectStatus", true, true, CryptoConst.SubjectStatus.class)),

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

	public static final NVConfigEntity NVC_SUBJECT_IDENTIFIER = new NVConfigEntityLocal(
	        "subject_identifier",
            null ,
            SubjectIdentifier.class.getSimpleName(),
            true,
            false,
            false,
            false,
            SubjectIdentifier.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );


	/**
	 * The default constructor.
	 */
	public SubjectIdentifier()
	{
	    super(NVC_SUBJECT_IDENTIFIER);
	}


	

	
	/**
	 * Returns the user ID.
	 * @return user id
	 */
	@Override
	public String getUserID()
	{
	    return getReferenceID();
	}


	@Override
	public String getSubjectID() {
		// TODO Auto-generated method stub
		return lookupValue(Param.SUBJECT_ID);
	}

	public String getSubjectGID()
	{
		return getGlobalID();
	}


	@Override
	public void setSubjectID(String id) {
		setValue(Param.SUBJECT_ID, id);
	}

	public CryptoConst.SubjectStatus getSubjectStatus()
	{
		return lookupValue(Param.SUBJECT_STATUS);
	}

	public void setSubjectStatus(CryptoConst.SubjectStatus subjectStatus)
	{
		setValue(Param.SUBJECT_STATUS, subjectStatus);
	}

	public SubjectType getSubjectType()
	{
		return lookupValue(Param.SUBJECT_TYPE);
	}

	public void setSubjectType(SubjectType type)
	{
		setValue(Param.SUBJECT_TYPE, type);
	}


	public NVGenericMap getCredential()
	{
		return getProperties();
	}

	
}