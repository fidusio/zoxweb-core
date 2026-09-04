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

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SUS;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Streaming file encryption.
 * <p>
 * <b>Writing</b> always produces the VX container described below.
 * <b>Reading</b> accepts a VX container or a legacy
 * <a href="http://www.aescrypt.com/aes_file_format.html">AES Crypt</a> version 1 or 2 file,
 * selected by the magic bytes at the start of the stream.
 *
 * <h3>VX container</h3>
 * A chunked AEAD: the plaintext is cut into segments of {@code segmentSize} bytes, each sealed with
 * AES-256-GCM under a per-file key. Every segment is authenticated on its own, so a reader releases a
 * segment only after its tag verifies, needs no pre-pass over the container, and detects truncation,
 * reordering and header tampering.
 * <pre>
 * magic        4   "ZAES"
 * version      1   1
 * cipher       1   1 = AES-256-GCM, 128-bit tag
 * kdf          1   0 = HKDF-SHA256 (raw key), 1 = PBKDF2-HMAC-SHA256 (typed password)
 * kdf params   4   0 for HKDF; the iteration count for PBKDF2, big-endian
 * salt         16  random per file, KDF salt
 * nonce prefix 7   random per file
 * segment size 4   plaintext bytes per segment, big-endian
 * segments     n x (segmentSize + 16); the last segment may be shorter, an empty input is one
 *                  empty last segment (16 bytes of tag)
 * </pre>
 * The 38-byte header is the associated data of every segment. The nonce of segment {@code i} is
 * {@code prefix || i (4 bytes, big-endian) || lastFlag (1 byte)}. The content key is
 * {@code HKDF-SHA256(ikm = key bytes, salt, info = "ZAES-1 content key", 32)} for kdf 0 and
 * {@code PBKDF2-HMAC-SHA256(password bytes, salt, iterations, 32)} for kdf 1.
 * <p>
 * <b>The byte-array constructor takes key material, not a password.</b> It is stretched by nothing
 * but HKDF, so callers must supply a random key of at least 16 bytes; 32 is recommended. The
 * {@code String} constructor takes a typed password, encoded as UTF-16LE, the AES Crypt convention:
 * legacy files are read with those bytes directly, and VX containers stretch them with PBKDF2 over
 * the per-file salt, {@link #VX_DEFAULT_PBKDF2_ITERATIONS} iterations unless
 * {@link #setPbkdf2Iterations(int)} says otherwise. The reader takes the iteration count from the
 * header, so the writer's setting travels with the file.
 * <p>
 * Thread-safety and sharing: this class is not thread-safe. {@code AESCrypt} objects can be used as
 * commands (create, use once and dispose) or reused to perform multiple operations sequentially.
 *
 * @author Vócali Sistemas Inteligentes (legacy AES Crypt reader)
 */
public class AESCrypt {

    /* ---------------------------------------------------------------- shared */

    private static final String CRYPT_ALG = "AES";
    private static final String HMAC_ALG = "HmacSHA256";
    private static final String DIGEST_ALG = "SHA-256";
    private static final String JDK_JCE_PROVIDER = "SunJCE";
    private static final String JDK_SUN_PROVIDER = "SUN";
    private static final int KEY_SIZE = 32;
    private static final String EXT = ".aes";

    /* -------------------------------------------------------------------- VX */

    public static final byte[] VX_MAGIC = {'Z', 'A', 'E', 'S'};
    public static final int VX_VERSION = 1;
    public static final int VX_CIPHER_AES_256_GCM = 1;
    public static final int VX_KDF_HKDF_SHA256 = 0;
    public static final int VX_KDF_PBKDF2_HMAC_SHA256 = 1;
    /** OWASP's 2023 figure for PBKDF2-HMAC-SHA256. */
    public static final int VX_DEFAULT_PBKDF2_ITERATIONS = 600_000;
    /** Lowest count a writer may use; keeps a misconfigured caller from writing an unstretched file. */
    public static final int VX_MIN_PBKDF2_ITERATIONS = 1_000;
    /** Highest count a reader will honour from a header, so a hostile file cannot pin a CPU. */
    public static final int VX_MAX_PBKDF2_ITERATIONS = 10_000_000;
    public static final int VX_SALT_SIZE = 16;
    public static final int VX_NONCE_PREFIX_SIZE = 7;
    public static final int VX_NONCE_SIZE = 12;
    public static final int VX_TAG_SIZE = 16;
    public static final int VX_HEADER_SIZE = 4 + 1 + 1 + 1 + 4 + VX_SALT_SIZE + VX_NONCE_PREFIX_SIZE + 4;
    public static final int VX_DEFAULT_SEGMENT_SIZE = 64 * 1024;
    public static final int VX_MAX_SEGMENT_SIZE = 16 * 1024 * 1024;
    public static final int VX_MIN_KEY_SIZE = 16;
    private static final byte[] VX_HKDF_INFO = "ZAES-1 content key".getBytes(StandardCharsets.US_ASCII);
    private static final String GCM_TRANS = "AES/GCM/NoPadding";
    private static final long MAX_SEGMENT_COUNTER = 0xFFFFFFFFL;

    /* ---------------------------------------------------------- legacy v1/v2 */

    private static final byte[] LEGACY_MAGIC = {'A', 'E', 'S'};
    private static final String LEGACY_CRYPT_TRANS = "AES/CBC/NoPadding";
    private static final int BLOCK_SIZE = 16;
    private static final int SHA_SIZE = 32;
    private static final int LEGACY_KDF_ROUNDS = 8192;
    /** CBC and HMAC chunk for the legacy reader; a multiple of {@link #BLOCK_SIZE}. */
    private static final int LEGACY_BUFFER_SIZE = 64 * 1024;

    /* ----------------------------------------------------------------- state */

    /** Bytes as given: raw key material from the byte-array setter, UTF-16LE text from the String one. */
    private byte[] password;
    /** True when {@link #password} is a typed password; VX then writes kdf 1 with PBKDF2 stretching. */
    private boolean textPassword;
    private int pbkdf2Iterations = VX_DEFAULT_PBKDF2_ITERATIONS;
    private int segmentSize = VX_DEFAULT_SEGMENT_SIZE;
    private SecureRandom random;
    private final Cipher gcm;
    private final Mac hmac;
    private Cipher cbc;
    private MessageDigest digest;
    private byte[] lastHeader;

    /* --------------------------------------------------------- construction */

    /**
     * Builds an object to encrypt or decrypt with the given key bytes.
     *
     * @param key key material; at least {@link #VX_MIN_KEY_SIZE} bytes to write a VX container. The
     *            array is held by reference so the caller can clear it afterwards.
     * @throws GeneralSecurityException if the platform lacks AES-GCM or HMAC-SHA256.
     */
    public AESCrypt(byte[] key) throws GeneralSecurityException, UnsupportedEncodingException {
        setPassword(key);
        gcm = cipher(GCM_TRANS);
        hmac = mac(HMAC_ALG);
    }

    /**
     * Builds an object from a typed password. Legacy files are read with the password's UTF-16LE
     * bytes, the AES Crypt convention. VX containers stretch those bytes with PBKDF2-HMAC-SHA256 over
     * the per-file salt; see {@link #setPbkdf2Iterations(int)}.
     */
    public AESCrypt(String password) throws GeneralSecurityException, UnsupportedEncodingException {
        this(password.getBytes("UTF-16LE"));
        setPassword(password);
    }

    /**
     * Changes the key this object uses, from a typed password; see {@link #AESCrypt(String)}.
     */
    public void setPassword(String password) throws UnsupportedEncodingException {
        SUS.checkIfNulls("Can't have a null password", password);
        if (password.isEmpty()) {
            throw new IllegalArgumentException("Can't have an empty password");
        }
        this.password = password.getBytes("UTF-16LE");
        this.textPassword = true;
    }

    /**
     * Changes the key this object uses. The array is held by reference, not copied, so the caller
     * can clear it afterwards.
     */
    public void setPassword(byte[] key) throws UnsupportedEncodingException {
        SUS.checkIfNulls("Can't have a null key", key);
        this.password = key;
        this.textPassword = false;
    }

    /**
     * @return PBKDF2 iterations {@link #encrypt} writes into the header for a typed password.
     */
    public int getPbkdf2Iterations() {
        return pbkdf2Iterations;
    }

    /**
     * Sets the PBKDF2 iteration count for subsequent {@link #encrypt} calls from a typed password.
     * Readers take the count from the file, so lowering it only affects files written afterwards.
     *
     * @param iterations between {@link #VX_MIN_PBKDF2_ITERATIONS} and {@link #VX_MAX_PBKDF2_ITERATIONS}
     */
    public void setPbkdf2Iterations(int iterations) {
        if (iterations < VX_MIN_PBKDF2_ITERATIONS || iterations > VX_MAX_PBKDF2_ITERATIONS) {
            throw new IllegalArgumentException("PBKDF2 iterations out of range: " + iterations);
        }
        this.pbkdf2Iterations = iterations;
    }

    /**
     * @return plaintext bytes per VX segment used by {@link #encrypt}.
     */
    public int getSegmentSize() {
        return segmentSize;
    }

    /**
     * Sets the plaintext bytes per VX segment for subsequent {@link #encrypt} calls.
     *
     * @param segmentSize between 1 and {@link #VX_MAX_SEGMENT_SIZE}
     */
    public void setSegmentSize(int segmentSize) {
        if (segmentSize < 1 || segmentSize > VX_MAX_SEGMENT_SIZE) {
            throw new IllegalArgumentException("segmentSize out of range: " + segmentSize);
        }
        this.segmentSize = segmentSize;
    }

    /**
     * @return a copy of the VX header written by the last {@link #encrypt} or read by the last
     * {@link #decrypt} / {@link #verify}, or null if none, or if the last read was a legacy file.
     * Callers that record salt, nonce prefix and segment size beside the container use this.
     */
    public byte[] getLastHeader() {
        return lastHeader != null ? lastHeader.clone() : null;
    }

    /* -------------------------------------------------------------- helpers */

    /**
     * Prefers the JDK's own provider, whose AES and SHA-2 are intrinsic-accelerated, over whatever
     * provider is installed first (BouncyCastle in this library). Falls back to the default lookup.
     */
    private static Cipher cipher(String transformation) throws GeneralSecurityException {
        try {
            return Cipher.getInstance(transformation, JDK_JCE_PROVIDER);
        } catch (GeneralSecurityException e) {
            return Cipher.getInstance(transformation);
        }
    }

    private static Mac mac(String algorithm) throws GeneralSecurityException {
        try {
            return Mac.getInstance(algorithm, JDK_JCE_PROVIDER);
        } catch (GeneralSecurityException e) {
            return Mac.getInstance(algorithm);
        }
    }

    private static MessageDigest messageDigest(String algorithm) throws GeneralSecurityException {
        try {
            return MessageDigest.getInstance(algorithm, JDK_SUN_PROVIDER);
        } catch (GeneralSecurityException e) {
            return MessageDigest.getInstance(algorithm);
        }
    }

    /**
     * Instance-local generator, created on first encrypt so decrypt-only instances never pay for it.
     */
    private SecureRandom random() {
        if (random == null) {
            random = new SecureRandom();
        }
        return random;
    }

    /**
     * RFC 5869 HKDF over HMAC-SHA256.
     */
    private byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int length) throws GeneralSecurityException {
        hmac.init(new SecretKeySpec(salt, HMAC_ALG));
        byte[] prk = hmac.doFinal(ikm);
        byte[] okm = new byte[length];
        byte[] t = new byte[0];
        try {
            hmac.init(new SecretKeySpec(prk, HMAC_ALG));
            int pos = 0;
            for (int i = 1; pos < length; i++) {
                hmac.update(t);
                hmac.update(info);
                hmac.update((byte) i);
                t = hmac.doFinal();
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
     * RFC 8018 PBKDF2 over HMAC-SHA256, one output block: {@code length} must not exceed 32.
     */
    private byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length) throws GeneralSecurityException {
        hmac.init(new SecretKeySpec(password, HMAC_ALG));
        hmac.update(salt);
        hmac.update(new byte[]{0, 0, 0, 1});  // block index 1
        byte[] u = hmac.doFinal();
        byte[] t = u.clone();
        for (int i = 1; i < iterations; i++) {
            u = hmac.doFinal(u);
            for (int j = 0; j < t.length; j++) {
                t[j] ^= u[j];
            }
        }
        Arrays.fill(u, (byte) 0);
        if (length == t.length) {
            return t;
        }
        byte[] out = Arrays.copyOf(t, length);
        Arrays.fill(t, (byte) 0);
        return out;
    }

    /**
     * The per-file content key for a header's kdf id and params over its salt.
     */
    private byte[] contentKey(int kdfId, int kdfParams, byte[] salt) throws GeneralSecurityException {
        if (kdfId == VX_KDF_PBKDF2_HMAC_SHA256) {
            return pbkdf2(password, salt, kdfParams, KEY_SIZE);
        }
        return hkdf(password, salt, VX_HKDF_INFO, KEY_SIZE);
    }

    private static void nonce(byte[] nonce, byte[] prefix, long counter, boolean last) {
        System.arraycopy(prefix, 0, nonce, 0, VX_NONCE_PREFIX_SIZE);
        nonce[7] = (byte) (counter >>> 24);
        nonce[8] = (byte) (counter >>> 16);
        nonce[9] = (byte) (counter >>> 8);
        nonce[10] = (byte) counter;
        nonce[11] = (byte) (last ? 1 : 0);
    }

    /**
     * Reads until the array holds {@code len} bytes from {@code off}. Loops over
     * {@link InputStream#read(byte[], int, int)} because a socket or chunked body may return fewer
     * bytes than requested.
     *
     * @throws IOException if the stream ends first.
     */
    protected void readBytes(InputStream in, byte[] bytes, int len) throws IOException {
        int got = readUpTo(in, bytes, 0, len);
        if (got != len) {
            throw new IOException("Unexpected end of file");
        }
    }

    protected void readBytes(InputStream in, byte[] bytes) throws IOException {
        readBytes(in, bytes, bytes.length);
    }

    /**
     * Reads up to {@code len} bytes, stopping early only at end of stream.
     *
     * @return bytes read, possibly 0 at end of stream
     */
    private static int readUpTo(InputStream in, byte[] buf, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int n = in.read(buf, off + total, len - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new IOException("Unexpected end of file");
        }
        return b;
    }

    /**
     * Skips exactly {@code len} bytes; {@link InputStream#skip(long)} may skip fewer.
     */
    private static void skipFully(InputStream in, long len) throws IOException {
        while (len > 0) {
            long n = in.skip(len);
            if (n <= 0) {
                readByte(in);
                n = 1;
            }
            len -= n;
        }
    }

    private static void emit(OutputStream out, byte[] buf, int off, int len) throws IOException {
        if (out != null && len > 0) {
            out.write(buf, off, len);
        }
    }

    private static IOException corrupt(String detail) {
        return new IOException("Input file is corrupt: " + detail);
    }

    /* ------------------------------------------------------------ public API */

    /**
     * Encrypts the file at {@code fromPath} into a VX container at {@code toPath}.
     */
    public void encrypt(String fromPath, String toPath) throws IOException, GeneralSecurityException {
        encrypt(new File(fromPath), new File(toPath));
    }

    /**
     * Encrypts {@code from} into a VX container at {@code to}.
     */
    public void encrypt(File from, File to) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            encrypt(new BufferedInputStream(fis), new BufferedOutputStream(fos));
        } finally {
            SharedIOUtil.close(fis);
            SharedIOUtil.close(fos);
        }
    }

    /**
     * Decrypts the VX or legacy container at {@code fromPath} into {@code toPath}.
     */
    public void decrypt(String fromPath, String toPath) throws IOException, GeneralSecurityException {
        decrypt(new File(fromPath), new File(toPath));
    }

    /**
     * Decrypts the VX or legacy container {@code from} into {@code to}.
     */
    public void decrypt(File from, File to) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            decrypt(from.length(), new BufferedInputStream(fis), new BufferedOutputStream(fos));
        } finally {
            SharedIOUtil.close(fis);
            SharedIOUtil.close(fos);
        }
    }

    /**
     * Encrypts {@code in} to {@code out} as a VX container, closing both streams.
     */
    public void encrypt(InputStream in, OutputStream out) throws IOException, GeneralSecurityException {
        encrypt(in, out, true, true);
    }

    /**
     * Encrypts {@code in} to {@code out} as a VX container.
     * <p>
     * Reads are tolerant of short returns, so {@code in} may be a socket or a chunked body.
     * Afterwards {@link #getLastHeader()} returns the header that was written.
     *
     * @throws IllegalStateException if a raw key is shorter than {@link #VX_MIN_KEY_SIZE}
     */
    public void encrypt(InputStream in, OutputStream out, boolean closeIn, boolean closeOut)
            throws IOException, GeneralSecurityException {
        if (!textPassword && password.length < VX_MIN_KEY_SIZE) {
            throw new IllegalStateException("key must be at least " + VX_MIN_KEY_SIZE + " bytes");
        }
        int kdfId = textPassword ? VX_KDF_PBKDF2_HMAC_SHA256 : VX_KDF_HKDF_SHA256;
        int kdfParams = textPassword ? pbkdf2Iterations : 0;
        byte[] key = null;
        byte[] plain = null;
        byte[] sealed = null;
        try {
            byte[] rnd = new byte[VX_SALT_SIZE + VX_NONCE_PREFIX_SIZE];
            random().nextBytes(rnd);
            byte[] salt = Arrays.copyOfRange(rnd, 0, VX_SALT_SIZE);
            byte[] prefix = Arrays.copyOfRange(rnd, VX_SALT_SIZE, rnd.length);

            byte[] header = new byte[VX_HEADER_SIZE];
            ByteBuffer bb = ByteBuffer.wrap(header);
            bb.put(VX_MAGIC);
            bb.put((byte) VX_VERSION);
            bb.put((byte) VX_CIPHER_AES_256_GCM);
            bb.put((byte) kdfId);
            bb.putInt(kdfParams);
            bb.put(salt);
            bb.put(prefix);
            bb.putInt(segmentSize);
            out.write(header);
            lastHeader = header;

            key = contentKey(kdfId, kdfParams, salt);
            SecretKeySpec keySpec = new SecretKeySpec(key, CRYPT_ALG);
            byte[] nonce = new byte[VX_NONCE_SIZE];
            plain = ByteBufferUtil.allocateByteArray(segmentSize);//new byte[segmentSize];
            sealed =  ByteBufferUtil.allocateByteArray(segmentSize + VX_TAG_SIZE);//new byte[segmentSize + VX_TAG_SIZE];
            long counter = 0;
            int filled = 0;
            int peek = -1;
            while (true) {
                filled += readUpTo(in, plain, filled, segmentSize - filled);
                boolean last;
                if (filled < segmentSize) {
                    last = true;
                } else {
                    // a full segment: it is the last one only if nothing follows
                    peek = in.read();
                    last = peek < 0;
                }
                nonce(nonce, prefix, counter, last);
                gcm.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(VX_TAG_SIZE * 8, nonce));
                gcm.updateAAD(header);
                int n = gcm.doFinal(plain, 0, filled, sealed, 0);
                out.write(sealed, 0, n);
                if (last) {
                    break;
                }
                if (++counter > MAX_SEGMENT_COUNTER) {
                    throw new IOException("Input exceeds the maximum segment count");
                }
                plain[0] = (byte) peek;
                filled = 1;
            }
            out.flush();
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (closeIn) {
                SharedIOUtil.close(in);
            }
            if (closeOut) {
                SharedIOUtil.close(out);
            }
            ByteBufferUtil.cache(plain, sealed);
        }
    }

    /**
     * Decrypts {@code in} to {@code out}, closing both streams. See
     * {@link #decrypt(long, InputStream, OutputStream, boolean, boolean)} for {@code inLength}.
     */
    public void decrypt(long inLength, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {
        decrypt(inLength, in, out, true, true);
    }

    /**
     * Decrypts a VX or legacy container from {@code in} to {@code out}.
     * <p>
     * {@code inLength} is the container size in bytes. A legacy file requires it, because that
     * format has no length field and a fixed trailer. For a VX container a positive value makes the
     * reader consume exactly that many bytes, so a container can be read out of a longer stream;
     * zero or a negative value reads to end of stream.
     * <p>
     * A VX segment is written to {@code out} only after its tag has verified. The legacy reader
     * writes plaintext before its final HMAC check; use {@link #verify} first when the source is
     * untrusted.
     *
     * @throws AEADBadTagException      if a VX segment or header was altered, or the key is wrong
     * @throws IOException              on a malformed container or I/O error; also raised by the
     *                                  legacy reader when its HMAC does not match
     * @throws IllegalArgumentException if a legacy file is read without a positive {@code inLength}
     */
    public void decrypt(long inLength, InputStream in, OutputStream out, boolean closeIn, boolean closeOut)
            throws IOException, GeneralSecurityException {
        try {
            lastHeader = null;
            byte[] magic = new byte[LEGACY_MAGIC.length];
            readBytes(in, magic);
            if (Arrays.equals(magic, LEGACY_MAGIC)) {
                if (inLength <= 0) {
                    throw new IllegalArgumentException("Legacy AES Crypt v1/v2 containers require inLength");
                }
                decryptLegacy(inLength, in, out);
            } else if (magic[0] == VX_MAGIC[0] && magic[1] == VX_MAGIC[1] && magic[2] == VX_MAGIC[2]
                    && readByte(in) == VX_MAGIC[3]) {
                decryptVX(inLength, in, out);
            } else {
                throw new IOException("Invalid file header");
            }
        } finally {
            if (closeIn) {
                SharedIOUtil.close(in);
            }
            if (closeOut) {
                SharedIOUtil.close(out);
            }
        }
    }

    /**
     * Verifies a container without producing plaintext, closing {@code in}. Same contract as
     * {@link #decrypt(long, InputStream, OutputStream, boolean, boolean)}; returns normally when
     * every tag or HMAC matches.
     */
    public void verify(long inLength, InputStream in) throws IOException, GeneralSecurityException {
        verify(inLength, in, true);
    }

    public void verify(long inLength, InputStream in, boolean closeIn) throws IOException, GeneralSecurityException {
        decrypt(inLength, in, null, closeIn, false);
    }

    /* ------------------------------------------------------------ VX reader */

    private void decryptVX(long inLength, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {
        byte[] header = new byte[VX_HEADER_SIZE];
        System.arraycopy(VX_MAGIC, 0, header, 0, VX_MAGIC.length);
        readBytes(in, header, VX_MAGIC.length, VX_HEADER_SIZE - VX_MAGIC.length);
        ByteBuffer bb = ByteBuffer.wrap(header, VX_MAGIC.length, VX_HEADER_SIZE - VX_MAGIC.length);
        int version = bb.get() & 0xff;
        int cipherId = bb.get() & 0xff;
        int kdfId = bb.get() & 0xff;
        int kdfParams = bb.getInt();
        byte[] salt = new byte[VX_SALT_SIZE];
        bb.get(salt);
        byte[] prefix = new byte[VX_NONCE_PREFIX_SIZE];
        bb.get(prefix);
        int segSize = bb.getInt();
        if (version != VX_VERSION) {
            throw new IOException("Unsupported version number: " + version);
        }
        if (cipherId != VX_CIPHER_AES_256_GCM) {
            throw new IOException("Unsupported cipher: " + cipherId);
        }
        if (kdfId == VX_KDF_HKDF_SHA256) {
            if (kdfParams != 0) {
                throw new IOException("Unsupported key derivation: " + kdfId + "/" + kdfParams);
            }
        } else if (kdfId == VX_KDF_PBKDF2_HMAC_SHA256) {
            if (kdfParams < 1 || kdfParams > VX_MAX_PBKDF2_ITERATIONS) {
                throw new IOException("PBKDF2 iteration count out of range: " + kdfParams);
            }
        } else {
            throw new IOException("Unsupported key derivation: " + kdfId + "/" + kdfParams);
        }
        if (segSize < 1 || segSize > VX_MAX_SEGMENT_SIZE) {
            throw corrupt("segment size " + segSize);
        }
        lastHeader = header;

        long remaining = inLength > 0 ? inLength - VX_HEADER_SIZE : -1;
        if (inLength > 0 && remaining < VX_TAG_SIZE) {
            throw corrupt("length " + inLength + " is shorter than a container");
        }

        byte[] key = contentKey(kdfId, kdfParams, salt);
        byte[] sealed = null;
        byte[] plain = null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, CRYPT_ALG);
            int chunk = segSize + VX_TAG_SIZE;
            sealed = ByteBufferUtil.allocateByteArray(chunk);
            plain = ByteBufferUtil.allocateByteArray(segSize);
            byte[] nonce = new byte[VX_NONCE_SIZE];
            long counter = 0;
            int pending = 0;
            int peek = -1;
            while (true) {
                int n;
                boolean last;
                if (remaining >= 0) {
                    // length-driven: the caller told us where the container ends
                    int want = (int) Math.min(chunk, remaining);
                    readBytes(in, sealed, want);
                    n = want;
                    remaining -= want;
                    last = remaining == 0;
                } else {
                    // stream-driven: a full segment is the last one only if nothing follows
                    n = pending + readUpTo(in, sealed, pending, chunk - pending);
                    pending = 0;
                    if (n < chunk) {
                        last = true;
                    } else {
                        peek = in.read();
                        last = peek < 0;
                    }
                }
                if (n < VX_TAG_SIZE) {
                    throw corrupt("segment shorter than its tag");
                }
                nonce(nonce, prefix, counter, last);
                gcm.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(VX_TAG_SIZE * 8, nonce));
                gcm.updateAAD(header);
                int m;
                try {
                    m = gcm.doFinal(sealed, 0, n, plain, 0);
                } catch (AEADBadTagException e) {
                    AEADBadTagException altered = new AEADBadTagException(
                            "Message has been altered or key incorrect (segment " + counter + ")");
                    altered.initCause(e);
                    throw altered;
                }
                emit(out, plain, 0, m);
                if (last) {
                    break;
                }
                if (++counter > MAX_SEGMENT_COUNTER) {
                    throw corrupt("segment count overflow");
                }
                if (remaining < 0) {
                    sealed[0] = (byte) peek;
                    pending = 1;
                }
            }
            if (out != null) {
                out.flush();
            }
        } finally {
            Arrays.fill(key, (byte) 0);
            ByteBufferUtil.cache(sealed, plain);
        }
    }

    private void readBytes(InputStream in, byte[] bytes, int off, int len) throws IOException {
        if (readUpTo(in, bytes, off, len) != len) {
            throw new IOException("Unexpected end of file");
        }
    }

    /* ------------------------------------------------- legacy v1/v2 reader */

    /**
     * Derives the legacy AES Crypt key: 8192 rounds of SHA-256 over IV1 padded to 32 bytes and the
     * UTF-16LE password.
     */
    private byte[] legacyKey1(byte[] iv, byte[] password) throws GeneralSecurityException {
        if (digest == null) {
            digest = messageDigest(DIGEST_ALG);
        }
        byte[] aesKey = new byte[KEY_SIZE];
        System.arraycopy(iv, 0, aesKey, 0, iv.length);
        for (int i = 0; i < LEGACY_KDF_ROUNDS; i++) {
            digest.reset();
            digest.update(aesKey);
            digest.update(password);
            aesKey = digest.digest();
        }
        return aesKey;
    }

    /**
     * Reads an AES Crypt version 1 or 2 body; the 3 magic bytes have already been consumed.
     * {@code total} tracks every header and trailer byte so the payload size follows from
     * {@code inLength}.
     */
    private void decryptLegacy(long inLength, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {
        if (cbc == null) {
            cbc = cipher(LEGACY_CRYPT_TRANS);
        }
        long total = 3 + 1 + 1 + BLOCK_SIZE + BLOCK_SIZE + KEY_SIZE + SHA_SIZE + 1 + SHA_SIZE;

        int version = readByte(in);
        if (version < 1 || version > 2) {
            throw new IOException("Unsupported version number: " + version);
        }
        readByte(in);  // Reserved.

        if (version == 2) {  // Extensions.
            byte[] ext = new byte[2];
            int len;
            do {
                readBytes(in, ext);
                len = ((0xff & (int) ext[0]) << 8) | (0xff & (int) ext[1]);
                skipFully(in, len);
                total += 2 + len;
            } while (len != 0);
        }

        byte[] key1 = null;
        byte[] ivAndKey2 = null;
        byte[] sealed = null;
        byte[] plain = null;
        try {
            byte[] iv1 = new byte[BLOCK_SIZE];
            readBytes(in, iv1);
            key1 = legacyKey1(iv1, password);
            SecretKeySpec aesKey1 = new SecretKeySpec(key1, CRYPT_ALG);

            byte[] sealedKey = new byte[BLOCK_SIZE + KEY_SIZE];
            readBytes(in, sealedKey);  // IV2 and key2 under key1.
            cbc.init(Cipher.DECRYPT_MODE, aesKey1, new IvParameterSpec(iv1));
            ivAndKey2 = cbc.doFinal(sealedKey);
            IvParameterSpec ivSpec2 = new IvParameterSpec(ivAndKey2, 0, BLOCK_SIZE);
            SecretKeySpec aesKey2 = new SecretKeySpec(ivAndKey2, BLOCK_SIZE, KEY_SIZE, CRYPT_ALG);

            hmac.init(new SecretKeySpec(key1, HMAC_ALG));
            byte[] expected = hmac.doFinal(sealedKey);
            byte[] stored = new byte[SHA_SIZE];
            readBytes(in, stored);
            if (!MessageDigest.isEqual(expected, stored)) {
                throw new IOException("Message has been altered or password incorrect");
            }

            total = inLength - total;  // Payload size.
            if (total < 0 || total % BLOCK_SIZE != 0) {
                throw corrupt("payload length " + total);
            }
            if (total == 0) {  // Empty payload: the loop below is skipped, consume the last-block byte.
                readByte(in);
            }

            cbc.init(Cipher.DECRYPT_MODE, aesKey2, ivSpec2);
            hmac.init(new SecretKeySpec(aesKey2.getEncoded(), HMAC_ALG));
            sealed = ByteBufferUtil.allocateByteArray(LEGACY_BUFFER_SIZE);
            plain = ByteBufferUtil.allocateByteArray(LEGACY_BUFFER_SIZE);
            int maxBlocks = LEGACY_BUFFER_SIZE / BLOCK_SIZE;
            int blockCount = 0;
            for (long block = total / BLOCK_SIZE; block > 0; block -= blockCount) {
                blockCount = (int) (block > maxBlocks ? maxBlocks : block);
                int len = BLOCK_SIZE * blockCount;
                readBytes(in, sealed, len);
                cbc.update(sealed, 0, len, plain);
                hmac.update(sealed, 0, len);
                if (block - blockCount == 0) {
                    int last = readByte(in);  // Last block size mod 16.
                    len = (last > 0 ? len - BLOCK_SIZE + last : len);
                }
                emit(out, plain, 0, len);
            }
            cbc.doFinal();

            expected = hmac.doFinal();
            readBytes(in, stored);
            if (!MessageDigest.isEqual(expected, stored)) {
                throw new IOException("Message has been altered or password incorrect");
            }
            if (out != null) {
                out.flush();
            }
        } finally {
            if (key1 != null) {
                Arrays.fill(key1, (byte) 0);
            }
            if (ivAndKey2 != null) {
                Arrays.fill(ivAndKey2, (byte) 0);
            }
            ByteBufferUtil.cache(sealed, plain);
        }
    }

    /* ------------------------------------------------------- static helpers */

    public static UByteArrayOutputStream encryptMessage(byte[] key, String msg)
            throws IOException, GeneralSecurityException {
        return encryptBAIS(key, new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8)));
    }

    public static UByteArrayOutputStream encryptBuffer(byte[] key, byte[] buffer)
            throws GeneralSecurityException, IOException {
        return encryptBAIS(key, new ByteArrayInputStream(buffer));
    }

    public static UByteArrayOutputStream encryptBAIS(byte[] key, InputStream is)
            throws GeneralSecurityException, IOException {
        AESCrypt c = new AESCrypt(key);
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        c.encrypt(is, baos);
        return baos;
    }

    public static UByteArrayOutputStream decryptBAIS(byte[] key, ByteArrayInputStream bais)
            throws GeneralSecurityException, IOException {
        return decryptBAIS(key, bais, bais.available());
    }

    public static UByteArrayOutputStream decryptBAIS(byte[] key, InputStream is, long length)
            throws GeneralSecurityException, IOException {
        AESCrypt c = new AESCrypt(key);
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        c.decrypt(length, is, baos);
        return baos;
    }

    public static UByteArrayOutputStream decryptBuffer(byte[] key, byte[] buffer)
            throws GeneralSecurityException, IOException {
        return decryptBAIS(key, new ByteArrayInputStream(buffer));
    }

    private static void error(String msg) {
        if (msg != null) {
            System.out.println("Error " + msg);
        }
        System.out.println("AESCrypt e|d fromPath [toPath]");
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                error(null);
            }
            String from = args[1];
            String to = null;
            Console c = System.console();
            if (c == null) {
                error("No console available to read the password");
            }
            char[] psswd = c.readPassword("[%s]", "Password:");
            AESCrypt aes = new AESCrypt(new String(psswd));
            long delta = System.currentTimeMillis();
            switch (args[0]) {
                case "e":
                case "-e":
                    to = args.length == 2 ? from + EXT : args[2];
                    aes.encrypt(from, to);
                    System.out.println("Encryption successful");
                    break;
                case "d":
                case "-d":
                    if (args.length == 2) {
                        if (from.endsWith(EXT)) {
                            to = from.substring(0, from.length() - EXT.length());
                        } else {
                            error("No destination path is defined");
                        }
                    } else {
                        to = args[2];
                    }
                    aes.decrypt(from, to);
                    break;
                default:
                    error(null);
            }
            delta = System.currentTimeMillis() - delta;
            System.out.println("It took " + delta + " millis");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
