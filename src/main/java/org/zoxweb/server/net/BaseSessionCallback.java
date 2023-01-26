package org.zoxweb.server.net;

import java.io.OutputStream;
import java.nio.ByteBuffer;


public abstract class BaseSessionCallback<CF>
        extends SessionCallback<CF, ByteBuffer, OutputStream>
{
}
