package org.zoxweb.shared.util;

public interface BiDataDecoder<IT, IU, R> extends Codec {
    R decode(IT it, IU iu);
}
