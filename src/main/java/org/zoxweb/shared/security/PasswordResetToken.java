package org.zoxweb.shared.security;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * One password-reset attempt for a subject. Stores only the SHA-256 of the clear token (the
 * token itself is returned once to the requester and never persisted), the expiry, the delivery
 * channel and the outcome. Exactly one token per subject is outstanding at a time: issuing a new
 * one marks the previous ACTIVE rows INACTIVE (superseded); a consumed row becomes DEACTIVATED.
 * <p>
 * Not a {@link CredentialInfo}: a reset token authenticates nothing and must stay out of the
 * credential collections the realm and {@code verifyPassword} read. {@code subject_guid} comes
 * from the entity base; {@code broker_guid} is the issuing admin for an ADMIN-channel token.
 */
public class PasswordResetToken extends PropertyDAO {

    /** How the token reaches its holder. */
    public enum Channel {
        /** Mailed to the subject's email principal(s). */
        EMAIL,
        /** Handed out of band by an administrator. */
        ADMIN
    }

    public enum Param implements GetNVConfig {
        PRINCIPAL_ID(NVConfigManager.createNVConfig("principal_id", "The principal the reset was requested for (normalized)", "PrincipalID", true, true, String.class)),
        TOKEN_HASH(NVConfigManager.createNVConfig("token_hash", "base64url SHA-256 of the clear token", "TokenHash", true, true, String.class)),
        EXPIRY_TS(NVConfigManager.createNVConfig("expiry_ts", "Epoch millis after which the token is dead", "ExpiryTS", true, true, Long.class)),
        CONSUMED_TS(NVConfigManager.createNVConfig("consumed_ts", "Epoch millis of successful use, 0 while unused", "ConsumedTS", false, true, Long.class)),
        STATUS(NVConfigManager.createNVConfig("status", "ACTIVE outstanding, DEACTIVATED consumed, INACTIVE superseded or cancelled", "Status", true, true, SecConst.SecStatus.class)),
        CHANNEL(NVConfigManager.createNVConfig("channel", "EMAIL or ADMIN", "Channel", true, true, Channel.class)),
        BROKER_GUID(NVConfigManager.createNVConfig(MetaToken.BROKER_GUID.getName(), "Issuing admin subject GUID, null for self-service", "BrokerGUID", false, true, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_PASSWORD_RESET_TOKEN = new NVConfigEntityPortable(
            "password_reset_token",
            null,
            "PasswordResetToken",
            false,
            true,
            false,
            false,
            PasswordResetToken.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    public PasswordResetToken() {
        super(NVC_PASSWORD_RESET_TOKEN);
    }

    public String getPrincipalID() {
        return lookupValue(Param.PRINCIPAL_ID);
    }

    public void setPrincipalID(String principalID) {
        setValue(Param.PRINCIPAL_ID, principalID);
    }

    public String getTokenHash() {
        return lookupValue(Param.TOKEN_HASH);
    }

    public void setTokenHash(String tokenHash) {
        setValue(Param.TOKEN_HASH, tokenHash);
    }

    public long getExpiryTS() {
        Long v = lookupValue(Param.EXPIRY_TS);
        return v != null ? v : 0L;
    }

    public void setExpiryTS(long expiryTS) {
        setValue(Param.EXPIRY_TS, expiryTS);
    }

    public long getConsumedTS() {
        Long v = lookupValue(Param.CONSUMED_TS);
        return v != null ? v : 0L;
    }

    public void setConsumedTS(long consumedTS) {
        setValue(Param.CONSUMED_TS, consumedTS);
    }

    public SecConst.SecStatus getStatus() {
        return lookupValue(Param.STATUS);
    }

    public void setStatus(SecConst.SecStatus status) {
        setValue(Param.STATUS, status);
    }

    public Channel getChannel() {
        return lookupValue(Param.CHANNEL);
    }

    public void setChannel(Channel channel) {
        setValue(Param.CHANNEL, channel);
    }

    public String getBrokerGUID() {
        return lookupValue(Param.BROKER_GUID);
    }

    public void setBrokerGUID(String brokerGUID) {
        setValue(Param.BROKER_GUID, brokerGUID);
    }

    /**
     * @param now epoch millis
     * @return true while the token is ACTIVE and not yet expired
     */
    public boolean isOutstanding(long now) {
        return getStatus() == SecConst.SecStatus.ACTIVE && getExpiryTS() > now;
    }
}
