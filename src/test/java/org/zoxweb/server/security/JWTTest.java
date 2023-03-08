package org.zoxweb.server.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTHeader;
import org.zoxweb.shared.security.JWTPayload;
import org.zoxweb.shared.crypto.CryptoConst.JWTAlgorithm;
import org.zoxweb.shared.util.BytesValue;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;

public class JWTTest {

  private static JWT jwtHS256 = null;
  private static JWT jwtNONE = null;
  private static JWT jwtHS512 = null;
  private static long index = 0;


  @BeforeAll
  public static void init() {

    jwtHS256 = new JWT();
    JWTHeader header = jwtHS256.getHeader();

    header.setJWTAlgorithm(CryptoConst.JWTAlgorithm.HS256);
    header.setTokenType("JWT");

    JWTPayload payload = jwtHS256.getPayload();
    payload.setDomainID("xlogistx.io");
    payload.setAppID("xlogistx");
    payload.setNonce(index++);
    //payload.setRandom(new byte[] {0,1,2,3});
    payload.setSubjectID("support@xlogistx.io");

    jwtNONE = new JWT();
    header = jwtNONE.getHeader();
    header.setJWTAlgorithm(CryptoConst.JWTAlgorithm.none);

    payload = jwtNONE.getPayload();
    payload.setDomainID("xlogistx.io");
    payload.setAppID("xlogistx");
    payload.setNonce(index++);
    //payload.setRandom(new byte[] {0,1,2,3});
    payload.setSubjectID("none@xlogistx.io");

    jwtHS512 = new JWT();

    header = jwtHS512.getHeader();

    header.setJWTAlgorithm(CryptoConst.JWTAlgorithm.HS512);
    header.setTokenType("JWT");

    payload = jwtHS512.getPayload();
    payload.setDomainID("xlogistx.io");
    payload.setAppID("xlogistx");
    payload.setNonce(index++);
    //payload.setRandom(new byte[] {0,1,2,3});
    payload.setSubjectID("support@xlogistx.io");


  }

  @Test
  public void toGSON() {
//      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException {
//		String json = GSONUtil.toJSON(jwtHS256, true, false, false, Base64Type.URL);
//		System.out.println(json);
//		
//		System.out.println("ToGSON:***************************************************************");
//		JWT localJwt = GSONUtil.fromJSON(json, JWT.class, Base64Type.URL);
//		json = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
//		System.out.println(json);
//		
//		System.out.println(localJwt.getPayload());
//		
//		String test = CryptoUtil.encodeJWT("secret", localJwt);
//		System.out.println(test);
//		
//		JWTPayload payload = new JWTPayload();
//		payload.setDomainID("xlogistx.io");
//		payload.setAppID("xlogistx");
//		payload.setSubjectID("batata@xlogistx.io");
//		payload.setName("John Doe");
//		payload.setAdmin(true);
//		payload.setNotBefore(System.currentTimeMillis());
//		payload.setNonce(index++);
//		payload.getNVGenericMap().add(new NVBoolean("bool", true));
//		
//		//payload.setRandom(new byte[] {0,1,2,3});
//		localJwt.setPayload(payload);
//		
//		NVGenericMap nvgm = payload.getNVGenericMap();
//		NVPair nvp = (NVPair)nvgm.get("name");
//		nvp.setValue("Mario");
//		json = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
//		System.out.println(json);
//		test = CryptoUtil.encodeJWT("secret", localJwt);
//		System.out.println(test);
//
//		System.out.println(CryptoUtil.decodeJWT("secret", test));
//		
//		String payloadJSON = GSONUtil.toJSON(payload, true, false, true, Base64Type.URL);
//		System.out.println("-------------------------------------------------------------------");
//		System.out.println(payloadJSON);
//	
//		
//		NVGenericMap gm = GSONUtil.fromJSONGenericMap(payloadJSON,(NVConfigEntity)payload.getNVConfig(), Base64Type.URL);
//		//System.out.println(gm);
//		NVGenericMapTest.printValue(gm);
//		
//		
//		JWTPayload tempJWTP = new  JWTPayload();
//		gm.add(new NVPair("http://toto.com", "batata"));
//		tempJWTP.setNVGenericMap(gm);
//		System.out.println(tempJWTP);
//	
//		tempJWTP.getNotBefore();
//		System.out.println("genericMapToJSON:" + GSONUtil.toJSONGenericMap(tempJWTP.getNVGenericMap(), false, false, false));
//		System.out.println( GSONUtil.toJSON(tempJWTP, false, false, true, Base64Type.URL));
//		
//		System.out.println("---------------++++++++++++++++++++----------------------------------------------------");
  }


