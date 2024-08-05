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


import org.zoxweb.shared.crypto.PasswordDAO;
import org.zoxweb.shared.data.UserIDDAO;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.security.SubjectIdentifier;
import org.zoxweb.shared.util.GetValue;

import java.util.List;
import java.util.Set;

public interface ShiroRealmStore
	extends ShiroRulesManager
{

	/**
	 * Add a subject
	 * @param subject
	 * @return ShiroSubjectDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroSubject addSubject(ShiroSubject subject)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Add a subject
	 * @param subject
	 * @return ShiroSubjectDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	SubjectIdentifier addSubject(SubjectIdentifier subject)
			throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Delete a subject
	 * @param subject
	 * @param withRoles
	 * @return ShiroSubjectDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroSubject deleteSubject(ShiroSubject subject, boolean withRoles)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Updates a subject, usually the password.
	 * @param subject
	 * @return ShiroSubjectDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroSubject updateSubject(ShiroSubject subject)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Add a role
	 * @param role 
	 * @return ShiroRoleDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRole addRole(ShiroRole role)
        throws NullPointerException, IllegalArgumentException, AccessException;
	
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
	 * Delete a role.
	 * @param role
	 * @param withPermissions
	 * @return ShiroRoleDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRole deleteRole(ShiroRole role, boolean withPermissions)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Updates a role
	 * @param role
	 * @return ShiroRoleDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRole updateRole(ShiroRole role)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Adds a role group.
	 * @param rolegroup
	 * @return ShiroRoleGroupDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRoleGroup addRoleGroup(ShiroRoleGroup rolegroup)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Delete a role group.
	 * @param rolegroup
	 * @return ShiroRoleGroupDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRoleGroup deleteRoleGroup(ShiroRoleGroup rolegroup)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Update a role group.
	 * @param rolegroup
	 * @return ShiroRoleGroupDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroRoleGroup updateRoleGroup(ShiroRoleGroup rolegroup)
        throws NullPointerException, IllegalArgumentException, AccessException;
	
	/**
	 * Add a permission
	 * @param permission
	 * @return ShiroPermissionDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroPermission addPermission(ShiroPermission permission)
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
	 * Delete a permission
	 * @param permission
	 * @return ShiroPermissionDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroPermission deletePermission(ShiroPermission permission)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Updates a permission.
	 * @param permission 
	 * @return ShiroPermissionDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroPermission updatePermission(ShiroPermission permission)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Returns all subjects.
	 * @return list of ShiroSubjectDAO
	 * @throws AccessException
	 */
	 List<ShiroSubject> getAllShiroSubjects()
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
	 * Looks up a subject based on username.
	 * @param userName
	 * @return ShiroSubjectDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	 ShiroSubject lookupSubject(String userName)
        throws NullPointerException, IllegalArgumentException, AccessException;

	/**
	 * Looks up association collection.
	 * @param shiroDao
	 * @param sat
	 * @return ShiroCollectionAssociationDAO
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
//	ShiroCollectionAssociationDAO lookupShiroCollection(ShiroBase shiroDao, ShiroAssociationType sat)
//		throws NullPointerException, IllegalArgumentException, AccessException;
	
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


	/**
	 * Get the user password
	 * @param domainID
	 * @param userID
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	PasswordDAO getSubjectPassword(String domainID, String userID) throws NullPointerException, IllegalArgumentException, AccessException;

	PasswordDAO setSubjectPassword(SubjectIdentifier subject, PasswordDAO passwd) throws NullPointerException, IllegalArgumentException, AccessException;
	PasswordDAO setSubjectPassword(String subject, PasswordDAO passwd) throws NullPointerException, IllegalArgumentException, AccessException;
	PasswordDAO setSubjectPassword(SubjectIdentifier subject, String passwd) throws NullPointerException, IllegalArgumentException, AccessException;
	PasswordDAO setSubjectPassword(String subject, String passwd) throws NullPointerException, IllegalArgumentException, AccessException;
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


	/**
	 *
	 * @param subjectID
	 * @param params
	 * @return
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 * @throws AccessException
	 */
	SubjectIdentifier lookupSubjectID(GetValue<String> subjectID, String...params)
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
	SubjectIdentifier lookupSubjectID(String subjectID, String...params)
			throws NullPointerException, IllegalArgumentException, AccessException;
}
