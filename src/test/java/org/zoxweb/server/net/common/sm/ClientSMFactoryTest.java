package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Declarative-config factory: the three schema shapes build the right phase sets, the config
 * carries no endpoint (only an optional default-port hint + timeout), and contradictory or
 * unsafe configs fail fast.
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
        assertNotNull(sm.lookupState(SSLClientHandshakeState.NAME), "TLS protocol must register handshake state");
        assertNotNull(sm.lookupState(SSLClientDataState.NAME));
        assertNull(sm.lookupState(SSHBannerPhase.NAME));

        // the config carries no endpoint — only an optional default-port hint the caller may use
        assertEquals(465, ClientSMFactory.port(sm.getContext().getSettings(), -1));
        assertEquals(7, ClientSMFactory.timeoutSec(sm.getContext().getSettings()));
    }

    @Test
    public void sshShape() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"ssh-fingerprint\", \"port\": 22,"
                        + " \"protocol\": \"ssh\", \"ssh\": {\"banner_prefix\": \"SSH-2.0-\", \"banner_contains\": \"OpenSSH\"} }");

        assertNotNull(sm.lookupState(SSHBannerPhase.NAME), "SSH protocol must register the banner state");
        assertNull(sm.lookupState(SSLClientHandshakeState.NAME), "no TLS block, no SSL states");
        assertEquals(5, ClientSMFactory.timeoutSec(sm.getContext().getSettings()), "default timeout");
        assertEquals(22, ClientSMFactory.port(sm.getContext().getSettings(), -1));
    }

    @Test
    public void starttlsReadyShape() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"smtp-starttls\", \"port\": 587,"
                        + " \"protocol\": \"plain\", \"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false} }");

        assertNotNull(sm.lookupState(SSLClientHandshakeState.NAME),
                "on_demand TLS must register the SSL states, upgrade-capable");
        assertNull(sm.lookupState(SSHBannerPhase.NAME));
    }

    @Test
    public void noEndpointInConfig() {
        // a fully endpoint-free config still builds — the caller provides the InetSocketAddress
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"no-endpoint\", \"protocol\": \"tls\", \"tls\": {\"cert_validation\": false} }");
        assertNotNull(sm.lookupState(SSLClientHandshakeState.NAME));
        // no port hint -> caller's fallback is returned
        assertEquals(443, ClientSMFactory.port(sm.getContext().getSettings(), 443));
    }

    @Test
    public void settingsBagIsTheParsedConfig() {
        ClientConSM sm = ClientSMFactory.fromJSON(
                "{ \"name\": \"bag-check\", \"protocol\": \"plain\", \"custom_knob\": \"42\" }");

        assertEquals("42", sm.getContext().getSettings().getValue("custom_knob"),
                "phases must be able to read their knobs from the settings bag");
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
        // ssh + exchange: two active IN_DATA owners (banner phase and exchange driver)
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"ssh\", \"exchange\": [ {\"expect\": \"txt:x\"} ] }"));
        // start_tls step with no tls block: START_TLS would have no consumer (silent hang)
        assertThrows(IllegalArgumentException.class, () -> ClientSMFactory.fromJSON(
                "{ \"name\": \"x\", \"protocol\": \"plain\", "
                        + "\"exchange\": [ {\"start_tls\": true} ] }"));
        // start_tls step with an immediate tls phase: session already secure, SECURE never refires
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
