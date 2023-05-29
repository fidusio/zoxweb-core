package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketProtocol {

    static String webSocketTag =  "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Test
    public void testSecWebSocketAccept() throws NoSuchAlgorithmException {
        String webSecKey ="dGhlIHNhbXBsZSBub25jZQ==";
        MessageDigest sha1 = HashUtil.getMessageDigest("sha-1");
        byte[] expected = sha1.digest(SharedStringUtil.getBytes(webSecKey + webSocketTag));

        String secWebSocketAccept = SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, expected);
        System.out.println(secWebSocketAccept);
        assert "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=".equals(secWebSocketAccept);

        System.out.println(Base64.getMimeEncoder().encodeToString(expected));

        System.out.println(HTTPUtil.toWebSocketAccept("x3JJHMbDL1EzLkh9GBhXDw=="));


    }
}