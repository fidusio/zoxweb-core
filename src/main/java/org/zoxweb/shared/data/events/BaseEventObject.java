package org.zoxweb.shared.data.events;

import java.util.EventObject;

@SuppressWarnings("serial")
public class BaseEventObject<D>
extends EventObject
{
  private final long timestamp;
  private final D data;
  
  public BaseEventObject(Object source, D data)
  {
    super(source);
    timestamp = System.currentTimeMillis();
    this.data = data;
  }
  
  public long getTimeStamp()
  {
    return timestamp;
  }
  
  public D getData()
  {
    return data;
  }
}
