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
package org.zoxweb.shared.crypto;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

/**
 * One encrypted value: an AES-256-GCM record whose authenticated attributes bind the ciphertext to
 * the key that wraps it.
 * <p>
 * The canonical form, {@link #toCanonicalID()}, is the attributes below in this fixed order joined
 * by {@code |}: binary as base64url, numbers as digits, absent as empty. It is what a store writes
 * into a text column. {@link #toAssociatedData()} is the same string without the trailing
 * {@code |ct} and is the GCM associated data, so every attribute except the ciphertext is
 * authenticated and a record cannot be re-pointed at another key without failing its tag.
 * <pre>
 * v            int     format version, 1
 * alg          string  A256GCM
 * kdf          string  HKDF-SHA256; the cipher key is derived from the wrapping key with the label "enc"
 * iv           b64url  12-byte random nonce, fresh per encryption
 * data_length  long    plaintext length in bytes
 * mask         string  optional display fragment computed at encryption time (ENCRYPT_MASK fields)
 * exp          long    optional expiry, epoch millis
 * hint         string  optional
 * cipher_data  b64url  ciphertext with the 16-byte GCM tag appended
 * </pre>
 * Text attributes may not contain {@code |}. The class stays a {@link PropertyDAO} so the meta-model
 * and JSON tooling can carry it, but the entity fields (GUID, subject GUID, name, timestamps) are
 * not part of the record and are not authenticated: only the attributes above are.
 * <p>
 * Set {@code mask}, {@code exp} and {@code hint} <em>before</em>
 * encrypting; changing any of them afterwards invalidates the record.
 */
