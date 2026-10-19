package org.zoxweb.shared.security;


import org.zoxweb.shared.data.SetNameDescriptionImpl;
import org.zoxweb.shared.util.*;

public class BasicAuthToken
        extends SetNameDescriptionImpl
        implements SubjectID<String>
{
    public enum Param
            implements GetNVConfig
    {
        USER(NVConfigManager.createNVConfig("user", "User name", "User", true, true, String.class)),
        PASSWORD(NVConfigManager.createNVConfig("password", "Password", "Password", true, true, byte[].class)),
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

    public static final NVConfigEntity NVC_BASIC_AUTH_TOKEN = new NVConfigEntityPortable(
            "basic_auth_token",
            null ,
            BasicAuthToken.class.getSimpleName(),
            true,
            false,
            false,
            false,
            BasicAuthToken.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionImpl.NVC_NAME_DESCRIPTION_IMPL
    );

    public BasicAuthToken()
    {
        super(NVC_BASIC_AUTH_TOKEN);
    }

    public String getUser()
    {
        return lookupValue(Param.USER);
    }
    public void setUser(String user)
    {
        setValue(Param.USER, user);
    }

    public byte[] getPassword()
    {
        return lookupValue(Param.PASSWORD);
    }

    public void setPassword(byte[] password)
    {
        setValue(Param.PASSWORD, password);
    }

    public void setPassword(String password)
    {
        setValue(Param.PASSWORD, SUS.getBytes(password));
    }

    @Override
    public String getSubjectID()
    {
        return getUser();
    }

    @Override
    public void setSubjectID(String subjectID)
    {
        setUser(subjectID);
    }
}
