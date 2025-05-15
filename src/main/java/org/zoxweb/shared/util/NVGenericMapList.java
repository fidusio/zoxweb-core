package org.zoxweb.shared.util;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class NVGenericMapList
        extends NVBase<List<NVGenericMap>> {
    public NVGenericMapList() {
        this(null, new ArrayList<NVGenericMap>());
    }

    public NVGenericMapList(String name) {
        this(name, new ArrayList<NVGenericMap>());
    }

    public NVGenericMapList(String name, List<NVGenericMap> value) {
        setValue(value);
        setName(name);
    }

    public void add(NVGenericMap value) {
        getValue().add(value);
    }
}
