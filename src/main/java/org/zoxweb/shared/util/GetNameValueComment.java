package org.zoxweb.shared.util;


public class GetNameValueComment<V>
{
  private String comment;
  private GetNameValue<V> gnv;
  
  
  public GetNameValueComment() {}
  
  public GetNameValueComment(GetNameValue<V> gnv, String comment)
  {
    setGNV(gnv);
    setComment(comment);
  }
  public void setComment(String comment)
  {
    this.comment = comment;
  }
  
  public String getComment()
  {
    return comment;
  }
  
  
  public void setGNV( GetNameValue<V> gnv)
  {
    this.gnv = gnv;
  }
  
  public GetNameValue<V> getGNV()
  {
    return gnv;
  }
  
  
  public String toString()
  {
    return gnv + "," + comment;
  }
}
