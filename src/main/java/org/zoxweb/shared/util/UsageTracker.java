package org.zoxweb.shared.util;

public interface UsageTracker
     extends IsExpired
{

     /**
      *
      * @return last usage
      */
     long lastUsage();


     /**
      * @return current usage update
      */
     long updateUsage();


     long updateUsage(long value);


}
