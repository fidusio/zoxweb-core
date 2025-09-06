package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.StatCounter;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.UUID;

public class IDGeneratorUtilTest {

    @Test
    public void perfTest() {
        // invoke it for the first time to force class loading
        IDGs.UUIDSHA256Base64.generateID();
        IDGs.SHA256Base64.generateID();

        StatCounter sc = new StatCounter();


        int max = 10000;
        for (int i = 0; i < max; i++) {
            IDGs.SHA256Base64.generateID();
        }

        System.out.println(TimeInMillis.toString(sc.deltaNow()) + " for " + max + " " + sc.deltaNow());

        max = 10;
        sc.setReferenceTime();

        for (int i = 0; i < max; i++) {
            String id = IDGs.SHA256Base64.generateID();
            System.out.println(id + " length:" + id.length());
        }

        System.out.println(sc.deltaNow() + " millis for " + max);

        sc.setReferenceTime();
        max = 10000;
        for (int i = 0; i < max; i++) {
            IDGs.UUIDBase64.generateID();
        }

        System.out.println(sc.deltaNow() + " millis for " + max);

        max = 10;
        sc.setReferenceTime();

        for (int i = 0; i < max; i++) {
            String id = IDGs.UUIDBase64.generateID();
            System.out.println(id + " length:" + id.length());
        }

        System.out.println(sc.deltaNow() + " millis for " + max);

        String id = UUID.randomUUID().toString();
        String b64 = SharedBase64.encodeAsString(Base64Type.URL, id);
        System.out.println(id + " length:" + id.length());
        System.out.println(b64 + " length:" + b64.length());
        System.out.println(SharedBase64.decodeAsString(Base64Type.URL, b64));
    }

    @Test
    public void testUUIDV4()
    {
        String id = IDGs.UUIDV4.generateID();
        String b64 = SharedBase64.encodeAsString(Base64Type.URL, id);
        System.out.println(id + " length:" + id.length());
        System.out.println(b64 + " length:" + b64.length());
        System.out.println(SharedBase64.decodeAsString(Base64Type.URL, b64));
        System.out.println(IDGs.UUIDV4.encode(IDGs.UUIDV4.decode(id)));
    }
}
