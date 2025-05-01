package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.GetNVProperties;

public interface ProtocolSession<S>
    extends GetNVProperties, CloseableType
{
    /**
     * @return the actual session associated with the implementation
     */
    S getSession();


    /**
     * @return true is the session is closed or the implementation can be closed, it is not mandatory to obied by the response the caller can invoke close regardless
     */
    boolean canClose();
}
