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
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.KeyStoreInfo;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CryptoUtil {


    private static final Lock LOCK = new ReentrantLock();
    //private static final Logger  log = Logger.getLogger(CryptoUtil.class.getName());
    private static final Map cacheMap = new HashMap();


    private static final String RSASSA_PSS = "RSASSA-PSS";

    /**
     * Minimum wrapping or content key length in bytes. The record KDF is HKDF, which adds no
     * stretching, so keys must be random and full size.
     */
    public static final int MIN_KEY_BYTES = 32;

    /**
     * Rounds for {@link #hashWithIterations} callers; the record path no longer iterates.
     */
    public static final int DEFAULT_ITERATION = 8192;

    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String JDK_JCE_PROVIDER = "SunJCE";
    /** HKDF label under which a record cipher key is derived from the wrapping key. */
    private static final byte[] RECORD_KDF_LABEL = SharedStringUtil.getBytes("enc");


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

    /**
     * RFC 5869 HKDF over HMAC-SHA256.
     *
     * @param ikm    input keying material
     * @param salt   salt, may be null or empty
     * @param info   context label
     * @param length output length in bytes, at most 255 x 32
     */
    public static byte[] hkdfSHA256(byte[] ikm, byte[] salt, byte[] info, int length)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SUS.checkIfNulls("Null HKDF input", ikm, info);
        if (length < 1 || length > 255 * 32) {
            throw new IllegalArgumentException("HKDF length out of range: " + length);
        }
        String algo = CryptoConst.SignatureAlgo.HMAC_SHA_256.getName();
        Mac mac = HashUtil.getMac(CryptoConst.SignatureAlgo.HMAC_SHA_256);
        mac.init(new SecretKeySpec(salt != null && salt.length > 0 ? salt : new byte[32], algo));
        byte[] prk = mac.doFinal(ikm);
        byte[] okm = new byte[length];
        byte[] t = new byte[0];
        try {
            mac.init(new SecretKeySpec(prk, algo));
            int pos = 0;
            for (int i = 1; pos < length; i++) {
                mac.update(t);
                mac.update(info);
                mac.update((byte) i);
                t = mac.doFinal();
                int n = Math.min(t.length, length - pos);
                System.arraycopy(t, 0, okm, pos, n);
                pos += n;
            }
        } finally {
            Arrays.fill(prk, (byte) 0);
            Arrays.fill(t, (byte) 0);
        }
        return okm;
    }

    /**
     * AES-GCM from the JDK provider, whose AES is intrinsic-accelerated, falling back to whatever
     * provider is installed first.
     */
    private static Cipher gcmCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            return Cipher.getInstance(GCM_TRANSFORMATION, JDK_JCE_PROVIDER);
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            return Cipher.getInstance(GCM_TRANSFORMATION);
        }
    }

    private static void checkRecordKey(byte[] key) {
        if (key.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException("Key too short " + key.length * Byte.SIZE + "(bits) min size "
                    + Const.TypeInBytes.BYTE.sizeInBits(MIN_KEY_BYTES) + "(bits)");
        }
    }

    /**
     * Seals {@code data} into {@code record} under {@code key}: AES-256-GCM with a fresh 12-byte
     * nonce, the cipher key derived as {@code HKDF-SHA256(key, salt = iv, "enc")}, and every
     * attribute of the record except the ciphertext as associated data. Set {@code mask},
     * {@code exp} and {@code hint} on the record before calling.
     *
     * @param record the record to fill; its attributes are overwritten
     * @param key    wrapping key, at least {@link #MIN_KEY_BYTES}
     * @param data   plaintext; null means a fresh random 32-byte key
     * @return {@code record}
     */
    public static EncryptedData encryptData(final EncryptedData record, final byte[] key, byte[] data)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return encryptData(record, key, data, null);
    }

    /**
     * As {@link #encryptData(EncryptedData, byte[], byte[])} with extra associated data appended
     * after the record attributes; the same bytes must be supplied to decrypt.
     */
    public static EncryptedData encryptData(final EncryptedData record, final byte[] key, byte[] data, byte[] extraAssociatedData)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return sealRecord(record, key, data, extraAssociatedData, CryptoConst.ALG_A256GCM);
    }

    /**
     * The one sealing path. {@code algorithm} is written into the record's {@code alg} before
     * sealing, so it is authenticated: {@link CryptoConst#ALG_A256GCM} for a plain record, or the
     * KEM that produced the outer key for a KEM-wrapped {@link EncapsulatedKey}.
     */
    private static EncryptedData sealRecord(final EncryptedData record, final byte[] key, byte[] data, byte[] extraAssociatedData, String algorithm)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        SUS.checkIfNulls("Null key or record", key, record);
        checkRecordKey(key);
        if (data == null) {
            data = SecUtil.randomBytes(CryptoConst.AES_256_KEY_SIZE);
        }
        byte[] iv = SecUtil.randomBytes(EncryptedData.IV_SIZE);
        record.setVersion(EncryptedData.VERSION);
        record.setAlgorithm(algorithm);
        record.setKDF(CryptoConst.KDF_HKDF_SHA256);
        record.setIV(iv);
        record.setDataLength(data.length);
        record.setEncryptedData(null);

        byte[] recordKey = hkdfSHA256(key, iv, RECORD_KDF_LABEL, CryptoConst.AES_256_KEY_SIZE);
        try {
            Cipher cipher = gcmCipher();
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(recordKey, CryptoConst.CryptoAlgo.AES.getName()),
                    new GCMParameterSpec(EncryptedData.TAG_SIZE * 8, iv));
            cipher.updateAAD(SharedStringUtil.getBytes(record.toAssociatedData()));
            if (extraAssociatedData != null) {
                cipher.updateAAD(extraAssociatedData);
            }
            record.setEncryptedData(cipher.doFinal(data));
        } finally {
            Arrays.fill(recordKey, (byte) 0);
        }
        return record;
    }

    /**
     * Opens a record sealed by {@link #encryptData(EncryptedData, byte[], byte[])}.
     *
     * @throws SignatureException       if any authenticated attribute or the ciphertext was altered,
     *                                  or the key is wrong
     * @throws IllegalArgumentException if the record version, cipher or KDF is not supported
     */
    public static byte[] decryptEncryptedData(final EncryptedData record, final byte[] key)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        return decryptEncryptedData(record, key, null);
    }

    public static byte[] decryptEncryptedData(final EncryptedData record, final byte[] key, byte[] extraAssociatedData)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        SUS.checkIfNulls("Null key or record", key, record);
        return openRecord(record, key, extraAssociatedData, record.getEncryptedData(), CryptoConst.ALG_A256GCM);
    }

    /**
     * Opens {@code cipherText} as the sealed payload of {@code record}: the associated data comes
     * from the record, the bytes to decrypt from the argument, so a caller can hand over a slice of
     * the stored ciphertext. {@code expectedAlgorithm} is what the record's {@code alg} must say.
     */
    private static byte[] openRecord(final EncryptedData record, final byte[] key, byte[] extraAssociatedData, byte[] cipherText, String expectedAlgorithm)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        SUS.checkIfNulls("Null key or record", key, record);
        checkRecordKey(key);
        if (record.getVersion() != EncryptedData.VERSION) {
            throw new IllegalArgumentException("Unsupported record version " + record.getVersion());
        }
        if (!expectedAlgorithm.equals(record.getAlgorithm())) {
            throw new IllegalArgumentException("Unsupported record cipher " + record.getAlgorithm());
        }
        if (!CryptoConst.KDF_HKDF_SHA256.equals(record.getKDF())) {
            throw new IllegalArgumentException("Unsupported record KDF " + record.getKDF());
        }
        byte[] iv = record.getIV();
        if (iv == null || iv.length != EncryptedData.IV_SIZE || cipherText == null || cipherText.length < EncryptedData.TAG_SIZE) {
            throw new SignatureException("Data tampered with");
        }

        byte[] recordKey = hkdfSHA256(key, iv, RECORD_KDF_LABEL, CryptoConst.AES_256_KEY_SIZE);
        byte[] plain;
        try {
            Cipher cipher = gcmCipher();
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(recordKey, CryptoConst.CryptoAlgo.AES.getName()),
                    new GCMParameterSpec(EncryptedData.TAG_SIZE * 8, iv));
            cipher.updateAAD(SharedStringUtil.getBytes(record.toAssociatedData()));
            if (extraAssociatedData != null) {
                cipher.updateAAD(extraAssociatedData);
            }
            plain = cipher.doFinal(cipherText);
        } catch (AEADBadTagException e) {
            throw new SignatureException("Data tampered with", e);
        } finally {
            Arrays.fill(recordKey, (byte) 0);
        }
        if (plain.length != record.getDataLength()) {
            Arrays.fill(plain, (byte) 0);
            throw new SignatureException("Data tampered with: length mismatch");
        }
        return plain;
    }

    /**
     * Wraps a fresh random 32-byte key for a new {@link EncapsulatedKey} with no binding fields.
     * Prefer {@link #createEncryptedKey(EncapsulatedKey, byte[])} with the binding fields set, so
     * the key is tied to its subject and reference.
     */
    public static EncapsulatedKey createEncryptedKey(final byte[] wrappingKey)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return createEncryptedKey(new EncapsulatedKey(), wrappingKey);
    }

    /**
     * Wraps a fresh random 32-byte key into {@code ek} under {@code wrappingKey}. The subject GUID,
     * reference GUID, reference type and key lock type must be set first: they are authenticated
     * with the wrapped record. The entity GUID is the datastore's business and is never used.
     */
    public static EncapsulatedKey createEncryptedKey(final EncapsulatedKey ek, final byte[] wrappingKey)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return wrapKey(ek, wrappingKey, null);
    }

    /**
     * Seals {@code keyMaterial} into {@code ek} under {@code wrappingKey}. The key row is itself the
     * record: its nonce, length and ciphertext are filled in place, and the extra associated data is
     * {@link EncapsulatedKey#toBindingData()}. The entity GUID is the datastore's and is never used.
     *
     * @param keyMaterial the key to wrap, at least {@link #MIN_KEY_BYTES}; null means a fresh random 32-byte key
     */
    public static EncapsulatedKey wrapKey(final EncapsulatedKey ek, final byte[] wrappingKey, byte[] keyMaterial)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        SUS.checkIfNulls("Null key or wrapping key", ek, wrappingKey);
        if (keyMaterial != null) {
            checkRecordKey(keyMaterial);
        }
        ek.setKeySize(wrappingKey.length);
        encryptData(ek, wrappingKey, keyMaterial, SharedStringUtil.getBytes(ek.toBindingData()));
        return ek;
    }

    /**
     * Opens the key sealed in {@code ek}.
     *
     * @throws SignatureException if the row was re-pointed, the record altered, or the wrapping key is wrong
     */
    public static byte[] unwrapKey(final EncapsulatedKey ek, final byte[] wrappingKey)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        SUS.checkIfNulls("Null key or wrapping key", ek, wrappingKey);
        if (ek.getEncryptedData() == null) {
            throw new SignatureException("No wrapped key");
        }
        return decryptEncryptedData(ek, wrappingKey, SharedStringUtil.getBytes(ek.toBindingData()));
    }

    /**
     * Re-seals the key in {@code ek} under {@code newKey}; the key material itself is unchanged.
     */
    public static EncapsulatedKey rekeyEncryptedKey(final EncapsulatedKey ek,
                                                    final byte[] originalKey, final byte[] newKey)
            throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        SUS.checkIfNulls("Null parameter", originalKey, ek, newKey);
        byte[] material = unwrapKey(ek, originalKey);
        try {
            return wrapKey(ek, newKey, material);
        } finally {
            Arrays.fill(material, (byte) 0);
        }
    }

    /* ------------------------------------------------------------ ML-KEM */

    /**
     * An ML-KEM key pair as raw bytes: the public key goes to whoever wraps keys for its owner,
     * the private key stays with the owner and never enters the datastore.
     */
    public static final class MLKEMKeyPair {
        private final String algorithm;
        private final byte[] publicKey;
        private final byte[] privateKey;

        MLKEMKeyPair(String algorithm, byte[] publicKey, byte[] privateKey) {
            this.algorithm = algorithm;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        /** The parameter set name, {@code ML-KEM-512}, {@code ML-KEM-768} or {@code ML-KEM-1024}. */
        public String getAlgorithm() {
            return algorithm;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }
    }

    /**
     * The BouncyCastle parameter set for an ML-KEM name, {@code ML-KEM-512}, {@code ML-KEM-768} or
     * {@code ML-KEM-1024}; the library's own names, so a set it knows needs no code here.
     */
    private static MLKEMParameters mlkemParameters(String algorithm) {
        SUS.checkIfNulls("Null KEM algorithm", algorithm);
        for (MLKEMParameters p : new MLKEMParameters[]{MLKEMParameters.ml_kem_512, MLKEMParameters.ml_kem_768, MLKEMParameters.ml_kem_1024}) {
            if (p.getName().equalsIgnoreCase(algorithm)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unsupported KEM " + algorithm);
    }

    /**
     * Generates an ML-KEM key pair for the named parameter set.
     */
    public static MLKEMKeyPair generateMLKEMKeyPair(String algorithm) {
        MLKEMParameters params = mlkemParameters(algorithm);
        MLKEMKeyPairGenerator kpg = new MLKEMKeyPairGenerator();
        kpg.init(new MLKEMKeyGenerationParameters(SecUtil.defaultSecureRandom(), params));
        AsymmetricCipherKeyPair kp = kpg.generateKeyPair();
        return new MLKEMKeyPair(params.getName(), ((MLKEMPublicKeyParameters) kp.getPublic()).getEncoded(),
                ((MLKEMPrivateKeyParameters) kp.getPrivate()).getEncoded());
    }

    private static MLKEMPublicKeyParameters mlkemPublicKey(MLKEMParameters params, byte[] publicKey) {
        SUS.checkIfNulls("Null public key", publicKey);
        try {
            return new MLKEMPublicKeyParameters(params, publicKey);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Not a " + params.getName() + " public key: " + publicKey.length + " bytes", e);
        }
    }

    private static MLKEMPrivateKeyParameters mlkemPrivateKey(MLKEMParameters params, byte[] privateKey) {
        SUS.checkIfNulls("Null private key", privateKey);
        try {
            return new MLKEMPrivateKeyParameters(params, privateKey);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Not a " + params.getName() + " private key: " + privateKey.length + " bytes", e);
        }
    }

    /**
     * Wraps a fresh random 32-byte key into {@code ek} for the holder of the ML-KEM private key
     * matching {@code publicKey}. Only the public key is needed; the result cannot be opened by
     * the caller. Set the binding fields and {@code key_guid}, the registry id of the public key,
     * before calling.
     *
     * @param algorithm the parameter set, {@code ML-KEM-512}, {@code ML-KEM-768} or {@code ML-KEM-1024}
     */
    public static EncapsulatedKey createEncryptedKeyMLKEM(final EncapsulatedKey ek, final String algorithm, final byte[] publicKey)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        return wrapKeyMLKEM(ek, algorithm, publicKey, null);
    }

    /**
     * Seals {@code keyMaterial} into {@code ek} under an outer key encapsulated to {@code publicKey}
     * with the named ML-KEM parameter set. The row's {@code cipher_data} becomes the encapsulated
     * key followed by the sealed key, {@code key_size} is the encapsulated key's length in bytes,
     * and {@code alg} is the parameter set name; all three are authenticated. The outer key is
     * never stored. An encapsulated key altered in place changes the decapsulated secret, so the
     * tag on the sealed key refuses it.
     *
     * @param algorithm   the parameter set, {@code ML-KEM-512}, {@code ML-KEM-768} or {@code ML-KEM-1024}
     * @param keyMaterial the key to wrap, at least {@link #MIN_KEY_BYTES}; null means a fresh random 32-byte key
     */
    public static EncapsulatedKey wrapKeyMLKEM(final EncapsulatedKey ek, final String algorithm, final byte[] publicKey, byte[] keyMaterial)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {
        SUS.checkIfNulls("Null key or public key", ek, publicKey);
        if (keyMaterial != null) {
            checkRecordKey(keyMaterial);
        }
        MLKEMParameters params = mlkemParameters(algorithm);
        SecretWithEncapsulation enc = new MLKEMGenerator(SecUtil.defaultSecureRandom())
                .generateEncapsulated(mlkemPublicKey(params, publicKey));
        byte[] outerKey = enc.getSecret();
        try {
            byte[] kemCiphertext = enc.getEncapsulation();
            ek.setKeySize(kemCiphertext.length);
            sealRecord(ek, outerKey, keyMaterial, SharedStringUtil.getBytes(ek.toBindingData()), params.getName());
            byte[] sealed = ek.getEncryptedData();
            byte[] combined = new byte[kemCiphertext.length + sealed.length];
            System.arraycopy(kemCiphertext, 0, combined, 0, kemCiphertext.length);
            System.arraycopy(sealed, 0, combined, kemCiphertext.length, sealed.length);
            ek.setEncryptedData(combined);
            return ek;
        } finally {
            Arrays.fill(outerKey, (byte) 0);
        }
    }

    /**
     * Opens a key sealed by {@link #wrapKeyMLKEM}: splits the encapsulated key off the front of
     * {@code cipher_data} at {@code key_size}, decapsulates it with {@code privateKey} for the
     * parameter set named by {@code alg}, and opens the rest with the recovered outer key.
     *
     * @throws IllegalArgumentException if the row is not KEM-wrapped or names a KEM this library does not know
     * @throws SignatureException       if the row was re-pointed or altered, or the private key does not match
     */
    public static byte[] unwrapKeyMLKEM(final EncapsulatedKey ek, final byte[] privateKey)
            throws NullPointerException,
            IllegalArgumentException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            SignatureException {
        SUS.checkIfNulls("Null key or private key", ek, privateKey);
        if (!ek.isKEMWrapped()) {
            throw new IllegalArgumentException("Unsupported record cipher " + ek.getAlgorithm() + ", expected a KEM");
        }
        MLKEMParameters params = mlkemParameters(ek.getAlgorithm());
        int kemSize = ek.getKeySize();
        byte[] combined = ek.getEncryptedData();
        if (kemSize != params.getEncapsulationLength() || combined == null || combined.length < kemSize + EncryptedData.TAG_SIZE) {
            throw new SignatureException("Missing or malformed encapsulated key");
        }
        byte[] kemCiphertext = Arrays.copyOfRange(combined, 0, kemSize);
        byte[] sealed = Arrays.copyOfRange(combined, kemSize, combined.length);
        // ML-KEM never fails to decapsulate: a wrong key yields an unrelated secret and the tag refuses it
        byte[] outerKey = new MLKEMExtractor(mlkemPrivateKey(params, privateKey)).extractSecret(kemCiphertext);
        try {
            return openRecord(ek, outerKey, SharedStringUtil.getBytes(ek.toBindingData()), sealed, params.getName());
        } finally {
            Arrays.fill(outerKey, (byte) 0);
        }
    }

    /**
     * Re-seals a KEM-wrapped key for a new public key of the named parameter set; the key material
     * itself is unchanged.
     */
    public static EncapsulatedKey rekeyEncryptedKeyMLKEM(final EncapsulatedKey ek,
                                                         final byte[] privateKey, final String newAlgorithm, final byte[] newPublicKey)
            throws NullPointerException, IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
        SUS.checkIfNulls("Null parameter", ek, privateKey, newAlgorithm, newPublicKey);
        byte[] material = unwrapKeyMLKEM(ek, privateKey);
        try {
            return wrapKeyMLKEM(ek, newAlgorithm, newPublicKey, material);
        } finally {
            Arrays.fill(material, (byte) 0);
        }
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
            SharedIOUtil.close(keyStoreOS);
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
            SharedIOUtil.close(os);
        }
    }


    public static KeyStore createKeyStore(final OutputStream keyStoreOS, String keyStoreType,
                                          String keyStorePass)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ret = KeyStore.getInstance(keyStoreType);
        char[] password = keyStorePass.toCharArray();

        try {
            // a KeyStore must be initialized before it can be stored, null stream creates an empty one
            ret.load(null, password);
            ret.store(keyStoreOS, password);
        } finally {
            SharedIOUtil.close(keyStoreOS);
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

            SharedIOUtil.close(keyStoreIS);
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

        byte[] bytes = SecUtil.randomBytes(random, arraySize);

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
                throw new SecurityException("none JWT Algo not supported");
            case RS256:
            case RS384:
            case RS512:
            case PS256:
            case PS384:
            case PS512:
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
            default:
                // without a signing branch above the token would be emitted with an empty
                // signature, which reads as signed but is not
                throw new SecurityException(jwtAlgo.getName() + " JWT Algo not supported");
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
        String key = type + "-" + keySizeInBits;
        LOCK.lock();
        try {
            KeyGenerator kg = (KeyGenerator) cacheMap.get(key);
            if (kg == null) {
                kg = KeyGenerator.getInstance(type);
                //kg.init(keySizeInBits, (SecureRandom)defaultSecureRandom());
                kg.init(keySizeInBits);
                cacheMap.put(key, kg);
            }

            return kg.generateKey();
        } finally {
            LOCK.unlock();
        }
    }

    public static SecretKey toSecretKey(byte[] key, String algoName) {
        return new SecretKeySpec(key, algoName);
    }


