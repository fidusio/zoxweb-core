package org.zoxweb.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class ParseHTTPRequestTest {
    public static UByteArrayOutputStream rawRequest;
    public static UByteArrayOutputStream chunkedRequest;

    @BeforeAll
    public static void loadData() throws IOException {
        rawRequest = IOUtil.inputStreamToByteArray(IOUtil.locateFile("multipart-raw-data-no-content-length.txt"), true);
        chunkedRequest = IOUtil.inputStreamToByteArray(IOUtil.locateFile("chunked-multipart-raw-full-request.bin"), true);
    }

    @Test
    public void parseMultipartRequest() {
        // System.out.println(rawRequest);

        HTTPRawMessage hrm = new HTTPRawMessage(rawRequest);
        hrm.parse();

        System.out.println("is message complete: " + hrm.isMessageComplete());
        System.out.println(hrm.getHTTPMessageConfig().getBoundary());
        System.out.println(hrm.getHTTPMessageConfig().getHeaders());
        System.out.println(hrm.getHTTPMessageConfig().getParameters());


    }


    @Test
    public void parseChunkedRequest() throws IOException {
        // System.out.println(rawRequest);


        HTTPRawMessage hrm = new HTTPRawMessage();
        InputStream is = chunkedRequest.toByteArrayInputStream();
        byte[] buffer = new byte[10];
        int read;
        int counter = 0;
        int totalRead = 0;
        while ((read = is.read(buffer)) != -1) {
            hrm.getDataStream().write(buffer, 0, read);
            hrm.parse();
            counter++;
            totalRead += read;
            if (counter % buffer.length == 0 || hrm.isMessageComplete()) {
                System.out.println("[" + counter + "]" + " read: " + totalRead);
                System.out.println(hrm.getHTTPMessageConfig().getParameters());
                System.out.println("--------------------------------------------------");
                //System.out.println("=================================\n" + hrm.getDataStream() + "=================================\n");
            }

            if (hrm.isMessageComplete()) {
                System.out.println("Transfer chunked: " + hrm.getHTTPMessageConfig().isTransferChunked());
                System.out.println("is message complete: " + hrm.isMessageComplete());
                System.out.println(hrm.getHTTPMessageConfig().getBoundary());
                System.out.println(hrm.getHTTPMessageConfig().getHeaders());
                System.out.println(hrm.getHTTPMessageConfig().getParameters());
                System.out.println("=================================\n" + hrm.getDataStream() + "=================================\n");
            }

        }


    }


    @Test
    public void parseHeaderTest() {
        String[] headers = {"Content-Type: text/html;charset=UTF-8; boundary=\"--exampleBoundary\", bata; bata-type=232",
                "Content-Disposition: form-data; name=\"file\"; filename=\"example.pdf\"",
                "Accept: text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7;q=0.9",
                "Content-Type: multipart/form-data; boundary=bd1a40c9-9408-4b59-8d4b-f6693561887e",
                "Content-Type: application/json",
                "Attachment: attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline",
                "Authorization: Bearer jdlksjfgdksljgikjrtjtkrejtiohyu4o35hjhj5rk",
                "Connection: keep-alive, Upgrade"
        };

        for (String header : headers) {
            System.out.println(header);
//            NamedValue<String> parsedHeader = HTTPHeaderParser.parseFullLineHeader(header);
//            System.out.println(parsedHeader);
            System.out.println("NEW ONE: " + HTTPHeaderParser.parseHTTPHeaderLine(header));
            System.out.println("-----------------------------------------------------------");

        }

    }


//    @Test
//    public void parseHeaderValue()
//    {
//        String[] headerValues = {
//                "form-data; name=\"file\"; filename=\"example.pdf\"",
//                "text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7",
//                "timeout=5, max=10",
//                "attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline",
//                "text/html; q=0.8, application/xhtml+xml; q=0.9, image/webp; q=0.7, */*; q=0.5",
//                "Bearer 439iu5krjtjritu34iu5ij43l5kjewlrjlkejtewtre",
//                "no-cache, no-store, must-revalidate",
//                "text/html; charset=UTF-8",
//                "keep-alive, Upgrade"
//
//        };
//        RateCounter rc = new RateCounter();
//        long ts = System.currentTimeMillis();
//        for(String hv: headerValues)
//        {
//
//            System.out.println("=============================================================");
//            System.out.println(hv);
//            long delta = System.currentTimeMillis();
//            NVGenericMap header = HTTPHeaderParser.parseHeader("toto", hv);
//            delta =  System.currentTimeMillis() - delta;
//            rc.register(delta);
//            System.out.println(header);
//            System.out.println("=============================================================\n");
//        }
//
//        System.out.println(rc  + " all " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));
//
//        System.out.println(GSONUtil.toJSONDefault(new NVGenericMap().build(HTTPHeaderParser.parseHeader(new NVPair("content-type","attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline")))));
//        System.out.println(GSONUtil.toJSONDefault(new NVGenericMap().build(HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE,"attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline"))));
//    }

//    @Test
//    public void parseFullHeaderTest()
//    {
//
//        String[] headers = {"Content-Type: text/html;charset=UTF-8; boundary=\"--exampleBoundary\", batata=232",
//                "Content-Disposition: form-data; name=\"file\"; filename=\"example.pdf\"",
//                "Accept: text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7;q=0.9",
//                "Content-Type: multipart/form-data; boundary=bd1a40c9-9408-4b59-8d4b-f6693561887e",
//                "Authorization: Bearer jdlksjfgdksljgikjrtjtkrejtiohyu4o35hjhj5rk;charset=UTF-8", // this is invalid
//                "Connection: keep-alive, Upgrade"
//        };
//
//        for (String header: headers)
//        {
//            System.out.println(header);
//            NVGenericMap parsedHeader = HTTPHeaderParser.parseHeader(header);
//            System.out.println(parsedHeader);
//            System.out.println("-----------------------------------------------------------");
//
//        }
//    }
}
