package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A typed reference to the securable resource a {@link PermissionGrant} is scoped to.
 * <p>
 * The map is embedded in the grant ({@code PermissionGrant.resource_map}): it is created
 * with the grant, deleted with it, and never shared between grants. A relational store may
 * keep it as a child row referenced by the grant, but nothing else points at it by GUID.
 * It names the underlying resource by {@code resource_type}, the fully qualified class
 * name of the entity, and {@code resource_guid}, that entity's GUID. Both are mandatory:
 * every datastore lookup by GUID needs the class name, and the manager loads the entity
 * through them to check that the resource belongs to the grantor before the grant is stored.
 * <p>
 * The security manager treats both values as caller-supplied and verifies them; nothing
 * here does.
 */
public class ResourceMap extends GrantBase {

    public enum Param implements GetNVConfig {

        RESOURCE_GUID(NVConfigManager.createNVConfig(MetaToken.RESOURCE_GUID.getName(), "The resource GUID", "ResourceGUID", true, false, String.class)),
        RESOURCE_TYPE(NVConfigManager.createNVConfig(MetaToken.RESOURCE_TYPE.getName(), "Resource type", "ResourceType", true, false, String.class)),
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
            SUS.extractNVConfigs(Param.values()),
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
     * Constructor that sets the resource reference.
     *
     * @param resourceGUID GUID of the underlying entity
     * @param type         fully qualified class name of the underlying entity
     */
    public ResourceMap(String resourceGUID, String type) {
        this();
        setResourceType(type);
        setResourceGUID(resourceGUID);
    }

    /**
     * Constructor that references an existing entity by its GUID and class name.
     *
     * @param resource the underlying entity; must carry a GUID
     * @throws NullPointerException if resource is null
     */
    public ResourceMap(NVEntity resource) {
        this(guidOf(resource), resource.getClass().getName());
    }

    private static String guidOf(NVEntity resource) {
        SUS.checkIfNulls("resource null", resource);
        return resource.getGUID();
    }

    /**
     * @param type fully qualified class name of the underlying entity
     */
    public void setResourceType(String type) {
        setValue(Param.RESOURCE_TYPE, type);
    }

    /**
     * @return fully qualified class name of the underlying entity
     */
    public String getResourceType() {
        return lookupValue(Param.RESOURCE_TYPE);
    }

    /**
     * @param resourceGUID GUID of the underlying entity
     */
    public void setResourceGUID(String resourceGUID) {
        setValue(Param.RESOURCE_GUID, resourceGUID);
    }

    /**
     * @return GUID of the underlying entity
     */
    public String getResourceGUID() {
        return lookupValue(Param.RESOURCE_GUID);
    }
}
