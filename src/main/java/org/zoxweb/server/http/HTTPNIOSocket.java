package org.zoxweb.server.http;

import org.zoxweb.server.net.NIOSocket;

import java.io.IOException;

public class HTTPNIOSocket
{
    private final NIOSocket nioSocket;
    public HTTPNIOSocket(NIOSocket nioSocket) {
        this.nioSocket = nioSocket;
    }


    public void send(HTTPURLCallback huc) throws IOException {
        huc.updateTimeStamp();
        nioSocket.addClientSocket(huc);
    }

    public NIOSocket getNIOSocket() {
        return nioSocket;
    }
}
