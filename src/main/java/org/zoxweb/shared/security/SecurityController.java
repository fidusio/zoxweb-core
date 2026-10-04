package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.util.*;

import java.util.function.Supplier;

public interface SecurityController
{
    void validateCredential(CredentialInfo ci, String input)
            throws AccessSecurityException;
    void validateCredential(CredentialInfo ci, byte[] input)
            throws AccessSecurityException;

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

    void associateNVEntityToSubjectGUID(NVEntity nve, String subjectGUID);

    String currentSubjectID()
            throws AccessSecurityException;
    String currentSubjectGUID()
            throws AccessSecurityException;


    /**
     * The access verdict on a resource for the subject bound to the calling thread: the owner
     * rights ({@code nveUserID}, the resource's {@code subject_guid}; null for a resource without
     * owner) or a grant on the resource itself. All requested verbs must be held. Always true in the
     * system context ({@link #isSystemContext()}); false when nobody is authenticated.
     */
    boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD... permissions);

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
