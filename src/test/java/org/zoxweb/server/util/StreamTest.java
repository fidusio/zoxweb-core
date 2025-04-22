package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class StreamTest {

    public static final String[] dataStr = {
            "Hello",
            "Hello World",
            "First",
            "Second",
            "Ready",
            "Hello",
            "Hello World",
            "First",
            "Second",
            "Ready",
            "Hello",
            "Hello World",
            "First",
            "Second",
            "Ready",
            "Hello",
            "Hello World",
            "First",
            "Second",
            "Ready",
    };

    @Test
    public void streamTest()
    {
        List<String> data = Arrays.asList(dataStr);
        data.stream().forEach(s -> System.out.println(s + " " + Thread.currentThread()));
    }


    @Test
    public void streamTestParallel()
    {
        List<String> data = Arrays.asList(dataStr);
        data.parallelStream().forEach(s -> System.out.println(s + " " + Thread.currentThread()));
    }
}
