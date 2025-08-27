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
package org.zoxweb.shared.accounting;

import java.math.BigDecimal;
import java.util.List;

import org.zoxweb.shared.data.CanonicalIDDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

@SuppressWarnings("serial")
public class BillingItemsContainerDAO
	extends CanonicalIDDAO
{

	public enum Params
		implements GetNVConfig
	{
		
		STATUS(NVConfigManager.createNVConfig("status", "Billing status", "Status", false, true, String.class)),
		BILLING_ITEMS(NVConfigManager.createNVConfigEntity("billing_items", "List of BillingItemDAO objects.", "BillingItems", false, true, BillingItemDAO[].class, ArrayType.LIST)),
		TOTAL(NVConfigManager.createNVConfig("total", "The sum of billable items unit.", "Total", false, true, BigDecimal.class)),
		
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}
	
	public static final NVConfigEntity NVC_BILLING_ITEMS_CONTAINER_DAO = new NVConfigEntityPortable(
																									"billing_items_container_dao", 
																									null, 
																									"Billing Items Container", 
																									true, 
																									false, 
																									false, 
																									false, 
																									BillingItemsContainerDAO.class, 
																									SharedUtil.extractNVConfigs(Params.values()), 
																									null, 
																									false, 
																									CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																					 			);
	
	/**
	 * The default constructor.
	 */
	public BillingItemsContainerDAO()
	{
		super(NVC_BILLING_ITEMS_CONTAINER_DAO);
	}
	
	/**
	 * This constructor instantiates BillingItemsContainerDAO object based on given NVConfigEntity.
	 * @param nvce
	 */
	protected BillingItemsContainerDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Returns billing status.
	 * @return status
	 */
	public String getBillingStatus()
	{
		return lookupValue(Params.STATUS);
	}
	
	/**
	 * Sets billing status.
	 * @param status
	 */
	public void setBillingStatus(String status)
	{		
		setValue(Params.STATUS, status);
	}
	
	/**
	 * Returns billing items as an array values of BillingItemDAO.
	 * @return list of items
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getBillingItems()
	{
		return (ArrayValues<NVEntity>) lookup(Params.BILLING_ITEMS);
	}
	
	/**
	 * Sets billing items given an array values of BillingItemDAO.
	 * @param values
	 */
	public void setBillingItems(ArrayValues<NVEntity> values)
	{
		getBillingItems().add(values.values(), true);
	}
	
	/**
	 * Sets billing items given a list of BillingItemDAO.
	 * @param values
	 */
	public void setBillingItems(List<NVEntity> values)
	{
		getBillingItems().add(values.toArray(new NVEntity[0]), true);
	}
	
	/**
	 * Returns the total.
	 * @return total as BigDecimal
	 */
	public BigDecimal getTotal()
	{
		return calculateTotal();
	}
	
	/**
	 * Calculates the total.
	 * @return compute the total
	 */
	public synchronized BigDecimal calculateTotal()
	{
		BigDecimal total = new BigDecimal(0);
		
		for (NVEntity nve : getBillingItems().values())
		{
			if (nve instanceof BillingItemDAO)
			{
				BillingItemDAO item = (BillingItemDAO) nve;
				
				if (item.getTotal() != null)
				{
					total = total.add(item.getTotal());
				}
			}
		}
		
		setValue(Params.TOTAL, total);
		
		return total;
	}
	
	/**
	 * Adds BillingItemDAO.
	 * @param billingItem
	 * @return the added item
	 */
	public synchronized BillingItemDAO addBillingItem(BillingItemDAO billingItem)
	{
		BillingItemDAO ret = null;
		
		if (billingItem != null && !contains(billingItem.getReferenceID()))
		{
			billingItem.setCanonicalID(getCanonicalID());
			ret = (BillingItemDAO) getBillingItems().add(billingItem);
			calculateTotal();
		}
		
		return ret;
	}
	
	/**
	 * Removes BillingItemDAO.
	 * @param billingItem
	 * @return the removed item
	 */
	public BillingItemDAO removeBillingItem(BillingItemDAO billingItem)
	{
		BillingItemDAO ret = null;
		
		if (billingItem != null)
		{
			ret = (BillingItemDAO) getBillingItems().remove(billingItem);
			calculateTotal();
		}
		
		return ret;
	}
	
	private boolean contains(String refID)
	{
		if (refID != null)
		{
			for (NVEntity nve : getBillingItems().values())
			{
				if (nve instanceof BillingItemDAO)
				{
					BillingItemDAO item = (BillingItemDAO) nve;
					
					if (item.getReferenceID() != null && refID.equals(item.getReferenceID()))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
}