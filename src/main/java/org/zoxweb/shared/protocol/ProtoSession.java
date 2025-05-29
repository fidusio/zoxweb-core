package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.BaseSubjectID;
import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.GetNVProperties;

import java.util.Set;

public interface ProtoSession<S, T>
    extends GetNVProperties, CloseableType, BaseSubjectID<T>
{
    /**
     * @return the actual session associated with the implementation
     */
    S getSession();


    /**
     * @return true is the session is closed or the implementation can be closed, it is not mandatory to obied by the response the caller can invoke close regardless
     */
    boolean canClose();

    Set<AutoCloseable> getAutoCloseables();

    /**
     * Attach the session to the current context like a thread or something else
     * @return true if the session was attached successfully
     */
    boolean attach();

    /**
     * Detach the session from the current context
     * @return true if the session was detached successfully
     */
    boolean detach();
}
