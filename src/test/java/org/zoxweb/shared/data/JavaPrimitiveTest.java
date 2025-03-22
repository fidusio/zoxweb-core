package org.zoxweb.shared.data;

import org.junit.jupiter.api.Test;

public class JavaPrimitiveTest {
    @Test
    public void byteClassNames() throws ClassNotFoundException {
        System.out.println("byte[]:" + byte[].class.getName());
        System.out.println("boolean:" + Boolean.class.getName());
        Class.forName(Boolean.class.getName());
        System.out.println("byte:" + byte.class.getName());
        System.out.println("int:" + int.class.getName());
        System.out.println("int[]:" + int[].class.getName());





        assert byte[].class.equals(Class.forName("[B"));
        assert int[].class.equals(Class.forName("[I"));

    }
}
