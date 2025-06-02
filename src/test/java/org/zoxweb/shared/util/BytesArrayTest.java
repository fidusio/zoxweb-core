package org.zoxweb.shared.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BytesArrayTest {
    static byte[][] byteIPs;
    static String[] strIPs;
    static Set<BytesArray> setBytesArrayIPs = new HashSet<>();
    static Set<String> setStrIPs = new HashSet<>();
    static  SecureRandom sr = new SecureRandom();



    static String toStrIP(byte[] addr)
    {
        return String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF);
    }

    @BeforeAll
    public static void setup()
    {



        int len = 10000;
        strIPs = new String[len];
        byteIPs = new byte[len][];
        for (int i = 0 ; i < len; i++)
        {
            byte[] addr = new byte[4];
            for (int j = 0; j < addr.length; j++)
            {
                addr[j] = (byte)sr.nextInt(255);
            }
            byteIPs[i] = addr;
            strIPs[i] = toStrIP(byteIPs[i]);//String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF);
            setBytesArrayIPs.add(new BytesArray(null, byteIPs[i]));
            setStrIPs.add(strIPs[i]);
        }
        System.out.println("BytesArray set size: " + setBytesArrayIPs.size() + " String set size: " + setStrIPs.size() );
    }
    @Test
    public void testHashSet() throws UnknownHostException {
        byte[][] buffers = {
                InetAddress.getLoopbackAddress().getAddress(),
                InetAddress.getByName("api.xlogistx.io").getAddress(),
                {8, 8, 8, 8},
                {4, 4, 4, 4}
        };

        String[] ipAddresses = {
                InetAddress.getLoopbackAddress().getHostAddress(),
                InetAddress.getByName("api.xlogistx.io").getHostAddress(),
                InetAddress.getByName("8.8.8.8").getHostAddress(),
                InetAddress.getByName("4.4.4.4").getHostAddress(),
        };

        Set<String> ipAddrSet = new HashSet<>(Arrays.asList(ipAddresses));

        Set<byte[]> byteArraySet = new HashSet<>(Arrays.asList(buffers));

        byte[] addr = {8,8,8,8};

        assert !byteArraySet.contains(addr);

        Set<BytesArray> bytesArraySet = new HashSet<>();
        for (byte[] toAdd: buffers)
        {
            bytesArraySet.add(new BytesArray(null, toAdd));
        }


        assert bytesArraySet.contains(new BytesArray(null, addr));

        BytesArray baAddress = new BytesArray(null, addr);

        final int max = 1_000_000;
        long ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert bytesArraySet.contains(baAddress);
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 1 ByteArray lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);




        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert bytesArraySet.contains(baAddress);

        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 2 ByteArray lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);



        String hotsAddress = InetAddress.getByName("8.8.8.8").getHostAddress();
        System.out.println(hotsAddress);

        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert ipAddrSet.contains(hotsAddress);
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 1 Host lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);



        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert ipAddrSet.contains(hotsAddress);
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 2 Host lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);



    }

    @Test
    public void testBigHashSet(){








        byte[] bytesAddress =  setBytesArrayIPs.toArray(new BytesArray[0])[sr.nextInt(setBytesArrayIPs.size())].asBytes();
        String strAddress = setStrIPs.toArray(new String[0])[sr.nextInt(setStrIPs.size())];

        final int max = 5_000_000;
        long ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setBytesArrayIPs.contains(new BytesArray(bytesAddress));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 1 ByteArray lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);




        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setBytesArrayIPs.contains(new BytesArray(byteIPs[i%byteIPs.length]));

        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 2 ByteArray lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);

        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setBytesArrayIPs.contains(new BytesArray(byteIPs[i%byteIPs.length]));

        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 3 ByteArray lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);





        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setStrIPs.contains(new String(strAddress.getBytes()));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 1 Host lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);



        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setStrIPs.contains(new String(strIPs[i%strIPs.length].getBytes()));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 2 Host lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);

        ts = System.currentTimeMillis();
        for(int i = 0; i < max; i++)
        {
            assert setStrIPs.contains(new String(strIPs[i%strIPs.length].getBytes()));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(" 3 Host lookup: " + Const.TimeInMillis.toString(ts) + " test count " + max);



    }
}
