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

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.Const.TypeInBytes;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

//import zoxsoft.util.UByteArrayOutputStream;
//import zoxsoft.util.UIO;
//import zoxsoft.util.UString;

/**
 * This class provides methods to encrypt and decrypt files using
 * <a href="http://www.aescrypt.com/aes_file_format.html">aescrypt file format</a>,
 * version 1 or 2.
 * <p>
 * Requires Java 6 and <a href="http://java.sun.com/javase/downloads/index.jsp">Java Cryptography
 * Extension (JCE) Unlimited Strength Jurisdiction Policy Files</a>.
 * <p>
 * Thread-safety and sharing: this class is not thread-safe.<br>
 * {@code AESCrypt} objects can be used as Commands (create, use once and dispose),
 * or reused to perform multiple operations (not concurrently though).
 *
 * @author Vócali Sistemas Inteligentes
 */
public class AESCrypt {

    private static final String JCE_EXCEPTION_MESSAGE = "Please make sure "
            + "\"Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files\" "
            + "(http://java.sun.com/javase/downloads/index.jsp) is installed on your JRE.";
    //private static final String RANDOM_ALG = "SHA1PRNG";
    private static final String DIGEST_ALG = "SHA-256";
    private static final String HMAC_ALG = "HmacSHA256";
    private static final String CRYPT_ALG = "AES";
    private static final String CRYPT_TRANS = "AES/CBC/NoPadding";

    private static final int KEY_SIZE = 32;
    private static final int BLOCK_SIZE = 16;
    private static final int SHA_SIZE = 32;

    private static final String EXT = ".aes";

    public static boolean debug = false;
    private byte[] password;
    private final Cipher cipher;
    private final Mac hmac;
    private final SecureRandom random;
    private final MessageDigest digest;
    private IvParameterSpec ivSpec1;
    private SecretKeySpec aesKey1;
    private IvParameterSpec ivSpec2;
    private SecretKeySpec aesKey2;

    /**
     * Prints a debug message on standard output if DEBUG mode is turned on.
     */
    private static void dbg(String message) {
        if (debug) {
            System.out.println("[AESCrypt] " + message);
        }
    }


    /**
     * Prints a debug message on standard output if DEBUG mode is turned on.
     */
    private static void dbg(String message, byte[] bytes) {
        if (debug) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(message);
            buffer.append("[");
            buffer.append(bytes.length);
            buffer.append(":");
            buffer.append(SharedStringUtil.bytesToHex(bytes));
            buffer.append("]");
            dbg(buffer.toString());
            //System.out.println(buffer.toString());
        }
    }


    /**
     * SHA256 digest over given byte array and random bytes.<br>
     * {@code bytes.length} * {@code num} random bytes are added to the digest.
     * <p>
     * The generated hash is saved back to the original byte array.<br> Maximum array size is {@link
     * #SHA_SIZE} bytes.
     */
    protected void digestRandomBytes(byte[] bytes, int num) {
        assert bytes.length <= SHA_SIZE;

        digest.reset();
        digest.update(bytes);
        for (int i = 0; i < num; i++) {
            random.nextBytes(bytes);
            digest.update(bytes);
        }
        System.arraycopy(digest.digest(), 0, bytes, 0, bytes.length);
    }

    /**
     * Generates a pseudo-random IV based on time and this computer's MAC.
     * <p>
     * This IV is used to crypt IV 2 and AES key 2 in the file.
     * @return IV.
     */
//	protected  byte[] generateIV1() {
//		byte[] iv = new byte[BLOCK_SIZE];
//		long time = System.currentTimeMillis();
//		byte[] mac = MAC_ADDRESS;
//		
//		if ( MAC_ADDRESS == null)
//		{
//			try 
//			{
//				LOCK.lock();
//				if (MAC_ADDRESS == null)
//				{
//					Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
//					while (mac == null && ifaces.hasMoreElements()) 
//					{
//						NetworkInterface ni = ifaces.nextElement();
//						//System.out.println( ni.getDisplayName());
//						mac = ni.getHardwareAddress();
//					
//						// quick mac address validation
//						// mac not null and length = 6
//						// at least one byte is not zero
//						boolean validMAC = false;
//						if ( mac != null && mac.length == MACADDRESS_LENGTH)
//						{
//							
//							for ( byte b : mac)
//							{
//								if ( b != 0)
//								{
//									validMAC = true;
//									break;
//								}
//							}
//						}
//						
//						if (validMAC)
//						{
//							MAC_ADDRESS = mac;
//							//System.out.println("We Found a MAC address " + ni.getName());
//							break;
//						}
//						mac = null;		
//					}
//				}
//				else
//				{
//					mac=MAC_ADDRESS;
//				}
//			} 
//			catch (Exception e) 
//			{
//				e.printStackTrace();
//				// Ignore.
//			}
//			finally
//			{
//				LOCK.unlock();
//			}
//		
//		}
//		if (mac == null) {
//			mac = DEFAULT_MAC;
//		}

