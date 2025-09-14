package org.zoxweb.server.security;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst.SignatureAlgo;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
//import sun.security.x509.X500Name;

public class CertificatesTest {

    public static void main(String... args) {

        for (String host : args) {
            try {
                Certificate[] certs = CryptoUtil.getRemoteCertificates(host);
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        System.out.println("<===============================================================================================");
                        X509Certificate xCert = (X509Certificate) cert;
                        System.out.println(xCert.getSubjectX500Principal().getName());
                        //X500Name xname = new X500Name(xCert.getSubjectX500Principal().getName());
//            NVGenericMap nvg = SharedUtil.toNVGenericMap(xCert.getSubjectX500Principal().getName(), "=",",", true);
                        NVGenericMap nvg = SecUtil.SINGLETON.certificateToNVGM(xCert);
                        //System.out.println("CN: " + nvg.get("cn").getValue());
                        String json = GSONUtil.toJSONDefault(nvg, true);
                        System.out.println(json);
                        NVGenericMap nvGenericMap = GSONUtil.fromJSONDefault(json, NVGenericMap.class);
//                        assert json.equals(GSONUtil.toJSONDefault(nvGenericMap, true));
                        String json2 = GSONUtil.toJSONDefault(nvGenericMap, true);
                        System.out.println(json2);
                        System.out.println(json.equals(json2) + "===============================================================================================>");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {

            KeyPair kp = CryptoUtil.generateKeyPair("EC", 256);
            System.out.println(
                    SharedUtil.toCanonicalID(',', kp.getPublic().getAlgorithm(), kp.getPublic().getFormat(),
                            SharedBase64.encodeAsString(Base64Type.URL, kp.getPublic().getEncoded())));
            System.out.println(
                    SharedUtil.toCanonicalID(',', kp.getPrivate().getAlgorithm(), kp.getPrivate().getFormat(),
                            SharedBase64.encodeAsString(Base64Type.URL, kp.getPrivate().getEncoded())));
            PublicKey pubKeyEC = CryptoUtil.generatePublicKey("EC", kp.getPublic().getEncoded());
            PrivateKey priKeyEC = CryptoUtil.generatePrivateKey("EC", kp.getPrivate().getEncoded());
            System.out.println(
                    "EC:" + pubKeyEC.equals(kp.getPublic()) + ", " + priKeyEC.equals(kp.getPrivate()));

            KeyPair aliceKey = CryptoUtil.generateKeyPair("RSA", 2048);
            PublicKey alicePubK = aliceKey.getPublic();
            PrivateKey alicePriK = aliceKey.getPrivate();
            System.out.println(SharedUtil
                    .toCanonicalID(',', aliceKey.getPublic().getAlgorithm(), aliceKey.getPublic().getFormat(),
                            SharedBase64.encodeAsString(Base64Type.URL, aliceKey.getPublic().getEncoded())));

            PublicKey pubK = CryptoUtil.generatePublicKey("RSA", aliceKey.getPublic().getEncoded());
            PrivateKey priK = CryptoUtil.generatePrivateKey("RSA", aliceKey.getPrivate().getEncoded());
            System.out.println("RSA:" + alicePubK.equals(pubK) + ", " + alicePriK.equals(priK));
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

            String tok = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Im1hcndhbiBuYWVsZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9";
            //byte[] sig = SharedBase64.decode(Base64Type.URL, "TUtOxVMqxsmhsCPMNc9BgV0BjpHqXWHLzoCDN8gRqNWvdQMP-3AwQXZP5966SDSYoPIWaCWCQuxLbgB3IRdpiMKaFP0qhrjRhI3DbAxWCg3c3qLYA7UoM70NhjvUYa4PUVGO8ngRs4hBOEuSpD1wg5-Hu3MmpSY012xwjGrdnG6gM9xx3rp_hWqaBSENXKRHPhUhV513MvQ6fafMn9aQa22PxQzAqz-Z-HDG6HjnxN9o4q9HAsfweluV1QRx5oO-KdzgrDn3Mn6N_HrsbWAgtosjFVpOYI3Q5rXTye_ueCCe5MkELSPkltuQ7R3qQCbvDbAp9bqzjLSCnJQfthYrPA");
            byte[] sig = SharedBase64.decode(Base64Type.URL, jwt.getHash());

            System.out.println("Singnature status: " + CryptoUtil
                    .verify(SignatureAlgo.SHA256_RSA, jwtPubKey, SharedStringUtil
                            .getBytes(tok), sig));


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
