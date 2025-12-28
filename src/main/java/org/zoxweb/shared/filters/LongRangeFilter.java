package org.zoxweb.shared.filters;

@SuppressWarnings("serial")
public class LongRangeFilter extends RangeFilter<Long> 
{

	public LongRangeFilter(long lowerLimit, boolean lowerLimitInclusive, long upperLimit, boolean upperLimitInclusive) 
	{
		super(lowerLimit, lowerLimitInclusive, upperLimit, upperLimitInclusive);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Long validate(Long in) throws NullPointerException, IllegalArgumentException {
		// TODO Auto-generated method stub
		if (!isValid(in))
		{
			throw new IllegalArgumentException(in + " is out of range");
		}
		
		return in;
	}

	@Override
	public boolean isValid(Long in) 
	{
		
		boolean stat = false;
		if (lowerLimitInclusive && in >= lowerLimit)
		{
			stat = true;
		}
		else if (in > lowerLimit)
		{
			stat = true;
		}
		
		// TODO Auto-generated method stub
		if (stat && upperLimitInclusive && in <= upperLimit )
		{
			return true;
		}
		else if (stat && in < upperLimit)
		{
			return true;
		}
			
			
		return false;
	}



}
