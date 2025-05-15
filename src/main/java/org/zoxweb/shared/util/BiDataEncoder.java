package org.zoxweb.shared.util;

public interface BiDataEncoder<IT, IU, R> extends Codec {
    R encode(IT it, IU iu);
}
