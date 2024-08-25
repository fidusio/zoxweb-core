/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.security.shiro;


import org.zoxweb.shared.data.UserIDDAO;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.security.SubjectIdentifier;
import org.zoxweb.shared.util.GetValue;

import java.util.List;
import java.util.Set;

public interface ShiroRealmStore<O,I>
	extends ShiroRealmController<O,I>
{





	
	/**
	 * Lookup for a role based on the role ID which can either be ref_id or the role subject id
	 * @param roleID
	 * @return the matching role or null if not found
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRole lookupRole(String roleID)
			throws NullPointerException, IllegalArgumentException, AccessException;





	
	/**
	 * Lookup permission based on the permission permission ID which can either be ref_id or the permission subject id
	 * @param permissionID
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroPermission lookupPermission(String permissionID)
			throws NullPointerException, IllegalArgumentException, AccessException;





	/**
	 * Returns all subjects.
	 *
	 * @return list of SubjectIdentifier
	 * @throws AccessException
	 */
	 List<SubjectIdentifier> getAllSubjects()
        throws AccessException;

	/**
	 * Returns all roles.
	 * @return list ShiroRoleDAO
	 * @throws AccessException
	 */
	 List<ShiroRole> getAllShiroRoles()
		throws AccessException;

	/**
	 * Returns all roles groups.
	 * @return list ShiroRoleGroupDAO
	 * @throws AccessException
	 */
	 List<ShiroRoleGroup> getAllShiroRoleGroups()
		throws AccessException;

	/**
	 * Returns all permissions.
	 * @return list ShiroPermissionDAO
	 * @throws AccessException
	 */
	List<ShiroPermission> getAllShiroPermissions()
		throws AccessException;
	


	/**
	 * Create an association.
	 * @param association
	 * @return ShiroAssociationDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	ShiroAssociation addShiroAssociation(ShiroAssociation association)
		throws NullPointerException, IllegalArgumentException, AccessException;
	
	/**
	 * Removes an association.
	 * @param association
	 * @return ShiroAssociationDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	ShiroAssociation removeShiroAssociation(ShiroAssociation association)
        throws NullPointerException, IllegalArgumentException, AccessException;

//
//	/**
//	 * Get the user password
//	 * @param domainID
//	 * @param userID
//	 * @return
//	 * @throws NullPointerException
//	 * @throws IllegalArgumentException
//	 * @throws AccessException
//	 */
//	PasswordDAO getSubjectPassword(String domainID, String userID) throws NullPointerException, IllegalArgumentException, AccessException;
//
//	PasswordDAO setSubjectPassword(SubjectIdentifier subject, PasswordDAO passwd) throws NullPointerException, IllegalArgumentException, AccessException;
//	PasswordDAO setSubjectPassword(String subject, PasswordDAO passwd) throws NullPointerException, IllegalArgumentException, AccessException;
//	PasswordDAO setSubjectPassword(SubjectIdentifier subject, String passwd) throws NullPointerException, IllegalArgumentException, AccessException;
//	PasswordDAO setSubjectPassword(String subject, String passwd) throws NullPointerException, IllegalArgumentException, AccessException;
	/**
	 * Get the user roles
	 * @param domainID
	 * @param userID
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	Set<String> getSubjectRoles(String domainID, String userID) throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Get subject permissions
	 * @param domainID
	 * @param userID
	 * @param roleNames
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 *  @throws AccessException
	 */
	Set<String> getSubjectPermissions(String domainID, String userID, Set<String> roleNames) throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 *
	 * @param subjectID
	 * @param params
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	@Deprecated
	UserIDDAO lookupUserID(GetValue<String> subjectID, String...params)
			throws NullPointerException, IllegalArgumentException, AccessException;
	/**
	 *
	 * @param subjectID
	 * @param params
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	@Deprecated
	UserIDDAO lookupUserID(String subjectID, String...params)
			throws NullPointerException, IllegalArgumentException, AccessException;

}
