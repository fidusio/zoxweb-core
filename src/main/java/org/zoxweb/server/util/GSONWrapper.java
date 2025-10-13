package org.zoxweb.server.util;

import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.util.List;


public class GSONWrapper {
    public final Base64Type b64Type;

    public GSONWrapper(Base64Type b64Type) {
        this.b64Type = b64Type;
    }

    public <V extends NVEntity> V fromJSON(String json)
            throws APIException {
        return fromJSON(json, null);
    }

    public <V extends NVEntity> V fromJSON(String json, Class<? extends NVEntity> clazz)
            throws AccessException, APIException {
        return fromJSON(json, clazz, b64Type);
    }

    public <V extends NVEntity> V fromJSON(String json, Class<? extends NVEntity> clazz, Base64Type b64t)
            throws AccessException, APIException {
        return GSONUtil.fromJSON(json, clazz, b64t);
    }

    public <V extends NVEntity> V fromJSON(byte[] json)
            throws APIException {
        return fromJSON(SharedStringUtil.toString(json));
    }

    public <V extends NVEntity> V fromJSON(byte[] json, Base64Type b64t)
            throws APIException {
        return fromJSON(SharedStringUtil.toString(json), null, b64t);
    }

    public List<NVEntity> fromJSONArray(String json) {
        return GSONUtil.fromJSONArray(json, b64Type);
    }

    public String toJSON(NVEntity nve, boolean indent)
            throws IOException {
        return toJSON(nve, indent, true, true);
    }


    public String toJSON(NVEntity nve, boolean indent, Base64Type b64t)
            throws IOException {
        return toJSON(nve, indent, true, true, b64t);
    }


    public String toJSON(NVEntity nve, boolean indent, boolean printNull, boolean printClassType)
            throws IOException {
        return GSONUtil.toJSON(nve, indent, printNull, printClassType, b64Type);
    }

    public String toJSON(NVEntity nve, boolean indent, boolean printNull, boolean printClassType, Base64Type b64t)
            throws IOException {
        return GSONUtil.toJSON(nve, indent, printNull, printClassType, b64t);
    }


    public String toJSONGenericMap(NVGenericMap nvgm, boolean indent, boolean printNull, boolean printClassType)
            throws IOException {
        return GSONUtil.toJSONGenericMap(nvgm, indent, printNull, printClassType);
    }

    public NVGenericMap fromJSONGenericMap(String json, NVConfigEntity nvce)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return GSONUtil.fromJSONGenericMap(json, nvce, b64Type);
    }

    public String toJSONArray(List<NVEntity> list, boolean indent, boolean printNull)
            throws IOException {
        return toJSONArray(list.toArray(new NVEntity[list.size()]), indent, printNull);
    }

    public String toJSONArray(NVEntity[] nves, boolean indent, boolean printNull)
            throws IOException {
        return GSONUtil.toJSONArray(nves, indent, printNull, b64Type);
    }

    public String toJSONValues(NVEntity[] list, boolean indent, boolean printNull, boolean printClass)
            throws IOException {
        return GSONUtil.toJSONValues(list, indent, printNull, printClass, b64Type);
    }

    public String toJSONWrapper(String wrapName, NVEntity nve, boolean indent, boolean printNull, boolean printClassType)
            throws IOException {
        return GSONUtil.toJSONWrapper(wrapName, nve, indent, printNull, printClassType, b64Type);
    }

    public Base64Type getBase64Type() {
        return b64Type;
    }
}
