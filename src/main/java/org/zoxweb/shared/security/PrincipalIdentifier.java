package org.zoxweb.shared.security;

import org.zoxweb.shared.app.AppIDDefault;
import org.zoxweb.shared.util.*;

import java.util.Objects;

/**
 * This class is used to define a unique Principal Identifier e.g. username or email. It
 * requires a unique principal id, with two fields domain id and app id that are neither
 * unique nor required.
 */
@SuppressWarnings("serial")
public class PrincipalIdentifier
        extends AppIDDefault
        implements PrincipalID<String>, DomainID<String>, AppID<String> {

    public enum Param
            implements GetNVConfig {
        PRINCIPAL_ID(NVConfigManager.createNVConfig("principal_id", "the unique identifier", "PrincipalID", true, false, true, String.class, null)),
        PRINCIPAL_STATUS(NVConfigManager.createNVConfig("principal_status", "Principal status", "PrincipalStatus", true, true, SecConst.SecStatus.class)),
//        DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "the domain id", "DomainID", false, false, false, String.class, null)),
//        APP_ID(NVConfigManager.createNVConfig("app_id", "the app id", "AppID", false, false, false, String.class, null)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_PRINCIPAL_IDENTIFIER = new NVConfigEntityPortable(
            "principal_identifier",
            null,
            "PrincipalIdentifier",
            true,
            false,
            true,
            false,
            PrincipalIdentifier.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AppIDDefault.NVC_APP_ID_DEFAULT);

    /**
     * The default constructor of the Principal id class
     */
    public PrincipalIdentifier() {
        super(NVC_PRINCIPAL_IDENTIFIER);
    }

    /**
     * Constructor that only sets principal id
     *
     * @param principalID string to set the principal id
     */
    public PrincipalIdentifier(String principalID) {
        this();
        setPrincipalID(principalID);
    }

    /**
     * Constructor that sets principal id, domain id, and app id
     *
     * @param principalID string to set the principal id
     * @param domainID    string to set the domain id
     * @param appID       string to set the app id
     */
    public PrincipalIdentifier(String principalID, String domainID, String appID) {
        this();
        setPrincipalID(principalID);
        setDomainAppID(domainID, appID);
    }

//    /**
//     *
//     * @param nvce NVConfigEntity to pass through
//     */
//    protected PrincipalIdentifier(NVConfigEntity nvce) {
//        super(nvce);
//    }

    /**
     *
     * @param id string principal id
     */
    @Override
    public void setPrincipalID(String id) {
        setValue(Param.PRINCIPAL_ID, id);
    }

    /**
     *
     * @return string principal id
     */
    @Override
    public String getPrincipalID() {
        return lookupValue(Param.PRINCIPAL_ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof PrincipalIdentifier) {
            PrincipalIdentifier principalIdentifier = (PrincipalIdentifier) obj;

            return SharedStringUtil.equals(getPrincipalID(), principalIdentifier.getPrincipalID(), true);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPrincipalID());
    }

    public void setStatus(SecConst.SecStatus status) {
        setValue(Param.PRINCIPAL_STATUS, status);
    }

    public SecConst.SecStatus getStatus() {
        return lookupValue(Param.PRINCIPAL_STATUS);
    }

//    /**
//     *
//     * @return string app id
//     */
//    @Override
//    public String getAppID() {
//        return lookupValue(Param.APP_ID);
//    }
//
//    /**
//     *
//     * @param appID string app id
//     */
//    @Override
//    public void setAppID(String appID) {
//        setValue(Param.APP_ID, appID);
//    }
//
//    /**
//     *
//     * @return string domain id
//     */
//    @Override
//    public String getDomainID() {
//        return lookupValue(Param.DOMAIN_ID);
//    }
//
//    /**
//     *
//     * @param domainID string domain id
//     */
//    @Override
//    public void setDomainID(String domainID) {
//        setValue(Param.DOMAIN_ID, domainID);
//    }
}
