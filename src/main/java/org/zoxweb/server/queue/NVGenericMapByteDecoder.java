package org.zoxweb.server.queue;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;

public class NVGenericMapByteDecoder implements DataDecoder<byte[], NVGenericMapQueueEvent> {

    @Override
    public NVGenericMapQueueEvent decode(byte[] input) {
        try {
            NVGenericMap content = GSONUtil.fromJSONGenericMap(SharedStringUtil.toString(input), null, Base64Type.URL);
            return new NVGenericMapQueueEvent(this, content);
        } catch (AccessException | APIException | NullPointerException | IllegalArgumentException e) {
            // TODO Auto-generated catch block
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
