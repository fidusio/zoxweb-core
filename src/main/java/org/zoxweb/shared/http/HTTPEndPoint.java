package org.zoxweb.shared.http;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.security.SecurityProfile;
import org.zoxweb.shared.util.*;

import java.util.Arrays;


public class HTTPEndPoint
extends SecurityProfile
{
    public enum Param
            implements GetNVConfig
    {
        BEAN_CLASS_NAME(NVConfigManager.createNVConfig("bean", "Bean class name", "Bean", false, true, String.class)),
        PATHS(NVConfigManager.createNVConfig("paths", "Paths", "Paths", false, true, NVStringList.class)),
        HTTP_METHODS(NVConfigManager.createNVConfig("methods", "HTTP Methods", "Methods", false, true, HTTPMethod[].class)),
        //PROTOCOLS(NVConfigManager.createNVConfig("protocols", "Http, Https...", "Protocols", false, true, URIScheme[].class)),
        INPUT_CONTENT_TYPE(NVConfigManager.createNVConfig("input_content_type", "InputContentType", "IContentType", false, true, String.class)),
        OUTPUT_CONTENT_TYPE(NVConfigManager.createNVConfig("output_content_type", "OutputContentType", "OContentType", false, true, String.class)),
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
            ret = getBeanClassName();
        return ret;
    }

    public String getBeanClassName()
    {
        return lookupValue(Param.BEAN_CLASS_NAME);
    }

    public void setBeanClassName(String beanClassName)
    {
        setValue(Param.BEAN_CLASS_NAME, beanClassName);
    }

    public String getOutputContentType()
    {
        return lookupValue(Param.OUTPUT_CONTENT_TYPE);
    }

    public void setOutputContentType(String outputContentType)
    {
        setValue(Param.OUTPUT_CONTENT_TYPE, outputContentType);
    }

    public String getInputContentType()
    {
        return lookupValue(Param.INPUT_CONTENT_TYPE);
    }

    public void setInputContentType(String inputContentType)
    {
        setValue(Param.INPUT_CONTENT_TYPE, inputContentType);
    }





    public String[] getPaths()
    {
        return ((NVStringList)lookup(Param.PATHS)).getValues();
    }

    public void setPaths(String ...paths)
    {
        ((NVStringList)lookup(Param.PATHS)).setValues(paths);
    }

//    public boolean isPathSupported(String path)
//    {
//        return ((NVStringList)lookup(Param.PATHS)).contains(path);
//    }

    public boolean isPathSupported(String uri)
    {
        uri = SharedStringUtil.removeCharFromEnd('/', uri);

        //NVStringList toParse =  ((NVStringList)lookup(Param.PATHS));
        String[] uriTokens = SharedStringUtil.parseString(uri, "/", true);//uri.split("/");
        for(String path: getPaths())
        {
            path = SharedStringUtil.removeCharFromEnd('/', path);
            if(uri.equalsIgnoreCase(path)){
                return true;
            }

            String[] pathTokens = SharedStringUtil.parseString(path, "/", true);//path.split("/");
            System.out.println(Arrays.toString(uriTokens) + ":" + Arrays.toString(pathTokens));
            for(int i = 0; i < pathTokens.length; i++)
            {
                boolean optional = pathTokens[i].startsWith("{");
                if(!optional && uriTokens.length == i)
                    break;

                if(optional || uriTokens.length == i ) {
                    return true;
                }


                if (!(uriTokens.length > i && uriTokens[i].equalsIgnoreCase(pathTokens[i]))) {
                    break;
                }
            }
        }
        return false;
    }

    public HTTPMethod[] getHTTPMethods()
    {
        return ((NVEnumList)lookup(Param.HTTP_METHODS)).getValues(new HTTPMethod[0]);
    }

    public boolean isHTTPMethodSupported(String httpMethod)
    {
        return isHTTPMethodSupported((HTTPMethod)SharedUtil.lookupEnum(httpMethod, HTTPMethod.values()));
    }
    public boolean isHTTPMethodSupported(HTTPMethod httpMethod)
    {
        NVEnumList methodList = (NVEnumList)lookup(Param.HTTP_METHODS);
        return methodList.getValue().size() > 0 ? methodList.contains(httpMethod) : true;
    }

    public void setHTTPMethods(HTTPMethod ...methods)
    {
        ((NVEnumList)lookup(Param.HTTP_METHODS)).setValues(methods);
    }



//    public URIScheme[] getProtocols()
//    {
//        return ((NVEnumList)lookup(Param.PROTOCOLS)).getValues(new URIScheme[0]);
//    }
//
//    public boolean isProtocolSupported(String protocol)
//    {
//        return isProtocolSupported((URIScheme)SharedUtil.lookupEnum(protocol, URIScheme.values()));
//    }
//    public boolean isProtocolSupported(URIScheme protocol)
//    {
//        NVEnumList protocolList = (NVEnumList)lookup(Param.PROTOCOLS);
//        return protocolList.getValue().size() > 0 ? protocolList.contains(protocol) : true;
//    }
//
//    public void setProtocols(URIScheme ...protocols)
//    {
//        ((NVEnumList)lookup(Param.PROTOCOLS)).setValues(protocols);
//    }
}
