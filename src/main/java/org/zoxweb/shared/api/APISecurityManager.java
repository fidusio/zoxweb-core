package org.zoxweb.shared.api;

import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.security.JWTToken;
import org.zoxweb.shared.security.shiro.ShiroRealmStore;
import org.zoxweb.shared.security.shiro.ShiroRulesManager;
import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.NVBase;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;

public interface APISecurityManager<S, O, I>
	extends ShiroRealmStore<O,I>, ShiroRulesManager
{
	
	Object encryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte msKey[])
			throws NullPointerException, IllegalArgumentException, AccessException;
	
	Object decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte msKey[])
			throws NullPointerException, IllegalArgumentException, AccessException;
	
	 String decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVPair nvp, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 Object decryptValue(String userID, APIDataStore<?, ?> dataStore, NVEntity container, Object value, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 NVEntity decryptValues(APIDataStore<?, ?> dataStore, NVEntity container, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 void associateNVEntityToSubjectUserID(NVEntity nve, String userID);
	 
	 String currentSubjectID()
			 throws AccessException;
	 
	 String currentUserID()
			 throws AccessException;
	 
	 String currentDomainID()
			 throws AccessException;
	 
	 String currentAppID()
			 throws AccessException;
	 
	 String currentJWTSubjectID()
			 throws AccessException;
	 
	 S getDaemonSubject();
	 void setDaemonSubject(S subject);
	 
	 boolean isNVEntityAccessible(NVEntity nve, CRUD ...permissions);
	 
	 boolean isNVEntityAccessible(LogicalOperator lo, NVEntity nve, CRUD ...permissions);
	 
	 boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD ...permissions);
	 
	 
	 String checkNVEntityAccess(NVEntity nve, CRUD ...permissions)
				throws NullPointerException, IllegalArgumentException, AccessException;
	 String checkNVEntityAccess(LogicalOperator lo, NVEntity nve, CRUD ...permissions)
				throws NullPointerException, IllegalArgumentException, AccessException;
	 String checkNVEntityAccess(String nveRefID, String nveUserID, CRUD ...permissions)
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 void checkSubject(String subjectID)
				throws NullPointerException, AccessException;
	 
	 String checkNVEntityAccess(String nveRefID, CRUD ...permissions)
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 
	 void checkPermissions(String ...permissions)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 void checkPermissions(boolean partial, String ...permissions)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 
	 
	 void checkPermission(NVEntity nve, String permission)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 boolean isPermitted(NVEntity nve, String permission)
			 throws NullPointerException, IllegalArgumentException;
	 
	 boolean isPermitted(String permission)
			 throws NullPointerException, IllegalArgumentException;
	 
	 /**
	  * Return true of subject has all the permissions
	  * @param permission
	  * @return
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 boolean hasPermission(String permission)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 /**
	  * Check if the current subject has all the roles
	  * @param roles
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 void checkRoles(String ...roles)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 /**
	  * Check if the current subject has the roles, if partial is true one of the is sufficient
	  * @param partial
	  * @param roles
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 void checkRoles(boolean partial, String ...roles)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 /**
	  * Check if the user has the role
	  * @param role
	  * @return
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 boolean hasRole(String role)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 
	 /**
	  * Login a user based on user name and password
	  * @param subjectID
	  * @param credentials
	  * @param domainID
	  * @param appID
	  * @param autoLogin
	  * @return
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 S login(String subjectID, String credentials, String domainID, String appID, boolean autoLogin)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 /**
	  * Login a subject based on jwtToken
	  * @param jwtToken
	  * @return
	  * @throws NullPointerException
	  * @throws IllegalArgumentException
	  * @throws AccessException
	  */
	 S login(JWTToken jwtToken)
			 throws NullPointerException, IllegalArgumentException, AccessException;
	
	 /**
	  * Logout the current subject
	  */
	 void logout();
	 
	 
	 /**
	  * Invalidate a resource
	  * @param resourceID
	  */
	 void invalidateResource(String resourceID);
	 
	 
	
	
	 
}
