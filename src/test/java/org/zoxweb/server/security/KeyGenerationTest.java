package org.zoxweb.server.security;


import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.CryptoConst.SignatureAlgo;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;


public class KeyGenerationTest {


  public static void main(String... args) {

    //byte[] message = SharedStringUtil.getBytes("JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata, JavaConsigliere Batata.");

    try {
      for (int i = 0; i < 5; i++) {

        EncryptedData ed = CryptoUtil
            .encryptData(new EncapsulatedKey(), SharedStringUtil.getBytes("password"), null, 1);
        String json = GSONUtil.toJSON(ed, false, false, false, Base64Type.URL);
        KeyPair aliceKey = CryptoUtil.generateKeyPair("RSA", 2048);
        long ts = System.nanoTime();
        KeyPair bobKey = CryptoUtil.generateKeyPair("RSA", 2048);
        ts = System.nanoTime() - ts;
        System.out.println("" + bobKey.toString() + " it took " + TimeInMillis.nanosToString(ts));
        System.out.println("json:" + json);
        System.out.println(
            "canonical:" + ed.toCanonicalID() + "   length:" + ed.toCanonicalID().length());
        EncryptedData edTemp = EncryptedData.fromCanonicalID(ed.toCanonicalID());
        System.out.println(GSONUtil.toJSON(edTemp, false, false, false, Base64Type.URL));
        bobKey.getPublic().getFormat();

        for (int j = 0; j < 2; j++) {
          System.out.println(CryptoUtil.toString(bobKey.getPublic()));
          System.out.println(CryptoUtil.toString(bobKey.getPrivate()));
          byte[] signature = CryptoUtil
              .sign(CryptoConst.SignatureAlgo.SHA256_RSA, bobKey.getPrivate(),
                  SharedStringUtil.getBytes(json));
          System.out.println("Signature length:" + signature.length + " : " + CryptoUtil
              .verify(CryptoConst.SignatureAlgo.SHA256_RSA, bobKey.getPublic(),
                  SharedStringUtil.getBytes(json), signature) + " : " + SharedBase64
              .encodeAsString(Base64Type.URL, signature));
          byte[] encrypted = CryptoUtil
              .encrypt(bobKey.getPublic(), SharedStringUtil.getBytes(json));

          byte[] decrypted = CryptoUtil.decrypt(bobKey.getPrivate(), encrypted);
          System.out.println("Signature length:" + signature.length + " : " + SharedBase64
              .encodeAsString(Base64Type.URL, signature));
          System.out.println("Decrypted Message:" + SharedStringUtil.toString(decrypted));
          System.out.println("Encrypted by bob based64 [" + encrypted.length + "]:" + SharedBase64
              .encodeAsString(Base64Type.URL, encrypted));

          Key aesKey = CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, 256);
          System.out.println(CryptoUtil.toString(aesKey));

          encrypted = CryptoUtil.encrypt(aliceKey.getPublic(), SharedStringUtil.getBytes(json));
          decrypted = CryptoUtil.decrypt(aliceKey.getPrivate(), encrypted);
          System.out.println("Decrypted Message:" + SharedStringUtil.toString(decrypted));
          System.out.println("Encrypted by alice based64 [" + encrypted.length + "]:" + SharedBase64
              .encodeAsString(Base64Type.URL, encrypted));
          System.out.println(j);
        }
        System.out.println("\n\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    int loopSize = 500;

    Key[] keys = new Key[loopSize];
    try {
      long ts = System.currentTimeMillis();
      for (int i = 0; i < loopSize; i++) {
        keys[i] = CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, i % 2 == 0 ? 256 : 128);
      }
      ts = System.currentTimeMillis() - ts;
      for (Key k : keys) {
        System.out.println(SharedUtil.toCanonicalID(',', k.getAlgorithm(), k.getEncoded().length,
            SharedStringUtil.bytesToHex(k.getEncoded())));
      }
      System.out.println("it took " + TimeInMillis.toString(ts));
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {

      int loop = 500;
      long ts = System.currentTimeMillis();
      for (int i = 0; i < loop; i++) {
        CryptoUtil
            .encryptData(new EncapsulatedKey(), SharedStringUtil.getBytes("password"), null, 8196);
      }
      ts = System.currentTimeMillis() - ts;
      System.out
          .println("Generated " + loop + " with rehash 8196 took " + TimeInMillis.toString(ts));
      loop = 100000;
      ts = System.currentTimeMillis();
      for (int i = 0; i < loop; i++) {
        CryptoUtil
            .encryptData(new EncapsulatedKey(), SharedStringUtil.getBytes("password"), null, 1);
      }
      ts = System.currentTimeMillis() - ts;
      System.out.println("Generated " + loop + " with rehash 1 took " + TimeInMillis.toString(ts));
      EncryptedData ed = CryptoUtil
          .encryptData(new EncapsulatedKey(), SharedStringUtil.getBytes("password"), null, 1);
      String json = GSONUtil.toJSON(ed, false, false, false, Base64Type.URL);
      System.out.println(json);
      System.out.println(json.length() + ":" + SharedStringUtil.getBytes(json).length);
      ed = GSONUtil.fromJSON(json, EncapsulatedKey.class, Base64Type.URL);
      byte[] data = CryptoUtil.decryptEncryptedData(ed, SharedStringUtil.getBytes("password"), 1);
      System.out.println(SharedStringUtil.bytesToHex(data));
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (String filename : args) {
      try {
//				String pem = IOUtil.inputStreamToString(filename);
//				String splits[] = pem.split("\n");
//				System.out.println(pem);
//				System.out.println(splits.length);
//				StringBuffer sb = new StringBuffer();
//				for (String str : splits)
//				{
//					if(!str.startsWith("-"))
//						sb.append(str);
//				}
//				System.out.println(sb.toString());
//				
//				
//				
//				
//				byte keys[] = SharedBase64.decode(sb.toString());
//				
//				
//				
//				
//				
//				//PublicKey pubKey = readPublicKey(keys);
//				PrivateKey priKey = readPrivateKey(keys);
//				System.out.println(filename);
//				//System.out.println("public  key:" +  SharedBase64.encodeAsString(Base64Type.URL, pubKey.getEncoded()));
        PublicKey pk = CryptoUtil.getRemotePublicKey(filename);
        System.out.println(filename + " public key:" + CryptoUtil.toString(pk));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      KeyPair aliceKey = CryptoUtil.generateKeyPair("RSA", 2048);
      PublicKey alicePubK = aliceKey.getPublic();
      PrivateKey alicePriK = aliceKey.getPrivate();

      PublicKey pubK = CryptoUtil.generatePublicKey("RSA", aliceKey.getPublic().getEncoded());
      PrivateKey priK = CryptoUtil.generatePrivateKey("RSA", aliceKey.getPrivate().getEncoded());
      System.out.println(alicePubK.equals(pubK) + "," + alicePriK.equals(priK));
      String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Im1hcndhbiBuYWVsZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.TUtOxVMqxsmhsCPMNc9BgV0BjpHqXWHLzoCDN8gRqNWvdQMP-3AwQXZP5966SDSYoPIWaCWCQuxLbgB3IRdpiMKaFP0qhrjRhI3DbAxWCg3c3qLYA7UoM70NhjvUYa4PUVGO8ngRs4hBOEuSpD1wg5-Hu3MmpSY012xwjGrdnG6gM9xx3rp_hWqaBSENXKRHPhUhV513MvQ6fafMn9aQa22PxQzAqz-Z-HDG6HjnxN9o4q9HAsfweluV1QRx5oO-KdzgrDn3Mn6N_HrsbWAgtosjFVpOYI3Q5rXTye_ueCCe5MkELSPkltuQ7R3qQCbvDbAp9bqzjLSCnJQfthYrPA";
      JWT jwt = SecUtil.SINGLETON.parseJWT(jwtToken);
      System.out.println(GSONUtil.toJSON(jwt, true, false, false));

      PublicKey jwtPubKey = CryptoUtil.generatePublicKey("RSA", SharedBase64
          .decode(Base64Type.DEFAULT,
              "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv\n" +
                  "vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc\n" +
                  "aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy\n" +
                  "tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0\n" +
                  "e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb\n" +
                  "V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9\n" +
                  "MwIDAQAB"));

//		  byte[] pemPrivKey = Base64.getDecoder().decode(SharedStringUtil.filterString("MIIEogIBAAKCAQEAnzyis1ZjfNB0bBgKFMSvvkTtwlvBsaJq7S5wA+kzeVOVpVWw\n" + 
//		      "kWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHcaT92whREFpLv9cj5lTeJSibyr/Mr\n" + 
//		      "m/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIytvHWTxZYEcXLgAXFuUuaS3uF9gEi\n" + 
//		      "NQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0e+lf4s4OxQawWD79J9/5d3Ry0vbV\n" + 
//		      "3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWbV6L11BWkpzGXSW4Hv43qa+GSYOD2\n" + 
//		      "QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9MwIDAQABAoIBACiARq2wkltjtcjs\n" + 
//		      "kFvZ7w1JAORHbEufEO1Eu27zOIlqbgyAcAl7q+/1bip4Z/x1IVES84/yTaM8p0go\n" + 
//		      "amMhvgry/mS8vNi1BN2SAZEnb/7xSxbflb70bX9RHLJqKnp5GZe2jexw+wyXlwaM\n" + 
//		      "+bclUCrh9e1ltH7IvUrRrQnFJfh+is1fRon9Co9Li0GwoN0x0byrrngU8Ak3Y6D9\n" + 
//		      "D8GjQA4Elm94ST3izJv8iCOLSDBmzsPsXfcCUZfmTfZ5DbUDMbMxRnSo3nQeoKGC\n" + 
//		      "0Lj9FkWcfmLcpGlSXTO+Ww1L7EGq+PT3NtRae1FZPwjddQ1/4V905kyQFLamAA5Y\n" + 
//		      "lSpE2wkCgYEAy1OPLQcZt4NQnQzPz2SBJqQN2P5u3vXl+zNVKP8w4eBv0vWuJJF+\n" + 
//		      "hkGNnSxXQrTkvDOIUddSKOzHHgSg4nY6K02ecyT0PPm/UZvtRpWrnBjcEVtHEJNp\n" + 
//		      "bU9pLD5iZ0J9sbzPU/LxPmuAP2Bs8JmTn6aFRspFrP7W0s1Nmk2jsm0CgYEAyH0X\n" + 
//		      "+jpoqxj4efZfkUrg5GbSEhf+dZglf0tTOA5bVg8IYwtmNk/pniLG/zI7c+GlTc9B\n" + 
//		      "BwfMr59EzBq/eFMI7+LgXaVUsM/sS4Ry+yeK6SJx/otIMWtDfqxsLD8CPMCRvecC\n" + 
//		      "2Pip4uSgrl0MOebl9XKp57GoaUWRWRHqwV4Y6h8CgYAZhI4mh4qZtnhKjY4TKDjx\n" + 
//		      "QYufXSdLAi9v3FxmvchDwOgn4L+PRVdMwDNms2bsL0m5uPn104EzM6w1vzz1zwKz\n" + 
//		      "5pTpPI0OjgWN13Tq8+PKvm/4Ga2MjgOgPWQkslulO/oMcXbPwWC3hcRdr9tcQtn9\n" + 
//		      "Imf9n2spL/6EDFId+Hp/7QKBgAqlWdiXsWckdE1Fn91/NGHsc8syKvjjk1onDcw0\n" + 
//		      "NvVi5vcba9oGdElJX3e9mxqUKMrw7msJJv1MX8LWyMQC5L6YNYHDfbPF1q5L4i8j\n" + 
//		      "8mRex97UVokJQRRA452V2vCO6S5ETgpnad36de3MUxHgCOX3qL382Qx9/THVmbma\n" + 
//		      "3YfRAoGAUxL/Eu5yvMK8SAt/dJK6FedngcM3JEFNplmtLYVLWhkIlNRGDwkg3I5K\n" + 
//		      "y18Ae9n7dHVueyslrb6weq7dTkYDi3iOYRW8HRkIQh06wEdbxt0shTzAJvvCQfrB\n" + 
//		      "jg/3747WSsf/zBTcHihTRBdAv6OmdhV4/dD5YBfLAkLrd+mX7iE=", "\n"));

      String tok = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Im1hcndhbiBuYWVsZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9";
      //byte[] sig = SharedBase64.decode(Base64Type.URL, "TUtOxVMqxsmhsCPMNc9BgV0BjpHqXWHLzoCDN8gRqNWvdQMP-3AwQXZP5966SDSYoPIWaCWCQuxLbgB3IRdpiMKaFP0qhrjRhI3DbAxWCg3c3qLYA7UoM70NhjvUYa4PUVGO8ngRs4hBOEuSpD1wg5-Hu3MmpSY012xwjGrdnG6gM9xx3rp_hWqaBSENXKRHPhUhV513MvQ6fafMn9aQa22PxQzAqz-Z-HDG6HjnxN9o4q9HAsfweluV1QRx5oO-KdzgrDn3Mn6N_HrsbWAgtosjFVpOYI3Q5rXTye_ueCCe5MkELSPkltuQ7R3qQCbvDbAp9bqzjLSCnJQfthYrPA");
      byte[] sig = SharedBase64.decode(Base64Type.URL, jwt.getHash());

      System.out.println("Singnature status: " + CryptoUtil
          .verify(SignatureAlgo.SHA256_RSA, jwtPubKey, SharedStringUtil.getBytes(tok), sig));

      //PrivateKey jwtPrivKey = readPrivateKey(pemPrivKey);
//		  PrivateKey jwtPrivKey = readPrivateKey(SharedBase64.decode(Base64Type.DEFAULT, "MIIEogIBAAKCAQEAnzyis1ZjfNB0bBgKFMSvvkTtwlvBsaJq7S5wA+kzeVOVpVWw\n" + 
//		      "kWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHcaT92whREFpLv9cj5lTeJSibyr/Mr\n" + 
//		      "m/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIytvHWTxZYEcXLgAXFuUuaS3uF9gEi\n" + 
//		      "NQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0e+lf4s4OxQawWD79J9/5d3Ry0vbV\n" + 
//		      "3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWbV6L11BWkpzGXSW4Hv43qa+GSYOD2\n" + 
//		      "QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9MwIDAQABAoIBACiARq2wkltjtcjs\n" + 
//		      "kFvZ7w1JAORHbEufEO1Eu27zOIlqbgyAcAl7q+/1bip4Z/x1IVES84/yTaM8p0go\n" + 
//		      "amMhvgry/mS8vNi1BN2SAZEnb/7xSxbflb70bX9RHLJqKnp5GZe2jexw+wyXlwaM\n" + 
//		      "+bclUCrh9e1ltH7IvUrRrQnFJfh+is1fRon9Co9Li0GwoN0x0byrrngU8Ak3Y6D9\n" + 
//		      "D8GjQA4Elm94ST3izJv8iCOLSDBmzsPsXfcCUZfmTfZ5DbUDMbMxRnSo3nQeoKGC\n" + 
//		      "0Lj9FkWcfmLcpGlSXTO+Ww1L7EGq+PT3NtRae1FZPwjddQ1/4V905kyQFLamAA5Y\n" + 
//		      "lSpE2wkCgYEAy1OPLQcZt4NQnQzPz2SBJqQN2P5u3vXl+zNVKP8w4eBv0vWuJJF+\n" + 
//		      "hkGNnSxXQrTkvDOIUddSKOzHHgSg4nY6K02ecyT0PPm/UZvtRpWrnBjcEVtHEJNp\n" + 
//		      "bU9pLD5iZ0J9sbzPU/LxPmuAP2Bs8JmTn6aFRspFrP7W0s1Nmk2jsm0CgYEAyH0X\n" + 
//		      "+jpoqxj4efZfkUrg5GbSEhf+dZglf0tTOA5bVg8IYwtmNk/pniLG/zI7c+GlTc9B\n" + 
//		      "BwfMr59EzBq/eFMI7+LgXaVUsM/sS4Ry+yeK6SJx/otIMWtDfqxsLD8CPMCRvecC\n" + 
//		      "2Pip4uSgrl0MOebl9XKp57GoaUWRWRHqwV4Y6h8CgYAZhI4mh4qZtnhKjY4TKDjx\n" + 
//		      "QYufXSdLAi9v3FxmvchDwOgn4L+PRVdMwDNms2bsL0m5uPn104EzM6w1vzz1zwKz\n" + 
//		      "5pTpPI0OjgWN13Tq8+PKvm/4Ga2MjgOgPWQkslulO/oMcXbPwWC3hcRdr9tcQtn9\n" + 
//		      "Imf9n2spL/6EDFId+Hp/7QKBgAqlWdiXsWckdE1Fn91/NGHsc8syKvjjk1onDcw0\n" + 
//		      "NvVi5vcba9oGdElJX3e9mxqUKMrw7msJJv1MX8LWyMQC5L6YNYHDfbPF1q5L4i8j\n" + 
//		      "8mRex97UVokJQRRA452V2vCO6S5ETgpnad36de3MUxHgCOX3qL382Qx9/THVmbma\n" + 
//		      "3YfRAoGAUxL/Eu5yvMK8SAt/dJK6FedngcM3JEFNplmtLYVLWhkIlNRGDwkg3I5K\n" + 
//		      "y18Ae9n7dHVueyslrb6weq7dTkYDi3iOYRW8HRkIQh06wEdbxt0shTzAJvvCQfrB\n" + 
//		      "jg/3747WSsf/zBTcHihTRBdAv6OmdhV4/dD5YBfLAkLrd+mX7iE="));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
