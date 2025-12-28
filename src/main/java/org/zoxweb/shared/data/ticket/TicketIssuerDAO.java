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
package org.zoxweb.shared.data.ticket;

import java.util.Date;

import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.CanonicalIDDAO;
import org.zoxweb.shared.data.PhoneDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.EmailID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * The TicketIssuerDAO class creates a ticket issue object based on ticket issuer's name, contact information, and incident details including
 * location, summary, and detailed description.
 */
@SuppressWarnings("serial")
public class TicketIssuerDAO
	extends CanonicalIDDAO
	implements EmailID
{

	public enum Param
       implements GetNVConfig
	{
		FIRST_NAME(NVConfigManager.createNVConfig("first_name", "The issuer's first name.", "First Name", false, true, String.class)),
		LAST_NAME(NVConfigManager.createNVConfig("last_name", "The issuer's last name.", "Last Name", false, true, String.class)),
		EMAIL(NVConfigManager.createNVConfig("email", "The issuer's email address.", "Email", true, true, false, String.class, FilterType.EMAIL)),
		PHONE_NUMBER(NVConfigManager.createNVConfigEntity("phone_number", "The issuer's phone number.", "Phone", false, true, PhoneDAO.class, ArrayType.NOT_ARRAY)),
		POSITION(NVConfigManager.createNVConfig("position", "The issuer's position at associated company.", "Position", false, true, String.class)),
		
		COMPANY(NVConfigManager.createNVConfig("company", "Name of associated company.", "Company", false, true, String.class)),
		INCIDENT_LOCATION(NVConfigManager.createNVConfig("incident_location", "The location of the incident.", "Incident Location", true, true, String.class)),
		INCIDENT_ADDRESS(NVConfigManager.createNVConfigEntity("incident_address", "The address of the incident.", "Address", false, true, AddressDAO.class, ArrayType.NOT_ARRAY)),		
		DUE_DATE(NVConfigManager.createNVConfig("due_date", "Date when the issue should be completed.", "DueDate", false, false, Date.class)),
	
		ASSIGNEE(NVConfigManager.createNVConfig("assignee", "The assignee's email address.", "Assignee", true, true, false, String.class, FilterType.EMAIL)),
		
		NAME(NVConfigManager.createNVConfig("name", "Summary of incident.", "Name", true, true, String.class)),
		DESCRIPTION(NVConfigManager.createNVConfig("description", "Detailed description of incident.", "Description", true, true, String.class)),
		
		;
		
		private final NVConfig cType;

        Param(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}
	
	public static final NVConfigEntity NVC_TICKET_ISSUER_DAO = new NVConfigEntityPortable
																					(
																						"ticket_issuer_dao", 
																						null, 
																						"Ticket Issuer", 
																						true, 
																						false, 
																						false, 
																						false, 
																						TicketIssuerDAO.class, 
																						SharedUtil.extractNVConfigs(Param.values()),
																						null, 
																						false, 
																						CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																					);
	
	/**
	 * The default constructor.
	 */
	public TicketIssuerDAO()
	{
		super(NVC_TICKET_ISSUER_DAO);
	}
	
	/**
	 * This constructor instantiates TicketIssuerDAO object based on given NVConfigEntity.
	 * @param nvce
	 */
	protected TicketIssuerDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}

	/**
	 * Returns the first name.
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
	 * Returns the last name.
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
	 * Returns the email address.
	 * @return email id
	 */
	public String getEmailID() 
	{
		return lookupValue(Param.EMAIL);
	}
	
	/**
	 * Sets the email address.
	 * @param email
	 */
	public void setEmailID(String email) 
	{
		setValue(Param.EMAIL, email);
	}
	
	/**
	 * Returns the phone number.
	 * @return phone number
	 */
	public PhoneDAO getPhoneNumber() 
	{
		return lookupValue(Param.PHONE_NUMBER);
	}
	
	/**
	 * Sets the phone number.
	 * @param phone
	 */
	public void setPhoneNumber(PhoneDAO phone) 
	{
		setValue(Param.PHONE_NUMBER, phone);
	}
	
	/**
	 * Returns the issuer's position at associated company.
	 * @return position
	 */
	public String getPosition() 
	{
		return lookupValue(Param.POSITION);
	}
	
	/**
	 * Sets the issuer's position at associated company.
	 * @param position
	 */
	public void setPosition(String position) 
	{
		setValue(Param.POSITION, position);
	}
	
	/**
	 * Returns the company.
	 * @return company
	 */
	public String getCompany() 
	{
		return lookupValue(Param.COMPANY);
	}
	
	/**
	 * Sets the company.
	 * @param company
	 */
	public void setCompany(String company) 
	{
		setValue(Param.COMPANY, company);
	}
	
	/**
	 * Returns the incident location.
	 * @return incident location.
	 */ 
	public String getIncidentLocation() 
	{
		return lookupValue(Param.INCIDENT_LOCATION);
	}
	
	/**
	 * Sets the incident location.
	 * @param location
	 */
	public void setIncidentLocation(String location) 
	{
		setValue(Param.INCIDENT_LOCATION, location);
	}
	
	/**
	 * Returns the incident address.
	 * @return  incident address.
	 */
	public AddressDAO getIncidentAddress() 
	{
		return lookupValue(Param.INCIDENT_ADDRESS);
	}
	
	/**
	 * Sets the incident address.
	 * @param address
	 */
	public void setIncidentAddress(AddressDAO address) 
	{
		setValue(Param.INCIDENT_ADDRESS, address);
	}
	
	/**
	 * Returns the due date.
	 * @return due date.
	 */
	public long getDueDate() 
	{
		return lookupValue(Param.DUE_DATE);
	}
	
	/**
	 * Sets the due date.
	 * @param date
	 */
	public void setDueDate(long date) 
	{
		setValue(Param.DUE_DATE, date);
	}
	
	/**
	 * Returns the assignee.
	 * @return assignee.
	 */
	public String getAssignee() 
	{
		return lookupValue(Param.ASSIGNEE);
	}
	
	/**
	 * Sets the assignee.
	 * @param assignee
	 */
	public void setAssignee(String assignee) 
	{
		setValue(Param.ASSIGNEE, assignee);
	}
	
	/**
	 * Returns the summary of issue.
	 */
    @Override
	public String getName() 
	{
		return lookupValue(Param.NAME);
	}
	
	/**
	 * Sets summary of issue.
	 * @param name
	 */
	@Override
	public void setName(String name) 
	{
		setValue(Param.NAME, name);
	}
	
	/**
	 * Returns the description of issue.
	 */
    @Override
	public String getDescription() 
	{
		return lookupValue(Param.DESCRIPTION);
	}
	
	/**
	 * Sets the description of issue.
	 * @param description
	 */
    @Override
	public void setDescription(String description) 
	{
		setValue(Param.DESCRIPTION, description);
	}
	
}