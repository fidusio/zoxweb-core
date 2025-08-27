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

import org.zoxweb.shared.data.CanonicalIDDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.math.BigDecimal;

/**
 *
 */
@SuppressWarnings("serial")
public class BillingItemDAO
	extends CanonicalIDDAO
{

	public enum Params
        implements GetNVConfig
	{
		NVE_REFERENCE(NVConfigManager.createNVConfigEntity("nve_reference", "The NVEntity reference.", "NVEntityReference", false, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		UNIT_COST(NVConfigManager.createNVConfig("unit_cost", "The unt cost (i.e. hours and/or minutes).", "UnitCost", false, true, BigDecimal.class)),
		QUANTITY(NVConfigManager.createNVConfig("quantity", "The quantity.", "Quantity", false, true, BigDecimal.class)),
		WAIVED(NVConfigManager.createNVConfig("waived", "Billing waived", "Waived", false, true, Boolean.class)),
		
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		@Override
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}

	public static final NVConfigEntity NVC_BILLING_ITEM_DAO = new NVConfigEntityPortable(
																							"billing_item_dao", 
																							null, 
																							"Billing Item", 
																							true, 
																							false, 
																							false, 
																							false, 
																							BillingItemDAO.class, 
																							SharedUtil.extractNVConfigs(Params.values()), 
																							null, 
																							false, 
																							CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																					 );

	/**
	 * The default constructor.
	 */
	public BillingItemDAO()
	{
		super(NVC_BILLING_ITEM_DAO);
	}
	
	/**
	 * This constructor instantiates BillingItemDAO object based on given NVConfigEntity.
	 * @param nvce
	 */
	protected BillingItemDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Returns referenced NVEntity.
	 * @return the referenced NVEntity
	 */
	public NVEntity getNVEntityReference()
	{
		return lookupValue(Params.NVE_REFERENCE);
	}
	
	/**
	 * Sets referenced NVEntity.
	 * @param nve
	 */
	public void setNVEntityReference(NVEntity nve)
	{
		setValue(Params.NVE_REFERENCE, nve);
	}
	
	/**
	 * Returns the unit cost.
	 * @return the cost of on unit
	 */
	public BigDecimal getUnitCost()
	{
		return lookupValue(Params.UNIT_COST);
	}
	
	/**
	 * Sets the unit cost.
	 * @param value
	 */
	public void setUnitCost(BigDecimal value)
	{		
		setValue(Params.UNIT_COST, value);
	}
	
	/**
	 * Returns the quantity.
	 * @return the quantity
	 */
	public BigDecimal getQuantity()
	{
		return lookupValue(Params.QUANTITY);
	}
	
	/**
	 * Sets the quantity.
	 * @param quantity
	 */
	public void setQuantity(BigDecimal quantity)
	{		
		setValue(Params.QUANTITY, quantity);
	}
	
	/**
	 * Checks if billing is waived.
	 * @return true if waived
	 */
	public boolean isWaived()
	{
		return lookupValue(Params.WAIVED);
	}
	
	/**
	 * Sets if billing is waived.
	 * @param waived
	 */
	public void setWaived(boolean waived)
	{		
		setValue(Params.WAIVED, waived);
	}
	
	public BigDecimal getTotal()
	{
		BigDecimal ret = new BigDecimal(0);
		
		if (getUnitCost() != null && getQuantity() != null)
		{
			ret = getUnitCost().multiply(getQuantity());
		}
		
		return ret;
	}
	
}