//    public static KeyPair generateKeyPair(String type, int keySizeInBits)
//            throws NoSuchAlgorithmException {
//        KeyPairGenerator kg = KeyPairGenerator.getInstance(type);
//        kg.initialize(keySizeInBits);//, (SecureRandom)defaultSecureRandom());
//        return kg.generateKeyPair();
//    }

    public static KeyPair generateKeyPair(CryptoConst.PKInfo keyInfo)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        return generateKeyPair(keyInfo, null, null);
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

        return generateKeyPair(CryptoConst.PKInfo.parse(keyCanonicalID), provider, sr);
    }

    public static KeyPair generateKeyPair(CryptoConst.PKInfo pkInfo, String provider, SecureRandom sr)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        SUS.checkIfNull("PKInfo null", pkInfo);
        if (sr == null)
            sr = SecUtil.defaultSecureRandom(); // get the default secure random
        KeyPairGenerator keyPairGenerator = provider != null ? KeyPairGenerator.getInstance(pkInfo.getType(), provider) : KeyPairGenerator.getInstance(pkInfo.getType());
        if ("RSA".equals(pkInfo.getType())) {
            keyPairGenerator.initialize(Integer.parseInt(pkInfo.getName()), sr);
        } else if ("EC".equals(pkInfo.getType())) {
            keyPairGenerator.initialize(new ECGenParameterSpec(pkInfo.getName()), sr);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + pkInfo.toCanonicalID());
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


    /**
     * RSASSA-PSS parameters per RFC 7518: MGF1 seeded with the same digest and a salt length
     * equal to the digest length. Returns null for algorithms that are not PSS based.
     */
    private static PSSParameterSpec toPSSParameterSpec(CryptoConst.SignatureAlgo sa) {
        switch (sa) {
            case SHA256_RSA_MGF1:
                return new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
            case SHA384_RSA_MGF1:
                return new PSSParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 48, 1);
            case SHA512_RSA_MGF1:
                return new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1);
            default:
                return null;
        }
    }

    /**
     * The PSS algorithms must be requested as RSASSA-PSS with explicit parameters, their
     * SignatureAlgo name is a BouncyCastle alias that the stock JCE providers do not resolve.
     */
    private static Signature toSignature(CryptoConst.SignatureAlgo sa)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        PSSParameterSpec pssSpec = toPSSParameterSpec(sa);

        if (pssSpec == null)
            return Signature.getInstance(sa.getName());

        Signature ret = Signature.getInstance(RSASSA_PSS);
        ret.setParameter(pssSpec);
        return ret;
    }

    public static byte[] sign(CryptoConst.SignatureAlgo sa, PrivateKey pk, byte[] data)
            throws GeneralSecurityException {
        Signature signature = toSignature(sa);
        signature.initSign(pk, SecUtil.defaultSecureRandom());
        signature.update(data);
        return signature.sign();
    }

    public static boolean verify(CryptoConst.SignatureAlgo sa, PublicKey pk, byte[] data, byte[] signedData)
            throws GeneralSecurityException {
        Signature signature = toSignature(sa);
        signature.initVerify(pk);
        signature.update(data);
        return signature.verify(signedData);
    }


    public static String toString(Key key) {
        return SUS.toCanonicalID(':', key.getAlgorithm(), key.getEncoded().length, key.getFormat(),
                SUS.fastBytesToHex(key.getEncoded()));
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
            SharedIOUtil.close(socket);
        }
    }

    public static NVGenericMap publicKeyToNVGM(PublicKey pk) {

        NVGenericMap ret = new NVGenericMap();
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
