package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.http.HTTPWSProto;

import java.security.NoSuchAlgorithmException;

public class WebSocketProtocolTest {



    @Test
    public void testSecWebSocketAccept() throws NoSuchAlgorithmException {
        String webSecKey ="dGhlIHNhbXBsZSBub25jZQ==";


        String secWebSocketAccept = HTTPUtil.toWebSocketAcceptValue(webSecKey);
        System.out.println(secWebSocketAccept);
        assert "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=".equals(secWebSocketAccept);

        System.out.println(HTTPUtil.toWebSocketAcceptValue("x3JJHMbDL1EzLkh9GBhXDw=="));
    }

    @Test
    public void hashSHA_1() throws NoSuchAlgorithmException {
        System.out.println(HashUtil.hashAsBase64("sha-1", "x3JJHMbDL1EzLkh9GBhXDw==" + HTTPWSProto.WEB_SOCKET_UUID));
    }
}