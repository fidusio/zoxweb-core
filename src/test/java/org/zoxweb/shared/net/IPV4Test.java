package org.zoxweb.shared.net;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;

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
       NIConfigDAO nicd = GSONUtil.fromJSON(json, NIConfigDAO.class);
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
       NIConfigDAO nicd = GSONUtil.fromJSON(json, NIConfigDAO.class);
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

}
