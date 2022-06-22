package org.zoxweb.shared.util;

public class NotFoundException
extends RuntimeException
{
    public NotFoundException()
    {
        super();
    }
    public NotFoundException(String reason)
    {
        super(reason);
    }
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public NotFoundException(Throwable cause) {
        super(cause);
    }
}
