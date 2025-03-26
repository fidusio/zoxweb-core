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
     * @param v to be converted
     * @return byte array
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
	V toValue(byte[] bytes, int offset);




	public static final BytesValue<Short> SHORT = new  BytesValue<Short>()
	{
		public byte[] toBytes(Short in)
		{
			
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

		public Short toValue(byte[] bytes, int offset, int length)
		{
//			int value = 0;
//			length += offset;
//			for (int i = offset; i < length; i++)
//			{
//				value = (value << 8) | (buffer[i] & 0xff);
//			}
//
//			return (short)value;
			return LONG.toValue(bytes, offset, length).shortValue();
		}
		public Short toValue(byte[] bytes, int offset)
		{
			return LONG.toValue(bytes, offset, Short.BYTES).shortValue();
		}

		@Override
		public Short toValue(byte[] bytes)
		{
			return LONG.toValue(bytes, 0, Short.BYTES).shortValue();
//			int i = (bytes[0] & 0xFF) << 8 | (short) (bytes[1] & 0xFF);
//			return (short)i;
		}
	};

	
	public static final BytesValue<Integer> INT = new  BytesValue<Integer>()
	{
		public byte[] toBytes(Integer in)
		{
			return toBytes(null, 0, in);
		}
		
		public Integer toValue(byte bytes[], int offset, int length)
		{
//			int value = 0;
//			length += offset;
//			for (int i = offset; i < length; i++)
//			{
//				value = (value << 8) | (buffer[i] & 0xff);
//			}
			return LONG.toValue(bytes, offset, length).intValue();
		}

		public Integer toValue(byte[] bytes, int offset)
		{
			return LONG.toValue(bytes, offset, Integer.BYTES).intValue();
		}

		@Override
		public Integer toValue(byte[] bytes) 
		{
			return LONG.toValue(bytes, 0, Integer.BYTES).intValue();
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

		public Long toValue(byte[] buffer, int offset)
		{
			return toValue(buffer, offset, Long.BYTES);
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
			length += offset;
			for (int i = offset; i < length; i++)
			{
				value = (value << 8) | (bytes[i] & 0xff);
			}

			return value;
		}

		@Override
		public Long toValue(byte[] bytes)
		{

			//return toValue(bytes, 0, bytes.length);
//			long ret = (bytes[0] & 0xFF) | (bytes[1] & 0xFF)  << 8 | (bytes[2] & 0xFF)  << 8 | (bytes[3] & 0xFF) << 8|
//					   (bytes[4] & 0xFF) << 8 | (bytes[5] & 0xFF) << 8 | (bytes[6] & 0xFF)  << 8 | (bytes[7] & 0xFF) << 8;
			long ret = bytes[0] & 0xFF;
			ret  = ret << 8 | (bytes[1] & 0xFF);
			ret  = ret << 8 | (bytes[2] & 0xFF);
			ret  = ret << 8 | (bytes[3] & 0xFF);
			ret  = ret << 8 | (bytes[4] & 0xFF);
			ret  = ret << 8 | (bytes[5] & 0xFF);
			ret  = ret << 8 | (bytes[6] & 0xFF);
			ret  = ret << 8 | (bytes[7] & 0xFF);

			return ret;
		}

	};
	
	
	public static final BytesValue<Float> FLOAT = new BytesValue<Float>()
	{
		public byte[] toBytes(Float in) {
			return toBytes(null, 0, in);
		}

		/**
		 * @param bytes
		 * @param offset
		 * @param length
		 * @return float value
		 */
		public Float toValue(byte bytes[], int offset, int length) {
			int value = 0;
			length += offset;
			for (int i = offset; i < length; i++)
			{
				 value = (value << 8) + (bytes[i] & 0xff);
			}

			return Float.intBitsToFloat(value);
		}

		public Float toValue(byte[] buffer, int offset)
		{
			return toValue(buffer, offset, Float.BYTES);
		}
		
		public Float toValue(byte[] bytes)
		{
			return toValue(bytes, 0, bytes.length);
		}

		@Override
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, Float ...ins) {

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


		public Double toValue(byte[] buffer, int offset)
		{
			return toValue(buffer, offset, Double.BYTES);
		}
	
		public Double toValue(byte bytes[], int offset, int length)
		{
			long value = 0;

			length += offset;
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

	public static final BytesValue<String> STRING = new BytesValue<String>()
	{


		@Override
		public byte[] toBytes(String s) {
			return SharedStringUtil.getBytes(s);
		}

		@Override
		public byte[] toBytes(byte[] retBuffer, int retStartIndex, String... v) {
			for (String str : v)
			{
				byte toAdd[] = toBytes(str);
				for(int i = 0; i < toAdd.length; i++)
				{
					retBuffer[i+retStartIndex] = toAdd[i];
				}
				retStartIndex += toAdd.length;
			}
			return retBuffer;
		}

		@Override
		public String toValue(byte[] bytes) {
			return SharedStringUtil.toString(bytes);
		}
		public String toValue(byte[] buffer, int offset)
		{
			throw new IllegalArgumentException("Not supported");

		}

		@Override
		public String toValue(byte[] bytes, int offset, int length) {
			return SharedStringUtil.toString(bytes, offset, length);
		}
	};
}