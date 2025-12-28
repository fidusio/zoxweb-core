package org.zoxweb.shared.util;


import org.junit.jupiter.api.Test;

public class GetNameValueCommentTest {



  @Test
  public void testPositiveParse() {
    String array[] = {
      "   n value ",
      "n    value #hello",
      "n   value",
      "n value"
    };
    
    for(String str : array)
    {
      System.out.println(str);
      GetNameValueComment<String> nvpc = SharedUtil.parseGetNameStringComment(str, " ", "#");
      System.out.println(nvpc);
      assert(nvpc != null);
        
      assert(nvpc.getGNV().getName().equals("n"));
      assert(nvpc.getGNV().getValue().equals("value"));
    }
  }
  
  
  @Test
  public void testNegativeParse() {
    String array[] = {
      "n#commment value",
      "#comment",
      "",
      null
    };
    
    for(String str : array)
    {
      System.out.println(str);
      GetNameValueComment <String>nvpc = SharedUtil.parseGetNameStringComment(str, " ", "#");
      assert(nvpc == null);
    }
  }
  
  
  @Test
  public void testNoCommentParse() {
    String array[] = {
        "   n value ",
        "n    value ",
        "n   value",
        "n value"
      };
      
      for(String str : array)
      {
        System.out.println(str);
        GetNameValueComment<String> nvpc = SharedUtil.parseGetNameStringComment(str, " ");
        System.out.println(nvpc);
        assert(nvpc != null);
          
        assert(nvpc.getGNV().getName().equals("n"));
        assert(nvpc.getGNV().getValue().equals("value"));
      }
  }

}
