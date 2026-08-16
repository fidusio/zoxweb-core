package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Declarative-config factory: the state-catalog composition — explicit {@code states} and the
 * sugar shapes (protocol / tls / exchange) — builds the right state sets, the config carries no
 * endpoint (only an optional default-port hint + timeout), and contradictory or unsafe configs
 * fail fast.
 */
public class ClientSMFactoryTest {

    @Test
    public void tlsImmediateShape() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"smtps-client\", \"port\": 465,"
                        + " \"protocol\": \"tls\", \"tls\": {\"mode\": \"immediate\", \"cert_validation\": false},"
                        + " \"timeout_sec\": 7 }");

        assertEquals("smtps-client", sm.getName());
        assertNotNull(sm.lookupState(ClientTransportState.NAME));
        assertNotNull(sm.lookupState(SSLClientState.NAME), "TLS protocol must compose the ssl state");
        assertNotNull(sm.lookupState(SSLClientHandshakeState.NAME), "ssl state must bring the handshake state");
        assertNotNull(sm.lookupState(SSLClientDataState.NAME));
        assertNull(sm.lookupState(MessageAssemblerState.NAME), "no script, no assembler");

        // the config carries no endpoint — only an optional default-port hint the caller may use
        assertEquals(465, ClientSMFactory.port(sm.getContext().getSettings(), -1));
        assertEquals(7, ClientSMFactory.timeoutSec(sm.getContext().getSettings()));
    }

    @Test
    public void sshShapeIsCatalogSugar() {
        // protocol "ssh" is factory sugar: delimited assembler + validating controller
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"ssh-fingerprint\", \"port\": 22,"
                        + " \"protocol\": \"ssh\", \"ssh\": {\"banner_prefix\": \"SSH-2.0-\", \"banner_contains\": \"OpenSSH\"} }");

        assertNotNull(sm.lookupState(MessageAssemblerState.NAME), "SSH sugar must compose the assembler");
        assertNotNull(sm.lookupState(ProtocolControllerState.NAME), "SSH sugar must compose the controller");
        assertNotNull(sm.lookupState(ProtocolTypeValidatorState.NAME), "controller implies validator");
        assertNull(sm.lookupState(SSLClientState.NAME), "no TLS block, no ssl state");
        assertEquals(5, ClientSMFactory.timeoutSec(sm.getContext().getSettings()), "default timeout");
        assertEquals(22, ClientSMFactory.port(sm.getContext().getSettings(), -1));
    }

    @Test
    public void starttlsReadyShape() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"smtp-starttls\", \"port\": 587,"
                        + " \"protocol\": \"plain\", \"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false} }");

        assertNotNull(sm.lookupState(SSLClientState.NAME),
                "on_demand TLS must compose the ssl state, upgrade-capable");
        assertNotNull(sm.lookupState(SSLClientHandshakeState.NAME));
        assertNull(sm.lookupState(MessageAssemblerState.NAME));
    }

    @Test
    public void explicitStatesShape() {
        // the v2 shape: the JSON composes the machine state by state, each config block seeded
        // into that state's properties bag (META-SM-PROTO-DESIGN.md §12)
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"dns-probe\", \"transport\": \"udp\", \"port\": 53, \"timeout_sec\": 3,"
                        + " \"states\": ["
                        + "   { \"state\": \"assembler\", \"config\": { \"boundary\": \"datagram\", \"max_message\": 65536 } },"
                        + "   { \"state\": \"controller\", \"config\": { \"exchange\": ["
                        + "       {\"send\":     \"hex:1234 0100 0001 0000 0000 0000 07 6578616d706c65 03 636f6d 00 0001 0001\"},"
                        + "       {\"validate\": { \"contains\": \"hex:1234\", \"report\": \"dns\" } }"
                        + "   ] } }"
                        + " ] }");

        assertNotNull(sm.lookupState(MessageAssemblerState.NAME));
        assertNotNull(sm.lookupState(ProtocolControllerState.NAME));
        assertNotNull(sm.lookupState(ResponseControllerState.NAME), "controller implies responder");
        assertNotNull(sm.lookupState(ProtocolTypeValidatorState.NAME), "controller implies validator");
        assertEquals(ClientSessionContext.Transport.UDP, sm.getContext().getTransport());
        assertEquals(53, ClientSMFactory.port(sm.getContext().getSettings(), -1));
        // unknown catalog state fails fast
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"states\": [ { \"state\": \"transmogrifier\", \"config\": {} } ] }"));
    }

    @Test
    public void noEndpointInConfig() {
        // a fully endpoint-free config still builds — the caller provides the InetSocketAddress
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"no-endpoint\", \"protocol\": \"tls\", \"tls\": {\"cert_validation\": false} }");
        assertNotNull(sm.lookupState(SSLClientState.NAME));
        // no port hint -> caller's fallback is returned
        assertEquals(443, ClientSMFactory.port(sm.getContext().getSettings(), 443));
    }

    @Test
    public void settingsBagIsTheParsedConfig() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"bag-check\", \"protocol\": \"plain\", \"custom_knob\": \"42\" }");

        assertEquals("42", sm.getContext().getSettings().getValue("custom_knob"),
                "states must be able to read their knobs from the settings bag");
    }

    @Test
    public void failFastOnMalformedConfig() {
        // unknown protocol
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"gopher\" }"));
        // unknown tls mode
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"tls\", \"tls\": {\"mode\": \"maybe\"} }"));
    }

    @Test
    public void failFastOnContradictoryOrUnsafeCombinations() {
        // protocol 'tls' + on_demand would publish READY on a plaintext link with no negotiator
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"tls\", \"tls\": {\"mode\": \"on_demand\"} }"));
        // ssh sugar + exchange: two scripts would drive one controller
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"ssh\", \"exchange\": [ {\"expect\": \"txt:x\"} ] }"));
        // start_tls step with no tls block: START_TLS would have no consumer (silent hang)
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"plain\", "
                        + "\"exchange\": [ {\"start_tls\": true} ] }"));
        // start_tls step with an immediate ssl state: session already secure, SECURE never refires
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"tls\", \"tls\": {\"mode\": \"immediate\", \"cert_validation\": false}, "
                        + "\"exchange\": [ {\"start_tls\": true} ] }"));
        // malformed data literal fails at build, never mid-session
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"plain\", "
                        + "\"exchange\": [ {\"expect\": \"hex:XYZ\"} ] }"));
        // unknown exchange op fails at build
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"plain\", "
                        + "\"exchange\": [ {\"transmogrify\": \"txt:x\"} ] }"));
    }
}
