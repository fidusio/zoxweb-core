package org.zoxweb.shared.util;

/**
 * Used to indicate a feature not supported yet, or setting not supported
 */
public class NotSupportedException
    extends RuntimeException
{

    public NotSupportedException()
    {
        super();
    }

    public NotSupportedException(String message) {
        super(message);
    }


    public NotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }


    public NotSupportedException(Throwable cause) {
        super(cause);
    }
}
