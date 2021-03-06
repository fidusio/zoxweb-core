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

@SuppressWarnings("serial")
public class ShiroRoleGroupRoleDAO
    extends ShiroAssociationDAO
{
	

	public ShiroRoleGroupRoleDAO()
	{
		super(ShiroAssociationType.ROLE_TO_ROLEGROUP, null, null);
	}
	
	public ShiroRoleGroupRoleDAO(ShiroRoleGroupDAO roleGroup, ShiroRoleDAO role)
	{
		super(ShiroAssociationType.ROLE_TO_ROLEGROUP, roleGroup, role);
	}

	public ShiroRoleDAO getRole()
	{
		return (ShiroRoleDAO) getAssociation();
	}

	public void setRole(ShiroRoleDAO role) 
	{
		setAssociation( role);
	}

	public ShiroRoleGroupDAO getRoleGroup() {
		return (ShiroRoleGroupDAO) getAssociatedTo();
	}

	public void setRoleGroup(ShiroRoleGroupDAO roleGroup) {
		setAssociatedTo( roleGroup);
	}


}