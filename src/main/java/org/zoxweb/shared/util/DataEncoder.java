package org.zoxweb.shared.util;

public interface DataEncoder<EI, EO>
        extends Codec {
    DataEncoder<String, String> StringLower = (s) -> {
        return s != null ? s.toLowerCase() : null;
    };
    DataEncoder<String, String> StringUpper = (s) -> {
        return s != null ? s.toUpperCase() : null;
    };

    EO encode(EI input);
}
