package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SharedStringUtil;

@SuppressWarnings("serial")
public class LowerCaseFilter
	implements ValueFilter<String, String>
{
	public static final LowerCaseFilter SINGLETON = new LowerCaseFilter();
	
	private LowerCaseFilter()
	{
		
	}

	@Override
	public String toCanonicalID() {
		// TODO Auto-generated method stub
		return LowerCaseFilter.class.getSimpleName();
	}

	@Override
	public String validate(String in) throws NullPointerException, IllegalArgumentException {
		in = SharedStringUtil.trimOrNull(in);
		if (in == null)
			throw new NullPointerException("String null or empty");
		// TODO Auto-generated method stub
		return in.toLowerCase();
	}

	@Override
	public boolean isValid(String in) 
	{
		if (SharedStringUtil.trimOrNull(in) != null)
		{
			return true;
		}
		// TODO Auto-generated method stub
		return false;
	}

}
