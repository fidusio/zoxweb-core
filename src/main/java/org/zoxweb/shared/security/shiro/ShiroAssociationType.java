/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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

public enum ShiroAssociationType 
{
	
	/**
	 * Permission associated to a resource
	 */
	PERMISSION_TO_RESOURCE,
	/**
	 * Permission Associated to Role
	 */
	PERMISSION_TO_ROLE,
	/**
	 * Permission Associated to Subject
	 */
	PERMISSION_TO_SUBJECT,
	/**
	 * Role Associated to Subject
	 */
	ROLE_TO_SUBJECT,
	/**
	 * Role Associated to RoleGroup
	 */
	ROLE_TO_ROLEGROUP,
	/**
	 * RoleGroup Associated to Subject
	 */
	ROLEGROUP_TO_SUBJECT,
	/**
	 * Role associated to resource
	 */
	ROLE_TO_RESOURCE,
	/**
	 * ROLEGROUP associated to resource
	 */
	ROLEGROUP_TO_RESOURCE
}
