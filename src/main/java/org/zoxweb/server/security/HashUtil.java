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

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.security.NoSuchAlgorithmException;
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
   * @param password the password to check
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
   * @param password the password to check
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
    return SharedUtil.slowEquals(hash, testHash);
  }

  /**
   * Computes the PBKDF2 hash of a password.
   *
   * @param password the password to hash.
   * @param salt the salt
   * @param iterations the iteration count (slowness factor)
   * @param bytes the length of the hash to compute in bytes
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

  public static MessageDigest createMessageDigest(String algorithm) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(algorithm);
  }

}
