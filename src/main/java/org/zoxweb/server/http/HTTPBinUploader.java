package org.zoxweb.server.http;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPBinUploader {
    static AtomicLong counter = new AtomicLong(0);


    public static NVGenericMap uploadFile(String url, String username, String password, String filepath, boolean cmp)
            throws IOException {
        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist: " + filepath);
            System.exit(1);
        }
        System.out.println("Uploading file: " + file);

        if(!cmp)
            url = url + (url.endsWith("/") ? "" : "/") + file.getName();
        System.out.println("URL: " + url);

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, HTTPMethod.POST, false);
        hmci.setBasicAuthorization(username, password);
        FileInputStream fis = new FileInputStream(file);
        if(cmp) {

            hmci.setContentType(HTTPMediaType.MULTIPART_FORM_DATA);
            hmci.setBasicAuthorization(username, password);
            hmci.setTransferChunked(true);
            NamedValue<InputStream> nv = new  NamedValue<>("file", fis);
            nv.getProperties().build(HTTPConst.CNP.FILENAME, file.getName())
                    .build(new NVEnum(HTTPConst.CNP.MEDIA_TYPE, HTTPMediaType.APPLICATION_OCTET_STREAM));
            hmci.getParameters().add(nv);
        } else{
            hmci.setContentType(HTTPMediaType.APPLICATION_OCTET_STREAM);

            hmci.setContentAsIS(fis, false);
            hmci.setTransferChunked(true);
        }

        HTTPResponseData hrd = OkHTTPCall.send(hmci);

        return GSONUtil.fromJSONDefault(hrd.getData(), NVGenericMap.class);
    }

//    public static NVGenericMap uploadFile(String url, String username, String password, String filepath)
//            throws IOException {
//        OkHttpClient client = OkHTTPCall.createOkHttpBuilder(null, true, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND, false, 20, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_40_SECOND).build();
//
//        File file = new File(filepath);
//        if (!file.exists() || !file.isFile()) {
//            System.err.println("File does not exist: " + filepath);
//            System.exit(1);
//        }
//
//        url = url + (url.endsWith("/") ? "" : "/") + file.getName();
//
//        String userpass = username + ":" + password;
//        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userpass.getBytes(StandardCharsets.UTF_8));
//
//
//        RequestBody streamBody = new RequestBody() {
//            @Override
//            public MediaType contentType() {
//                return MediaType.parse("application/octet-stream");
//            }
//
//            @Override
//            public void writeTo(BufferedSink sink) throws IOException {
//                try (InputStream in = new FileInputStream(file)) {
//                    byte[] buffer = new byte[8196];
//                    int read;
//                    while ((read = in.read(buffer)) != -1) {
//                        sink.write(buffer, 0, read);
//                    }
//                }
//            }
//
//            @Override
//            public long contentLength() {
//                return -1; // Forces OkHttp to use chunked encoding
//            }
//        };
//
//
//        Request request = new Request.Builder()
//                .url(url)
//                .header("Authorization", basicAuth)
//                .header("Transfer-Encoding", "chunked")
//                .post(streamBody)
//                .build();
//        long ts = System.currentTimeMillis();
//        try (Response response = client.newCall(request).execute()) {
//            NVGenericMap resp = new NVGenericMap(filepath);
//            NVGenericMap remoteResp = GSONUtil.fromJSONDefault(response.body().string(), NVGenericMap.class);
//            remoteResp.setName("response");
//
//
//            resp.build(new NVInt("status", response.code()))
//                    .build("round-trip-duration", Const.TimeInMillis.toString(System.currentTimeMillis() - ts))
//                    .build(remoteResp);
//            return resp;
//
//        }
//    }


    public static void main(String[] args) {
        try {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            params.hide("password");
            String url = params.stringValue("url");
            String username = params.stringValue("user");
            String password = params.stringValue("password");
            String[] files = ParamUtil.parseWithSep(",", params.stringValue("files"));
            boolean cmp = params.booleanValue("cmp", true);

            System.out.println(params);


            for (String filepath : files) {
                System.out.println("Trying to upload: " + filepath + " to: " + url);
                try {
                    NVGenericMap resp = uploadFile(url, username, password, filepath, cmp);
                    System.out.println(GSONUtil.toJSONDefault(resp, true));
                } catch (Exception e) {
                    System.err.println(filepath + " failed to upload");
                    e.printStackTrace();
                }
            }


        } catch (Exception e) {

            e.printStackTrace();
            ServerUtil.exitWithError(-1, "Usage: java HTTPBinUploader url=https://domain/upload user=username password=password file=file1,file2,... [cmp=true/false(chunked multipart form)]");
        }
    }


}