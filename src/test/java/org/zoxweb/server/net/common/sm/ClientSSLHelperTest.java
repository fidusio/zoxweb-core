package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the teardown trap: SSLSessionConfig.close() closes its SSLConnectionHelper BEFORE
 * TCPSMCallback's delegate publishes CLOSED — so the helper's close() must never close the
 * machine, and its publish must be a silent no-op once the machine is closed (the config-close
 * drain publishes during teardown).
 */
public class ClientSSLHelperTest {

    @Test
    public void helperCloseNeverClosesTheMachine() {
        ClientConnectionSM sm = ClientConnectionSMBuilder.create("helper-close").build();
        ClientSSLHelper helper = new ClientSSLHelper(sm);

        helper.close();

        assertFalse(sm.isClosed(),
                "helper.close() must be a no-op — closing the machine here would lose CLOSED");
    }

    @Test
    public void publishAfterMachineCloseIsSilentNoOp() {
        ClientConnectionSM sm = ClientConnectionSMBuilder.create("helper-publish").build();
        ClientSSLHelper helper = new ClientSSLHelper(sm);

        sm.close();

        // the config-close drain publishes with a null callback after teardown started;
        // this must not throw IllegalStateException
        assertDoesNotThrow(() -> helper.publish(NEED_WRAP, null));
    }

    @Test
    public void configIsNullBeforeUpgrade() {
        ClientConnectionSM sm = ClientConnectionSMBuilder.create("helper-config").build();
        ClientSSLHelper helper = new ClientSSLHelper(sm);

        assertNull(helper.getConfig(), "no SSL session before the upgrade ran");
    }
}
