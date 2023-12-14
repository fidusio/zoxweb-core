package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.BiDataEncoder;

public abstract class HTTPAPIEncoder<I>
        implements BiDataEncoder<HTTPMessageConfigInterface, I, HTTPMessageConfigInterface>
{
}
