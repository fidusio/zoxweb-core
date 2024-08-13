package org.zoxweb.shared.data;


import java.io.IOException;
//import org.junit.BeforeClass;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.ZWDataFactory.NVEntityTypeClass;
import org.zoxweb.shared.security.shiro.ShiroSessionData;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVEntityInstance;

public class DataTest {




  
  @Test
  public void dataFactoryTest()
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
     
      
      for (int i =0; i < 1; i++)
      {
        int negCounter = 0;
        long totalData = 0;
        long totalZW = 0;
      
        for (NVEntityTypeClass nvei : ZWDataFactory.NVEntityTypeClass.values())
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
            
            System.out.println("dataDelta:" + dataDelta +", zwDelta:" + zwDelta + ", diff:" +(diff) +  ", "  + nvei.getNVConfigEntity().toCanonicalID() + ":" + nvei.name() );
            if (!nvei.getNVConfigEntity().toCanonicalID().equalsIgnoreCase(nvei.name()))
            {
              System.out.println("MISSMATCH !!!!!!!!!!");
            }
                
            
            
        }
        
        System.out.println("Total:" +  ZWDataFactory.NVEntityTypeClass.values().length + " slow:" + negCounter +  " result:"+(totalData + totalZW));
      }
  }
  
  @Test
  public void testSubjectData() throws IOException
  {
    ShiroSessionData ssd = new ShiroSessionData();
    
    ssd.setSubjectID("xlogistx");
    ssd.setAppGUID("xlogistx.com-xlogistx");
    ssd.getPermissions().add("p-all");
    ssd.getRoles().add("r-all");
    
    String json = GSONUtil.toJSON(ssd, true, false, true);
    ShiroSessionData ssd1 = GSONUtil.fromJSON(json);
    String json1 = GSONUtil.toJSON(ssd1, true, false, true);
    
    assert(json.equals(json1));
//    System.out.println(json);
//    System.out.println(json1);
  }

  
}
