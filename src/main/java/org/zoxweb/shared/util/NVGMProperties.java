package org.zoxweb.shared.util;

public abstract class NVGMProperties
        implements SetNVProperties {
    private volatile NVGenericMap nvmg;

    protected NVGMProperties(boolean create) {
        if(create) {
            nvmg = new NVGenericMap();
        }
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
