/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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
import org.zoxweb.shared.crypto.CryptoConst.SecureRandomType;
import org.zoxweb.shared.crypto.EncryptedDAO;
import org.zoxweb.shared.crypto.EncryptedKeyDAO;
import org.zoxweb.shared.filters.BytesValueFilter;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWT.JWTField;
import org.zoxweb.shared.security.JWTHeader;
import org.zoxweb.shared.security.JWTPayload;
import org.zoxweb.shared.security.KeyStoreInfo;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
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

  /**
   * Name of the secure random algorithm
   */
  public static SecureRandomType SECURE_RANDOM_ALGO = null;


  private static final Lock LOCK = new ReentrantLock();
  //private static final Logger  log = Logger.getLogger(CryptoUtil.class.getName());


  public static final int MIN_KEY_BYTES = 6;

  public static final int DEFAULT_ITERATION = 8196;

  //public static final int SALT_LENGTH = 32;

  public static byte[] generateRandomBytes(SecureRandom sr, int size)
      throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
    if (size < 1) {
      throw new IllegalArgumentException("invalid size " + size + " must be greater than zero.");
    }

    if (sr == null) {
      sr = defaultSecureRandom();
    }

    byte[] ret = new byte[size];
    sr.nextBytes(ret);

    return ret;
  }


  public static SecureRandom newSecureRandom(SecureRandomType srt)
      throws NoSuchAlgorithmException {
    switch (srt) {
      case SECURE_RANDOM_VM_STRONG:
        // very bad and blocking on linux
        // not recommended yet
        return SecureRandom.getInstanceStrong();
      case SECURE_RANDOM_VM_DEFAULT:
        return new SecureRandom();
      case NATIVE:
        return SecureRandom.getInstance(SecureRandomType.NATIVE.getName());
      case SHA1PRNG:
        return SecureRandom.getInstance(SecureRandomType.SHA1PRNG.getName());
      default:
        return SecureRandom.getInstance(SECURE_RANDOM_ALGO.getName());
    }
  }


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

  public static SecureRandom defaultSecureRandom()
      throws NoSuchAlgorithmException {
    if (SECURE_RANDOM_ALGO == null) {
      try {
        LOCK.lock();

        if (SECURE_RANDOM_ALGO == null) {
          for (SecureRandomType srt : SecureRandomType.values()) {
            try {
              newSecureRandom(srt);
              SECURE_RANDOM_ALGO = srt;
              //System.out.println("Default secure algorithm:"+srt);
              break;
            } catch (NoSuchAlgorithmException e) {
              //e.printStackTrace();
            }
          }
        }
      } finally {
        LOCK.unlock();
      }
    }

    return newSecureRandom(SECURE_RANDOM_ALGO);
  }

//  public static PasswordDAO hashedPassword(String algo, int saltLength, int saltIteration,
//      String password)
//      throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
//    SharedUtil.checkIfNulls("Null parameter", algo, password);
//    return hashedPassword(MDType.lookup(algo), saltLength, saltIteration, password);
//  }
//
//  public static PasswordDAO hashedPassword(MDType algo, int saltLength, int saltIteration,
//      String password)
//      throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
//    SharedUtil.checkIfNulls("Null parameter", algo, password);
//    byte[] paswd = SharedStringUtil.getBytes(password);
//
//    return hashedPassword(algo, saltLength, saltIteration, paswd);
//  }

//  public static PasswordDAO mergeContent(PasswordDAO password, PasswordDAO toMerge) {
//    synchronized (password) {
//      password.setName(toMerge.getName());
//      password.setHashIteration(toMerge.getHashIteration());
//      password.setSalt(toMerge.getSalt());
//      password.setPassword(toMerge.getPassword());
//    }
//
//    return password;
//  }

