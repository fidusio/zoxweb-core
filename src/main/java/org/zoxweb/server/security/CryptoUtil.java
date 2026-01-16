/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.security;

import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.filters.BytesValueFilter;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.KeyStoreInfo;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CryptoUtil {


    private static final Lock LOCK = new ReentrantLock();
    //private static final Logger  log = Logger.getLogger(CryptoUtil.class.getName());


    public static final int MIN_KEY_BYTES = 6;

    public static final int DEFAULT_ITERATION = 8196;


    public static String base64URLHmacSHA256(String secret, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] hmac = hmacSHA256(SharedStringUtil.getBytes(secret), SharedStringUtil.getBytes(data));
        return SharedStringUtil.toString(SharedBase64.encode(Base64Type.URL, hmac, 0, hmac.length));
    }

    public static byte[] hmacSHA256(byte[] secret, byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256HMAC = HashUtil.getMac(CryptoConst.SignatureAlgo.HMAC_SHA_256);
        SecretKeySpec secret_key = new SecretKeySpec(secret, CryptoConst.SignatureAlgo.HMAC_SHA_256.getName());
        sha256HMAC.init(secret_key);
        return sha256HMAC.doFinal(data);
    }


    public static EncapsulatedKey rekeyEncryptedKey(final EncapsulatedKey toBeRekeyed,
                                                    String originalKey, String newKey)
            throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        SUS.checkIfNulls("Null parameter", originalKey, toBeRekeyed, newKey);
        return rekeyEncryptedKey(toBeRekeyed, SharedStringUtil.getBytes(originalKey),
                SharedStringUtil.getBytes(newKey));
    }

    public static EncapsulatedKey rekeyEncryptedKey(final EncapsulatedKey toBeRekeyed,
                                                    final byte[] originalKey, final byte[] newKey)
            throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        SUS.checkIfNulls("Null parameter", originalKey, toBeRekeyed, newKey);
        byte[] decyptedKey = decryptEncryptedData(toBeRekeyed, originalKey);

        return (EncapsulatedKey) encryptData(toBeRekeyed, newKey, decyptedKey);
    }

    public static EncapsulatedKey createEncryptedKey(String key)
            throws NullPointerException,
            IllegalArgumentException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return createEncryptedKey(SharedStringUtil.getBytes(key));
    }

    public static EncapsulatedKey createEncryptedKey(final byte[] key)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {

        return (EncapsulatedKey) encryptData(new EncapsulatedKey(), key, null);
    }


    public static EncryptedData encryptData(final EncryptedData ekd, final byte[] key, byte[] data)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return encryptData(ekd, key, data, DEFAULT_ITERATION);
    }

    public static EncryptedData encryptData(final EncryptedData ekd, final byte[] key, byte[] data, int hashIteration)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {

        SUS.checkIfNulls("Null key", key, ekd);

        if (key.length < MIN_KEY_BYTES || hashIteration < 1) {
            throw new IllegalArgumentException(
                    "Key too short " + key.length * Byte.SIZE + "(bits) min size " + Const.TypeInBytes.BYTE
                            .sizeInBits(MIN_KEY_BYTES) + "(bits)" + " hash iteration " + hashIteration);
        }

        //EncryptedData ret = ekd ;
        ekd.setName(CryptoConst.CryptoAlgo.AES.getName() + "-" + Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE));
        ekd.setDescription(CryptoConst.AES_ENCRYPTION_CBC_NO_PADDING);
        ekd.setHMACAlgoName(CryptoConst.SignatureAlgo.HMAC_SHA_256.getName());

        // create iv vector
        MessageDigest digest = HashUtil.getMessageDigest(CryptoConst.HashType.SHA_256);
        //IvParameterSpec ivSpec = new IvParameterSpec(generateRandomHashedBytes(digest, AES_BLOCK_SIZE, DEFAULT_ITERATION));
        IvParameterSpec ivSpec = new IvParameterSpec(
                generateKey(CryptoConst.CryptoAlgo.AES, (Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE) / 2)).getEncoded());
        SecretKeySpec aesKey = new SecretKeySpec(
                hashWithIterations(digest, ivSpec.getIV(), key, hashIteration, true), CryptoConst.CryptoAlgo.AES.getName());
        Cipher cipher = Cipher.getInstance(CryptoConst.AES_ENCRYPTION_CBC_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        Mac hmac = HashUtil.getMac(CryptoConst.SignatureAlgo.HMAC_SHA_256);
        hmac.init(new SecretKeySpec(aesKey.getEncoded(), CryptoConst.SignatureAlgo.HMAC_SHA_256.getName()));
        // the initialization vector first
        hmac.update(ivSpec.getIV());

        hmac.update(SharedStringUtil.getBytes(ekd.getName().toLowerCase()));
        hmac.update(SharedStringUtil.getBytes(ekd.getDescription().toLowerCase()));
        hmac.update(SharedStringUtil.getBytes(ekd.getHMACAlgoName().toLowerCase()));
        if (ekd.isHMACAll()) {
            if (!SUS.isEmpty(ekd.getSubjectGUID())) {
                hmac.update(SharedStringUtil.getBytes(ekd.getSubjectGUID()));
            }

            if (!SUS.isEmpty(ekd.getGUID())) {
                hmac.update(
                        SharedStringUtil.getBytes(SharedStringUtil.toTrimmedLowerCase(ekd.getGUID())));
            }
        }

        if (data == null) {
            data = generateKey(CryptoConst.CryptoAlgo.AES, Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE)).getEncoded();
        }

        ekd.setDataLength(data.length);
        hmac.update(BytesValueFilter.SINGLETON.validate(ekd.getDataLength()));

        // create a new key and encrypted with the key

        ekd.setIV(ivSpec.getIV());

        // create a loop to read the data in the size of 16 bytes
        // write the output to a byteoputput stream

        if (data.length % CryptoConst.AES_BLOCK_SIZE != 0 || data.length == 0) {
            UByteArrayOutputStream baos = new UByteArrayOutputStream();
            baos.write(data);

            while ((baos.size() % CryptoConst.AES_BLOCK_SIZE) != 0 || baos.size() == 0) {
                // padding
                // instead of zero
                // add the size
                baos.write(baos.size());
            }

            IOUtil.close(baos);
            data = baos.toByteArray();
        }

        //byte[] encryptedData = ;//(data != null ? data : generateKey(AES_256_KEY_SIZE, AES).getEncoded());
        //byte[] encryptionKey = (data != null ? data : generateRandomBytes(null, AES_256_KEY_SIZE/8));
        byte[] encryptedData = cipher.doFinal(data);
        hmac.update(encryptedData);

        // last
        ekd.setHMAC(hmac.doFinal());
        ekd.setEncryptedData(encryptedData);
        return ekd;
    }


    public static byte[] decryptEncryptedData(final EncryptedData ekd, final String key)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        return decryptEncryptedData(ekd, SharedStringUtil.getBytes(key));
    }


    public static byte[] decryptEncryptedData(final EncryptedData ekd, final String key, int hashIteration)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        return decryptEncryptedData(ekd, SharedStringUtil.getBytes(key), hashIteration);
    }


    public static byte[] decryptEncryptedData(final EncryptedData ekd, final byte[] key)
            throws InvalidKeyException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        return decryptEncryptedData(ekd, key, DEFAULT_ITERATION);
    }


    public static byte[] decryptEncryptedData(final EncryptedData ekd, final byte[] key, int hashIteration)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        // create iv vector
        MessageDigest digest = HashUtil.getMessageDigest(CryptoConst.HashType.SHA_256);
        IvParameterSpec ivSpec = new IvParameterSpec(ekd.getIV());
        SecretKeySpec aesKey = new SecretKeySpec(
                hashWithIterations(digest, ivSpec.getIV(), key, hashIteration, true), CryptoConst.CryptoAlgo.AES.getName());
        Cipher cipher = Cipher.getInstance(CryptoConst.AES_ENCRYPTION_CBC_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        Mac hmac = HashUtil.getMac(CryptoConst.SignatureAlgo.HMAC_SHA_256.getName());
        hmac.init(new SecretKeySpec(aesKey.getEncoded(), CryptoConst.SignatureAlgo.HMAC_SHA_256.getName()));
        hmac.update(ivSpec.getIV());
        // create a new key and encrypted with the key
        hmac.update(SharedStringUtil.getBytes(ekd.getName().toLowerCase()));
        hmac.update(SharedStringUtil.getBytes(ekd.getDescription().toLowerCase()));
        hmac.update(SharedStringUtil.getBytes(ekd.getHMACAlgoName().toLowerCase()));
        if (ekd.isHMACAll()) {
            if (!SUS.isEmpty(ekd.getSubjectGUID())) {
                hmac.update(SharedStringUtil.getBytes(ekd.getSubjectGUID()));
            }

            if (!SUS.isEmpty(ekd.getGUID())) {
                hmac.update(
                        SharedStringUtil.getBytes(SharedStringUtil.toTrimmedLowerCase(ekd.getGUID())));
            }
        }

        hmac.update(BytesValueFilter.SINGLETON.validate(ekd.getDataLength()));

        hmac.update(ekd.getEncryptedData());

        if (!SUS.slowEquals(ekd.getHMAC(), hmac.doFinal())) {
            throw new SignatureException("Data tempered with");
        }

        byte[] decryptedData = cipher.doFinal(ekd.getEncryptedData());
        byte[] toRet = decryptedData;

        if (decryptedData.length != ekd.getDataLength()) {
            // we must truncate the data
            toRet = new byte[(int) ekd.getDataLength()];
            System.arraycopy(decryptedData, 0, toRet, 0, toRet.length);
        }

        return toRet;
    }

    public static Key getKeyFromKeyStore(final InputStream keyStoreIS,
                                         String keyStoreType,
                                         String keystorePass,
                                         String alias,
                                         String aliasPass)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        KeyStore keystore = loadKeyStore(keyStoreIS, keyStoreType, keystorePass.toCharArray());

        if (!keystore.containsAlias(alias)) {
            throw new IllegalArgumentException("Alias for key not found");
        }
        return getKeyFromKeyStore(keystore, alias, aliasPass);
    }

    public static Key getKeyFromKeyStore(KeyStore ks, String alias, String aliasPassword)
            throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return ks.getKey(alias, aliasPassword != null ? aliasPassword.toCharArray() : null);
    }

