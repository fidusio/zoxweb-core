package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.security.CredentialInfo;
import org.zoxweb.shared.security.SubjectIdentifier;


public interface ShiroRealmManager<O,I>
extends AuthorizationInfoLookup<O,I>
{
    SubjectIdentifier addSubjectIdentifier(String subjectID)
            throws AccessSecurityException;
    SubjectIdentifier deleteSubjectIdentifier(String subjectID)
            throws AccessSecurityException;

    CredentialInfo addCredentialInfo(CredentialInfo ci)
            throws AccessSecurityException;
    CredentialInfo deleteCredentialInfo(CredentialInfo ci)
            throws AccessSecurityException;
    CredentialInfo updateCredentialInfo(CredentialInfo oldCI, CredentialInfo newCI)
            throws AccessSecurityException;



    ShiroPermission addPermission(ShiroPermission permission)
            throws AccessSecurityException;
    ShiroPermission updatePermission(ShiroPermission permission)
            throws AccessSecurityException;
    ShiroPermission deletePermission(ShiroPermission permission)
            throws AccessSecurityException;

    ShiroRole addRole(ShiroRole shiroRole)
            throws AccessSecurityException;
    ShiroRole updateRole(ShiroRole shiroRole)
            throws AccessSecurityException;
    ShiroRole deleteRole(ShiroRole shiroRole)
            throws AccessSecurityException;


    ShiroRoleGroup addRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;
    ShiroRoleGroup updateRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;
    ShiroRoleGroup deleteRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;


    ShiroAuthzInfo addShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;
    ShiroAuthzInfo updateShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;
    ShiroAuthzInfo deleteShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;




    void setDataStore(APIDataStore<?> dataStore);
    APIDataStore<?> getDataStore();

}
