package org.zoxweb.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPHeader;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.protocol.ProtoMarker;
import org.zoxweb.shared.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ParseHTTPRequestTest {
    public static UByteArrayOutputStream rawRequest;
    public static List<UByteArrayOutputStream> chunkedRequest = new ArrayList<>();

    @BeforeAll
    public static void loadData() throws IOException {
        rawRequest = IOUtil.inputStreamToByteArray(IOUtil.locateFile("multipart-raw-data-no-content-length.txt"), true);
        chunkedRequest.add(IOUtil.inputStreamToByteArray(IOUtil.locateFile("chunked_multipart_test2_request.bin"), true));
        chunkedRequest.add(IOUtil.inputStreamToByteArray(IOUtil.locateFile("chunked_multipart_test_request.bin"), true));
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

        int[] byteArrayLength = {
                1, 2, 3, 10, 15, 64, 1024
        };

        HTTPCodecs.log.setEnabled(false);

//        int[] byteArrayLength = {
//                2,
//        };

        for (UByteArrayOutputStream data : chunkedRequest) {

            for (int b = 0; b < byteArrayLength.length; b++) {


                HTTPRawMessage hrm = new HTTPRawMessage();
                InputStream is = new ByteArrayInputStream(data.getInternalBuffer(), 0, data.size());
                UByteArrayOutputStream file1Content = new UByteArrayOutputStream();
                UByteArrayOutputStream file2Content = new UByteArrayOutputStream();
                boolean lastChunkFile1 = false;
                boolean lastChunkFile2 = false;
                byte[] buffer = new byte[byteArrayLength[b]];
                System.out.println("Running test with byte buffer size " + buffer.length);
                System.out.println("----------------------------Start--------------------------------");
                int read;
                int counter = 0;
                int totalRead = 0;
                while ((read = is.read(buffer)) != -1) {
//                    System.out.println("bytes read: " + read);
                    hrm.getDataStream().write(buffer, 0, read);
                    HTTPMessageConfigInterface hmci = hrm.parse();
                    counter++;
//                    System.out.println(hrm.getDataStream());
//                    if(hrm.isEndOfChunkedContentReached())
//                    {
//                        System.out.println(hrm.getDataStream());
//                        assert hrm.getDataStream().size() > 0;
//                    }
                    totalRead += read;
                    if (hrm.areHeadersParsed()) {
//                    System.out.println("[" + counter + "]" + " read: " + totalRead);
//                    System.out.println(hrm.getHTTPMessageConfig().getParameters());
//                    System.out.println("incomplete Parameter: " + hrm.incompleteParam());
//                    System.out.println("is it chunked: " + hmci.isTransferChunked());
//                    System.out.println("++++++++++++++++++++++RAW DATA START +++++++++++++++++++++++");
//                    System.out.println(hrm.getDataStream());
//                    System.out.println("++++++++++++++++++++++RAW DATA END   +++++++++++++++++++++++");
//                    System.out.println("parameter size: " + hmci.getParameters().size());
//                    if(hmci.getParameters().size() > 0)
//                        System.out.println(hmci.getParameters());
                        NamedValue<UByteArrayInputStream> nvs1 = hmci.getParameters().getNV("file1");
                        NamedValue<UByteArrayInputStream> nvs2 = hmci.getParameters().getNV("file2");
                        if (!lastChunkFile1) {
                            lastChunkFile1 = processStreams(nvs1, file1Content);
                        }


                        if (!lastChunkFile2) {
                            lastChunkFile2 = processStreams(nvs2, file2Content);
                        }


//                System.out.println("------------------------------------------------------------");
                    }

                    if (hrm.isMessageComplete()) {
                        System.out.println("Message Complete " + "Transfer chunked: " + hrm.getHTTPMessageConfig().isTransferChunked());
                        System.out.println("Message Complete " + "is message complete: " + hrm.isMessageComplete());
                        System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getBoundary());
                        System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getHeaders());
                        System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getParameters());
//                System.out.println("=================================\n" +"Message Complete " +  hrm.getDataStream() + "=================================\n");
//
//                System.out.println("Full parameters\n" + hmci.getParameters());
                        NamedValue<InputStream> nvs1 = hmci.getParameters().getNV("file1");
                        NamedValue<InputStream> nvs2 = hmci.getParameters().getNV("file2");

                        print(nvs1, file1Content);
                        print(nvs2, file2Content);
                        System.out.println("\n\n\n");
                        //System.out.println(hrm.getDataStream().size() + " ******************************************* dataStream \n" + hrm.getDataStream() + "\n********************************************** dataStream\n");
                        break;
                    }

                }

                System.out.println("----------------------------END--------------------------------");
            }
        }

    }


    @Test
    public void parseChunkedRequestBigFile() throws IOException {
        // System.out.println(rawRequest);

        int[] byteArrayLength = {

                1, 2, 3, 10, 15, 64, 1024
        };

        HTTPCodecs.log.setEnabled(false);


        UByteArrayOutputStream data = IOUtil.inputStreamToByteArray(IOUtil.locateFile("chunked_AESCrypt_java_request.bin"), true);


        for (int b = 0; b < byteArrayLength.length; b++) {


            HTTPRawMessage hrm = new HTTPRawMessage();
            InputStream is = new ByteArrayInputStream(data.getInternalBuffer(), 0, data.size());
            UByteArrayOutputStream fileContent = new UByteArrayOutputStream();

            boolean lastChunkFile = false;

            byte[] buffer = new byte[byteArrayLength[b]];
            System.out.println("Running test with byte buffer size " + buffer.length);
            System.out.println("----------------------------Start--------------------------------");
            int read;
            int counter = 0;
            int totalRead = 0;
            while ((read = is.read(buffer)) != -1) {
//                    System.out.println("bytes read: " + read);
                hrm.getDataStream().write(buffer, 0, read);
                HTTPMessageConfigInterface hmci = hrm.parse();
                counter++;
//                    System.out.println(hrm.getDataStream());
//                    if(hrm.isEndOfChunkedContentReached())
//                    {
//                        System.out.println(hrm.getDataStream());
//                        assert hrm.getDataStream().size() > 0;
//                    }
                totalRead += read;
                if (hrm.areHeadersParsed()) {
//                    System.out.println("[" + counter + "]" + " read: " + totalRead);
//                    System.out.println(hrm.getHTTPMessageConfig().getParameters());
//                    System.out.println("incomplete Parameter: " + hrm.incompleteParam());
//                    System.out.println("is it chunked: " + hmci.isTransferChunked());
//                    System.out.println("++++++++++++++++++++++RAW DATA START +++++++++++++++++++++++");
//                    System.out.println(hrm.getDataStream());
//                    System.out.println("++++++++++++++++++++++RAW DATA END   +++++++++++++++++++++++");
//                    System.out.println("parameter size: " + hmci.getParameters().size());
//                    if(hmci.getParameters().size() > 0)
//                        System.out.println(hmci.getParameters());
                    NamedValue<UByteArrayInputStream> nvs = hmci.getParameters().getNV("file");

                    if (!lastChunkFile) {
                        lastChunkFile = processStreams(nvs, fileContent);
                    }


//                System.out.println("------------------------------------------------------------");
                }

                if (hrm.isMessageComplete()) {
                    System.out.println("Message Complete " + "Transfer chunked: " + hrm.getHTTPMessageConfig().isTransferChunked());
                    System.out.println("Message Complete " + "is message complete: " + hrm.isMessageComplete());
                    System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getBoundary());
                    System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getHeaders());
                    System.out.println("Message Complete " + hrm.getHTTPMessageConfig().getParameters());
//                System.out.println("=================================\n" +"Message Complete " +  hrm.getDataStream() + "=================================\n");
//
//                System.out.println("Full parameters\n" + hmci.getParameters());
                    NamedValue<InputStream> nvs = hmci.getParameters().getNV("file");


//                    print(nvs, fileContent);
                    System.out.println("total read: " + totalRead + " counter: " + counter + "\n\n\n");
                    //System.out.println(hrm.getDataStream().size() + " ******************************************* dataStream \n" + hrm.getDataStream() + "\n********************************************** dataStream\n");
                    break;
                }

            }

            System.out.println("----------------------------END--------------------------------");
        }


    }


    private static boolean processStreams(NamedValue<UByteArrayInputStream> nvs, UByteArrayOutputStream fileContent)
            throws IOException {

        if (nvs != null) {
//            HTTPCodecs.log.setEnabled(true);
            boolean lastChunk = nvs.getProperties().getValue(ProtoMarker.LAST_CHUNK);

            int dataToProcess = nvs.getValue().available();

            IOUtil.relayStreams(nvs.getValue(), fileContent, true);
            if (dataToProcess > 0)
                System.out.println("DataToProcess: " + dataToProcess + " fileContent size: " + fileContent.size());
//            HTTPCodecs.log.setEnabled(false);
            return lastChunk;
        }

        return false;

    }

    private static void print(NamedValue<InputStream> nvs) throws IOException {
        if (nvs != null) {
            System.out.println(nvs.getName() + " : file ******************************************");
            System.out.println(IOUtil.inputStreamToString(nvs.getValue(), true));
            System.out.println(nvs.getName() + " : end file ******************************************");
        }
    }

    private static void print(NamedValue<InputStream> nvs, UByteArrayOutputStream baos) throws IOException {
        if (nvs != null && baos != null) {
            System.out.println(baos.size() + " " + nvs.getName() + " : content ******************************************");
            System.out.println(baos);
            System.out.println(nvs.getName() + " : end content ******************************************");
        }
    }


    @Test
    public void parseHeaderTest() {
        String[] headers = {
                "Content-Type: text/html;charset=UTF-8; boundary=\"--exampleBoundary\"",
                "Content-Disposition: form-data; name=\"file\"; filename=\"example.pdf\"",
                "Accept: text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7;q=0.9",
                "Content-Type: multipart/form-data; boundary=bd1a40c9-9408-4b59-8d4b-f6693561887e",
                "Content-Type: application/json",
                "Location: file:///web/downloadable/",
                "Content-Length: " + Long.MAX_VALUE,
                "Attachment: attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline",
                "Authorization: Bearer jdlksjfgdksljgikjrtjtkrejtiohyu4o35hjhj5rk",
                "Connection: keep-alive, Upgrade"
        };

        for (String header : headers) {
            System.out.println(header);
            System.out.println("NEW ONE: " + HTTPHeaderParser.parseFullHeaderLine(header));
            System.out.println("-----------------------------------------------------------");
        }

    }


    @Test
    public void parseHeaderValue() {
        String[] headerValues = {"form-data; name=\"file\"; filename=\"example.pdf\"", "text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7", "timeout=5, max=10", "attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline", "text/html; q=0.8, application/xhtml+xml; q=0.9, image/webp; q=0.7, */*; q=0.5", "Bearer 439iu5krjtjritu34iu5ij43l5kjewlrjlkejtewtre", "no-cache, no-store, must-revalidate", "text/html; charset=UTF-8", "keep-alive, Upgrade"

        };
        RateCounter rc = new RateCounter();
        long ts = System.currentTimeMillis();
        for (String hv : headerValues) {

            System.out.println("=============================================================");
            System.out.println(hv);
            long delta = System.currentTimeMillis();
            NamedValue<?> header = HTTPHeaderParser.parseHeader("toto", hv);
            delta = System.currentTimeMillis() - delta;
            rc.register(delta);
            System.out.println(header);
            System.out.println("=============================================================\n");
        }

        System.out.println(rc + " all " + Const.TimeInMillis.toString(System.currentTimeMillis() - ts));

        System.out.println(GSONUtil.toJSONDefault(new NVGenericMap().build(HTTPHeaderParser.parseHeader(new NVPair("content-type", "attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline")))));
        System.out.println(GSONUtil.toJSONDefault(new NVGenericMap().build(HTTPHeaderParser.parseHeader(HTTPHeader.CONTENT_TYPE, "attachment; filename=\"file,: name.pdf\"; creation-date=\"Wed, 12 Feb 2020 16:00:00 GMT\", inline"))));
    }

}
