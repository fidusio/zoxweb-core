package org.zoxweb.server.http;

import okhttp3.*;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChunkedMultipartUploader {

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: java ChunkedMultipartUploader <url> <username> <password> <filepath>");
            System.exit(1);
        }

        String url = args[0];
        String username = args[1];
        String password = args[2];
        String filepath = args[3];

        OkHttpClient client =  OkHTTPCall.createOkHttpBuilder(null, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND, false, 20, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_40_SECOND).build();

        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist: " + filepath);
            System.exit(1);
        }

        String userpass = username + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes(StandardCharsets.UTF_8));

        // Build multipart body (OkHttp will use chunked transfer if file is large)
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
//                .addFormDataPart("username", username)
//                .addFormDataPart("password", password)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")));

        RequestBody requestBody = multipartBuilder.build();


        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", basicAuth)
                .header("Transfer-Encoding", "chunked")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response: " + response.code());
            System.out.println(response.body() != null ? response.body().string() : "No response body");
        }
    }
}