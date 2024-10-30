package org.zoxweb.shared.util;

public class NamedValue<V>
    implements GetNameValue<V>

{
    protected String name;
    protected V value;
    public NamedValue(){}

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

    public NamedValue<V> setName(String name)
    {
        this.name = name;
        return this;
    }

    /**
     * Sets the value.
     *
     * @param value
     */
    public NamedValue<V> setValue(V value) {
        this.value = value;
        return this;
    }
}
