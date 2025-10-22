package org.zoxweb.shared.annotation;

import org.zoxweb.shared.crypto.CryptoConst.AuthenticationType;
import org.zoxweb.shared.http.URIScheme;
import org.zoxweb.shared.util.Const;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author javaconsigliere@gmail.com
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityProp {
    /**
     *
     * @return array of authentication types
     */
    AuthenticationType[] authentications() default {};

    /**
     * List of permissions to be applied to the current function
     * Permissions format: "permission-1,permisssion-2...,permission-n".
     *
     * @return list of permissions
     */
    String permissions() default "";

    Const.LogicalOperator permissionOperator() default Const.LogicalOperator.AND;


    /**
     * List of roles to be applied to the current function
     * Roles format: "role-1,role-2...,role-n".
     *
     * @return list of roles
     */
    String roles() default "";

    Const.LogicalOperator roleOperator() default Const.LogicalOperator.AND;

    /**
     * List of custom restrictions such as localhost
     *
     * @return array of restrictions
     */
    String[] restrictions() default {};


    /**
     * List of NetProtocols to be assigned HTTPS, NetProtocol.HTTP ...
     *
     * @return array of URIScheme
     */
    URIScheme[] protocols() default {};

}
