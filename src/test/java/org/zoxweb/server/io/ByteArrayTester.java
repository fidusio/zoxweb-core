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

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.BytesArray;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.Const.TypeInBytes;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ByteArrayTester
{

	@Test
	public void test1()
    {

		try
        {
			UByteArrayOutputStream baos = new UByteArrayOutputStream();

			UByteArrayOutputStream.printInfo(baos);
			String numbers = "1234567890";
			String letters = "ABCDEF";

			baos.write(numbers);
			UByteArrayOutputStream.printInfo(baos);
			baos.reset();
			UByteArrayOutputStream.printInfo(baos);
			baos.write(numbers);
			UByteArrayOutputStream.printInfo(baos);
			baos.insertAt(baos.size(), letters);
			UByteArrayOutputStream.printInfo(baos);
			baos.insertAt(0, letters);
			UByteArrayOutputStream.printInfo(baos);
			baos.insertAt(letters.length(), numbers);
			UByteArrayOutputStream.printInfo(baos);
			baos.insertAt(letters.length() + numbers.length(), letters);
			UByteArrayOutputStream.printInfo(baos);

			baos.write(letters);
			UByteArrayOutputStream.printInfo(baos);

			for (int i = 0; i < 256; i++) {
				baos.write("A");
			}
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ");
			UByteArrayOutputStream.printInfo(baos);
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream("baos"));
			oos.writeObject(baos);
			oos.close();

			baos.removeAt(letters.length(), numbers.length());
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt(0, letters.length());
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt(0, letters.length());
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt(numbers.length(), letters.length());
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt(baos.size() - 1, 1);
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt("12345".length(), 2);
			UByteArrayOutputStream.printInfo(baos);

			baos.removeAt(0, 1);
			UByteArrayOutputStream.printInfo(baos);

			System.out.println("Exceptions sections +++++++++++++++++++++++++++++++++++++");
			try {
				baos.insertAt(60, "toto");
			} catch (Exception e) {
				e.printStackTrace();
			}

			UByteArrayOutputStream.printInfo(baos);

			try {
				baos.insertAt(-1, "toto");
			} catch (Exception e) {
				e.printStackTrace();
			}

			UByteArrayOutputStream.printInfo(baos);

			System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("baos"));
			UByteArrayOutputStream tmp = (UByteArrayOutputStream) ois
					.readObject();
			ois.close();
			UByteArrayOutputStream.printInfo(tmp);
			System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

			baos.reset();
			baos.write("Marwan");
			UByteArrayOutputStream os = new UByteArrayOutputStream();
			System.out.println("Equals " + baos.equals(os));
			os.write("Marwa");
			System.out.println("Equals " + baos.equals(os));
			os.write("n");
			System.out.println("Equals " + baos.equals(os));
			UByteArrayOutputStream.printInfo( os);
			os.writeAt(1, (byte)'N');
			UByteArrayOutputStream.printInfo( os);
			os.writeAt(33, (byte)'N');
			UByteArrayOutputStream.printInfo( os);
			
			String matches[] = 
				{
					"MN",
					"n",
					"X",
					"xdFDFDFSDFSfds",
					"a",
					"A",
					"MNrwannael",
				};
			
			for ( String str: matches)
			{
				System.out.println( str + " index " +SharedUtil.indexOf(os.getInternalBuffer(), 0, os.size(), str.getBytes(), 0, str.length()));
				try
				{
					System.out.println( str + " index details " + SharedUtil.indexOf(os.getInternalBuffer(), 0, os.size(), str, 0, str.length(), true));
				}
				catch( Exception e)
				{
					e.printStackTrace();
				}
			}
			
			
			
			UByteArrayOutputStream ubaosShift = new UByteArrayOutputStream();
			ubaosShift.write(" Marwan NAEL");
			UByteArrayOutputStream.printInfo(ubaosShift);
			
			ubaosShift.shiftLeft(7, 1);
			UByteArrayOutputStream.printInfo(ubaosShift);
			ubaosShift.write(" Marwan");
			ubaosShift.shiftLeft(1, 0);
			UByteArrayOutputStream.printInfo(ubaosShift);
			
			
			ubaosShift.shiftLeft(11, 0);
			UByteArrayOutputStream.printInfo(ubaosShift);
			ubaosShift.shiftLeft(1, 0);
			UByteArrayOutputStream.printInfo(ubaosShift);

			
			int intArray[] = {0, 1, 2, 3, 100000, 100000001, 1304434343};
			ByteBuffer.allocate(4).putInt(0).array();
			TypeInBytes.intToBytes(0);
			//long delta, delta1 = System.nanoTime();

			for (int val : intArray) {
				long delta = System.nanoTime();
				byte[] buffer = ByteBuffer.allocate(4).putInt(val).array();
				delta = System.nanoTime() - delta;
				
				long delta1 = System.nanoTime();
				byte[] buffer1 = TypeInBytes.intToBytes(val);
				delta1 = System.nanoTime() - delta1;
				
				System.out.println(val + " delta " + delta + " delta1 " + delta1 + " equals " + Arrays.equals(buffer, buffer1));
			}

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

			List<byte[]> matchedTokens = IOUtil.parse(bufferOutput, delimiter, true);

			System.out.println("Tokens found " + matchedTokens.size() + " buffer size: " + bufferOutput.size());
			if(matchedTokens.size() > 0) {


				for (int j = 0; j < matchedTokens.size(); j++) {
					String s = new String(matchedTokens.get(j));
					System.out.println(j + ":" + s.length() + ":" + s);
				}
			}


			byte[] buffer = new byte[1000000];
			bufferOutput.write(buffer);
			buffer = bufferOutput.toByteArray();
			ByteBuffer bb =  ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, buffer, 0, buffer.length, true);
			bufferOutput.reset();
			long delta = System.nanoTime();
			ByteBufferUtil.write(bb, bufferOutput, true);
			delta = System.nanoTime() - delta;

			System.out.println("fast write:" + bufferOutput.size() +" it took:" + Const.TimeInMillis.nanosToString(delta));

			bb =  ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, buffer, 0, buffer.length, true);
			bufferOutput.reset();

			delta = System.currentTimeMillis();
			ByteBufferUtil.write(bb, bufferOutput, true);
			delta = System.currentTimeMillis() - delta;

			System.out.println(bufferOutput.size() + " millis " + delta);

			bb =  ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, buffer, 0, buffer.length, true);
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

		} catch (Exception e) {
			e.printStackTrace();

		}

	}


	@Test
	public void testBytesArray()
	{
		BytesArray ba = new BytesArray(null, SharedStringUtil.getBytes("Hello"));

		ByteBuffer bb = ByteBufferUtil.toByteBuffer(ba);

		System.out.println(new String(ByteBufferUtil.toBytes(bb,  false)));

	}


	@Test
	public void testMapBytesArray()
	{
		Set<BytesArray> set = new HashSet<>();
		int len = 100;
		for (int i = 0; i < len; i++)
		{
			set.add(new BytesArray(null, new byte[]{(byte)(i+1), (byte) (i+2), (byte)(i+3), (byte)(i+4)}));
		}

		assert set.size() == len;
		assert set.contains(new BytesArray(null, new byte[]{1,2,3,4}));
		assert set.contains(new BytesArray(null, new byte[]{2,3,4,5}));


		Set<Integer> intSet = new HashSet<>();
		for(int i=0; i < 100 ; i++)
			intSet.add(i);
		assert intSet.contains(50);
	}

}
