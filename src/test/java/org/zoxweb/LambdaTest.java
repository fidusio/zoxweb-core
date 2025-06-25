/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Tony Test Comment
 */


package org.zoxweb;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.Const.TimeInMillis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LambdaTest {

    interface AddLamba {

        int add(int x, int y);

        default int sub(int x, int y) {
            return x - y;
        }
    }
    @Test
    public void test() {

        AddLamba t = Integer::sum;

        AddLamba simpleAdd = (x, y) -> (x + y);

        Runnable r = () -> System.out.println("Tizi " + new Date() + " " + Thread.currentThread());

        r.run();
        r.run();

        System.out.println("CurrentThread " + Thread.currentThread());
        System.out.println(t.add(1, 2));
        System.out.println(simpleAdd.add(5, 6));
        System.out.println(t.sub(1, 2));
        System.nanoTime();
        String s = "John";
        String s2 = "John";
        System.out.println((s == s2) + " " + s.hashCode() + " " + s2.hashCode());
        System.out.println((s == s2) + " " + new Object().hashCode() + " " + new Object().hashCode());

        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add("One");
            list.add("OneAndOnly");
            list.add("Derek");
            list.add("Change");
            list.add("factory");
            list.add("justBefore");
            list.add("Italy");
            list.add("Italy");
            list.add("Thursday");
            list.add("");
            list.add("");
        }
        long dt = System.nanoTime();
        Stream<String> stream = list.stream().filter(element -> element.contains("f"));
        List<String> result = stream.collect(Collectors.toList());
        dt = System.nanoTime() - dt;
        //stream.forEach(System.out::println);
        System.out.println(result);
        System.out.println("It took: " + TimeInMillis.nanosToString(dt));

        dt = System.nanoTime();
        dt = System.nanoTime() - dt;
        System.out.println("nano to string: " + TimeInMillis.nanosToString(dt));

        System.out.println(3511 ^ 3671 ^ 4153);


    }
}
