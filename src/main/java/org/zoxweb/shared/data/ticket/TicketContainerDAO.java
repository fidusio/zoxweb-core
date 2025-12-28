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

import org.zoxweb.shared.accounting.BillingItemsContainerDAO;
import org.zoxweb.shared.data.NVEntityContainerDAO;
import org.zoxweb.shared.data.ResolutionStatus;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 *
 */
@SuppressWarnings("serial")
public class TicketContainerDAO
	extends NVEntityContainerDAO
{

	public enum Param
		implements GetNVConfig
	{
		STATUS(NVConfigManager.createNVConfig("status", "The status of the ticket.", "Status", false, true, ResolutionStatus.class)),
		ISSUER(NVConfigManager.createNVConfigEntity("issuer", "The ticket issuer.", "Issuer", true, true, TicketIssuerDAO.NVC_TICKET_ISSUER_DAO, ArrayType.NOT_ARRAY)),
		BILLING_ITEMS_CONTAINER(NVConfigManager.createNVConfigEntity("billing_items_container", "The billing items container.", "BillingItemsContainer", false, true, BillingItemsContainerDAO.class, ArrayType.NOT_ARRAY)),

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
	
	public static final NVConfigEntity NVC_TICKET_CONTAINER_DAO = new NVConfigEntityPortable
																						(
																							"ticket_container_dao", 
																							null, 
																							"Ticket", 
																							true, 
																							false, 
																							false, 
																							false, 
																							TicketContainerDAO.class, 
																							SharedUtil.extractNVConfigs(Param.values()),
																							null, 
																							false, 
																							NVEntityContainerDAO.NVC_NVENTITY_CONTAINER_DAO
																						);

	/**
	 * The default constructor.
	 */
	public TicketContainerDAO()
	{
		super(NVC_TICKET_CONTAINER_DAO);
	}
	
	/**
	 * This constructor instantiates TicketContainerDAO object based on given NVConfigEntity.
	 * @param nvce
	 */
	protected TicketContainerDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}

	@Override
	public String getName()
	{
		return getIssuerInfo().getName();
	}

	@Override
	public void setName(String name)
	{
		getIssuerInfo().setName(name);
	}
	
	public ResolutionStatus getStatus()
	{
		return lookupValue(Param.STATUS);
	}
	
	public void setStatus(ResolutionStatus status)
	{
		setValue(Param.STATUS, status);
	}

	public TicketIssuerDAO getIssuerInfo() 
	{
		return lookupValue(Param.ISSUER);
	}
	
	public void setIssuerInfo(TicketIssuerDAO issuer) 
	{
		if (issuer != null)
		{
			issuer.setCanonicalID(getCanonicalID());
		}
		
		setValue(Param.ISSUER, issuer);
	}

	
	public BillingItemsContainerDAO getBillingItemsContainerDAO() 
	{
		return lookupValue(Param.BILLING_ITEMS_CONTAINER);
	}

	public void setBillingItemsContainerDAO(BillingItemsContainerDAO container) 
	{
		if (container != null)
		{
			container.setCanonicalID(getCanonicalID());
		}
		
		setValue(Param.BILLING_ITEMS_CONTAINER, container);
	}
	
	/**
	 * Adds TicketResolutionDAO to container.
	 * A TicketContainerDAO object can contain multiple TicketResolution objects.
	 * @param ticketResolutionToAdd
	 * @return TicketResolutionDAO
	 */
	public synchronized TicketResolutionDAO addTicketResolutionDAO(TicketResolutionDAO ticketResolutionToAdd)
	{
		if (ticketResolutionToAdd != null && contains(ticketResolutionToAdd.getReferenceID()) == null)
		{
			ticketResolutionToAdd.setCanonicalID(getCanonicalID());
			
			return (TicketResolutionDAO) getContent().add(ticketResolutionToAdd);
		}
		
		return null;
	}
	
	/**
	 * Removes TicketResolutionDAO from container.
	 * @param ticketResolutionToRemove
	 * @return TicketResolutionDAO
	 */
	public synchronized TicketResolutionDAO removeTicketResolutionDAO(TicketResolutionDAO ticketResolutionToRemove)
	{
		if (ticketResolutionToRemove != null)
		{
			return (TicketResolutionDAO) getContent().remove(ticketResolutionToRemove);
		}
		
		return null;
	}
	
}