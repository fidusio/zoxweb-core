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
package org.zoxweb.shared.net;

import org.zoxweb.shared.net.InetProp.NICategory;
import org.zoxweb.shared.net.InetProp.NIStatus;
import org.zoxweb.shared.net.InetProp.NIType;
import org.zoxweb.shared.util.SUS;

public class ConnectionPropDAO
{

	private int bandwidthCapacity = 1;
	private String name;	
	private NICategory category;
	private NIType type;
	private String niName;
	private int routeID = 0;
	private NIStatus status = NIStatus.OK;

	public ConnectionPropDAO()
    {

	}

	public ConnectionPropDAO(String niName,
							   String name,
							   NIType niType,
							   NICategory niCategory,
							   int band) {
		setNIName( niName);
		setName(name);
		setType(niType);
		setCategory(niCategory);
		setBandwidthCapacity(band);
	}

    /**
     * Returns the network name.
     * @return the network name
     */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the network interface name.
	 * @param name
	 */
	public void setName(String name) {
		SUS.checkIfNulls("Can't have a null name",name);
		this.name = name;
	}
	
	/**
	 * @return the network interface category
	 */
	public NICategory getCategory() {
		return category;
	}

	/**
	 * Set the network interface category
	 * @param category
	 */
	public void setCategory(NICategory category) {
		SUS.checkIfNulls("Can't have a null category",category);
		this.category = category;
	}

	/**
	 * @return the network interface type
	 */
	public NIType getType() {
		return type;
	}

	/**
	 * Set the network interface type
	 * @param type
	 */
	public void setType(NIType type) {
		SUS.checkIfNulls("Can have a null type",type);
		this.type = type;
	}
	
	/**
	 * Change the network properties status
	 * @param status
	 * @exception NullPointerException if status is null
	 */
	protected void changeStatus(NIStatus status)
		throws NullPointerException {
		SUS.checkIfNulls("Can't change null to null status", status);
		this.status = status;
	}

	/**
	 * Get the current assigned status
	 * @return the network interface status
	 */
	public NIStatus currentStatus() {
		return status;
	}
	
	/**
	 * @return the network interface set by the os name
	 */
	public String getNIName() {
		return niName;
	}

	/**
	 * Set the network interface name
	 * @param niName
	 */
	public void setNIName(String niName) {
		this.niName = niName;
	}

	public String toString() {
		return niName + "," + name + "," + type + "," + category +"," + currentStatus();
	}
	
	/**
	 * @return the bandwidth capacity associated with the current connection
	 */
	public int getBandwidthCapacity() {
		return bandwidthCapacity;
	}

	/**
	 * Set the connection bandwidth capacity index: 1 is the lowest, the higher is the number 
	 * the more session will be assigned to the current connection
	 * @param connectionCapacity
	 * @exception IllegalArgumentException if the connection capacity is set less than 1
	 */
	public void setBandwidthCapacity(int connectionCapacity) {

		if (connectionCapacity < 1) {
			throw new IllegalArgumentException("Connection capaticy can't be < 1" + connectionCapacity);
		}

		this.bandwidthCapacity = connectionCapacity;
	}
	
	/** 
	 * @return routeID that will be assigned with current connection, if the initial route id was set 
	 * to zero the NIPropManager will assign the routeID automatically
	 */
	public int getRouteID() {
		return routeID;
	}

	/**
	 * Set the routeID of the current connection
	 * @param maskMark
	 */
	public void setRouteID(int maskMark) {
		if (maskMark < 0) {
			throw new IllegalArgumentException("Connection mask must be > 0 " + maskMark);
		}

		this.routeID = maskMark;
	}

}