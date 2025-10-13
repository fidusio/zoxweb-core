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
package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.NetUtil;
import org.zoxweb.shared.data.SystemInfoDAO;
import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.util.*;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ServerUtil {

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
     * Utility method to wait on a object
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
     * Utility method to wait on a object
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
     * Concatenates two arrays as one
     * @param array1
     * @param array2
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

        if (array1 != null && array2 == null) {
            return array1;
        }

        return null;
    }

    /**
     * This method reads files based on the file name/location
     * on the computer and stores the data in an array list of
     * strings.
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
            IOUtil.close(fis);
        }

        return ret;
    }

    /**
     * This method is used to convert input streams to an
     * array list of strings.
     * @param is
     * @return string list
     * @throws IOException
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
            IOUtil.close(textReader);
            IOUtil.close(is);
        }

        return messageList;

    }

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

        IOUtil.close(decoder);
        IOUtil.close(is);

        return ret.toArray();
    }

    /**
     * Write beans to XML file.
     * @param os
     * @param objs
     */
    public static void writeBeansToXML(OutputStream os, Object... objs) {

        XMLEncoder enc = new XMLEncoder(os);

        for (Object o : objs) {
            enc.writeObject(o);
        }

        enc.flush();
        IOUtil.close(enc);
        IOUtil.close(os);
    }

    /**
     * Load the systeminfo
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
                        niDAO.setMACAddress(SharedStringUtil.bytesToHex(ni.getHardwareAddress(), ":"));
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

    public static SystemInfoDAO loadSystemInfoDAO() {
        return loadSystemInfoDAO(true);
    }

//	public static void configureLogger(Logger logger, String filename) throws SecurityException, IOException
//	{
//		 // This block configure the logger with handler and formatter  
//		FileHandler  fh = new FileHandler(filename);  
//		
//		
//		
//		
//		
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFormatter();  
//        fh.setFormatter(formatter); 
//        logger.info("Logger activated");
//	}

    /**
     * Create delay in nanos
     * @param nanos to delay
     * @return the difference
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
     * @param timeToSleepNanos
     * @return delay
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
     * Check if the list of all object are derived from Clazz
     * @param list of objects
     * @param clazz to be matched with
     * @return true if all the list object are matching
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


    public static void main(String... args) {
        try {
            SystemInfoDAO siDAO = loadSystemInfoDAO();
            System.out.println(SharedUtil.toCanonicalID(':', siDAO.getName(), siDAO.getDescription()));
            System.out.println(siDAO.getSystemProperties());
            NVEntity[] allInterfaces = siDAO.getNetworkInterfaces().values();
            //Iterator<NetworkInterfaceDAO> it = (Iterator<NetworkInterfaceDAO> )allInterfaces.iterator();
            for (int i = 0; i < allInterfaces.length; i++) {
                NetworkInterfaceDAO niDAO = (NetworkInterfaceDAO) allInterfaces[i];
                System.out.println(SharedUtil.toCanonicalID(',', i, niDAO.getName(), niDAO.getMACAddress()));
                System.out.println(niDAO.getInetAddresses());
            }

            System.out.println(GSONUtil.toJSON(siDAO, true));

            System.out.println(siDAO.getContent().getClass().getName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Done");
    }


    public static void lock(Lock lock) {
        if (lock != null)
            lock.lock();
    }

    public static void unlock(Lock lock) {
        if (lock != null)
            lock.unlock();
    }

}