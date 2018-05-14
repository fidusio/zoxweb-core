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

import java.math.BigDecimal;
import java.util.Date;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zoxweb.shared.data.ReferenceIDDAO;
import org.zoxweb.shared.filters.GetValueFilter;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.Const.GNVType;

/**
 * Contains shared utility methods.
 */
public class SharedUtil
{
	
	/**
	 * The constructor is declared private to prevent instantiation.
	 */
	private SharedUtil()
    {

	}

	/**
	 * Checks all the objs if any of them is null it will throws a NullPointerException.
	 * @param msg to be added to the NullPointerException
	 * @param objs to be checked
	 * @throws NullPointerException
	 */
	public static void checkIfNulls(String msg, Object... objs)
		throws NullPointerException
	{
		if (objs == null)
		{
			// error in invoking the check
			throw new NullPointerException("Null Array Object");
		}

		for (Object o : objs)
		{
            if (o == null)
            {
                throw new NullPointerException(msg);
            }
        }
	}

	@SuppressWarnings("unchecked")
	public static <T> T getWrappedValue(T v)
    {
		if (v instanceof GetWrappedValue)
		{
			return (T) ((GetWrappedValue<?>)v).getWrappedValue();
		}
		
		return v;
	}
	
	public static <V> void putUnique(Map<Long, V> map, long key, V v)
	{
		synchronized(map)
		{
			while (map.get(key) != null)
			{
				key++;
			}
			
			map.put(key, v);
		}
	}
	
	/**
	 * Creates a canonical string based on an array of objects which are separated by a character.
	 * @param sep
	 * @param objArray
	 * @return obj0.toString() + sep + obj1.toString() + sep + obj2.toString() ...
	 */
	public static String toCanonicalID(char sep, Object... objArray)
    {
		return toCanonicalID(false, sep, objArray);
	}
	
	
	
	
	public static String toCanonicalID(boolean ignoreNulls, char sep, Object... objArray)
    {
		StringBuilder sb = new StringBuilder();
		
		if (objArray != null)
		{
			for (int i = 0; i < objArray.length; i++)
			{
				_toCanonicalID(i, sb, ignoreNulls, sep, objArray[i] );
			}
		}
		
		return sb.toString();
	}

	/**
	 * Creates a canonical string based on a enum list of a certain data type and 
	 * the elements in list are read separately and are specified by a character.
	 * @param sep
	 * @param enumer
	 * @return obj0.toString() + sep + obj1.toString() + sep + obj2.toString() ...
	 */
	public static String toCanonicalID(boolean ignoreNulls, char sep, Enumeration<?> enumer)
    {
		StringBuilder sb = new StringBuilder();
		
		if (enumer != null)
		{
			int pos = 0;
			while (enumer.hasMoreElements())
            {
				_toCanonicalID(pos++, sb, ignoreNulls, sep, enumer.nextElement());
			}
		}
			
		return sb.toString();
	}
	
	private static StringBuilder _toCanonicalID(int pos, StringBuilder sb, boolean ignoreNull, char sep, Object val)
    {
		if ( val == null && ignoreNull)
		{
			return sb;
		}
		
		if (ignoreNull && sb.length() != 0)
		{
			sb.append(sep);
		}
		else if (pos != 0)
		{
			sb.append(sep);
		}
		
		if (val != null)
		{
            sb.append(val);
        }

		return sb;
	}


	public static boolean equals(ValueFilter<?, ?> vf1, ValueFilter<?, ?> vf2)
    {
		if (vf1 != null && vf2 != null)
		{
			if (vf1 == vf2)
			{
				return true;
			}

			if (vf1.toCanonicalID() != null && vf2.toCanonicalID() != null)
			{
				return vf1.toCanonicalID().equals(vf2.toCanonicalID());
			}
		}

		return false;
	}

	/**
	 * This utility method look for the matching enum in case insensitive fashion.
	 * 
	 * <br>It will try different matches with the following inventory:
	 * <ol>
	 * <li> try match enum.name();
	 * <li> try match enum.toString();
	 * <li> try match enum.getName() if enum instance of GetName
	 * <li> try match enum.getValue() if enum instance of GetValue
	 * </ol> 
	 * @param list
	 * @param str
	 * @return matching enum
	 */
	@SuppressWarnings("unchecked")
	public static <V extends Enum<?>> V lookupEnum(Enum<?>[] list, String str)
    {
		if (str != null)
		{
			for (Enum<?> e: list)
			{
				if (str.equalsIgnoreCase(e.name()))
				{
					return (V) e;
				}
				
				if (str.equalsIgnoreCase(e.toString()))
				{
					return (V) e;
				}
				
				if (e instanceof GetName && str.equalsIgnoreCase( ((GetName)e).getName()))
				{
					return (V) e;
				}
				
				if (e instanceof GetValue && str.equalsIgnoreCase( "" + ((GetValue<?>)e).getValue()))
				{
					return (V) e;
				}
			}
		}
		
		return null;
	}
	
	public static<T extends Enum<?>> T lookupTypedEnum(T[] list, String str)
    {
		if (str != null)
		{
			for (T e: list)
			{
				if (str.equalsIgnoreCase(e.name()))
				{
					return e;
				}
				
				if (str.equalsIgnoreCase(e.toString()))
				{
					return e;
				}
				
				if (e instanceof GetName && str.equalsIgnoreCase( ((GetName)e).getName()))
				{
					return e;
				}
				
				if (e instanceof GetValue && str.equalsIgnoreCase( "" + ((GetValue<?>)e).getValue()))
				{
					return e;
				}
			}
		}
		
		return null;
	}

	public static Enum<?> matchingEnumContent(Enum<?>[] list, String str)
    {
		if (str != null)
		{
			for (Enum<?> e: list)
			{
				if (SharedStringUtil.contains(str, e.name(), true))
				{
					return e;
				}

				if (SharedStringUtil.contains(str, e.toString(), true))
				{
					return e;
				}
				
				if (e instanceof GetName && SharedStringUtil.contains(str, ((GetName)e).getName(), true))
				{
					return e;
				}
				
				if (e instanceof GetValue && SharedStringUtil.contains(str, "" + ((GetValue<?>)e).getValue(),true))
				{
					return e;
				}	
			}
		}
		
		return null;
	}

	public static Enum<?> matchingEnumContent(String str, Enum<?>[] list)
    {
		if (str != null)
		{
			for (Enum<?> e: list)
			{
				if (SharedStringUtil.contains(e.name(), str, true))
				{
					return e;
				}

				if (SharedStringUtil.contains(e.toString(), str,true))
				{
					return e;
				}
				
				if (e instanceof GetName && SharedStringUtil.contains(((GetName)e).getName(), str, true))
				{
					return e;
				}
				
				if (e instanceof GetValue && SharedStringUtil.contains("" + ((GetValue<?>)e).getValue(), str,true))
				{
					return e;
				}
			}
		}
		
		return null;
	}

