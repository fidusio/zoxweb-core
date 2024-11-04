package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.annotation.EndPointProp;
import org.zoxweb.shared.annotation.MappedProp;
import org.zoxweb.shared.annotation.ParamProp;
import org.zoxweb.shared.annotation.SecurityProp;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.http.HTTPMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class AnnotationScanTest {

    @MappedProp(name = "toBeTested", id = "toBeTested-class")
    @SecurityProp(authentications = {CryptoConst.AuthenticationType.DOMAIN}, roles = "admin")
    public static class ToBeTested
    {
        @EndPointProp(uris = "/profile/{profileId}/{realm}", name = "profileReader", methods = {HTTPMethod.GET}, responseContentType = "text")
        @SecurityProp(authentications = {CryptoConst.AuthenticationType.ALL})
        public String getProfile(@ParamProp(name ="profileId") String profileId, @ParamProp(name ="realm", optional = false) String realm)
        {
            return "str";
        }

        public void noAnnotations(){};
        @Deprecated
        private void notToBeReported(){}


        @EndPointProp(uris = "/check-user/{user}", name = "CheckUser", methods = {HTTPMethod.POST}, responseContentType = "boolean")
        @SecurityProp(authentications = {CryptoConst.AuthenticationType.ALL}, permissions= "self, admin")
        public static boolean checkStatus(String user, int i)
        {
            return true;
        }

    }

    @Test
    public void annotationScan()
    {
       ReflectionUtil.AnnotationMap result =  ReflectionUtil.scanClassAnnotations(ToBeTested.class, EndPointProp.class, Deprecated.class, SecurityProp.class);
       assert(result!=null);
       assert(result.getClassAnnotations() != null);
       assert(!result.getMethodsAnnotations().isEmpty());
       assert(result.isClassAnnotatedBy(SecurityProp.class));

       result =  ReflectionUtil.scanClassAnnotations(ToBeTested.class, MappedProp.class);
       assert(result != null);
       System.out.println(Arrays.toString(result.getClassAnnotations()));

    }

    @Test
    public void testClassAnnotation()
    {
        assert(!ReflectionUtil.isClassAnnotatedAs(ToBeTested.class, Deprecated.class));
        assert(ReflectionUtil.isClassAnnotatedAs(ToBeTested.class, SecurityProp.class));
        assert(ReflectionUtil.isClassAnnotatedAs(ToBeTested.class, MappedProp.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method m = ToBeTested.class.getMethod("getProfile", String.class, String.class);
        assert(ReflectionUtil.isMethodAnnotatedAs(m, EndPointProp.class, SecurityProp.class));
        assert(ReflectionUtil.isMethodAnnotatedAs(m, EndPointProp.class));
        assert(ReflectionUtil.isMethodAnnotatedAs(m, SecurityProp.class));
        assert(!ReflectionUtil.isMethodAnnotatedAs(m, EndPointProp.class, Deprecated.class));
        assert(!ReflectionUtil.isMethodAnnotatedAs(m, Deprecated.class));
    }

    @Test
    public void testParameter() throws NoSuchMethodException {
        Method mGetProfile = ToBeTested.class.getMethod("getProfile", String.class, String.class);
        Parameter[] parameters = mGetProfile.getParameters();
        assert(ReflectionUtil.isParameterAnnotatedAs(parameters[0], ParamProp.class));
        assert(ReflectionUtil.areAllMethodParametersAnnotatedAs(mGetProfile, ParamProp.class));

        Method cCheckState = ToBeTested.class.getMethod("checkStatus", String.class, int.class);
        parameters = cCheckState.getParameters();
        for(Parameter p : parameters)
        {
            assert(!ReflectionUtil.isParameterAnnotatedAs(p, ParamProp.class));
        }

        assert(!ReflectionUtil.areAllMethodParametersAnnotatedAs(cCheckState, ParamProp.class));

    }
}
