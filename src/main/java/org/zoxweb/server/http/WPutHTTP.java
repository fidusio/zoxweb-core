package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WPutHTTP {
    private static final String dummy = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static class FileToData
    {

        private String filename;
        private InputStream is;
        private HTTPMediaType mediaType;
        private final long length;

        public FileToData(String filename) throws IOException {
           File file = new File(filename);
           if (file.exists())
           {
               filename = file.getName();
               length = file.length();
               is = new FileInputStream(file);
               mediaType = HTTPMediaType.lookupByExtension(filename);
               return;
           }


           String[] tokens = filename.split(":");
           if (tokens.length != 2)
               throw  new IllegalArgumentException("Invalid dummy filename : " + filename);

           this.filename = tokens[0];
            mediaType = HTTPMediaType.lookupByExtension(this.filename);
           length = Const.SizeInBytes.parse(tokens[1]);
            UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
            int len = dummy.length();
            while (ubaos.size() < length)
            {
                ubaos.write(dummy.charAt(ubaos.size()%len));

            }
            is = ubaos.toByteArrayInputStream();
        }

        public String getFilename()
        {
            return filename;
        }

        public InputStream getInputStream()
        {
            return is;
        }

        public HTTPMediaType getMediaType()
        {
            return mediaType;
        }
        public FileToData setMediaType(HTTPMediaType hmt)
        {
            mediaType = hmt;
            return this;
        }

        public long getLength()
        {
            return length;
        }


        @Override
        public String toString() {
            return "FileToData{" +
                    "filename='" + getFilename() + '\'' +
                    ", mediaType=" + getMediaType() +
                    ", length=" + getLength() +
                    '}';
        }
    }

    public static void main(String[] args) {




        long ts = System.currentTimeMillis();
        try {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String url = params.stringValue("url");
            String filePath = params.stringValue("file");
            int repeat = params.intValue("repeat", 1);
            HTTPMethod method = params.enumValue("method", HTTPMethod.POST, HTTPMethod.PUT);
            if(method == null)
                method = HTTPMethod.POST;
            boolean  disableSSL = params.booleanValue("dssl", true);







            //UByteArrayOutputStream baos = IOUtil.inputStreamToByteArray(inputStream, true);
            for (int i=0; i < repeat; i++) {
                FileToData fileToData = new FileToData(filePath);
                fileToData.setMediaType(HTTPMediaType.APPLICATION_OCTET_STREAM);
                System.out.println(fileToData);
                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, method, !disableSSL);
                //hmci.getHeaders().build("Connection", "Close");
                hmci.setContentType(HTTPMediaType.MULTIPART_FORM_DATA);
                NamedValue<InputStream> nvc = new NamedValue<>();
                nvc.setValue(fileToData.getInputStream())
                        .setName("file")
                        .getProperties()
                        .build(HTTPConst.CNP.FILENAME, fileToData.getFilename())
                        .build(new NVLong(HTTPConst.CNP.CONTENT_LENGTH, fileToData.getLength()))
                        .build(new NVEnum(HTTPConst.CNP.MEDIA_TYPE, fileToData.getMediaType()))
                ;

                NVGenericMap paramNVGM = new NVGenericMap("data-nvgm");
                paramNVGM.build("n1", "v1").build(new NVInt("int", 256)).build(new NVDouble("pi", Math.PI));
                hmci.getParameters().build("text", "simple date").build(new NVInt("int-value", 20)).build(nvc).build(paramNVGM);
//                hmci.setContent(IOUtil.inputStreamToByteArray(fileToData.getInputStream(), true).toByteArray() );


                System.out.println(OkHTTPCall.send(hmci));
            }





        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Usage: java WputHttp <url=URL> <file=FilePath> [dssl=yes/no] [method=POST/PUT]");
        }

        System.out.println("It took overall " + Const.TimeInMillis.toString((System.currentTimeMillis() - ts)));
    }
}
