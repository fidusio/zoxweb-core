package org.zoxweb.shared.data;


import java.util.List;

import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.security.model.SecurityModel.Role;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEnumList;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SubjectID;

@SuppressWarnings("serial")
public class AppAccessMode
	extends SetNameDescriptionDAO
	implements SubjectID<String>
{
	
	 public enum Param
     	implements GetNVConfig
	 {
	     SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject ID", "Subject ID", true, false, true, String.class, null)),
	     ROLES(NVConfigManager.createNVConfig("roles", "Roles", "Roles", true, false, SecurityModel.Role[].class))
	
	     ;
	
	     private final NVConfig nvc;
	
	     Param(NVConfig nvc) {
	         this.nvc = nvc;
	     }
	
	     @Override
	     public NVConfig getNVConfig() {
	         return nvc;
	     }
	 }
	
	 public static final NVConfigEntity NVC_APP_ACCESS_MODE = new NVConfigEntityPortable(
	         "app_access_mode",
	         null,
	         AppAccessMode.class.getSimpleName(),
	         true,
	         false,
	         false,
	         false,
	         AppAccessMode.class,
	         SharedUtil.extractNVConfigs(Param.values()),
	         null,
	         false,
	         SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
	 );


	public AppAccessMode() 
	{
		super(NVC_APP_ACCESS_MODE);
	}

	/**
	 * Return the subject id, if it is an acutal user will return the email address, if a system or device return the subject id
	 */
	public String getSubjectID()
    {
		return lookupValue(Param.SUBJECT_ID);
	}

	@Override
	public void setSubjectID(String id)
    {
		setValue(Param.SUBJECT_ID, id);
	}
	
	public NVEnumList getRolesAsList()
	{
		return (NVEnumList) lookup(Param.ROLES);
	}
	
	public SecurityModel.Role[] getRoles()
	{
		List<Enum<?>> val = lookupValue(Param.ROLES);
		SecurityModel.Role[] ret = new SecurityModel.Role[val.size()];
		for (int i = 0; i <ret.length; i++)
		{
			ret[i] = (Role) val.get(i);
		}
		
		return ret;
	}
	
}
