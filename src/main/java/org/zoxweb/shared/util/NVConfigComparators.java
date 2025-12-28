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
package org.zoxweb.shared.util;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

/**
 * This enum contains comparison methods for the following 
 * types: string, long, integer, double, float, or boolean which
 * are used to compare two objects of the same type.
 * @author mzebib
 *
 */
public enum NVConfigComparators
{
	/**
	 * This defines the comparator of string type objects.
	 */
	STRING(new Comparator<Object>()
			{

				public int compare(Object o1, Object o2) 
				{
					int val = 0;
					
					if (o1!= null && o2 == null)
					{
						val = +1;
					}
					else if (o1== null && o2 != null)
					{
						val = -1;
					}
					else if (o1!= null && o2!=null)
					{
						val = (((String) o1).toLowerCase()).compareTo(((String) o2).toLowerCase());
					}
					
					return val;
				}
			
			}, String.class),

	/**
	 * This defines the comparator of long type objects.
	 */
	LONG(new Comparator<Object>()
			{

				public int compare(Object o1, Object o2) 
				{
					int val = 0;
					
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{
						val = (int) ((Long)o1 - (Long)o2);
					}
					
					return val;
				}
			
			}, Long.class, long.class),
	
	/**
	 * This defines the comparator of integer type objects.
	 */
	INT(new Comparator<Object>()
			{

				public int compare(Object o1, Object o2) 
				{
					int val = 0;
					
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{
						val = (int) ((Integer)o1 - (Integer)o2);
					}
					
					return val;
				}
			
			}, Integer.class, int.class),	

	/**
	 * This defines the comparator of double type objects.
	 */		
	DOUBLE(new Comparator<Object>()
			{

				public int compare(Object o1, Object o2) 
				{
					int val = 0;
							
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{
						val = (int) ((Double)o1 - (Double)o2);
					}
					
					return val;
				}
					
			}, Double.class, double.class),	
					
	/**
	 * This defines the comparator of float type objects.
	 */			
	FLOAT(new Comparator<Object>()
			{
	
				public int compare(Object o1, Object o2) 
				{
					int val = 0;
									
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{			
						val = (int) ((Float)o1 - (Float)o2);
					}
					
					return val;
				}
							
			}, Float.class, float.class),	
	
	/**
	 * This defines the comparator of boolean type objects.
	 */
	BOOLEAN(new Comparator<Object>()
			{
		
				public int compare(Object o1, Object o2) 
				{
					int val = 0;
											
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2!= null)
					{				
						val = ((Boolean)o1).compareTo((Boolean)o2);
					}
					
					return val;
				}
									
			}, Boolean.class, boolean.class),	
			
	/**
	 * This defines the comparator of enum type objects.
	 */	
	ENUM(new Comparator<Object>()
			{
				
				public int compare(Object o1, Object o2) 
				{
					int val = 0;
					
					if (o1 != null && o2 == null)
					{
						val = +1;
					}
					else if (o1 == null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{
						val = ((Enum<?>)o1).ordinal() - ((Enum<?>)o2).ordinal();
					}
						
					return val;
				}
		
			}, Enum.class),
			
			
	/**
	 * This defines the comparator of BigDecimal type objects.
	 */			
	BIG_DECIMAL(new Comparator<Object>()
			{
	
				public int compare(Object o1, Object o2) 
				{
					int val = 0;
									
					if (o1 !=  null && o2 == null)
					{
						val = +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						val = -1;
					}
					else if (o1 != null && o2 != null)
					{			
						val = ((BigDecimal) o1).compareTo((BigDecimal) o2);
					}
					
					return val;
				}
							
			}, BigDecimal.class),
			
	/**
	 * This defines the comparator of Date type objects.
	 */
	DATE(new Comparator<Object>()
			{
				public int compare(Object o1, Object o2) 
				{					
					if (o1 !=  null && o2 == null)
					{
						return +1;
					}
					else if (o1 ==  null && o2 != null)
					{
						return -1;
					}
					else if (o1 != null && o2 != null)
					{
						long val = getLong(o1) - getLong(o2);
						
						if (val > 0)
						{
							return +1;
						}
						else if (val < 0)
						{
							return -1;
						}
						else
						{
							return 0;
						}
					}
					
					return 0;
				}
				
				private long getLong(Object o)
				{
					if (o instanceof Date)
					{
						return ((Date) o).getTime();
					}
					//else if (o instanceof Long)
					{
						return ((Long) o).longValue();
					}
				}
			
			}, Date.class),
			
	;

	
	private final Class<?>[] classes;
	/**
	 * This constructor sets up the comparator.
	 */
	public final Comparator<Object> COMPARATOR;
	
	NVConfigComparators(Comparator<Object> comparator, Class<?> ... classes)
	{
		this.COMPARATOR =  comparator;
		this.classes = classes;
	}
	
	
	/**
	 * This method returns the appropriate comparator type
	 * of an object of NVConfig type.
	 * @param nvc
	 * @return matching comparator
	 */
	public static Comparator<Object> lookupComparator(NVConfig nvc)
	{
		if (nvc != null)
		{
			Class<?> c = nvc.getMetaType();
			
			if (c.isEnum())
			{
				return ENUM.COMPARATOR;
			}
			
			for (NVConfigComparators nvcComparator : NVConfigComparators.values())
			{
				for (Class<?> clazz : nvcComparator.classes)
				{					
					if (clazz.equals(c))
					{
						return nvcComparator.COMPARATOR;
					}
				}
			}			
		}
		
		return null;
	}

	
	
}
