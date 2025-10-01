package org.zoxweb.shared.util;

public class MappedObject<O, M>
        implements GetName {


    public interface Setter<O, M> {
        void valueToMap(MappedObject<O, M> nvMap);

        void mapToValue(MappedObject<O, M> nvMap);

    }


    private final O nv;
    private final M map;
    private final Setter<O, M> setter;


    public MappedObject(O nv, InstanceFactory.Creator<M> creator, Setter<O, M> vmSetter) {
        this(nv, creator.newInstance(), vmSetter);
    }

    public MappedObject(O nv, M map, Setter<O, M> vmSetter) {
        SUS.checkIfNulls("nv or map can't be null", nv, map, vmSetter);
        if (nv instanceof GetName)
            SUS.checkIfNulls("nv.getName() can't be null", ((GetName) nv).getName());
        this.setter = vmSetter;
        this.nv = nv;
        this.map = map;
    }

    public <V> V get() {
        return (V) nv;
    }

    public M getMap() {
        return map;
    }


    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
        return nv instanceof GetName ? ((GetName) nv).getName() : null;
    }

    /**
     * This method will use the associated data decoder set the NV object from the associated map
     */
    public void valueToMap() {
        setter.valueToMap(this);
    }

    /**
     * Set the mapped object the value of the NV object
     */
    public void mapToValue() {
        setter.mapToValue(this);
    }
}
