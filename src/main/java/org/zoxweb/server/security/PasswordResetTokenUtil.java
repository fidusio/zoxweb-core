package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Reset tokens: 256 bits of randomness, base64url without padding, stored as their SHA-256.
 * A fast hash is enough because the token space cannot be enumerated; the comparison is
 * constant time.
 */
public final class PasswordResetTokenUtil {

    public static final int TOKEN_BYTES = 32;

    private PasswordResetTokenUtil() {
    }

    /** A fresh random token, 43 base64url characters. */
    public static String newToken() {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, SecUtil.randomBytes(TOKEN_BYTES));
    }

    /** base64url of SHA-256 of the token's UTF-8 bytes. */
    public static String hash(String token) {
        SUS.checkIfNulls("token null", token);
        try {
            byte[] digest = HashUtil.hash(CryptoConst.HashType.SHA_256, SharedStringUtil.getBytes(token));
            return SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time comparison of a stored hash with a presented token; false on any null. */
    public static boolean matches(String storedHash, String token) {
        if (storedHash == null || token == null) {
            return false;
        }
        return MessageDigest.isEqual(SharedStringUtil.getBytes(storedHash), SharedStringUtil.getBytes(hash(token)));
    }
}
