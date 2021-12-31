package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;

public final class HTTPConst {
    private HTTPConst(){}
    public static final GetNameValue<String> CHARSET_UTF_8 = new NVPair("charset", "utf-8");
}
