package org.zoxweb.server.api;

import java.io.IOException;

//import org.junit.Before;
//import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.security.JWTProvider;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.api.APIAppManager;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.AppDeviceDAO;
import org.zoxweb.shared.data.AppIDDAO;
import org.zoxweb.shared.data.DeviceDAO;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTHeader;
import org.zoxweb.shared.security.JWTPayload;
import org.zoxweb.shared.security.SubjectAPIKey;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

public class APIAppManagerTest {

	private String domainID = "xlogistx.io";
	private String appID = "xlogistx";
	private String subjectID = "xlogistx@xlogistx.io";
	APIAppManager aam = new APIAppManagerProvider();
	@BeforeAll
	public void init()
	{
		DeviceDAO dd = new DeviceDAO();
		AppIDDAO aid = new AppIDDAO();
		aid.setDomainAppID(domainID, appID);
		AppDeviceDAO add = new AppDeviceDAO();
		add.setAppGID(aid.getAppGID());
		add.setDevice(dd);
		add.setSubjectID(subjectID);
		aam.createAppDeviceDAO(add);
		
		
	}
	
	@Test
	public void testToGSON() throws IOException
	{
		SubjectAPIKey sak  = aam.lookupSubjectAPIKey(subjectID, true);
		System.out.println(GSONUtil.toJSON(sak, false, false, false));
	}
	
	
	@Test
	public void jwtValidation() throws IOException
	{

		SubjectAPIKey sak  = aam.lookupSubjectAPIKey(subjectID, true);
		JWT jwt = new JWT();
		JWTHeader header = jwt.getHeader();
		
		header.setJWTAlgorithm(CryptoConst.JWTAlgorithm.HS256);
		header.setTokenType("JWT");
		
		JWTPayload payload =jwt.getPayload();
		payload.setDomainID(domainID);
		payload.setAppID(appID);
		payload.setSubjectID(subjectID);
		
		String token = JWTProvider.SINGLETON.encode(sak.getAPIKeyAsBytes(), jwt);
		
		System.out.println(token);
		
		
		JWT validateJWT = aam.validateJWT(token);
		System.out.println(GSONUtil.toJSON(jwt, false, false, false, Base64Type.URL));
		System.out.println(GSONUtil.toJSON(validateJWT, false, false, false, Base64Type.URL));
	}
	
	
	
}
