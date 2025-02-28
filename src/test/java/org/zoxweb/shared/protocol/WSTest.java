package org.zoxweb.shared.protocol;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.util.BytesArray;

import java.security.NoSuchAlgorithmException;

public class WSTest
{
    @Test
    public void opCodeTest()
    {
        int[] toTest = {0x1, 0x2, 0x8, 0x9, 0xA, 12};
        for(int oc: toTest)
        {
            System.out.println(HTTPWSProto.OpCode.decode(oc));
        }
    }


    @Test
    public void testSecWebSocketAccept() throws NoSuchAlgorithmException
    {
        String webSecKey ="dGhlIHNhbXBsZSBub25jZQ==";


        String secWebSocketAccept = HTTPUtil.toWebSocketAcceptValue(webSecKey);
        System.out.println(secWebSocketAccept);
        assert "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=".equals(secWebSocketAccept);

        System.out.println(HTTPUtil.toWebSocketAcceptValue("x3JJHMbDL1EzLkh9GBhXDw=="));
    }

    @Test
    public void hashSHA_1() throws NoSuchAlgorithmException
    {
        System.out.println(HashUtil.hashAsBase64("sha-1", "x3JJHMbDL1EzLkh9GBhXDw==" + HTTPWSProto.WEB_SOCKET_UUID));
    }


    @Test
    public void frameFormat()
    {
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.PING, new byte[]{2,3,5, 10}, "hello");
        System.out.println(baos.size());
        HTTPWSFrame frame = new HTTPWSFrame(baos, 0);
        System.out.println(frame.isFin() + ", " + frame.isMasked() + ", " + frame.opCode() + ", " + frame.dataLength());

        byte[] maskingKey = frame.maskingKey();
        if(maskingKey != null)
        {
            for(byte b: maskingKey)
            {
                System.out.print(b + ", ");
            }
            System.out.println();
        }

        for(HTTPWSProto.WSFrameField f : HTTPWSProto.WSFrameField.values())
        {
            System.out.println(f + " index " + frame.byteIndex(f));
        }
        BytesArray data = frame.data();
        System.out.println("Message: " + data.asString());
    }

    @Test
    public void frameFormatLong()
    {
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        int to = baos.size();
        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.PING, null, "{hell ni how zdravo Здравейте Привет مرحبا}");
        //int startIndex = baos.size();
        HTTPWSFrame frame = new HTTPWSFrame(baos, to);
        BytesArray data = frame.data();
        System.out.println("FrameSize: " + frame.frameSize() +  " Data: " + data.length  + " " + data.offset + " \"" + data.asString() + '\"');


        System.out.println(baos.toString(false));
        to = baos.shiftLeft(data.offset, to);
        System.out.println(baos.toString(false));

        System.out.println("startIndex: " + to  + " \"" + baos+ '\"');

        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.PING, null, "{hell ni how zdravo Здравейте Привет مرحبا}");
        frame = new HTTPWSFrame(baos, to);
        data = frame.data();
        to = baos.shiftLeft(data.offset, to);


        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.PONG, new byte[]{10,11,12, 13}, "{hello hello hello hello hello hello hello hello hello hello " +
                " xlogistx $#%$#^$^FGRFDTGREYTRYTytrjrtoirjeoiyurteoiwugroewiuyoirweyutiur5toiurewiotireowutiorewutoiruewoitujreweroiturewoiutorewiutewroi}");
        System.out.println(baos.size());
        frame = new HTTPWSFrame(baos, to);
        System.out.println(frame.isFin() + ", " + frame.isMasked() + ", " + frame.opCode() + ", " + frame.dataLength());



        for(HTTPWSProto.WSFrameField f : HTTPWSProto.WSFrameField.values())
        {
            System.out.println(f + " index " + frame.byteIndex(f));
        }
        data = frame.data();

        System.out.println("FrameSize: " + frame.frameSize() +  " Data: " + data.length  + " " + data.offset + " \"" + data.asString() + '\"');

        System.out.println("baos size: " + baos.size());
        to = baos.shiftLeft(frame.data().offset, to);
        assert data.isValid();
        System.out.println("to : " + to + "\n" +baos.toString());

        baos.toByteArrayInputStream();
        assert !data.isValid();
    }
}
