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

import java.nio.ByteBuffer;

import java.util.List;

import org.zoxweb.server.io.ByteBufferUtil.BufferType;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedUtil;

public class BufferTest
{
	public static void main(String[] args){
		
		UByteArrayOutputStream bufferOutput = new UByteArrayOutputStream();
		byte[] delims = "\n".getBytes();
	
		//ByteBufferParser byteParser = new ByteBufferParser(bufferOutput, delims);

		byte[][] data = {"John".getBytes(),
			" Smith\r".getBytes(),
			"\nis at work".getBytes(),
			"\ncoding\r\n from 9 to 5.\nMarwan is always looking over my shoulder\n".getBytes(),
			"".getBytes(),
			null};

		for (int i = 0; i < data.length; i++) {
			if (data[i] != null) {
				byte[] dataInput = data[i];
		    	bufferOutput.write(dataInput);
		    	//System.out.println(bufferOutput.toString());
			}
		
		List<byte[]> matchedTokens = ByteBufferParser.parse(bufferOutput, delims);
		//ArrayList<byte[]> matchedTokens = byteParser.parse();
		
			if(matchedTokens.size() > 0) {
				System.out.println(matchedTokens.size());
		
				for (int j = 0; j < matchedTokens.size(); j++) {
					String s = new String(matchedTokens.get(j));
					System.out.println(s);
				}
			}

			try {
//				CustomClassLoader cl = new CustomClassLoader(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs());
//				
//
//				Class<?> clazz = Class.forName("java.nio.CustomHeapByteBuffer", true, cl);
//				Constructor<?> constructor = clazz.getConstructor(byte[].class, int.class, int.class, int.class, int.class, int.class);
				
				byte buffer[] = new byte[1000000];
				bufferOutput.write(buffer);
				buffer = bufferOutput.toByteArray();
				ByteBuffer bb =  ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, buffer, 0, buffer.length, true);
				bufferOutput.reset();
				long delta = System.nanoTime();
				ByteBufferUtil.write(bb, bufferOutput, true);
				delta = System.nanoTime() - delta;
				
				System.out.println("fastwrite:" + bufferOutput.size() +" it took:" + Const.TimeInMillis.nanosToString(delta));

				bb =  ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, buffer, 0, buffer.length, true);
				bufferOutput.reset();
				
				delta = System.currentTimeMillis();
				ByteBufferUtil.write(bb, bufferOutput, true);
				delta = System.currentTimeMillis() - delta;
				
				System.out.println(bufferOutput.size() + " millis " + delta);
				
				bb =  ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, buffer, 0, buffer.length, true);
				System.out.println(bb.getClass().getName());
				
				System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
				while (bb.hasRemaining()) {
					bb.get();
					//System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
				}
				System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
	
				bb.clear();
				//bb.flip();
				
				for (i=0; i < 11; i++) {
					try {
						bb.put((byte) i);
						System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
					} catch(Exception e) {
						e.printStackTrace();
						break;
					}
				}

				bb.flip();
				System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
				
				while(bb.hasRemaining()) {
					bb.get();
					System.out.println(SharedUtil.toCanonicalID(',', bb.position(), bb.limit(), bb.capacity()));
				}
				
				//ByteBufferUtil.toByteBuffer(bufferOutput);
				Class<?> c = Class.forName("java.nio.CustomHeapByteBuffer");
				System.out.println(c.getName());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}