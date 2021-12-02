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

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SimpleQueue;

public class ByteBufferUtil 
{
	public enum BufferType 
	{
		DIRECT, 
		HEAP
	}
	
	private static final ByteBufferUtil SINGLETON = new ByteBufferUtil();

	private Map<Integer, SimpleQueue<ByteBuffer>> cachedBuffers = new HashMap<Integer, SimpleQueue<ByteBuffer>>();

	public static final int DEFAULT_BUFFER_SIZE = 4096;
	public static final int CACHE_LIMIT = 256;


	public static final ByteBuffer EMPTY = allocateByteBuffer(0);
	
	private ByteBufferUtil()
	{	

		
	}
	
	private void cache0(ByteBuffer bb)
	{
		synchronized(cachedBuffers)
		{
			if (bb != null)
			{
				SimpleQueue<ByteBuffer> sq = cachedBuffers.get(bb.capacity());

				if (sq == null)
				{
					sq = new SimpleQueue<ByteBuffer>(false);
					//System.out.println("new capacity:" + bb.capacity());
					cachedBuffers.put(bb.capacity(), sq);
				}

				if (sq.size() < CACHE_LIMIT)
				{
					bb.clear();
					if (!sq.contains(bb))
					{
						sq.queue(bb);
					}
				}
			}
		}
	}
	
	private  ByteBuffer toByteBuffer0(BufferType bType, byte[] buffer, int offset, int length, boolean copy)
	{
		ByteBuffer bb = null;
		SimpleQueue<ByteBuffer> sq = null;

		synchronized(cachedBuffers)
		{
			sq = cachedBuffers.get(length-offset);

			if (sq != null)
			{
				bb = sq.dequeue();
			}
		}
		
		if (bb == null)
		{
			switch(bType)
			{
			case DIRECT:
				bb = ByteBuffer.allocateDirect(length-offset);
				break;
			case HEAP:
				bb = ByteBuffer.allocate(length-offset);
				break;
			}
			//log.info("["+ (counter++) + "]must create new buffer:" + bb.capacity() + " " + bb.getClass().getName());
		}

		if (copy)
		{
			bb.put(buffer, offset, length);
			bb.flip();
		}
			
		
		
		return bb;
		
	}
	

	
	public static void write(ByteChannel bc, UByteArrayOutputStream ubaos) throws IOException
	{	
		write(bc, ubaos.getInternalBuffer(), 0, ubaos.size());
	}
	
	public static void write(ByteChannel bc, byte array[], int off, int len) throws IOException
	{
		SharedUtil.checkIfNulls("null byte channel", bc);

		if (off < 0)
		{
			throw new IllegalArgumentException("invalid offset " + off);
		}

		ByteBuffer bb = allocateByteBuffer(BufferType.HEAP, DEFAULT_BUFFER_SIZE);

		try
		{
			int end = off + len;

			if (end > array.length)
			{
				end = array.length;
			}
			
			for (int offset = off; offset < end;)
			{
				int length = offset+bb.capacity() > end ? end - offset : bb.capacity();
	
				bb.clear();
				bb.put(array, offset, length);
				offset+=length;
				write(bc, bb);
			}
		}
		finally
		{
			cache(bb);
		}
	}

	public  static ByteBuffer allocateByteBuffer(int capacity)
	{
		return SINGLETON.toByteBuffer0(BufferType.HEAP, null, 0, capacity, false);
	}

	public static ByteBuffer allocateByteBuffer(BufferType bType, int capacity)
	{
		return SINGLETON.toByteBuffer0(bType, null, 0, capacity, false);
	}

	public static ByteBuffer allocateByteBuffer(BufferType bType, byte buffer[], int offset, int length, boolean copy)
	{
		return SINGLETON.toByteBuffer0(bType, buffer, offset, length, copy);
	}

	public static int write(ByteChannel bc, ByteBuffer bb) throws IOException
	{
		bb.flip();
		int totalWritten = 0;
		while(bb.hasRemaining())
		{
			int written = bc.write(bb);
			if (written == -1)
				return -1;

			totalWritten += written;
		}
		return totalWritten;
	}


//	public static int smartWrite(ByteChannel bc, ByteBuffer bb) throws IOException{
//		return smartWrite(null, bc, bb);
//	}


	public static int smartWrite(Lock lock, ByteChannel bc, ByteBuffer bb) throws IOException
	{
		int totalWritten = 0;
		if(lock != null)
			lock.lock();
		try {
			bb.flip();

			synchronized (bc) {
				while (bb.hasRemaining()) {
					int written = bc.write(bb);
					if (written == -1)
						return -1;

					totalWritten += written;
				}
			}
			bb.compact();
		}
		finally {
			if(lock != null)
				lock.unlock();
		}
		return totalWritten;
	}
	
	public static String toString(ByteBuffer bb) throws IOException
	{
		UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
		write(ubaos, bb);

		return ubaos.toString();
	}
	


	public static void write(UByteArrayOutputStream ubaos, ByteBuffer bb) throws IOException
	{
		bb.flip();

		for (int i = 0; i < bb.limit(); i++)
		{
			ubaos.write(bb.get());
		}
	}
	
	public static void cache(ByteBuffer bb)
	{
		if(bb != null) {

			SINGLETON.cache0(bb);
		}
	}
	
}

