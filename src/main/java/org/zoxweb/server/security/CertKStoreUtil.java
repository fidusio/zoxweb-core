package org.zoxweb.server.security;

import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.shared.util.Const;
import sun.security.x509.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertKStoreUtil {

    public static X509Certificate generateSelfCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException
    {
        PrivateKey privateKey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * Const.TimeInMillis.DAY.MILLIS);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));

        //info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.SHA256_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privateKey, algorithm);
        return cert;
    }

    public static KeyStore generateKeyStore(X509Certificate cert, String  alias, Key key, String ksPassword, String  ksType) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(ksType);
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, key, ksPassword.toCharArray(),  new Certificate[]{cert});
        return keyStore;
    }


    public static KeyStore generateSelfSignedKStore(String keyType,
                                                    int keySizeInBits,
                                                    String commonName,
                                                    String signAlgoName,
                                                    String ksPassword)
            throws GeneralSecurityException, IOException
    {
        KeyPair keyPair = CryptoUtil.generateKeyPair(keyType, keySizeInBits);
        X509Certificate certificate = generateSelfCertificate(commonName, keyPair, 30, signAlgoName);
        return generateKeyStore(certificate, "tempo", keyPair.getPrivate(), ksPassword, CryptoUtil.PKCS12);
    }

    public static SSLContextInfo generateRandomSSLContextInfo(String keyType, String signAlgo) throws GeneralSecurityException, IOException {
        KeyStore ks = CertKStoreUtil.generateSelfSignedKStore(keyType, 256, "C=US, ST=California, L=LA, O=zoxweb, CN=test", signAlgo, "password");
        return new SSLContextInfo(ks, "password".toCharArray(), null, null);
    }

}
