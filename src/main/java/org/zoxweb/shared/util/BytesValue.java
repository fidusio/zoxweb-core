/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

/**
 *
 * @param <V>
 */
public interface BytesValue<V>
{
    /**
     *
     * @param v
     * @return
     */
	byte[] toBytes(V v);
	
	//byte[] toBytes(V v, byte[] retBuffer, int retStartIndex);

	byte[] toBytes(byte[] retBuffer, int retStartIndex, @SuppressWarnings("unchecked") V ...v);
    /**
     *
     * @param bytes
     * @return
     */
	V toValue(byte[] bytes);

    /**
     *
     * @param bytes
     * @param offset
     * @param length
     * @return
     */
	V toValue(byte[] bytes, int offset, int length);




	public static final BytesValue<Short> SHORT = new  BytesValue<Short>()
	{
		public byte[] toBytes(Short in)
		{
//			int val = in.intValue();
//			byte buffer[] = new byte[Short.SIZE/Byte.SIZE];
//
//			for (int i = buffer.length; i > 0; i--)
//			{
//				buffer[i-1] = (byte)val;
//				val = val >> 8;
//			}
//
//			return buffer;
			
			return toBytes(null, 0, in);
		}
		
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Short ...ins)
		{			
			int size = Short.SIZE/Byte.SIZE;
			if (retBuffer == null)
			{
				retBuffer = new byte[size*ins.length];
				retStartIndex = 0;
			}

			
			for (int j=0; j < ins.length; j++)
			{
				int val = ins[j].intValue();
				int index = retStartIndex + j*size;
				for (int i = (index + size) ; i > index; i--)
				{
					retBuffer[i-1] = (byte)val;
					val = val >> 8;
				}
			}

			return retBuffer;
		}

		public Short toValue(byte buffer[], int offset, int length)
		{
			int value = 0;

			for (int i = offset; i < length; i++)
			{
				value = (value << 8) + (buffer[i] & 0xff);
			}

			return (short)value;
		}

