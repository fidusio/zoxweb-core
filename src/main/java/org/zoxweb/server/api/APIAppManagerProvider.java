package org.zoxweb.server.api;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.security.*;
import org.zoxweb.server.util.IDGeneratorUtil;
import org.zoxweb.shared.api.APIAppManager;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.api.APISecurityManager;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.CryptoConst.HashType;
import org.zoxweb.shared.crypto.EncryptedKey;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.data.*;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.db.QueryMatch;
import org.zoxweb.shared.db.QueryMatchString;
import org.zoxweb.shared.filters.AppIDNameFilter;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.security.model.PPEncoder;
import org.zoxweb.shared.security.model.PermissionModel;
import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.security.model.SecurityModel.AppPermission;
import org.zoxweb.shared.security.model.SecurityModel.Role;
import org.zoxweb.shared.security.shiro.ShiroAssociationRule;
import org.zoxweb.shared.security.shiro.ShiroAssociationType;
import org.zoxweb.shared.security.shiro.ShiroPermission;
import org.zoxweb.shared.security.shiro.ShiroRole;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.Const.RelationalOperator;
import org.zoxweb.shared.util.Const.Status;
import org.zoxweb.shared.util.ExceptionReason.Reason;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class APIAppManagerProvider
    implements APIAppManager {

    private volatile APIDataStore<?,? > dataStore;
    private volatile APISecurityManager<?, ?, ?> apiSecurityManager;

    //private HashMap<String, SubjectAPIKey> cache = new HashMap<String, SubjectAPIKey>();
	public static final LogWrapper log = new LogWrapper(APIAppManagerProvider.class);
	private static final NVConfigEntity[] USER_NVCs =
		{
			UserIDCredentialsDAO.NVC_USER_ID_CREDENTIALS_DAO,
			SubjectPreference.NVC_SUBJECT_PREFERENCE,
			AppDeviceDAO.NVC_APP_DEVICE_DAO,
			EncryptedKey.NVCE_ENCRYPTED_KEY,
			ShiroAssociationRule.NVC_SHIRO_ASSOCIATION_RULE,
			ShiroPermission.NVC_SHIRO_PERMISSION,
			ShiroRole.NVC_SHIRO_ROLE,
			AddressDAO.NVC_ADDRESS_DAO,
			CreditCardDAO.NVC_CREDIT_CARD_DAO,
			DeviceDAO.NVC_DEVICE_DAO,
			PhoneDAO.NVC_PHONE_DAO,
		};
    


    public APIAppManagerProvider() {

    }

    
  


	public APISecurityManager<?, ?, ?> getAPISecurityManager() {
		return apiSecurityManager;
	}


	public void setAPISecurityManager(APISecurityManager<?, ?, ?> apiSecurityManager) {
		this.apiSecurityManager = apiSecurityManager;
	}


	public APIDataStore<?, ?> getAPIDataStore() {
        return dataStore;
    }

 
    public synchronized void setAPIDataStore(APIDataStore<?, ?> dataStore)
            throws NullPointerException, IllegalArgumentException {
        this.dataStore = dataStore;
    }

    
    public SubjectAPIKey createAppDeviceDAO(AppDeviceDAO appDeviceDAO)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException{
        SUS.checkIfNulls("AppDeviceDAO is null", appDeviceDAO);

        if (appDeviceDAO.getAppID() == null || appDeviceDAO.getDomainID() == null)
        {
            throw new IllegalArgumentException("AppID or DomainID null");
        }

        if (appDeviceDAO.getDevice() == null) 
        {
            throw new IllegalArgumentException("Device null");
        }

        return createSubjectAPIKey(appDeviceDAO);
    }

    public SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey)
    		throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {
    	return createSubjectAPIKey(subjectAPIKey, Status.ACTIVE, 0);
    }
    public SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey, Status status, long ttl)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {
        SUS.checkIfNulls("Null SubjectAPIKey", subjectAPIKey);

        if (subjectAPIKey.getSubjectID() == null) {
            subjectAPIKey.setSubjectID(IDGeneratorUtil.SHA256Base64.generateID());
        }

        if (subjectAPIKey.getAPIKey() == null) {
            try {
                subjectAPIKey.setAPIKey(CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, 256).getEncoded());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        //if (subjectAPIKey.getStatus() == null) {
            subjectAPIKey.setStatus(status);
            subjectAPIKey.setTimeStampRquired(true);
        //}
        if (ttl > 0 )
        	subjectAPIKey.setExpiryDate(System.currentTimeMillis() + ttl);
        
        
        if (subjectAPIKey instanceof AppDeviceDAO)
        {
        	AppDeviceDAO temp = (AppDeviceDAO) subjectAPIKey;
        	DeviceDAO device = lookupDeviceDAO(temp.getDevice().getSubjectID());
        	if (device != null)
        	{
        		temp.getDevice().setReferenceID(device.getReferenceID());
        		temp.getDevice().setSubjectGUID(getAPISecurityManager().currentUserID());
        		temp.getDevice().setGUID(device.getGUID());
        	}

        	AppIDDAO appIDDAO = lookupAppIDDAO(temp.getDomainID(), temp.getAppID());

        	if (appIDDAO != null) {
        	    temp.setSubjectGUID(appIDDAO.getGUID());
            }
        	else 
        	{
        		throw new APIException("APP " + new AppIDDAO(temp.getDomainID(), temp.getAppID()).toCanonicalID() + " do not exists" );
        	}
        	
        	
        	ShiroAssociationRule sard = new ShiroAssociationRule();
     		sard.setAssociatedTo(getAPISecurityManager().currentUserID());
     		sard.setAssociate(SecurityModel.toSubjectID(temp.getDomainID(), temp.getAppID(), Role.APP_USER));
     		sard.setAssociationType(ShiroAssociationType.ROLE_TO_SUBJECT);
     		sard.setName("AppUserRule");
     		///sard.setExpiration(null);
     		sard.setAssociationStatus(Status.ACTIVE);
             
             getAPISecurityManager().addShiroRule(sard);
        }
        
        
       

        subjectAPIKey = getAPIDataStore().insert(subjectAPIKey);

        

        return subjectAPIKey;
    }
    
    
    public DeviceDAO lookupDeviceDAO(String deviceID)
    {
    	 SUS.checkIfNulls("Null SubjectAPIKey", deviceID);
    	 List<DeviceDAO> ret = getAPIDataStore().search(DeviceDAO.NVC_DEVICE_DAO, null, new QueryMatchString(RelationalOperator.EQUAL, deviceID, DeviceDAO.Param.SUBJECT_ID));
    	 
    	 if(ret != null && ret.size() == 1)
    	 {
    		 return ret.get(0);
    	 }
    	 
    	 
    	 return null;
    }

    
    public UserIDDAO lookupUserIDDAO(String subjectID, String ...params)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
    	return lookupUserID(getAPIDataStore(), subjectID, params);
	}
    
    
    public static UserIDDAO lookupUserID(APIDataStore<?, ?> apiDataStore, String subjectID, String ...params)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
		SUS.checkIfNulls("subjectID null", apiDataStore, subjectID);
		QueryMatch<?> query;
		if (FilterType.EMAIL.isValid(subjectID))
		{
			// if we have an email
			query = new QueryMatch<String>(RelationalOperator.EQUAL, subjectID, UserIDDAO.Param.PRIMARY_EMAIL.getNVConfig());
		}
		else
		{
			query = new QueryMatch<String>(RelationalOperator.EQUAL, subjectID, MetaToken.REFERENCE_ID);//"_id", new BasicDBObject("$in", listOfObjectID)
		}
	
		ArrayList<String> listParams = null;
		if (params != null && params.length > 0)
		{
			listParams = new ArrayList<String>();
			for (String str : params)
			{
				if (!SUS.isEmpty(str))
				{
					listParams.add(str);
				}
			}
		}
		
		List<UserIDDAO> listOfUserIDDAO = apiDataStore.search(UserIDDAO.NVC_USER_ID_DAO, listParams, query);
		
		if (listOfUserIDDAO == null || listOfUserIDDAO.size() != 1)
		{
			return null;
		}
		
		return listOfUserIDDAO.get(0);

	}
    
	
	public synchronized UserIDDAO lookupUserIDDAO(GetValue<String> subjectID, String ...params)
			throws NullPointerException, IllegalArgumentException, AccessException
	{
		SUS.checkIfNulls("DB or user ID null", subjectID);
		return lookupUserIDDAO(subjectID.getValue(), params);
	}
	
	public synchronized UserIDDAO createUserIDDAO(UserIDDAO userID, CryptoConst.SubjectStatus userIDStatus, String password)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
		SUS.checkIfNulls("UserIDDAO object is null.", userID, userIDStatus);

		// validate the password
		password = FilterType.PASSWORD.validate(password);

		/// check if the user exists
		if (lookupUserIDDAO(userID.getSubjectID()) != null)
		{
			throw new APIException("User already exist");
		}

		if (log.isEnabled()) {
			log.getLogger().info("SubjectID: " + userID.getSubjectID());
			log.getLogger().info("First Name: " + userID.getUserInfo().getFirstName());
			log.getLogger().info("Middle Name: " + userID.getUserInfo().getMiddleName());
			log.getLogger().info("Last Name: " + userID.getUserInfo().getLastName());
			log.getLogger().info("Birthday: " + userID.getUserInfo().getDOB());
		}
		
		userID.setReferenceID(null);
		SharedUtil.validate(userID, true, true);

		// special case to avoid chicken and egg situation
		String userIDRef = getAPIDataStore().getIDGenerator().generateID();
		String globalID = IDGeneratorUtil.UUIDV4.generateID();
		getAPISecurityManager().associateNVEntityToSubjectUserID(userID, userIDRef);
		userID.setReferenceID(userIDRef);
		userID.setSubjectGUID(userIDRef);
		userID.getUserInfo().setReferenceID(userIDRef);
		userID.setGUID(globalID);

		////////////////////////
		
		try
		{
			// insert the user_info dao first
			userID.getUserInfo().setGUID(globalID);
			getAPIDataStore().insert(userID.getUserInfo());
			
			getAPIDataStore().insert(userID);
			
			UserIDCredentialsDAO userIDCredentials = new UserIDCredentialsDAO();
			userIDCredentials.setReferenceID(userID.getReferenceID());
			userIDCredentials.setGUID(globalID);
			userIDCredentials.setSubjectGUID(userID.getReferenceID());
			userIDCredentials.setLastStatusUpdateTimestamp(System.currentTimeMillis());
			userIDCredentials.setUserStatus(userIDStatus);
			userIDCredentials.setCanonicalID(userID.getSubjectID());
			
			CIPassword passwordDAO = HashUtil.toPassword(HashType.BCRYPT, 0, 10, password);
			passwordDAO.setSubjectGUID(userID.getReferenceID());
			passwordDAO.setReferenceID(userID.getReferenceID());
			passwordDAO.setGUID(userID.getGUID());
			userIDCredentials.setPassword(passwordDAO);
			
			
			
			switch(userIDStatus)
			{
			case ACTIVE:
			case DEACTIVATED:
			case INACTIVE:
				break;
			case PENDING_ACCOUNT_ACTIVATION:
			case PENDING_RESET_PASSWORD:
				userIDCredentials.setPendingToken(UUID.randomUUID().toString());
				break;
			}

			getAPIDataStore().insert(userIDCredentials);

			// create the user master key
			// TODO REMOVE
			//getAPIDataStore().insert(KeyMakerProvider.SINGLETON.createSubjectIDKey(userID, KeyMakerProvider.SINGLETON.getMasterKey()));

		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new AccessException(e.getMessage());			
		}
		
		return userID;
	}
	
	
	public synchronized void deleteUser(String subjectID)
		throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
		
		// crutial permission check
		// of the super admin can delete user
		getAPISecurityManager().checkPermissions(SecurityModel.Permission.USER_DELETE.getValue());
		
		SUS.checkIfNulls("subjectID null", subjectID);
		UserIDDAO userID = lookupUserIDDAO(subjectID);
		if (userID == null)
		{
			throw new APIException("subjectID " + subjectID + " not found.");
		}
		// delete a user requires the following
		// delete the associated UserInfoDOA, UserIDCredentialsDAO and the encrypted key dao associated with the user id
		getAPISecurityManager().invalidateResource(subjectID);
		getAPIDataStore().delete(userID, true);
		for(NVConfigEntity nvce : USER_NVCs)
		{
			getAPIDataStore().delete(nvce, new QueryMatch<String>(RelationalOperator.EQUAL, userID.getReferenceID(), MetaToken.SUBJECT_GUID));
		}
