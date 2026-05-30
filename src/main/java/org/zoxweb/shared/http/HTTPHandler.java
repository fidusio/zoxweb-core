package org.zoxweb.shared.http;

import java.io.IOException;

public interface HTTPHandler<T> {

    /**
     *
     * @param data to process
     * @return if true the caller must process the response, false the implementation takes care of it
     * @throws IOException in case of encountered errors
     */
    boolean handle(T data) throws IOException;
}
