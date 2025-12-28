package org.zoxweb.shared.util;

public interface NVECRUDMonitor {
    /**
     * Applies CRUDNVEntity to session data cache.
     * @param crudNVE
     */
    void monitorNVEntity(CRUDNVEntity crudNVE);

    /**
     * Applies CRUDNVEntityList to session data cache.
     * @param crudNVEList
     */
    void monitorNVEntity(CRUDNVEntityList crudNVEList);

}
