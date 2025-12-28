/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
package org.zoxweb;

import java.io.File;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Iterator;

import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedUtil;

public class FileSystemTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) {

		try {
            FileSystem fs = FileSystems.getDefault();
            System.out.println(SharedUtil.toCanonicalID(',', fs, fs.provider(), fs.supportedFileAttributeViews()));
            Iterable<FileStore> ifs = fs.getFileStores();
            Iterator<FileStore> fsi = ifs.iterator();

            while (fsi.hasNext()) {
                FileStore fStore = fsi.next();
                System.out.println(SharedUtil.toCanonicalID(',', fStore.name(), fStore.type(), fStore.getTotalSpace(), fStore.getUnallocatedSpace(), fStore.getUsableSpace()));
            }

            File file = new File("/temp/dummy.lnk");

            RandomAccessFile pipe = new RandomAccessFile(args[0], "rws");
            FileChannel channel = pipe.getChannel();//FileChannel.open("/temp/dummy.lnk", FileChannel.MapMode.READ_WRITE);

            System.out.println("channel:" + channel.isOpen());

//			int read;
//			while( (read = is.read()) != -1)
//			{
//				System.out.print( (char)read);
//			}

            int size = 4096;
            MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);

            for (int i = 0; i < SharedBase64.BASE_64.length; i++) {
                mbb.put(SharedBase64.BASE_64[i]);
            }

            System.exit(0);

            System.out.println(SharedUtil.toCanonicalID(',', file.canRead(), file.canWrite(), file.getName()));

            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);

            Sender s = new Sender(out);
            Receiver r = new Receiver(in);
            Thread t1 = new Thread(s);
            Thread t2 = new Thread(r);
            t1.start();
            t2.start();
        } catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class Sender
            implements Runnable {

	    OutputStream out;
	
        public Sender(OutputStream out) {
            this.out = out;
        }

        public void run() {
            byte value;

            try {
                for (int i = 0; i < 5; i++) {
                  value = (byte) (Math.random() * 256);
                  System.out.print("About to send " + value + ".. ");
                  out.write(value);
                  System.out.println("..sent..");
                  Thread.sleep((long) (Math.random() * 1000));
                }
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

	static class Receiver implements Runnable {
	    InputStream in;

        public Receiver(InputStream in) {
            this.in = in;
        }

        public void run() {
            int value;

            try {
                while ((value = in.read()) != -1) {
                System.out.println("received " + (byte) value);
                }

                System.out.println("Pipe closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}