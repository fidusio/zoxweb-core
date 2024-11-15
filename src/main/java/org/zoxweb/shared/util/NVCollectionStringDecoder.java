package org.zoxweb.shared.util;

public class NVCollectionStringDecoder
	implements DataDecoder<String, NVCollection<String>>
{
	private final String nvpSeparator;
	private final String valueSeparator;
	private final boolean filterEmpty;
	
	public NVCollectionStringDecoder(String nvpSep, String valueSep)
	{
		this(nvpSep, valueSep, false);
	}
	
	
	public NVCollectionStringDecoder(String nvpSep, String valueSep, boolean filterEmpty)
	{
		SUS.checkIfNulls("Null is not allowed", SharedStringUtil.trimOrNull(nvpSep), SharedStringUtil.trimOrNull(valueSep));
		if (nvpSep.equals(valueSep))
		{
			throw new IllegalArgumentException("Name value separator cannot be equal to value separator " + nvpSep);
		}
		nvpSeparator = nvpSep;
		valueSeparator = valueSep;
		this.filterEmpty = filterEmpty;
	}

	@Override
	public NVCollection<String> decode(String input)
		throws NullPointerException, IllegalArgumentException
	{
		SUS.checkIfNulls("Null input", SharedStringUtil.trimOrNull(input));
		String[] splitter = input.split(nvpSeparator);
		if (splitter.length != 2)
		{
			throw new IllegalArgumentException("NVP separator detected more than once or none " + input);
		}
		NVCollection<String> ret = new NVCollection<String>(splitter[0]);
		
		splitter = splitter[1].split(valueSeparator);
		for (String val : splitter)
		{
			if (filterEmpty)
			{
				val = SharedStringUtil.trimOrNull(val);
				if (val != null)
					ret.getValue().add(val);
			}	
			else
				ret.getValue().add(val);
		}
		
		
		// TODO Auto-generated method stub
		return ret;
	}

}
