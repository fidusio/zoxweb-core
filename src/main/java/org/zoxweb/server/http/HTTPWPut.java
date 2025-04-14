package org.zoxweb.server.http;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HTTPWPut {
    private static final String dummy = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static class FileToData
    {

        private final String filename;
        private final InputStream is;
        private HTTPMediaType mediaType;
        private final long length;

        public FileToData(String fileToUpload, InputStream is, long length)
        {
            this.filename = fileToUpload;
            this.length = length;
            this.is = is;
            mediaType = HTTPMediaType.lookupByExtension(fileToUpload);
        }

        public FileToData(String fileToUpload) throws IOException {
           File file = new File(fileToUpload);
           if (file.exists())
           {
               this.filename = file.getName();
               length = file.length();
               is = new FileInputStream(file);
               mediaType = HTTPMediaType.lookupByExtension(fileToUpload);
               return;
           }


           String[] tokens = fileToUpload.split(":");
           if (tokens.length != 2)
               throw  new IllegalArgumentException("Invalid dummy filename : " + fileToUpload);

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

    public static HTTPResponseData uploadFile(String url,
                                              HTTPMethod method,
                                              HTTPAuthorization httpAuthorization,
                                              boolean sslCheckEnabled,
                                              FileToData fileToData,
                                              String remoteFileLocation,
                                              String remoteFilePassword)
            throws IOException
    {
        fileToData.setMediaType(HTTPMediaType.APPLICATION_OCTET_STREAM);
        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, method, sslCheckEnabled);
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

        hmci.getParameters().build(nvc);
        if (remoteFileLocation != null)
        {
            hmci.getParameters().build("file-location", remoteFileLocation);
        }
        if(remoteFilePassword != null)
        {
            hmci.getParameters().build("file-password", remoteFilePassword);
        }

        if (httpAuthorization != null)
        {
            hmci.setAuthorization(httpAuthorization);
        }

        return OkHTTPCall.send(hmci);
    }

    public static void main(String[] args) {




        long ts = System.currentTimeMillis();
        try {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String url = params.stringValue("url");
            String filePath = params.stringValue("file");
            String user = params.stringValue("user", true);
            String password = params.stringValue("password", true);

            String fileLocation = params.stringValue("file-location", true);
            String filePassword = params.stringValue("file-password", true);
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

//                HTTPAuthorization httpAuthorization = null;
//                HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, null, method, !disableSSL);
//                //hmci.getHeaders().build("Connection", "Close");
//                hmci.setContentType(HTTPMediaType.MULTIPART_FORM_DATA);
//                NamedValue<InputStream> nvc = new NamedValue<>();
//                nvc.setValue(fileToData.getInputStream())
//                        .setName("file")
//                        .getProperties()
//                        .build(HTTPConst.CNP.FILENAME, fileToData.getFilename())
//                        .build(new NVLong(HTTPConst.CNP.CONTENT_LENGTH, fileToData.getLength()))
//                        .build(new NVEnum(HTTPConst.CNP.MEDIA_TYPE, fileToData.getMediaType()))
//                ;
//
//                hmci.getParameters().build(nvc);
//                if (fileLocation != null)
//                {
//                    hmci.getParameters().build("file-location", fileLocation);
//                }
//                if(filePassword != null)
//                {
//                    hmci.getParameters().build("file-password", filePassword);
//                }
//

//                NVGenericMap paramNVGM = new NVGenericMap("data-nvgm");
//
//                paramNVGM.build("n1", "v1").build(new NVInt("int", 256)).build(new NVDouble("pi", Math.PI));
//                hmci.getParameters().build("text", "simple date").build(new NVInt("int-value", 20)).build(paramNVGM);

//                System.out.println(OkHTTPCall.send(hmci));
                HTTPResponseData hrd = uploadFile(url,
                        method,
                        user != null && password != null ? new HTTPAuthorizationBasic(user, password) : null,
                        !disableSSL,
                        fileToData,
                        fileLocation,
                        filePassword);

                if(hrd.getStatus() == HTTPStatusCode.OK.CODE)
                    System.out.println(hrd.getDataAsString());
                else
                    System.out.println(hrd);
            }





        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Usage: java HTTPWPut <url=URL> <file=FilePath> [dssl=yes/no] [method=POST/PUT]");
        }

        System.out.println("It took overall " + Const.TimeInMillis.toString((System.currentTimeMillis() - ts)));
    }
}
