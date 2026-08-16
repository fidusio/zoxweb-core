package org.zoxweb.shared.util;

public abstract class NVGMProperties
        implements SetNVProperties {
    private volatile NVGenericMap nvmg;


    protected NVGMProperties(boolean create, String name) {
        if (create) {
            nvmg = new NVGenericMap(name);
        }
    }

    protected NVGMProperties(boolean create) {
        this(create, null);
    }

    protected NVGMProperties(NVGenericMap nvmg) {
        setProperties(nvmg);
    }

    @Override
    public void setProperties(NVGenericMap nvgm) {
        this.nvmg = nvgm;
    }

    @Override
    public NVGenericMap getProperties() {
        return nvmg;
    }
}
