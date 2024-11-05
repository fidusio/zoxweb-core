package org.zoxweb.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.util.NamedValue;

import java.io.IOException;

public class ParseHTTPRequestTest
{
    public static UByteArrayOutputStream rawRequest;
    @BeforeAll
    public static void loadData() throws IOException
    {
        rawRequest = IOUtil.inputStreamToByteArray(IOUtil.locateFile("multipart-raw-data.txt"), true);
    }

    @Test
    public void parseMultipartRequest()
    {
       // System.out.println(rawRequest);

        HTTPRawMessage hrm = new HTTPRawMessage(rawRequest);
        hrm.parse(true);
        System.out.println("is message complete: " + hrm.isMessageComplete());
        System.out.println(hrm.getHTTPMessageConfig().getBoundary());
        System.out.println(hrm.getHTTPMessageConfig().getHeaders());
        System.out.println(hrm.getHTTPMessageConfig().getParameters());


    }




    @Test
    public void parseHeaderTest()
    {
        String[] headers = {"Content-Type: text/html;charset=UTF-8; boundary=\"--exampleBoundary\", batata=232",
                "Content-Disposition: form-data; name=\"file\"; filename=\"example.pdf\"",
                "Accept: text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7;q=0.9",
                "Content-Type: multipart/form-data; boundary=bd1a40c9-9408-4b59-8d4b-f6693561887e",
                "Authorization: Bearer jdlksjfgdksljgikjrtjtkrejtiohyu4o35hjhj5rk;charset=UTF-8"
            };

        for (String header: headers)
        {
            System.out.println(header);
            NamedValue<String> parsedHeader = HTTPHeaderParser.parseHeader(header);
            System.out.println(parsedHeader);
            System.out.println("-----------------------------------------------------------");

        }

    }
}
