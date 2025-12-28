package org.zoxweb.server.api;

import org.junit.jupiter.api.BeforeAll;
import org.zoxweb.shared.api.APIAppManager;
import org.zoxweb.shared.data.AppDeviceDAO;
import org.zoxweb.shared.data.AppIDDAO;
import org.zoxweb.shared.data.DeviceDAO;

public class APIAppManagerTest {

	private static String domainID = "xlogistx.io";
	private static String appID = "xlogistx";
	private static String subjectID = "xlogistx@xlogistx.io";
	static APIAppManager aam = new APIAppManagerProvider();
	@BeforeAll
	public static void init()
	{
		DeviceDAO dd = new DeviceDAO();
		AppIDDAO aid = new AppIDDAO();
		aid.setDomainAppID(domainID, appID);
		AppDeviceDAO add = new AppDeviceDAO();
		add.setSubjectGUID(aid.getGUID());
		add.setDevice(dd);
		add.setSubjectID(subjectID);
		aam.createAppDeviceDAO(add);
		
		
	}
	
//	@Test
//	public void testToGSON() throws IOException
//	{
//		SubjectAPIKey sak  = aam.lookupSubjectAPIKey(subjectID, true);
//		System.out.println(GSONUtil.toJSON(sak, false, false, false));
//	}
	
	
//	@Test
//	public void jwtValidation() throws IOException
//	{
//
//		SubjectAPIKey sak  = aam.lookupSubjectAPIKey(subjectID, true);
//		JWT jwt = new JWT();
//		JWTHeader header = jwt.getHeader();
//
//		header.setJWTAlgorithm(CryptoConst.JWTAlgo.HS256);
//		header.setTokenType("JWT");
//
//		JWTPayload payload =jwt.getPayload();
//		payload.setDomainID(domainID);
//		payload.setAppID(appID);
//		payload.setSubjectID(subjectID);
//
//		String token = JWTProvider.SINGLETON.encode(sak.getAPIKeyAsBytes(), jwt);
//
//		System.out.println(token);
//
//
//		JWT validateJWT = aam.validateJWT(token);
//		System.out.println(GSONUtil.toJSON(jwt, false, false, false, Base64Type.URL));
//		System.out.println(GSONUtil.toJSON(validateJWT, false, false, false, Base64Type.URL));
//	}
	
	
	
}
