package org.zoxweb.server.security;

import org.bouncycastle.jce.ECPointUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedUtil;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

public class CryptoKeyTest {
    @Test
    public void ECKey256() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPair kp = CryptoUtil.generateKeyPair("ec", 256);
        X509EncodedKeySpec kSpec = new X509EncodedKeySpec(kp.getPublic().getEncoded());
        Assertions.assertTrue(kp.getPublic() instanceof ECPublicKey);
        System.out.println(kp.getPublic());
        ECPublicKey ecpk = (ECPublicKey) kp.getPublic();
        ECParameterSpec ecpc = ecpk.getParams();
        ECPoint ecPoint = ecpk.getW();
        System.out.println("ECPoint: " + ecpc.getGenerator().getAffineX() + " " + ecpc.getGenerator().getAffineY());
        System.out.println("ECParameterSpec: " + ecpc);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ecPoint = new ECPoint(ecPoint.getAffineX(), ecPoint.getAffineY());

        PublicKey generatedPK = keyFactory.generatePublic(new ECPublicKeySpec(ecPoint, ecpc));
        System.out.println(generatedPK);


        String publicKeyString ="MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE6NSq2eBuy80My3Ew0zWhKRtDOlbzud8ZJwiU3q7PQQ+4JOeAU2miwhcImE0kyeht6101L2xizofO6sPestmIA==";
        X509EncodedKeySpec cert = new X509EncodedKeySpec(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, publicKeyString));
        ecpk = (ECPublicKey) keyFactory.generatePublic(cert);
        System.out.println(cert.getFormat() + " " + ecpk);


        ecpc = ecpk.getParams();
        ecPoint = ecpk.getW();
        generatedPK = keyFactory.generatePublic(new ECPublicKeySpec(ecPoint, ecpc));
        System.out.println(generatedPK);


//        System.out.println("x: " + SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, SharedUtil.reverseBytes(ecpc.getGenerator().getAffineX().toByteArray())));
//        System.out.println("x: " + SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, (ecpc.getGenerator().getAffineX().toByteArray())));
        byte[] x = SharedBase64.decode(SharedBase64.Base64Type.URL, "6NSq2eBuy80My3Ew0zWhKRtDOlbzud8-ZJwiU3q7PQQ");
       // x = Base64.getUrlDecoder().decode("PuCTngFNposIXCJhNJMnobetdNS9sYs6HzurD3rLZiA");
        System.out.println(x.length);
        BigInteger bi = new BigInteger(1, x);
        System.out.println(bi);
        bi = new BigInteger(SharedUtil.reverseBytes(x));
        System.out.println(bi);
        System.out.println(SharedBase64.decodeAsString(SharedBase64.Base64Type.URL, "6NSq2eBuy80My3Ew0zWhKRtDOlbzud8-ZJwiU3q7PQQ"));

    }


    @Test
    public void generatePKECC() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        KeyPair kp = CryptoUtil.generateKeyPair("ec", 256);
        byte[] pubKey = kp.getPublic().getEncoded();


        BigInteger x = new BigInteger(1, pubKey, 1, 32);
        BigInteger y = new BigInteger(1, pubKey, 32, 32);


        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256r1");
        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC");
        algorithmParameters.init(ecGenParameterSpec);
        ECParameterSpec ecParameterSpec = algorithmParameters.getParameterSpec(ECParameterSpec.class);
        ECPoint point = ECPointUtil.decodePoint(ecParameterSpec.getCurve(), pubKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(point, ecParameterSpec);
        ECPublicKey ecPublicKey = (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
    }
}
