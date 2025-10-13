package org.zoxweb.shared.util;

public interface DataHandler<S, D> {
    void handleData(S source, D data);
}
