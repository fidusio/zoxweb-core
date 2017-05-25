package org.zoxweb.server.shiro;

import org.apache.shiro.subject.Subject;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.NVBase;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;

public interface SecurityManagerAPI 
{
	
	Object encryptValue(APIDataStore<?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte msKey[])
			throws NullPointerException, IllegalArgumentException, AccessException;
	
	Object decryptValue(APIDataStore<?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte msKey[])
			throws NullPointerException, IllegalArgumentException, AccessException;
	
	 String decryptValue(APIDataStore<?> dataStore, NVEntity container, NVPair nvp, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 Object decryptValue(String userID, APIDataStore<?> dataStore, NVEntity container, Object value, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 NVEntity decryptValues(APIDataStore<?> dataStore, NVEntity container, byte msKey[])
				throws NullPointerException, IllegalArgumentException, AccessException;
	 
	 void associateNVEntityToSubjectUserID(NVEntity nve, String userID);
	 
	 String getCurrentPrincipal();
	 
	 String getCurrentUserID();
	 
	 Subject getDaemonSubject();
	 
	 boolean isNVEntityAccessible(NVEntity nve, CRUD ...permissions);
	 
	 boolean isNVEntityAccessible(LogicalOperator lo, NVEntity nve, CRUD ...permissions);
	 
	 boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD ...permissions);
	 
}