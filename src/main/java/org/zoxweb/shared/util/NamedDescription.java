package org.zoxweb.shared.util;

public class NamedDescription
        implements NamedDescriptionInt
{
    private String name;
    private String description;

    public NamedDescription(Enum<?> name)
    {
        this(SUS.enumName(name), null);
    }

    public NamedDescription(Enum<?> name, String description)
    {
        this(SUS.enumName(name), description);
    }


    public NamedDescription()
    {

    }

    public NamedDescription(String name)
    {
        this(name, null);
    }
    public NamedDescription(String name, String description){
    SharedUtil.checkIfNulls("name can't be null.", name);
        this.name = name;
        this.description = description;
    }


    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NamedDescription{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
