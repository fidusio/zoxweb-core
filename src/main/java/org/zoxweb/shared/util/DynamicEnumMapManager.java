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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The class is used to manage dynamic enum maps.
 * @author mzebib
 */
public class DynamicEnumMapManager
{

	/**
	 * This variable declares that only one instance of this class can be created.
	 */
	public static final DynamicEnumMapManager SINGLETON = new DynamicEnumMapManager();
	
	/**
	 * This variable creates a hash map of string and dynamic enum map.
	 */
	private HashMap<String, DynamicEnumMap> allEnumMaps = new HashMap<String, DynamicEnumMap>();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private DynamicEnumMapManager()
    {

	}

	/**
	 * This method returns all dynamic enum maps.
	 * @return the map
	 */
	public HashMap<String, DynamicEnumMap> getAllEnumMaps()
    {
		return allEnumMaps;
	}

	/**
	 * This method is for bean compliance only.
	 * @param allEnumMaps
	 * @Deprecated caller must never invoke this method
	 */
	public void setAllEnumMaps(HashMap<String, DynamicEnumMap> allEnumMaps)
    {
		this.allEnumMaps = allEnumMaps;
	}

	/**
	 * Adds a dynamic enum map.
	 * @param enumMap enum map object
	 */
	public synchronized DynamicEnumMap addDynamicEnumMap(DynamicEnumMap enumMap)
    {
		SharedUtil.checkIfNulls("Can't add null values", enumMap);
		
		DynamicEnumMap currentDEM = allEnumMaps.get(enumMap.getName());
		
		if (currentDEM != null)
		{
			currentDEM.setValue(enumMap.getValue());
			currentDEM.setReferenceID(enumMap.getReferenceID());
			return currentDEM;
		}
		else
		    {
			allEnumMaps.put(enumMap.getName(), enumMap);
			return enumMap;
		}
	}
	
	/**
	 * Deletes a dynamic enum map based on the given name.
	 * @param name
	 */
	public synchronized DynamicEnumMap deleteDynamicEnumMap(String name)
    {
		DynamicEnumMap toDelete = lookup(name);
		
		if (toDelete != null)
		{
			return allEnumMaps.remove(toDelete.getName());
		}
		
		return null;
	}
	
	/**
	 * Looks up the dynamic enum map based on the given enum map name.
	 * @param enumMapName
	 * @return tehe map
	 */
	public DynamicEnumMap lookup(String enumMapName)
    {
		DynamicEnumMap ret = allEnumMaps.get(enumMapName);
		
		if (ret == null && enumMapName != null)
		{
			ret = allEnumMaps.get(SharedUtil.toCanonicalID(':', DynamicEnumMap.NAME_PREFIX, enumMapName));
		}
		
		return ret;
	}
	
	/**
	 * Returns the number of entries.
	 * @return size
	 */
	public int size()
    {
		return allEnumMaps.size();
	}
	

	/**
	 * Clears all entries.
	 * @param keepStatic
	 */
	public synchronized void clear(boolean keepStatic)
    {
		if (!keepStatic)
		{
			allEnumMaps.clear();
		}
		else
        {
			String allEnumTypeNames[] = allEnumMaps.keySet().toArray(new String[0]);
			
			for (String enumTypeName : allEnumTypeNames)
			{
				DynamicEnumMap dem = lookup(enumTypeName);
				
				if (dem != null)
				{
					if (!dem.isStatic())
					{
						deleteDynamicEnumMap(enumTypeName );
					}
				}
			}
		}
	}
	
	/**
	 * Adds dynamic enum map based on given enum name and names of the parameters.
	 * @param enumName
	 * @param names
	 * @return DynamicEnumMap
	 */
	public synchronized DynamicEnumMap addDynamicEnumMap(String enumName, String... names)
    {
		DynamicEnumMap ret = lookup(enumName);
		
		if (ret == null)
		{
			ret = new DynamicEnumMap();
			ret.setName(enumName);
			
			for (String name : names)
			{
				ret.addEnumValue(new NVPair(name, (String)null));
			}
			
			addDynamicEnumMap(ret);
		}
				
		return ret;		
	}
	
	/**
	 * Adds dynamic enum map based on the given name and value.
	 * @param name
	 * @param values
	 * @return DynamicEnumMap
	 */
	public synchronized DynamicEnumMap addDynamicEnumMap(String name, Enum<?>... values)
    {
		DynamicEnumMap ret = lookup(name);
		
		if (ret == null)
		{
			ret = new DynamicEnumMap();
			ret.setName(name);
			
			for (Enum<?> value : values)
			{
				ret.addEnumValue(value);
			}
			
			addDynamicEnumMap(ret);
		}
		
		return ret;		
	}
	
	/**
	 * Returns an array of all dynamic enum maps.
	 * @return all DynamicEnumMap
	 */
	public DynamicEnumMap[] getAll()
    {
		return allEnumMaps.values().toArray(new DynamicEnumMap[0]);
	}
	
	/**
	 * Validates the given dynamic enum map.
	 * @param dem
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	public static void validateDynamicEnumMap(DynamicEnumMap dem)
		throws NullPointerException, IllegalArgumentException
    {
		SharedUtil.checkIfNulls("Dynamic enum map is null.", dem);
		
		List<NVPair> values = dem.getValue();
		
		SharedUtil.checkIfNulls("Dynamic enum map values are null.", values);
		
		HashSet<String> names = new HashSet<String>();
		
		for (int i = 0; i < values.size(); i++)
		{
			NVPair nvp = values.get(i);
			
			if (nvp == null)
			{
				throw new NullPointerException("Dynamic enum value at index " + i + " is null.");
			}
			
			if (SUS.isEmpty(nvp.getName()))
			{
				throw new IllegalArgumentException("Dynamic enum name at index " + i + " is empty or null.");
			}
			
			names.add(nvp.getName());
			
			//	This check must be made after name is added because sequence matters in this case.
			if (names.size() != i + 1)
			{
				throw new IllegalArgumentException("Duplicate enum names exist.");
			}
		}
	}
}