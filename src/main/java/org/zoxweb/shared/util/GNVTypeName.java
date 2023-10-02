package org.zoxweb.shared.util;

import org.zoxweb.shared.util.Const.GNVType;

public class GNVTypeName
	implements GetName
{
	private final GNVType type;
	private final String name;
	
	
	public GNVTypeName(GNVType type, String name)
	{
		this.type = type;
		this.name = name;
	}




	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	
	public GNVType getType()
	{
		return type;
	}

	@Override
	public String toString() {
		return "GNVTypeName{" +
				"type=" + type +
				", name='" + name + '\'' +
				'}';
	}
}
