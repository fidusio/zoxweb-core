package org.zoxweb.shared.util;

/**
 * Created on 7/22/17
 */
public interface AppID<T>
        extends DomainID<T>
{


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