		@Override
		public Short toValue(byte[] bytes)
		{
			return toValue(bytes, 0, bytes.length);
		}
	};

	
	public static final BytesValue<Integer> INT = new  BytesValue<Integer>()
	{
		public byte[] toBytes(Integer in)
		{
			return toBytes(null, 0, in);
		}
		
		public Integer toValue(byte buffer[], int offset, int length)
		{
			int value = 0;

			for (int i = offset; i < length; i++)
			{
				value = (value << 8) + (buffer[i] & 0xff);
			}

			return value;
		}

		@Override
		public Integer toValue(byte[] bytes) 
		{
			return toValue(bytes, 0, bytes.length);
		}

		@Override
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Integer ...ins) {
			
			int size = Integer.SIZE/Byte.SIZE;
			if (retBuffer == null)
			{
				retBuffer = new byte[size*ins.length];
				retStartIndex = 0;
			}

			
			for (int j=0; j < ins.length; j++)
			{
				int val = ins[j];
				int index = retStartIndex + j*size;
				for (int i = (index + size) ; i > index; i--)
				{
					retBuffer[i-1] = (byte)val;
					val = val >> 8;
				}
			}

			return retBuffer;
		}
	};
	
	public static final BytesValue<Long> LONG = new BytesValue<Long>()
	{
		public byte[] toBytes(Long in)
		{			
			return toBytes(null, 0, in);
		}
		
		

		
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Long ...ins) {
			
			int size = Long.SIZE/Byte.SIZE;
			if (retBuffer == null)
			{
				retBuffer = new byte[size*ins.length];
				retStartIndex = 0;
			}
			
			for (int j=0; j < ins.length; j++)
			{
				long val = ins[j];
				int index = retStartIndex + j*size;
				for (int i = (index + size) ; i > index; i--)
				{
					retBuffer[i-1] = (byte)val;
					val = val >> 8;
				}
			}

			return retBuffer;
		}
		
		
		

		/**
		 * @param bytes array
		 * @param offset
		 * @param length
		 * @return Long converted value
		 */
		public Long toValue(byte[] bytes, int offset, int length)
		{
			long value = 0;

			for (int i = offset; i < length; i++)
			{
				value = (value << 8) + (bytes[i] & 0xff);
			}

			return value;
		}

		@Override
		public Long toValue(byte[] bytes)
		{
			return toValue(bytes, 0, bytes.length);
		}

	};
	
	
	public static final BytesValue<Float> FLOAT = new BytesValue<Float>()
	{
		public byte[] toBytes(Float in)
		{
//			int val = Float.floatToIntBits(in);
//			byte buffer[] = new byte[Float.SIZE/Byte.SIZE];
//			
//			for (int i = buffer.length; i > 0; i--)
//			{
//				buffer[i-1] = (byte)val;
//				val = val >> 8;
//			}
//
//			return buffer;	
			
			return toBytes(null, 0, in);
		}

		/**
		 * @param bytes
		 * @param offset
		 * @param length
		 * @return float value
		 */
		public Float toValue(byte bytes[], int offset, int length)
		{
			int value = 0;

			for (int i = offset; i < length; i++)
			{
				 value = (value << 8) + (bytes[i] & 0xff);
			}

			return Float.intBitsToFloat(value);
		}
		
		public Float toValue(byte[] bytes)
		{
			return toValue(bytes, 0, bytes.length);
		}

		@Override
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Float ...ins) {
			// TODO Auto-generated method stub
//			int val = Float.floatToIntBits(in);
//			int size = Float.SIZE/Byte.SIZE;
//			if (retBuffer == null)
//			{
//				retBuffer = new byte[size];
//			}
//
//			for (int i = retStartIndex + size; i > retStartIndex; i--)
//			{
//				retBuffer[i-1] = (byte)val;
//				val = val >> 8;
//			}
//
//			return retBuffer;
//			
//			
			int size = Float.SIZE/Byte.SIZE;
			if (retBuffer == null)
			{
				retBuffer = new byte[size*ins.length];
				retStartIndex = 0;
			}

			
			for (int j=0; j < ins.length; j++)
			{
				int val = Float.floatToIntBits(ins[j]);
				int index = retStartIndex + j*size;
				for (int i = (index + size) ; i > index; i--)
				{
					retBuffer[i-1] = (byte)val;
					val = val >> 8;
				}
			}

			return retBuffer;
			
			
			
		}

	};

	public static final BytesValue<Double> DOUBLE = new BytesValue<Double>()
	{
		public byte[] toBytes(Double in)
		{
//			long val = Double.doubleToLongBits(in);
//			byte buffer[] = new byte[Double.SIZE/Byte.SIZE];
//
//			for (int i = buffer.length; i > 0; i--)
//			{
//				buffer[i-1] = (byte)val;
//				val = val >> 8;
//			}
//
//			return buffer;
			return toBytes(null, 0, in);
		}
		
		@Override
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Double ...ins) {
//			// TODO Auto-generated method stub
//			long val = Double.doubleToLongBits(in);
//			int size = Double.SIZE/Byte.SIZE;
//			if (retBuffer == null)
//			{
//				retBuffer = new byte[size];
//			}
//
//			for (int i = retStartIndex + size; i > retStartIndex; i--)
//			{
//				retBuffer[i-1] = (byte)val;
//				val = val >> 8;
//			}
//
//			return retBuffer;
			
			
			int size = Double.SIZE/Byte.SIZE;
			if (retBuffer == null)
			{
				retBuffer = new byte[size*ins.length];
				retStartIndex = 0;
			}

			
			for (int j=0; j < ins.length; j++)
			{
				long val = Double.doubleToLongBits(ins[j]);
				int index = retStartIndex + j*size;
				for (int i = (index + size) ; i > index; i--)
				{
					retBuffer[i-1] = (byte)val;
					val = val >> 8;
				}
			}

			return retBuffer;
		}
	
	
		public Double toValue(byte bytes[], int offset, int length)
		{
			long value = 0;

			for (int i = offset; i < length; i++)
			{
				 value = (value << 8) + (bytes[i] & 0xff);
			}

			return Double.longBitsToDouble(value);
		}
		
		public Double toValue(byte[] bytes)
		{
			return toValue(bytes, 0, bytes.length);
		}

	};
		
}