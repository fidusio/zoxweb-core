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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This is a meta defined object which can be represented as a bean object. 
 * This class is declared abstract, therefore it cannot be instantiated. 
 * @author mzebib
 */
@SuppressWarnings("serial")
public abstract class NVEntity
	implements ReferenceID<String>,
			   //AccountID<String>,
			   //UserID<String>,
			   SubjectGUID<String>,
               GlobalID<String>,
			   GetNVConfig,
			   GetName {
	
	static class NVCB<V>
    {
		final NVConfig nvc;
		final NVBase<V> nvb;
		
		NVCB(NVConfig nvc, NVBase<V> nvb)
        {
			this.nvc = nvc;
			this.nvb = nvb;
		}
	}

	public static boolean GLOBAL_ID_AS_REF_ID = false;
	
	protected transient NVConfigEntity config;
	protected Map<String, NVBase<?>> attributes;

	protected NVEntity(NVConfigEntity nvce)
    {
//		config = nvce;
//		attributes = SharedUtil.toData( config.getAttributes());
		this(nvce, SharedUtil.toData(nvce.getAttributes()));
	}
	
	/**
	 * 
	 * @param c
	 * @param a
	 */
	protected NVEntity(NVConfigEntity c, Map<String, NVBase<?>>  a)
    {
		config = c;
		attributes = a;
	}
	
	/**
	 * Returns the NVConfig.
	 */
	public NVConfig getNVConfig()
    {
		return config;
	}

	/**
	 * Returns the attributes.
	 * @return attributes map 
	 */
	public Map<String, NVBase<?>> getAttributes()
    {
		return attributes;
	}
	
	/**
	 * Sets the attributes.
	 * @param attr
	 */
	public void setAttributes(Map<String, NVBase<?>> attr)
    {
		attributes = attr;
	}
	
	/**
	 * Looks up NVBase object by name.
	 * @param name
	 * @return lookup matching name nvbase
	 */
	public  <V extends NVBase<?>> V lookup(String name)
    {
		return (V)attributes.get(name);
	}

	public <V extends NVBase<?>> V lookup(GetNVConfig gnvc)
    {
		return (V)attributes.get(gnvc.getNVConfig().getName());
	}
	
	public <V extends NVBase<?>> V lookup(GetName gName)
    {
		return (V)attributes.get(gName.getName());
	}
	
	
	/**
	 * Looks up the NVBase object of generic type
	 * by given parameter of NVConfig type.
	 * @param nvc
	 * @return value
	 */
	public <V> V lookupValue(NVConfig nvc)
    {
		@SuppressWarnings("unchecked")
		NVBase<V> ret = (NVBase<V>) attributes.get(nvc.getName());

		if (ret != null)
		{
			return ret.getValue();
		}

		return null;
	}
	
	/**
	 * 
	 * Looks up the NVBase object of generic type
	 * by given parameter of GetName type.
	 * @param gName
	 * @return value
	 */
	public <V> V lookupValue(GetName gName)
    {
		@SuppressWarnings("unchecked")
		NVBase<V> ret = (NVBase<V>) attributes.get(gName.getName());

		if (ret != null)
		{
			return ret.getValue();
		}

		return null;
	}

	/**
	 * This method looks up the NVBase object of generic 
	 * type object by name. 
	 * the name and return its value.
	 * @param name
	 * @return value
	 */
	public <V> V lookupValue(String name) {
		@SuppressWarnings("unchecked")
		NVBase<V> ret = (NVBase<V>) attributes.get(name);
		
		if (ret != null) {
			return ret.getValue();
		}
		
		return null;
	}
	
	/**
	 * This method looks up the NVBase object of generic type
	 * by given parameter of GetNVConfig type.
	 * return its value.
	 * @param gnvc
	 * @return value
	 */
	public <V> V lookupValue(GetNVConfig gnvc)
	{	
		return lookupValue(gnvc.getNVConfig());
	}
	
	
	/**
	 * This method will set the value for the NVBase
	 * object.
	 * @param name
	 * @param v
	 */
	@SuppressWarnings("unchecked")
	public <V> void setValue(String name, V v)
	{	
		NVCB<V> nvcb = lookupNVCB(name);

		if (nvcb != null)
		{
			//NVBase<V> ret = (NVBase<V>) attributes.get(name);
		
			//if (ret != null)
			{
				//NVConfig nvc = config.lookup(name);
				if (config.isAttributesValidationRequired() && v == null && nvcb.nvc.isMandatory())
				{
					throw new NullPointerException("attibute " + nvcb.nvc + " is a required value can't be null");
				}
				
				ValueFilter<Object, Object> vf = (ValueFilter<Object, Object>) nvcb.nvc.getValueFilter();
				
				if (vf != null)
				{	
					//If an array object has a value filter, the value filter will be applied 
					//to the contained object not to the array.
					if (nvcb.nvc.isArray() && v instanceof List)
					{
						List<Object> list = (List<Object>) v; 

						for (int i = 0; i < list.size(); i++)
						{
							Object value = list.get(i);
							if (value instanceof NVPair)
							{
								((NVPair) value).setValue((String) vf.validate(((NVPair) value).getValue())); 
							}
							else
							{
								value = vf.validate(value);
							}
							
							list.set(i, value);
						}
	
					}
					else
					{
						if (nvcb.nvc.isMandatory() || v != null)
							v = (V) vf.validate(v);
	
					}
					
				}
				
				
				nvcb.nvb.setValue(v);
			}
		}
	}
	
	/**
	 * Sets the value to the given NVBase object.
	 * @param gnvc
	 * @param v
	 */
	public <V> void setValue(GetNVConfig gnvc, V v) {
		setValue(gnvc.getNVConfig(), v);
	}
	
	/**
	 * Sets the value to the given NVBase object.
	 * @param nvc
	 * @param v
	 */
	public <V> void setValue(NVConfig nvc, V v) {
		setValue(nvc.getName(), v);
	}
	
	
	/**
	 * Returns the string representation of the object.
	 */
	@SuppressWarnings("rawtypes")
	public String toString()
    {
		if (attributes != null)
		{
			StringBuilder sb = new StringBuilder();
			
			if (config.getDisplayAttributes() == null)
			{
				for (int i = 0; i < config.getAttributes().size(); i++)
				{
					NVBase<?> nvb = attributes.get( config.getAttributes().get(i).getName());
					
					if (nvb != null && nvb.getValue() != null)
					{
						if ( i > 0)
						{
							sb.append(",");
						}
						
//						if (nvb instanceof NVGenericMap)
//						{
//							sb.append(nvb.getName() + ":[");
//							for (Object o: ((ArrayValues)nvb).values())
//							{
//								sb.append(""+o);
//							}
//							sb.append("]");
//						}
//						
//						else
							if (nvb instanceof ArrayValues) {
							sb.append( nvb.getName() + ":" + Arrays.toString(((ArrayValues)nvb).values()));
						} else {
							sb.append( nvb.getName() + ":" + nvb.getValue());
						}
					}
				}
			}
			else
            {
				for (int i = 0; i < config.getDisplayAttributes().size(); i++)
				{
					NVConfig confAttr = config.getDisplayAttributes().get(i);
					NVBase<?> nvb = lookup( confAttr.getName());
					
					if (nvb != null && nvb.getValue() != null)
					{
						if (i > 0)
						{
							sb.append(",");
						}
						
						if (nvb instanceof ArrayValues)
						{
							sb.append( nvb.getName() + ":" + Arrays.toString(((ArrayValues)nvb).values()));
						}
						else
                        {
							sb.append( nvb.getName() + ":" + nvb.getValue());
						}
					}
				}
			}
			
			return sb.toString();
		}
		
		return super.toString();
	}
	
	@SuppressWarnings("unchecked")
	private <V> NVCB<V> lookupNVCB(String name)
    {
		NVBase<?> retNVB = (NVBase<?>) attributes.get(name);
		NVConfig  retNVC = config.lookup(name);

		if (retNVB == null && name.indexOf('.') != -1)
		{
			String subNames [] = name.split("\\.");
			NVEntity nve = this;

			for (int i = 0; i < subNames.length; i++)
			{
				if (nve != null)
				{
					retNVB = nve.lookup(subNames[i]);

					if (retNVB != null && retNVB instanceof NVEntityReference)
					{
						nve = (NVEntity) retNVB.getValue();
					}
					else if (i+1 < subNames.length)
					{
						return null;
					}
					else
                    {
						// we have a match
						retNVC = ((NVConfigEntity)nve.getNVConfig()).lookup(subNames[i]);
					}
				}
				else
                {
					return null;
				}
			}
			
		}

		if (retNVB != null)
		{
			return new NVCB<V>(retNVC, (NVBase<V>)retNVB); 
		}
		
		return null;
	}

}