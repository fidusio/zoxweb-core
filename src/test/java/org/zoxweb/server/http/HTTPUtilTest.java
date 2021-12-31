package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
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
        HTTPMessageConfigInterface hmci = HTTPUtil.formatResponse(address);
        UByteArrayOutputStream data = HTTPUtil.formatResponse(hmci, null);
        System.out.println(SharedStringUtil.toString(data.getInternalBuffer(), 0, data.size()));

    }

    @Test
    public void nvgmHTTPMessageConfig() throws IOException {
        NVGenericMap nvgm = new NVGenericMap();
        nvgm.add("string", "hello");
        nvgm.add(new NVLong("long", 1000));
        nvgm.add(new NVBoolean("bool", true));
        nvgm.add(new NVFloat("float", (float) 12.43534));
        HTTPMessageConfigInterface hmci = HTTPUtil.formatResponse(nvgm);
        UByteArrayOutputStream data = HTTPUtil.formatResponse(hmci, null);
        System.out.println(SharedStringUtil.toString(data.getInternalBuffer(), 0, data.size()));

    }
}
