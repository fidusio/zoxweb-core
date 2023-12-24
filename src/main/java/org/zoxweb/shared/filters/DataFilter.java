package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.*;

public abstract class DataFilter<I, O, T>
    implements Identifier<String>, GetName, GetDescription, DataDecoder<I, O>

{

    private final String id;
    private final NamedDescription namedDescription;

    private final T type;
    public DataFilter(T type, String id, String name, String description)
    {
        this.type = type;
        this.id = id;
        namedDescription = new NamedDescription(name, description);
    }

    public T getType() {
        return type;
    }

    @Override
    public String getID()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return namedDescription.getName();
    }

    @Override
    public String getDescription()
    {
        return namedDescription.getDescription();
    }
}
