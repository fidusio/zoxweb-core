package org.zoxweb.server.net.ssl;


import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLChannelOutputStream extends BaseChannelOutputStream {
    private final SSLSessionConfig config;

    protected SSLChannelOutputStream(ProtocolHandler protocolHandler, SSLSessionConfig config, int outAppBufferSize)
    {
        super(protocolHandler, config.sslChannel, outAppBufferSize);
        this.config = config;
    }


    /**
     *
     * @param bb unencrypted to be encrypted and sent over the wire
     * @return the number of encrypted data sent
     * @throws IOException in case of error
     */
    public synchronized int write(ByteBuffer bb) throws IOException
    {
        int written = -1;
        if (config.getHandshakeStatus() == NOT_HANDSHAKING)
        {
            SSLEngineResult result = config.smartWrap(bb, config.outSSLNetData); // at handshake stage, data in appOut won't be
            if(log.isEnabled())
                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
            switch (result.getStatus())
            {
                case BUFFER_UNDERFLOW:
                case BUFFER_OVERFLOW:
                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
                            SharedUtil.toCanonicalID(',', config.outSSLNetData.capacity(),config.outSSLNetData.limit(),config.outSSLNetData.position()));
                case OK:
                    try
                    {
                        written = ByteBufferUtil.smartWrite(null, outChannel, config.outSSLNetData);
                        protocolHandler.updateUsage();
                    }
                    catch (IOException e)
                    {
                        //e.printStackTrace();
                        IOUtil.close(this);
                        throw e;
                    }
                    break;
                case CLOSED:
                   throw new IOException("Closed");
            }
        }
        else
        {
            throw new SSLException("handshaking state can't send data yet");
        }

        return written;
    }

    public void close() throws IOException
    {
        //log.getLogger().info("Calling close: " + RuntimeUtil.stackTrace());
        if(!isClosed.getAndSet(true))
        {
            if(log.isEnabled()) log.getLogger().info("Calling close" );
            IOUtil.close(config, protocolHandler);
            ByteBufferUtil.cache(outAppData);
        }
    }


}
