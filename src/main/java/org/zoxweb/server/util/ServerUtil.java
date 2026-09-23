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
package org.zoxweb.server.util;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.net.NetUtil;
import org.zoxweb.shared.data.SystemInfoDAO;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.util.*;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-side (full JVM) static helpers that do not fit a more specific utility class:
 * a shared {@link SecureRandom} and global {@link Lock}, monitor wait and sub-millisecond
 * delay helpers, throwable-to-string conversion, text file/stream to line list readers,
 * JavaBeans XML (de)serialization, {@link SystemInfoDAO} population from system properties
 * and network interfaces, and a few OS/JVM level conveniences.
 * <p>
 * Everything here is stateless except the lazily computed {@link #isMacOS()} flag; the
 * class cannot be instantiated.
 */
public final class ServerUtil {

    /** Lazily computed, lock-guarded cache for {@link #isMacOS()}; {@code null} until first call. */
    private static AtomicBoolean isMac = null;

    private ServerUtil() {

    }

    /**
     * Secure random
     */
    public final static SecureRandom RNG = new SecureRandom();

    /**
     * Utility global lock
     */
    public final static Lock LOCK = new ReentrantLock();

    /**
     * Utility method to wait on a object.
     * <p>
     * Synchronizes on {@code obj} and calls {@link Object#wait(long, int)}. An
     * {@link InterruptedException} is printed to stderr and swallowed; the interrupt
     * flag of the calling thread is not restored.
     * @param obj to be synchronized and wait on
     * @param millis to wait
     * @param nanos to wait
     */
    public static void waitNano(Object obj, long millis, int nanos) {
        synchronized (obj) {
            try {
                obj.wait(millis, nanos);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Utility method to wait on a object.
     * <p>
     * Synchronizes on {@code obj} and calls {@link Object#wait(long)}. An
     * {@link InterruptedException} is printed to stderr and swallowed; the interrupt
     * flag of the calling thread is not restored.
     * @param obj to be synchronized and wait on
     * @param millis to wait
     */
    public static void waitNano(Object obj, long millis) {
        synchronized (obj) {
            try {
                obj.wait(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Renders a throwable exactly as {@link Throwable#printStackTrace()} would, including
     * the message, every frame and the full cause chain, and returns it as a string
     * instead of printing it.
     * <p>
     * The scratch buffer is a pooled {@link UByteArrayOutputStream}; the writer is flushed
     * before the buffer is read (a {@link PrintWriter} over an output stream buffers internally
     * and would otherwise yield an empty string) and the buffer is returned to the pool.
     * Bytes are decoded with the platform default charset, matching the encoding used by
     * the writer.
     * @param e throwable to render
     * @return the stack trace text, as printed by {@code printStackTrace}
     * @throws NullPointerException if {@code e} is null
     */
    public static String throwableToString(Throwable e)
    {
        UByteArrayOutputStream baos = ByteBufferUtil.allocateUBAOS(1024);
        PrintWriter pw = new PrintWriter(baos);
        e.printStackTrace(pw);
        pw.flush();
        String ret = baos.toString();
        ByteBufferUtil.cache(baos);
        return ret;
    }

    /**
     * Concatenates two arrays as one.
     * <p>
     * When both arrays are non-null a new array of the same component type is returned.
     * When exactly one is null the other array is returned <b>as is</b> (not copied);
     * when both are null the result is null.
     * @param <T> component type
     * @param array1 first array, may be null
     * @param array2 second array, may be null
     * @return concatenated type array
     */
    public static <T> T[] concat(T[] array1, T[] array2) {
        if (array1 != null && array2 != null) {
            T[] result = Arrays.copyOf(array1, array1.length + array2.length);
            System.arraycopy(array2, 0, result, array1.length, array2.length);
            return result;
        }

        if (array1 == null && array2 != null) {
            return array2;
        }

        return array1;
    }

    /**
     * This method reads files based on the file name/location
     * on the computer and stores the data in an array list of
     * strings, one entry per line. The file is always closed.
     * @param fileName to be read
     * @return string list
     * @throws IOException in case of IO error
     */
    public static List<String> toStringList(String fileName)
            throws IOException {
        List<String> ret = null;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(fileName);
            ret = toStringList(fis);
        } finally {
            SharedIOUtil.close(fis);
        }

        return ret;
    }

    /**
     * This method is used to convert input streams to an
     * array list of strings, one entry per line (line terminators stripped).
     * The stream is decoded with the platform default charset and is
     * <b>always closed</b> on return, including on error.
     * @param is to be read
     * @return string list
     * @throws IOException in case of IO error
     */
    public static List<String> toStringList(InputStream is)
            throws IOException {
        BufferedReader textReader = null;

        ArrayList<String> messageList = new ArrayList<String>();

        try {
            textReader = new BufferedReader(new InputStreamReader(is));
            String line = textReader.readLine();

            while (line != null) {
                messageList.add(line);
                line = textReader.readLine();
            }
        } finally {
            SharedIOUtil.close(textReader);
            SharedIOUtil.close(is);
        }

        return messageList;

    }

    /**
     * Reads every JavaBean object encoded in an {@link XMLEncoder} document
     * (the inverse of {@link #writeBeansToXML(OutputStream, Object...)}).
     * <p>
     * Objects are read until the decoder throws, which is how {@link XMLDecoder} signals
     * the end of the document, so any decoding error also silently ends the read with the
     * objects decoded so far. The stream is always closed.
     * @param is XML stream produced by {@link XMLEncoder}
     * @return the decoded objects in document order, never null
     */
    public static Object[] readXMLToBeans(InputStream is) {
        XMLDecoder decoder = new XMLDecoder(is);
        ArrayList<Object> ret = new ArrayList<Object>();

        try {
            do {
                ret.add(decoder.readObject());


            } while (true);

        } catch (Exception e) {
            //e.printStackTrace();
        }

        SharedIOUtil.close(decoder);
        SharedIOUtil.close(is);

        return ret.toArray();
    }

    /**
     * Write beans to XML using {@link XMLEncoder} (JavaBeans long-term persistence
     * format). The encoder is flushed and both the encoder and {@code os} are closed.
     * @param os destination stream, closed on return
     * @param objs beans to encode, in order
     */
    public static void writeBeansToXML(OutputStream os, Object... objs) {

        XMLEncoder enc = new XMLEncoder(os);

        for (Object o : objs) {
            enc.writeObject(o);
        }

        enc.flush();
        SharedIOUtil.close(enc);
        SharedIOUtil.close(os);
    }

    /**
     * Load the systeminfo: every JVM system property as an {@link NVPair}, and optionally
     * one {@link NetworkInterfaceDAO} (name, display name, MAC, addresses) per physical
     * network interface. Loopback, point-to-point, virtual and MAC-less interfaces are
     * skipped. Interface enumeration errors are swallowed and simply leave the network
     * list empty.
     * @param includeNetworkDetails true add networking info
     * @return SystemInfoDAO
     */
    public static SystemInfoDAO loadSystemInfoDAO(boolean includeNetworkDetails) {
        SystemInfoDAO ret = new SystemInfoDAO();

        Map.Entry<?, ?>[] all = System.getProperties().entrySet().toArray(new Map.Entry[0]);

        for (Map.Entry<?, ?> e : all) {
            ret.getSystemProperties().add(new NVPair((String) e.getKey(), (String) e.getValue()));
        }

        if (includeNetworkDetails) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

                for (; interfaces.hasMoreElements(); ) {
                    NetworkInterface ni = interfaces.nextElement();

                    if (!ni.isPointToPoint() && !ni.isLoopback() && !ni.isVirtual() && ni.getHardwareAddress() != null) {
                        NetworkInterfaceDAO niDAO = new NetworkInterfaceDAO();
                        niDAO.setMACAddress(SUS.bytesToHex(ni.getHardwareAddress(), ":"));
                        niDAO.setName(ni.getName());
                        niDAO.setDisplayName(ni.getDisplayName());


                        Enumeration<InetAddress> addresses = ni.getInetAddresses();

                        while (addresses.hasMoreElements()) {
                            niDAO.getInetAddresses().add(NetUtil.toInetAddressDAO(addresses.nextElement()));
                        }

                        ret.getNetworkInterfaces().add(niDAO);
                    }

                }
            } catch (IOException e) {

            }
        }

        return ret;
    }

    /**
     * Load the systeminfo including network details; equivalent to
     * {@code loadSystemInfoDAO(true)}.
     * @return SystemInfoDAO
     */
    public static SystemInfoDAO loadSystemInfoDAO() {
        return loadSystemInfoDAO(true);
    }


    /**
     * This method will print statements to the error stream and invoke system exit with existCode.
     * WARNING: after the invocation of this method the JVM will cease to exist
     * @param exitCode the application exist code
     * @param statements to be printed
     */
    public static void exitWithError(int exitCode, String... statements) {
        for (String msg : statements)
            System.err.println(msg);
        System.exit(exitCode);
    }

    /**
     * Create delay in nanos by busy-spinning on {@link System#nanoTime()}.
     * @param nanos to delay
     * @return the difference between the actual and requested end time (overshoot in nanos)
     */
    private static long delayInNanos(long nanos) {
        long stopAt = System.nanoTime() + nanos;

        do {

        } while (System.nanoTime() < stopAt);

        return System.nanoTime() - stopAt;
    }

    /**
     * This method initiates a delay based on the specified time in
     * nanoseconds. If the time is less than 1 millisecond, the program
     * enters a while loop for the delay. Otherwise, the program calls
     * the sleep function based on the time length.
     * <p>
     * On the sleep path an {@link InterruptedException} is printed and swallowed;
     * the interrupt flag is not restored.
     * @param timeToSleepNanos delay length in nanoseconds
     * @return delay overshoot in nanos: actual end time minus requested end time
     */
    public static long delay(long timeToSleepNanos) {
        if (timeToSleepNanos <= 1000000) {
            return delayInNanos(timeToSleepNanos);
        } else {
            final long endingTime = System.nanoTime() + timeToSleepNanos;
            long remainingTime = timeToSleepNanos;
            //while( remainingTime > 0)
            {
                long ms = remainingTime / 1000000;
                int ns = (int) remainingTime % 1000000;
                if (ms > 0) {
                    try {
                        Thread.sleep(ms, ns);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    delayInNanos(ns);
                }

                return System.nanoTime() - endingTime;
            }

        }
    }

    /**
     * Check if the list of all object are derived from Clazz.
     * Null elements are ignored, and an empty list matches.
     * @param list of objects
     * @param clazz to be matched with
     * @return true if all the list object are matching
     * @throws NullPointerException if {@code list} or {@code clazz} is null
     */
    public static boolean areAllInstancesMatchingType(List<?> list, Class<?> clazz) {
        SUS.checkIfNulls("Null list or class.", list, clazz);

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null && !clazz.isAssignableFrom(list.get(i).getClass())) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Diagnostic entry point: loads {@link #loadSystemInfoDAO()} and prints the system
     * properties, network interfaces and the JSON rendering to stdout.
     * @param args ignored
     */
    public static void main(String... args) {
        try {
            SystemInfoDAO siDAO = loadSystemInfoDAO();
            System.out.println(SUS.toCanonicalID(':', siDAO.getName(), siDAO.getDescription()));
            System.out.println(siDAO.getSystemProperties());
            NVEntity[] allInterfaces = siDAO.getNetworkInterfaces().values();
            //Iterator<NetworkInterfaceDAO> it = (Iterator<NetworkInterfaceDAO> )allInterfaces.iterator();
            for (int i = 0; i < allInterfaces.length; i++) {
                NetworkInterfaceDAO niDAO = (NetworkInterfaceDAO) allInterfaces[i];
                System.out.println(SUS.toCanonicalID(',', i, niDAO.getName(), niDAO.getMACAddress()));
                System.out.println(niDAO.getInetAddresses());
            }

            System.out.println(GSONUtil.toJSON(siDAO, true));

            System.out.println(siDAO.getContent().getClass().getName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Done");
    }


    /**
     * Null-tolerant {@link Lock#lock()}: does nothing when {@code lock} is null.
     * @param lock to acquire, may be null
     */
    public static void lock(Lock lock) {
        if (lock != null)
            lock.lock();
    }

    /**
     * Null-tolerant {@link Lock#unlock()}: does nothing when {@code lock} is null.
     * @param lock to release, may be null
     */
    public static void unlock(Lock lock) {
        if (lock != null)
            lock.unlock();
    }

    /**
     * Whether the JVM is running on macOS, decided once from the {@code os.name}
     * system property (case-insensitive contains "mac") and cached under {@link #LOCK}.
     * @return true on macOS
     */
    public static boolean isMacOS() {

        if (isMac == null) {
            LOCK.lock();

            try {
                if (isMac == null) {
                    isMac = new AtomicBoolean();
                    isMac.set(System.getProperty("os.name")
                            .toLowerCase()
                            .contains("mac"));
                }
            } finally {
                LOCK.unlock();
            }
        }

        return isMac.get();
    }

}