//		getAPIDataStore().delete(UserIDCredentialsDAO.NVC_SUBJECT_GUID_CREDENTIALS_DAO,  new QueryMatch<String>(RelationalOperator.EQUAL, userID.getReferenceID(), MetaToken.REFERENCE_ID));
//		getAPIDataStore().delete(AppDeviceDAO.NVC_APP_DEVICE_DAO, new QueryMatch<String>(RelationalOperator.EQUAL, userID.getReferenceID(), MetaToken.SUBJECT_GUID));
//		getAPIDataStore().delete(EncryptedKeyDAO.NVCE_ENCRYPTED_KEY_DAO,  new QueryMatch<String>(RelationalOperator.EQUAL, userID.getReferenceID(), MetaToken.SUBJECT_GUID));

		
		// TODO check if a user is logged in and invalidate his current session
		
		
	}
    
    
  
//    public void deleteSubjectAPIKey(String subjectID)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
//
//    	SubjectAPIKey sak = lookupSubjectAPIKey(subjectID, true);
//
//    	deleteSubjectAPIKey(sak);
//
//    }

   
    public synchronized void deleteSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {
    	if (subjectAPIKey != null) {
			getAPISecurityManager().invalidateResource(subjectAPIKey.getSubjectID());
			if (log.isEnabled()) log.getLogger().info("" + subjectAPIKey.getClass().getName());
			delete(subjectAPIKey, subjectAPIKey instanceof AppDeviceDAO);
		}
    }

    
