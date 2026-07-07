package org.zoxweb.shared.security;

import org.zoxweb.shared.util.AppID;
import org.zoxweb.shared.util.SetDescription;
import org.zoxweb.shared.util.SetName;

/**
 * Contract for an object that carries an API key: a shared secret presented by a
 * caller to identify and authenticate itself without a username/password exchange.
 * The key type is generic - typically a {@code String} token (see
 * {@link SubjectAPIKey}) but any representation such as {@code byte[]} is possible.
 * Being {@link SetName} and {@link SetDescription}, every API key can also carry a
 * human-readable name and description.
 *
 * @param <V> the type of the key material
 */
public interface APIKey<V>
        extends SetName, SetDescription,CredentialInfo {

    /**
     * Sets the key material.
     *
     * @param apiKey the key material
     */
    void setAPIKey(V apiKey);

    /**
     * Returns the key material.
     *
     * @return the key material, or {@code null} if unset
     */
    V getAPIKey();

    void setAppID(AppID<String> appID);
    AppID<String> getAppID();
}
