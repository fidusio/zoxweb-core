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

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * This class defines the user id data access object used to create
 * user id for access.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class SubjectIDDAO
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
		SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject identifier", "SubjectID", true, true, true, String.class, SecurityConsts.SubjectIDFilter.SINGLETON)),
		SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "Subject Type", "SubjectType", true, true, SubjectType.class)),


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

	public static final NVConfigEntity NVC_USER_ID_DAO = new NVConfigEntityLocal(
	        "subject_id_dao",
            null ,
            SubjectIDDAO.class.getSimpleName(),
            true,
            false,
            false,
            false,
            SubjectIDDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );


	/**
	 * The default constructor.
	 */
	public SubjectIDDAO()
	{
	    super(NVC_USER_ID_DAO);
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


	@Override
	public void setSubjectID(String id) {
		setValue(Param.SUBJECT_ID, id);
	}

	public SubjectType getSubjectType()
	{
		return lookupValue(Param.SUBJECT_TYPE);
	}

	public void setSubjectType(SubjectType type)
	{
		setValue(Param.SUBJECT_TYPE, type);
	}

	
}