//  public static SSLContext initSSLContext(String keyStoreFilename,
//                                          String keyStoreType,
//                                          final char[] keyStorePassword,
//                                          final char[] crtPassword,
//                                          String trustStoreFilename,
//                                          final char[] trustStorePassword)
//      throws GeneralSecurityException, IOException {
//
//    return initSSLContext("TLS", null, new File(keyStoreFilename),
//            keyStoreType,
//            keyStorePassword,
//            crtPassword,
//            trustStoreFilename != null ? new File(trustStoreFilename) : null, trustStorePassword);
//
//  }
//
//
//  public static SSLContext initSSLContext(String protocol,
//                                          final Provider provider,
//                                          final File keyStoreFilename,
//                                          String keyStoreType,
//                                          final char[] keyStorePassword,
//                                          final char[] crtPassword,
//                                          final File trustStoreFilename,
//                                          final char[] trustStorePassword)
//          throws GeneralSecurityException, IOException {
//    FileInputStream ksfis = null;
//    FileInputStream tsfis = null;
//
//    try {
//      ksfis = new FileInputStream(keyStoreFilename);
//      tsfis = trustStoreFilename != null ? new FileInputStream(trustStoreFilename) : null;
//      return initSSLContext(protocol, provider, ksfis, keyStoreType, keyStorePassword, crtPassword, tsfis,trustStorePassword);
//    } finally {
//      IOUtil.close(ksfis);
//      IOUtil.close(tsfis);
//    }
//
//  }
//
//  public static SSLContext initSSLContext(String protocol,
//                                          final Provider provider,
//                                          final InputStream keyStoreIS,
//                                          String keyStoreType,
//                                          final char[] keyStorePassword,
//                                          final char[] crtPassword,
//                                          final InputStream trustStoreIS,
//                                          final char[] trustStorePassword)
//      throws GeneralSecurityException, IOException {
//    KeyStore ks = CryptoUtil.loadKeyStore(keyStoreIS, keyStoreType, keyStorePassword);
//    KeyStore ts = null;
//    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//
//    if (trustStoreIS != null) {
//      ts = CryptoUtil.loadKeyStore(trustStoreIS, keyStoreType, trustStorePassword);
//    }
//
//    if (crtPassword != null) {
//      kmf.init(ks, crtPassword);
//      tmf.init(ts != null ? ts : ks);
//    } else {
//      kmf.init(ks, keyStorePassword);
//      tmf.init(ts != null ? ts : ks);
//    }
//
//    SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
//    sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
//    return sslContext;
//  }
//
//  public static SSLContext initSSLContext(final String protocol,
//                                          final Provider provider,
//                                          final KeyStore keyStore,
//                                          final char[] keyStorePassword,
//                                          final char[] crtPassword,
//                                          final KeyStore trustStore)
//          throws GeneralSecurityException
//  {
//
//
//    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//
//
//
//    if (crtPassword != null) {
//      kmf.init(keyStore, crtPassword);
//      tmf.init(keyStore != null ? trustStore : keyStore);
//    } else {
//      kmf.init(keyStore, keyStorePassword);
//      tmf.init(trustStore != null ? trustStore : keyStore);
//    }
//
//    SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
//    sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
//    return sslContext;
//  }

    public static void updateKeyPasswordInKeyStore(final InputStream keyStoreIS,
                                                   String keyStoreType,
                                                   String keystorePass,
                                                   String alias,
                                                   String keyPass,
                                                   final OutputStream keyStoreOS,
                                                   String newKeystorePass,
                                                   String newAlias,
                                                   String newKeyPass)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        try {

            KeyStore keystore = loadKeyStore(keyStoreIS, keyStoreType, keystorePass.toCharArray());

            if (!keystore.containsAlias(alias)) {
                throw new IllegalArgumentException("Alias for key not found");
            }

            Key key = keystore.getKey(alias, keyPass.toCharArray());
            keystore.deleteEntry(alias);
            keystore.setKeyEntry(newAlias, key, newKeyPass.toCharArray(), null);
            keystore.store(keyStoreOS, newKeystorePass.toCharArray());

        } finally {
            IOUtil.close(keyStoreOS);
        }
    }

    public static KeyStore createKeyStore(String keyStoreFilename, String keyStoreType, String keyStorePass)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        return createKeyStore(new File(keyStoreFilename), keyStoreType, keyStorePass, false);
    }

    public static KeyStore createKeyStore(final File keyStoreFile, String keyStoreType,
                                          String keyStorePass, final boolean fileOverride)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        OutputStream os = null;

        if (keyStoreFile.exists()) {
            if (!fileOverride) {
                throw new IllegalArgumentException("File already exist");
            }
        } else {
            keyStoreFile.createNewFile();
        }

        try {
            os = new FileOutputStream(keyStoreFile);
            return createKeyStore(os, keyStoreType, keyStorePass);
        } finally {
            IOUtil.close(os);
        }
    }


    public static KeyStore createKeyStore(final OutputStream keyStoreOS, String keyStoreType,
                                          String keyStorePass)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ret = KeyStore.getInstance(keyStoreType);

        try {
            ret.store(keyStoreOS, keyStorePass.toCharArray());
        } finally {
            IOUtil.close(keyStoreOS);
        }

        return ret;
    }


    public static KeyStore loadKeyStore(final String filename, String keyStoreType, final char[] keyStorePassword)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        return loadKeyStore(Files.newInputStream(Paths.get(filename)), keyStoreType, keyStorePassword);
    }

    public static KeyStore loadKeyStore(final InputStream keyStoreIS, String keyStoreType,
                                        final char[] keyStorePassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        try {
            if (keyStoreType == null) {
                keyStoreType = CryptoConst.KEY_STORE_TYPE;
            }
            KeyStore keystore = KeyStore.getInstance(keyStoreType);
            keystore.load(keyStoreIS, keyStorePassword);
            return keystore;
        } finally {

            IOUtil.close(keyStoreIS);
        }
    }

    public static byte[] hashWithIterations(MessageDigest digest,
                                            byte[] salt,
                                            byte[] data,
                                            int hashIterations,
                                            boolean reChewData) {
        // reset the digest
        digest.reset();

        if (salt != null) {
            // insert the salt
            digest.update(salt);
        }

        // process the data
        byte[] hashed = digest.digest(data);
        int iterations = hashIterations - 1; //already hashed once above
        //iterate remaining number:
        for (int i = 0; i < iterations; i++) {
            digest.reset();
            digest.update(hashed);

            if (reChewData) {
                digest.update(data);
            }

            hashed = digest.digest();
        }
        return hashed;
    }


    public static byte[] generateRandomHashedBytes(MessageDigest digest,
                                                   int arraySize,
                                                   int hashIteration)
            throws NoSuchAlgorithmException {
        SecureRandom random = SecUtil.defaultSecureRandom();

        byte[] bytes = SecUtil.generateRandomBytes(random, arraySize);

        digest.reset();
        digest.update(bytes);
        for (int i = 0; i < hashIteration; i++) {
            random.nextBytes(bytes);
            digest.update(bytes);
        }

        System.arraycopy(digest.digest(), 0, bytes, 0, bytes.length);

        return bytes;
    }


    public static PublicKey generatePublicKey(String type, String publicKey)
            throws GeneralSecurityException {
        String publicKeyPEM = CryptoConst.applyPemFilters(publicKey);//SharedStringUtil.filterString(publicKey, "BEGIN PUBLIC KEY", "END PUBLIC KEY", "-", "\n");
        // Use Base64Type.DEFAULT DO NOT USE Base64Type.URL because of - char
        return generatePublicKey(type, SharedBase64.decode(Base64Type.DEFAULT, publicKeyPEM));
    }

    public static PublicKey generatePublicKey(String type, byte[] keys)
            throws GeneralSecurityException {
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(keys);
        KeyFactory keyFactory = KeyFactory.getInstance(type);
        return keyFactory.generatePublic(publicSpec);
    }

    public static KeyPair toKeyPair(String type, String provider, String pubKeyBase64, String privKeyBase64) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return toKeyPair(type,
                provider,
                SharedBase64.decode(CryptoConst.applyPemFilters(pubKeyBase64)),
                SharedBase64.decode(CryptoConst.applyPemFilters(privKeyBase64)));
    }

    public static KeyPair toKeyPair(String type, String provider, byte[] pubKey, byte[] privKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

        KeyFactory kf = KeyFactory.getInstance(type, provider);

        EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKey);
        EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKey);

        PublicKey regeneratedPublicKey = kf.generatePublic(pubKeySpec);
        PrivateKey regeneratedPrivateKey = kf.generatePrivate(privKeySpec);
        return new KeyPair(regeneratedPublicKey, regeneratedPrivateKey);
    }


    public static PublicKey convertRSAJwkToPublicKey(String n, String e) {
        try {
            // Base64 decode the values
            byte[] decodedN = SharedBase64.decode(Base64Type.URL, n);
            byte[] decodedE = SharedBase64.decode(Base64Type.URL, e);

            BigInteger modulus = new BigInteger(1, decodedN);
            BigInteger exponent = new BigInteger(1, decodedE);

            // Use the RSA key spec to generate the key
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException("Failed to convert JWK to PublicKey", ex);
        }
    }

