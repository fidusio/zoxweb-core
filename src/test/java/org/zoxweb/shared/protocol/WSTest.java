package org.zoxweb.shared.protocol;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.http.HTTPWSFrame;
import org.zoxweb.shared.http.HTTPWSProto;
import org.zoxweb.shared.io.BytesArray;

import java.io.IOException;
import java.io.InputStream;
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
        HTTPWSFrame frame = HTTPWSFrame.parse(baos, 0);
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
        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{hello ni how zdravo Здравейте Привет مرحبا}");
        //int startIndex = baos.size();
        HTTPWSFrame frame = HTTPWSFrame.parse(baos, to);
        BytesArray data = frame.data();
        System.out.println("FrameSize: " + frame.frameSize() +  " Data: " + data.length  + " " + data.offset + " \"" + data.asString() + '\"');


        System.out.println(baos.toString(false));
        to = baos.shiftLeft(data.offset, to);
        System.out.println(baos.toString(false));

        System.out.println("startIndex: " + to  + " \"" + baos+ '\"');

        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{hello ni how zdravo Здравейте Привет مرحبا}");
        frame = HTTPWSFrame.parse(baos, to);
        data = frame.data();
        to = baos.shiftLeft(data.offset, to);


        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, new byte[]{10,11,12, 13}, "{hello hello hello hello hello hello hello hello hello hello " +
                " xlogistx $#%$#^$^FGRFDTGREYTRYTytrjrtoirjeoiyurteoiwugroewiuyoirweyutiur5toiurewiotireowutiorewutoiruewoitujreweroiturewoiutorewiutewroi}");
        System.out.println(baos.size());
        frame = HTTPWSFrame.parse(baos, to);
        System.out.println(frame.isFin() + ", " + frame.isMasked() + ", " + frame.opCode() + ", " + frame.dataLength());



        for(HTTPWSProto.WSFrameField f : HTTPWSProto.WSFrameField.values())
        {
            System.out.println(f + " index " + frame.byteIndex(f));
        }
        data = frame.data();

        System.out.println("FrameSize: " + frame.frameSize() +  " Data: " + data.length  + " " + data.offset + " \"" + data.asString() + '\"');

        System.out.println("baos size: " + baos.size());
        to = baos.shiftLeft(frame.data().offset, to);
        assert !data.isValid();
        System.out.println("to : " + to + "\n" +baos.toString());

        baos.toByteArrayInputStream();
    }


    @Test
    public void parseMultiFrames()
    {
        UByteArrayOutputStream baos = new UByteArrayOutputStream();

        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{1-hello ni how zdravo Здравейте Привет مرحبا}");

        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{2-hello ni how zdravo Здравейте Привет مرحبا}");
        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{3-hello ni how zdravo Здравейте Привет مرحبا}");
        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, null, "{4-hello ni how zdravo Здравейте Привет مرحبا}");

        HTTPWSFrame frame = null;
        int pos = 0;
        while((frame = HTTPWSFrame.parse(baos, pos)) != null)
        {
            System.out.println(frame);
            System.out.println(frame.data().asString());
            pos += frame.frameSize();
        }

        baos.reset();
        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{1-hello ni how zdravo Здравейте Привет مرحبا}");

        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{2-2-hello ni how zdravo Здравейте Привет مرحبا}");
        HTTPWSProto.formatFrame(baos, false, HTTPWSProto.OpCode.TEXT, null, "{3-3-3-hello ni how zdravo Здравейте Привет مرحبا}");
        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, null, "{4-4-4-4-hello ni how zdravo Здравейте Привет مرحبا}");

        while((frame = HTTPWSFrame.parse(baos, 0)) != null)
        {
            System.out.println("With Data shiftLeft " + frame);
            System.out.println(frame.data().asString());
            baos.shiftLeft(frame.frameSize(), 0);
            assert !frame.data().isValid();
        }

    }


    @Test
    public void parsePartialFrames() throws IOException {
        UByteArrayOutputStream baos = new UByteArrayOutputStream();

        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, null, "{1-hello ni how zdravo Здравейте Привет مرحبا}");

        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, null, "{2-hello ni how zdravo Здравейте Привет مرحبا}");
        HTTPWSProto.formatFrame(baos, true, HTTPWSProto.OpCode.TEXT, null, "{3-hello ni how zdravo Здравейте Привет مرحبا}");
        UByteArrayOutputStream baosPartial = new UByteArrayOutputStream();
        HTTPWSProto.formatFrame(baosPartial, true, HTTPWSProto.OpCode.TEXT, null, "{4-hello ni how zdravo Здравейте Привет مرحبا}");

        InputStream is = baosPartial.toByteArrayInputStream();
        int toCopy = is.available();
        System.out.println(is.available());
        for(int i = 0; i < toCopy -1; i++)
        {
            baos.write(is.read());
        }

        HTTPWSFrame frame = null;
        int pos = 0;
        while((frame = HTTPWSFrame.parse(baos, pos)) != null)
        {
            System.out.println(frame);
            System.out.println(frame.data().asString());
            pos += frame.frameSize();

        }
        baos.shiftLeft(pos, 0);
        baos.write(is.read());
        frame = HTTPWSFrame.parse(baos, 0);
        System.out.println(frame);
        System.out.println(frame.data().asString());
        assert frame.frameSize() == baos.size();

    }
}
