package org.zoxweb.shared.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class NVStringSet
extends NVBase<Set<String>>
{
	/**
	 * The default constructor (Java Bean compliant).
	 */
	public NVStringSet()
	{
		super((String)null, new HashSet<String>());
	}



	/**
	 * This constructor instantiates NVLongList based on name value.
	 * @param name of the list
	 * @param value list values
	 */
	public NVStringSet(String name, Set<String> value)
	{
		super(name, value);
	}

	/**
	 * Construct String list with name and string arrays
	 * @param name of string ling
	 * @param values values to be set
	 */
	public NVStringSet(String name, String ...values)
	{
		this(name);
		setValues(values);
	}

	/**
	 * This constructor instantiates NVLongList based on name value.
	 * @param name
	 */
	public NVStringSet(String name)
	{
		super(name, new HashSet<String>());
	}


	public void setValues(String ... vals)
	{
		value.clear();
		if(vals != null && vals.length > 0)
			value.addAll(Arrays.asList(vals));
	}

	public String[] getValues()
	{
		return value.toArray(new String[value.size()]);
	}

	public boolean contains(String val)
	{
		return getValue().contains(val);
	}


	public void add(String value)
	{
		this.value.add(value);
	}
	  
}
