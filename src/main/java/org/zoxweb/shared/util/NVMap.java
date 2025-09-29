package org.zoxweb.shared.util;

public class NVMap<V, M> {

    private final GetNameValue<V> nv;
    private final M map;
    private volatile DataCodec<V, M> dataCodec;

    public NVMap(GetNameValue<V> nv, M map) {
        SUS.checkIfNulls("nv or map can't be null", nv, map);
        SUS.checkIfNulls("nv.getName() can't be null", nv.getName());
        this.nv = nv;
        this.map = map;
    }

    public <T> T getNV() {
        return (T) nv;
    }

    public M getMap() {
        return map;
    }

    public void setDataCodec(DataCodec<V, M> dataCodec) {
        this.dataCodec = dataCodec;
    }

    public DataCodec<V, M> getDataCodec() {
        return dataCodec;
    }

    /**
     * @return the name of NV object
     */
    public String toString() {
        return nv.getName();
    }

    @Override
    public int hashCode() {
        return nv.getName().hashCode();
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (o == this)
            return true;

        if (o.getClass() == nv.getClass()) {
            return nv.getName().equals(((GetNameValue<?>) o).getName());
        }

        return false;
    }

}
