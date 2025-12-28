package org.zoxweb.shared.data;

import org.zoxweb.shared.util.*;


@SuppressWarnings("serial")
public class PropertyDAO
        extends TimeStampDAO
        implements SetCanonicalID, GetNVProperties {
    public enum Param
            implements GetNVConfig {
        CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "Canonical ID", "CanonicalID", false, true, String.class)),
        PROPERTIES(NVConfigManager.createNVConfig("properties", "Configuration properties", "Properties", false, true, NVGenericMap.class)),
        ;
        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }


    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
     */
    public static final NVConfigEntity NVC_PROPERTY_DAO = new NVConfigEntityPortable("property_dao",
            null,
            "PropertyDAO",
            true,
            false,
            false,
            false,
            PropertyDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO);
    private final CanonicalIDSetter cids;

    public PropertyDAO() {
        super(NVC_PROPERTY_DAO);
        this.cids = null;
    }

    protected PropertyDAO(NVConfigEntity nvce, CanonicalIDSetter cids) {
        super(nvce);
        this.cids = cids;
    }

    protected PropertyDAO(NVConfigEntity nvce) {
        super(nvce);
        this.cids = null;
    }

    /**
     * Returns string representation of this class.
     */
    @Override
    public String toCanonicalID() {
        return getCanonicalID();
    }

    /**
     * Returns canonical ID.
     */
    @Override
    public String getCanonicalID() {
        return lookupValue(Param.CANONICAL_ID);
    }

    /**
     * Sets canonical ID.
     */
    @Override
    public void setCanonicalID(String id) {
        setValue(Param.CANONICAL_ID, id);
    }

    public NVGenericMap getProperties() {
        return lookup(Param.PROPERTIES);
    }


    public synchronized <V> void setValue(String attrName, V value) {
        super.setValue(attrName, value);
        if (cids != null)
            cids.setCanonicalID(this, attrName);
    }
}
