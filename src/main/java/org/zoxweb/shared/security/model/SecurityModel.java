package org.zoxweb.shared.security.model;

import org.zoxweb.shared.app.AppIDDefault;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.security.PermissionInfo;
import org.zoxweb.shared.security.RoleInfo;
import org.zoxweb.shared.util.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SecurityModel {
    private SecurityModel() {
    }

    public enum AuthzType {
        PERMISSION,
        ROLE,
        ROLE_GROUP
    }


    public final static String PART_SEP = ":";
    public final static char   C_PART_SEP = PART_SEP.charAt(0);
    public final static String SUBPART_SEP = ",";
    public final static char   C_SUBPART_SEP = SUBPART_SEP.charAt(0);
    public final static String WILDCARD = "*";



    // token to be dynamically replaced
    public final static String TOK_APP_ID = "$$app_id$$";

    public final static String TOK_GUID = "$$" + Const.GUID + "$$";
    public final static String TOK_RESOURCE_GUID = "$$" + Const.RESOURCE_GUID + "$$";
    //public final static String TOK_SUBJECT_ID = "$$" + Const.SUBJECT_ID + "$$";
    public final static String TOK_SUBJECT_GUID = "$$" + Const.SUBJECT_GUID + "$$";
    public final static String TOK_CRUD = "$$crud$$";


    public enum SecToken
            implements GetName {
        APP_ID(TOK_APP_ID),
        RESOURCE_GUID(TOK_RESOURCE_GUID),
//        SUBJECT_ID(TOK_SUBJECT_ID),
        SUBJECT_GUID(TOK_SUBJECT_GUID),
        CRUD(TOK_CRUD),
        ;

        private final String tag;

        SecToken(String tag) {
            this.tag = tag;
        }

        /**
         * @return the name of the object
         */
        @Override
        public String getName() {
            return tag;
        }

        public GetNameValue<String> toGNV(String value) {
            return new NVPair(this, value);
        }


        public static String updateToken(String token, GetNameValue<String>... gnvs) {
            for (GetNameValue<String> gnv : gnvs) {
                token = SharedStringUtil.embedText(token, gnv.getName(), gnv.getValue());
            }

            return token;
        }
    }


    // cruds:

    public final static String CREATE = "create";
    public final static String READ = "read";
    public final static String UPDATE = "update";
    public final static String DELETE = "delete";
    public final static String SHARE = "share";
    public final static String USE = "use";

    public final static String ASSIGN = "assign";
    public final static String REMOVE = "remove";

    // permissions
    public final static String PERMISSION = "permission";
    public final static String ROLE = "role";
    public final static String ROLE_GROUP = "role_group";

    //
    public final static String DOMAIN = "domain";
    public final static String RESOURCE = "resource";
    public final static String APP = "app";
    public final static String SUBJECT = "subject";
    /**
     * Namespace of entity-instance permissions: {@code nventity:<verbs>[:<entity guid>]}.
     * The security controller checks {@code nventity:<verb>:<guid>} for every entity access.
     */
    public final static String NVENTITY = "nventity";


    public final static String PERM_ADD_PERMISSION = PERMISSION + PART_SEP + CREATE;//;PERMISSION + SEP + CREATE;//"permission:create";
    public final static String PERM_DELETE_PERMISSION = PERMISSION + PART_SEP + DELETE;//PERMISSION + SEP + DELETE;//"permission:delete";
    public final static String PERM_UPDATE_PERMISSION = PERMISSION + PART_SEP + UPDATE;//PERMISSION + SEP + UPDATE;//"permission:update";
    public final static String PERM_ADD_ROLE = ROLE + PART_SEP + CREATE;//ROLE + SEP + CREATE;//"role:create";
    public final static String PERM_DELETE_ROLE = ROLE + PART_SEP + DELETE;//ROLE + SEP + DELETE;//"role:delete";
    public final static String PERM_UPDATE_ROLE = ROLE + PART_SEP + UPDATE;//ROLE + SEP + UPDATE;//"role:update";
    public final static String PERM_CREATE_APP_ID = APP + PART_SEP + CREATE;//APP + SEP + CREATE;//"app:create";
    public final static String PERM_DELETE_APP_ID = APP + PART_SEP + DELETE;//APP + SEP + DELETE;//"app:delete";
    public final static String PERM_UPDATE_APP_ID = APP + PART_SEP + UPDATE;//APP + SEP + UPDATE;//"app:update";
    public final static String PERM_ADD_SUBJECT = SUBJECT + PART_SEP + CREATE;
    public final static String PERM_DELETE_SUBJECT = SUBJECT + PART_SEP + DELETE;
    public final static String PERM_READ_SUBJECT = SUBJECT + PART_SEP + READ;
    public final static String PERM_UPDATE_SUBJECT = SUBJECT + PART_SEP + UPDATE;
    @Deprecated
    public final static String PERM_SELF = "self";
    @Deprecated
    public final static String PERM_PRIVATE = "private";
    @Deprecated
    public final static String PERM_PUBLIC = "public";
    @Deprecated
    public final static String PERM_STATUS = "status";
    @Deprecated
    public final static String PERM_ADD_RESOURCE = RESOURCE + PART_SEP + CREATE;//"resource:add";
    @Deprecated
    public final static String PERM_DELETE_RESOURCE = RESOURCE + PART_SEP + DELETE;//"resource:delete";
    @Deprecated
    public final static String PERM_READ_RESOURCE = RESOURCE + PART_SEP + READ;//"resource:read";
    @Deprecated
    public final static String PERM_UPDATE_RESOURCE = RESOURCE + PART_SEP + UPDATE;//"resource:update";
    public final static String PERM_RESOURCE_ANY = "any";
    public final static String PERM_ASSIGN_PERMISSION = PERMISSION + PART_SEP + ASSIGN + PART_SEP + PERMISSION;
    public final static String PERM_REMOVE_PERMISSION = PERMISSION + PART_SEP + REMOVE + PART_SEP + PERMISSION;

    //public final static String PERM_CREATE = toSecTok(PERMISSION, CREATE);
    //public final static String PERM_ASSIGN_PERMISSION = "assign:permission";
    public final static String PERM_ASSIGN_ROLE = PERMISSION + PART_SEP + ASSIGN + PART_SEP + ROLE;
    public final static String PERM_REMOVE_ROLE = PERMISSION + PART_SEP + REMOVE + PART_SEP + ROLE;

    public final static String PERM_ACCESS = PERMISSION + PART_SEP + "access";


    // subject resource permission explicit permission "resource:subject_guid:*:subject_guid of the resource"
    @Deprecated
    public static final String PERM_SUBJECT_GUID_RESOURCE_ACCESS = RESOURCE + PART_SEP + Const.SUBJECT_GUID + PART_SEP + WILDCARD + PART_SEP + TOK_SUBJECT_GUID;
    @Deprecated
    public static final String PERM_RESOURCE_ACCESS_VIA_SUBJECT_GUID = RESOURCE + PART_SEP + Const.SUBJECT_GUID + PART_SEP + TOK_CRUD + PART_SEP + TOK_SUBJECT_GUID;

    // resource:R,U,D:resource_guid
    @Deprecated
    public static final String PERM_RESOURCE_ACCESS = RESOURCE + PART_SEP + TOK_CRUD + PART_SEP + TOK_RESOURCE_GUID;


    public static String toSecTok(String... secTokens) {
        StringBuilder sb = new StringBuilder();
        for (String token : secTokens) {
            token = DataEncoder.TrimLowerCase.encode(token);
            if (token != null) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != C_PART_SEP)
                    sb.append(PART_SEP);

                sb.append(token);
            }
        }
        return sb.toString();
    }

    /**
     * Builds and validates an inlined entity permission token, {@code nventity:<verbs>}, from
     * the given verbs. Verbs are normalized by {@link NVEPermissionTokenFilter}: lower-cased,
     * de-duplicated, and limited to {@code read}, {@code update}, {@code share} and {@code delete}.
     *
     * @param verbs one or more verbs
     * @return the normalized token, for example {@code nventity:read,share}
     * @throws NullPointerException     if no verb is given
     * @throws IllegalArgumentException if a verb is not allowed
     */
    public static String toNVEToken(String... verbs) {
        SUS.checkIfNulls("verbs null", (Object) verbs);
        if (verbs.length == 0) {
            throw new IllegalArgumentException("at least one verb is required");
        }
        return NVEPermissionTokenFilter.SINGLETON.validate(NVENTITY + PART_SEP + String.join(SUBPART_SEP, verbs));
    }

    /**
     * Tells whether a catalog permission token can be scoped to one resource instance by
     * appending {@code :<resource guid>}: it must have exactly two non-blank parts,
     * {@code <namespace>:<verbs>}. A token that already carries a third part, an instance
     * or a wildcard, is not scopable.
     *
     * @param token the permission token
     * @return true if the token is {@code <namespace>:<verbs>}
     */
    public static boolean isInstanceScopable(String token) {
        if (SUS.isEmpty(token)) {
            return false;
        }
        String[] parts = token.split(PART_SEP, -1);
        return parts.length == 2 && !SUS.isEmpty(parts[0].trim()) && !SUS.isEmpty(parts[1].trim());
    }

    /**
     * Validates and normalizes the permission token inlined on a {@code PermissionGrant} for
     * subject-to-subject sharing: {@code nventity:<verb>[,<verb>]*} with every verb drawn from
     * {@link #VERBS} ({@code read}, {@code update}, {@code share}, {@code delete}). No other
     * namespace, no {@code create}, no wildcard, and no instance part: the resource comes from
     * the grant's resource map, never from the token. The returned value is lower-cased,
     * trimmed and de-duplicated, and is the form that must be stored and compared.
     */
    public final static class NVEPermissionTokenFilter
            implements ValueFilter<String, String> {
        public static final NVEPermissionTokenFilter SINGLETON = new NVEPermissionTokenFilter();

        /**
         * The verbs an inlined token may carry, in canonical order.
         */
        public static final Set<String> VERBS = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(READ, UPDATE, SHARE, DELETE)));

        private NVEPermissionTokenFilter() {
        }

        /**
         * Validate and normalize the token.
         *
         * @param in value to be validated
         * @return the normalized token {@code nventity:<verbs>}
         * @throws NullPointerException     if in is null or blank
         * @throws IllegalArgumentException if the namespace is not {@code nventity}, a verb is
         *                                  empty or not in {@link #VERBS}, or the token carries an
         *                                  instance part
         */
        @Override
        public String validate(String in) throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNull("value null", in);
            String lower = DataEncoder.StringLower.encode(in);
            String[] parts = lower.split(PART_SEP, -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("token must be " + NVENTITY + PART_SEP + "<verbs>: " + in);
            }
            if (!NVENTITY.equals(parts[0].trim())) {
                throw new IllegalArgumentException("namespace must be " + NVENTITY + ": " + in);
            }
            Set<String> verbs = new LinkedHashSet<>();
            for (String verb : parts[1].split(SUBPART_SEP, -1)) {
                verb = verb.trim();
                if (verb.isEmpty()) {
                    throw new IllegalArgumentException("empty verb in token: " + in);
                }
                if (!VERBS.contains(verb)) {
                    throw new IllegalArgumentException("verb not allowed: " + verb + " in token: " + in);
                }
                verbs.add(verb);
            }
            return NVENTITY + PART_SEP + String.join(SUBPART_SEP, verbs);
        }

        /**
         * Converts the implementing object in its canonical form.
         *
         * @return text identification of the object
         */
        @Override
        public String toCanonicalID() {
            return "NVE_PERMISSION_TOKEN_FILTER";
        }
    }


    /**
     * True when a permission token is the reserved wildcard: the whole token is {@code *}, or its
     * first {@code :} part is {@code *}. Such a token implies every permission under Shiro's
     * {@code WildcardPermission} and is reserved for the bootstrap super-admin; the security
     * manager refuses it anywhere else.
     *
     * @param token the permission token
     * @return true if the token is a wildcard token
     */
    public static boolean isWildcardToken(String token) {
        token = SUS.trimOrNull(token);
        if (token == null) {
            return false;
        }
        if (WILDCARD.equals(token)) {
            return true;
        }
        String[] parts = token.split(PART_SEP, -1);
        return WILDCARD.equals(parts[0].trim());
    }

    /**
     * What a permission acts on: the first part of a permission token.
     */
    public enum Target
            implements GetName {
        SUBJECT(SecurityModel.SUBJECT),
        PRINCIPAL("principal"),
        CREDENTIAL("credential"),
        PERMISSION(SecurityModel.PERMISSION),
        ROLE(SecurityModel.ROLE),
        ROLE_GROUP(SecurityModel.ROLE_GROUP),
        APP(SecurityModel.APP),
        NVENTITY(SecurityModel.NVENTITY),
        ;

        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * What a permission allows: the second part of a permission token.
     */
    public enum Action
            implements GetName {
        CREATE(SecurityModel.CREATE),
        READ(SecurityModel.READ),
        UPDATE(SecurityModel.UPDATE),
        DELETE(SecurityModel.DELETE),
        SHARE(SecurityModel.SHARE),
        ASSIGN(SecurityModel.ASSIGN),
        REMOVE(SecurityModel.REMOVE),
        ALL(WILDCARD),
        ;

        private final String name;

        Action(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * The global permission catalog: one entry per permission the platform checks, each with a
     * fixed token {@code <target>:<action>[:<part>...]}. Instance identifiers are never part of a
     * catalog token; they come from a grant's resource map. {@link #SUPER_ADMIN_ALL} is the one
     * reserved entry whose token is the wildcard {@code *}.
     */
    public enum Permission
            implements PermissionModel {
        SUBJECT_CREATE("subject_create", "Create a subject", Target.SUBJECT, Action.CREATE),
        SUBJECT_READ("subject_read", "Read a subject", Target.SUBJECT, Action.READ),
        SUBJECT_UPDATE("subject_update", "Update a subject, its principals and credentials", Target.SUBJECT, Action.UPDATE),
        SUBJECT_DELETE("subject_delete", "Delete a subject", Target.SUBJECT, Action.DELETE),

        PERMISSION_CREATE("permission_create", "Create a catalog permission", Target.PERMISSION, Action.CREATE),
        PERMISSION_UPDATE("permission_update", "Update a catalog permission", Target.PERMISSION, Action.UPDATE),
        PERMISSION_DELETE("permission_delete", "Delete a catalog permission", Target.PERMISSION, Action.DELETE),
        PERMISSION_ASSIGN("permission_assign", "Grant a permission to a subject", Target.PERMISSION, Action.ASSIGN, SecurityModel.PERMISSION),
        PERMISSION_REMOVE("permission_remove", "Revoke a permission grant", Target.PERMISSION, Action.REMOVE, SecurityModel.PERMISSION),

        ROLE_CREATE("role_create", "Create a role or role group", Target.ROLE, Action.CREATE),
        ROLE_UPDATE("role_update", "Update a role or role group", Target.ROLE, Action.UPDATE),
        ROLE_DELETE("role_delete", "Delete a role or role group", Target.ROLE, Action.DELETE),
        ROLE_ASSIGN("role_assign", "Grant a role or role group to a subject", Target.PERMISSION, Action.ASSIGN, SecurityModel.ROLE),
        ROLE_REMOVE("role_remove", "Revoke a role or role group grant", Target.PERMISSION, Action.REMOVE, SecurityModel.ROLE),

        APP_CREATE("app_create", "Create an app", Target.APP, Action.CREATE),
        APP_UPDATE("app_update", "Update an app", Target.APP, Action.UPDATE),
        APP_DELETE("app_delete", "Delete an app", Target.APP, Action.DELETE),

        NVE_ALL("nve_all", "Every action on every entity", Target.NVENTITY, Action.ALL),
        NVE_CREATE_ALL("nve_create_all", "Create any entity", Target.NVENTITY, Action.CREATE, WILDCARD),
        NVE_READ_ALL("nve_read_all", "Read every entity", Target.NVENTITY, Action.READ, WILDCARD),
        NVE_UPDATE_ALL("nve_update_all", "Update every entity", Target.NVENTITY, Action.UPDATE, WILDCARD),
        NVE_DELETE_ALL("nve_delete_all", "Delete every entity", Target.NVENTITY, Action.DELETE, WILDCARD),
        NVE_SHARE_ALL("nve_share_all", "Share every entity", Target.NVENTITY, Action.SHARE, WILDCARD),

        /**
         * Reserved: implies every permission. Only the bootstrap super-admin subject may hold it.
         */
        SUPER_ADMIN_ALL("super_admin_all", "Reserved: every permission; only the bootstrap super-admin may hold it", WILDCARD),
        ;

        private final String name;
        private final String pattern;
        private final String description;
        private final Target target;
        private final Action action;

        Permission(String name, String description, Target target, Action action, String... parts) {
            this.name = name;
            this.description = description;
            this.target = target;
            this.action = action;
            String[] values = new String[2 + (parts != null ? parts.length : 0)];
            values[0] = target.getName();
            values[1] = action.getName();
            if (parts != null) {
                System.arraycopy(parts, 0, values, 2, parts.length);
            }
            this.pattern = PPEncoder.SINGLETON.encode(values);
        }

        Permission(String name, String description, String literalToken) {
            this.name = name;
            this.description = description;
            this.target = null;
            this.action = null;
            this.pattern = literalToken;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getValue() {
            return pattern;
        }

        public String pattern() {
            return pattern;
        }

        /**
         * @return the target part, null for the reserved wildcard entry
         */
        public Target getTarget() {
            return target;
        }

        /**
         * @return the action part, null for the reserved wildcard entry
         */
        public Action getAction() {
            return action;
        }

        /**
         * @return true for the one reserved wildcard entry
         */
        public boolean isReserved() {
            return this == SUPER_ADMIN_ALL;
        }

        /**
         * @return a new global (app-less) catalog row for this entry
         */
        public PermissionInfo toPermissionInfo() {
            PermissionInfo ret = new PermissionInfo(name, pattern);
            ret.setDescription(description);
            return ret;
        }

        public PermissionInfo toPermission(NVPair... tokens) {
            return SecurityModel.toPermission(getName(), getDescription(), getValue(), tokens);
        }
    }

    /**
     * Built-in roles with their declared permissions. Seeded and kept in sync by the security
     * manager's catalog seeder; {@link #SUPER_ADMIN} is reserved for the bootstrap super-admin.
     * Ownership of an entity is implicit and needs no role.
     */
    public enum Role
            implements GetName, GetDescription {
        SUPER_ADMIN("super_admin", "Super admin role: every permission", Permission.SUPER_ADMIN_ALL),
        DOMAIN_ADMIN("domain_admin", "Domain admin role",
                Permission.SUBJECT_CREATE, Permission.SUBJECT_READ, Permission.SUBJECT_UPDATE, Permission.SUBJECT_DELETE,
                Permission.PERMISSION_CREATE, Permission.PERMISSION_UPDATE, Permission.PERMISSION_DELETE,
                Permission.PERMISSION_ASSIGN, Permission.PERMISSION_REMOVE,
                Permission.ROLE_CREATE, Permission.ROLE_UPDATE, Permission.ROLE_DELETE,
                Permission.ROLE_ASSIGN, Permission.ROLE_REMOVE,
                Permission.APP_CREATE, Permission.APP_UPDATE, Permission.APP_DELETE,
                Permission.NVE_ALL),
        APP_ADMIN("app_admin", "App admin role",
                Permission.SUBJECT_CREATE, Permission.SUBJECT_READ, Permission.SUBJECT_UPDATE,
                Permission.ROLE_ASSIGN, Permission.ROLE_REMOVE,
                Permission.PERMISSION_ASSIGN, Permission.PERMISSION_REMOVE,
                Permission.APP_UPDATE),
        APP_USER("app_user", "App user role"),
        APP_SERVICE_PROVIDER("app_service_provider", "App service provider role", Permission.SUBJECT_READ),
        USER("user", "This role is granted to all users"),
        ;

        private final String name;
        private final String description;
        private final Permission[] permissions;

        Role(String name, String description, Permission... permissions) {
            this.name = name;
            this.description = description;
            this.permissions = permissions != null ? permissions : new Permission[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        /**
         * @return the permissions this role declares (a copy)
         */
        public Permission[] getPermissions() {
            return permissions.clone();
        }

        /**
         * @return true for the reserved super-admin role
         */
        public boolean isReserved() {
            return this == SUPER_ADMIN;
        }

        public static RoleInfo toRole(String name, String description) {
            return new RoleInfo(name, description);
        }

        public static RoleInfo addPermission(RoleInfo role, PermissionInfo permission) {
            role.addPermission(permission);
            return role;
        }
    }

    /**
     * Built-in role groups. {@link Role#SUPER_ADMIN} belongs to no group by design.
     */
    public enum RoleGroup
            implements GetName, GetDescription {
        DOMAIN_ADMINS("domain_admins", "Domain administrators", Role.DOMAIN_ADMIN, Role.APP_ADMIN, Role.USER),
        APP_ADMINS("app_admins", "App administrators", Role.APP_ADMIN, Role.APP_USER, Role.USER),
        APP_USERS("app_users", "App users", Role.APP_USER, Role.USER),
        SERVICE_PROVIDERS("service_providers", "Service providers", Role.APP_SERVICE_PROVIDER, Role.USER),
        ;

        private final String name;
        private final String description;
        private final Role[] roles;

        RoleGroup(String name, String description, Role... roles) {
            this.name = name;
            this.description = description;
            this.roles = roles != null ? roles : new Role[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        /**
         * @return the roles this group declares (a copy)
         */
        public Role[] getRoles() {
            return roles.clone();
        }
    }


//    public static String toSubjectID(String domainID, String appID, GetName gn) {
//        return toSubjectID(domainID, appID, gn.getName());
//    }
//
//
//    public static String toSubjectID(String domainID, String appID, String name) {
//        return SUS.toCanonicalID(AppID.CAN_ID_SEP, domainID, appID, name);
//    }

    /**
     * @deprecated app-scoped catalog seeded only by {@code APIAppManagerProvider.createAppIDDAO},
     * which has no production caller; the {@code $$resource_guid$$} / {@code $$subject_guid$$}
     * placeholders are never substituted. Superseded by {@link Permission} + {@link Role} and the
     * shiro-ds catalog seeder; to be deleted with {@code APIAppManagerProvider}.
     */
    @Deprecated
    public enum AppPermission
            implements PermissionModel {
        ASSIGN_ROLE_APP("assign_role_app", "Assign a role to user", PERM_ADD_ROLE, TOK_APP_ID),
        ORDER_CREATE("order_create", "Create order", "order:create", TOK_APP_ID, PERM_SELF),
        ORDER_DELETE("order_delete", "Delete order", "order:delete", TOK_APP_ID, TOK_RESOURCE_GUID),
        ORDER_UPDATE("order_update", "Update order", "order:update", TOK_APP_ID, TOK_RESOURCE_GUID),
        ORDER_READ_APP("order_read_app", "Read app  order", "order:read", TOK_APP_ID),
        ORDER_READ_USER_APP("order_read_user_app", "Read app  order", "order:read", TOK_APP_ID, TOK_RESOURCE_GUID, TOK_SUBJECT_GUID),
        ORDER_UPDATE_STATUS_APP("order_update_status_app", "Read app  order", "order:update", TOK_APP_ID, TOK_RESOURCE_GUID, PERM_STATUS),
        RESOURCE_ADD("resource_add", "Add resource", PERM_ADD_RESOURCE, TOK_APP_ID),
        RESOURCE_DELETE("resource_delete", "delete resource", PERM_DELETE_RESOURCE, TOK_APP_ID, TOK_RESOURCE_GUID),
        RESOURCE_READ_PRIVATE("resource_read_private", "read private resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_GUID, PERM_PRIVATE),
        RESOURCE_READ_PUBLIC("resource_read_public", "read public resource", PERM_READ_RESOURCE, TOK_APP_ID, TOK_RESOURCE_GUID, PERM_PUBLIC),
        RESOURCE_UPDATE("resource_update", "update resource", PERM_UPDATE_RESOURCE, TOK_APP_ID),
        SELF("self", "self", PERM_SELF),
        ;
        private final String name;
        private final String pattern;
        private final String description;


        AppPermission(String name, String description, String... values) {
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

        public String pattern() {
            return pattern;
        }
    }

    /**
     * The name is the name of the permission and value is the patterm
     * @param gnv
     * @return
     */
    public static PermissionInfo toPermission(GetNameValue<String> gnv, GetNameValue<String>... tokens) {
        return toPermission(gnv.getName(), null, gnv.getValue(), tokens);
    }

    public static PermissionInfo toPermission(String name, String description, String pattern, GetNameValue<String>... tokens) {
        PermissionInfo ret = new PermissionInfo();
        ret.setName(name);
        ret.setDescription(description);
        //ret.setEmbedAppIDEnabled(embedAppID);
        //ret.setDomainAppID(domainID, appID);


        if (tokens != null && tokens.length > 0) {
            for (GetNameValue<String> token : tokens)
                pattern = SharedStringUtil.embedText(pattern, token.getName(), token.getValue());
        }


        ret.setPermissionToken(pattern);
        return ret;

    }

    public static PermissionInfo toPermission(AppIDDefault appIDDefault, String name, String description, String pattern, GetNameValue<String>... tokens) {
        PermissionInfo ret = new PermissionInfo();
        ret.setName(name);
        ret.setDescription(description);
        ret.setAppIdDAO(appIDDefault);
        //ret.setEmbedAppIDEnabled(embedAppID);
        //ret.setDomainAppID(domainID, appID);


        if (tokens != null && tokens.length > 0) {
            for (GetNameValue<String> token : tokens)
                pattern = SharedStringUtil.embedText(pattern, token.getName(), token.getValue());
        }


        ret.setPermissionToken(pattern);
        return ret;

    }

}
