package org.zoxweb.server.net.protocols.pqc;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.NVGenericMap;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The auditor's run-identity contract (META-TCP-PQC.md §8): the shared
 * {@code ProtoUtil.ResKey} entries are present from construction, and {@code close_ts}
 * freezes exactly once on close — hermetic, no connection is ever opened.
 */
public class TCPPQCProtocolIdentityTest {

    @Test
    public void resultsCarryRunIdentityFromBirth() throws Exception {
        TCPPQCProtocol audit = new TCPPQCProtocol(null,
                InetSocketAddress.createUnresolved("pqc.example.com", 8443));
        NVGenericMap r = audit.getResults();

        UUID.fromString((String) r.getValue("guid"));
        assertEquals(TCPPQCProtocol.PROTOCOL_NAME, r.getValue("proto-name"));
        assertEquals("tcp", r.getValue("transport"));
        assertEquals("pqc.example.com", r.getValue("host"));
        assertEquals(8443, (Integer) r.getValue("port"));
        assertTrue((Long) r.getValue("open_ts") > 0, "open_ts stamps at construction");
        assertNull(r.getValue("close_ts"), "close_ts must wait for the close");

        audit.close();
        long closeTs = (Long) r.getValue("close_ts");
        assertTrue(closeTs >= (Long) r.getValue("open_ts"), "close_ts freezes on close");

        audit.close(); // idempotent close must not restamp
        assertEquals(closeTs, (long) (Long) r.getValue("close_ts"));
    }

    @Test
    public void guidsAreUniquePerSession() throws Exception {
        TCPPQCProtocol a = new TCPPQCProtocol(null, InetSocketAddress.createUnresolved("h", 443));
        TCPPQCProtocol b = new TCPPQCProtocol(null, InetSocketAddress.createUnresolved("h", 443));
        assertNotEquals((String) a.getResults().getValue("guid"),
                (String) b.getResults().getValue("guid"));
        a.close();
        b.close();
    }
}
