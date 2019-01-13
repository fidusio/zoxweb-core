package org.zoxweb.server.http;


import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.zoxweb.server.io.IOUtil;

import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.util.DataHandler;
import org.zoxweb.shared.util.SharedStringUtil;



@ClientEndpoint
public class WebSocketClient
    implements Closeable
{
    private  Session userSession = null;
    private DataHandler<WebSocketClient, String> dataHandler = null;
    private AutoCloseable closeHandler;
    private AtomicLong requestCounter= new AtomicLong(0);
    private AtomicLong responseCounter= new AtomicLong(0);
    private final boolean statEnabled;

    
    public WebSocketClient(HTTPMessageConfig hmc, DataHandler<WebSocketClient, String> dataHandler, AutoCloseable closeHandler, boolean statEnabled)
        throws DeploymentException, IOException, URISyntaxException
    {
      
      this(new URI(SharedStringUtil.concat(hmc.getURL(), hmc.getURI(), "/")), dataHandler, closeHandler, statEnabled); 
    }
    

    public WebSocketClient(URI endpointURI, DataHandler<WebSocketClient, String> dataHandler, AutoCloseable closeHandler, boolean statEnabled)
        throws DeploymentException, IOException
    {    
      this.closeHandler  = closeHandler;
      setDataHandler(dataHandler);
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      container.connectToServer(this, endpointURI);
      this.statEnabled = statEnabled;
       
    }

    /**
     * Callback hook for Connection open events.
     * 
     * @param userSession
     *            the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     * 
     * @param userSession
     *            the userSession which is getting closed.
     * @param reason
     *            the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason)
    {
        IOUtil.close(closeHandler);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a
     * client send a message.
     * 
     * @param message
     *            The text message
     */
    @OnMessage
    public void onMessage(String message) {
        //respCounter.incrementAndGet();
        //respCounter++;
      if (statEnabled)
      {
          responseCounter.incrementAndGet();
      }
        if (dataHandler != null)
          dataHandler.handleData(this, message);
    }

    /**
     * Return the request count if stat is enabled
     * @return
     */
    public long requestCount()
    {
      return requestCounter.get();
    }
    
    
    public boolean isStartEnabled()
    {
      return statEnabled;
    }
    /**
     * Return the response count if stat is enabled
     * @return
     */
    public long responseCount()
    {
      return responseCounter.get();
    }
    
    /**
     * set the data handler
     * @param dh
     */
    public void setDataHandler(DataHandler<WebSocketClient, String> dh) {
        this.dataHandler = dh;
    }


    /**
     * Send a message
     * @param message
     */
    public void sendMessage(String message) throws IOException
    {
        sendMessage(message, false);
    }

    public void sendMessage(String message, boolean async) throws IOException {
        if(userSession.isOpen()) {
            if (async)
                userSession.getAsyncRemote().sendText(message);
            else
                userSession.getBasicRemote().sendText(message);
            if (statEnabled) {
                requestCounter.incrementAndGet();
            }
        }
        else
        {
            throw new IOException("WebSocket closed");
        }
    }

    public void close() throws IOException {

        IOUtil.close(userSession);
        IOUtil.close(closeHandler);
    }

}