  @Test
  public void testJWTNone()
      throws IOException, SecurityException, NullPointerException, IllegalArgumentException, GeneralSecurityException {
    String jsonNone = GSONUtil.toJSON(jwtNONE, true, false, false, Base64Type.URL);
    System.out.println(jsonNone);
    JWT localJwt = GSONUtil.fromJSON(jsonNone, JWT.class, Base64Type.URL);
    jsonNone = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
    System.out.println(jsonNone);

    System.out.println(localJwt.getPayload());

    String test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    jsonNone = GSONUtil.toJSON(localJwt, false, false, false, Base64Type.URL);
    System.out.println(jsonNone);
    test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    System.out.println(CryptoUtil.decodeJWT((String) null, test));
  }


  @Test
  public void testJWTHS256()
      throws IOException, GeneralSecurityException {

    System.out.println("--------------------------------------------------------------");
    String jsonHS256 = GSONUtil.toJSON(jwtHS256, false, false, false, Base64Type.URL);
    System.out.println(jsonHS256);
    JWT localJwt = GSONUtil.fromJSON(jsonHS256, JWT.class, Base64Type.URL);
    jsonHS256 = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
    System.out.println(jsonHS256);

    System.out.println(localJwt.getPayload());

    String test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    jsonHS256 = GSONUtil.toJSON(localJwt, false, false, false, Base64Type.URL);
    System.out.println(jsonHS256);
    test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    System.out.println(CryptoUtil.decodeJWT("secret", test));
    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }

  @Test
  public void testJWTES256()
      throws IOException, GeneralSecurityException {

    System.out.println("--------------------------------------------------------------");
    JWT jwtES256 = new JWT();
    jwtES256.getHeader().setJWTAlgorithm(CryptoConst.JWTAlgorithm.ES256);
    jwtES256.getHeader().setTokenType("JWT");

    JWTPayload payload = jwtES256.getPayload();
    payload.setDomainID("xlogistx.io");
    payload.setAppID("xlogistx");
    payload.setNonce(index++);
    payload.setSubjectID("support@xlogistx.io");

    String jsonES256 = GSONUtil.toJSON(jwtES256, false, false, false, Base64Type.URL);
    System.out.println(jsonES256);
    JWT localJwt = GSONUtil.fromJSON(jsonES256, JWT.class, Base64Type.URL);
    jsonES256 = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
    System.out.println(jwtES256);

    System.out.println(localJwt.getPayload());
    KeyPair kp = CryptoUtil.generateKeyPair("EC", 256);

    String test = CryptoUtil.encodeJWT(kp.getPrivate().getEncoded(), localJwt);
    System.out.println("Encoded: " + test);

    jsonES256 = GSONUtil.toJSON(localJwt, false, false, false, Base64Type.URL);
    System.out.println(jwtES256);
    test = CryptoUtil.encodeJWT(kp.getPrivate().getEncoded(), localJwt);
    System.out.println(test);

    System.out.println("Decoded: " + CryptoUtil.decodeJWT(kp.getPublic().getEncoded(), test));

    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }


  @Test
  public void testJWTHS512()
      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, GeneralSecurityException {

    System.out
        .println("testJWTHS512--------------------------------------------------------------");
    String jsonHS512 = GSONUtil.toJSON(jwtHS512, false, false, false, Base64Type.URL);
    System.out.println(jsonHS512);
    JWT localJwt = GSONUtil.fromJSON(jsonHS512, JWT.class, Base64Type.URL);
    jsonHS512 = GSONUtil.toJSON(localJwt, true, false, false, Base64Type.URL);
    System.out.println(jsonHS512);

    System.out.println(localJwt.getPayload());

    String test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    jsonHS512 = GSONUtil.toJSON(localJwt, false, false, false, Base64Type.URL);
    System.out.println(jsonHS512);
    test = CryptoUtil.encodeJWT("secret", localJwt);
    System.out.println(test);

    System.out.println(CryptoUtil.decodeJWT("secret", test));
    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }


  @Test
  public void testJWTGWT()
      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, GeneralSecurityException {

    System.out.println("--------------------------------------------------------------");
    //"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1MDg0NjA1MzMzNDEsImRvbWFpbiI6Inhsb2dpc3R4LmlvIiwiYXBwIjoieGxvZ2lzdHgifQ.oxqpJP18sxq51qNt8_kisYeS0oR31QL2DrP-M_wYaO0";
    String gwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1MDg0NzMyMzU0OTMsImRvbWFpbiI6Inhsb2dpc3R4LmlvIiwiYXBwIjoieGxvZ2lzdHgifQ.MrrlKa90ut2RBcfJ61lXB7ScwHikPyvkvpd_DVxPnFg";
    JWT decoded = CryptoUtil.decodeJWT("secret", gwtToken);
    String test = CryptoUtil.encodeJWT("secret", decoded);
    System.out.println("TESTGWT  :" + gwtToken);
    System.out.println("local:" + test);
    System.out.println(decoded.getPayload().getProperties());
    System.out.println(GSONUtil.toJSON(decoded, false, false, false, Base64Type.URL));
    System.out.println("Are equals:" + test.equals(gwtToken));
    Assertions.assertEquals(test, gwtToken);
    decoded.getPayload().getProperties().add(new NVPair("mario", "taza"));
    String json = GSONUtil.toJSON(decoded, false, false, true, Base64Type.URL);
    System.out.println(json);
    JWT fromJSON = GSONUtil.fromJSON(json);
    json = GSONUtil.toJSON(fromJSON, false, false, false, Base64Type.URL);
    System.out.println(json);
    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }


