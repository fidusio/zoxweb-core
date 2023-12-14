package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.DataDecoder;

public abstract class HTTPAPIDecoder<O>
        implements DataDecoder<HTTPResponseData, O>
{
}
