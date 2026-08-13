package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;

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
    public void binDecodesBase64() {
        byte[] raw = {0, 1, 2, 'h', 'e', 'l', 'l', 'o'};
        String b64 = org.zoxweb.shared.util.SharedBase64.encodeAsString(
                org.zoxweb.shared.util.SharedBase64.Base64Type.DEFAULT, raw);
        assertArrayEquals(raw, SMProtoUtil.STRING_TO_DATA.decode("bin:" + b64));
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
}
