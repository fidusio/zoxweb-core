package org.zoxweb.shared.security;

/**
 * A self-service password reset was requested for a subject that owns no email principal, so
 * there is no channel to deliver a token through. An administrator can still reset it.
 * <p>
 * Carries {@link Reason#INCOMPLETE}: the subject is valid but its recovery setup is missing.
 */
public class NoRecoveryChannelException extends AccessSecurityException {

    public NoRecoveryChannelException(String message) {
        super(message, Reason.INCOMPLETE);
    }
}
