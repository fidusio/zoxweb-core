package org.zoxweb.server.net;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.net.ssl.SSLSessionCallback;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.util.InstanceCreator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class EchoProtocol
{
    private final static InstanceCreator<PlainSessionCallback> echoPIC = EchoSession::new;

    private final static InstanceCreator<SSLSessionCallback> echoSIC = SSLEchoSession::new;

    public static class EchoSession
        extends PlainSessionCallback
    {
        private UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
        @Override
        public void accept(ByteBuffer byteBuffer)
        {
            try
            {
               mergedCallback(byteBuffer, ubaos, get());
            } catch (IOException e) {
                e.printStackTrace();
                IOUtil.close(get());
            }
        }
    }

    public static class SSLEchoSession
            extends SSLSessionCallback
    {

        private UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
        @Override
        public void accept(ByteBuffer byteBuffer)
        {
            try
            {
                mergedCallback(byteBuffer, ubaos, get());
            } catch (IOException e) {
                e.printStackTrace();
                IOUtil.close(get());
            }
        }
    }

    private static void mergedCallback(ByteBuffer bb, UByteArrayOutputStream ubaos, OutputStream os)
            throws IOException
    {
        ByteBufferUtil.write(bb, ubaos, true);
        if (ubaos.byteAt(ubaos.size() - 1) == (byte)'\n')
        {
            os.write(ubaos.getInternalBuffer(), 0, ubaos.size());
            ubaos.reset();
        }
    }


    public static void main(String ...args)
    {
        try
        {

            int port = 1024;
            int backlog = 128;
            NIOSocket nioSocket = new NIOSocket(TaskUtil.getDefaultTaskProcessor());
            nioSocket.addSeverSocket(port, backlog, new NIOPlainSocketFactory(echoPIC));

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
