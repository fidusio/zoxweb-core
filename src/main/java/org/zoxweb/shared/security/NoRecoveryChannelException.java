package org.zoxweb.shared.security;

/**
 * A self-service password reset was requested for a subject that owns no email principal, so
 * there is no channel to deliver a token through. An administrator can still reset it.
 */
public class NoRecoveryChannelException extends SecurityException {

    public NoRecoveryChannelException(String message) {
        super(message);
    }
}
