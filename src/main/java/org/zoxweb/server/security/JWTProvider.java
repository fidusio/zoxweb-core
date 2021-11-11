package org.zoxweb.server.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.UUID;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTDecoder;
import org.zoxweb.shared.security.JWTDecoderData;
import org.zoxweb.shared.security.JWTEncoder;
import org.zoxweb.shared.security.JWTEncoderData;

public final class JWTProvider
    implements JWTEncoder, JWTDecoder {

  public final static JWTProvider SINGLETON = new JWTProvider();

  private JWTProvider() {

  }


  @Override
  public String encode(byte[] key, JWT jwt) throws AccessSecurityException {
    // TODO Auto-generated method stub
    try {
      return CryptoUtil.encodeJWT(key, jwt,false);
    } catch (SecurityException | IOException  | GeneralSecurityException e) {

      throw new AccessSecurityException(e.getMessage());
    }
  }


  @Override
  public JWT decode(byte[] key, String b64urlToken) throws AccessSecurityException {
    // TODO Auto-generated method stub
    try {
      return CryptoUtil.decodeJWT(key, b64urlToken);
    } catch ( SecurityException | IOException | GeneralSecurityException e) {

      e.printStackTrace();
      throw new AccessSecurityException(e.getMessage());
    }
  }


  @Override
  public String encode(JWTEncoderData jed)
      throws AccessSecurityException {
    return encode(jed.getKey(), jed.getJWT());
  }


  @Override
  public JWT decode(JWTDecoderData jdd)
      throws AccessSecurityException {
    return decode(jdd.getKey(), jdd.getToken());
  }


  public static void main(String... args) {
    try {

      for (int i = 0; i < args.length; i++) {
        JWT jwt = CryptoUtil.parseJWT(args[i]);
        System.out.println(GSONUtil.toJSON(jwt, true, false, false));
        System.out.println(new Date(jwt.getPayload().getIssuedAt() * 1000));
        System.out.println((jwt.getPayload().getIssuedAt()));
        System.out.println(System.currentTimeMillis());
        if (jwt.getHeader().getProperties().getValue("salt") != null) {
          UUID uuid = UUID.fromString(jwt.getHeader().getProperties().getValue("salt"));
          System.out.println(uuid);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }


  }

}