//    @SuppressWarnings("unchecked")
//	public 	<V extends SubjectAPIKey> V  lookupSubjectAPIKey(String subjectID, boolean throwExceptionIfNotFound)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
//    	List<SubjectAPIKey> result = getAPIDataStore().search(AppDeviceDAO.NVC_APP_DEVICE_DAO,
//    			null,
//    			new QueryMatchString(RelationalOperator.EQUAL, subjectID, SubjectAPIKey.Param.SUBJECT_ID));
//
//    	if (result == null || result.size() == 0)
//    	{
//    		result = getAPIDataStore().search(SubjectAPIKey.NVC_SUBJECT_API_KEY,
//        			null,
//        			new QueryMatchString(RelationalOperator.EQUAL, subjectID, SubjectAPIKey.Param.SUBJECT_ID));
//
//    	}
//
//    	if (result == null || result.size() != 1)
//    	{
//    		if (throwExceptionIfNotFound)
//    			throw new APIException("Subject not found " + subjectID);
//    		else return null;
//
//    	}
//
//    	return (V)result.get(0);
//    }
    
   

  
    public void updateSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        

    }

   
    public JWT validateJWT(String token)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException{
		throw new IllegalArgumentException("Not implemented yet");
//        SUS.checkIfNulls("Null Token", token);
//        JWT jwt;
//        try {
//            jwt = CryptoUtil.parseJWT(token);
//        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//            e.printStackTrace();
//            throw new AccessSecurityException("Invalid token");
//        }
//
//        if (jwt.getPayload().getSubjectID() == null) {
//            throw new AccessSecurityException("Missing subject id");
//        }
//
//        // lookup subject id
//        SubjectAPIKey subjectAPIKey = lookupSubjectAPIKey(jwt.getPayload().getSubjectID(), true);
//
//        if (subjectAPIKey == null) {
//            throw new AccessSecurityException("Subject not found: " + jwt.getPayload().getSubjectID());
//        }
//
//        if (subjectAPIKey.getStatus() != Status.ACTIVE) {
//            throw new AccessSecurityException("Invalid SubjectAPIKey: " + subjectAPIKey.getStatus());
//        }
//
//        if (subjectAPIKey instanceof AppDeviceDAO) {
//            // validate domainID and AppID
//            AppDeviceDAO add = (AppDeviceDAO) subjectAPIKey;
//            if (!SharedStringUtil.equals(add.getDomainID(), jwt.getPayload().getDomainID(), true) ||
//                    !SharedStringUtil.equals(add.getAppID(), jwt.getPayload().getAppID(), true)) {
//                throw new AccessSecurityException("Invalid AppID");
//            }
//        }
//
//        return JWTProvider.SINGLETON.decode(subjectAPIKey.getAPIKeyAsBytes(), token);
    }

   
    public void resetPassword(String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
    	// lookup user id 
    	// generate token 
    	// send email 
    	
    	getAPISecurityManager().invalidateResource(subjectID);
    }

  
    public UserIDDAO createUserIDDAO(String subjectID, CryptoConst.SubjectStatus userIDstatus, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {

    	UserIDDAO uid = new UserIDDAO();
    	uid.setPrimaryEmail(subjectID);
        return createUserIDDAO(uid, userIDstatus, password);
    }


//    @Override
//    public UserIDDAO createUserIDDAO(UserIDDAO userIDDAO, String password)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
//        SUS.checkIfNulls("UserIDDAO is null.", userIDDAO);
//        SUS.checkIfNulls("Password is null.", password);
//
//        password = FilterType.PASSWORD.validate(password);
//
//        try {
//            userIDDAO = dataStore.insert(userIDDAO);
//
//            UserIDCredentialsDAO userIDCredentials = new UserIDCredentialsDAO();
//            userIDCredentials.setReferenceID(userIDDAO.getReferenceID());
//            userIDCredentials.setUserID(userIDDAO.getReferenceID());
//            userIDCredentials.setLastStatusUpdateTimestamp(System.currentTimeMillis());
//            userIDCredentials.setUserStatus(UserIDCredentialsDAO.UserStatus.ACTIVE);
//            PasswordDAO passwordDAO = CryptoUtil.hashedPassword(CryptoConst.MDType.SHA_512, 0, 8196, password);
//            passwordDAO.setUserID(userIDDAO.getReferenceID());
//            userIDCredentials.setPassword(passwordDAO);
//            dataStore.insert(userIDCredentials);
//            userIDCredentials.getPassword().setReferenceID(userIDCredentials.getReferenceID());
//            dataStore.update(userIDCredentials);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new AccessException(e.getMessage());
//        }
//
//        return userIDDAO;
//    }

//
//    public UserIDDAO lookupUserIDDAO(String subjectID)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException 
//    {
//    	subjectID = FilterType.EMAIL.validate(subjectID);
//    	
//
//        UserIDDAO ret = null;
//
//        List<UserIDDAO> result = getAPIDataStore().search(UserIDDAO.NVC_SUBJECT_GUID_DAO, null,
//                new QueryMatchString(RelationalOperator.EQUAL, subjectID, UserIDDAO.Param.PRIMARY_EMAIL)
//        );
//
//        if (result != null && !result.isEmpty()) {
//            ret = result.get(0);
//        }
//
//        return ret;
//    	
//    }

  
    public SubjectPreference lookupUserPreferenceDAO(AppIDDAO appIDDAO, String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
    	UserIDDAO userIDDAO = lookupUserIDDAO(subjectID);

        return lookupUserPreferenceDAO(appIDDAO, userIDDAO);
    }

  
    public SubjectPreference lookupUserPreferenceDAO(AppIDDAO appIDDAO, UserIDDAO userIDDAO)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        SUS.checkIfNulls("AppIDDAO is null", appIDDAO);
        SUS.checkIfNulls("UserIDDAO is null", userIDDAO);

        SubjectPreference ret = null;

        List<SubjectPreference> result = getAPIDataStore().search(SubjectPreference.NVC_SUBJECT_PREFERENCE, null,
                new QueryMatchString(RelationalOperator.EQUAL, userIDDAO.getReferenceID(), MetaToken.SUBJECT_GUID),
                LogicalOperator.AND,
                new QueryMatchString(RelationalOperator.EQUAL, appIDDAO.getGUID(), SubjectPreference.Param.APP_GUID.getNVConfig())
        );

        if (result != null && !result.isEmpty()) {
            ret = result.get(0);
        }

        return ret;
    }

 
    public void changePassword(String oldPassword, String newPassword)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
    	newPassword = FilterType.PASSWORD.validate(newPassword);
    	String subjectID = getAPISecurityManager().currentSubjectID();
    	// make the user is logged in
    	String userID = getAPISecurityManager().currentUserID();
    	
    	List<UserIDCredentialsDAO>  ret= getAPIDataStore().search(UserIDCredentialsDAO.NVC_USER_ID_CREDENTIALS_DAO, null, new QueryMatchString(RelationalOperator.EQUAL, userID, UserIDCredentialsDAO.NVC_REFERENCE_ID));
    	if (ret == null || ret.size() != 1)
    	{
    		throw new AccessException("User not found");
    	}
    	
    	
    	UserIDCredentialsDAO credentials = ret.get(0);
    	// validate the old password
    	SecUtil.SINGLETON.validatePassword(credentials.getPassword(), oldPassword);
    	
    	try 
    	{
			CIPassword newPasswordDAO = HashUtil.toPassword(HashType.SHA_512, 0, 8196, newPassword);
            newPasswordDAO.setSubjectGUID(credentials.getReferenceID());
            newPasswordDAO.setReferenceID(credentials.getReferenceID());
			credentials.setPassword(newPasswordDAO);
			getAPIDataStore().update(credentials);
			
		} catch (NoSuchAlgorithmException e) {
			throw new AccessException("Invalid new Password");
		}
    	getAPISecurityManager().invalidateResource(subjectID);
    	
    }

  
    public <V extends NVEntity> V create(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        return getAPIDataStore().insert(nve);
    }

    
    public <V extends NVEntity> List<V> lookup(String subjectID, Class<V> classType)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        return null;
    }

   
    public <V extends NVEntity> V update(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        return getAPIDataStore().update(nve);
    }

   
    public <V extends NVEntity> boolean delete(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException 
    {
    	return delete(nve, false);
        
    }

	public <V extends NVEntity> boolean delete(V nve, boolean withReference)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
		return getAPIDataStore().delete(nve, withReference);

	}

   
    public AppIDDAO lookupAppIDDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        return lookupAppIDDAO(domainID, appID, true);
    }
    
    
    public AppIDDAO lookupAppIDDAO(String domainID, String appID, boolean exceptionIfNotFound)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