	/**
	 * Returns enum based on given enum class and value.
	 * @param enumClass
	 * @param value
	 * @return matching enum
	 */
	public static Enum<?> enumValue(Class<?> enumClass, String value)
    {
		if (value != null)
		{
			if (enumClass.isArray())
			{
				enumClass = enumClass.getComponentType();
			}
			
			if (enumClass.isEnum())
			{
				Enum<?>[] all = (Enum<?>[]) enumClass.getEnumConstants();
				
				for (int i=0; i < all.length; i++)
				{
					//Enum<?> e = (Enum<?>) all[i];
					if (all[i].name().equals(value))
					{
						return all[i];
					}
				}
			}
			else
			    {
				throw new IllegalArgumentException(enumClass + " is an enum class"); 
			}
		}
		
		return null;
	}

	/**
	 * Parses a name = value String and return an NVPair object.
	 * @param str
	 * @return parse name=value into nvpair
	 */
	public static NVPair toNVPair(String str)
    {
		return toNVPair(str, "=", false);
	}
	
	/**
	 * Converts a string to a NVPair based on the first occurrence of the sep in str.
	 * @param str
	 * @param sep
	 * @return parse name sep value into nvpair
	 */
	public static NVPair toNVPair(String str, String sep)
    {
		return toNVPair(str, sep, false);
	}

