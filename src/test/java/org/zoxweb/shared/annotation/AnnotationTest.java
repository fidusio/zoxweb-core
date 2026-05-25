package org.zoxweb.shared.annotation;

import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.security.SecConst;

public class AnnotationTest {

    @SecurityProp(permissions = "p1,p2", roles = "r1,r2", authentications = {SecConst.AuthenticationType.ALL})
    @EndPointProp(name = "batata", uris="/", methods = {HTTPMethod.POST})
    public void securityTest()
    {

    }
}
