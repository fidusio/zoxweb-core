package org.zoxweb.shared.net;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IPV4Test {



  @Test
  public void validIPS() throws IOException {
  
    String ipList[] = {
      "10.0.0.1",
      "192.168.0.1",
      "1.1.1.1",
      "1.2.3.4",
      "128.128.128.128",
      "127.128.128.128",
      "255.255.255.255",
    };
    
    for(String ip : ipList)
    {
      byte[] ipV4Bytes = InetAddress.getByName(ip).getAddress();
      byte[] ipV4Shared = SharedNetUtil.getV4Address(ip);
      System.out.println(SharedNetUtil.toV4Address(ipV4Bytes) + " " + Arrays.toString(ipV4Bytes) + " " + Arrays.toString(ipV4Shared));
      
      assert(Arrays.equals(ipV4Bytes, ipV4Shared));
      assert(ip.equals(SharedNetUtil.toV4Address(ipV4Shared)));
    }
  }

  @Test
  public void invalidIPS(){

    String ipList[] = {
            "10.0.0.1983",
            "192.168.0",
            "1.1.1.1.1",
            "1.2.3.a",
            "255.255.255.256",
            "batata.com",
            null
    };

    for(String ip : ipList)
    {

      try {
        SharedNetUtil.getV4Address(ip);
        assert(false);
      } 
      catch(Exception e)
      {

      }
    }
  }

  @Test
  public void networkTest() throws IOException
  {
    byte netmask[] = SharedNetUtil.getV4Address("255.255.128.0");
    byte router[] = SharedNetUtil.getV4Address("192.168.0.1");
    byte network[] = SharedNetUtil.getNetwork(router, netmask);

      String ipList[] = {
              "192.168.0.2",
              "192.168.0.254",
              "192.168.1.1",
              "192.168.1.155",
              "192.168.127.155",
              "192.168.128.0",
              "10.0.1.0",
      };


      for(String ip : ipList)
      {
          byte address[] = SharedNetUtil.getV4Address(ip);
          System.out.println(ip + " belongs to network " + SharedNetUtil.belongsToNetwork(address, netmask, network));
      }

  }
  
  @Test
  public void niConfigDAOPass() throws IOException
  {
    
    String data[] =
      {
        "{\"name\":\"wan\",\"properties\":{\"address\":\"192.168.1.100\",\"netmask\":\"255.255.255.0\",\"gateway\":\"192.168.1.1\",\"dns-nameservers\":\"127.0.0.1\"},\"ni_name\":\"eth0\",\"inet_proto\":\"STATIC\"}",
        "{\"name\":\"wan\",\"properties\":{\"address\":\"192.168.1.100\",\"netmask\":\"255.255.255.0\",\"dns-nameservers\":\"127.0.0.1\"},\"ni_name\":\"eth0\",\"inet_proto\":\"STATIC\"}",
        "{\"name\":\"wan\",\"ni_name\":\"eth0\",\"inet_proto\":\"DHCP\"}",
      };
    
     for (String json : data)
     {
       NIConfigInfo nicd = GSONUtil.fromJSON(json, NIConfigInfo.class);
       System.out.println(GSONUtil.toJSON(nicd, false, false, false));
       //System.out.println(nicd.getAddress());
       assert(SharedNetUtil.validateNIConfig(nicd));
     }
  }
  
  
  @Test
  public void niConfigDAOFail() throws IOException
  {
    
    String data[] =
      {
        "{\"name\":\"wan\",\"properties\":{\"address\":\"192.168.1.100\",\"netmask\":\"255.255.255.0\",\"gateway\":\"192.168.7.1\",\"dns-nameservers\":\"127.0.0.1\"},\"ni_name\":\"eth0\",\"inet_proto\":\"STATIC\"}",
        "{\"name\":\"wan\",\"properties\":{\"address\":\"192.168.1.100\",\"dns-nameservers\":\"127.0.0.1\"},\"ni_name\":\"eth0\",\"inet_proto\":\"STATIC\"}",
        "{\"name\":\"wan\",\"properties\":{\"netmask\":\"255.255.255.0\",\"gateway\":\"192.168.7.1\",\"dns-nameservers\":\"127.0.0.1\"},\"ni_name\":\"eth0\",\"inet_proto\":\"STATIC\"}",
      };
    
     for (String json : data)
     {
       NIConfigInfo nicd = GSONUtil.fromJSON(json, NIConfigInfo.class);
       System.out.println("Fail:" +GSONUtil.toJSON(nicd, false, false, false));
       System.out.println(nicd.getNetmask());
       try
       {
         assert(!SharedNetUtil.validateNIConfig(nicd));
       }
       catch(Exception e)
       {
       }
     }
  }
  
  @Test 
  public void netmaskTestPass() throws IOException
  {
    String maskList[] = {
        "0.0.0.0",
        "255.255.255.0",
        "255.255.0.0",
        "255.0.0.0",
        "128.0.0.0",
        "255.255.255.255",
        "255.255.255.252",
        "255.255.255.248",
        "255.255.255.240",
        "255.255.255.224",
        "255.255.255.192",
        "255.255.255.128",
        "255.255.255.0",
        "255.255.128.0",
    };
    
    for(String mask : maskList)
    {
        byte netmask[] = SharedNetUtil.getV4Address(mask);
        System.out.println("netmask:" + mask + "/" + SharedNetUtil.toNetmaskIPV4(netmask));
        assert(SharedNetUtil.validateV4Netmask(netmask));
    }
    
  }
  
  
  @Test 
  public void netmaskTestFail() throws IOException
  {
    String maskList[] = {
        "1.2.2.1",
        "255.0.255.0",
        "255.255.0.192",
        "255.0.0.1",
        "128.0.0.128",
        "255.0.128.0",
        "-1.255.255.255",
    };
    
    for(String mask : maskList)
    {
      try
      {
        System.out.println("netmask:" + mask);
        byte netmask[] = SharedNetUtil.getV4Address(mask);
        System.out.println("netmask:" + mask);
        
          assert(!SharedNetUtil.validateV4Netmask(netmask));
        }
        catch(IOException e) {}
    }

  }

  @Test
  public void toCIDRBlockPass() throws IOException
  {
    // address, netmask, expected CIDR block
    String data[][] = {
        {"192.168.1.100", "255.255.255.0", "192.168.1.0/24"},
        {"192.168.1.0", "255.255.255.0", "192.168.1.0/24"},
        {"10.1.2.3", "255.0.0.0", "10.0.0.0/8"},
        {"172.16.200.9", "255.255.128.0", "172.16.128.0/17"},
        {"172.16.100.9", "255.255.128.0", "172.16.0.0/17"},
        {"200.200.200.203", "255.255.255.252", "200.200.200.200/30"},
        {"192.168.1.77", "255.255.255.255", "192.168.1.77/32"},
        {"255.255.255.255", "128.0.0.0", "128.0.0.0/1"},
        {"8.8.8.8", "0.0.0.0", "0.0.0.0/0"},
    };

    for (String[] row : data)
    {
      byte address[] = SharedNetUtil.getV4Address(row[0]);
      byte addressCopy[] = Arrays.copyOf(address, address.length);
      byte netmask[] = SharedNetUtil.getV4Address(row[1]);
      byte netmaskCopy[] = Arrays.copyOf(netmask, netmask.length);

      String cidr = SharedNetUtil.toCIDRBlock(address, netmask);
      System.out.println(row[0] + " " + row[1] + " -> " + cidr);
      assertEquals(row[2], cidr);

      // the inputs must not be modified
      assertArrayEquals(addressCopy, address);
      assertArrayEquals(netmaskCopy, netmask);

      // the block must round trip: network/prefix -> same network and netmask
      String parsed[] = cidr.split("/");
      assertEquals(2, parsed.length);
      assertArrayEquals(SharedNetUtil.getNetwork(address, netmask), SharedNetUtil.getV4Address(parsed[0]));
      assertArrayEquals(netmask, SharedNetUtil.toNetmaskIPV4(Short.parseShort(parsed[1])));
    }
  }

  @Test
  public void toCIDRBlockPrefixPass() throws IOException
  {
    // address, prefix, expected CIDR block
    String data[][] = {
        {"192.168.1.100", "24", "192.168.1.0/24"},
        {"10.1.2.3", "8", "10.0.0.0/8"},
        {"172.16.200.9", "17", "172.16.128.0/17"},
        {"200.200.200.203", "30", "200.200.200.200/30"},
        {"192.168.1.77", "32", "192.168.1.77/32"},
        {"255.255.255.255", "1", "128.0.0.0/1"},
        {"8.8.8.8", "0", "0.0.0.0/0"},
    };

    for (String[] row : data)
    {
      byte address[] = SharedNetUtil.getV4Address(row[0]);
      assertEquals(row[2], SharedNetUtil.toCIDRBlock(address, Short.parseShort(row[1])));
    }

    // every prefix, checked against plain int arithmetic
    byte address[] = SharedNetUtil.getV4Address("203.119.85.173");
    int addressInt = 0xCB7755AD;
    for (short prefix = 0; prefix <= 32; prefix++)
    {
      int network = prefix == 0 ? 0 : addressInt & (-1 << (32 - prefix));
      String expected = (network >>> 24) + "." + ((network >> 16) & 0xff) + "." + ((network >> 8) & 0xff) + "." + (network & 0xff) + "/" + prefix;
      assertEquals(expected, SharedNetUtil.toCIDRBlock(address, prefix));
      // both overloads must agree
      assertEquals(expected, SharedNetUtil.toCIDRBlock(address, SharedNetUtil.toNetmaskIPV4(prefix)));
    }

    // the address must not be modified
    assertArrayEquals(SharedNetUtil.getV4Address("203.119.85.173"), address);
  }

  @Test
  public void toCIDRBlockPrefixFail() throws IOException
  {
    byte address[] = SharedNetUtil.getV4Address("192.168.1.100");

    assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toCIDRBlock(address, (short) 33));
    assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toCIDRBlock(address, Short.MAX_VALUE));

    // negative prefixes, including the ones that used to wrap around to a valid looking mask
    // -1 -> /0, -32 -> /32, -33 -> /31, -40 -> /24, -56 -> /8
    for (short prefix : new short[]{-1, -8, -31, -32, -33, -40, -56, Short.MIN_VALUE})
    {
      assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toCIDRBlock(address, prefix), "prefix " + prefix);
      assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toNetmaskIPV4(prefix), "prefix " + prefix);
    }
    assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toNetmaskIPV4((short) 33));
    assertThrows(NullPointerException.class, () -> SharedNetUtil.toCIDRBlock(null, (short) 24));
    assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(new byte[16], (short) 24));
    assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(new byte[3], (short) 24));
  }

  @Test
  public void toCIDRBlockFail() throws IOException
  {
    byte address[] = SharedNetUtil.getV4Address("192.168.1.100");
    byte netmask[] = SharedNetUtil.getV4Address("255.255.255.0");

    // null parameters
    assertThrows(NullPointerException.class, () -> SharedNetUtil.toCIDRBlock(null, netmask));
    assertThrows(NullPointerException.class, () -> SharedNetUtil.toCIDRBlock(address, null));

    // non contiguous netmasks
    String badMasks[] = {
        "255.0.255.0",
        "255.255.0.192",
        "1.2.2.1",
        "0.255.255.255",
    };
    for (String mask : badMasks)
    {
      byte badMask[] = SharedNetUtil.getV4Address(mask);
      assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(address, badMask), mask);
    }

    // not ipv4 lengths
    assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(new byte[16], netmask));
    assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(new byte[3], netmask));
    assertThrows(IOException.class, () -> SharedNetUtil.toCIDRBlock(new byte[0], netmask));
    assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toCIDRBlock(address, new byte[16]));
    assertThrows(IllegalArgumentException.class, () -> SharedNetUtil.toCIDRBlock(address, new byte[3]));
  }

}
