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
}
