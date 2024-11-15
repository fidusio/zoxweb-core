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
package org.zoxweb.shared.util;

import org.zoxweb.shared.filters.ValueFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * The dynamic enum map class extends NVPairList is used as a
 * dynamic value filter.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class DynamicEnumMap
	extends NVPairList
	implements ValueFilter<String, String>,
			   AccountID<String>,
			   SubjectGUID<String>,
			   SetDescription
{
	
	private boolean ignoreCase = true;
	private boolean staticEnum = false;
	public static final String NAME_PREFIX = "dynamic_enum_map";
	private String accountID;
	private String subjectGUID;
	private String description;
	
	/**
	 * The default constructor.
	 */
	public DynamicEnumMap()
    {
		//Call super constructor empty. Do not call super(name, value), 
		//Constructor must be called like this, otherwise an exception
		//will be thrown.
		super();
		value = new ArrayList<NVPair>();
	}
	
	/**
	 * This constructor instantiates DynamicEnumMap based on given name.
	 * @param name
	 */
	public DynamicEnumMap(String name)
    {
		super(name, new ArrayList<NVPair>());
	}
	
	/**
	 * This constructor instantiates DynamicEnumMap based on given enum class.
	 * @param enumClass
	 */
	public DynamicEnumMap(Class<?extends Enum<?>> enumClass)
    {
		this(enumClass.getName(), enumClass.getEnumConstants());
	}
	
	/**
	 * This constructor instantiates DynamicEnumMap based on given name and 
	 * list of NVPair.
	 * @param n
	 * @param v
	 */
	public DynamicEnumMap(String n, List<NVPair> v)
    {
		super(n, (ArrayList<NVPair>) v);
	}
	
	/**
	 * This constructor instantiates DynamicEnumMap based on given name and enums of Enum<?> type.
	 * @param name
	 * @param enums
	 */
	public DynamicEnumMap(String name, Enum<?>... enums)
    {
		super(name, new ArrayList<NVPair>());
	
		if (enums != null)
		{
			for (Enum<?> e: enums)
			{
				addEnumValue(e);
			}
		}
	}
	
	/**
	 * This constructor instantiates DynamicEnumMap based on name and enums of NVPair type.
	 * @param name
	 * @param enums
	 */
	public DynamicEnumMap(String name, NVPair... enums)
    {
		super(name, new ArrayList<NVPair>());
	
		if (enums != null)
		{
			for (NVPair nvp : enums)
			{
				addEnumValue(nvp);
			}
		}
	}
	
	/**
	 * Validates the given string.
	 * @param v
	 * @return validated string
	 */
	public final String validate(String v) 
        throws NullPointerException, IllegalArgumentException
    {
		if (v != null)
		{
			for (NVPair nvp : value)
			{
				if ((ignoreCase ? v.equalsIgnoreCase(nvp.getName()) : v.equals(nvp.getName())))
				{
					return nvp.getName();
				}
				else if ((ignoreCase ? v.equalsIgnoreCase(nvp.getValue()) : v.equals(nvp.getValue())))
				{
					return nvp.getName();
				}
			}
		}
		else
        {
			return null;
		}
		
		throw new IllegalArgumentException(v + " is not a valid value");
	}
	
	/**
	 * Checks whether the given string is valid.
	 * @param v
	 * @return true if valid
	 */
	public boolean isValid(String v)
    {
		if (v != null)
		{
			for (NVPair nvp : value)
			{
				if ((ignoreCase ? v.equalsIgnoreCase(nvp.getName()) : v.equals(nvp.getName())))
				{
					return true;
				}
				else if ((ignoreCase ? v.equalsIgnoreCase(nvp.getValue()) : v.equals(nvp.getValue())))
				{
					return true;
				}
			}
		}
		else
        {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds an enum type value.
	 * @param e
	 */
	public void addEnumValue(Enum<?> e)
    {
		SUS.checkIfNulls("Enum is null.", e);
		
		if (e instanceof GetName)
		{
			addEnumValue(new NVPair(e.name(), ((GetName) e).getName()));
		}
		else if (e instanceof GetValue)
		{
			addEnumValue(new NVPair(e.name(), "" + ((GetValue<?>) e).getValue()));
		}
		else
        {
			addEnumValue(new NVPair(e.name(), (String)null));
		}
	}
	
	/**
	 * This method is declared synchronized to avoid multi-threading and
	 * is used add NVPair type enum value. 
	 * @param nvp
	 */
	public synchronized void addEnumValue(NVPair nvp)
    {
		if (!isValid( nvp.getName()))
		{
			value.add(nvp);
		}
	}
	
	/**
	 * Looks up and returns NVPair based on enum name or value.
	 * @param enumNameOrValue
	 * @return mapped nvpair
	 */
	public final NVPair lookup(String enumNameOrValue)
    {
		if (enumNameOrValue != null)
		{
			for (NVPair nvp : value)
			{
				if ((ignoreCase ? enumNameOrValue.equalsIgnoreCase(nvp.getName()) : enumNameOrValue.equals(nvp.getName())))
				{
					return nvp;
				}
				else if ((ignoreCase ? enumNameOrValue.equalsIgnoreCase(nvp.getValue()) : enumNameOrValue.equals(nvp.getValue())))
				{
					return nvp;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the name.
	 * @return to string conversion
	 */
	public String toString()
    {
		return name +':'+ SharedUtil.toCanonicalID(',', value.toArray());
	}

	/**
	 * Checks if ignore case.
	 * @return true if case insensitive
	 */
	public boolean isIgnoreCase()
    {
		return ignoreCase;
	}

	/**
	 * Sets the ignore case value.
	 * @param ignoreCase
	 */
	public void setIgnoreCase(boolean ignoreCase)
    {
		this.ignoreCase = ignoreCase;
	}

	/**
	 * Returns the string representation of the class.
	 * @return the canonical id 
	 */
	@Override
	public String toCanonicalID()
    {
		return getName();
	}

	/**
	 * Checks if static enum.
	 * @return true if static
	 */
	public boolean isStatic()
    {
		return staticEnum;
	}

	/**
	 * Sets the static enum value.
	 * @param staticEnum
	 */
	public void setStatic(boolean staticEnum)
    {
		this.staticEnum = staticEnum;
	}
	
	/**
	 * This method is declared synchronized to avoid multi-threading and
	 * is used to set the name.
	 * @param name
	 */
	public synchronized void setName(String name)
    {
		String oldName = this.name;

		SUS.checkIfNulls("Null value", name);
		
		name = SharedStringUtil.filterString(name, NAME_PREFIX + ":", NAME_PREFIX);
		
		name = SharedStringUtil.trimOrNull(name);
		
		if (name == null)
		{
			throw new IllegalArgumentException("Invalid name");
		}
		
		this.name = SharedUtil.toCanonicalID(':', NAME_PREFIX, name);
		
		if (oldName != null)
		{
			DynamicEnumMapManager.SINGLETON.deleteDynamicEnumMap(oldName);
		}
		
		DynamicEnumMapManager.SINGLETON.addDynamicEnumMap(this);
	}

	/**
	 * Returns the user ID.
	 * @return user id
	 */
	@Override
	public String getSubjectGUID()
    {
		return subjectGUID;
	}

	/**
	 * Sets the user ID.
	 * @param userID
	 */
	@Override
	public void setSubjectGUID(String userID)
    {
		this.subjectGUID = userID;
	}

	/**
	 * Returns the account ID.
	 * @return account id
	 */
	@Override
	public String getAccountID()
    {
		return accountID;
	}

	/**
	 * Sets the account ID.
	 * @param accountID
	 */
	@Override
	public void setAccountID(String accountID)
    {
		this.accountID = accountID;
	}

	/**
	 * Returns the description.
	 * @return description
	 */
	public String getDescription()
    {
		return description;
	}

	/**
	 * Sets the description.
	 * @param description
	 */
	public void setDescription(String description)
    {
		this.description = description;
	}

	/**
	 * Returns the display name.
	 * @return display name
	 */
	public String getDisplayName()
    {
		return SharedStringUtil.filterString(getName(), NAME_PREFIX + ":", NAME_PREFIX);
	}

}