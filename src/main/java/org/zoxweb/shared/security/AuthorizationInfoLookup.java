package org.zoxweb.shared.security;

import org.zoxweb.shared.util.DataDecoder;

/**
 * Lookup hook that resolves the authorization information (roles, permissions) held for an
 * identity.
 * <p>
 * The types are left generic so this contract stays free of any security framework: the
 * implementer binds them to its own types. A Shiro realm, for example, binds {@code I} to its
 * {@code PrincipalCollection} and {@code O} to its {@code AuthorizationInfo}, which lets a caller
 * that only holds the realm fetch the authorization information the realm computes internally.
 * </p>
 * <p>
 * The interface is also a {@link DataDecoder}: {@link #decode(Object)} is the same lookup, so an
 * implementation can be passed wherever a decoder from identity to authorization information is
 * expected.
 * </p>
 *
 * @param <I> the input type identifying whose authorization information is looked up
 * @param <O> the authorization information type returned
 * @see DataDecoder
 */
public interface AuthorizationInfoLookup<I, O>
        extends DataDecoder<I, O> {
    /**
     * Same as {@link #lookupAuthorizationInfo(Object)}.
     *
     * @param input the identity to look up
     * @return the authorization information for the identity
     */
    default O decode(I input) {
        return lookupAuthorizationInfo(input);
    }

    /**
     * Looks up the authorization information of an identity.
     *
     * @param pc the identity to look up
     * @return the authorization information for the identity
     */
    O lookupAuthorizationInfo(I pc);
}
