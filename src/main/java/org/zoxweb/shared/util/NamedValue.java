package org.zoxweb.shared.util;

/**
 * A named value with its own attached properties: an {@link NVBase} pair (name, V) that also
 * carries a per-instance {@link NVGenericMap}, letting metadata ride along with the value.
 * <p>
 * Notes:
 * <ul>
 * <li>The {@link #NamedValue(Enum, Object)} constructor derives the name via
 * {@link SUS#enumName(Enum)}, the same function used by name-based lookups such as
 * {@code NVGenericMap.getNV(SUS.enumName(...))}, so enum-keyed store and lookup always agree.</li>
 * <li>{@link #close()} cascades to the value when it is itself {@link AutoCloseable}, so a
 * NamedValue can manage the lifecycle of the resource it names.</li>
 * </ul>
 *
 * @param <V> the value type
 */
public class NamedValue<V>
        extends NVBase<V>
        implements
        GetNVProperties,
        AutoCloseable {

    private final NVGenericMap properties = new NVGenericMap("properties");

    public NamedValue() {
    }

    /**
     * Creates a NamedValue from an existing name value pair.
     *
     * @param gnv the name value pair to copy
     */
    public NamedValue(GetNameValue<V> gnv) {
        super(gnv);
    }


    /**
     * Creates a NamedValue with a name and no value.
     *
     * @param name the name
     */
    public NamedValue(String name) {
        this.name = name;
    }

    /**
     * Creates a NamedValue keyed by an enum, the name is resolved via {@link SUS#enumName(Enum)}.
     *
     * @param name  the enum whose name keys the value
     * @param value the value
     */
    public NamedValue(Enum<?> name, V value) {
        super(SUS.enumName(name) , value);
    }


    /**
     * Creates a NamedValue.
     *
     * @param name  the name
     * @param value the value
     */
    public NamedValue(String name, V value) {
        super(name, value);
    }


    /**
     * @return the properties of the instance
     */
    @Override
    public NVGenericMap getProperties() {
        return properties;
    }

    /**
     * Closes the contained value if it is {@link AutoCloseable}, no-op otherwise.
     *
     * @throws Exception in case of error
     */
    @Override
    public void close() throws Exception {
        if (value != null && value instanceof AutoCloseable)
            ((AutoCloseable) value).close();
    }

    @Override
    public String toString() {
        return "NamedValue{" +
                "name='" + getName() + '\'' +
                ", value=" + getValue() +
                ", properties=" + getProperties() +
                '}';
    }
}
