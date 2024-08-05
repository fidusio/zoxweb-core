package org.zoxweb.shared.security.model;

import org.zoxweb.shared.security.shiro.ShiroBase;
import org.zoxweb.shared.security.shiro.ShiroPermission;
import org.zoxweb.shared.security.shiro.ShiroRole;
import org.zoxweb.shared.util.AppID;
import org.zoxweb.shared.util.GetDescription;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

public final class SecurityModel
{
	private SecurityModel()
	{
	}
	public enum AuthzType
	{
		PERMISSION,
		ROLE,
		ROLE_GROUP
	}



	public final static String SEP = ":";


	public final static String TOK_APP_ID = "$$app_id$$";

	public final static String TOK_REFERENCE_ID = "$$reference_id$$";
	public final static String TOK_RESOURCE_ID = "$$resource_id$$";
	public final static String TOK_SUBJECT_ID = "$$subject_id$$";
	public final static String TOK_USER_ID = "$$user_id$$";

	// cruds:
	public final static String ALL = "*";
	public final static String CREATE = "create";
	public final static String READ = "read";
	public final static String UPDATE = "update";
	public final static String DELETE = "delete";

	//
	public final static String ASSIGN = "assign";
	public final static String REMOVE = "remove";

	// permissions
	public final static String PERMISSION = "permission";
	public final static String ROLE = "role";
	public final static String ROLE_GROUP = "role_group";

	//
	public final static String DOMAIN = "domain";
	public final static String RESOURCE = "resource";
	public final static String APP ="app";
	public final static String SHARE = "share";
	public final static String USER = "user";








	public final static String PERM_ADD_PERMISSION = PERMISSION + SEP +  CREATE;//;PERMISSION + SEP + CREATE;//"permission:create";
	public final static String PERM_DELETE_PERMISSION = PERMISSION + SEP + DELETE;//PERMISSION + SEP + DELETE;//"permission:delete";
	public final static String PERM_UPDATE_PERMISSION = PERMISSION + SEP + UPDATE;//PERMISSION + SEP + UPDATE;//"permission:update";
	public final static String PERM_ADD_ROLE =  ROLE + SEP + CREATE;//ROLE + SEP + CREATE;//"role:create";
	public final static String PERM_DELETE_ROLE = ROLE + SEP + DELETE;//ROLE + SEP + DELETE;//"role:delete";
	public final static String PERM_UPDATE_ROLE = ROLE + SEP + UPDATE;//ROLE + SEP + UPDATE;//"role:update";
	public final static String PERM_CREATE_APP_ID = APP + SEP +CREATE;//APP + SEP + CREATE;//"app:create";
	public final static String PERM_DELETE_APP_ID = APP + SEP + DELETE;//APP + SEP + DELETE;//"app:delete";
	public final static String PERM_UPDATE_APP_ID = APP + SEP + UPDATE;//APP + SEP + UPDATE;//"app:update";
	public final static String PERM_ADD_USER = USER + SEP + CREATE;//"user:create";
	public final static String PERM_DELETE_USER = USER + SEP + DELETE;//"user:delete";
	public final static String PERM_READ_USER = USER + SEP + READ;//"user:read";
	public final static String PERM_UPDATE_USER = USER + SEP +USER;//"user:update";
	public final static String PERM_SELF = "self";
	public final static String PERM_PRIVATE = "private";
	public final static String PERM_PUBLIC= "public";
	public final static String PERM_STATUS= "status";
	public final static String PERM_ADD_RESOURCE = RESOURCE + SEP + CREATE;//"resource:add";
	public final static String PERM_DELETE_RESOURCE = RESOURCE + SEP + DELETE;//"resource:delete";
	public final static String PERM_READ_RESOURCE = RESOURCE + SEP + READ;//"resource:read";
	public final static String PERM_UPDATE_RESOURCE = RESOURCE + SEP + UPDATE;//"resource:update";
	public final static String PERM_RESOURCE_ANY = "any";
	public final static String PERM_ASSIGN_PERMISSION = PERMISSION + SEP + ASSIGN + SEP + PERMISSION;
	public final static String PERM_REMOVE_PERMISSION = PERMISSION + SEP + REMOVE + SEP + PERMISSION;

	//public final static String PERM_CREATE = toSecTok(PERMISSION, CREATE);
	//public final static String PERM_ASSIGN_PERMISSION = "assign:permission";
	public final static String PERM_ASSIGN_ROLE = PERMISSION + SEP + ASSIGN + SEP + ROLE;
	public final static String PERM_REMOVE_ROLE = PERMISSION + SEP + REMOVE + SEP + ROLE;

	public final static String PERM_ACCESS = PERMISSION + SEP + "access";
	



