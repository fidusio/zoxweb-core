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
        PRINCIPAL_ID(NVConfigManager.createNVConfig(MetaToken.PRINCIPAL_ID.getName(), "the unique identifier", "PrincipalID", true, false, true, String.class, SecConst.SubjectIDFilter.SINGLETON)),
        PRINCIPAL_STATUS(NVConfigManager.createNVConfig("principal_status", "Principal status", "PrincipalStatus", true, true, SecConst.SecStatus.class)),
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
            SUS.extractNVConfigs(Param.values()),
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

            return SUS.equals(getPrincipalID(), principalIdentifier.getPrincipalID(), true);
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

}
