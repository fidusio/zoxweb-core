package org.zoxweb.shared.http;

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SUS;

public enum HTTPEncoder
    implements GetNameValue<String>, ValueFilter<GetNameValue<String>, String>
{
	
	URL_ENCODED("=", "&")
	{

		@Override
		public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
			if (!isValid(nvp))
			{
				throw new IllegalArgumentException("Invalid NVP:" + nvp);
			}

			return nvp.getName() +
					getNameValueSep() +
					(Object)nvp.getValue();// MN 2025-02-222 DO NOT REMOVE the Object casting It is on purpose to support int and long and other type NOT String
		}

		@Override
		public boolean isValid(GetNameValue<String> nvp) 
		{
			return nvp!=null && nvp.getValue()!=null && nvp.getName() != null;
		}

		@Override
		public String toCanonicalID() {
			return null;
		}
	},
	URI_REST_ENCODED(null, "/")
	{

		@Override
		public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
			if (!isValid(nvp))
			{
				throw new IllegalArgumentException("Invalid NVP:" + nvp);
			}

			return nvp.getValue();
		}

		@Override
		public boolean isValid(GetNameValue<String> nvp) {
			return nvp!=null && nvp.getValue()!=null;
		}

		@Override
		public String toCanonicalID() {
			return null;
		}
	},
	HEADER("=", "; ")
	{

		@Override
		public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
			if (!isValid(nvp))
			{
				throw new IllegalArgumentException("Invalid NVP:" + nvp);
			}

			return nvp.getName() +
					getNameValueSep() +
					(Object)nvp.getValue();// MN 2025-02-222 DO NOT REMOVE the Object casting It is on purpose to support int and long and other type NOT String
		}

		@Override
		public boolean isValid(GetNameValue<String> nvp) {
			return nvp!=null && nvp.getValue()!=null;
		}
		@Override
		public String toCanonicalID() {
			return null;
		}
	}
	
	;
	
	private final String paramSep;
	private final String nvSep;
	
	HTTPEncoder(String nvSep, String sep)
	{
		paramSep = sep;
		this.nvSep = nvSep;
	}

	
	@Override
	public String getName() {
		return name().toLowerCase();
	}

	/**
	 * Return the value of the parameter separator
	 */
	@Override
	public String getValue() {
		return paramSep;
	}
	
	public String getNameValueSep()
	{
		return nvSep;
	}
	
	
	private StringBuilder format_int(StringBuilder sb, GetNameValue<String> nvp)
	{
		if (sb == null)
		{
			sb = new StringBuilder();
		}

		if (isValid(nvp))
		{
			if (sb.length() > 0)
			{
				int index = sb.length() - paramSep.length();
				if (index > 0){
					boolean appendSep = false;
					for (int i = 0; i < paramSep.length(); i++)
					{
						if(sb.charAt(index+i) != paramSep.charAt(i))
						{
							appendSep = true;
							break;
						}
					}
					if(appendSep)
					{
						sb.append(paramSep);
					}
				}

			}
			sb.append(validate(nvp));
		}
		return sb;
	}

	private StringBuilder format_int(StringBuilder sb, String str)
	{
		if (sb == null)
		{
			sb = new StringBuilder();
		}

		if (!SUS.isEmpty(str))
		{
			if (sb.length() > 0)
			{
				int index = sb.length() - paramSep.length();
				if (index > 0){
					boolean appendSep = false;
					for (int i = 0; i < paramSep.length(); i++)
					{
						if(sb.charAt(index+i) != paramSep.charAt(i))
						{
							appendSep = true;
							break;
						}
					}
					if(appendSep)
					{
						sb.append(paramSep);
					}
				}
			}
			sb.append(str);
		}
		return sb;
	}

	@SuppressWarnings("unchecked")
    public String format(GetNameValue<String> ...nvps)
	{
		return format(null, nvps);
	}
	
	/**
	 * Format the parameters 
	 * @param sb if null a local value will be created 
	 * @param nvps parameters to be formated
	 * @return the formatted parameters as string
	 */
	@SuppressWarnings("unchecked")
    public String format(StringBuilder sb, GetNameValue<String> ...nvps)
	{
		if (sb == null)
		{
			sb = new StringBuilder();
		}
		for (GetNameValue<String> nvp :nvps)
		{ 
			sb = format_int(sb, nvp);
		}
		
		return sb.toString();
	}

	public String format(String ...params)
	{
		return format(null, params);
	}


	public String format(StringBuilder sb, String ...params)
	{
		if (sb == null)
		{
			sb = new StringBuilder();
		}
		for (String str :params)
		{
			sb = format_int(sb, str);
		}

		return sb.toString();
	}



	
}