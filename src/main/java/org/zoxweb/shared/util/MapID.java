package org.zoxweb.shared.util;

import java.util.Map;

public class MapID
        implements Identifier<String>,
        GetValue<Map<?,?>>
{
    private final String id;
    private final Map<?,?> map;
    public MapID(String id, Map<?,?> map) {
        this.id = id;
        this.map = map;
    }

    public String getID() {
        return id;
    }
    public Map<?,?> getValue() {
        return map;
    }
}