	public static NVPair toNVPair(String str, String sep, boolean trim)
    {
		NVPair ret = null;
		str = SharedStringUtil.trimOrNull(str);
		
		if (str != null)
		{
			int index = str.indexOf(sep);
			
			if (index != -1)
			{
				String name = SharedStringUtil.trimOrNull(str.substring(0, index));
				String value = str.substring(index+1, str.length());

				if (value.length() == 0)
				{
					value = null;
				}

				if (value != null && trim)
				{
					value = value.trim();
				}
				
				if (name != null)
				{
					ret = new NVPair(name, value);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Looks up the NVBase on name.
	 * @param list
	 * @param name
	 * @return nvbase that matches getName()
	 */
	public static NVBase<?> lookupNVPB(List<NVBase<?>> list, String name)
    {
		if (name != null)
		{
			for (NVBase<?> nvpb: list)
			{
				if (name.equalsIgnoreCase(nvpb.getName()))
				{
					return nvpb;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Looks up the NVBase based on enum.
	 * @param list
	 * @param e
	 * @return nvbase that matches getName()
	 */
	public static NVBase<?> lookupNVPB(List<NVBase<?>> list, Enum<?> e)
	{
		if (e != null)
		{
			for (NVBase<?> nvpb: list)
			{
				if (e.name().equalsIgnoreCase( nvpb.getName()))
				{
					return nvpb;
				}
				else if (e instanceof GetName)
				{
					String n = ((GetName) e).getName();

					if (n.equalsIgnoreCase(nvpb.getName()))
					{
						return nvpb;
					}
				}
				else if (e instanceof GetNVConfig)
				{
					String n = ((GetNVConfig) e).getNVConfig().getName();

					if (n.equalsIgnoreCase(nvpb.getName()))
					{
						return nvpb;
					}
					else
                    {
						n = ((GetNVConfig) e).getNVConfig().getDisplayName();

						if (n.equalsIgnoreCase(nvpb.getName()))
						{
							return nvpb;
						}
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * Looks NV list based on name.
	 * @param list
	 * @param name
	 * @return GateNameValue<?> matches name
	 */
	public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> list, String name)
    {
		return lookupNV(list, name, null);
	}
	
	/**
	 * Looks up NV based on name and canonical separator.
	 * @param list
	 * @param name
	 * @param canonicalSep
	 * @return GateNameValue<?> matches name
	 */
	public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> list, String name, String canonicalSep)
    {
		if (name != null)
		{
			if (canonicalSep != null)
			{
				name = SharedStringUtil.parseNameValue(name, canonicalSep)[0];
			}
			
			for (GetNameValue<V> nvp: list)
			{
				if (name.equalsIgnoreCase( nvp.getName()))
				{
					return nvp;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Looks up value based on name.
	 * @param list
	 * @param name
	 * @return return the value that matches name
	 */
	public static <V> V lookupValue(List<? extends GetNameValue<V>> list, String name)
    {
		V ret = null;
		
		if (name != null)
		{
			for (GetNameValue<V> nvp: list)
			{
				if (name.equalsIgnoreCase(nvp.getName()))
				{
					ret = nvp.getValue();
					break;
				}
			}
		}
		
		return ret;
	}

	public static <V> V lookupValue(ArrayValues<GetNameValue<V>> list, String name)
    {
		V ret = null;
		
		if (name != null)
		{
			for (GetNameValue<V> nvp: list.values())
			{
				if (name.equalsIgnoreCase(nvp.getName()))
				{
					ret = nvp.getValue();
					break;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Looks up value based on enum.
	 * @param list
	 * @param e
	 * @return lookup value that matched e name 
	 */
	public static <V> V lookupValue(List<? extends GetNameValue<V>> list, Enum<?> e)
    {
		V ret = null;
		
		if (e != null)
		{
			for (GetNameValue<V> nvp: list)
			{
				if (e.name().equalsIgnoreCase( nvp.getName()))
				{
					ret = nvp.getValue();
					break;
				}
				else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase( nvp.getName()))
				{
					ret = nvp.getValue();
					break;
				}
			}
		}
		
		return ret;
	}
	
	
	public static <V> V lookupValue(GetNameValue<V> v)
    {
		if (v != null)
		{
			return v.getValue();
		}
		
		return null;
	}
	
	/**
	 * Looks up list which GetName based on name.
	 * @param list
	 * @param name
	 * @return return the matching GetName that matches name
	 */
	@SuppressWarnings("unchecked")
	public static  <V> V lookup(List<? extends GetName> list, String name)
    {
		V ret = null;
		
		if (name != null)
		{
			for (GetName nvp: list)
			{
				if (name.equalsIgnoreCase(nvp.getName()))
				{
					ret = (V)nvp;
					break;
				}
			}
		}
		
		return ret;
	}

	public static <V> V lookupMap(Map<String, V> map, String key, boolean ignoreCase)
    {
		if (key != null)
		{
			if (ignoreCase)
			{
				Set<Map.Entry<String, V>> set = map.entrySet();
				Iterator<Map.Entry<String, V>> it = set.iterator();
				while(it.hasNext()) {
					Map.Entry<String, V> entry = it.next();
					if (key.equalsIgnoreCase(entry.getKey()))
					{
						return entry.getValue();
					}
				}
			}
		}
		
		return map.get(key);
	}

	@SuppressWarnings("unchecked")
	public static  <V> List<V> search(GetName[] list, String... name)
    {
		List<V> ret = new ArrayList<V>();
		
		if (name != null && name.length > 0 && name[0] != null)
		{
			for (GetName nvp: list)
			{
				if (name[0].equalsIgnoreCase(nvp.getName()))
				{
					ret.add( (V) nvp);
				}
			}
		}
		
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static  <V> List<V> search(List<? extends GetName> list, String... name)
    {
		List<V> ret = new ArrayList<V>();
		
		if (name != null && name.length > 0 && name[0] != null)
		{
			for (GetName nvp: list)
			{
				if (name[0].equalsIgnoreCase(nvp.getName()))
				{
					ret.add( (V) nvp);
				}
			}
		}
		
		return ret;
	}

	/**
	 * Looks up list which extends GetName based on enum.
	 * @param list
	 * @param e
	 * @return return the matching GetName that matches e
	 */
	@SuppressWarnings("unchecked")
	public static  <V> V lookup(List<? extends GetName> list, Enum<?> e)
    {
		V ret = null;
		
		if (e != null)
		{
			for (GetName nvp: list)
			{
				if (e.name().equalsIgnoreCase( nvp.getName()))
				{
					ret = (V) nvp;
					break;
				}
				else if (e instanceof GetName && ((GetName) e).getName().equalsIgnoreCase(nvp.getName()))
				{
					ret = (V) nvp;
					break;
				}
			}
		}
		
		return ret;
	}

	/**
	 * Removes the prefix in a string and returns a substring of the original string
	 * less the prefix	
	 * @param prefix
	 * @param str
	 * @return str stripped of prefix
	 */
	public static String removePrefix(String prefix, String str)
    {
		if (prefix != null && str.startsWith(prefix))
		{
			// tele-sign do not like the + 
			str = str.substring(prefix.length());
		}
		
		return str;
	}

	/**
	 * Looks up NV list that extends GetNameValue based on enum.
	 * @param arrayList
	 * @param e
	 * @return matching GetNameValue<V>
	 */
	public static <V> GetNameValue<V> lookupNV(List<? extends GetNameValue<V>> arrayList, Enum<?> e)
    {
		if (e != null)
		{
			for (GetNameValue<V> nvp: arrayList)
			{
				if (e.name().equalsIgnoreCase(nvp.getName()))
				{
					return nvp;
				}
				else if (e instanceof GetName && ((GetName)e).getName().equalsIgnoreCase(nvp.getName()))
				{
					return nvp;
				}
			}
		}
		
		return null;
	}	
	
	/**
	 * This method converts an object array into a string.
	 * Ex. String str[] = new String[3];
	 * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
	 * toString(str) returns [0]:Zox [1]:Web [2]:Core
	 * @param array
	 * @return obj[0] + \n + ob[1] +\n + ...
	 */
	public static String toString(Object[] array)
    {
		return toString(array, "\n");
	}
	
	/**
	 * This method converts an object array into a string which contains 
	 * a specified string that separates each value of the array.
	 * Ex. String str[] = new String[3];
	 * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
	 * toString(str, "-") returns [0]:Zox-[1]:Web-[2]:Core
	 * @param array
	 * @param sep
	 * @return  obj[0] + sep + ob[1] +sep  + ...
	 */
	public static String toString(Object[] array, String sep)
    {
		return toString(array, sep, true);
	}
	
	/**
	 * This method converts an object array into a string which contains
	 * a specified string that separates each value of the array. Also, if index
	 * is false, returns only string without the value of its location within the 
	 * object array (in brackets). Otherwise if true, all characters are included.
	 * 	 * * Ex. String str[] = new String[3];
	 * str[0] = "Zox"; str[1] = "Web"; str[2] = "Core";
	 * toString(str, "-",false) returns Zox-Web-Core
	 * @param array
	 * @param sep
	 * @param index
	 * @return formatted string
	 */
	public static String toString(Object[] array, String sep, boolean index)
    {
		StringBuilder sb = new StringBuilder();
		
		if (array != null)
		{
			for (int i = 0; i < array.length; i++)
			{
				if (index)
				{
					sb.append("[");
					sb.append(i);
					sb.append("]:");
				}
				
				sb.append(array[i]);
				
				if (i + 1 !=  array.length)
				{
                    sb.append(sep);
                }
			}
		}
		
		return sb.toString();
	}

	/**
	 * Converts NVEntity to debug string.
	 * @param nve
	 * @return debug string
	 */
	public static String toDebugString(NVEntity nve)
    {
		StringBuilder sb = new StringBuilder();
		
		if (nve != null)
		{
			sb.append("[" + nve.getClass().getName() + "]\n");
			
			for (NVBase<?> nvb : nve.getAttributes().values())
			{
				sb.append("\t" + nvb.getClass().getName() + ",");
				
				if (nvb instanceof NVEntityReference)
				{
					sb.append(nvb.getName() + ":" + toDebugString((NVEntity)nvb.getValue()) + "\n");
				}
				
				if (nvb instanceof NVEntityReferenceList)
				{
					NVEntityReferenceList tempList = (NVEntityReferenceList) nvb;
					
					for (NVEntity nveTemp : tempList.getValue())
					{
						sb.append(nvb.getName() + ":" + toDebugString(nveTemp) + "\n");
					}
				}
				else {
					sb.append(nvb.getName() + ":" + nvb.getValue() + "\n") ;
				}
			}
		}
		else {
			sb.append("null");
		}
		
		return sb.toString();
	}
 	
	
	/**
	 * Looks up array values based on given String.
	 * @param arrayValues
	 * @param name
	 * @return list that matches name
	 */
	public static <V> List<? extends GetNameValue<V>> lookupArrayValues(ArrayValues<? extends GetNameValue<V>> arrayValues, String name)
    {
		ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>(); 
		
		if (name != null)
		{
			for (GetNameValue<V> nvp : arrayValues.values())
			{
				if (name.equalsIgnoreCase(nvp.getName()))
				{
					ret.add(nvp);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Looks up array values based on given enum.
	 * @param arrayValues
	 * @param e
	 * @return list that matches e
	 */
	public static <V> List<? extends GetNameValue<V>> lookupArrayValues(ArrayValues<? extends GetNameValue<V>> arrayValues, Enum<?> e)
    {
		ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>(); 
		
		if (e != null)
		{
			for (GetNameValue<V> nvp : arrayValues.values())
			{
				if (e.name().equalsIgnoreCase(nvp.getName()))
				{
					ret.add( nvp);
				}
				else if (e instanceof GetName && ((GetName)e).getName().equalsIgnoreCase(nvp.getName()))
				{
					ret.add(nvp);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Looks up all NV list that extends GetNameValue based on enum.
	 * @param arrayList
	 * @param e
	 * @return  list that matches e
	 */
	public static <V> List<? extends GetNameValue<V>> lookupAllNV(List<GetNameValue<V>> arrayList, Enum<?> e)
    {
		ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>(); 
		
		if (e != null)
		{
			for (GetNameValue<V> nvp : arrayList)
			{
				if (e.name().equalsIgnoreCase(nvp.getName()))
				{
					ret.add( nvp);
				}
				else if (e instanceof GetName && ((GetName)e).getName().equalsIgnoreCase(nvp.getName()))
				{
					ret.add(nvp);
				}
			}
		}
		
		return ret;
	}

	/**
	 * 
	 * @param arrayList
	 * @param name
	 * @return  list that matches name
	 */
	public static <V>List< ? extends GetNameValue<V>> lookupAllNV(List<? extends GetNameValue<V>> arrayList, String name)
    {
		return lookupAllNV(arrayList, name, null);
	}
	
	/**
	 * 
	 * @param arrayList
	 * @param name
	 * @param canonicalSep
	 * @return  list that matches name
	 */
	public static <V>List< ? extends GetNameValue<V>> lookupAllNV(List<? extends GetNameValue<V>> arrayList, String name, String canonicalSep)
    {
		ArrayList<GetNameValue<V>> ret = new ArrayList<GetNameValue<V>>(); 
		
		if (name != null)
		{
			if (canonicalSep != null)
			{
				name = SharedStringUtil.parseNameValue(name, canonicalSep)[0];
			}

			for (GetNameValue<V> nvp: arrayList)
			{
				if (name.equalsIgnoreCase(nvp.getName()))
				{
					ret.add(nvp);
				}
			}
		}
		
		return ret;
	}
	
//	public static <V>List<V> lookupMatches(List<? extends GetNameValue<V>> arrayList, String name)
//	{
//		return lookupMatches(arrayList, name, null);
//	}
//	
//	@SuppressWarnings("unchecked")
//	public static <V>List<V> lookupMatches(List<? extends GetNameValue<V>> arrayList, String name, String canonicalSep)
//	{
//				
//		ArrayList<V> ret = new ArrayList<V>(); 
//		
//		if (name != null)
//		{
//			if (canonicalSep != null)
//			{
//				name = SharedStringUtil.parseNameValue(name, canonicalSep)[0];
//			}
//			for (GetNameValue<V> nvp: arrayList)
//			{
//				if (name.equalsIgnoreCase(((GetName)nvp).getName()))
//				{
//					ret.add((V) nvp);
//				}
//			}
//		}
//		
//		return ret;
//	}
	
	/**
	 * 
	 * @param nvMap
	 * @return convert map to list nvpairs
	 */
	public static <V>List<? extends GetNameValue<String>> toNVPairs(Map<String, String[]> nvMap)
    {
		return toNVPairs(nvMap, false);
	}
	
	/**
	 * 
	 * @param nvMap
	 * @param nullAllowed
	 * @return convert map to list nvpairs
	 */
	public static <V>List<? extends GetNameValue<String>> toNVPairs(Map<String, String[]> nvMap, boolean nullAllowed)
    {
		List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();
		
		for (Map.Entry<String, String[]> nvp : nvMap.entrySet())
		{
			if (nvp != null)
			{
				String[] values = nvp.getValue();
				
				if (values != null)
				{
					for (String value : values)
					{
						if (!nullAllowed)
						{
                            value = SharedStringUtil.trimOrNull(value);
                        }
						
						if ((nullAllowed || value != null) && nvp.getKey() != null)
						{
                            ret.add(new NVPair(nvp.getKey(), value));
                        }
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param enums
	 * @return convert enum to list nvpairs
	 */
	@SuppressWarnings("unchecked")
	public static <V>List< ? extends GetNameValue<String>> toNVPairs(Enum<?>... enums)
    {
		List<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();
		
		for (Enum<?> e : enums)
		{
			NVPair nvp = new NVPair();
			
			if (e instanceof GetName)
			{
				nvp.setName(((GetName)e).getName());
			}
            else
            {
				nvp.setName(e.name());
			}
			
			if (e instanceof GetValue)
			{
				nvp.setValue(((GetValue<String>)e).getValue());
			}
			
			if (e instanceof GetValueFilter)
			{
			    ValueFilter<String, String> vf = ((GetValueFilter<String, String>)e).getValueFilter();
				
				//nvp.setValueFilter(((GetValueFilter<String, String>)e).getValueFilter());
				if (vf instanceof DynamicEnumMap)
				{
					vf = DynamicEnumMapManager.SINGLETON.lookup(((DynamicEnumMap) vf).getName());
				}
				
				nvp.setValueFilter(vf);
			}
			
			ret.add(nvp);
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param nvMap
	 * @return convert map to list nvpairs
	 */
	public static ArrayList<? extends GetNameValue<String>> listToNVPairs(Map<String, List<String>> nvMap)
    {
		ArrayList< GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();
		
		for (Map.Entry<String, List<String>> nvp : nvMap.entrySet())
		{
			if (nvp != null) {
                List<String> values = nvp.getValue();

                if (values != null)
                {
                    for (String value : values)
                    {
                        ret.add(new NVPair(nvp.getKey(), value));
                    }
                }
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param paramList
	 * @param configList
	 * @return true if all mandatory parameters are set
	 */ 
	public static boolean areAllMandtorySet(List<NVPair> paramList, GetNVConfig[] configList)
    {
		return (firstMissingMandtory(paramList, configList) == null);
	}
	
	/**
	 * 
	 * @param paramList
	 * @param configList
	 * @return the first not set mandatory nvconfig
	 */
	public static NVConfig firstMissingMandtory(List<NVPair> paramList, GetNVConfig[] configList)
    {
		for (GetNVConfig con : configList)
		{
			NVConfig config = con.getNVConfig();
			
			if (config.isMandatory())
			{
				if (lookupNV( paramList, config.getName()) == null)
				{
					if (lookupNV( paramList, (Enum<?>)con) != null)
					{
						continue;
					}

					return config;
				}		
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param params
	 * @return create map nvbase based on the meta params
	 */
	public static Map<String, NVBase<?>> toData(List<NVConfig> params)
    {
		HashMap<String, NVBase<?>> ret = new LinkedHashMap<String, NVBase<?>>();
		
		for (NVConfig config : params)
		{
			ret.put(config.getName(), metaConfigToNVBase(config));
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param params
	 * @return create list nvbased on nvconfig array
	 */
	public static ArrayList<NVBase<?>> toData(NVConfig[] params)
    {
		ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();
		
		for (NVConfig config : params)
		{
			ret.add(metaConfigToNVBase(config));
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param params
	 * @return create list nvbased on nvconfig array
	 */
	public static ArrayList<NVBase<?>> toData(GetNVConfig[] params)
    {
		ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();
		
		for (GetNVConfig config : params)
		{
			ret.add(metaConfigToNVBase(config.getNVConfig()));
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param nvce
	 * @param values
	 * @return create list nvbased on nvce 
	 */
	public static ArrayList<NVBase<?>> toData(NVConfigEntity nvce, ArrayList<NVBase<?>> values)
    {
		if (values == null)
		{
            values = new ArrayList<NVBase<?>>();
        }
		
		for (NVConfig config : nvce.getAttributes())
		{
			values.add(metaConfigToNVBase(config));
		}
		
		return values;
	}

	/**
	 * Converts NVConfig to NVBase based on
	 * the variable name shared between both objects.
	 * <br>Primitive object conversion list:</br>
	 * <ul>
	 * <li>Enum type class to NVEnum
	 * <li>String type class to NVPair
	 * <li>Long type class to NVLong
	 * <li>Integer type class to NVInt
	 * <li>Boolean type class to NVBoolean
	 * <li>Float type class to NVFloat
	 * <li>Double type class to NVDouble
	 * </ul>
	 * <br>Array object conversion list:</br>
	 * <ul>
	 * <li>Enum type class to NVEnumList
	 * <li>String array type class to NVPairList
	 * <li>Long array type class to NVLongList
	 * <li>Byte array type class to NVBlob
	 * <li>Integer array type class to NVIntList
	 * <li>Float array type class to NVFloatList
	 * <li>Double array type class to NVDoubleList
	 * </ul>
	 * @param config
	 * @return nvbase based on nvconfig
	 */
	@SuppressWarnings("unchecked")
	public static NVBase<?> metaConfigToNVBase(NVConfig config)
    {
		Class<?> c = config.getMetaType();
		
		if (config.isArray())
		{
			if (config instanceof NVConfigEntity)
			{
				NVConfigEntity nvce = (NVConfigEntity) config;
				//System.out.println(""+config);
				
				switch (nvce.getArrayType())
				{
				case GET_NAME_MAP:
					return new NVEntityGetNameMap (config.getName());
				case LIST:
					return new NVEntityReferenceList(config.getName());
				case NOT_ARRAY:
					break;
				case REFERENCE_ID_MAP:
					return new NVEntityReferenceIDMap (config.getName());
				default:
					break;
				
				}
				
				//return new NVEntityReferenceList(config.getName());
			}
			
			// enum must be checked first
			if (config.isEnum())
			{
				return (new NVEnumList(config.getName(), new ArrayList<Enum<?>>()));
			}
			else if (String[].class.equals(c))
			{
				if (config.isUnique())
				{
					return (new NVPairGetNameMap (config.getName(), new LinkedHashMap<GetName, GetNameValue<String>>()));
				}
				
				return (new NVPairList (config.getName(), new ArrayList<NVPair>()));
			}
			else if (Long[].class.equals(c))
			{
				return (new NVLongList(config.getName(), new ArrayList<Long>()));
			}
			else if (byte[].class.equals(c))
			{
				return (new NVBlob(config.getName(), null));
			}
			else if (Integer[].class.equals(c))
			{
				return (new NVIntList(config.getName(), new ArrayList<Integer>()));
			}
			else if (Float[].class.equals(c))
			{
				return (new NVFloatList( config.getName(), new ArrayList<Float>()));
			}
			else if (Double[].class.equals( c))
			{
				return (new NVDoubleList(config.getName(), new ArrayList<Double>()));
			}
			else if (Date[].class.equals(c))
			{
				return (new NVLongList( config.getName(), new ArrayList<Long>()));
			}
			else if (BigDecimal[].class.equals(c))
			{
				return (new NVBigDecimalList( config.getName(), new ArrayList<BigDecimal>()));
			}
		}
		else
        {
		    // Not array
			if (config instanceof NVConfigEntity)
			{
				return new NVEntityReference(config);
			}
			
			if (config.isEnum())
			{
				
//				if ( DynamicEnumMap.class.equals( config.getMetaType()))
//				{
//					return new NVDynamicEnum(config.getName(), null, (DynamicEnumMap) config.getValueFilter());
//				}
				
				return (new NVEnum(config.getName(), null));
			}
			else if (String.class.equals(c))
			{
				NVPair nvp = new NVPair(config.getName(), (String)null);
				nvp.setValueFilter(config.getValueFilter());
				return nvp;
			}
			else if (Long.class.equals(c))
			{
				return new NVLong(config.getName(), 0);
			}
			else if (Integer.class.equals(c))
			{
				return new NVInt(config.getName(), 0);
			}
			else if (Boolean.class.equals(c))
			{
				return (new NVBoolean(config.getName(), false));
			}
			else if (Float.class.equals(c))
			{
				return new NVFloat(config.getName(), 0);
			}
			else if (Double.class.equals(c))
			{
				return new NVDouble(config.getName(), 0);
			}
			else if (Date.class.equals(c))
			{
				return new NVLong(config.getName(), 0);
			}
			else if (BigDecimal.class.equals(c))
			{
				return new NVBigDecimal(config.getName(), new BigDecimal(0));
			}
			else if (NVGenericMap.class.equals(c))
			{
				return new NVGenericMap(config.getName());
			}
			else if (NVGenericMapList.class.equals(c))
			{
				return new NVGenericMapList(config.getName());
			}
			else if (NVStringList.class.equals(c))
			{
				return new NVStringList(config.getName());
			}
		}
		
		throw new IllegalArgumentException("Unsupported type " + config + " class:" + c);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T parsePrimitiveValue(GNVType type, Number n)
	{
		switch(type)
		{
//		case NVBLOB:
//			break;
//		case NVBOOLEAN:
//			break;
		case NVDOUBLE:
			return (T) new Double(n.doubleValue());
			
		case NVFLOAT:
			return (T) new Float(n.floatValue());
		case NVINT:
			return (T) new Integer(n.intValue());
		case NVLONG:
			return (T) new Long(n.longValue());
//		case NVString:
//			break;
		default:
			throw new IllegalArgumentException("Invalid type " + type);
		
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T parsePrimitiveValue(GNVType type, String v)
	{
		switch(type)
		{
//		case NVBLOB:
//			break;
//		case NVBOOLEAN:
//			break;
		case NVDOUBLE:
			return (T) new Double(Double.parseDouble(v));
			
		case NVFLOAT:
			return (T) new Float(Float.parseFloat(v));
		case NVINT:
			return (T) new Integer(Integer.parseInt(v));
		case NVLONG:
			return (T) new Long(Long.parseLong(v));
//		case NVString:
//			break;
		default:
			throw new IllegalArgumentException("Invalid type " + type);
		
		}
	}
	
	/**
	 * 
	 * @param config
	 * @param value
	 * @return convert string to value dictated by nvconfig
	 */
	public static Object stringToValue(NVConfig config, String value)
    {
		Class<?> c = config.getMetaType();
		
		if (c.isArray())
		{
			throw new IllegalArgumentException(config + " Cannot be converted, is of type array.");
//			if ( config instanceof NVConfigEntity)
//			{
//				return new NVEntityReferenceList(config);
//			}
//			
//		
//			// enum must be checked first
//			if ( c.getComponentType().isEnum())
//			{
//				return  ( new NVEnumList( config.getName(), new ArrayList<Enum<?>>()));
//			}
//			else if ( String[].class.equals( c))
//			{
//				return ( new NVPairList (config.getName(), new ArrayList<NVPair>()));
//			}
//			else if ( Long[].class.equals( c))
//			{
//				return ( new NVLongList( config.getName(), new ArrayList<Long>()));
//			}
//			else if ( byte[].class.equals( c))
//			{
//				return ( new NVBlob());
//			}
//			else if ( Integer[].class.equals( c))
//			{
//				return ( new NVIntList( config.getName(), new ArrayList<Integer>()));
//			}
//			else if ( Float[].class.equals( c))
//			{
//				return ( new NVFloatList( config.getName(), new ArrayList<Float>()));
//			}
//			else if ( Double[].class.equals( c))
//			{
//				return ( new NVDoubleList( config.getName(), new ArrayList<Double>()));
//			}
		}
		else
        {
		    // Not array
			if (c.isEnum())
			{
				return lookupEnum((Enum<?>[]) c.getEnumConstants(), value);
			}
			else if (String.class.equals(c))
			{
				return value;
			}
			else if (Long.class.equals(c))
			{
				if (!SharedStringUtil.isEmpty(value))
				{
					return Long.valueOf(value);
				}
				else  if (!config.isMandatory())
				{
					return Long.valueOf(0);
				}
			}
			else if (Integer.class.equals(c))
			{
				if (!SharedStringUtil.isEmpty(value))
				{
					return Integer.valueOf(value);
				}
				else  if (!config.isMandatory())
				{
					return Integer.valueOf(0);
				}
			}
			else if (Boolean.class.equals(c))
			{
				return Boolean.valueOf(value);
			}
			else if (Float.class.equals(c))
			{
				if (!SharedStringUtil.isEmpty(value))
				{
					return Float.valueOf(value);
				}
				else if (!config.isMandatory())
				{
				    return Float.valueOf((float) 0.0);
				}	
			}
			else if (Double.class.equals(c))
			{
				if (!SharedStringUtil.isEmpty(value))
				{
					return Double.valueOf(value);
				}
				else if ( !config.isMandatory())
				{
					return Double.valueOf( 0.0);
				}
			}
		}

		throw new IllegalArgumentException("Unsuppored type " + config);
	}

	/**
	 * First checks whether the class is of array type. Then checks the primitive data type of 
	 * class c and returns true if class type is primitive, otherwise returns false.
	 * @param c
	 * @return true if primitive note string is considered primitive
	 */
	public static boolean isPrimitive(Class<?> c)
    {
		checkIfNulls("Class is null.", c);
		
		if (c.isArray())
		{
			c = c.getComponentType();
		}
		
		if (c.isPrimitive() || c.equals( String.class) ||
                c.equals( Long.class) || c.equals( Integer.class) || c.equals( Float.class) || c.equals( Double.class))
			
		{
			return true;
		}
			
		return false;
	}
	
	/**
	 * 
	 * @param array
	 * @return extract the nvconfig from the GetNVConfig array 
	 */
	public static ArrayList<NVConfig> extractNVConfigs(GetNVConfig[] array)
    {
		ArrayList<NVConfig> ret = null;
		
		if (array != null)
		{
			ret = new ArrayList<NVConfig>();
			
			for (int i = 0 ; i < array.length; i++)
			{
				ret.add(array[i].getNVConfig());
			}
			
		}
		
		return ret;
	}
	
	
	public static NVGenericMap toNVGenricMap(NVEntity nve)
	{
		NVGenericMap ret = new NVGenericMap();
		NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
		for(NVConfig nvc : nvce.getAttributes())
		{
			ret.add(nve.lookup(nvc));
		}
		return ret;
	}
	
	
	public static NVGenericMap updateGetNVGenericMap(GetNVGenericMap toUpdate, NVGenericMap value)
	{
		return updateNVGenericMap(toUpdate.getNVGenericMap(), value);
	}
	
	
	
	@SuppressWarnings("unchecked")
	public static NVGenericMap updateNVGenericMap(NVGenericMap toUpdate, NVGenericMap value)
	{
		for(GetNameValue<?> gnv : value.values())
		{
			GetNameValue<?> gnvToUpdate = toUpdate.get(gnv);
			if (gnvToUpdate != null)
			{
				((NVBase<Object>)gnvToUpdate).setValue(gnv.getValue());
			}
			else
			{
				toUpdate.add(gnv);
			}
		}
		
		return toUpdate;
	}
	
	
	/**
	 * 
	 * @param array
	 * @return convert NVConfig array to List
	 */
	public static ArrayList<NVConfig> toNVConfigList(NVConfig... array)
    {
		ArrayList<NVConfig> ret = null;
		
		if(array != null)
		{
			ret = new ArrayList<NVConfig>();
			
			for (int i = 0; i < array.length; i++)
			{
				ret.add(array[i]);
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param list
	 * @param toAdd
	 * @return merged list + toAdd
	 */
	public static List<NVConfigEntity> merge(List<NVConfigEntity> list, NVConfigEntity... toAdd)
    {
		if (list == null)
		{
			list = new ArrayList<NVConfigEntity>();
		}
		
		for (NVConfigEntity nvce : toAdd)
		{
			list.add(nvce);
		}
		
		return list;
	}
	
//	public static NVConfigEntity mergeMeta(NVConfigEntity ... cList)
//	{
//		NVConfigEntity first = null;
//		for (int i= 0; i < cList.length; i++)//NVConfigEntity c : cList)
//		{
//			
//			NVConfigEntity c = cList[i];
//			if ( first == null)
//			{
//				first = c;
//				synchronized( LOCK)
//				{
//					if (!first.isMetaInitComplete())
//					{
//						first.setMetaInitComplete( true);
//						continue;
//					}
//					else
//					{
//						break;
//					}
//				}
//			}
//			first.getAttributes().addAll(0, c.getAttributes());
//			if ( i+1 == cList.length)
//				first.setAttributes(first.getAttributes());
//		}
//		return first;
//	}
	
	
	@SafeVarargs
	public static List<NVConfig> mergeMeta(List<NVConfig>... cList)
    {
//		ArrayList<NVConfig> first = null;
//		
//		for (int i = 0; i < cList.length; i++)//NVConfigEntity c : cList)
//		{
//			ArrayList<NVConfig> c = cList[i];
//			
//			if (first == null)
//			{
//				first = c;
//				continue;
//			}
//			
//			first.addAll(0, c);
//			
//		}
//		
//		return first;
		return mergeMeta(true, cList);
	}
	
	@SafeVarargs
	public static List<NVConfig> mergeMeta(boolean deepCopy, List<NVConfig>... cList)
    {
		List<NVConfig> first = null;
		
		for (int i = 0; i < cList.length; i++) {
			List<NVConfig> c = cList[i];
			
			if (first == null)
			{
				first = c;
				continue;
			}
			
			first.addAll(0, c);
		}
		
		return first;
	}
	
//	public static NVConfigEntity mergeMeta(List<NVConfigEntity> cList)
//	{
//		NVConfigEntity first = null;
//		for (int i= 0; i < cList.size(); i++)//NVConfigEntity c : cList)
//		{
//			
//			NVConfigEntity c = cList.get(i);
//			if ( first == null)
//			{
//				first = c;
//				synchronized( LOCK)
//				{
//					if (!first.isMetaInitComplete())
//					{
//						first.setMetaInitComplete( true);
//						continue;
//					}
//					else
//					{
//						break;
//					}
//				}
//			}
//			first.getAttributes().addAll(0, c.getAttributes());
//			if ( i+1 == cList.size())
//				first.setAttributes(first.getAttributes());
//		}
//		return first;
//	}
	
	/**
	 * 
	 * @param list
	 * @return convert nvpair list to GetNameValue<String> list
	 */
	public static List<GetNameValue<String>> toNVList(List<NVPair> list)
    {
		List<GetNameValue<String>> ret = null;
		
		if (list != null)
		{
			ret = new ArrayList<GetNameValue<String>>();
			
			for (NVPair nvp : list)
			{
				ret.add( nvp);
			}
		}	
		
		return ret;
	}

	/**
	 * 
	 * Parse a line that contains a list of name=value separated by & \r or \n
	 * 
	 * @param str
	 * @param nvpSep the name nvpSep value
	 * @param regExp the separator nvp1 rexExp nvp2
	 * @return list nvpair (name sep value regExp)+
	 */
	public static List<NVPair> toNVPairs(String str, String nvpSep, String regExp)
    {
		String[] pairs = SharedStringUtil.parseString(str,regExp, (CharSequence[]) null );
		ArrayList<NVPair> ret = new ArrayList<NVPair>();
		
		for (String p : pairs)
		{
			NVPair nv = toNVPair(p, nvpSep, false);
			
			if (nv != null)
			{
				ret.add(nv);
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param list
	 * @param nameOfNVToBeFiltered
	 * @return something
	 */
	public static List<GetNameValue<String>> filterNV(List<GetNameValue<String>> list, String nameOfNVToBeFiltered) {
		List<GetNameValue<String>> ret = null;
		
		if (nameOfNVToBeFiltered != null)
		{
			for (GetNameValue<String> nvp: list)
			{
				if (!nameOfNVToBeFiltered.equalsIgnoreCase(nvp.getName()))
				{
					if (ret == null)
					{
						ret = new ArrayList<GetNameValue<String>>();
					}
					
					ret.add( nvp);
				}
			}
		}
		
		if (ret == null)
		{
			ret = list;
		}
		
		return ret;
	}

	/**
	 * Return the index of the first occurrence of match[] in buffer[]
	 * @param buffer
	 * @param match
	 * @return index of the match, -1 if no match found
	 */
	public static int indexOf(byte[] buffer, byte[] match) {
		return indexOf(buffer, 0, buffer.length, match, 0, match.length);
	}

	/**
	 * 
	 * @param buffer
	 * @param bufferStartIndex
	 * @param bufferEndIndex
	 * @param match
	 * @param matchOffset
	 * @param matchLen
	 * @return matching index
	 */
	public static int indexOf(byte[] buffer, int bufferStartIndex, int bufferEndIndex, byte[] match, int matchOffset, int matchLen)
    {
		if (matchOffset < 0 || matchLen < 1 || (matchOffset+matchLen) > match.length || bufferEndIndex > buffer.length)
		{
			throw new IndexOutOfBoundsException();
		}
		
		for (int i = bufferStartIndex; i < bufferEndIndex; i++)
		{
			int j = 0;
			
			for ( ; j < matchLen && j+i < bufferEndIndex; j++)
			{
				if (buffer[i + j] != match[matchOffset + j])
				{
					break;
				}
			}
			
			if (j == matchLen)
			{
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param gnv
	 * @return value 
	 */
	public static <V> V getValue(GetNameValue<V> gnv)
    {
		if (gnv != null)
		{
			return gnv.getValue();
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param buffer
	 * @param str
	 * @return matching index
	 */
	public static int indexOf(byte[] buffer, String str)
    {
		return indexOf(buffer, 0, buffer.length, str, 0, str.length(), false);
	}
	
	/**
	 * 
	 * @param buffer
	 * @param str
	 * @return matching index
	 */
	public static int indexOfIgnoreCase(byte[] buffer, String str) {
		return indexOf(buffer, 0, buffer.length, str, 0, str.length(), true);
	}
	
	/**
	 * 
	 * @param buffer
	 * @param bufferStartIndex
	 * @param bufferEndIndex
	 * @param cs
	 * @param csOffset
	 * @param csLen
	 * @param ignoreCase
	 * @return matching index
	 */
	public static int indexOf(byte[] buffer, int bufferStartIndex, int bufferEndIndex, CharSequence cs, int csOffset, int csLen, boolean ignoreCase)
    {
		if (csOffset < 0 || csLen < 1 || (csOffset+csLen) > cs.length()  || bufferEndIndex > buffer.length)
		{
			throw new IndexOutOfBoundsException();
		}
		
		for (int i = bufferStartIndex; i < bufferEndIndex; i++)
		{
			int j = 0;
			
			for ( ; j < csLen && j+i < bufferEndIndex; j++)
			{
				if (ignoreCase)
				{
					if ((buffer[i + j] != Character.toUpperCase(cs.charAt(csOffset + j)) && buffer[i + j] != Character.toLowerCase(cs.charAt(csOffset + j))))
					{
						break;
					}
				}
				else if (buffer[i + j] != cs.charAt(csOffset + j))
				{
					break;
				}
			}

			if (j == csLen)
			{
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * Compares two byte arrays in length-constant time. This comparison method
     * is used so that password hashes cannot be extracted from an on-line 
     * system using a timing attack and then attacked off-line.
     * 
     * @param   a       the first byte array
     * @param   b       the second byte array 
     * @return          true if both byte arrays are the same, false if not
	 */
	public static boolean slowEquals(byte[] a, byte[] b) {
		checkIfNulls("one of the byte array is null", a, b);
		int diff = a.length ^ b.length;
		
		for(int i = 0; i < a.length && i < b.length; i++)
			diff |= a[i] ^ b[i];
		return diff == 0;
	}
	
	
	/**
	 * Compares two byte arrays 
     * 
     * @param   a       the first byte array
     * @param   b       the second byte array 
     * @param	length	the length to be compared
     * @return          true if both byte arrays are the same, false if not
	 */
	public static boolean equals(byte[] a, byte[] b, int length) {
		checkIfNulls("one of the byte array is null", a, b);
		if (length < 0 || length > a.length || length > b.length)
		{
			throw new IllegalArgumentException("Invalid length " + length);
		}
		
		
		for(int i = 0; i < length; i++)
		{
			if (a[i] != b[i])
			{
				return false;
			}
		}
		return true;
	}
	
	
	
	

//	/**
//	 * Format a list of NPairs
//	 * @param list
//	 * @return
//	 */
//	public static String format(ArrayList<NVPair> list)
//	{
//		return format(list,"=", false, "&"); 
//	}
//	
//	/**
//	 * This method will convert an ArrayList of NVPair into a single string
//	 * @param list all the NVPair
//	 * @param nameValueSep the name value separator
//	 * @param quotedValue if the value should be quoted
//	 * @param nvPairSep the separator between the nvpairs
//	 * @return the formated string
//	 */
//	public static String format(ArrayList<NVPair> list, String nameValueSep, boolean quotedValue, String nvPairSep)
//	{
//		StringBuilder sb = new StringBuilder();
//		
//		for (int i = 0; i < list.size(); i++)
//		{
//			NVPair nvp = list.get(i);
//			sb.append(format( nvp.getName(), nvp.getValue(), nameValueSep, quotedValue));
//			if (i + 1 < list.size())
//			{
//				sb.append(nvPairSep);
//			}
//		}
//		
//		return sb.toString();
//	}
//	
//	
//	/**
//	 * 
//	 * @param list
//	 * @param sep
//	 * @return
//	 */
//	public static String format(List<?> list, String sep)
//	{
//		StringBuilder sb = new StringBuilder();
//		
//		for (Object o : list)
//		{
//			if (o != null)
//			{
//				if (sb.length() > 0)
//				{
//					sb.append(sep);
//				}
//				
//				sb.append( o);
//			}
//		}
//		
//		return sb.toString();
//	}
//	
//	/**
//	 * 
//	 * @param pair
//	 * @return
//	 */
//	public static <V> String format(GetNameValue<V> pair)
//	{
//		if (pair != null)
//			return format(pair.getName(), pair.getValue());
//		else 
//			return format((String)null, (String)null);
//	}
//	
//	/**
//	 * 
//	 * @param pair
//	 * @param nameValueSep
//	 * @param quotedValue
//	 * @return
//	 */
//	public static <V> String format(GetNameValue<V> pair, String nameValueSep, boolean quotedValue)
//	{
//		return format(pair.getName(), pair.getValue(), nameValueSep,  quotedValue);
//	}
//	
//	/**
//	 * 
//	 * @param name
//	 * @param value
//	 * @return
//	 */
//	public static <V> String format(String name, V value)
//	{
//		return format(name, value, "=", false);
//	}

	/**
	 * 
	 * @param name
	 * @param value
	 * @param nameValueSep
	 * @param quotedValue
	 * @return formatted string name sep quote value quote
	 */
	public static <V> String format(String name, V value, String nameValueSep, boolean quotedValue)
    {
		StringBuilder sb = new StringBuilder();
		
		if (name != null)
		{
			sb.append(name);
			sb.append(nameValueSep);
		}
		
		if (value != null)
		{
			if (quotedValue)
			{
                sb.append('\"');
            }
			
			sb.append(value);
			
			if (quotedValue)
			{
                sb.append('\"');
            }
		}
		
		return sb.toString();
	}

	/**
	 * This method will check value == null then return defaultValue otherwise it will return value.
	 * The value and defaultValue can not be null simultaneously.  
	 * @param value
	 * @param defaultValue
	 * @return override null with default value
	 * @throws NullPointerException if defaultValue == null && value == null
	 */
	public static <V extends Object> V nullToDefault(V value, V defaultValue)
		throws NullPointerException
    {

		if (value == null && defaultValue == null)
		{
			throw new NullPointerException("value and defaultValue can not be set to null simultaneously");
		}
		
		if (value == null)
		{
			return defaultValue;
		}
		
		return value;
	}

	public static long referenceIDToLong(ReferenceID<?> refID)
    {
		if (refID != null && refID.getReferenceID()!= null)
		{
			return Long.parseLong(""+ refID.getReferenceID());
		}
		
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	public static <V> void validate(NVConfig nvc, NVBase<V> nvb, boolean setValue)
    {
		if (nvb != null)
		{
			if (nvb.getValue() == null && nvc.isMandatory())
			{
				throw new NullPointerException("attribute " + nvc + " is a required value can't be null");
			}
			
			ValueFilter<Object, Object> vf = (ValueFilter<Object, Object>) nvc.getValueFilter();
			
			if (vf != null)
			{
				if (setValue)
				{
					V v = nvb.getValue();
					
					if (nvc.isArray() && v instanceof List)
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
						nvb.setValue((V) vf.validate(nvb.getValue()));
					}
				}
				else if (!nvc.isArray())
				{
					vf.validate(nvb.getValue());
				}
			}
		}
	}
	
	public static void validate(NVEntity nve, boolean setValue, boolean validateRecursive)
    {
		NVConfigEntity nvce = (NVConfigEntity) nve.getNVConfig();
		
		for (NVConfig nvc : nvce.getAttributes())
		{
            //Logic needs to be changed, NVEntiy must have reference ID implementation.
            if (nvc != ReferenceIDDAO.NVC_REFERENCE_ID && nvc != ReferenceIDDAO.NVC_USER_ID && nvc != ReferenceIDDAO.NVC_ACCOUNT_ID) {

                if (validateRecursive && nvc instanceof NVConfigEntity)
                {
                    NVConfigEntity nvcetemp = (NVConfigEntity) nvc;

                    if (!nvcetemp.isArray())
                    {
                        NVEntityReference nver = (NVEntityReference) nve.lookup(nvc.getName());
                        validate(nver.getValue(), setValue, validateRecursive);
                    }
                }
                else
                {
                    validate(nvc, nve.lookup(nvc.getName()), setValue);
                }
            }
		}
	}

    /**
     * Returns a copy of the given NVPair list.
     * @param list NVPair list to copy
     * @return copied NVPair list
     */
	public static List<NVPair> copy(List<NVPair> list)
    {
		List<NVPair> ret = new ArrayList<NVPair>();
		
		for (NVPair nvp : list)
		{
			ret.add(copy(nvp));
		}
		
		return ret;
	}

    /**
     * Returns a copy of the given NVPair.
     * @param nvp NVPair to copy
     * @return copied NVPair
     */
	public static NVPair copy(NVPair nvp)
    {
		NVPair ret = new NVPair();
		
		ret.setName(nvp.getName());
		ret.setValue(nvp.getValue());
		ret.setValueFilter(nvp.getValueFilter());
		
		return ret;
	}
	
	public static boolean doesNameExistNVList(List<NVPair> list, String name)
    {
		return lookup(list, name) != null;
	}

	public static <V extends TimeStampInterface> V touch(V ts, CRUD... ops)
    {
		SharedUtil.checkIfNulls("Document info is null.", ts);

		if (ts.getCreationTime() == 0)
		{
			ts.setCreationTime(System.currentTimeMillis());
		}
		
		if (ops == null || ops.length ==0)
		{
			ts.setLastTimeUpdated(System.currentTimeMillis());
			ts.setLastTimeRead(System.currentTimeMillis());
		} else {
			for (CRUD op : ops)
			{
				if (op != null)
				{
					switch(op)
					{
					case CREATE:
						if (ts.getCreationTime() == 0)
						{
							ts.setCreationTime(System.currentTimeMillis());
						}
						break;
					case DELETE:
						break;
					case READ:
						ts.setLastTimeRead(System.currentTimeMillis());
						break;
					case UPDATE:
						ts.setLastTimeUpdated(System.currentTimeMillis());
						break;
					default:
						break;
					}
				}
			}
		}
		
		return ts;
	}
	
	public static boolean doesNVEntityExist(List<? extends NVEntity> list, String referenceID)
    {
		if (list != null && referenceID != null)
		{
			for (NVEntity nve : list)
			{
				if (nve.getReferenceID() != null && nve.getReferenceID().equals(referenceID))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	public static void close(AutoCloseable autoCloseable)
    {
		if (autoCloseable != null)
		{
			try
            {
				autoCloseable.close();
			}
			catch (Exception e)
            {
				
			}
		}
	}
	
	
	
	public static boolean equals(NVEntity nve1, NVEntity nve2)
    {
		if (nve1 != null && nve2 != null 
				&& nve1.getNVConfig().getMetaType() != null 
				&& nve2.getNVConfig().getMetaType() != null
				&& nve1.getNVConfig().getMetaType().equals(nve2.getNVConfig().getMetaType())
				&& nve1.getReferenceID() != null
				&& nve2.getReferenceID() != null
				&& nve1.getReferenceID().equals(nve2.getReferenceID())
			)
		{
			return true;
		}
		
		return false;
	}
	
	
	public static NVStringList toNVStringList(String name, String values[], boolean skipEmptyOrNull)
	{
		NVStringList ret = new NVStringList(name);
		if (values != null)
		{
			for(String val : values)
			{
				if (skipEmptyOrNull)
				{
					val = SharedStringUtil.trimOrNull(val);
					if (val  == null)
						continue;
				}
				ret.getValue().add(val);
			}
		}
		
		
		return ret;
		
		
	}
	
	/**
	 * Checks if given year is a leap year.
	 * A year is a leap year if its a multiple of 400 or a multiple of 4 but not 100.
	 * Example: 2015 (non-leap year) returns false and 2008 (leap year) returns true.
	 * 
	 * @param year
	 * @return true if leap year, false if not
	 */
	public static boolean isLeapYear(int year)
    {
		return (year % 400 == 0) || ((year % 100) != 0 && (year % 4 == 0));
	}

}