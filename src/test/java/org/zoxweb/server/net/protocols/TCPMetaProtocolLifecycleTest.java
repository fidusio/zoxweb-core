package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The TCP validator's session-lifecycle contract: exception = close-cause stash + verdict +
 * close once; close idempotence; the completion latch; transport guard at construction.
 */
public class TCPMetaProtocolLifecycleTest {

    @Test
    public void udpDefinitionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new TCPMetaProtocol("t", "{\"transport\": \"udp\"}"));
    }

    @Test
    public void malformedDefinitionRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new TCPMetaProtocol("t", "{\"exchange\": [{\"expect\": \"hex:XYZ\"}]}"));
        assertThrows(IllegalArgumentException.class,
                () -> new TCPMetaProtocol("t", "{\"protocol\": \"ssh\"}"));
    }

    @Test
    public void exceptionStashesCauseRecordsVerdictAndClosesOnce() throws Exception {
        TCPMetaProtocol v = new TCPMetaProtocol("t", "{\"exchange\": [{\"expect\": \"txt:never\"}]}");
        assertFalse(v.isClosed());
        assertFalse(v.waitForClose(0), "poll on an open session is false");

        IOException boom = new IOException("boom");
        v.exception(boom);
        assertTrue(v.isClosed(), "exception must close the session");
        assertSame(boom, v.getCloseCause());
        assertEquals(Boolean.FALSE, v.getResults().getValue("validated"),
                "externally-caused failure still yields a verdict");
        assertEquals("boom", v.getResults().getValue("reason"));
        assertTrue(v.waitForClose(1000), "latch released on close");

        // a later exception must not overwrite the first cause
        v.exception(new IOException("later"));
        assertSame(boom, v.getCloseCause());
    }

    @Test
    public void closeIsIdempotent() throws Exception {
        TCPMetaProtocol v = new TCPMetaProtocol("t", "{\"name\": \"bare\"}");
        v.close();
        v.close();
        assertTrue(v.isClosed());
        assertNull(v.getCloseCause(), "manual close carries no cause");
        assertTrue(v.waitForClose(0), "poll after close is true");
    }

    // ---- onClose: the event-driven completion hook ----

    @Test
    public void onCloseFiresExactlyOnceWithFinalVerdict() throws Exception {
        TCPMetaProtocol v = new TCPMetaProtocol("t", "{\"exchange\": [{\"expect\": \"txt:never\"}]}");
        final java.util.concurrent.atomic.AtomicInteger fired = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<Object> verdict =
                new java.util.concurrent.atomic.AtomicReference<Object>();
        v.onClose(closed -> {
            fired.incrementAndGet();
            // the hook must see the finished session: verdict recorded, cause stashed
            verdict.set(closed.getResults().getValue("validated"));
        });
        assertEquals(0, fired.get(), "hook must not fire before close");

        v.exception(new IOException("boom"));
        assertEquals(1, fired.get(), "hook fires on close");
        assertEquals(Boolean.FALSE, verdict.get(), "hook sees the final verdict");

        v.close(); // idempotent close must not re-fire
        assertEquals(1, fired.get(), "hook fires exactly once");
    }

    @Test
    public void onCloseAfterCloseFiresImmediately() throws Exception {
        TCPMetaProtocol v = new TCPMetaProtocol("t", "{\"name\": \"bare\"}");
        v.close();
        final java.util.concurrent.atomic.AtomicInteger fired = new java.util.concurrent.atomic.AtomicInteger();
        v.onClose(closed -> fired.incrementAndGet());
        assertEquals(1, fired.get(), "a hook registered on a closed session runs immediately");
    }

    @Test
    public void onCloseThrowingHookDoesNotUnwindTheCloser() throws Exception {
        TCPMetaProtocol v = new TCPMetaProtocol("t", "{\"name\": \"bare\"}");
        v.onClose(closed -> {
            throw new RuntimeException("hook boom");
        });
        v.close(); // must not throw
        assertTrue(v.isClosed());
    }

    // ---- ProtoConnect callback factories: Consumer<NVGenericMap> wired through onClose ----

    @Test
    public void factoryCallbackDeliversFinalResultsOnce() {
        final java.util.concurrent.atomic.AtomicInteger fired = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<org.zoxweb.shared.util.NVGenericMap> bag =
                new java.util.concurrent.atomic.AtomicReference<org.zoxweb.shared.util.NVGenericMap>();
        TCPMetaProtocol v = ProtoConnect.createTCPProtocol(
                new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 9),
                "{\"exchange\": [{\"expect\": \"txt:never\"}]}",
                results -> {
                    fired.incrementAndGet();
                    bag.set(results);
                });
        assertEquals(0, fired.get(), "callback must not fire before close");

        v.exception(new IOException("boom"));
        assertEquals(1, fired.get(), "callback fires on close");
        assertSame(v.getResults(), bag.get(), "callback receives the session's results bag");
        assertEquals(Boolean.FALSE, bag.get().getValue("validated"));
    }

    @Test
    public void factoryNullCallbackRejected() {
        assertThrows(NullPointerException.class, () -> ProtoConnect.createTCPProtocol(
                new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 9),
                "{\"name\": \"bare\"}",
                (java.util.function.Consumer<org.zoxweb.shared.util.NVGenericMap>) null));
    }
}
