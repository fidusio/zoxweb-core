package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class that defines a canonical handle for any securable thing - a domain object,
 * a method in a class, a URI, etc. Objects reference it through the ResourceMapGUID.
 */
public class ResourceMap extends GrantBase {
    public enum ResourceType {
        OBJECT,
        PRINTER,
        METHOD,
        URI,
    }

    public enum Param implements GetNVConfig {
        TYPE(NVConfigManager.createNVConfig("type", "The type of resource", "ResourceType", false, false, ResourceType.class)),
        RESOURCE_GUID(NVConfigManager.createNVConfig("resource_guid", "The resource GUID", "ResourceGUID", false, false, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_RESOURCE_MAP = new NVConfigEntityPortable(
            "resource_map",
            null,
            "ResourceMap",
            true,
            false,
            false,
            false,
            ResourceMap.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            GrantBase.NVC_GRANT_BASE
    );

    /**
     * The default constructor for the ResourceMap class.
     */
    public ResourceMap() {
        super(NVC_RESOURCE_MAP);
    }

    /**
     * Constructor that sets the Resource Type.
     *
     * @param type the resource type
     */
    public ResourceMap(ResourceType type) {
        this();
        setResourceType(type);
    }

    public ResourceMap(ResourceType type, String resourceGUID) {
        this(type);
        setResourceGUID(resourceGUID);
    }

    /**
     *
     * @param type the resource type
     */
    public void setResourceType(ResourceType type) {
        setValue(Param.TYPE, type);
    }

    /**
     *
     * @return the resource type
     */
    public ResourceType getResourceType() {
        return lookupValue(Param.TYPE);
    }

    /**
     *
     * @param resourceGUID the resource reference
     */
    public void setResourceGUID(String resourceGUID) {
        setValue(Param.RESOURCE_GUID, resourceGUID);
    }

    /**
     *
     * @return the resource reference
     */
    public String getResourceGUID() {
        return lookupValue(Param.RESOURCE_GUID);
    }
}
