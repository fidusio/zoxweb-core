package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.NVGenericMap;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The exchange data encoding: txt / hex / bin prefixes, the bare-string fallback, and the byte
 * substring search used by expect matching.
 */
public class ProtoDataTest {

    @Test
    public void txtIsUtf8Verbatim() {
        assertArrayEquals("PING\r\n".getBytes(StandardCharsets.UTF_8), SMProtoUtil.STRING_TO_DATA.decode("txt:PING\r\n"));
    }

    @Test
    public void hexDecodesIgnoringWhitespace() {
        assertArrayEquals(new byte[]{0x0d, 0x0a}, SMProtoUtil.STRING_TO_DATA.decode("hex:0d0a"));
        assertArrayEquals(new byte[]{0x0d, 0x0a}, SMProtoUtil.STRING_TO_DATA.decode("hex:0d 0a"));
    }

    @Test
    public void base64DecodesBase64() {
        byte[] raw = {0, 1, 2, 'h', 'e', 'l', 'l', 'o'};
        String b64 = org.zoxweb.shared.util.SharedBase64.encodeAsString(
                org.zoxweb.shared.util.SharedBase64.Base64Type.DEFAULT, raw);
        assertArrayEquals(raw, SMProtoUtil.STRING_TO_DATA.decode("base64:" + b64));
    }

    @Test
    public void noPrefixFallsBackToText() {
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), SMProtoUtil.STRING_TO_DATA.decode("hello"));
    }

    @Test
    public void emptyOrNullYieldsEmpty() {
        assertEquals(0, SMProtoUtil.STRING_TO_DATA.decode(null).length);
        assertEquals(0, SMProtoUtil.STRING_TO_DATA.decode("").length);
    }

    @Test
    public void indexOfFindsSubsequence() {
        byte[] hay = "220-first\r\n250 ok\r\n".getBytes(StandardCharsets.UTF_8);
        assertEquals(11, DataExchangePhase.indexOf(hay, "250 ".getBytes(StandardCharsets.UTF_8)));
        assertEquals(-1, DataExchangePhase.indexOf(hay, "999".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, DataExchangePhase.indexOf(hay, new byte[0]));
    }

    @Test
    public void hasVarsDetectsPlaceholders() {
        assertTrue(SMProtoUtil.hasVars("txt:EHLO ${helo}\r\n"));
        assertFalse(SMProtoUtil.hasVars("txt:EHLO static.local\r\n"));
        assertFalse(SMProtoUtil.hasVars(null));
    }

    @Test
    public void decodeResolvesVarsInBodyOnly() {
        NVGenericMap vars = new NVGenericMap();
        vars.build("helo", "probe.example");
        assertArrayEquals("EHLO probe.example\r\n".getBytes(StandardCharsets.UTF_8),
                SMProtoUtil.STRING_VARS_TO_DATA.decode("txt:EHLO ${helo}\r\n", vars));
        // a var value containing a colon must not be mistaken for the encoding prefix
        vars.build("addr", "a:b");
        assertArrayEquals("x a:b".getBytes(StandardCharsets.UTF_8),
                SMProtoUtil.STRING_VARS_TO_DATA.decode("txt:x ${addr}", vars));
    }

    @Test
    public void prefixLessLiteralNeverReinterpretsResolvedValueAsEncoding() {
        // prefix-less literal: the resolved value is UTF-8 text verbatim, even when the injected
        // value is shaped like an encoding prefix — data, not a directive
        NVGenericMap vars = new NVGenericMap();
        vars.build("motd", "base64:SGVsbG8=");
        assertArrayEquals("base64:SGVsbG8=".getBytes(StandardCharsets.UTF_8),
                SMProtoUtil.STRING_VARS_TO_DATA.decode("${motd}", vars));
        // a colon-bearing value that would be a malformed hex body must not throw either
        vars.build("note", "hex: call me at 5");
        assertArrayEquals("hex: call me at 5".getBytes(StandardCharsets.UTF_8),
                SMProtoUtil.STRING_VARS_TO_DATA.decode("${note}", vars));
    }

    @Test
    public void substituteEncoderResolvesTemplate() {
        NVGenericMap vars = new NVGenericMap();
        vars.build("helo", "probe.example");
        assertEquals("EHLO probe.example", SMProtoUtil.STRING_VARS_TO_STRING.encode("EHLO ${helo}", vars));
        assertEquals("no placeholders", SMProtoUtil.STRING_VARS_TO_STRING.encode("no placeholders", null));
    }

    @Test
    public void unresolvedVarIsFatal() {
        NVGenericMap empty = new NVGenericMap();
        assertThrows(IllegalArgumentException.class, () -> SMProtoUtil.STRING_VARS_TO_DATA.decode("txt:EHLO ${missing}\r\n", empty));
        assertThrows(IllegalArgumentException.class, () -> SMProtoUtil.STRING_VARS_TO_DATA.decode("txt:${x}", null));
        assertThrows(IllegalArgumentException.class, () -> SMProtoUtil.STRING_VARS_TO_STRING.encode("${x}", empty));
    }

    @Test
    public void staticLiteralsUnaffectedBySubstitution() {
        assertArrayEquals("EHLO static.local".getBytes(StandardCharsets.UTF_8),
                SMProtoUtil.STRING_VARS_TO_DATA.decode("txt:EHLO static.local", null));
    }
}
