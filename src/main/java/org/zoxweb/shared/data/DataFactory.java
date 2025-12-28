package org.zoxweb.shared.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVEntityInstance;

public class DataFactory
implements NVEntityFactory
{
  
  public static final DataFactory SINGLETON = new DataFactory();
  
  
  private DataFactory()
  {
    
  }
  
  
//  static class NVEntityInstanceKey
//  {
//
//    private NVEntityInstance nvei = null;
//    
//    NVEntityInstanceKey(NVEntityInstance nvei)
//    {
//      this.nvei = nvei;
//    }
//   
//    public boolean equals(Object o)
//    {
//      if(o != null)
//      {
//        if (o instanceof String)
//        {
//          String canonicalID = (String) o;
//          if (canonicalID.equals(nvei.getNVConfigEntity().toCanonicalID())
//              || canonicalID.equals(nvei.getNVConfigEntity().getName()))
//          {
//            return true;
//          }
//        }
//      }
//      return false;
//    }
//    
//    public int hashCode()
//    {
//      return nvei.getNVConfigEntity().toCanonicalID().hashCode();
//    }
//    
//  }
  
  private Map<Object, NVEntityInstance> metaNVE = new HashMap<Object, NVEntityInstance>();
  
  private Map<String, NVEntityInstance> classMap = new HashMap<String, NVEntityInstance>();
  private Map<String, NVEntityInstance> canIDMap = new HashMap<String, NVEntityInstance>();
  private Map<String, NVEntityInstance> nameMap = new HashMap<String, NVEntityInstance>();
  private Set<NVEntityFactory> factoriesSet = new HashSet<>();
  
  @Override
  public <V extends NVEntity> V createNVEntity(String className) {
    // TODO Auto-generated method stub
    NVEntityInstance ret = classMap.get(className);
    if (ret == null)
      ret = nameMap.get(className);
    if (ret == null)
      ret = canIDMap.get(className);
    if (ret != null)
    {
      return ret.newInstance();
    }
    
    return null;
  }

  public synchronized void registerFactory(NVEntityFactory factory, NVEntityInstance[] nvies)
  {
      if (factory != null)
      {
          factoriesSet.add(factory);
      }
      
      int cClass = classMap.size();
      int cCan = canIDMap.size();
      int cName = nameMap.size();
      for (NVEntityInstance nvie: nvies)
      {
        if (nvie.getNVConfigEntity().getName() == null)
        {
          System.out.println("metaName null:" + nvie.getNVConfigEntity().getMetaTypeBase());
        }
        classMap.put(nvie.getNVConfigEntity().getMetaTypeBase().getName(), nvie);
        if (nvie.getNVConfigEntity().toCanonicalID() != null)
          canIDMap.put(nvie.getNVConfigEntity().toCanonicalID(), nvie);
        
        nameMap.put(nvie.getNVConfigEntity().getName(), nvie);
        if (cClass == classMap.size())
        {
          System.out.println("classname not added:"  + nvie.getNVConfigEntity().getMetaTypeBase());
        }
        else
        {
          cClass++;
        }
        
        if (cCan == canIDMap.size())
        {
          System.out.println("can id not added:"  + nvie.getNVConfigEntity().getMetaTypeBase());
        }
        else
        {
          cCan++;
        }
        
        if (cName == nameMap.size())
        {
          System.out.println("name not added:"  + nvie.getNVConfigEntity().getMetaTypeBase());
        }
        else
        {
          cName++;
        }
        
      }
  }
  
  public int size()
  {
    return metaNVE.size();
  }
}
