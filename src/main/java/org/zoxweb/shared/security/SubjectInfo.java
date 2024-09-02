package org.zoxweb.shared.security;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;

public class SubjectInfo
extends PropertyDAO
{
    public enum Param
            implements GetNVConfig
    {
        SUBJECT_GUID(NVConfigManager.createNVConfig(MetaToken.SUBJECT_GUID.getName(), "The subject global identifier.", "SubjectGUID", true, false, true, String.class, null)),
        EMAIL(NVConfigManager.createNVConfig("email", "Primary email address", "Email", true, true, false, String.class, FilterType.EMAIL))
        ;
        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_SUBJECT_INFO = new NVConfigEntityLocal(
            "subject_info",
            null ,
            SubjectInfo.class.getName(),
            true,
            false,
            false,
            false,
            SubjectInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    public SubjectInfo()
    {
        super(NVC_SUBJECT_INFO);
    }


    /**
     * @param subjectGUID the same subjectGUID and GUID of the SubjectIdentifier
     */
    public void setSubjectGUID(String subjectGUID)
    {
        setValue(MetaToken.SUBJECT_GUID.getName(), subjectGUID);
        setValue(MetaToken.GUID.getName(), subjectGUID);
    }
    /**
     * @param guid the same subjectGUID and GUID of the SubjectIdentifier
     */
    public void setGUID(String guid)
    {
        setValue(MetaToken.SUBJECT_GUID.getName(), guid);
        setValue(MetaToken.GUID.getName(), guid);
    }

    public void setEmail(String email)
    {
        setValue(Param.EMAIL, email);
    }

    public String getEmail()
    {
        return lookupValue(Param.EMAIL);
    }
}
