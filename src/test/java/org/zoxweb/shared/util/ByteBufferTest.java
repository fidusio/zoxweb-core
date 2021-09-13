package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class ByteBufferTest {
    @Test
    public void bufferCompact()
    {
        ByteBuffer bb = ByteBuffer.allocate(10);
        System.out.println(bb);
        System.out.println("buffer.put: " + bb.put((byte) 1));
        System.out.println("buffer.put: " + bb.put((byte) 2));
        System.out.println("buffer.put: " + bb.put((byte) 3));

        System.out.println(bb);
        System.out.println("buffer.flip: " +bb.flip());

        System.out.println("buffer.get: " +bb.get());
        System.out.println("buffer.compact: "+ bb.compact());
        System.out.println("buffer.flip: " +bb.flip());
        System.out.println("buffer.compact: "+ bb.compact());
        System.out.println("buffer.flip: " +bb.flip());
        System.out.println("buffer.get: " +bb.get());
        System.out.println("buffer.get: " +bb.get());
        System.out.println("buffer.compact: "+ bb.compact());
        System.out.println("buffer.clear: "+ bb.clear());



    }
}
