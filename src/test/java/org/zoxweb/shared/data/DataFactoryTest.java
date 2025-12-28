package org.zoxweb.shared.data;

//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVEntityInstance;

import java.util.HashSet;
import java.util.Set;

public class DataFactoryTest {
  @Test
  public void testDataFactory()
  {
      DataFactory.SINGLETON.registerFactory(ZWDataFactory.SINGLETON, ZWDataFactory.NVEntityTypeClass.values());
      long zwDelta;
      long dataDelta;
      NVEntity one;
      NVEntity two;
      
      
      for (NVEntityInstance nvei : ZWDataFactory.NVEntityTypeClass.values())
      {
          DataFactory.SINGLETON.createNVEntity(nvei.getNVConfigEntity().getName());
          ZWDataFactory.SINGLETON.createNVEntity(nvei.getNVConfigEntity().getName());
      }
      
      int negCounter = 0;
      long totalData = 0;
      long totalZW = 0;
      for (NVEntityInstance nvei : ZWDataFactory.NVEntityTypeClass.values())
      {
          dataDelta = System.nanoTime();
          one = DataFactory.SINGLETON.createNVEntity(nvei.getNVConfigEntity().getMetaTypeBase().getName());
          dataDelta = System.nanoTime() - dataDelta;
          
          zwDelta = System.nanoTime();
          two = ZWDataFactory.SINGLETON.createNVEntity(nvei.getNVConfigEntity().getName());
          zwDelta = System.nanoTime() - zwDelta;
          long diff = zwDelta - dataDelta;
          if (diff < 0 )
          {
            negCounter++;
            totalZW += diff;
          }
          else 
          {
            totalData += diff;
          }
          
          
          if (one != null && two !=null && !one.getClass().equals(two.getClass()))
          {
              System.out.println("mismatch:" + one.getClass() + "," + two.getClass()) ; 
          }
          else if (!one.getClass().equals(two.getClass()))
             System.out.println(nvei.getNVConfigEntity().getName() +":"+ one.getClass().getName() +":"+ two.getClass().getName());
          
          System.out.println("dataDelta:" + dataDelta +", zwDelta:" + zwDelta + ", diff:" +(diff) + (diff < 0 ? ", "  + nvei.getNVConfigEntity().getName() : ""));
          
          
      }
      
      System.out.println("Total:" +  ZWDataFactory.NVEntityTypeClass.values().length + " slow:" + negCounter +  " result:"+(totalData + totalZW));
  }
  @Test
  public  void testSize()
  {
      Set<String> nameSet = new HashSet<String>();
      Set<NVConfigEntity> nvceSet = new HashSet<NVConfigEntity>();
      Set<NVEntity> nveSet = new HashSet<NVEntity>();
      for (ZWDataFactory.NVEntityTypeClass nvetc : ZWDataFactory.NVEntityTypeClass.values())
      {
          nameSet.add(nvetc.getName());
          nvceSet.add(nvetc.getNVConfigEntity());
          nveSet.add(ZWDataFactory.SINGLETON.createNVEntity(nvetc.getName()));
      }
      assert(ZWDataFactory.NVEntityTypeClass.values().length == (nameSet.size() & nvceSet.size() & nveSet.size()));
      System.out.println(ZWDataFactory.NVEntityTypeClass.values().length + " " + nameSet.size() + " " + nvceSet.size() + " " + nveSet.size());

  }
  

  
}