//        SUS.checkIfNulls("Domain ID is null", domainID);
//        SUS.checkIfNulls("App ID is null", appID);
        domainID = FilterType.DOMAIN.validate(domainID);
        appID =  AppIDNameFilter.SINGLETON.validate(appID);
        

        List<AppIDDAO> result = getAPIDataStore().search(AppIDDAO.NVC_APP_ID_DAO, null,
                new QueryMatchString(RelationalOperator.EQUAL, domainID, AppIDDAO.Param.DOMAIN_ID),
                LogicalOperator.AND,
                new QueryMatchString(RelationalOperator.EQUAL, appID, AppIDDAO.Param.APP_ID));
        

        if (result == null || result.isEmpty()) 
        {
        	if (exceptionIfNotFound)
        		throw new APIException("AppID " + new AppIDDAO(domainID, appID) + " not found", Reason.NOT_FOUND);
        	else
        		return null;
        }

        return result.get(0);
    }

    public AppConfigDAO lookupAppConfigDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {
        domainID = FilterType.DOMAIN.validate(domainID);
        appID =  AppIDNameFilter.SINGLETON.validate(appID);


        AppIDDAO appIDDAO = lookupAppIDDAO(domainID, appID);

        List<AppConfigDAO> result = search(AppConfigDAO.NVC_APP_CONFIG_DAO, new QueryMatchString(Const.RelationalOperator.EQUAL, appIDDAO.getReferenceID(), AppConfigDAO.Param.APP_ID.getNVConfig().getName(), MetaToken.REFERENCE_ID.getName()));

        if (result == null || result.size() != 1) {
            throw new APIException("AppConfigDAO not found", Reason.NOT_FOUND);
        }

        return result.get(0);
    }

    public synchronized SubjectAPIKey registerSubjectAPIKey(UserInfoDAO userInfoDAO, AppDeviceDAO appDeviceDAO, String subjectID, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException {

        // Procedure
        // 1. Validation (Check null, validate password, etc.)
        // 2. Lookup AppIDDAO based on domain ID and app ID (if not found, throw error)
        // 3. Lookup UserIDDAO based on username (if found, throw already found error)
        // 4. Create UserIDDAO with username and UserInfoDAO
        // 5. Create Credentials with password
        // 6. Create AppDeviceDAO
        // 7. Create UserPreferenceDAO
    	
    	

        SUS.checkIfNulls("UserInfoDAO is null", userInfoDAO);
        SUS.checkIfNulls("AppDeviceDAO is null", appDeviceDAO);
        SUS.checkIfNulls("AppIDDAO is null", appDeviceDAO.getSubjectGUID());
        if (SUS.isEmpty(subjectID) || SUS.isEmpty(password)) {
            throw new NullPointerException("Username and/or password is null");
        }
        
        String domainID = appDeviceDAO.getDomainID();
        String appID = appDeviceDAO.getAppID();

        // check and confirm that app already exist
        AppIDDAO appIDDAO = lookupAppIDDAO(domainID, appID);
        

       

       
     
        appDeviceDAO.setSubjectGUID(appIDDAO.getGUID());

        UserIDDAO userIDDAO = lookupUserIDDAO(subjectID);
        if (userIDDAO == null)
        {
        	userIDDAO = new UserIDDAO();
        	// 1.
			userIDDAO.setUserInfo(userInfoDAO);
			// 2.
        	userIDDAO.setSubjectID(subjectID);


        	userIDDAO = createUserIDDAO(userIDDAO, CryptoConst.SubjectStatus.ACTIVE, password);
        }

        getAPISecurityManager().login(subjectID, password, appIDDAO.getDomainID(), appIDDAO.getAppID(), false);
        
      
        
        // Lookup UserPreferenceDAO based on AppIDDAO and UserIDDAO
        SubjectPreference subjectPreference = lookupUserPreferenceDAO(appIDDAO, userIDDAO);
        if (subjectPreference == null) {
            // Does not exist, create UserPreferenceDAO
            subjectPreference = new SubjectPreference();
            subjectPreference.setSubjectGUID(userIDDAO.getReferenceID());
            subjectPreference.setSubjectGUID(appIDDAO.getGUID());

            getAPIDataStore().insert(subjectPreference);
        }

        // Create AppDeviceDAO
        appDeviceDAO.setSubjectGUID(userIDDAO.getReferenceID());
        appDeviceDAO = (AppDeviceDAO) createAppDeviceDAO(appDeviceDAO);

        return appDeviceDAO;
    }
    
    public synchronized UserInfoDAO registerSubject(String subjectID, String password)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException 
    {
		// TODO Auto-generated method stub
    	
         if (SUS.isEmpty(subjectID) || SUS.isEmpty(password)) {
             throw new NullPointerException("Username and/or password is null");
         }
         
         UserIDDAO userIDDAO = lookupUserIDDAO(subjectID);
         if (userIDDAO == null)
         {
         	userIDDAO = new UserIDDAO();
			 // 1.
			 userIDDAO.setUserInfo(new UserInfoDAO());
			 // 2.
			 userIDDAO.setSubjectID(subjectID);

         	userIDDAO = createUserIDDAO(userIDDAO, CryptoConst.SubjectStatus.ACTIVE, password);
         	return userIDDAO.getUserInfo();
         }
         
         throw new AccessException("Access Denied");
	}
    
    
    
    
    
    
    
    
    public synchronized AppIDDAO createAppIDDAO(String domainID, String appID)
    		throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {
    	
    	getAPISecurityManager().checkPermissions(SecurityModel.Permission.APP_ID_CREATE.getValue());
    	// permission super admin only
    	AppIDDAO ret = lookupAppIDDAO(domainID, appID, false);
    	if (ret == null)
    	{
    		ret = new AppIDDAO(domainID, appID);
 
    		ret = getAPIDataStore().insert(ret);
    		NVPair appIDNVP = new NVPair(SecurityModel.TOK_APP_ID, ret.toCanonicalID());
    		
    		NVGenericMap permissions = new NVGenericMap();
    		for (AppPermission ap : AppPermission.values())
    		{
    			ShiroPermission permission = SecurityModel.toPermission(domainID, appID, ap, appIDNVP);
    			apiSecurityManager.addPermission(permission);
    			permissions.add(permission);
    		}
    		
    		
    		
    		ShiroRole appAdminRole = SecurityModel.Role.APP_ADMIN.toRole(domainID, appID);
    		PermissionModel[] adminPermissions = {
    				AppPermission.ASSIGN_ROLE_APP,
    				AppPermission.ORDER_DELETE,
    				AppPermission.ORDER_UPDATE,
    				AppPermission.ORDER_READ_APP,
    				AppPermission.ORDER_UPDATE_STATUS_APP,
    				AppPermission.RESOURCE_ADD,
    				AppPermission.RESOURCE_DELETE,
    				AppPermission.RESOURCE_READ_PRIVATE,
    				AppPermission.RESOURCE_READ_PUBLIC,
    				AppPermission.RESOURCE_UPDATE,
    				AppPermission.SELF
    				
    		};
    		for(PermissionModel ap : adminPermissions)
    		{
    			
    			
    			appAdminRole.getPermissions().add(permissions.getValue(ap));
    		}
    		
    		ShiroRole appUserRole = SecurityModel.Role.APP_USER.toRole(domainID, appID);
    		PermissionModel[] userPermissions = {
    				AppPermission.ORDER_CREATE,
    				AppPermission.ORDER_DELETE,
    				AppPermission.ORDER_UPDATE,
    				AppPermission.ORDER_READ_USER_APP,
    				AppPermission.RESOURCE_READ_PUBLIC,
    				AppPermission.SELF
    			
    		};
    		for(PermissionModel ap : userPermissions)
    		{
    			appUserRole.getPermissions().add(permissions.getValue(ap));
    		}
    		
    		ShiroRole appServiceProviderRole = SecurityModel.Role.APP_SERVICE_PROVIDER.toRole(domainID, appID);
    		PermissionModel[] spPermissions =
    		{
    				AppPermission.ORDER_UPDATE_STATUS_APP,
    				AppPermission.ORDER_READ_APP,
    				AppPermission.RESOURCE_READ_PUBLIC,
    				AppPermission.SELF
    			
    		};
    		for(PermissionModel ap : spPermissions)
    		{
    			appServiceProviderRole.getPermissions().add(permissions.getValue(ap));
    		}
    		
    		ShiroRole appResourceRole = SecurityModel.Role.RESOURCE.toRole(domainID, appID);
    		PermissionModel[] resourcePermissions = {
    				AppPermission.RESOURCE_READ_PRIVATE,
    				AppPermission.RESOURCE_READ_PUBLIC
    		};
    		for(PermissionModel ap : resourcePermissions)
    		{
    			appResourceRole.getPermissions().add(permissions.getValue(ap));
    		}
    		
    		
    		apiSecurityManager.addRole(appAdminRole);
    		apiSecurityManager.addRole(appUserRole);
    		apiSecurityManager.addRole(appServiceProviderRole);
    		apiSecurityManager.addRole(appResourceRole);
    		getAPIDataStore().createSequence(ret.toCanonicalID());

            AppConfigDAO appConfigDAO = new AppConfigDAO();
            appConfigDAO.setAppIDDAO(ret);
            appConfigDAO = create(appConfigDAO);
    	}
    	
    	return ret;
    }
    
    
    public synchronized AppIDDAO deleteAppIDDAO(String domainID, String appID)
    	throws NullPointerException, IllegalArgumentException, AccessException, APIException
    {
    	SUS.checkIfNulls("Null domain or app id", domainID, appID);
    	getAPISecurityManager().checkPermissions(SecurityModel.Permission.APP_ID_DELETE.getValue());
    	AppIDDAO ret = lookupAppIDDAO(domainID, appID, true);
//
//        List<AppConfigDAO> list = search(AppConfigDAO.NVC_APP_CONFIG_DAO, new QueryMatchString(Const.RelationalOperator.EQUAL, ret.getReferenceID(), AppConfigDAO.Param.APP_ID.getNVConfig().getName(), MetaToken.REFERENCE_ID.getName()));
//
//        if (list != null && list.size() == 1) {
//            delete(list.get(0));
//        }

    	if (ret != null)
    	{
    		// delete the APP-ID
    		delete(ret);
    		
    		// delete the APP-PERMISSIONS
    		getAPIDataStore().delete(ShiroPermission.NVC_SHIRO_PERMISSION,
            new QueryMatch<String>(RelationalOperator.EQUAL, ret.getDomainID(), AppIDDAO.Param.DOMAIN_ID),
            LogicalOperator.AND, new QueryMatch<String>(RelationalOperator.EQUAL, ret.getAppID(), AppIDDAO.Param.APP_ID));
    		
    		// delete the APP-ROLES
    		getAPIDataStore().delete(ShiroRole.NVC_SHIRO_ROLE,
    	            new QueryMatch<String>(RelationalOperator.EQUAL, ret.getDomainID(), AppIDDAO.Param.DOMAIN_ID),
    	            LogicalOperator.AND, new QueryMatch<String>(RelationalOperator.EQUAL, ret.getAppID(), AppIDDAO.Param.APP_ID));
    		
    		// Delelte the APP-DEVICES
    		getAPIDataStore().delete(AppDeviceDAO.NVC_APP_DEVICE_DAO, new QueryMatch<String>(RelationalOperator.EQUAL, ret.getReferenceID(), "app_id","reference_id"));
    		
    	}

    	return ret;
    }
    
    public <V extends NVEntity> List<V> search(NVConfigEntity nvce, QueryMarker ... queryCriteria) 
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
    	return search(nvce, null, queryCriteria);
	}

    public <V extends NVEntity> List<V> search(NVConfigEntity nvce, List<String> fieldNames, QueryMarker ... queryCriteria)
			throws NullPointerException, IllegalArgumentException, AccessException, APIException
	{
    	return getAPIDataStore().search(nvce, fieldNames, queryCriteria);
	}
	
    public void updateSubjectRole(String subjectID, AppIDDAO appID, String roleName, CRUD crud)
			 throws NullPointerException, IllegalArgumentException, AccessException
	{
		String permission = PPEncoder.SINGLETON.encode(SecurityModel.PERM_ADD_ROLE, appID.getGUID());
		if (log.isEnabled()) log.getLogger().info("permision to check:" + permission);
		if (log.isEnabled()) log.getLogger().info(SharedUtil.toCanonicalID(',', subjectID, roleName));
		getAPISecurityManager().checkPermissions(permission);
		// permission checked
		UserIDDAO userID = lookupUserIDDAO(subjectID);
		if (userID != null)
		{
			String roleSubjectID = appID.getGUID() + "-" + roleName;
			if (log.isEnabled()) log.getLogger().info("role:" + roleSubjectID);
			if (log.isEnabled()) log.getLogger().info("userid:" +userID.getPrimaryEmail() + ":" + userID.getSubjectGUID());
			ShiroRole role = getAPISecurityManager().lookupRole(roleSubjectID);
			if (role == null)
			{
				throw new APIException("Role not found");
			}
			else
			{
				ShiroAssociationRule sard = new ShiroAssociationRule(role.getName()+"-" +userID.getSubjectID(), role, ShiroAssociationType.ROLE_TO_SUBJECT, userID);
				
				switch(crud)
				{
				case CREATE:
					getAPISecurityManager().addShiroRule(sard);
					if (log.isEnabled()) log.getLogger().info("Created");
					getAPISecurityManager().invalidateResource(subjectID);
					break;
				case DELETE:
					getAPISecurityManager().deleteShiroRule(sard);
					if (log.isEnabled()) log.getLogger().info("Deleted");
					getAPISecurityManager().invalidateResource(subjectID);
				default:
					break;
				
				}
				
				
				
			}
		}
		else
		{
			throw new APIException("User not found");
		}
		
	}
		
	public void updateSubjectPermission(String subjectID, AppIDDAO appID, String permssionName, CRUD crud)
			 throws NullPointerException, IllegalArgumentException, AccessException
	{
		 
	}




//	public SubjectAPIKey renewSubjectAPIKEy(String subjectID)
//			throws NullPointerException, IllegalArgumentException, AccessException, APIException
//	{
//		return renewSubjectAPIKEy(lookupSubjectAPIKey(subjectID, true));
//	}
	
//	@Override
//	public SubjectAPIKey renewSubjectAPIKEy(SubjectAPIKey sak)
//			throws NullPointerException, IllegalArgumentException, AccessException, APIException {
//		// TODO Auto-generated method stub
//		SubjectAPIKey ret = null;
//		if (sak instanceof AppDeviceDAO)
//		{
//			ret = new AppDeviceDAO();
//			((AppDeviceDAO)ret).setSubjectGUID(((AppDeviceDAO) sak).getSubjectGUID());
//			((AppDeviceDAO)ret).setDevice(((AppDeviceDAO) sak).getDevice());
//		}
//		else if (sak instanceof SubjectAPIKey)
//		{
//			ret = new SubjectAPIKey();
//		}
//
//		ret = createSubjectAPIKey(ret);
//
//
//		deleteSubjectAPIKey(sak);
//		return ret;
//	}




	

}