//		UUID uuid = UUID.randomUUID();
//		long val = uuid.getLeastSignificantBits();
//		for (int i = 0; i < 8; i++) {
//			iv[i] = (byte) (val >> (i * 8));
//		}
//		
//		val = uuid.getMostSignificantBits();
//		for (int i = 0; i < 8; i++) {
//			iv[i+8] = (byte) (val >> (i * 8));
//		}
//		//System.arraycopy(mac, 0, iv, 8, mac.length);
//		digestRandomBytes(iv, 256);
//		return iv;
//	}


    /**
     * Generates an AES key starting with an IV and applying the supplied user password.
     * <p>
     * This AES key is used to crypt IV 2 and AES key 2.
     *
     * @return AES key of {@link #KEY_SIZE} bytes.
     */
    protected byte[] generateAESKey1(byte[] iv, byte[] password) {
        byte[] aesKey = new byte[KEY_SIZE];
        System.arraycopy(iv, 0, aesKey, 0, iv.length);
        for (int i = 0; i < 8192; i++) {
            digest.reset();
            digest.update(aesKey);
            digest.update(password);
            aesKey = digest.digest();
        }
        return aesKey;
    }

    /**
     * Generates the random IV used to crypt file contents.
     * @return IV 2.
     */
//	protected byte[] generateIV2() {
//		byte[] iv = generateRandomBytes(BLOCK_SIZE);
//		digestRandomBytes(iv, 256);
//		return iv;
//	}


    /**
     * Generates the random AES key used to crypt file contents.
     */
