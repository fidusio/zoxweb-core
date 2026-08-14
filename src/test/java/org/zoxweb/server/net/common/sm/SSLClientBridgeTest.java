package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.fsm.State;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.util.SharedStringUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bridge decrypted-data delivery contract: the engine's inAppData arrives in write-mode and is
 * reused, so the bridge must publish a detached read-mode copy as IN_DATA and fully drain
 * (clear) the source; an empty buffer publishes nothing.
 */
public class SSLClientBridgeTest {

    @Test
    public void writeModeAppDataPublishedAsDetachedReadModeCopy() {
        ClientConSM sm = ClientConSMBuilder.create("bridge-data").build();
        final AtomicReference<String> data = new AtomicReference<String>();
        State<Object> app = new State<Object>("app");
        app.register((Consumer<ByteBuffer>) bb -> {
            byte[] chunk = new byte[bb.remaining()];
            bb.get(chunk);
            data.set(new String(chunk));
            ByteBufferUtil.cache(bb);
        }, ClientEvent.IN_DATA);
        sm.register(app);

        SSLClientBridge bridge = new SSLClientBridge(sm);
        ByteBuffer inAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, 1024);
        inAppData.put(SharedStringUtil.getBytes("secret-app-data")); // write-mode, engine style

        bridge.accept(inAppData);

        assertEquals("secret-app-data", data.get(), "decrypted bytes must arrive as IN_DATA");
        assertEquals(0, inAppData.position(), "source must be cleared (fully drained)");
        assertEquals(inAppData.capacity(), inAppData.limit(), "source must be back in write-mode");
        ByteBufferUtil.cache(inAppData);
    }

    @Test
    public void emptyAppDataPublishesNothing() {
        ClientConSM sm = ClientConSMBuilder.create("bridge-empty").build();
        final AtomicInteger count = new AtomicInteger();
        State<Object> app = new State<Object>("app");
        app.register((Consumer<ByteBuffer>) bb -> count.incrementAndGet(), ClientEvent.IN_DATA);
        sm.register(app);

        SSLClientBridge bridge = new SSLClientBridge(sm);
        ByteBuffer inAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, 1024);

        bridge.accept(inAppData);

        assertEquals(0, count.get(), "an empty unwrap must not publish IN_DATA");
        ByteBufferUtil.cache(inAppData);
    }
}