//  public static PasswordDAO hashedPassword(MDType algo, int saltLength, int saltIteration,
//      byte[] password)
//      throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
//    SharedUtil.checkIfNulls("Null parameter", algo, password);
//    if (password.length < 6) {
//      throw new IllegalArgumentException("password length too short");
//    }
//
//    // Generate a random salt
//    SecureRandom random = defaultSecureRandom();
//
//    if (saltLength < SALT_LENGTH) {
//      saltLength = SALT_LENGTH;
//    }
//
//    if (saltIteration < 0) {
//      saltIteration = 0;
//    }
//
//    byte[] salt = new byte[saltLength];
//    random.nextBytes(salt);
//    MessageDigest md = MessageDigest.getInstance(algo.getName());
//    PasswordDAO passwordDAO = new PasswordDAO();
//    passwordDAO.setSalt(salt);
//    passwordDAO.setPassword(hashWithIterations(md, salt, password, saltIteration, false));
//    passwordDAO.setHashIteration(saltIteration);
//    passwordDAO.setName(algo);
//
//    return passwordDAO;
//  }

//  public static boolean isPasswordValid(PasswordDAO passwordDAO, String password)
//      throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
//    SharedUtil.checkIfNulls("Null values", passwordDAO, password);
//    byte[] genHash = hashWithIterations(MessageDigest.getInstance(passwordDAO.getName()),
//        passwordDAO.getSalt(), SharedStringUtil.getBytes(password), passwordDAO.getHashIteration(),
//        false);
//
//    return SharedUtil.slowEquals(genHash, passwordDAO.getPassword());
//  }
//
//  public static void validatePassword(final PasswordDAO passwordDAO, String password)
//      throws NullPointerException, IllegalArgumentException, AccessException {
//    SharedUtil.checkIfNulls("Null values", passwordDAO, password);
//    validatePassword(passwordDAO, password.toCharArray());
//  }
//
//
//  public static void validatePassword(final PasswordDAO passwordDAO, final char[] password)
//      throws NullPointerException, IllegalArgumentException, AccessException {
//
//    SharedUtil.checkIfNulls("Null values", passwordDAO, password);
//
//    try
//    {
//      if(isPasswordValid(passwordDAO, new String(password)))
//        return; // we hava a valid password
//    } catch (NoSuchAlgorithmException e) {
//      //e.printStackTrace();
//      throw new AccessException("Invalid Credentials");
//    }
//    // password validation failed,
//    throw new AccessException("Invalid Credentials");
//
//  }

  public static EncryptedKeyDAO rekeyEncrytedKeyDAO(final EncryptedKeyDAO toBeRekeyed,
      String originalKey, String newKey)
      throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    SharedUtil.checkIfNulls("Null parameter", originalKey, toBeRekeyed, newKey);
    return rekeyEncrytedKeyDAO(toBeRekeyed, SharedStringUtil.getBytes(originalKey),
        SharedStringUtil.getBytes(newKey));
  }

  public static EncryptedKeyDAO rekeyEncrytedKeyDAO(final EncryptedKeyDAO toBeRekeyed,
      final byte[] originalKey, final byte[] newKey)
      throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    SharedUtil.checkIfNulls("Null parameter", originalKey, toBeRekeyed, newKey);
    byte[] decyptedKey = decryptEncryptedDAO(toBeRekeyed, originalKey);

    return (EncryptedKeyDAO) encryptDAO(toBeRekeyed, newKey, decyptedKey);
  }

  public static EncryptedKeyDAO createEncryptedKeyDAO(String key)
      throws NullPointerException,
      IllegalArgumentException,
      InvalidKeyException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {
    return createEncryptedKeyDAO(SharedStringUtil.getBytes(key));
  }

  public static EncryptedKeyDAO createEncryptedKeyDAO(final byte[] key)
      throws NullPointerException,
      IllegalArgumentException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {

    return (EncryptedKeyDAO) encryptDAO(new EncryptedKeyDAO(), key, null);
  }


  public static EncryptedDAO encryptDAO(final EncryptedDAO ekd, final byte[] key, byte[] data)
      throws NullPointerException,
      IllegalArgumentException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {
    return encryptDAO(ekd, key, data, DEFAULT_ITERATION);
  }

  public static EncryptedDAO encryptDAO(final EncryptedDAO ekd, final byte[] key, byte[] data, int hashIteration)
      throws NullPointerException,
      IllegalArgumentException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException {

    SharedUtil.checkIfNulls("Null key", key, ekd);

    if (key.length < MIN_KEY_BYTES || hashIteration < 1) {
      throw new IllegalArgumentException(
          "Key too short " + key.length * Byte.SIZE + "(bits) min size " + Const.TypeInBytes.BYTE
              .sizeInBits(MIN_KEY_BYTES) + "(bits)" + " hash iteration " + hashIteration);
    }

    //EncryptedDAO ret = ekd ;
    ekd.setName(CryptoConst.CryptoAlgo.AES.getName() + "-" + Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE));
    ekd.setDescription(CryptoConst.AES_ENCRYPTION_CBC_NO_PADDING);
    ekd.setHMACAlgoName(CryptoConst.SignatureAlgo.HMAC_SHA_256.getName());

    // create iv vector
    MessageDigest digest = HashUtil.getMessageDigest(CryptoConst.HASHType.SHA_256);
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
      if (!SharedStringUtil.isEmpty(ekd.getSubjectGUID())) {
        hmac.update(SharedStringUtil.getBytes(ekd.getSubjectGUID()));
      }

      if (!SharedStringUtil.isEmpty(ekd.getGUID())) {
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


  public static byte[] decryptEncryptedDAO(final EncryptedDAO ekd, final String key)
      throws NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      SignatureException {
    return decryptEncryptedDAO(ekd, SharedStringUtil.getBytes(key));
  }


  public static byte[] decryptEncryptedDAO(final EncryptedDAO ekd, final String key, int hashIteration)
      throws NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      SignatureException {
    return decryptEncryptedDAO(ekd, SharedStringUtil.getBytes(key), hashIteration);
  }


  public static byte[] decryptEncryptedDAO(final EncryptedDAO ekd, final byte[] key)
      throws InvalidKeyException,
      NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      SignatureException {
    return decryptEncryptedDAO(ekd, key, DEFAULT_ITERATION);
  }


  public static byte[] decryptEncryptedDAO(final EncryptedDAO ekd, final byte[] key, int hashIteration)
      throws NoSuchAlgorithmException,
      NoSuchPaddingException,
      InvalidKeyException,
      InvalidAlgorithmParameterException,
      IllegalBlockSizeException,
      BadPaddingException,
      SignatureException {
    // create iv vector
    MessageDigest digest = HashUtil.getMessageDigest(CryptoConst.HASHType.SHA_256);
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
      if (!SharedStringUtil.isEmpty(ekd.getSubjectGUID())) {
        hmac.update(SharedStringUtil.getBytes(ekd.getSubjectGUID()));
      }

      if (!SharedStringUtil.isEmpty(ekd.getGUID())) {
        hmac.update(
            SharedStringUtil.getBytes(SharedStringUtil.toTrimmedLowerCase(ekd.getGUID())));
      }
    }

    hmac.update(BytesValueFilter.SINGLETON.validate(ekd.getDataLength()));

    hmac.update(ekd.getEncryptedData());

    if (!SharedUtil.slowEquals(ekd.getHMAC(), hmac.doFinal())) {
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

  public static SSLContext initSSLContext(String keyStoreFilename,
                                          String keyStoreType,
                                          final char[] keyStorePassword,
                                          final char[] crtPassword,
                                          String trustStoreFilename,
                                          final char[] trustStorePassword)
      throws GeneralSecurityException, IOException {

    return initSSLContext("TLS", null, new File(keyStoreFilename),
            keyStoreType,
            keyStorePassword,
            crtPassword,
            trustStoreFilename != null ? new File(trustStoreFilename) : null, trustStorePassword);

  }


  public static SSLContext initSSLContext(String protocol,
                                          final Provider provider,
                                          final File keyStoreFilename,
                                          String keyStoreType,
                                          final char[] keyStorePassword,
                                          final char[] crtPassword,
                                          final File trustStoreFilename,
                                          final char[] trustStorePassword)
          throws GeneralSecurityException, IOException {
    FileInputStream ksfis = null;
    FileInputStream tsfis = null;

    try {
      ksfis = new FileInputStream(keyStoreFilename);
      tsfis = trustStoreFilename != null ? new FileInputStream(trustStoreFilename) : null;
      return initSSLContext(protocol, provider, ksfis, keyStoreType, keyStorePassword, crtPassword, tsfis,trustStorePassword);
    } finally {
      IOUtil.close(ksfis);
      IOUtil.close(tsfis);
    }

  }

  public static SSLContext initSSLContext(String protocol,
                                          final Provider provider,
                                          final InputStream keyStoreIS,
                                          String keyStoreType,
                                          final char[] keyStorePassword,
                                          final char[] crtPassword,
                                          final InputStream trustStoreIS,
                                          final char[] trustStorePassword)
      throws GeneralSecurityException, IOException {
    KeyStore ks = CryptoUtil.loadKeyStore(keyStoreIS, keyStoreType, keyStorePassword);
    KeyStore ts = null;
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

    if (trustStoreIS != null) {
      ts = CryptoUtil.loadKeyStore(trustStoreIS, keyStoreType, trustStorePassword);
    }

    if (crtPassword != null) {
      kmf.init(ks, crtPassword);
      tmf.init(ts != null ? ts : ks);
    } else {
      kmf.init(ks, keyStorePassword);
      tmf.init(ts != null ? ts : ks);
    }

    SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
    return sslContext;
  }

  public static SSLContext initSSLContext(final String protocol,
                                          final Provider provider,
                                          final KeyStore keyStore,
                                          final char[] keyStorePassword,
                                          final char[] crtPassword,
                                          final KeyStore trustStore)
          throws GeneralSecurityException
  {


    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");



    if (crtPassword != null) {
      kmf.init(keyStore, crtPassword);
      tmf.init(keyStore != null ? trustStore : keyStore);
    } else {
      kmf.init(keyStore, keyStorePassword);
      tmf.init(trustStore != null ? trustStore : keyStore);
    }

    SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
    return sslContext;
  }

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
          throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException
  {
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
                                          boolean reChewData)
  {
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
    SecureRandom random = defaultSecureRandom();

    byte[] bytes = generateRandomBytes(random, arraySize);

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
          throws GeneralSecurityException
  {
    String publicKeyPEM = SharedStringUtil.filterString(publicKey, "BEGIN PUBLIC KEY", "END PUBLIC KEY", "-", "\n");
    // Use Base64Type.DEFAULT DO NOT USE Base64Type.URL because of - char
    return generatePublicKey(type, SharedBase64.decode(Base64Type.DEFAULT, publicKeyPEM));
  }
  public static PublicKey generatePublicKey(String type, byte[] keys)
      throws GeneralSecurityException
  {
    X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(keys);
    KeyFactory keyFactory = KeyFactory.getInstance(type);
    return  keyFactory.generatePublic(publicSpec);
  }


  public static PublicKey convertRSAJwkToPublicKey(String n, String e) {
    try {
      // Base64 decode the values
      byte[] decodedN = SharedBase64.decode(Base64Type.URL,n);
      byte[] decodedE = SharedBase64.decode(Base64Type.URL,e);

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
    return  keyFactory.generatePrivate(keySpec);
  }


  public static String encodeJWT(String key, JWT jwt)
          throws  GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
    return encodeJWT(key, jwt, false);
  }

  public static String encodeJWT(String key, JWT jwt, boolean setHash)
      throws  GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
    return encodeJWT(key != null ? SharedStringUtil.getBytes(key) : null, jwt, setHash);
  }

  public static String encodeJWT(byte[] key, JWT jwt)
          throws  GeneralSecurityException, IOException, SecurityException, NullPointerException, IllegalArgumentException {
    return encodeJWT(key, jwt, false);
  }
  public static String encodeJWT(byte[] key, JWT jwt, boolean setHash)
      throws   IOException,
      SecurityException, GeneralSecurityException {
    SharedUtil.checkIfNulls("Null jwt", jwt);
    SharedUtil.checkIfNulls("Null jwt header", jwt.getHeader());
    SharedUtil.checkIfNulls("Null jwt algorithm", jwt.getHeader().getJWTAlgorithm());

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
        SharedUtil.checkIfNulls("Null key", key);
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
        SharedUtil.checkIfNulls("Null key", key);
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
      if(setHash)
        jwt.setHash(b64Hash);
    }

    return sb.toString();
  }

  public static JWT decodeJWT(String key, String token)
      throws IOException,
      SecurityException, NullPointerException, IllegalArgumentException, GeneralSecurityException {
    return decodeJWT(key != null ? SharedStringUtil.getBytes(key) : null, token);
  }


  public static JWT decodeJWT(byte[] key, String token)
      throws IOException,
      SecurityException, GeneralSecurityException {

    JWT jwt;
    try {
      jwt = parseJWT(token);
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new SecurityException();
    }


    String[] tokens = token.trim().split("\\.");
    CryptoConst.JWTAlgo jwtAlgo = jwt.getHeader().getJWTAlgorithm();
    switch (jwtAlgo) {
      case HS256:
      case HS384:
      case HS512:
        SharedUtil.checkIfNulls("Null key", key);
        if (tokens.length != JWTField.values().length) {
          throw new SecurityException("Invalid token");
        }
        Mac shaHMAC = HashUtil.getMac(jwtAlgo.getSignatureAlgo());
        SecretKeySpec secret_key = new SecretKeySpec(key, jwtAlgo.getSignatureAlgo().getName());
        shaHMAC.init(secret_key);
        shaHMAC.update(SharedStringUtil.getBytes(tokens[JWTField.HEADER.ordinal()]));

        shaHMAC.update((byte) '.');
        byte[] b64Hash = shaHMAC.doFinal(SharedStringUtil.getBytes(tokens[JWTField.PAYLOAD.ordinal()]));

        if (!SharedBase64.encodeAsString(Base64Type.URL, b64Hash).equals(jwt.getHash())) {
          throw new SecurityException("Invalid token");
        }
        break;

      case none:
        if (tokens.length != JWTField.values().length - 1) {
          throw new SecurityException("Invalid token");
        }
        break;
      case RS256:
      case RS384:
      case RS512:
      case ES256:
      case ES384:
      case ES512:
        SharedUtil.checkIfNulls("Null key", key);
        if (tokens.length != JWTField.values().length) {
          throw new SecurityException("Invalid token");
        }
        PublicKey publicKey = generatePublicKey(jwtAlgo.getSignatureAlgo().getCryptoAlgo().getName(),key);

        if (!CryptoUtil.verify(jwtAlgo.getSignatureAlgo(), publicKey,
            SharedStringUtil.getBytes(
                tokens[JWTField.HEADER.ordinal()] + "." + tokens[JWTField.PAYLOAD.ordinal()]),
            SharedBase64.decode(Base64Type.URL, jwt.getHash()))) {
          throw new SecurityException("Invalid token");
        }
        break;

    }

    return jwt;
  }



  public static JWT parseJWT(String token)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException, NullPointerException, IllegalArgumentException {
    SharedUtil.checkIfNulls("Null token", token);
    String[] tokens = token.trim().split("\\.");

    if (tokens.length < 2 || tokens.length > 3) {
      throw new IllegalArgumentException("Invalid token JWT token");
    }

    NVGenericMap nvgmHeader = GSONUtil.fromJSONGenericMap(
        SharedBase64.decodeAsString(Base64Type.URL, tokens[JWTField.HEADER.ordinal()]),
        JWTHeader.NVC_JWT_HEADER,
        Base64Type.URL);//GSONUtil.fromJSON(SharedBase64.decodeAsString(Base64Type.URL,tokens[JWTField.HEADER.ordinal()]), JWTHeader.class);
    NVGenericMap nvgmPayload = GSONUtil.fromJSONGenericMap(
        SharedBase64.decodeAsString(Base64Type.URL, tokens[JWTField.PAYLOAD.ordinal()]),
        JWTPayload.NVC_JWT_PAYLOAD, Base64Type.URL);
    if (nvgmPayload == null) {
      throw new SecurityException("Invalid JWT");
    }
    JWT ret = new JWT();

    //jwtPayload = GSONUtil.fromJSON(SharedStringUtil.toString(SharedBase64.decode(Base64Type.URL,tokens[JWTToken.PAYLOAD.ordinal()])), JWTPayload.class);
    JWTPayload jwtPayload = ret.getPayload();
    JWTHeader jwtHeader = ret.getHeader();
    if (jwtHeader == null || jwtPayload == null) {
      throw new SecurityException("Invalid JWT");
    }
    jwtPayload.setProperties(nvgmPayload);
    jwtHeader.setProperties(nvgmHeader);

    SharedUtil
        .checkIfNulls("Null jwt header or parameters", jwtHeader, jwtHeader.getJWTAlgorithm());
//		JWT ret = new JWT();
    //ret.setHeader(jwtHeader);
    //ret.setPayload(jwtPayload);
    switch (jwtHeader.getJWTAlgorithm()) {
      case HS256:
      case HS512:
      case RS256:
      case RS512:
      case ES256:
      case ES512:
        if (tokens.length != JWTField.values().length) {
          throw new IllegalArgumentException("Invalid token JWT token length expected 3");
        }
        ret.setHash(tokens[JWTField.HASH.ordinal()]);
        break;
      case none:
        if (tokens.length != JWTField.values().length - 1) {
          throw new IllegalArgumentException("Invalid token JWT token length expected 2");
        }
        break;
    }

    return ret;
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


  public static KeyPair generateKeyPair(String type, int keySizeInBits)
      throws NoSuchAlgorithmException
  {
    KeyPairGenerator kg = KeyPairGenerator.getInstance(type);
    kg.initialize(keySizeInBits);//, (SecureRandom)defaultSecureRandom());
    return kg.generateKeyPair();
  }


  public static KeyPair generateKeyPair(String keyCanonicalID)
          throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
    return generateKeyPair(keyCanonicalID, null, defaultSecureRandom());
  }

  public static KeyPair generateKeyPair(CanonicalID keyCanonicalID, String provider, SecureRandom sr)
          throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException
  {
    return generateKeyPair(keyCanonicalID.toCanonicalID(), provider, sr);
  }

  public static KeyPair generateKeyPair(String keyCanonicalID, String provider, SecureRandom sr)
          throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {

    CryptoConst.PKInfo pkInfo = CryptoConst.PKInfo.parse(keyCanonicalID);



    if(sr == null)
      sr = defaultSecureRandom(); // get the default secure random
    KeyPairGenerator keyPairGenerator = provider != null ? KeyPairGenerator.getInstance(pkInfo.getType(), provider) : KeyPairGenerator.getInstance(pkInfo.getType());
    if ("RSA".equals(pkInfo.getType()))
    {
      keyPairGenerator.initialize(Integer.parseInt(pkInfo.getName()), sr);
    }
    else if ("EC".equals(pkInfo.getType()))
    {
      keyPairGenerator.initialize(new ECGenParameterSpec(pkInfo.getName()), sr);
    }
    else
    {
      throw new IllegalArgumentException("Unsupported key type: " + keyCanonicalID);
    }

    return keyPairGenerator.generateKeyPair();
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
    if(algParameters != null)
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
      InetSocketAddressDAO address = HTTPUtil.parseHost(url, 443);
      SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();
      socket = (SSLSocket) factory.createSocket(address.getInetAddress(), address.getPort());
      socket.startHandshake();
      return socket.getSession().getPeerCertificates();
    } finally {
      IOUtil.close(socket);
    }
  }

  public static NVGenericMap publicKeyToNVGM(PublicKey pk)
  {

    NVGenericMap ret = new NVGenericMap();
    System.out.println(pk);
    ret.add("algorithm", pk.getAlgorithm());
    ret.add("format", pk.getFormat());

    ret.add(new NVInt("key_size", pk.getEncoded().length*8));
    ret.add("key", SharedStringUtil.bytesToHex(pk.getEncoded()));
    return ret;
  }

  public static NVGenericMap certificateToNVGM(X509Certificate cert)
  {
    NVGenericMap ret = SharedUtil.toNVGenericMap(cert.getSubjectX500Principal().getName(), "=",",", true);
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
