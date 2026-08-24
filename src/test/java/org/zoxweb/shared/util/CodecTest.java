package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPNVGMBiEncoder;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.filters.FilterType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CodecTest
{

    @Test
    public void dataEncoder()
    {
        // StringLower / StringUpper: locale string case, null-tolerant
        assertEquals("hello", DataEncoder.StringLower.encode("HeLLo"));
        assertEquals("HELLO", DataEncoder.StringUpper.encode("HeLLo"));
        assertNull(DataEncoder.StringLower.encode(null));
        assertNull(DataEncoder.StringUpper.encode(null));

        // LowerAscii: folds 'A'..'Z' only — digits, symbols, control and high bytes untouched
        byte[] input = {'A', 'Z', 'a', 'z', '0', '9', '-', ':', 0x0d, (byte) 0xC3, (byte) 0x89};
        byte[] folded = DataEncoder.LowerAscii.encode(input);
        assertArrayEquals(new byte[]{'a', 'z', 'a', 'z', '0', '9', '-', ':', 0x0d, (byte) 0xC3, (byte) 0x89}, folded);
        // the input array is never modified; a non-empty input folds into a fresh copy
        assertEquals('A', input[0]);
        assertNotSame(input, folded);
        assertArrayEquals("server: nginx".getBytes(StandardCharsets.UTF_8),
                DataEncoder.LowerAscii.encode("Server: NGINX".getBytes(StandardCharsets.UTF_8)));
        // null-tolerant like its string siblings; empty passes through
        assertNull(DataEncoder.LowerAscii.encode(null));
        assertEquals(0, DataEncoder.LowerAscii.encode(new byte[0]).length);
    }

    @Test
    public void dataDecoder()
    {
        // AsStringOrNull: a String passes through, anything else is null
        assertEquals("text", DataDecoder.AsStringOrNull.decode("text"));
        assertNull(DataDecoder.AsStringOrNull.decode(42));
        assertNull(DataDecoder.AsStringOrNull.decode(null));

        // StringToData: the txt / hex / base64 prefix contract
        assertArrayEquals("PING\r\n".getBytes(StandardCharsets.UTF_8), DataDecoder.StringToData.decode("txt:PING\r\n"));
        assertArrayEquals(new byte[]{0x0d, 0x0a}, DataDecoder.StringToData.decode("hex:0d 0a"));
        assertArrayEquals(new byte[]{0x0d, 0x0a}, DataDecoder.StringToData.decode("HEX:0D0A"), "prefix is case-insensitive");
        byte[] raw = {0, 1, 2, 'h', 'i'};
        assertArrayEquals(raw, DataDecoder.StringToData.decode(
                "base64:" + SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, raw)));
        // no recognized prefix: the whole string — colon included — is UTF-8 text
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), DataDecoder.StringToData.decode("hello"));
        assertArrayEquals("USER: bob".getBytes(StandardCharsets.UTF_8), DataDecoder.StringToData.decode("USER: bob"));
        // null/empty yield an empty array, never null; a malformed declared body is fatal
        assertEquals(0, DataDecoder.StringToData.decode(null).length);
        assertEquals(0, DataDecoder.StringToData.decode("").length);
        assertThrows(IllegalArgumentException.class, () -> DataDecoder.StringToData.decode("hex:XYZ"));
    }

    @Test
    public void json()
    {
        NVGenericMap nvgm = new NVGenericMap("parameters");
        nvgm.add("par1", "val1");
        nvgm.add("par2", "val2");
        nvgm.add("par3", "val3");
        nvgm.add("par4", "val4");

        nvgm.add(new NVPair("mailto", "batata@batata.com", FilterType.EMAIL));
        HTTPNVGMBiEncoder httpBiEncoder = new HTTPNVGMBiEncoder(nvgm, "mailto", "to_include");

        String json = GSONUtil.toJSONDefault(httpBiEncoder);
        System.out.println(json);
        httpBiEncoder = GSONUtil.fromJSONDefault(json, HTTPNVGMBiEncoder.class);
        String json1 = GSONUtil.toJSONDefault(httpBiEncoder);

        assert (json1.equals(json));
        System.out.println(GSONUtil.toJSONDefault(httpBiEncoder));

        Map<String, String> map = new HashMap<>();
        map.put("par1", "destination_par1");
        map.put("par3", "destination_par30");

        httpBiEncoder = new HTTPNVGMBiEncoder(nvgm,  map, false);
        System.out.println(GSONUtil.toJSONDefault(httpBiEncoder));
    }
}
