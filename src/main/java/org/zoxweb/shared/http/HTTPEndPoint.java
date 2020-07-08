package org.zoxweb.shared.http;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.security.SecurityProfile;
import org.zoxweb.shared.util.*;


public class HTTPEndPoint
extends SecurityProfile
{
    public enum Param
            implements GetNVConfig
    {
        BEAN(NVConfigManager.createNVConfig("bean", "Bean class name", "Bean", false, true, String.class)),
        PATHS(NVConfigManager.createNVConfig("paths", "Paths", "Paths", false, true, NVStringList.class)),
        METHODS(NVConfigManager.createNVConfig("methods", "HTTP Methods", "Methods", false, true, HTTPMethod[].class)),
        PROTOCOLS(NVConfigManager.createNVConfig("protocols", "Http, Https...", "Protocols", false, true, URIScheme[].class)),
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



    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
     */
    public static final NVConfigEntity NVC_HTTP_END_POINT = new NVConfigEntityLocal("http_end_point",
            null,
            "HTTPEndPoint",
            true,
            false,
            false,
            false,
            HTTPEndPoint.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SecurityProfile.NVC_SECURITY_PROFILE);


    public HTTPEndPoint()
    {
        super(NVC_HTTP_END_POINT);
    }

    public String getName()
    {
        String ret = lookupValue(DataConst.DataParam.NAME);
        if(ret == null)
            ret = getBean();
        return ret;
    }

    public String getBean()
    {
        return lookupValue(Param.BEAN);
    }

    public void setBean(String beanClassName)
    {
        setValue(Param.BEAN, beanClassName);
    }



    public String[] getPaths()
    {
        return ((NVStringList)lookup(Param.PATHS)).getValues();
    }

    public void setPaths(String ...paths)
    {
        ((NVStringList)lookup(Param.PATHS)).setValues(paths);
    }

    public boolean isPathSupported(String path)
    {
        return ((NVStringList)lookup(Param.PATHS)).contains(path);
    }

    public HTTPMethod[] getMethods()
    {
        return ((NVEnumList)lookup(Param.METHODS)).getValues(new HTTPMethod[0]);
    }

    public boolean isMethodSupported(String httpMethod)
    {
        return isMethodSupported((HTTPMethod)SharedUtil.lookupEnum(httpMethod, HTTPMethod.values()));
    }
    public boolean isMethodSupported(HTTPMethod httpMethod)
    {
        NVEnumList methodList = (NVEnumList)lookup(Param.METHODS);
        return methodList.getValue().size() > 0 ? methodList.contains(httpMethod) : true;
    }

    public void setMethods(HTTPMethod ...methods)
    {
        ((NVEnumList)lookup(Param.METHODS)).setValues(methods);
    }



    public URIScheme[] getProtocols()
    {
        return ((NVEnumList)lookup(Param.PROTOCOLS)).getValues(new URIScheme[0]);
    }

    public boolean isProtocolSupported(String protocol)
    {
        return isProtocolSupported((URIScheme)SharedUtil.lookupEnum(protocol, URIScheme.values()));
    }
    public boolean isProtocolSupported(URIScheme protocol)
    {
        NVEnumList protocolList = (NVEnumList)lookup(Param.PROTOCOLS);
        return protocolList.getValue().size() > 0 ? protocolList.contains(protocol) : true;
    }

    public void setProtocols(URIScheme ...protocols)
    {
        ((NVEnumList)lookup(Param.PROTOCOLS)).setValues(protocols);
    }
}
