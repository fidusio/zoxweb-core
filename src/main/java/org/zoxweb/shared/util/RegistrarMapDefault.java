package org.zoxweb.shared.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegistrarMapDefault<K, V>
        extends RegistrarMap<K, V, RegistrarMapDefault<K, V>> {

    public RegistrarMapDefault(Map<K, V> map) {
        super(map);
    }
    public RegistrarMapDefault(DataEncoder<K, K> keyFilter, DataDecoder<V, K> valueToKey)
    {
        this();
        setValueKeyDecoder(valueToKey);
        setKeyFilter(keyFilter);
    }

    public RegistrarMapDefault() {
        this(new LinkedHashMap<>());
    }
}
