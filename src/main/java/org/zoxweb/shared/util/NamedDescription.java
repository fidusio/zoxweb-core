package org.zoxweb.shared.util;

import java.util.Objects;

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
    public NamedDescription(String name, String description)
    {
        SUS.checkIfNulls("name can't be null.", name);
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

//    public boolean equals(Object obj)
//    {
//        if (obj instanceof NamedDescription)
//        {
//            String nameToCompare = ((NamedDescription) obj).getName();
//            String descriptionToCompare = ((NamedDescription) obj).getDescription();
//            return nameToCompare.equals(name) && (SUS.isNotEmpty(description) ? description.equals(descriptionToCompare) : description == descriptionToCompare);
//        }
//        return false;
//    }


    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedDescription that = (NamedDescription) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }
}
