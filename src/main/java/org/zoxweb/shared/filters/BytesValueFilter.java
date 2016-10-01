package org.zoxweb.shared.filters;

import java.io.UnsupportedEncodingException;




import org.zoxweb.shared.util.BytesValue;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVBase;
import org.zoxweb.shared.util.NVBlob;
import org.zoxweb.shared.util.NVDouble;
import org.zoxweb.shared.util.NVFloat;
import org.zoxweb.shared.util.NVInt;
import org.zoxweb.shared.util.NVLong;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class BytesValueFilter
	implements ValueFilter<Object, byte[]>
{
	
	public static final BytesValueFilter SINGLETON = new BytesValueFilter();
	
	private BytesValueFilter()
	{
	}

	@Override
	public String toCanonicalID()
	{
		return BytesValueFilter.class.getName();
	}	
	
	@SuppressWarnings("rawtypes")
	@Override
	public byte[] validate(Object in)
			throws NullPointerException, IllegalArgumentException
	{
		SharedUtil.checkIfNulls("Null value", in);
		if (in instanceof GetValue)
		{
			in = ((GetValue)in).getValue();
		}
		
		SharedUtil.checkIfNulls("GetValue.getValue() null", in);
		if (in instanceof String)
		{
			return SharedStringUtil.getBytes((String)in);
		}
		else if (in instanceof byte[])
		{
			return (byte[])in;
		}
		else if (in instanceof Boolean)
		{
			byte[] ret = new byte[1];
			
			if ((Boolean)in)
			{
				ret[0] = 1;
			}
			else
			{
				ret[0] = 0;
			}
			return ret;
		}
		else if (in instanceof Integer)
		{
			return BytesValue.INT.toBytes((Integer)in);
		}
		else if (in instanceof Long)
		{
			return  BytesValue.LONG.toBytes((Long)in);
		}
		else if (in instanceof Float)
		{
			return  BytesValue.FLOAT.toBytes((Float)in);
		}
		else if (in instanceof Double)
		{
			return  BytesValue.DOUBLE.toBytes((Double)in);
		}
		
		throw new IllegalArgumentException("Unsupported type " + in.getClass().getName());
	}

	@Override
	public boolean isValid(Object in)
	{
		try
		{
			validate(in);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	
	public static void setByteArrayToNVBase(NVBase<?> in, byte[] value)
	{
		SharedUtil.checkIfNulls("Null NVBase<?>", in, value);
		if (in instanceof NVPair)
		{
			try 
			{
				((NVPair)in).setValue(new String(value, SharedStringUtil.UTF_8));
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		else if (in instanceof NVBlob)
		{
			((NVBlob)in).setValue(value);
		}
		else if (in instanceof NVInt)
		{
			((NVInt)in).setValue(BytesValue.INT.toValue(value));
		}
		else if (in instanceof NVLong)
		{
			((NVLong)in).setValue(BytesValue.LONG.toValue(value));
		}
		else if (in instanceof NVFloat)
		{
			((NVFloat)in).setValue(BytesValue.FLOAT.toValue(value));
		}
		else if (in instanceof NVDouble)
		{
			((NVDouble)in).setValue(BytesValue.DOUBLE.toValue(value));
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type " + in.getClass().getName());
		}
	}
	

	public static Object bytesToValue(Class<?> in, byte[] value)
	{
		SharedUtil.checkIfNulls("Null values", in, value);
		return bytesToValue(in, value, 0, value.length);
	}
	
	public static Object bytesToValue(Class<?> in, byte[] value, int offset, int length)
	{
		SharedUtil.checkIfNulls("Null values", in, value);
		if (String.class.equals(in))
		{
			
			try 
			{
				return new String(value, offset, length, SharedStringUtil.UTF_8);
			}
			catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalArgumentException(e.getMessage());
			}
			
		}
		else if (byte[].class.equals(in))
		{
			byte[] ret = new byte[length];
			System.arraycopy(value, offset, ret, 0, length);
			return ret;
		}
		else if (Boolean.class.equals(in) || boolean.class.equals(in))
		{
			return value[offset] == 0;
		}
		else if (Integer.class.equals(in) || int.class.equals(in))
		{
			return BytesValue.INT.toValue(value, offset, Integer.SIZE/Byte.SIZE);
		
		}
		else if (Long.class.equals(in) || long.class.equals(in))
		{
			return BytesValue.LONG.toValue(value, offset, Long.SIZE/Byte.SIZE);
		
		}
		else if (Float.class.equals(in) || float.class.equals(in))
		{
			return BytesValue.FLOAT.toValue(value, offset,  Float.SIZE/Byte.SIZE);
		}
		else if (Double.class.equals(in) || double.class.equals(in))
		{
			return BytesValue.DOUBLE.toValue(value, offset, Double.SIZE/Byte.SIZE);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type " + in.getClass().getName());
		}
	}
	
	
//	public static Object byteArrayToObject(Class<?> in, byte[] value)
//	{
//		SharedUtil.checkIfNulls("Null values", in, value);
//		return byteArrayToObject(in, value, 0, value.length);
//	}
//	
//	
//	
//	public static Object byteArrayToObject(Class<?> in , byte[] value, int offset, int length)
//	{
//		SharedUtil.checkIfNulls("Null values", in, value);
//		if (String.class.equals(in))
//		{
//			
//			try 
//			{
//				return new String(value, offset, length, SharedStringUtil.UTF_8);
//			}
//			catch (UnsupportedEncodingException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				throw new IllegalArgumentException(e.getMessage());
//			}
//			
//		}
//		else if (byte[].class.equals(in))
//		{
//			byte[] ret = new byte[length];
//			System.arraycopy(value, offset, ret, 0, length);
//			return ret;
//		}
//		else if (Boolean.class.equals(in) || boolean.class.equals(in))
//		{
//			return value[offset] == 0;
//		}
//		else if (Integer.class.equals(in) || int.class.equals(in))
//		{
//			return BytesValue.INT.toValue(value, offset, Integer.SIZE/Byte.SIZE);
//		
//		}
//		else if (Long.class.equals(in) || long.class.equals(in))
//		{
//			return BytesValue.LONG.toValue(value, offset, Long.SIZE/Byte.SIZE);
//		
//		}
//		else if (Float.class.equals(in) || float.class.equals(in))
//		{
//			return BytesValue.FLOAT.toValue(value, offset,  Float.SIZE/Byte.SIZE);
//		}
//		else if (Double.class.equals(in) || double.class.equals(in))
//		{
//			return BytesValue.DOUBLE.toValue(value, offset, Double.SIZE/Byte.SIZE);
//		}
//		else
//		{
//			throw new IllegalArgumentException("Unsupported type " + in.getClass().getName());
//		}
//	}
	
}