package org.zoxweb.shared.util;

import org.zoxweb.shared.filters.AppIDNameFilter;
import org.zoxweb.shared.filters.FilterType;

/**
 * Created on 7/22/17
 */
public interface AppID<T>
        extends DomainID<T> {
    char CAN_ID_SEP = '-';

    static String toDomainAppID(String domainID, String appIDName) {
        return SUS.toCanonicalID(AppID.CAN_ID_SEP, FilterType.DOMAIN.validate(domainID), AppIDNameFilter.SINGLETON.validate(appIDName));
    }

    /**
     * Gets the app ID.
     * @return app id
     */
    T getAppID();

    /**
     * Sets the app ID.
     * @param appID
     */
    void setAppID(T appID);


}
