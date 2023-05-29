package org.zoxweb.server.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedUtil;

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

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
        System.out.println("PublicKey from cert: " + cert.getFormat() + " " + ecpk);



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


//    @Test
//    public void generatePKECC() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
//        KeyPair kp = CryptoUtil.generateKeyPair("ec", 256);
//        byte[] pubKey = kp.getPublic().getEncoded();
//
//
//        BigInteger x = new BigInteger(1, pubKey, 1, 32);
//        BigInteger y = new BigInteger(1, pubKey, 32, 32);
//
//
//        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256r1");
//        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC");
//        algorithmParameters.init(ecGenParameterSpec);
//        ECParameterSpec ecParameterSpec = algorithmParameters.getParameterSpec(ECParameterSpec.class);
//        ECPoint point = ECPointUtil.decodePoint(ecParameterSpec.getCurve(), pubKey);
//        KeyFactory keyFactory = KeyFactory.getInstance("EC");
//        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(point, ecParameterSpec);
//        ECPublicKey ecPublicKey = (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
//    }


    @Test
    public void generateECSymmetricKey() throws NoSuchAlgorithmException, InvalidKeyException {
        KeyPairGenerator kpgA = KeyPairGenerator.getInstance("EC");
        kpgA.initialize(256);
        KeyPair keyPairA = kpgA.generateKeyPair();
        PublicKey publicKeyA = keyPairA.getPublic();
        PrivateKey privateKeyA = keyPairA.getPrivate();

        // Generate an EC KeyPair for party B
        KeyPairGenerator kpgB = KeyPairGenerator.getInstance("EC");
        kpgB.initialize(256);
        KeyPair keyPairB = kpgB.generateKeyPair();
        PublicKey publicKeyB = keyPairB.getPublic();
        PrivateKey privateKeyB = keyPairB.getPrivate();

        // Perform key agreement
        KeyAgreement ecdhA = KeyAgreement.getInstance("ECDH");
        ecdhA.init(privateKeyA);
        ecdhA.doPhase(publicKeyB, true);
        byte[] secretKeyA = ecdhA.generateSecret();

        // Perform key agreement for party B
        KeyAgreement ecdhB = KeyAgreement.getInstance("ECDH");
        ecdhB.init(privateKeyB);
        ecdhB.doPhase(publicKeyA, true);
        byte[] secretKeyB = ecdhB.generateSecret();

        // Should print "true" as both secret keys should be equal
        System.out.println(java.util.Arrays.equals(secretKeyA, secretKeyB) + " " + secretKeyA.length);
    }

    @Test
    public void testPublicFromCert() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {

        String pemPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEimMu5gsCm8/Tw4ZtAq/xhOroAXjrCwGo\n" +
                "HBjA4xL2yXvWVwdUSqIil2YFvzFvkPD1fXp42UiB5FJ99SgovyJKlA==\n" +
                "-----END PUBLIC KEY-----";

        // Remove the first and last lines
        String publicKeyPEM = pemPublicKey.replace("-----BEGIN PUBLIC KEY-----\n", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\n", "");
        byte[] publicKeyBytes = SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, publicKeyPEM); // replace with actual byte array

        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = kf.generatePublic(x509Spec);

        // Print the public key
        System.out.println("Public Key: " + publicKey);

        KeyPairGenerator kpgA = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        kpgA.initialize(ecSpec, new SecureRandom());
        KeyPair keyPairA = kpgA.generateKeyPair();
        PublicKey publicKeyA = keyPairA.getPublic();
        PrivateKey privateKeyA = keyPairA.getPrivate();

        // Perform key agreement
        KeyAgreement ecdhA = KeyAgreement.getInstance("ECDH");
        ecdhA.init(privateKeyA);
        ecdhA.doPhase(publicKey, true);
        byte[] secretKeyA = ecdhA.generateSecret();

        System.out.println(SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, secretKeyA));
    }

    @Test
    public void testKeyAgreement() throws GeneralSecurityException {
        String pemPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEimMu5gsCm8/Tw4ZtAq/xhOroAXjrCwGo\n" +
                "HBjA4xL2yXvWVwdUSqIil2YFvzFvkPD1fXp42UiB5FJ99SgovyJKlA==\n" +
                "-----END PUBLIC KEY-----";

        // Convert byte array to PublicKey
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey publicKeyFromPem = CryptoUtil.generatePublicKey("EC", pemPublicKey);

        // Generate an EC KeyPair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        kpg.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] publicKeyEnc = publicKey.getEncoded();
        byte[] privateKeyEnc = privateKey.getEncoded();
        System.out.println("public key: " + SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, publicKeyEnc));
        System.out.println("private key: " + SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, privateKeyEnc));
        publicKey = CryptoUtil.generatePublicKey("EC", publicKeyEnc);
        String privateKeyTest = "MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCDXlo+pQlfThU7ed7BqsRQfF2LQZ30NSlDQbjH2Rtdg+A==";
        privateKey = CryptoUtil.generatePrivateKey("ec", SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, privateKeyTest));
//        public key: MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE8qccxFkZ+W8801CagKbFa6XvaHuo3pStDxrrPpaUBciQY6RL2NeRAub7swOEGePxgFgWCK5b3FP88bVyHw3aag==
//        private key: MD4CAQAwEAYHKoZIzj0CAQYFK4EEAAoEJzAlAgEBBCDXlo+pQlfThU7ed7BqsRQfF2LQZ30NSlDQbjH2Rtdg+A==
//                Shared Secret: Qi1iDW+YLe5W60waylHdE45bhmDqEJHxQqQDCg09Ep0=

        // Perform ECDH key agreement
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKeyFromPem, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // Print the shared secret
        System.out.println("Shared Secret: " + Base64.getEncoder().encodeToString(sharedSecret));
        System.out.println(System.getProperty("java.runtime.version"));

    }

    @Test
    public void generateSignatureAlgorithm()
    {
        Set<String> algorithms = new HashSet<>();
        for (Provider provider : Security.getProviders()) {
            provider.getServices().stream()
                    .filter(service -> "Signature".equals(service.getType()))
                    .map(Provider.Service::getAlgorithm)
                    .forEach(algorithms::add);
        }
        algorithms.forEach(System.out::println);
        RSAPublicKeySpec ds;
    }

}
