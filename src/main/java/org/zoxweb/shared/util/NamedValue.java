package org.zoxweb.shared.util;

public class NamedValue<V>
    implements SetNameValue<V>

{
    protected String name;
    protected V value;
    public NamedValue(String name, V value)
    {
        this.name = name;
        this.value = value;
    }
    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     *
     * @return typed value
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Set name property.
     *
     * @param name
     */
    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the value.
     *
     * @param value
     */
    @Override
    public void setValue(V value) {
        this.value = value;
    }
}
