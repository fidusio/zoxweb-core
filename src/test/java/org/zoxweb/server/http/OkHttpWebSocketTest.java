package org.zoxweb.server.http;

import okhttp3.*;
import okio.ByteString;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.RateCounter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class OkHttpWebSocketTest {

    static AtomicInteger receiveCounter = new AtomicInteger(0);

    public static WebSocket creatWebSocket(OkHttpClient client, String url, String username, String password, int repeat) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        // Generate the Basic Authorization header value
        if (username != null && password != null) {
            requestBuilder.header("Authorization", Credentials.basic(username, password));
        }


        Request request = requestBuilder.build();

        RateCounter rc = new RateCounter();
        long ts = System.currentTimeMillis();
        AtomicInteger ai = new AtomicInteger();
        long timerAfterClose = 2;
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket opened: " + response);
                webSocket.send("Hello, WebSocket!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                receiveCounter.incrementAndGet();
                int count = ai.incrementAndGet();
//                    System.out.println(count + " " + text);
                if (count == repeat) {
                    long delta = System.currentTimeMillis() - ts;
                    rc.register(delta, ai.get());
                    System.out.println(text);
                    System.out.println(receiveCounter.get() + " " + ai.get() + "  " + Const.TimeInMillis.toString(delta) + " " + rc.rate(1000) + " msg/sec");
                    TaskUtil.defaultTaskScheduler().queue(Const.TimeInMillis.SECOND.mult(timerAfterClose), () ->
                    {
                        try {
                            webSocket.close(1000, "finished");
                            //TaskUtil.sleep(Const.TimeInMillis.SECOND.mult(timerAfterClose));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                //System.out.println("Received bytes: " + bytes.string(StandardCharsets.UTF_8));
                int count = ai.incrementAndGet();
                //System.out.println(count + " " + text);
                if (count == repeat) {
                    long delta = System.currentTimeMillis() - ts;
                    rc.register(delta, ai.get());
                    System.out.println(bytes.string(StandardCharsets.UTF_8));
                    System.out.println(ai.get() + "  " + Const.TimeInMillis.toString(delta) + " " + rc.rate(1000) + " msg/sec");
                    TaskUtil.defaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS * 5, () ->
                    {
                        try {
                            webSocket.close(1000, "finished");
                            TaskUtil.sleep(Const.TimeInMillis.SECOND.MILLIS * 5);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket is closing: " + code + " / " + reason);
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket closed: " + code + " / " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("WebSocket error: " + t);
            }
        };

        // Establish the WebSocket connection
        return client.newWebSocket(request, listener);
        //TaskUtil.sleep(Const.TimeInMillis.SECOND.MILLIS*5);
    }

    public static void main(String[] args) {

        try {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            params.hide("password");
            System.out.println(params);
            String url = params.stringValue("url", false);
            String username = params.stringValue("user", true);
            String password = params.stringValue("password", true);
            boolean binary = params.booleanValue("bin", true);
            boolean sslCheck = params.booleanValue("ssl-check", true);
            OkHttpClient client = OkHTTPCall.createOkHttpBuilder(null, true, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND, sslCheck, 10, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND).build();

            int repeat = params.intValue("repeat", 1000);
            int clientNum = params.intValue("client-num", 1);


            WebSocket[] wsArray = new WebSocket[clientNum];

            for (int i = 0; i < clientNum; i++) {
                wsArray[i] = creatWebSocket(client, url, username, password, repeat);
            }


            for (int i = 0; i < repeat; i++) {
                String message = (i + 1) + " hello ";
                //TaskUtil.defaultTaskScheduler().execute( ()->ws.send(message);
                for (int j = 0; j < wsArray.length; j++) {
                    if (binary)
                        wsArray[j].send(ByteString.encodeUtf8(message));
                    else
                        wsArray[j].send(message);
                }

            }

            TaskUtil.waitIfBusyThenClose(500, () -> (receiveCounter.get() >= wsArray.length * repeat));
            //TaskUtil.waitIfBusy();
            System.out.println("Done sending " + receiveCounter.get());
            System.exit(0);

            // The client will continue to run; you can shut it down when you're done:
            //client.dispatcher().executorService().shutdown();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("usage: url=https://domain.com/websocket [user=userName password=userPassword] [repeat=x] [bin=true] [client-num=y]");
        }
    }

}

