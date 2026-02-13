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
package org.zoxweb.server.net.security;

import org.zoxweb.server.io.IOStreamInfo;
import org.zoxweb.server.io.StreamStats;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.security.ScanResult;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.Const.TypeInBytes;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Simple client for ClamAV's clamd scanner. Provides straightforward instream scanning.
 */
public class ClamAVClient {

    public static class ClamAVScanResult
            extends FilterInputStream
            implements StreamStats {
        IOStreamInfo ci;
        ScanResult result = new ScanResult();

        ClamAVScanResult(IOStreamInfo ci, InputStream is, String scanName) {
            super(is);
            result.setName(scanName);
            this.ci = ci;
        }

        public ScanResult getScanResult() {
            return result;
        }


        public IOStreamInfo getConnectionInfo() {
            return ci;
        }


        public int read(byte[] buf)
                throws IOException {
            return read(buf, 0, buf.length);
        }

        public int read(byte[] buf, int offset, int len)
                throws IOException {
            int read = in.read(buf, offset, len);

            try {
                if (read > 0) {
                    partialScan(this, buf, offset, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return read;
        }

        public void close()
                throws IOException {
            try {
                finishScan(this);
            } catch (Exception e) {

            }
            SharedIOUtil.close(in);
            SharedIOUtil.close(ci);
        }

        public String toString() {
            return (result.getName() != null ? result.getName() + "," : "") + "Data length:" + result.getLength() + ",Scan duration:" + result.getScanDuration() + " millis" + ",Is Clean:" + result.getStatus() + ".\n"
                    + result.getResult();
        }

        @Override
        public long totalBytes() {
            return result.getLength();
        }
    }


    private String hostName;
    private int port;
    //private int timeout;

    // "do not exceed StreamMaxLength as defined in clamd.conf, otherwise clamd will reply with INSTREAM size limit exceeded and close the connection."
    public static final int CHUNK_SIZE = 2048;
    public static final int DEFAULT_TIMEOUT = 500;

    /**
     * @param hostName The hostname of the server running clamav-daemon
     * @param port The port that clamav-daemon listens to(By default it might not listen to a port. Check your clamav configuration).
     * @param timeout zero means infinite timeout. Not a good idea, but will be accepted.
     */
    public ClamAVClient(String hostName, int port, int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value does not make sense.");
        }
        this.hostName = hostName;
        this.port = port;
        //this.timeout = timeout;
    }

    public ClamAVClient(String hostName, int port) {
        this(hostName, port, DEFAULT_TIMEOUT);
    }

    /**
     * Run PING command to clamd to test it is responding.
     *
     * @return true if the server responded with proper ping reply.
     * @throws IOException
     */
    public boolean ping() throws IOException {
        try (Socket s = new Socket(hostName, port); OutputStream outs = s.getOutputStream()) {
            //s.setSoTimeout(timeout);
            outs.write(asBytes("zPING\0"));
            outs.flush();
            byte[] b = new byte[4];
            s.getInputStream().read(b);
            return Arrays.equals(b, asBytes("PONG"));
        }
    }

    public ClamAVScanResult initScan(String fileName, InputStream is)
            throws IOException {
        long delta = System.currentTimeMillis();
        Socket s = new Socket();
        //s.setSoTimeout(timeout);


        s.connect(new InetSocketAddress(hostName, port));
        IOStreamInfo ci = new IOStreamInfo(s);
        ci.os.write(asBytes("zINSTREAM\0"));
        ci.os.flush();
        ClamAVScanResult ret = new ClamAVScanResult(ci, is, fileName);
        delta = System.currentTimeMillis() - delta;
        ret.getScanResult().setScanDuration(ret.getScanResult().getScanDuration() + delta);
        return ret;
    }

    public static ClamAVScanResult partialScan(ClamAVScanResult cavsr, byte[] buffer, int offset, int len)
            throws IOException {
        long delta = System.currentTimeMillis();

        for (int i = 0; i < len; i += CHUNK_SIZE) {
            int chunkSize = CHUNK_SIZE;
            if (i + CHUNK_SIZE > len) {
                chunkSize = len - i;
            }


            byte[] chunkSizeBuffer = TypeInBytes.intToBytes(chunkSize);
            cavsr.ci.os.write(chunkSizeBuffer);
            cavsr.ci.os.write(buffer, offset + i, chunkSize);
            cavsr.ci.os.flush();


            cavsr.getScanResult().setLength(cavsr.getScanResult().getLength() + chunkSize);
        }
        delta = System.currentTimeMillis() - delta;

        cavsr.getScanResult().setScanDuration(cavsr.getScanResult().getScanDuration() + delta);
        return cavsr;
    }

    public static ClamAVScanResult finishScan(ClamAVScanResult cavsr)
            throws IOException {
        long delta = System.currentTimeMillis();

        cavsr.ci.os.write(new byte[]{0, 0, 0, 0});

        cavsr.ci.os.flush();


        // read reply

        byte[] result = readAll(cavsr.ci.is);

        cavsr.getScanResult().setResult(new String(result));
        cavsr.close();
        delta = System.currentTimeMillis() - delta;

        cavsr.getScanResult().setScanDuration(cavsr.getScanResult().getScanDuration() + delta);
        cavsr.getScanResult().setStatus(isCleanReply(result) ? Const.ScanStatus.OK : Const.ScanStatus.INFECTED);
        return cavsr;
    }

    /**
     * Streams the given data to the server in chunks. The whole data is not kept in memory.
     * This method is preferred if you don't want to keep the data in memory, for instance by scanning a file on disk.
     * Since the parameter InputStream is not reset, you can not use the stream afterwards, as it will be left in a EOF-state.
     * If your goal is to scan some data, and then pass that data further, consider using {@link #scan(byte[]) scan(byte[] in)}.
     * <p>
     * Opens a socket and reads the reply. Parameter input stream is NOT closed.
     *
     * @param is data to scan. Not closed by this method!
     * @return server reply
     * @throws IOException
     */
    public ClamAVScanResult scan(InputStream is)
            throws IOException {
        ClamAVScanResult ret = new ClamAVScanResult(null, is, null);
        try (Socket s = new Socket(hostName, port); OutputStream outs = new BufferedOutputStream(s.getOutputStream())) {
            //s.setSoTimeout(timeout);
            long delta = System.currentTimeMillis();
            // handshake

            outs.write(asBytes("zINSTREAM\0"));
            outs.flush();

            byte[] chunk = new byte[CHUNK_SIZE];

            // send data
            int read = ret.read(chunk);
            while (read >= 0) {

                ret.getScanResult().setLength(ret.getScanResult().getLength() + read);
                // The format of the chunk is: '<length><data>' where <length> is the size of the following data in bytes expressed as a 4 byte unsigned
                // integer in network byte inventory and <data> is the actual chunk. Streaming is terminated by sending a zero-length chunk.
                //ByteBuffer.allocate(4).putInt(read).array();
                byte[] chunkSize = TypeInBytes.intToBytes(read);
                outs.write(chunkSize);
                outs.write(chunk, 0, read);
                outs.flush();

                read = is.read(chunk);


            }

            // terminate scan
            outs.write(new byte[]{0, 0, 0, 0});
            outs.flush();

            // read reply
            try (InputStream clamIs = s.getInputStream()) {

                byte result[] = readAll(clamIs);


                ret.getScanResult().setResult(new String(result));
                delta = System.currentTimeMillis() - delta;

                ret.getScanResult().setScanDuration(+delta);
                ret.getScanResult().setStatus(isCleanReply(result) ? Const.ScanStatus.OK : Const.ScanStatus.INFECTED);
                return ret;
            }
        }
    }


    public ClamAVScanResult scan2(String scanName, InputStream is)
            throws IOException {

        ClamAVScanResult ret = null;
        try {
            ret = initScan(scanName, is);
            byte[] chunk = new byte[CHUNK_SIZE];
            while (ret.read(chunk) >= 0) ;
            ret.close();
        } finally {
            SharedIOUtil.close(ret);
        }


        return ret;


    }

    /**
     * Scans bytes for virus by passing the bytes to clamav
     *
     * @param in data to scan
     * @return server reply
     * @throws IOException
     **/
    public ClamAVScanResult scan(byte[] in) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(in);
        return scan(bis);
    }

    /**
     * Interpret the result from a  ClamAV scan, and determine if the result means the data is clean
     *
     * @param reply The reply from the server after scanning
     * @return true if no virus was found according to the clamd reply message
     */
    public static boolean isCleanReply(byte[] reply) {
        String r = new String(reply, StandardCharsets.US_ASCII);
        return (r.contains("OK") && !r.contains("FOUND"));
    }

    // byte conversion based on ASCII character set regardless of the current system locale
    private static byte[] asBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    // reads all available bytes from the stream
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();

        byte[] buf = new byte[512];
        int read;
        while ((read = is.read(buf)) != -1) {
            tmp.write(buf, 0, read);

        }
        return tmp.toByteArray();
    }

    public static void main(String... args) {

        try {
            int index = 0;
            IPAddress hostInfo = new IPAddress(args[index++]);
            ClamAVClient cavc = new ClamAVClient(hostInfo.getInetAddress(), hostInfo.getPort(), 0);
            InputStream is = null;


            for (; index < args.length; index++) {
                String filename = args[index];
                if (filename.equalsIgnoreCase("-p")) {
                    System.out.println("ping:" + cavc.ping());
                } else {
                    is = null;
                    System.out.println("[" + index + "]Scanning:" + filename);
                    try {
                        try {
                            is = new URL(filename).openConnection().getInputStream();
                        } catch (Exception e) {
                            //e.printStackTrace();
                            is = new FileInputStream(filename);
                        }


                        ClamAVScanResult cavsr = cavc.scan2(filename, is);

                        System.out.println(cavsr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Processing error:" + e);
                    } finally {
                        SharedIOUtil.close(is);
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("ClamAVClient host:port <-p ping> | <list of file or urls>");
        }
    }
}