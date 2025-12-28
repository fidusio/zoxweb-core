package org.zoxweb.shared.control;

public abstract class MVControllerBase<V>
implements MVController<V>
{
	protected V value;
	
	protected MVControllerBase(V v)
	{
		setValue(v);
	}
	
	public void setValue(V v)
	{
		value = v;
	}
	
	public V getValue()
	{
		return value;
	}
}
