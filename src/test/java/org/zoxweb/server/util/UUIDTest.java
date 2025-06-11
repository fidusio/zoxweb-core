package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.RateCounter;

import java.util.UUID;

public class UUIDTest {
    @Test
    public void uuidTest()
    {
        UUID[] array = new UUID[100];
        UUID.randomUUID();
        RateCounter rc = new RateCounter("test");
        rc.start();
        for(int i = 0; i < array.length; i++) {
            array[i] = UUID.randomUUID();
        }
        rc.stop();
        System.out.println(rc);
        rc.reset();
        rc.start();
        int count = 0;
        for(int j = 0; j < 1000; j++) {
            for (int i = 0; i < array.length; i++) {
                UUID temp = UUID.fromString(array[i].toString());
                assert temp.equals(array[i]);
                assert temp != array[i];
                count++;
            }
        }
        rc.stop();
        System.out.println("Iteration " + count + " for uuids " +rc);


    }
}