	public static String toSecTok(String ...secTokens)
	{
		StringBuilder sb = new StringBuilder();
		for(String token : secTokens)
		{
			token = SharedStringUtil.toTrimmedLowerCase(token);
			if(token != null)
			{
				if(sb.length() > 0 && sb.charAt(sb.length() -1) != SEP.charAt(0) )
					sb.append(SEP);

				sb.append(token);
			}
		}
		return sb.toString();
	}
	

	
	
	
	public enum PermissionToken
		implements GetValue<String>
	{
		APP_ID(TOK_APP_ID),
		PRIVATE(PERM_PRIVATE),
		PUBLIC(PERM_PUBLIC),
		REFERENCE_ID(TOK_REFERENCE_ID),
		RESOURCE_ID(TOK_RESOURCE_ID),
		SUBJECT_ID(TOK_SUBJECT_ID),
		USER_ID(TOK_USER_ID),
		;

		
		private final String value;
		PermissionToken(String value)
		{
			this.value = value ;
		}
		
		@Override
		public String getValue() {
			// TODO Auto-generated method stub
			return value;
		}
		
	}


	public enum Permission
	implements PermissionModel
	{
		APP_ID_CREATE("app_id_create", "Permission to create an app", PERM_CREATE_APP_ID),
		APP_ID_DELETE("app_id_delete", "Permission to delete an app", PERM_DELETE_APP_ID),
		APP_ID_UPDATE("app_id_update", "Permission to update an app", PERM_UPDATE_APP_ID),
		NVE_ALL("nve_all", "Permission nventities all", "nventity", ALL),
		NVE_READ_ALL("nve_read_all", "Permission to read all nventities", "nventity:read", ALL),
		NVE_UPDATE_ALL("nve_update_all", "Permission to read all nventities", "nventity:update", ALL),
		NVE_DELETE_ALL("nve_delete_all", "Permission to delete all nventities", "nventity:delete", ALL),
		NVE_CREATE_ALL("nve_create_all", "Permission to create all nventities", "nventity:create", ALL),
		PERMISSION_ADD("permission_add", "Permission to add a permission", PERM_ADD_PERMISSION),
		PERMISSION_DELETE("permission_delete", "Permission to delete a permission", PERM_DELETE_PERMISSION),
		PERMISSION_UPDATE("permission_update", "Permission to update a permission", PERM_UPDATE_PERMISSION),
		ROLE_ADD("role_add", "Permission to add a role", PERM_ADD_ROLE),
		ROLE_DELETE("role_delete", "Permission to delete a role", PERM_DELETE_ROLE),
		ROLE_UPDATE("role_update", "Permission to update a role", PERM_UPDATE_ROLE),
		USER_CREATE("user_create", "Permission to create a user", PERM_ADD_USER),
		USER_DELETE("user_delete", "Permission to delete a user", PERM_DELETE_USER),
		USER_UPDATE("user_update", "Permission to update a user", PERM_UPDATE_USER),
		USER_READ("user_read", "Permission to update a user", PERM_UPDATE_USER),
		RESOURCE_ADD("resource_add", "Permission to add a resource", PERM_ADD_RESOURCE, TOK_APP_ID),
		RESOURCE_ANY("resource_any", "Any permission applicable", PERM_RESOURCE_ANY),
		
		RESOURCE_DELETE("resource_delete", "Permission to delete a resource", PERM_DELETE_RESOURCE, TOK_APP_ID),
		RESOURCE_UPDATE("resource_update", "Permission to update a resource", PERM_UPDATE_RESOURCE, TOK_APP_ID),
		RESOURCE_READ_ALL("resource_read_all", "Permission to read all resources", PERM_READ_RESOURCE, ALL),
		RESOURCE_READ_PUBLIC("resource_read_public", "Permission to read a public resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_ID, PERM_PUBLIC),
		RESOURCE_READ_PRIVATE("resource_private", "Permission to read  a private resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_ID, PERM_PRIVATE),
		SELF("self", "permission granted to all users", PERM_SELF),
		SELF_USER("self_user", "permission granted to all users", "nventity:create,read,update,delete", TOK_USER_ID, TOK_RESOURCE_ID),
	
		
		;
		private final String name;
		private final String pattern;
		private final String description;
		
		
		
		
			
		
		Permission(String name, String description, String ...values)
		{
			this.name = name;
			this.pattern = PPEncoder.SINGLETON.encode(values);
			this.description = description;
		
		}
	
		
		public String getName() {
			// TODO Auto-generated method stub
			return name;
		}
		
		public String getDescription() {
			// TODO Auto-generated method stub
			return description;
		}
		public String getValue() {
			// TODO Auto-generated method stub
			return pattern();
		}
		
		public String pattern()
		{
			return pattern;
		}
		
		
		
		
		
		
		public ShiroPermission toPermission(String domainID, String appID, NVPair ...tokens)
		{
			return SecurityModel.toPermission(domainID, appID, getName(), getDescription(), getValue(), tokens);
		}
		
		
		
	}
	
	public enum Role
	    implements GetName, GetDescription
	{
		SUPER_ADMIN("super_admin", "Super admin role"),
		DOMAIN_ADMIN("domain_admin", "domain admin role"),
        APP_ADMIN("app_admin", "App admin role"),
        APP_USER("app_user", "App user role"),
        APP_SERVICE_PROVIDER("app_service_provider", "App service provider role"),
        USER("user", "This role is granted to all users"),
        RESOURCE("resource", "role granted to resources")
		
		;
		private final String name;
		private final String description;
	
		
		Role(String name, String description)
		{
			this.name = name;
			this.description = description;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDescription() {
			return description;
		}
		
		public ShiroRole toRole(AppID<String> appID)
		{
			return toRole(appID.getDomainID(), appID.getAppID());
		}
		
		public ShiroRole toRole(String domainID, String appID)
		{
			return toRole(domainID, appID, name, description);
		}
		
		
		
		public static ShiroRole toRole(AppID<String> appID, String name, String description)
		{
			return new ShiroRole(appID.getDomainID(), appID.getAppID(), name, description);
		}
		
		public static ShiroRole toRole(String domainID, String appID, String name, String description)
		{
			return new ShiroRole(domainID, appID, name, description);
		}
			
		public static ShiroRole addPermission(ShiroRole role, ShiroPermission permission)
		{
			permission.setDomainAppID(role.getDomainID(), role.getAppID());
			role.getPermissions().add(permission);
			return role;
		}
	}
	
	
	public static String toSubjectID(String domainID, String appID, GetName gn)
	{
		return toSubjectID(domainID, appID, gn.getName());
	}
	
	
	public static String toSubjectID(String domainID, String appID, String name)
	{
		return SharedUtil.toCanonicalID(ShiroBase.CAN_ID_SEP, domainID, appID, name);
	}
	
	public enum AppPermission
		implements PermissionModel
	{
		ASSIGN_ROLE_APP("assign_role_app", "Assign a role to user", PERM_ADD_ROLE, TOK_APP_ID),
		ORDER_CREATE("order_create", "Create order", "order:create", TOK_APP_ID, PERM_SELF),
		ORDER_DELETE("order_delete", "Delete order", "order:delete", TOK_APP_ID, TOK_RESOURCE_ID),
		ORDER_UPDATE("order_update", "Update order", "order:update", TOK_APP_ID, TOK_RESOURCE_ID),
		ORDER_READ_APP("order_read_app", "Read app  order", "order:read", TOK_APP_ID),
		ORDER_READ_USER_APP("order_read_user_app", "Read app  order", "order:read", TOK_APP_ID, TOK_RESOURCE_ID, TOK_USER_ID),
		ORDER_UPDATE_STATUS_APP("order_update_status_app", "Read app  order", "order:update", TOK_APP_ID, TOK_RESOURCE_ID, PERM_STATUS),
		RESOURCE_ADD("resource_add", "Add resource", PERM_ADD_RESOURCE, TOK_APP_ID),
		RESOURCE_DELETE("resource_delete", "delete resource", PERM_DELETE_RESOURCE, TOK_APP_ID, TOK_RESOURCE_ID),
		RESOURCE_READ_PRIVATE("resource_read_private", "read private resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_ID, PERM_PRIVATE),
		RESOURCE_READ_PUBLIC("resource_read_public", "read public resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_ID, PERM_PUBLIC),
		RESOURCE_UPDATE("resource_update", "update resource", PERM_UPDATE_RESOURCE, TOK_APP_ID),
		SELF("self", "self", PERM_SELF),
		;
		private final String name;
		private final String pattern;
		private final String description;
		
		
		
		
			
		
		AppPermission(String name, String description, String ...values)
		{
			this.name = name;
			this.pattern = PPEncoder.SINGLETON.encode(values);
			this.description = description;
		
		}
	
		
		public String getName() {
			// TODO Auto-generated method stub
			return name;
		}
		
		public String getDescription() {
			// TODO Auto-generated method stub
			return description;
		}
		public String getValue() {
			// TODO Auto-generated method stub
			return pattern();
		}
		
		public String pattern()
		{
			return pattern;
		}
		
		
//		public ShiroPermissionDAO toPermission(String domainID, String appID, NVPair ...tokens)
//		{
//			return SecurityModel.toPermission(domainID, appID, getName(), getDescription(), getValue());
//		}
		
	
	}
	
	/**
	 * The name is the name of the permission and value is the patterm
	 * @param gnv
	 * @return
	 */
	public static ShiroPermission toPermission(String domainID, String appID, GetNameValue<String> gnv, NVPair ...tokens)
	{
		return toPermission(domainID, appID, gnv.getName(), null,  gnv.getValue(), tokens);
	}
	
	public static ShiroPermission toPermission(String domainID, String appID, String name, String description, String pattern, NVPair ...tokens)
	{
		ShiroPermission ret = new ShiroPermission();
		ret.setName(name);
		ret.setDescription(description);
		//ret.setEmbedAppIDEnabled(embedAppID);
		ret.setDomainAppID(domainID, appID);
		
		
		if (tokens != null && tokens.length > 0)
		{
			for(NVPair token : tokens)
				pattern = SharedStringUtil.embedText(pattern, token.getName(), token.getValue());
		}
		
		
		ret.setPermissionPattern(pattern);
		return ret;
		
	}
	
}
