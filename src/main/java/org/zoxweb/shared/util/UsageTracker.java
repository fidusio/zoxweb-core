package org.zoxweb.shared.util;

public interface UsageTracker
     extends IsExpired
{

     /**
      *
      * @return last time used
      */
     long lastUsage();


     /**
      * @return current usage update
      */
     long updateUsage();


}
