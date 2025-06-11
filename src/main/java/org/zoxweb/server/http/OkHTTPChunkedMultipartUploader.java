package org.zoxweb.server.http;

import okhttp3.*;
import okio.BufferedSink;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.Const;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class OkHTTPChunkedMultipartUploader {

    public static void uploadFile(String url, String username, String password, String filepath, String remoteLocation)
            throws IOException
    {
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
            System.out.println("Response: " + response.code());
            System.out.println(response.body() != null ? response.body().string() : "No response body");
            System.out.println("It took " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: java ChunkedMultipartUploader <url> <username> <password> <filepath>");
            System.exit(1);
        }

        int index = 0;
        String url = args[index++];
        String username = args[index++];
        String password = args[index++];
        String filepath = args[index++];
        String remoteLocation = index < args.length ? args[index++] : null;

        uploadFile(url, username, password, filepath, remoteLocation);
    }
}