package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.BytesValue;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedBase64;

import java.nio.ByteBuffer;
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


    @Test
    public void test() {
        long most = System.nanoTime();
        long least = System.nanoTime();
        int size = 10;
        long listNanos[] = new long[size];

        for (int i = 0; i < size; i++) {
            listNanos[i] = System.nanoTime();
        }

        UUID uuid = new UUID(most, least);

        System.out.println(uuid);
        System.out.println("most\t:" + most);
        System.out.println("least\t:" + least);

        long last = 0L;

        for (long n : listNanos) {
            System.out.println(n + " last equals " + (last == n) + " " + UUID.randomUUID() + " " + Long.toHexString(n));
            last = n;
        }

        uuid = UUID.fromString("94ecdf50-cdb9-4668-941b-6005108b4840");
        System.out.println(uuid + " most " + uuid.getMostSignificantBits() + " least " + uuid.getLeastSignificantBits());


        byte result[] = SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, "x3JJHMbDL1EzLkh9GBhXDw==");

        System.out.println(convertBytesToUUID(result));

        System.out.println("" + result.length);
        uuid = new UUID(BytesValue.LONG.toValue(result, 0), BytesValue.LONG.toValue(result, 8));
        System.out.println(uuid);

        uuid = UUID.fromString("94ecdf50-cdb9-4668-941b-6005108b4840");
        System.out.println("\n" + uuid);
        result = BytesValue.LONG.toBytes(new byte[16], 0, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
        byte[] leastBytes = BytesValue.LONG.toBytes(uuid.getLeastSignificantBits());
        System.out.println(uuid.getLeastSignificantBits() + " " + BytesValue.LONG.toValue(leastBytes));

        byte[] mostBytes = BytesValue.LONG.toBytes(uuid.getMostSignificantBits());
        System.out.println(uuid.getMostSignificantBits() + " " + BytesValue.LONG.toValue(mostBytes));
        uuid = new UUID(BytesValue.LONG.toValue(mostBytes), BytesValue.LONG.toValue(leastBytes));
        System.out.println(uuid);

        String webSocketTag = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    }


    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
}
