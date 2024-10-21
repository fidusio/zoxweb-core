package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.*;

public abstract class DataFilter<I, O, T>
    implements Identifier<String>, GetName, GetDescription, DataDecoder<I, O>, ValueFilter<I,O>

{

    private final String id;
    private final NamedDescription namedDescription;

    private final T type;
    protected DataFilter(T type, String id, String name, String description)
    {
        this.type = type;
        this.id = id;
        namedDescription = new NamedDescription(name, description);
    }

    public T getType() {
        return type;
    }

    @Override
    public String toCanonicalID() {
        return getID();
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

    public O validate(I input)
    {
        return decode(input);
    }

    public boolean isValid(I input)
    {
        throw new IllegalArgumentException("Not implemented yet");
    }
}
