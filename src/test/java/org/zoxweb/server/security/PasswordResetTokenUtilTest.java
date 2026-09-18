package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.security.PasswordResetRequest;
import org.zoxweb.shared.security.PasswordResetToken;
import org.zoxweb.shared.security.SecConst;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenUtilTest {

    @Test
    void newToken_isBase64Url_andUnique() {
        String a = PasswordResetTokenUtil.newToken();
        String b = PasswordResetTokenUtil.newToken();
        assertEquals(43, a.length(), a);
        assertTrue(a.matches("[A-Za-z0-9_-]+"), a);
        assertNotEquals(a, b);
    }

    @Test
    void hash_isDeterministic_andMatchesConstantTime() {
        String token = PasswordResetTokenUtil.newToken();
        String h = PasswordResetTokenUtil.hash(token);
        assertEquals(h, PasswordResetTokenUtil.hash(token));
        assertNotEquals(token, h);
        assertTrue(PasswordResetTokenUtil.matches(h, token));
        assertFalse(PasswordResetTokenUtil.matches(h, token + "x"));
        assertFalse(PasswordResetTokenUtil.matches(h, PasswordResetTokenUtil.newToken()));
        assertFalse(PasswordResetTokenUtil.matches(null, token));
        assertFalse(PasswordResetTokenUtil.matches(h, null));
        assertThrows(NullPointerException.class, () -> PasswordResetTokenUtil.hash(null));
    }

    @Test
    void token_isOutstanding_boundaries_andRoundTrip() {
        PasswordResetToken t = new PasswordResetToken();
        t.setPrincipalID("someone@example.com");
        t.setTokenHash(PasswordResetTokenUtil.hash("x"));
        t.setExpiryTS(1000);
        t.setStatus(SecConst.SecStatus.ACTIVE);
        t.setChannel(PasswordResetToken.Channel.EMAIL);
        assertTrue(t.isOutstanding(999));
        assertFalse(t.isOutstanding(1000), "expiry == now is expired");
        t.setStatus(SecConst.SecStatus.DEACTIVATED);
        assertFalse(t.isOutstanding(0));
        t.setStatus(SecConst.SecStatus.ACTIVE);
        assertEquals(0L, t.getConsumedTS());

        String json = GSONUtil.toJSONDefault(t);
        PasswordResetToken back = GSONUtil.fromJSONDefault(json, PasswordResetToken.class);
        assertEquals(t.getTokenHash(), back.getTokenHash());
        assertEquals(1000L, back.getExpiryTS());
        assertEquals(PasswordResetToken.Channel.EMAIL, back.getChannel());
        assertEquals(SecConst.SecStatus.ACTIVE, back.getStatus());
    }

    @Test
    void request_masksToken() {
        PasswordResetRequest r = new PasswordResetRequest("secret-token", "g", "p@example.com",
                new String[]{"p@example.com"}, 5, PasswordResetToken.Channel.EMAIL);
        assertFalse(r.toString().contains("secret-token"));
        assertEquals("secret-token", r.getToken());
        assertEquals(1, r.getDeliveryPrincipalIDs().length);
    }
}
