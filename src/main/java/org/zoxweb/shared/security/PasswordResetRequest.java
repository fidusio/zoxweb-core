package org.zoxweb.shared.security;

/**
 * The outcome of a password-reset request: the clear token, returned exactly once and never
 * persisted, plus what the caller needs to deliver it. Deliberately a plain object rather than an
 * entity so it can never be stored or serialised by accident; {@link #toString()} masks the token.
 */
public final class PasswordResetRequest {

    private final String token;
    private final String subjectGUID;
    private final String principalID;
    private final String[] deliveryPrincipalIDs;
    private final long expiryTS;
    private final PasswordResetToken.Channel channel;

    public PasswordResetRequest(String token, String subjectGUID, String principalID,
                                String[] deliveryPrincipalIDs, long expiryTS, PasswordResetToken.Channel channel) {
        this.token = token;
        this.subjectGUID = subjectGUID;
        this.principalID = principalID;
        this.deliveryPrincipalIDs = deliveryPrincipalIDs != null ? deliveryPrincipalIDs.clone() : new String[0];
        this.expiryTS = expiryTS;
        this.channel = channel;
    }

    /** The clear token; hand it to the subject (mail) or the admin, never log it. */
    public String getToken() {
        return token;
    }

    public String getSubjectGUID() {
        return subjectGUID;
    }

    /** The principal the reset was requested for, normalized. */
    public String getPrincipalID() {
        return principalID;
    }

    /** Email principals of the subject the token should be mailed to; empty when there are none. */
    public String[] getDeliveryPrincipalIDs() {
        return deliveryPrincipalIDs.clone();
    }

    public long getExpiryTS() {
        return expiryTS;
    }

    public PasswordResetToken.Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "PasswordResetRequest{principal=" + principalID + ", subject=" + subjectGUID + ", channel=" + channel
                + ", expiry=" + expiryTS + ", delivery=" + deliveryPrincipalIDs.length + ", token=****}";
    }
}
