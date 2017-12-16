package org.zoxweb.shared.util;

import org.zoxweb.shared.data.AppIDDAO;

public class AppIDURI 
{
	private AppIDDAO appID;
	private String rest[];
	
	private AppIDURI()
	{
		
	}
	
	public AppIDDAO getAppIDDAO()
	{
		return appID;
	}
	
	public String[] getRest()
	{
		return rest;
	}
	
	
	public static AppIDURI parse(String path)
	{
		String tokens[] = path.split("/");
		
		
		if(tokens.length < 2)
			throw new IllegalArgumentException("path too short");
		
		AppIDURI ret = new AppIDURI();
		int index = 0;
		ret.appID = new AppIDDAO(tokens[index++], tokens[index++]);
		
		if (tokens.length > index)
		{
			ret.rest = new String[tokens.length - index];
			for(int marker = index;index < tokens.length; index++)
			{
				ret.rest[index - marker] = tokens[index];
			}
		}
		
		
		
		return ret;
			
		
	}
}