//  public static PublicKey convertECJwkToPublicKey(String x, String y) {
//    try {
//      // Base64 decode the values
//      byte[] decodedX = SharedBase64.decode(Base64Type.URL, x);
//      byte[] decodedY = SharedBase64.decode(Base64Type.URL, y);
//
//      BigInteger bigIntegerX = new BigInteger(1, decodedX);
//      BigInteger bigIntegerY = new BigInteger(1, decodedY);
//
//      // Create the ECPoint from the X and Y coordinates
//      ECPoint ecPoint = new ECPoint(bigIntegerX, bigIntegerY);
//
//      // Use the P-256 curve parameters. Java refers to P-256 as secp256r1.
//      ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec("secp256r1");
//
//
//
//      AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
//      parameters.init(new java.security.spec.NamedParameterSpec("secp256r1"));
//      ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
//      // Use the EC key spec to generate the key
//      ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
//      KeyFactory kf = KeyFactory.getInstance("EC");
//
//      return kf.generatePublic(keySpec);
//    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
//      throw new RuntimeException("Failed to convert JWK to PublicKey", ex);
//    }
//  }

    public static PrivateKey generatePrivateKey(String type, byte[] keys)
            throws GeneralSecurityException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keys);
        KeyFactory keyFactory = KeyFactory.getInstance(type);
        return keyFactory.generatePrivate(keySpec);
    }


    public static String encodeJWT(String key, JWT jwt)
            throws GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
        return encodeJWT(key, jwt, false);
    }

    public static String encodeJWT(String key, JWT jwt, boolean setHash)
            throws GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
        return encodeJWT(key != null ? SharedStringUtil.getBytes(key) : null, jwt, setHash);
    }

    public static String encodeJWT(byte[] key, JWT jwt)
            throws GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
        return encodeJWT(key, jwt, false);
    }

    public static String encodeJWT(byte[] key, JWT jwt, boolean setHash)
            throws IOException,
            SecurityException, GeneralSecurityException {
        SUS.checkIfNulls("Null jwt", jwt);
        SUS.checkIfNulls("Null jwt header", jwt.getHeader());
        SUS.checkIfNulls("Null jwt algorithm", jwt.getHeader().getJWTAlgorithm());

        StringBuilder sb = new StringBuilder();
        byte[] b64Header = SharedBase64.encode(Base64Type.URL,
                GSONUtil.toJSONGenericMap(jwt.getHeader().getProperties(), false, false, false));
        String payloadJSON = GSONUtil
                .toJSONGenericMap(jwt.getPayload().getProperties(), false, false, false);
        //System.out.println(payloadJSON);
        byte[] b64Payload = SharedBase64.encode(Base64Type.URL, payloadJSON);
        sb.append(SharedStringUtil.toString(b64Header));
        sb.append(".");
        sb.append(SharedStringUtil.toString(b64Payload));

        String b64Hash = null;

        CryptoConst.JWTAlgo jwtAlgo = jwt.getHeader().getJWTAlgorithm();
        switch (jwtAlgo) {
            case HS256:
            case HS384:
            case HS512:
                SUS.checkIfNulls("Null key", key);
                Mac hmac = HashUtil.getMac(jwtAlgo.getSignatureAlgo());
                SecretKeySpec secret_key = new SecretKeySpec(key, jwtAlgo.getSignatureAlgo().getName());
                hmac.init(secret_key);
                b64Hash = SharedBase64.encodeAsString(Base64Type.URL,
                        hmac.doFinal(SharedStringUtil.getBytes(sb.toString())));
                break;
            case none:
                break;
            case RS256:
            case RS384:
            case RS512:
            case ES256:
            case ES384:
            case ES512:
                SUS.checkIfNulls("Null key", key);
                PrivateKey privateKey = CryptoUtil.generatePrivateKey(jwtAlgo.getSignatureAlgo().getCryptoAlgo().getName(), key);
                b64Hash = SharedBase64.encodeAsString(Base64Type.URL,
                        CryptoUtil.sign(jwtAlgo.getSignatureAlgo(),
                                privateKey,
                                SharedStringUtil.getBytes(sb.toString())));
                break;
        }

        sb.append(".");

        if (b64Hash != null) {
            sb.append(b64Hash);
            if (setHash)
                jwt.setHash(b64Hash);
        }

        return sb.toString();
    }


    public static SecretKey generateKey(CryptoConst.CryptoAlgo type, int keySizeInBits)
            throws NoSuchAlgorithmException {
        return generateKey(type.getName(), keySizeInBits);
    }


    public static SecretKey generateKey(String type, int keySizeInBits)
            throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance(type);
        //kg.init(keySizeInBits, (SecureRandom)defaultSecureRandom());
        kg.init(keySizeInBits);
        return kg.generateKey();
    }

    public static SecretKey toSecretKey(byte[] key, String algoName) {
        return new SecretKeySpec(key, algoName);
    }


    public static KeyPair generateKeyPair(String type, int keySizeInBits)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kg = KeyPairGenerator.getInstance(type);
        kg.initialize(keySizeInBits);//, (SecureRandom)defaultSecureRandom());
        return kg.generateKeyPair();
    }


    public static KeyPair generateKeyPair(String keyCanonicalID)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        return generateKeyPair(keyCanonicalID, null, SecUtil.defaultSecureRandom());
    }

    public static KeyPair generateKeyPair(CanonicalID keyCanonicalID, String provider, SecureRandom sr)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        return generateKeyPair(keyCanonicalID.toCanonicalID(), provider, sr);
    }

    public static KeyPair generateKeyPair(String keyCanonicalID, String provider, SecureRandom sr)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {

        CryptoConst.PKInfo pkInfo = CryptoConst.PKInfo.parse(keyCanonicalID);

        if (sr == null)
            sr = SecUtil.defaultSecureRandom(); // get the default secure random
        KeyPairGenerator keyPairGenerator = provider != null ? KeyPairGenerator.getInstance(pkInfo.getType(), provider) : KeyPairGenerator.getInstance(pkInfo.getType());
        RSAKeyGenParameterSpec h;

        if ("RSA".equals(pkInfo.getType())) {
            keyPairGenerator.initialize(Integer.parseInt(pkInfo.getName()), sr);
        } else if ("EC".equals(pkInfo.getType())) {
            keyPairGenerator.initialize(new ECGenParameterSpec(pkInfo.getName()), sr);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + keyCanonicalID);
        }

        return keyPairGenerator.generateKeyPair();
    }

    public static KeyPair generateKeyPair(String type, String provider, AlgorithmParameterSpec keySpec, SecureRandom random) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(type, provider);
        kpg.initialize(keySpec, random != null ? random : SecUtil.defaultSecureRandom());
        return kpg.generateKeyPair();
    }

    public static byte[] encrypt(PublicKey receiver, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, ShortBufferException {
        return encrypt(receiver, receiver.getAlgorithm(), data);
    }


    public static byte[] encrypt(PublicKey receiver, String cipherName, byte[] data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, ShortBufferException {
        Cipher cipher = Cipher.getInstance(cipherName);
        cipher.init(Cipher.ENCRYPT_MODE, receiver);
        return cipher.doFinal(data);

    }

    public static byte[] decrypt(PrivateKey sender, byte[] data)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        return decrypt(sender, sender.getAlgorithm(), null, data);
    }

    public static byte[] decrypt(PrivateKey sender, String cipherName, AlgorithmParameters algParameters, byte[] data)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(cipherName);
        if (algParameters != null)
            cipher.init(Cipher.DECRYPT_MODE, sender, algParameters);
        else
            cipher.init(Cipher.DECRYPT_MODE, sender);
        return cipher.doFinal(data);
    }


    public static byte[] sign(CryptoConst.SignatureAlgo sa, PrivateKey pk, byte[] data)
            throws GeneralSecurityException {
        SecureRandom secureRandom = new SecureRandom();
        Signature signature = Signature.getInstance(sa.getName());
        signature.initSign(pk, secureRandom);
        signature.update(data);
        return signature.sign();
    }

    public static boolean verify(CryptoConst.SignatureAlgo sa, PublicKey pk, byte[] data, byte[] signedData)
            throws NoSuchAlgorithmException,
            InvalidKeyException,
            SignatureException {
        Signature signature = Signature.getInstance(sa.getName());
        signature.initVerify(pk);
        signature.update(data);
        return signature.verify(signedData);
    }


    public static String toString(Key key) {
        return SharedUtil
                .toCanonicalID(':', key.getAlgorithm(), key.getEncoded().length, key.getFormat(),
                        SharedStringUtil.bytesToHex(key.getEncoded()));
    }

    public static KeyStoreInfo generateKeyStoreInfo(String keyStoreName, String alias,
                                                    String keyStoreType) throws NoSuchAlgorithmException {
        KeyStoreInfo ret = new KeyStoreInfo();
        ret.setKeyStore(keyStoreName);
        ret.setAlias(alias);
        ret.setKeyStorePassword(generateKey(CryptoConst.CryptoAlgo.AES, CryptoConst.AES_256_KEY_SIZE * 8).getEncoded());
        if (CryptoConst.PKCS12.equalsIgnoreCase(keyStoreType)) {
            ret.setAliasPassword(ret.getKeyStorePassword());
        } else {
            ret.setAliasPassword(generateKey(CryptoConst.CryptoAlgo.AES, CryptoConst.AES_256_KEY_SIZE * 8).getEncoded());
        }
        ret.setKeyStoreType(keyStoreType);
        return ret;
    }

    /**
     * Connect to a remote host and extract the public key
     */
    public static PublicKey getRemotePublicKey(String url)
            throws IOException {
        Certificate[] certs = getRemoteCertificates(url);
        return certs[0].getPublicKey();

    }


    public static Certificate[] getRemoteCertificates(String url) throws IOException {
        SSLSocket socket = null;
        try {
            IPAddress address = HTTPUtil.parseHost(url, 443);
            SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();
            socket = (SSLSocket) factory.createSocket(address.getInetAddress(), address.getPort());
            socket.startHandshake();
            return socket.getSession().getPeerCertificates();
        } finally {
            IOUtil.close(socket);
        }
    }

    public static NVGenericMap publicKeyToNVGM(PublicKey pk) {

        NVGenericMap ret = new NVGenericMap();
        System.out.println(pk);
        ret.add("algorithm", pk.getAlgorithm());
        ret.add("format", pk.getFormat());

        ret.add(new NVInt("key_size", pk.getEncoded().length * 8));
        ret.add("key", SharedStringUtil.bytesToHex(pk.getEncoded()));
        return ret;
    }

    public static NVGenericMap certificateToNVGM(X509Certificate cert) {
        NVGenericMap ret = SharedUtil.toNVGenericMap(null, cert.getSubjectX500Principal().getName(), "=", ",", true);
        ret.add("type", cert.getType());
        NVGenericMap nvmg = publicKeyToNVGM(cert.getPublicKey());
        nvmg.setName("public_key");
        ret.add(nvmg);

        return ret;
    }

    public static void main(String... args) {
        try {

            int index = 0;
            String command = args[index++];
            switch (command.toLowerCase()) {
                case "generate":
                    System.out.println(GSONUtil
                            .toJSON(CryptoUtil.generateKeyStoreInfo(args[index++], args[index++], args[index++]),
                                    true, false, false));
                    break;
                case "read":
                    String keystoreName = args[index++];
                    String ksType = args[index++];
                    String ksPassword = args[index++];
                    String alias = args[index++];
                    String aliasPassword = args.length > index ? args[index++] : null;

                    KeyStore keystore = CryptoUtil
                            .loadKeyStore(new FileInputStream(keystoreName), ksType, ksPassword.toCharArray());
                    Key key = CryptoUtil.getKeyFromKeyStore(keystore, alias, aliasPassword);
                    System.out.println(
                            "algo:" + key.getAlgorithm() + " format:" + key.getFormat() + " size:" + (
                                    key.getEncoded().length * 8) + " in bits  key:" + SharedBase64
                                    .encodeAsString(Base64Type.DEFAULT, key.getEncoded()));
                    break;
                default:
                    throw new Exception();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("usage:\n"
                    + "read keystore keystoreType keyStorePassword alias [aliasPassword]\n"
                    + "generate keystore keystoreType keyStorePassword ");
        }
    }


}
