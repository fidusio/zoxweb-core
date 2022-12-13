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

import org.zoxweb.server.io.ByteBufferUtil.BufferType;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.nio.ByteBuffer;
import java.util.List;

public class BufferTest
{
	public static void main(String[] args){
		
		UByteArrayOutputStream bufferOutput = new UByteArrayOutputStream();
		byte[] delimiter = SharedStringUtil.getBytes( "\n");




		String[] data = {"John",
				" Smith\r",
				"\nis at work",
				"\ncoding\r\n from 9 to 5.\nMario is always looking over my shoulder\n",
				"\n\nMarioTaZa ",
			null};

		System.out.println("Data length " + data.length);
		for (String str : data) {
			if (str!= null) {
				bufferOutput.write(str);
			}
		}
		
		List<byte[]> matchedTokens = bufferOutput.parse(delimiter, true);
		System.out.println("Tokens found " + matchedTokens.size() + " buffer size: " + bufferOutput.size());
		if(matchedTokens.size() > 0) {


			for (int j = 0; j < matchedTokens.size(); j++) {
				String s = new String(matchedTokens.get(j));
				System.out.println(j + ":" + s.length() + ":" + s);
			}
		}

		long total = 0;
		for(int i=0; i< 50; i++)
		{
			for (String str : data) {
				if (str!= null) {
					bufferOutput.write(str);
				}
			}
			long ts = System.nanoTime();
			matchedTokens = bufferOutput.parse(delimiter,true);
			ts = System.nanoTime() - ts;
			total+=ts;
			System.out.println("it took:" + Const.TimeInMillis.nanosToString(total));
		}

		//ArrayList<byte[]> matchedTokens = byteParser.parse();
		System.out.println("Tokens found " + matchedTokens.size() + " buffer size: " + bufferOutput.size());
		if(matchedTokens.size() > 0) {


			for (int j = 0; j < matchedTokens.size(); j++) {
				String s = new String(matchedTokens.get(j));
				System.out.println(j + ":" + s.length() + ":" + s);
			}
		}

			try {
				byte[] buffer = new byte[1000000];
				bufferOutput.write(buffer);
				buffer = bufferOutput.toByteArray();
				ByteBuffer bb =  ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, buffer, 0, buffer.length, true);
				bufferOutput.reset();
				long delta = System.nanoTime();
				ByteBufferUtil.write(bb, bufferOutput, true);
				delta = System.nanoTime() - delta;
				
				System.out.println("fast write:" + bufferOutput.size() +" it took:" + Const.TimeInMillis.nanosToString(delta));

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
				
				for (int i=0; i < 11; i++) {
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
				

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

}