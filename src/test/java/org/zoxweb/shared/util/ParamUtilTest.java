package org.zoxweb.shared.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.ParamInfo;

import java.util.Arrays;


public class ParamUtilTest {

    @Test
    public void parameterParsing() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");
        System.out.println(result);
        result = ParamUtil.parse("-", false, "https://localhost", "-URI", "context", "-content", "hello", "5");
        System.out.println(result);
    }


    @Test
    public void defaultValues() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");

        System.out.println(result.intValue("-c", 5));
        System.out.println(result.longValue("-c", (long) 5));
        System.out.println(result.stringValue("-c", "hrooo"));
        System.out.println(result.stringValue(0));
    }

    @Test
    public void paramInfos() {
        ParamUtil.ParamInfoList pil = new ParamUtil.ParamInfoList().add("uri", ParamInfo.ValueType.SINGLE, "-URI", true, false)
                .add("content", ParamInfo.ValueType.MULTI, "-content", false, true);


        ParamUtil.ParamMap result = ParamUtil.parse(pil, "https://localhost", "-URI", "context", "-content", "hello", "5", "/hello");
        System.out.println(result.lookup("content"));
        System.out.println(result.lookup("-content"));
        System.out.println(pil);
        System.out.println(result);
        System.out.println(result.stringValue("-URI"));
        System.out.println(result.longValue("-c", (long) 5));
        System.out.println(result.stringValue("-c", "hrooo"));
        System.out.println(result.stringValue(0));
        System.out.println(result.lookup("content") == result.lookup("-content"));
        System.out.println(result.lookup("content"));
        System.out.println(result.lookup("-content"));


    }


    @Test//(expected = IllegalArgumentException.class)
    public void errorTest() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "https://localhost", "-URI", "context", "-content", "hello", "5", "-uri", "/hello");

        Assertions.assertThrows(IllegalArgumentException.class, () -> System.out.println(result.intValue("-c")));
    }

    @Test
    public void testMultiInput() {
        String[] params = {
                "-i2c command ping aref",
                "-ws congig.json -i2c command cpu aref -url https://google.com -url http://zoxweb.com"
        };

        for (String param : params) {
            ParamUtil.ParamMap result = ParamUtil.parse("-", param.split(" "));
            System.out.println(result.namelessCount() + " " + result);
            String i2c = result.stringValue("-i2c");
            System.out.println(Arrays.toString(result.namelessValues(i2c)));


        }
    }

    // ---------------------------------------------------------------
    // charsValue(...) overloads
    // ---------------------------------------------------------------

    private enum Key implements GetName {
        PASSWORD("-password"),
        MISSING("-missing");

        private final String name;

        Key(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Test
    public void charsValueByName() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-user", "bob", "-password", "s3cr3t!");

        char[] chars = result.charsValue("-password");
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), chars);
        Assertions.assertEquals(result.stringValue("-password"), new String(chars));
    }

    @Test
    public void charsValueByNameIgnoresCase() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-PassWord", "s3cr3t!");
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue("-password"));
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue("-PASSWORD"));
    }

    @Test
    public void charsValueByNameCaseSensitive() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", false, "-PassWord", "s3cr3t!");
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue("-PassWord"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue("-password"));
        Assertions.assertNull(result.charsValue("-password", true));
    }

    @Test
    public void charsValueByNameMissingThrows() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-user", "bob");
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue("-password"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue("-password", false));
    }

    @Test
    public void charsValueByNameMissingNullOk() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-user", "bob");
        Assertions.assertNull(result.charsValue("-password", true));
    }

    @Test
    public void charsValueByNameMultiValueThrows() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-password", "one", "-password", "two");
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue("-password"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue("-password", true));
    }

    @Test
    public void charsValueByIndex() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "first", "-user", "bob", "second");
        Assertions.assertEquals(2, result.namelessCount());
        Assertions.assertArrayEquals("first".toCharArray(), result.charsValue(0));
        Assertions.assertArrayEquals("second".toCharArray(), result.charsValue(1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue(2));
    }

    @Test
    public void charsValueByGetName() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-password", "s3cr3t!");
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue(Key.PASSWORD));
        Assertions.assertThrows(IllegalArgumentException.class, () -> result.charsValue(Key.MISSING));
    }

    @Test
    public void charsValueEmptyString() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-password", "");
        char[] chars = result.charsValue("-password");
        Assertions.assertNotNull(chars);
        Assertions.assertEquals(0, chars.length);
    }

    @Test
    public void charsValueTagWithoutValueIsNull() {
        // "-password" is the last token: the parser records the name with a null value
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-user", "bob", "-password");
        Assertions.assertTrue(result.nameExists("-password"));
        Assertions.assertNull(result.stringValue("-password"));
        Assertions.assertNull(result.charsValue("-password"));
        Assertions.assertNull(result.charsValue(Key.PASSWORD));
    }

    @Test
    public void charsValueNameEqualsValueSyntax() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "password=s3cr3t!");
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue("password"));
    }

    @Test
    public void charsValueReturnsIndependentCopy() {
        ParamUtil.ParamMap result = ParamUtil.parse("-", "-password", "s3cr3t!");
        char[] first = result.charsValue("-password");
        char[] second = result.charsValue("-password");
        Assertions.assertNotSame(first, second);
        Assertions.assertArrayEquals(first, second);

        // wiping the returned array (typical secret handling) must not alter the map
        Arrays.fill(first, '\0');
        Assertions.assertEquals("s3cr3t!", result.stringValue("-password"));
        Assertions.assertArrayEquals("s3cr3t!".toCharArray(), result.charsValue("-password"));
    }
}
