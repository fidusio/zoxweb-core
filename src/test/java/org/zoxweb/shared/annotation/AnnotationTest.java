package org.zoxweb.shared.annotation;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.http.HTTPMethod;

public class AnnotationTest {

    @SecurityProp(permissions = "p1,p2", roles = "r1,r2", authentications = {CryptoConst.AuthenticationType.ALL})
    @EndPointProp(name = "batata", uris="/", methods = {HTTPMethod.POST})
    public void securityTest()
    {

    }
}
