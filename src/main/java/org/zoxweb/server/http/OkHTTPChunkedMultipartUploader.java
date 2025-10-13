package org.zoxweb.server.http;

import okhttp3.*;
import okio.BufferedSink;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class OkHTTPChunkedMultipartUploader {

    public static NVGenericMap uploadFile(String url, String username, String password, String filepath, String remoteLocation)
            throws IOException {
        OkHttpClient client = OkHTTPCall.createOkHttpBuilder(null, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND, false, 20, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_40_SECOND).build();

        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist: " + filepath);
            System.exit(1);
        }

        String userpass = username + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes(StandardCharsets.UTF_8));


        RequestBody streamBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[8196];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        sink.write(buffer, 0, read);
                    }
                }
            }

            @Override
            public long contentLength() {
                return -1; // Forces OkHttp to use chunked encoding
            }
        };


        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), streamBody);
//        if(remoteLocation != null)
//            multipartBuilder.addFormDataPart("Location", remoteLocation);

        RequestBody requestBody = multipartBuilder.build();


        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", basicAuth)
                .header("Transfer-Encoding", "chunked")
                .post(requestBody)
                .build();
        long ts = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            NVGenericMap resp = new NVGenericMap(filepath);
            NVGenericMap remoteResp = GSONUtil.fromJSONDefault(response.body().string(), NVGenericMap.class);
            remoteResp.setName("response");


            resp.build(new NVInt("status", response.code()))
                    .build("round-trip-duration", Const.TimeInMillis.toString(System.currentTimeMillis() - ts))
                    .build(remoteResp);
            return resp;

        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: java ChunkedMultipartUploader <url> <username> <password> <file1,file2,...>");
            System.exit(1);
        }

        int index = 0;
        String url = args[index++];
        String username = args[index++];
        String password = args[index++];
        String[] files = ParamUtil.parseWithSep(",", args[index++]);
        String remoteLocation = index < args.length ? args[index++] : null;

        for (String filepath : files) {
            System.out.println("Trying to upload: " + filepath + " to: " + url);
            try {
                NVGenericMap resp = uploadFile(url, username, password, filepath, remoteLocation);
                System.out.println(GSONUtil.toJSONDefault(resp, true));
//                System.out.println("" + resp.getValue("content"));
//                System.out.println("It took " + Const.TimeInMillis.toString(resp.getValue("duration")));
            } catch (Exception e) {
                System.err.println(filepath + " failed to upload");
                e.printStackTrace();
            }
        }
    }
}