@SuppressWarnings("serial")
public class EncryptedData
        extends PropertyDAO
        implements CryptoBase {

    public static final int VERSION = 1;
    public static final int IV_SIZE = 12;
    public static final int TAG_SIZE = 16;
    /** Separator of the canonical form. */
    public static final char SEP = '|';
    private static final int FIELD_COUNT = 9;

    protected enum Param
            implements GetNVConfig {
        FORMAT_VERSION(NVConfigManager.createNVConfig("v", "Record format version", "Version", true, true, Integer.class)),
        ALGORITHM(NVConfigManager.createNVConfig("alg", "Cipher", "Algorithm", true, true, String.class)),
        KDF(NVConfigManager.createNVConfig("kdf", "Key derivation from the wrapping key", "KDF", true, true, String.class)),
        IV(NVConfigManager.createNVConfig("iv", "GCM nonce", "IV", true, true, byte[].class)),
        DATA_LENGTH(NVConfigManager.createNVConfig("data_length", "clear data length in bytes", "DataLength", true, true, Long.class)),
        MASK(NVConfigManager.createNVConfig("mask", "Display fragment for masked fields", "Mask", false, true, String.class)),
        EXPIRY(NVConfigManager.createNVConfig("exp", "Expiry, epoch millis", "Expiry", false, true, Long.class)),
        HINT(NVConfigManager.createNVConfig("hint", "Hint", "Hint", false, true, String.class)),
        CIPHER_DATA(NVConfigManager.createNVConfig("cipher_data", "Cipher data with the GCM tag appended", "CipherData", true, true, byte[].class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public final static NVConfigEntity NVCE_ENCRYPTED_DATA = new NVConfigEntityPortable("encrypted_data", null, "EncryptedData", false, true, false, false, EncryptedData.class, SharedUtil.extractNVConfigs(Param.values()), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    public EncryptedData() {
        super(NVCE_ENCRYPTED_DATA);
    }

    protected EncryptedData(NVConfigEntity nvce) {
        super(nvce);
    }

    /* ----------------------------------------------------------- attributes */

    public int getVersion() {
        Integer v = lookupValue(Param.FORMAT_VERSION);
        return v != null ? v : 0;
    }

    public void setVersion(int version) {
        setValue(Param.FORMAT_VERSION, version);
    }

    public String getAlgorithm() {
        return lookupValue(Param.ALGORITHM);
    }

    public void setAlgorithm(String algorithm) {
        setValue(Param.ALGORITHM, checkText("alg", algorithm));
    }

    public String getKDF() {
        return lookupValue(Param.KDF);
    }

    public void setKDF(String kdf) {
        setValue(Param.KDF, checkText("kdf", kdf));
    }

    public byte[] getIV() {
        return lookupValue(Param.IV);
    }

    public void setIV(byte[] iv) {
        setValue(Param.IV, iv);
    }

    public long getDataLength() {
        Long l = lookupValue(Param.DATA_LENGTH);
        return l != null ? l : 0;
    }

    public void setDataLength(long dataLength) {
        if (dataLength < 0) {
            throw new IllegalArgumentException("Illegal data length " + dataLength);
        }
        setValue(Param.DATA_LENGTH, dataLength);
    }

    public String getMask() {
        return lookupValue(Param.MASK);
    }

    public void setMask(String mask) {
        setValue(Param.MASK, checkText("mask", mask));
    }

    /**
     * @return expiry as epoch millis, 0 when none.
     */
    public long getExpiry() {
        Long l = lookupValue(Param.EXPIRY);
        return l != null ? l : 0;
    }

    public void setExpiry(long expiry) {
        setValue(Param.EXPIRY, expiry > 0 ? Long.valueOf(expiry) : null);
    }

    public String getHint() {
        return lookupValue(Param.HINT);
    }

    public void setHint(String hint) {
        setValue(Param.HINT, checkText("hint", hint));
    }

    /**
     * @return ciphertext with the GCM tag appended.
     */
    public byte[] getEncryptedData() {
        return lookupValue(Param.CIPHER_DATA);
    }

    public void setEncryptedData(byte[] cipheredData) {
        setValue(Param.CIPHER_DATA, cipheredData);
    }

    /* ---------------------------------------------------------- canonical */

    /**
     * The canonical form without the ciphertext: the GCM associated data.
     */
    public String toAssociatedData() {
        return canonical(false);
    }

    /**
     * The whole record in canonical form, what a store persists.
     */
    @Override
    public String toCanonicalID() {
        return canonical(true);
    }

    private String canonical(boolean includeCipherText) {
        StringBuilder sb = new StringBuilder(96 + (includeCipherText && getEncryptedData() != null ? getEncryptedData().length * 4 / 3 : 0));
        sb.append(getVersion()).append(SEP);
        append(sb, getAlgorithm()).append(SEP);
        append(sb, getKDF()).append(SEP);
        append(sb, encode(getIV())).append(SEP);
        sb.append(getDataLength()).append(SEP);
        append(sb, getMask()).append(SEP);
        if (getExpiry() > 0) {
            sb.append(getExpiry());
        }
        sb.append(SEP);
        append(sb, getHint());
        if (includeCipherText) {
            sb.append(SEP);
            append(sb, encode(getEncryptedData()));
        }
        return sb.toString();
    }

    /**
     * Parses a record written by {@link #toCanonicalID()}.
     *
     * @throws IllegalArgumentException if the text is malformed or carries no version
     */
    public static EncryptedData fromCanonicalID(String canonical) {
        return fromCanonicalID(new EncryptedData(), canonical);
    }

    protected static <T extends EncryptedData> T fromCanonicalID(T ret, String canonical) {
        String[] t = split(canonical, FIELD_COUNT);
        ret.setVersion(parseInt("v", t[0]));
        ret.setAlgorithm(text(t[1]));
        ret.setKDF(text(t[2]));
        ret.setIV(decode(text(t[3])));
        ret.setDataLength(parseLong("data_length", t[4]));
        ret.setMask(text(t[5]));
        ret.setExpiry(parseLong("exp", t[6]));
        ret.setHint(text(t[7]));
        ret.setEncryptedData(decode(text(t[8])));
        return ret;
    }

    /* ------------------------------------------------ shared record helpers */

    static StringBuilder append(StringBuilder sb, String value) {
        if (value != null) {
            sb.append(value);
        }
        return sb;
    }

    /**
     * Text attributes are stored raw, so the separator is refused in them.
     */
    static String checkText(String name, String value) {
        if (value != null && value.indexOf(SEP) >= 0) {
            throw new IllegalArgumentException("attribute " + name + " may not contain '" + SEP + "'");
        }
        return value;
    }

    /**
     * Splits a canonical form into exactly {@code count} fields; the first, the version, must be set.
     */
    static String[] split(String canonical, int count) {
        if (SUS.isEmpty(canonical)) {
            throw new IllegalArgumentException("empty record");
        }
        String[] t = new String[count];
        int start = 0;
        int n = 0;
        while (n < count) {
            int sep = canonical.indexOf(SEP, start);
            if (sep < 0) {
                break;
            }
            t[n++] = canonical.substring(start, sep);
            start = sep + 1;
        }
        if (n != count - 1 || canonical.indexOf(SEP, start) >= 0) {
            throw new IllegalArgumentException("Invalid record format: expected " + count + " fields");
        }
        t[n] = canonical.substring(start);
        if (t[0].isEmpty()) {
            throw new IllegalArgumentException("record has no format version");
        }
        return t;
    }

    static String text(String token) {
        return token.isEmpty() ? null : token;
    }

    static int parseInt(String name, String token) {
        try {
            return token.isEmpty() ? 0 : Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("attribute " + name + " is not a number: " + token);
        }
    }

    static long parseLong(String name, String token) {
        try {
            return token.isEmpty() ? 0 : Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("attribute " + name + " is not a number: " + token);
        }
    }

    static String encode(byte[] bytes) {
        return bytes != null ? SharedStringUtil.toString(SharedBase64.encode(Base64Type.URL, bytes, 0, bytes.length)) : null;
    }

    static byte[] decode(String text) {
        return text != null ? SharedBase64.decode(Base64Type.URL, text) : null;
    }
}
