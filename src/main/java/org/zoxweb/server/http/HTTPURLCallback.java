package org.zoxweb.server.http;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class HTTPURLCallback extends TCPSessionCallback {

    public static final LogWrapper log = new LogWrapper(HTTPURLCallback.class);
    private HTTPMessageConfigInterface hmci;
    private HTTPRawMessage hrm = null;
    private AtomicLong ts = new AtomicLong(0);
    Consumer<HTTPResponse> callback;

    public HTTPURLCallback(String url) throws IOException {
        this(url, HTTPMethod.GET, false);
    }

    public HTTPURLCallback(String url, HTTPMethod httpMethod, boolean certValidationEnabled) throws IOException {
        this(HTTPMessageConfig.buildHMCI(url, httpMethod, certValidationEnabled));
    }

    public HTTPURLCallback(HTTPMessageConfigInterface hmci)
            throws IOException {
        //this.nioSocket = nioSocket;
        SUS.checkIfNulls("null HTTPMessageConfigInterface", hmci);
        this.hmci = hmci;
        try {
            init();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException(e);
        }
    }


    private void init() throws NoSuchAlgorithmException, KeyManagementException {
        hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
        URLInfo urlInfo = hmci.toURLInfo();
        if (hmci.isSSL()) {
            SSLContextInfo sslContextInfo = new SSLContextInfo(urlInfo.ipAddress, hmci.isSecureCheckEnabled());
            setSSLContextInfo(sslContextInfo);
            setRemoteAddress(sslContextInfo.getClientAddress());
        }
        if (hmci.getAuthorization() != null)
            hmci.getHeaders().add(hmci.getAuthorization().toHTTPHeader());
    }


    /**
     * The application specific data processor
     *
     * @param byteBuffer the input argument
     */
    @Override
    public void accept(ByteBuffer byteBuffer) {

        try {
            if (hrm.parseResponse(hmci.getURIScheme(), byteBuffer)) {
                HTTPMessageConfigInterface hmci = hrm.parse();
                hmci.setContent(hrm.getDataStream().toByteArray());
                close();
                if(callback != null)
                {
                    callback.accept(new HTTPResponseData(hmci.getHTTPStatusCode().CODE, hmci.getHeaders(), hmci.getContent(), System.currentTimeMillis() - ts.get()));
                }
//                System.out.println(hmci.getHTTPStatusCode());
//                System.out.println(GSONUtil.toJSONDefault(hmci.getHeaders(), true));
//                System.out.println(SharedStringUtil.toString(hmci.getContent()));

            }
        } catch (IOException e) {
            e.printStackTrace();
            IOUtil.close(this);
        }
    }


    public void send(NIOSocket nioSocket, Consumer<HTTPResponse> callback) throws IOException {
        ts.set(System.currentTimeMillis());
        this.callback = callback;
        nioSocket.addClientSocket(this);
    }

    @Override
    protected void connectedFinished() throws IOException {
        SocketChannel channel = getChannel();
        HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);
        getOutputStream().write(hrf.format(), true);
        if (log.isEnabled()) log.getLogger().info(getRemoteAddress() + " " + channel.isConnected());
        hrm = new HTTPRawMessage(true);

    }
}
