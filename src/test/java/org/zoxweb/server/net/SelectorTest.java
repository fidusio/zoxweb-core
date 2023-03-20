package org.zoxweb.server.net;

import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;

public class SelectorTest {

    @Test
    public void display()
    {
        System.out.println("OP_READ: " + SelectionKey.OP_READ);
        System.out.println("OP_WRITE: " + SelectionKey.OP_WRITE);
        System.out.println("OP_CONNECT: " + SelectionKey.OP_CONNECT);
        System.out.println("OP_ACCEPT: " + SelectionKey.OP_ACCEPT);
    }
}
