package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.data.UUIDInfoDAO;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPStatusCode;
import org.zoxweb.shared.protocol.ProtocolDelimiter;
import org.zoxweb.shared.util.*;

import java.io.IOException;

public class HTTPUtilTest {
    @Test
    public void nveHTTPMessageConfig() throws IOException {
        AddressDAO address = new AddressDAO();
        address.setCountry("usa");
        address.setCity("Los Angeles");
        address.setStreet("100 main street");
        address.setStateOrProvince("CA");
        address.setZIPOrPostalCode("90001");
        HTTPMessageConfigInterface hmci = HTTPUtil.formatResponse(address, HTTPStatusCode.OK);
        UByteArrayOutputStream data = HTTPUtil.formatResponse(hmci, null);

        System.out.println(SharedStringUtil.toString(data.getInternalBuffer(), 0, data.size()));

        UUIDInfoDAO uuid = new UUIDInfoDAO();
        hmci = HTTPUtil.formatResponse(uuid, HTTPStatusCode.OK);
        data = HTTPUtil.formatResponse(hmci, null);

        System.out.println(SharedStringUtil.toString(data.getInternalBuffer(), 0, data.size()));

    }

    @Test
    public void nvgmHTTPMessageConfig() throws IOException {
        NVGenericMap nvgm = new NVGenericMap();
        nvgm.add("string", "hello");
        nvgm.add(new NVLong("long", 1000));
        nvgm.add(new NVBoolean("bool", true));
        nvgm.add(new NVFloat("float", (float) 12.43534));
        HTTPMessageConfigInterface hmci = HTTPUtil.formatResponse(nvgm, HTTPStatusCode.OK);
        UByteArrayOutputStream data = HTTPUtil.formatResponse(hmci, null);
        System.out.println(SharedStringUtil.toString(data.getInternalBuffer(), 0, data.size()));

    }

    @Test
    public void parseHTTPRaw()
    {
        String message = "GET /timestamp?a=b&c=d HTTP/1.1" + ProtocolDelimiter.CRLF
                + "Content-Type: application/json" + ProtocolDelimiter.CRLF
                + "User-Agent: Java/11.0.13" + ProtocolDelimiter.CRLF
                + "Host: localhost:8443" + ProtocolDelimiter.CRLF
                + "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2" + ProtocolDelimiter.CRLF
                + "Connection: keep-alive" + ProtocolDelimiter.CRLFCRLF
                ;

        HTTPRawMessage hrm = new HTTPRawMessage(message);
        System.out.println(hrm.parse(true));
    }
}
