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

import org.zoxweb.shared.util.*;

/**
 * This class is used to define parameters used by devices 
 * which transmit messages. All message classes will extend
 * this class in inventory to obtain the applicable message parameters.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class DataDAO
    extends PropertyDAO
{
	

	private static final NVConfig NVC_DATA =  NVConfigManager.createNVConfig("data", "This is the raw message in byte array format.","Data",true, false, byte[].class);
	private static final NVConfig NVC_SOURCE_ID =  NVConfigManager.createNVConfig("source_id", "This is the source ID to identify the device within the system.","SourceID",true, false, String.class);
	private static final NVConfig NVC_FULL_NAME =  NVConfigManager.createNVConfig("full_name", "This is  the source full name.","FullName",true, false, String.class);
	
		
	
	public static final NVConfigEntity NVC_DATA_DAO = new NVConfigEntityPortable("data_dao", null , "DataDAO", true, false, false, false, DataDAO.class, SharedUtil.toNVConfigList(NVC_DATA, NVC_SOURCE_ID, NVC_FULL_NAME), null, false, PropertyDAO.NVC_PROPERTY_DAO);
	
	/**
	 * This constructor creates a MessageBase object
	 * based on a defined list and a set value for 
	 * the time stamp.
	 * @param nvce
	 */
	protected DataDAO(NVConfigEntity nvce) 
	{
		super(nvce);
		// TODO Auto-generated constructor stub
		setCreationTime(System.currentTimeMillis());
	}
	
	/**
	 * This is a default constructor that creates a
	 * MessageBase object.
	 */
	public DataDAO() 
	{
		super(NVC_DATA_DAO);
		setCreationTime(System.currentTimeMillis());
	}
	
	
	/**
	 * This method returns the system ID.
	 * @return the system id
	 */
	public String getSystemID() 
	{
		return getCanonicalID();
	}

	/**
	 * This method sets the system ID.
	 * @param systemID
	 */
	public void setSystemID(String systemID) 
	{
		setCanonicalID(systemID);
	}

	public void setData(String data)
	{
		if(data != null)
		{
			setData(SharedStringUtil.getBytes(data));
		}
		else
		{
			setData((byte[])null);
		}
	}
	
	/**
	 * This method sets the data.
	 * @param data
	 */
	public void setData(byte[] data)
	{
		setValue(NVC_DATA, data);
	}
	
	/**
	 * This method returns the data.
	 * @return the data as byte array
	 */
	public byte[] getData()
	{
		return lookupValue(NVC_DATA);
	}
	
	/**
	 * This method sets the source ID.
	 * @param sourceID
	 */
	public void setSourceID(String sourceID)
	{
		setValue(NVC_SOURCE_ID, sourceID);
	}
	
	/**
	 * This method returns the source ID, the source id the uuid of a the device that have generated the message.
	 * <br> The list of possible device type that can generate such message are:
	 * <ul>
	 * <li> GPS module.
	 * <li> Temperature sensor.
	 * <li> Accelerometer module.
	 * <li> Gyroscope sensor.
	 * <li> or any device that can be integrated or supported by rubus pi project.
	 * </ul>
	 * @return the source id 
	 */
	public String getSourceID()
	{
		return lookupValue(NVC_SOURCE_ID);
	}


	/**
	 * This method sets the source ID.
	 * @param fullname of the data source
	 */
	public void setFullName(String fullname)
	{
		setValue(NVC_FULL_NAME, fullname);
	}

	/**
	 * Get the fullname of the data source
	 * @return the data source full name
	 */
	public String getFullName()
	{
		return lookupValue(NVC_FULL_NAME);
	}

}
