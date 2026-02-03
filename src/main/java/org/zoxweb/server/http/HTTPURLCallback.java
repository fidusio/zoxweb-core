package org.zoxweb.server.http;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPURLCallback extends TCPSessionCallback {

    public static final LogWrapper log = new LogWrapper(HTTPURLCallback.class);
    private HTTPMessageConfigInterface hmci;
    private HTTPRawMessage hrm = null;
    private transient AtomicLong ts = new AtomicLong(0);
    private transient ConsumerCallback<HTTPResponse> callback;

    public HTTPURLCallback(String url, ConsumerCallback<HTTPResponse> callback) throws IOException {
        this(url, HTTPMethod.GET, false, callback);
    }

    public HTTPURLCallback(String url, HTTPMethod httpMethod, boolean certValidationEnabled, ConsumerCallback<HTTPResponse> callback) throws IOException {
        this(HTTPMessageConfig.buildHMCI(url, httpMethod, certValidationEnabled), callback);
    }

    public HTTPURLCallback(HTTPMessageConfigInterface hmci, ConsumerCallback<HTTPResponse> callback)
            throws IOException {
        SUS.checkIfNulls("null HTTPMessageConfigInterface", hmci);
        this.callback = callback;
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

    public HTTPURLCallback updateTimeStamp() {
        return updateTimeStamp(System.currentTimeMillis());
    }

    public HTTPURLCallback updateTimeStamp(long timeStamp) {
        ts.set(timeStamp);
        return this;
    }

    public synchronized HTTPURLCallback setCallback(ConsumerCallback<HTTPResponse> callback) {
        this.callback = callback;
        return this;

    }

    public ConsumerCallback<HTTPResponse> getCallback() {
        return callback;
    }

    public long getDuration() {
        return System.currentTimeMillis() - ts.get();
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
                IOUtil.close(this);
                if (callback != null) {
                    callback.accept(new HTTPResponseData(hmci.getHTTPStatusCode().CODE, hmci.getHeaders(), hmci.getContent(), System.currentTimeMillis() - ts.get()).setCorrelationID(getID()));
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            IOUtil.close(this);
        }
    }


    @Override
    protected void connectedFinished() throws IOException {
        SocketChannel channel = getChannel();
        hrm = new HTTPRawMessage(true);
        HTTPRawFormatter hrf = new HTTPRawFormatter(hmci);
        getOutputStream().write(hrf.format(), true);
        if (log.isEnabled()) log.getLogger().info(getRemoteAddress() + " " + channel.isConnected());

    }

    @Override
    public void exception(Throwable e) {
        e.printStackTrace();
        if (callback != null) {
            callback.exception(e);
        }
    }
}
