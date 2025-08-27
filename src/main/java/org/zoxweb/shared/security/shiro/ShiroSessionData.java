package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.data.AppIDResource;
import org.zoxweb.shared.util.*;

import java.util.*;

@SuppressWarnings("serial")
public class ShiroSessionData
extends AppIDResource
implements SubjectID<String>
{
  public enum ExtraParam
    implements GetName
  {
    REALM("realm"),
    SESSION_TIMEOUT("session_timeout"),
    SESSION_ID("session_id"),

    ;
    private final String name;
    ExtraParam(String name)
    {
      this.name = name;
    }

    public String getName()
    {
      return name;
    }
  }


  public enum Param
  implements GetNVConfig, GetName
  {
    SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject ID", "SubjectID", false, true, String.class)),
    ROLES(NVConfigManager.createNVConfig("roles", "Subject roles", "Roles", false, true, NVStringList.class)),
    PERMISSIONS(NVConfigManager.createNVConfig("permissions", "Subject permissions", "Permissions", false, true, NVStringList.class)),
 
    ;   
  
    private final NVConfig cType;
    
    Param(NVConfig c) {
      cType = c;
    }
    
    public NVConfig getNVConfig() {
      return cType;
    }

    @Override
    public String getName() {
      // TODO Auto-generated method stub
      return cType.getName();
    }
  
  }
  
  public static final NVConfigEntity NVC_SESSION_DATA = new NVConfigEntityPortable("shiro_session_data", null , "ShiroSubjectData", true, false, false, false, ShiroSessionData.class, SharedUtil.extractNVConfigs(Param.SUBJECT_ID, Param.ROLES, Param.PERMISSIONS), null, false, AppIDResource.NVC_APP_ID_RESOURCE);
  public ShiroSessionData() {
    super(NVC_SESSION_DATA);
    // TODO Auto-generated constructor stubs
  }

  public List<String> getPermissions()
  {
    return lookupValue((GetName)Param.PERMISSIONS);
  }

  public List<String> getRoles()
  {
    return lookupValue((GetName)Param.ROLES);
  }



  @Override
  public String getSubjectID() {
    // TODO Auto-generated method stub
    return lookupValue((GetName)Param.SUBJECT_ID);
  }

  @Override
  public void setSubjectID(String id) {
    // TODO Auto-generated method stub
    setValue(Param.SUBJECT_ID, id);
  }


  public Set<String> permissions()
  {
    return Collections.unmodifiableSet(new HashSet<String>(lookupValue((GetName)Param.PERMISSIONS)));
  }

  public Set<String> roles()
  {
    return Collections.unmodifiableSet(new HashSet<String>(lookupValue((GetName)Param.ROLES)));
  }


  public synchronized void setPermissions(Collection<String> permissionsToSet)
  {
    List<String> permissions = lookupValue(Param.PERMISSIONS.getName());
    permissions.clear();
    if(permissionsToSet != null)
      permissions.addAll(permissionsToSet);
  }

  public synchronized ShiroSessionData addPermissions(String ...permissionsToAdd)
  {
    List<String> permissions = lookupValue(Param.PERMISSIONS.getName());
    for (String permission : permissionsToAdd)
    {
      permissions.add(permission);
    }
    return this;
  }


  public synchronized ShiroSessionData addRoles(String ...rolesToAdd)
  {
    List<String> roles = lookupValue(Param.ROLES.getName());
    for (String role : rolesToAdd)
    {
      roles.add(role);
    }
    return this;
  }

  public synchronized void setRoles(Collection<String> rolesToSet)
  {
    List<String> roles = lookupValue(Param.ROLES.getName());
    roles.clear();
    if(rolesToSet != null)
      roles.addAll(rolesToSet);
  }


  public String realm()
  {
    return getProperties().lookupValue(ExtraParam.REALM);
  }

  public long sessionTimeout()
  {
    return getProperties().lookupValue(ExtraParam.SESSION_TIMEOUT);
  }

  public String sessionID(){ return getProperties().lookupValue(ExtraParam.SESSION_ID);}

}
