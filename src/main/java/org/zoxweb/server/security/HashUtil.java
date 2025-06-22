/*
 * Password Hashing With PBKDF2 (http://crackstation.net/hashing-security.htm).
 * Copyright (c) 2013, Taylor Hornby
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.BCryptHash;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/*
 * PBKDF2 salted password hashing.
 * Author: havoc AT defuse.ca
 * www: http://crackstation.net/hashing-security.htm
 */
public class HashUtil {

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    // The following constants may be changed without breaking existing hashes.
    public static final int SALT_BYTE_SIZE = 24;
    public static final int HASH_BYTE_SIZE = 24;
    public static final int PBKDF2_ITERATIONS = 1000;

    public static final int ITERATION_INDEX = 0;
    public static final int SALT_INDEX = 1;
    public static final int PBKDF2_INDEX = 2;

    public static final int SALT_LENGTH = 32;


    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param password the password to hash
     * @return a salted PBKDF2 hash of the password
     */
    public static String createHash(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return createHash(password.toCharArray());
    }

    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param password the password to hash
     * @return a salted PBKDF2 hash of the password
     */
    public static String createHash(char[] password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Generate a random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTE_SIZE];
        random.nextBytes(salt);

        // Hash the password
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
        // format iterations:salt:hash
        return PBKDF2_ITERATIONS + ":" + SharedStringUtil.bytesToHex(salt) + ":" + SharedStringUtil
                .bytesToHex(hash);
    }

    /**
     * Validates a password using a hash.
     *
     * @param password    the password to check
     * @param correctHash the hash of the valid password
     * @return true if the password is correct, false if not
     */
    public static boolean validatePassword(String password, String correctHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return validatePassword(password.toCharArray(), correctHash);
    }

    /**
     * Validates a password using a hash.
     *
     * @param password    the password to check
     * @param correctHash the hash of the valid password
     * @return true if the password is correct, false if not
     */
    public static boolean validatePassword(char[] password, String correctHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Decode the hash into its parameters
        String[] params = correctHash.split(":");
        int iterations = Integer.parseInt(params[ITERATION_INDEX]);
        byte[] salt = SharedStringUtil.hexToBytes(params[SALT_INDEX]);
        byte[] hash = SharedStringUtil.hexToBytes(params[PBKDF2_INDEX]);
        // Compute the hash of the provided password, using the same salt,
        // iteration count, and hash length
        byte[] testHash = pbkdf2(password, salt, iterations, hash.length);
        // Compare the hashes in constant time. The password is correct if
        // both hashes match.
        return SUS.slowEquals(hash, testHash);
    }

    /**
     * Computes the PBKDF2 hash of a password.
     *
     * @param password   the password to hash.
     * @param salt       the salt
     * @param iterations the iteration count (slowness factor)
     * @param bytes      the length of the hash to compute in bytes
     * @return the PBDKF2 hash of the password
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    public static byte[] hashSequence(String algorithm, byte[]... seqs)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        for (byte[] seq : seqs) {
            md.update(seq);
        }

        return md.digest();
    }


    public static byte[] hashSequence(String algorithm, String... seqs)
            throws NoSuchAlgorithmException {
        return hashSequence(algorithm, SharedStringUtil.getBytesArray(seqs));
    }


    public static MessageDigest getMessageDigest(CryptoConst.HashType hashType) throws NoSuchAlgorithmException {
        return getMessageDigest(hashType.getName());
    }

    public static MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    public static Mac getMac(CryptoConst.SignatureAlgo algo) throws NoSuchAlgorithmException {
        return Mac.getInstance(algo.getName());
    }

    public static Mac getMac(String algo) throws NoSuchAlgorithmException {
        return Mac.getInstance(algo);
    }

    public static MessageDigest getMessageDigestSilent(String algorithm) {
        try {
            return getMessageDigest(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static BCryptHash toBCryptHash(String bCryptCanonicalID) {
        return new BCryptHash(bCryptCanonicalID);
    }

    public static BCryptHash toBCryptHash(int logRounds, byte[] password) {
        String salt = BCrypt.gensalt(logRounds);
        String hashedPW = BCrypt.hashpw(password, salt);
        return toBCryptHash(hashedPW);
    }

    public static BCryptHash toBCryptHash(int logRounds, String password) {
        return toBCryptHash(logRounds, SharedStringUtil.getBytes(password));
    }

    public static boolean isBCryptPasswordValid(String password, String bCryptHash) {
        return BCrypt.isAMatch(password, bCryptHash);
    }

    public static boolean isBCryptPasswordValid(byte[] password, String bCryptHash) {
        return BCrypt.isAMatch(password, bCryptHash);
    }

    public static CIPassword toPassword(String algo, int saltLength, int saltIteration,
                                        String password)
            throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
        SUS.checkIfNulls("Null parameter", algo, password);
        return toPassword(CryptoConst.HashType.lookup(algo), saltLength, saltIteration, password);
    }

    public static CIPassword toPassword(CryptoConst.HashType algo, int saltLength, int saltIteration,
                                        String password)
            throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
        SUS.checkIfNulls("Null parameter", algo, password);
        return toPassword(algo, saltLength, saltIteration, SharedStringUtil.getBytes(FilterType.PASSWORD.validate(password)));
    }

    public static CIPassword toBCryptPassword(String password, int logRounds) {
        try {
            return toPassword(CryptoConst.HashType.BCRYPT, 0, logRounds, password);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }


    public static CIPassword toBCryptPassword(byte[] password, int logRounds) {
        try {
            return toPassword(CryptoConst.HashType.BCRYPT, 0, logRounds, password);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }


    public static CIPassword toBCryptPassword(String password) {
        try {
            return toPassword(CryptoConst.HashType.BCRYPT, 0, 10, password);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }


    public static CIPassword toBCryptPassword(byte[] password) {
        try {
            return toPassword(CryptoConst.HashType.BCRYPT, 0, 10, password);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
    }


    public static CIPassword toPassword(CryptoConst.HashType algo, int saltLength, int saltIteration,
                                        byte[] password)
            throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
        SUS.checkIfNulls("Null parameter", algo, password);
        if (password.length < 6) {
            throw new IllegalArgumentException("password length too short");
        }
        byte[] hashedPassword = null;
        byte[] salt = null;

        CIPassword ciPassword = new CIPassword();
        if (algo == CryptoConst.HashType.BCRYPT) {
            BCryptHash bcryptHash = toBCryptHash(saltIteration, password);
            ciPassword.setCanonicalID(bcryptHash.toCanonicalID());
            salt = SharedStringUtil.getBytes(bcryptHash.salt);
            hashedPassword = SharedStringUtil.getBytes(bcryptHash.hash);
        } else {
            if (saltLength < SALT_LENGTH) {
                saltLength = SALT_LENGTH;
            }

            if (saltIteration < 0) {
                saltIteration = 0;
            }
            SecureRandom random = SecUtil.SINGLETON.defaultSecureRandom();
            salt = new byte[saltLength];
            random.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance(algo.getName());
            hashedPassword = hashWithIterations(md, salt, password, saltIteration, false);
        }


        ciPassword.setSalt(salt);
        ciPassword.setPasswordHash(hashedPassword);
        ciPassword.setHashIterations(saltIteration);
        ciPassword.setName(algo);

        return ciPassword;
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


    public static CIPassword mergeContent(CIPassword password, CIPassword toMerge) {
        synchronized (password) {
            password.setName(toMerge.getName());
            password.setHashIterations(toMerge.getHashIterations());
            password.setSalt(toMerge.getSalt());
            password.setPasswordHash(toMerge.getPasswordHash());
        }

        return password;
    }

    public static boolean isPasswordValid(final CIPassword passwordDAO, String password)
            throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
        SUS.checkIfNulls("Null values", passwordDAO, password);
        if (CryptoConst.HashType.lookup(passwordDAO.getName()) == CryptoConst.HashType.BCRYPT) {
            return isBCryptPasswordValid(password, passwordDAO.getCanonicalID());
        } else {
            byte[] genHash = hashWithIterations(MessageDigest.getInstance(passwordDAO.getName()),
                    passwordDAO.getSalt(), SharedStringUtil.getBytes(password), passwordDAO.getHashIterations(),
                    false);

            return SUS.slowEquals(genHash, passwordDAO.getPasswordHash());
        }
    }

    public static void validatePassword(final CIPassword passwordDAO, String password)
            throws NullPointerException, IllegalArgumentException, AccessException {
        SUS.checkIfNulls("Null values", passwordDAO, password);
        validatePassword(passwordDAO, password.toCharArray());
    }


    public static String hashAsBase64(String algo, byte[] data) throws NoSuchAlgorithmException {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash(algo, data));
    }

    public static String hashAsBase64(String algo, String msg) throws NoSuchAlgorithmException {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash(algo, SharedStringUtil.getBytes(msg)));
    }


    public static String hashAsBase64(CryptoConst.HashType algo, byte[] data) throws NoSuchAlgorithmException {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash(algo, data));
    }

    public static String hashAsBase64(CryptoConst.HashType algo, String msg) throws NoSuchAlgorithmException {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash(algo, SharedStringUtil.getBytes(msg)));
    }

    public static byte[] hash(CryptoConst.HashType algo, byte[] data) throws NoSuchAlgorithmException {
        return hash(algo.getName(), data);
    }

    public static byte[] hash(String algo, byte[] data) throws NoSuchAlgorithmException {
        return hashSequence(algo, data);
    }


    public static void validatePassword(final CIPassword passwordDAO, final char[] password)
            throws NullPointerException, IllegalArgumentException, AccessException {

        SUS.checkIfNulls("Null values", passwordDAO, password);

        try {
            if (isPasswordValid(passwordDAO, new String(password)))
                return; // we hava a valid password
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
            throw new AccessException("Invalid Credentials");
        }
        // password validation failed,
        throw new AccessException("Invalid Credentials");

    }
}
