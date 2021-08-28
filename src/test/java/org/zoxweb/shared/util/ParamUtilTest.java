package org.zoxweb.shared.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.ParamInfo;

import java.util.Arrays;


public class ParamUtilTest {

    @Test
    public void parameterParsing()
    {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");
        System.out.println(result);
        result = ParamUtil.parse("-", false, "https://localhost", "-URI", "context", "-content", "hello", "5");
        System.out.println(result);
    }


    @Test
    public void defaultValues()
    {
        ParamUtil.ParamMap result = ParamUtil.parse("-","https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");

        System.out.println(result.intValue("-c", 5));
        System.out.println(result.longValue("-c", (long)5));
        System.out.println(result.stringValue("-c", "hrooo"));
        System.out.println(result.stringValue(0));
    }

    @Test
    public void paramInfos()
    {
        ParamUtil.ParamInfoList pil = new ParamUtil.ParamInfoList().add("uri", ParamInfo.ValueType.SINGLE, "-URI", true, false)
                .add("content", ParamInfo.ValueType.MULTI, "-content", false, true)
                ;



        ParamUtil.ParamMap result = ParamUtil.parse(pil,"https://localhost", "-URI", "context", "-content", "hello", "5", "/hello");
        System.out.println(result.lookup("content"));
        System.out.println(result.lookup("-content"));
        System.out.println(pil);
        System.out.println (result);
        System.out.println(result.stringValue("-URI"));
        System.out.println(result.longValue("-c", (long)5));
        System.out.println(result.stringValue("-c", "hrooo"));
        System.out.println(result.stringValue(0));
        System.out.println(result.lookup("content") == result.lookup("-content"));
        System.out.println(result.lookup("content"));
        System.out.println(result.lookup("-content"));


    }







    @Test//(expected = IllegalArgumentException.class)
    public void errorTest()
    {
        ParamUtil.ParamMap result = ParamUtil.parse("-","https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");

        Assertions.assertThrows(IllegalArgumentException.class, ()->System.out.println(result.intValue("-c")));
    }
    @Test
    public void testMultiInput()
    {
        String[] params = {
          "-i2c command ping aref",
          "-ws congig.json -i2c command cpu aref"
        };

        for(String param : params)
        {
            ParamUtil.ParamMap result = ParamUtil.parse("-", param.split(" "));
            System.out.println(result.namelessCount()  + " " + result);
            String i2c = result.stringValue("-i2c");
            System.out.println(Arrays.toString(result.namelessValues(i2c)));


        }
    }
}
