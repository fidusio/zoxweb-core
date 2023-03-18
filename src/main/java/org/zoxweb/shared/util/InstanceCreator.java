package org.zoxweb.shared.util;

public interface InstanceCreator<T>
{
   /**
    * Create a new instance based on the type T
    * @return  new instance of T
    */
   T newInstance();
}
