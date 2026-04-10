package org.zoxweb.shared.http;

import java.io.IOException;

public interface HTTPHandler<T> {
    boolean handle(T data) throws IOException;
}