  @Test
  public void testJWTJohnDoe()
      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, GeneralSecurityException {

    System.out
        .println("testJWTJohnDoe--------------------------------------------------------------");
    //"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1MDg0NjA1MzMzNDEsImRvbWFpbiI6Inhsb2dpc3R4LmlvIiwiYXBwIjoieGxvZ2lzdHgifQ.oxqpJP18sxq51qNt8_kisYeS0oR31QL2DrP-M_wYaO0";
    String gwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o";
    System.out.println(CryptoUtil.parseJWT(gwtToken));
    JWT decoded = CryptoUtil.decodeJWT("secret", gwtToken);
    System.out.println(
        "Subject class id:" + decoded.getPayload().getProperties().get("sub").getClass().getName());

    String test = CryptoUtil.encodeJWT("secret", decoded);
    System.out.println("JOHNDOE:" + gwtToken);
    System.out.println("local  :" + test);
    System.out.println(decoded.getPayload().getProperties());
    System.out.println(GSONUtil.toJSON(decoded, false, false, false, Base64Type.URL));
    System.out.println("Are equals:" + test.equals(gwtToken));
    Assertions.assertEquals(test, gwtToken);
    decoded.getPayload().getProperties().add(new NVPair("mario", "taza"));
    String json = GSONUtil.toJSON(decoded, false, false, true, Base64Type.URL);
    System.out.println(json);
    JWT fromJSON = GSONUtil.fromJSON(json);
    json = GSONUtil.toJSON(fromJSON, false, false, false, Base64Type.URL);
    System.out.println(json);
    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }

  @Test
  public void testJWTKP()
      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, GeneralSecurityException {

    System.out.println("--------------------------------------------------------------");
    JWT jwt = new JWT();

    jwt.getHeader().setTokenType("JWT");

    JWTPayload payload = jwt.getPayload();
    payload.setDomainID("xlogistx.io");
    payload.setAppID("xlogistx");
    payload.setNonce(index++);
    payload.setSubjectID("support@xlogistx.io");


    KeyPair ecKP = CryptoUtil.generateKeyPair("EC", 521);
    KeyPair rsaKP = CryptoUtil.generateKeyPair("RSA", 2048);

    JWTAlgorithm[] list = {
        CryptoConst.JWTAlgorithm.ES256, CryptoConst.JWTAlgorithm.ES512, CryptoConst.JWTAlgorithm.RS256, CryptoConst.JWTAlgorithm.RS512
    };

    for (JWTAlgorithm algo : list) {
      jwt.getHeader().setJWTAlgorithm(algo);
      KeyPair kp = null;
      switch (algo) {

        case none:
          break;
        case HS256:
          break;
        case HS512:
          break;
        case RS256:
        case RS512:
          kp = rsaKP;
          break;
        case ES256:
        case ES512:
          kp = ecKP;
          break;
      }

      String test = CryptoUtil.encodeJWT(kp.getPrivate().getEncoded(), jwt);

      CryptoUtil.decodeJWT(kp.getPublic().getEncoded(), test);
      System.out.println(algo);
    }

    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }

//	@Test (expected = IllegalArgumentException.class)
//	public void invalidDomain()
//	{
//
//		
//		JWTPayload payload = new JWTPayload();
//		
//		payload.setDomainID("xlogistx");
//		payload.setAppID("xlogistx");
//		//payload.setRandom(new byte[] {0,1,2,3});
//		payload.setSubjectID("support@xlogistx.io");
//	}

  @Test
  public void sequenceHashTest() throws NoSuchAlgorithmException, CloneNotSupportedException {
    MessageDigest md = HashUtil.createMessageDigest("sha-256");


    int sum = 0;
    for (int i=0; i < 10; i++)
    {
      md.update(BytesValue.INT.toBytes(i));

      MessageDigest md2 = (MessageDigest)md.clone();
      System.out.println(SharedStringUtil.bytesToHex(md2.digest()));
      System.out.println(SharedStringUtil.bytesToHex(HashUtil.createMessageDigest("sha-256").digest(BytesValue.INT.toBytes(i))));
      System.out.println(md == md2);
    }
  }


}
