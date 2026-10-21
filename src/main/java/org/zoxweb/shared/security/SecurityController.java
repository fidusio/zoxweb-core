package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.util.*;

import java.util.function.Supplier;

public interface SecurityController
{

    Object encryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Opens a sealed value in its storage form: the packed record written by
     * {@code CipherCodecs.EDEncoder}.
     *
     * @param value the packed record, null returns null
     * @return the clear text
     * @throws IllegalArgumentException when {@code value} is not a packed record
     */
    String decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, byte[] value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(String userID, APIDataStore<?,?> dataStore, NVEntity container, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    NVEntity decryptValues(APIDataStore<?, ?> dataStore, NVEntity container, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     *
     * @param nve as resource to be associated
     * @param subjectGUID the unique identifier of the subject
     */
    void associateResourceToSubject(NVEntity nve, String subjectGUID);

    String currentPrincipalID()
            throws AccessSecurityException;
    String currentSubjectGUID()
            throws AccessSecurityException;


    /**
     * The access verdict on a resource for the subject bound to the calling thread: the owner
     * rights ({@code nveUserID}, the resource's {@code subject_guid}; null for a resource without
     * owner) or a grant on the resource itself. All requested verbs must be held. Always true in the
     * system context ({@link #isSystemContext()}); false when nobody is authenticated.
     */
    boolean isResourceAccessible(String nveRefID, String nveUserID, CRUD... permissions);

    /**
     * The access verdict on a resource that may be owned by an app (dual ownership, 2026-10-20): a
     * row carrying {@code appGUID} ({@code app_guid}) is the app's, and its owner rights are the
     * caller's app-resource rights on {@code resourceType} in that app
     * ({@code app_resource:<app>:<type>:<caller>:<verb>}), never the subject in {@code subject_guid};
     * a row without an app is judged as {@link #isResourceAccessible(String, String, CRUD...)}. A
     * grant on the row itself (a share) counts for both. The default ignores the app and delegates
     * to the 3-argument form: a controller that knows no app ownership keeps judging on the subject.
     *
     * @param nveRefID     the resource GUID
     * @param nveUserID    the resource's {@code subject_guid}, null for a resource without owner
     * @param appGUID      the resource's {@code app_guid}, null for a resource no app owns
     * @param resourceType the resource's class ({@code SecurityModel.resourceTypeOf}), null when unknown
     * @param permissions  the verbs, all required
     */
    default boolean isResourceAccessible(String nveRefID, String nveUserID, String appGUID, String resourceType, CRUD... permissions) {
        return isResourceAccessible(nveRefID, nveUserID, permissions);
    }

    /**
     * The GUID of the app record the bound subject logged into (its {@code app_guid} for the rows it
     * creates in registered classes), null for a login without domain/app, when nobody is bound, or
     * for a controller that knows no apps (the default).
     */
    default String currentAppGUID() {
        return null;
    }

    /**
     * Runs {@code work} in the system context of the calling thread: the datastore access checks
     * are lifted for it. Meant for the code that must reach the store with nobody logged in, or on
     * behalf of every subject — login, grant loading, key-chain lookups, setup — never for
     * application requests. The default lifts nothing: an implementation that knows no system
     * context stays closed.
     */
    default <V> V runAsSystem(Supplier<V> work) {
        return work.get();
    }

    /**
     * @return true while the calling thread is inside {@link #runAsSystem(Supplier)}
     */
    default boolean isSystemContext() {
        return false;
    }

}
