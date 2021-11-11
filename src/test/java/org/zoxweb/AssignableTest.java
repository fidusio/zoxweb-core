package org.zoxweb;

import org.junit.jupiter.api.Test;

public class AssignableTest {
    @Test
    public void isAssignable()
    {
        System.out.println("Object is assignableFrom Long :" + Object.class.isAssignableFrom(Long.class));
        System.out.println("Long is not assignableFrom Object :" + Long.class.isAssignableFrom(Object.class));
    }
}
