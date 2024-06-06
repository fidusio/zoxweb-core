package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVInt;

import java.util.Objects;

public class GSONUtilTest {

    static class TestObj
    {
        final String name;
        final String lastName;
        final int age;

        TestObj(String name, String lastName,int age)
        {
            this.name = name;
            this.lastName = lastName;
            this.age = age;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObj testObj = (TestObj) o;
            return age == testObj.age && Objects.equals(name, testObj.name) && Objects.equals(lastName, testObj.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, lastName, age);
        }

        @Override
        public String toString() {
            return "TestObj{" +
                    "name='" + name + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", age=" + age +
                    '}';
        }
    }


    @Test
    public void defaultObjects()
    {
        Object[][] objs2D = {
                {"I am string", String.class},
                {100, int.class},
                {new NVGenericMap().build(new NVGenericMap("test").build("string", "svalue").build(new NVInt("int", 10))), NVGenericMap.class},
                {new TestObj("zoxweb", "org", 10), TestObj.class},
        };

        for (Object[] obj2D : objs2D)
        {
            String json = GSONUtil.toJSONDefault(obj2D[0]);
            System.out.println("json: " + json);
            Object obj = GSONUtil.fromJSONDefault(json, (Class<?>)obj2D[1]);
            System.out.println("object: " + obj.getClass() + " val : " + obj);
        }
    }
}
