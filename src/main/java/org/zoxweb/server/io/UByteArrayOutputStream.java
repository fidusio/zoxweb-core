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
package org.zoxweb.server.io;

import java.io.*;
import java.util.Arrays;

import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The UByteArrayOutputStream class.
 */
public class UByteArrayOutputStream 
	extends ByteArrayOutputStream 
	implements Externalizable, InternalBufferAccess
{

	/**
	 * Create a byte array output stream.
	 */
	public UByteArrayOutputStream() 
	{
		super();
	}

	/**
	 * 
	 * @param size
	 */
	public UByteArrayOutputStream(int size)
	{
		super(size);
	}

	
	public UByteArrayOutputStream(String str)
	{
		this(SharedStringUtil.getBytes(str));
	}
	
	/**
	 * 
	 * @param buffer
	 */
	public UByteArrayOutputStream(byte[] buffer)
	{
		this(buffer, 0, buffer.length);
	}

	/**
	 * 
	 * @param buffer
	 * @param offset
	 * @param len
	 */
	public UByteArrayOutputStream(byte[] buffer , int offset, int len)
	{
		super(buffer.length);
		write(buffer, offset, len);
	}
	
	
	
	/**
	 * 
	 */
	public synchronized void writeExternal(ObjectOutput out) throws IOException 
	{
		out.writeInt(count);
		byte[] newBuffer = new byte[ count];
		System.arraycopy( buf, 0, newBuffer, 0, count);
		((ObjectOutputStream)out).writeUnshared( newBuffer);
		
		out.flush();		
	}

	public synchronized void readExternal(ObjectInput in) 
			throws IOException, ClassNotFoundException 
	{
		count = in.readInt();
		buf = (byte[])((ObjectInputStream)in).readUnshared();
		
	}
	
	/**
	 * This method will transfer the data to ByteArrayInputStream and reset the UByteArrayOutputStream
	 * @return ByteArrayInputStream
	 */
	public synchronized ByteArrayInputStream toByteArrayInputStream()
	{
		byte[] tempBuf = buf;
		int tempCount = size();
		buf = new byte[32];
		reset();
		
		return new ByteArrayInputStream(tempBuf, 0 , tempCount);
		
	}

	/**
	 * @param index 
	 * @return the char at index.
	 */
	public char charAt(int index) 
	{
		return (char) buf[index];
	}

	/**
	 * @param index 
	 * @return the byte at index.
	 */
	public byte byteAt(int index) 
	{
		return buf[index];
	}
	
	/**
	 * Return the first index of matching bytes in contained within the stream
	 * @param match
	 * @return index of the match, -1 no match found
	 */
	public int indexOf(byte[] match)
	{
		return SharedUtil.indexOf(getInternalBuffer(), 0, size(), match, 0, match.length);
	}
	
	/**
	 * 
	 * @param startAt
	 * @param match
	 * @param offset
	 * @param length
	 * @return index
	 */
	public int indexOf(int startAt, byte[] match, int offset, int length)
	{
		return SharedUtil.indexOf(getInternalBuffer(), startAt, size(), match, offset, length);
	}
	
	public int indexOf(String str)
	{
		return SharedUtil.indexOf(getInternalBuffer(), 0, size(), str, 0, str.length(), false);
	}
	
	public int indexOfIgnoreCase(String str)
	{
		return SharedUtil.indexOf(getInternalBuffer(), 0, size(), str, 0, str.length(), true);
	}
	
	/**
	 * @param index 
	 * @return the integer at index.
	 */
	public int intAt(int index)
	{
		return buf[index];
	}

	public static UByteArrayOutputStream diff(UByteArrayOutputStream baos1, UByteArrayOutputStream baos2)
	{
		UByteArrayOutputStream ret = new UByteArrayOutputStream();
		// check condition
		{
			byte[] buff1 = baos1.getInternalBuffer();
			byte[] buff2 = baos2.getInternalBuffer();
			for (int i = 0; i < baos2.size(); i++)
			{
				byte diff = buff2[i];
				
				if (i < baos1.size())
				{
					diff = (byte) ((byte)diff-(byte)buff1[i]);
				}
				
				ret.write((byte) diff);
			}
			
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param baos1
	 * @param baos2
	 * @return true if equals
	 */
	public static boolean areEqual(UByteArrayOutputStream baos1, UByteArrayOutputStream baos2)
	{
		if (baos1 == baos2)
		{
			return true;
		}
		
		if (baos1 != null && baos2 != null && baos1.size() == baos2.size())
		{
            int len = baos1.size();

            for (int i =0; i < len; i++)
            {
                if (baos1.buf[i] !=  baos2.buf[i])
                {
                    return false;
                }
            }

            return true;

		}
		
		return false;
	}

	/**
	 * Write a string to the data buffer
	 * @param str
	 */
	public synchronized void write(String str)
	{
		write(SharedStringUtil.getBytes(str), 0, str.length());
	}

	@Override
	public synchronized void write(byte[] buf)
	{
		write(buf, 0, buf.length);
	}

	/**
	 * Insert a string at the specified location
	 * @param index
	 * @param str
	 * @throws IndexOutOfBoundsException 
	 */
	public synchronized void insertAt(int index, String str)
			throws IndexOutOfBoundsException 
	{
		insertAt(index, str.getBytes());
	}

	/**
	 * Insert a byte at the specified index location
	 * 
	 * @param index location
	 * @param b byte value
	 * @throws IndexOutOfBoundsException 
	 */
	public synchronized void writeAt(int index, byte b)
			throws IndexOutOfBoundsException 
	{
		if (index < 0)
			throw new IndexOutOfBoundsException("Index " + index);
		
		if (index >= buf.length)
		{
			int newcount = index; 
			buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount+1));
	        
	        //buf[index] = b;
	        count = newcount;
		}
		
		buf[index] = b;
		
	}

	public UByteArrayOutputStream clone()
	{
		return new UByteArrayOutputStream(getInternalBuffer(), 0, size()); 
	}
		
	/**
	 * Insert a byte array at the specified location
	 * @param index
	 * @param array
	 * @throws IndexOutOfBoundsException 
	 */
	public synchronized void insertAt(int index, byte[] array)
			throws IndexOutOfBoundsException 
	{
		if (index < 0)
		{
			throw new IndexOutOfBoundsException("Invalid index " + index);
		}
		
		if (index > size()) 
		{
			throw new IndexOutOfBoundsException("Index " + index + "bigger than size " + size());
		}

		if (array.length == 0)
		{
			return;
		}

		// set the new size
		int newCount = count + array.length;
		
		if (newCount > buf.length) 
		{
			byte[] newbuf = new byte[Math.max(buf.length << 1, newCount)];
			System.arraycopy(buf, 0, newbuf, 0, count);
			buf = newbuf;
		}

		int remainderLength = count - index;
		byte[] remainderBuf = null;
		// copy the remainder

		if (remainderLength > 0) 
		{
			remainderBuf = new byte[count - index];
			System.arraycopy(buf, index, remainderBuf, 0, remainderBuf.length);
		}

		// copy the array
		System.arraycopy(array, 0, buf, index, array.length);

		// copy remainder;
		if (remainderLength > 0)
		{
			System.arraycopy(remainderBuf, 0, buf, index + array.length, remainderBuf.length);
		}

		count = newCount;
	}

	@Override
	public synchronized boolean equals(Object obj) 
	{
		if (obj != null && obj instanceof UByteArrayOutputStream)
		{
			synchronized (obj) 
			{
				UByteArrayOutputStream boas = (UByteArrayOutputStream) obj;

				if (size() == boas.size()) 
				{
					for (int i = 0; i < size(); i++) 
					{
						if (byteAt(i) != boas.byteAt(i)) 
						{
							return false;
						}
					}
					
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Remove from the output stream at index the specified length
	 * 
	 * @param index
	 *            where to start removing data
	 * @param length
	 *            to be removed
	 * @throws IndexOutOfBoundsException
	 *             if the index or length are out of bound
	 */
	public synchronized void removeAt(int index, int length)
			throws IndexOutOfBoundsException 
	{
		if (index < 0 || length < 0 || index >= count || (index + length) > count)
		{
			throw new IndexOutOfBoundsException("Size " + count + " index " + index + " length " + length);
		}

		if (length == 0)
		{
			return;
		}

		int newCount = count - length;

		System.arraycopy(buf, index + length, buf, index, count - (index + length));
		
		count = newCount;
	}
	
	/**
	 * Shift the data left.
	 * @param from start index
	 * @param to end index
	 */
	public synchronized void shiftLeft(int from, int to)
	{
		if (from < 0 || to < 0 || from > count || to > count || to > from)
		{
			throw new IndexOutOfBoundsException("Size " + count + " from " + from + " to " + to);
		}
		
		if (from == count && to == 0)
		{
			count = 0;
			return;
		}
		
		if (from == to)
		{
			return;
		}
		
		System.arraycopy(buf, from, buf, to, count - from);
		
		count = count - (from-to);
	}
	
	/**
	 * Returns the internal buffer.
	 * @return internal buffer 
	 */
	public byte[] getInternalBuffer()
	{
		return buf;
	}

	public String getString(int startIndex)
	{
		int length = size() - startIndex;
		return SharedStringUtil.toString(getInternalBuffer(), startIndex, length);
	}


	public String getString(int indexStart, int length)
	{
		return SharedStringUtil.toString(getInternalBuffer(), indexStart, length);
	}

	/**
	 * Write entire inner content to output stream based ong block size
	 * @param os to write the data to
	 * @param blockSize to sent per write
	 * @return data send
	 * @throws IOException is case error
	 */
	public synchronized int writeTo(OutputStream os, int blockSize) throws IOException {
		return writeTo(os, blockSize, size());
	}
	/**
	 * Write inner content to output steam based on block size and max bytes to write
	 * @param os to write the data to
	 * @param blockSize to sent per write
	 * @param maxBytes to be sent
	 * @return data send
	 * @throws IOException is case error
	 */
	public synchronized int writeTo(OutputStream os, int blockSize, int maxBytes) throws IOException {
		SharedUtil.checkIfNulls("Null outputStream", os);
		if (blockSize < 1 || size() == 0 || maxBytes < 1 || maxBytes > size())
			throw new IllegalArgumentException("Invalid block size: " + blockSize + " size: " + size());
		if (blockSize > maxBytes)
			blockSize = maxBytes;

		int offset = 0;
		do
		{
			os.write(getInternalBuffer(), offset, blockSize);
			offset+=blockSize;
			if(offset + blockSize > size())
			{
				blockSize = size() - offset;
			}
		}
		while(offset != maxBytes);
		return maxBytes;
	}

	/**
	 * 
	 * @param baos
	 */
	public static void printInfo(UByteArrayOutputStream baos) 
	{
		System.out.println("*************************************");
		System.out.println("Size  " + baos.size());
		System.out.println("Buffer Length " + baos.buf.length);
		System.out.println( new String(baos.buf, 0, baos.size()));
	}
 
}