//	protected byte[] generateAESKey2() {
//		byte[] aesKey = generateRandomBytes(KEY_SIZE);
//		digestRandomBytes(aesKey, 32);
//		return aesKey;
//	}
    protected void readBytes(InputStream in, byte[] bytes) throws IOException {
        readBytes(in, bytes, bytes.length);
    }


    /**
     * Utility method to read bytes from a stream until the given array is fully filled.
     *
     * @throws IOException if the array can't be filled.
     */
    protected void readBytes(InputStream in, byte[] bytes, int len) throws IOException {
        if (in.read(bytes, 0, len) != len) {
            throw new IOException("Unexpected end of file");
        }
    }

    /**************
     * PUBLIC API *
     **************/


    /**
     * Builds an object to encrypt or decrypt files with the given password.
     *
     * @throws GeneralSecurityException     if the platform does not support the required cryptographic
     *                                      methods.
     * @throws UnsupportedEncodingException if UTF-16 encoding is not supported.
     */
    public AESCrypt(byte password[]) throws GeneralSecurityException, UnsupportedEncodingException {
        try {
            //debug = dbg;
            setPassword(password);
            random = SecUtil.defaultSecureRandom();
            digest = MessageDigest.getInstance(DIGEST_ALG);
            cipher = Cipher.getInstance(CRYPT_TRANS);
            hmac = Mac.getInstance(HMAC_ALG);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException(JCE_EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Builds an object to encrypt or decrypt files with the given password.
     *
     * @throws GeneralSecurityException     if the platform does not support the required cryptographic
     *                                      methods.
     * @throws UnsupportedEncodingException if UTF-16 encoding is not supported.
     */
    public AESCrypt(String password) throws GeneralSecurityException, UnsupportedEncodingException {
        this(password.getBytes("UTF-16LE"));
    }


    /**
     * Changes the password this object uses to encrypt and decrypt.
     *
     * @throws UnsupportedEncodingException if UTF-16 encoding is not supported.
     */
    public void setPassword(String password) throws UnsupportedEncodingException {
        setPassword(password.getBytes("UTF-16LE"));
    }

    /**
     * Changes the password this object uses to encrypt and decrypt.
     *
     * @throws UnsupportedEncodingException if UTF-16 encoding is not supported.
     */
    public void setPassword(byte pass[]) throws UnsupportedEncodingException {
        SUS.checkIfNulls("Can't have a null password", pass);
        this.password = pass;
        dbg("Using password: ", this.password);
    }


    /**
     * The file at {@code fromPath} is encrypted and saved at {@code toPath} location.
     * <p>
     * {@code version} can be either 1 or 2.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void encrypt(int version, String fromPath, String toPath)
            throws IOException, GeneralSecurityException {
        encrypt(version, new File(fromPath), new File(toPath));
    }


    /**
     * The file at {@code fromPath} is encrypted and saved at {@code toPath} location.
     * <p>
     * {@code version} can be either 1 or 2.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void encrypt(int version, File fromPath, File toPath)
            throws IOException, GeneralSecurityException {

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(fromPath);
            fos = new FileOutputStream(toPath);
            encrypt(version, new BufferedInputStream(fis), new BufferedOutputStream(fos));
        } finally {
            SharedIOUtil.close(fis);
            SharedIOUtil.close(fos);
        }


    }

    /**
     * The file at {@code fromPath} is decrypted and saved at {@code toPath} location.
     * <p>
     * Source file can be encrypted using version 1 or 2 of aescrypt.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void decrypt(String fromPath, String toPath)
            throws IOException, GeneralSecurityException {

        decrypt(new File(fromPath), new File(toPath));

    }


    /**
     * The file at {@code fromPath} is decrypted and saved at {@code toPath} location.
     * <p>
     * Source file can be encrypted using version 1 or 2 of aescrypt.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void decrypt(File fromPath, File toPath)
            throws IOException, GeneralSecurityException {

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(fromPath);
            fos = new FileOutputStream(toPath);
            decrypt(fromPath.length(), new BufferedInputStream(fis), new BufferedOutputStream(fos));
        } finally {
            SharedIOUtil.close(fis);
            SharedIOUtil.close(fos);
        }
    }


    public void encrypt(int version, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {
        encrypt(version, in, out, true, true);
    }

    /**
     * The file at {@code fromPath} is encrypted and saved at {@code toPath} location.
     * <p>
     * {@code version} can be either 1 or 2.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void encrypt(int version, InputStream in, OutputStream out, boolean closeIn,
                        boolean closeOut)
            throws IOException, GeneralSecurityException {

        byte[] text = null;
        try {
            ivSpec1 = new IvParameterSpec(
                    CryptoUtil.generateKey(CRYPT_ALG, TypeInBytes.BYTE.sizeInBits(BLOCK_SIZE)).getEncoded());
            aesKey1 = new SecretKeySpec(generateAESKey1(ivSpec1.getIV(), password), CRYPT_ALG);
            ivSpec2 = new IvParameterSpec(
                    CryptoUtil.generateKey(CRYPT_ALG, TypeInBytes.BYTE.sizeInBits(BLOCK_SIZE)).getEncoded());
            aesKey2 = new SecretKeySpec(
                    CryptoUtil.generateKey(CRYPT_ALG, TypeInBytes.BYTE.sizeInBits(KEY_SIZE)).getEncoded(),
                    CRYPT_ALG);
            if (debug) {
                dbg("IV1: ", ivSpec1.getIV());
                dbg("AES1: ", aesKey1.getEncoded());
                dbg("IV2: ", ivSpec2.getIV());
                dbg("AES2: ", aesKey2.getEncoded());
            }

            out.write("AES".getBytes("UTF-8"));  // Heading.
            out.write(version);  // Version.
            out.write(0);  // Reserved.
            if (version == 2) {  // No extensions.
                out.write(0);
                out.write(0);
            }
            out.write(ivSpec1.getIV());  // Initialization Vector.

            text = new byte[BLOCK_SIZE + KEY_SIZE];
            cipher.init(Cipher.ENCRYPT_MODE, aesKey1, ivSpec1);
            cipher.update(ivSpec2.getIV(), 0, BLOCK_SIZE, text);
            cipher.doFinal(aesKey2.getEncoded(), 0, KEY_SIZE, text, BLOCK_SIZE);
            out.write(text);  // Crypted IV and key.
            if (debug) {
                dbg("IV2 + AES2 ciphertext: ", text);
            }

            hmac.init(new SecretKeySpec(aesKey1.getEncoded(), HMAC_ALG));
            text = hmac.doFinal(text);
            out.write(text);  // HMAC from previous cyphertext.
            if (debug) {
                dbg("HMAC1: ", text);
            }

            cipher.init(Cipher.ENCRYPT_MODE, aesKey2, ivSpec2);
            if (debug) {
                dbg("key " + aesKey2.getAlgorithm());
            }
            hmac.init(new SecretKeySpec(aesKey2.getEncoded(), HMAC_ALG));

            //text = new byte[BLOCK_SIZE];
            // to optimize the scanner
            text = new byte[2048];
            int len, last = 0;
            while ((len = in.read(text)) > 0) {

                int bufferLen = (len / BLOCK_SIZE) * BLOCK_SIZE;
                if (len % BLOCK_SIZE != 0) {
                    bufferLen += BLOCK_SIZE;
                }

//				cipher.update(text, 0, BLOCK_SIZE, text);
                cipher.update(text, 0, bufferLen, text);
                hmac.update(text, 0, bufferLen);
                out.write(text, 0, bufferLen);  // Crypted file data block.
                last = len;
            }
            last &= 0x0f;
            out.write(last);  // Last block size mod 16.
            if (debug) {
                dbg("Last block size mod 16: " + last);
            }

            text = hmac.doFinal();
            out.write(text);  // HMAC from previous ciphertext.
            if (debug) {
                dbg("HMAC2: ", text);
            }
        } catch (InvalidKeyException e) {
            throw new GeneralSecurityException(JCE_EXCEPTION_MESSAGE, e);
        } finally {
            if (closeIn) {
                SharedIOUtil.close(in);
            }
            if (closeOut) {
                SharedIOUtil.close(out);
            }
        }
    }


    public void decrypt(long inLength, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {
        decrypt(inLength, in, out, true, true);
    }

    /**
     * The file at {@code fromPath} is decrypted and saved at {@code toPath} location.
     * <p>
     * Source file can be encrypted using version 1 or 2 of aescrypt.
     *
     * @throws IOException              when there are I/O errors.
     * @throws GeneralSecurityException if the platform does not support the required cryptographic
     *                                  methods.
     */
    public void decrypt(long inLength, InputStream in, OutputStream out, boolean closeIn,
                        boolean closeOut)
            throws IOException, GeneralSecurityException {

        byte[] text = null, backup = null;
        long total = 3 + 1 + 1 + BLOCK_SIZE + BLOCK_SIZE + KEY_SIZE + SHA_SIZE + 1 + SHA_SIZE;
        int version;
        try {

            text = new byte[3];
            readBytes(in, text);  // Heading.
            if (!new String(text, "UTF-8").equals("AES")) {
                throw new IOException("Invalid file header");
            }

            version = in.read();  // Version.
            if (version < 1 || version > 2) {
                throw new IOException("Unsupported version number: " + version);
            }

            if (debug) {
                dbg("Version: " + version);
            }

            in.read();  // Reserved.

            if (version == 2) {  // Extensions.
                text = new byte[2];
                int len;
                do {
                    readBytes(in, text);
                    len = ((0xff & (int) text[0]) << 8) | (0xff & (int) text[1]);
                    if (in.skip(len) != len) {
                        throw new IOException("Unexpected end of extension");
                    }
                    total += 2 + len;
                    if (debug) {
                        dbg("Skipped extension sized: " + len);
                    }
                } while (len != 0);
            }

            text = new byte[BLOCK_SIZE];
            readBytes(in, text);  // Initialization Vector.
            ivSpec1 = new IvParameterSpec(text);
            aesKey1 = new SecretKeySpec(generateAESKey1(ivSpec1.getIV(), password), CRYPT_ALG);
            if (debug) {
                dbg("IV1: ", ivSpec1.getIV());
            }
            if (debug) {
                dbg("AES1: ", aesKey1.getEncoded());
            }

            cipher.init(Cipher.DECRYPT_MODE, aesKey1, ivSpec1);
            backup = new byte[BLOCK_SIZE + KEY_SIZE];
            readBytes(in, backup);  // IV and key to decrypt file contents.
            if (debug) {
                dbg("IV2 + AES2 ciphertext: ", backup);
            }
            text = cipher.doFinal(backup);
            ivSpec2 = new IvParameterSpec(text, 0, BLOCK_SIZE);
            aesKey2 = new SecretKeySpec(text, BLOCK_SIZE, KEY_SIZE, CRYPT_ALG);
            if (debug) {
                dbg("IV2: ", ivSpec2.getIV());
            }

            if (debug) {
                dbg("AES2: ", aesKey2.getEncoded());
            }

            hmac.init(new SecretKeySpec(aesKey1.getEncoded(), HMAC_ALG));
            backup = hmac.doFinal(backup);
            text = new byte[SHA_SIZE];
            readBytes(in, text);  // HMAC and authenticity test.
            if (!Arrays.equals(backup, text)) {
                throw new IOException("Message has been altered or password incorrect");
            }
            if (debug) {
                dbg("HMAC1: ", text);
            }

            total = inLength - total;  // Payload size.
            if (total % BLOCK_SIZE != 0) {
                throw new IOException("Input file is corrupt");
            }
            if (total == 0) {  // Hack: empty files won't enter block-processing for-loop below.
                in.read();  // Skip last block size mod 16.
            }

            if (debug) {
                dbg("Payload size: " + total);
            }

            cipher.init(Cipher.DECRYPT_MODE, aesKey2, ivSpec2);
            hmac.init(new SecretKeySpec(aesKey2.getEncoded(), HMAC_ALG));
            backup = new byte[2048];
            text = new byte[2048];
            int blockCount = 0;
            int maxBlocks = backup.length / BLOCK_SIZE;
            for (long block = (total / BLOCK_SIZE); block > 0; block -= blockCount) {

                blockCount = (int) (block > maxBlocks ? maxBlocks : block);
                int len = BLOCK_SIZE * blockCount;
                if (in.read(backup, 0, len) != len) {  // Cyphertext block.
                    throw new IOException("Unexpected end of file contents");
                }
                cipher.update(backup, 0, len, text);
                hmac.update(backup, 0, len);
                if (block - blockCount == 0) {
                    int last = in.read();  // Last block size mod 16.
                    dbg("Last block size mod 16: " + last);
                    len = (last > 0 ? len - BLOCK_SIZE + last : len);
                }
                out.write(text, 0, len);
            }
            // useless write since in this context cipher.doFinal() returns empty array
            out.write(cipher.doFinal());

            backup = hmac.doFinal();
            text = new byte[SHA_SIZE];
            readBytes(in, text);  // HMAC and authenticity test.
            if (!Arrays.equals(backup, text)) {
                throw new IOException("Message has been altered or password incorrect");
            }

            if (debug) {
                dbg("HMAC2: ", text);
            }
        } catch (InvalidKeyException e) {
            throw new GeneralSecurityException(JCE_EXCEPTION_MESSAGE, e);
        } finally {
            if (closeIn) {
                SharedIOUtil.close(in);
            }
            if (closeOut) {
                SharedIOUtil.close(out);
            }
        }
    }

    public static UByteArrayOutputStream encryptMessage(byte[] password, String msg)
            throws IOException, GeneralSecurityException {
        return encryptBAIS(password, new ByteArrayInputStream(msg.getBytes()));
    }


    public static UByteArrayOutputStream encryptBuffer(byte[] password, byte buffer[])
            throws GeneralSecurityException, IOException {
        return encryptBAIS(password, new ByteArrayInputStream(buffer));
    }

//	public static UByteArrayOutputStream encryptBAIS( byte[] password, ByteArrayInputStream bais) 
//		throws GeneralSecurityException, IOException
//	{
//		AESCrypt c = new AESCrypt( password);
//		UByteArrayOutputStream baos = new UByteArrayOutputStream(bais.available() +256);
//		c.encrypt(2, bais, baos);
//		return baos;
//	}


    public static UByteArrayOutputStream encryptBAIS(byte[] password, InputStream is)
            throws GeneralSecurityException, IOException {
        AESCrypt c = new AESCrypt(password);
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        c.encrypt(2, is, baos);
        return baos;
    }


    public static UByteArrayOutputStream decryptBAIS(byte[] password, ByteArrayInputStream bais)
            throws GeneralSecurityException, IOException {
        return decryptBAIS(password, bais, bais.available());
    }


    public static UByteArrayOutputStream decryptBAIS(byte[] password, InputStream is, long length)
            throws GeneralSecurityException, IOException {
        AESCrypt c = new AESCrypt(password);
        UByteArrayOutputStream baos = new UByteArrayOutputStream();
        c.decrypt(length, is, baos);
        return baos;
    }


    public static UByteArrayOutputStream decryptBuffer(byte[] password, byte[] buffer)
            throws GeneralSecurityException, IOException {
        return decryptBAIS(password, new ByteArrayInputStream(buffer));
    }

    private static void error(String msg) {
        if (msg != null) {
            System.out.println("Error " + msg);
        }

        System.out.println("AESCrypt e|d fromPath [toPath]");
        System.out.println("Java version 6 or higher is required with 256 bit encryption enabled");
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
            char psswd[] = c.readPassword("[%s]", "Password:");

            String password = new String(psswd);
            AESCrypt aes = new AESCrypt(password);
            long delta = System.currentTimeMillis();
            switch (args[0]) {
                case "e":
                case "-e":
                    if (args.length == 2) {
                        to = from + EXT;
                    } else {
                        to = args[2];
                    }
                    aes.encrypt(2, from, to);
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
