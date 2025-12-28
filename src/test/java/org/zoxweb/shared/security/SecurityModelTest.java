package org.zoxweb.shared.security;

import org.zoxweb.shared.security.model.PPEncoder;
import org.zoxweb.shared.security.model.SecurityModel.Permission;
import org.zoxweb.shared.security.model.SecurityModel.PermissionToken;

public class SecurityModelTest
{
	public static void main(String ...args)
	{
		for (Permission perm : Permission.values())
		{
			System.out.println(perm.getValue());
		}
		
		System.out.println(PPEncoder.SINGLETON.encodePattern(Permission.RESOURCE_READ_PUBLIC.pattern(), PermissionToken.APP_ID, "xlogistx.io-xlogistx"));
		System.out.println(PPEncoder.SINGLETON.encodePattern(Permission.RESOURCE_READ_PRIVATE.pattern(), PermissionToken.APP_ID, "*"));
	}
}
