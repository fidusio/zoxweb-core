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

import org.zoxweb.shared.data.DataConst.DataParam;


import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;

import org.zoxweb.shared.util.SetName;
import org.zoxweb.shared.util.SharedUtil;

//import javax.persistence.Column;
//import javax.persistence.MappedSuperclass;

/**
 * This class defines set name data access object.
 * @author mzebib
 *
 */
//@MappedSuperclass
@SuppressWarnings("serial")
public abstract class SetNameDAO
	extends ReferenceIDDAO
	implements SetName
{

	public static final NVConfigEntity NVC_NAME_DAO = new NVConfigEntityLocal(
            null,
            null ,
            null,
            true,
            false,
            false,
            false,
            SetNameDAO.class,
            SharedUtil.toNVConfigList(DataParam.NAME.getNVConfig()),
            null,
            false,
            ReferenceIDDAO.NVC_REFERENCE_ID_DAO
    );

	protected SetNameDAO(NVConfigEntity nvce)
    {
		super(nvce);
	}
	
	/**
	 * Returns the name.
	 * @return name
	 */
	//@Column(name = "name")
	@Override
	public String getName()
    {
		return lookupValue(DataParam.NAME);
	}

	/**
	 * Sets the name.
	 * @param name
	 */
	@Override
	public void setName(String name) 
        throws NullPointerException, IllegalArgumentException
    {
		setValue(DataParam.NAME, name);
	}

}