package org.zoxweb.server.security;

import org.zoxweb.shared.util.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ASN.1 DER codecs for X.509 certificate OID values.
 * Uses DataDecoder pattern for decoding binary ASN.1 data into readable structures.
 */
public final class OIDCodecs {




    private static volatile Map<String, String> OID_MAP_internal;

    public static String oidLookup(String oid)
    {
        if(OID_MAP_internal == null)
        {
            SecUtil.SEC_LOCK.lock(true);
            try {
                if (OID_MAP_internal == null) {
                    Map<String, String> m = new HashMap<>();

                    // =========================================================================
                    // Subject / Issuer Attribute OIDs (RDNs) - X.500 Distinguished Names
                    // =========================================================================
                    m.put("2.5.4.3", "CN (Common Name)");
                    m.put("2.5.4.4", "SN (Surname)");
                    m.put("2.5.4.5", "SERIALNUMBER");
                    m.put("2.5.4.6", "C (Country)");
                    m.put("2.5.4.7", "L (Locality)");
                    m.put("2.5.4.8", "ST (State/Province)");
                    m.put("2.5.4.9", "STREET (Street Address)");
                    m.put("2.5.4.10", "O (Organization)");
                    m.put("2.5.4.11", "OU (Organizational Unit)");
                    m.put("2.5.4.12", "T (Title)");
                    m.put("2.5.4.13", "Description");
                    m.put("2.5.4.17", "Postal Code");
                    m.put("2.5.4.41", "Name");
                    m.put("2.5.4.42", "GN (Given Name)");
                    m.put("2.5.4.43", "Initials");
                    m.put("2.5.4.44", "Generation Qualifier");
                    m.put("2.5.4.46", "DN Qualifier");
                    m.put("2.5.4.65", "Pseudonym");
                    m.put("1.2.840.113549.1.9.1", "emailAddress");
                    m.put("0.9.2342.19200300.100.1.25", "DC (Domain Component)");
                    m.put("0.9.2342.19200300.100.1.1", "UID (User ID)");

                    // =========================================================================
                    // Signature / Public Key Algorithms
                    // =========================================================================
                    // RSA
                    m.put("1.2.840.113549.1.1.1", "RSA Encryption");
                    m.put("1.2.840.113549.1.1.4", "md5WithRSAEncryption (deprecated)");
                    m.put("1.2.840.113549.1.1.5", "sha1WithRSAEncryption");
                    m.put("1.2.840.113549.1.1.10", "RSASSA-PSS");
                    m.put("1.2.840.113549.1.1.11", "sha256WithRSAEncryption");
                    m.put("1.2.840.113549.1.1.12", "sha384WithRSAEncryption");
                    m.put("1.2.840.113549.1.1.13", "sha512WithRSAEncryption");
                    m.put("1.2.840.113549.1.1.14", "sha224WithRSAEncryption");

                    // ECDSA
                    m.put("1.2.840.10045.2.1", "EC Public Key");
                    m.put("1.2.840.10045.4.1", "ecdsaWithSHA1");
                    m.put("1.2.840.10045.4.3.1", "ecdsaWithSHA224");
                    m.put("1.2.840.10045.4.3.2", "ecdsaWithSHA256");
                    m.put("1.2.840.10045.4.3.3", "ecdsaWithSHA384");
                    m.put("1.2.840.10045.4.3.4", "ecdsaWithSHA512");

                    // EdDSA / X25519 / X448
                    m.put("1.3.101.110", "X25519 (Key Agreement)");
                    m.put("1.3.101.111", "X448 (Key Agreement)");
                    m.put("1.3.101.112", "Ed25519");
                    m.put("1.3.101.113", "Ed448");

                    // DSA
                    m.put("1.2.840.10040.4.1", "DSA Public Key");
                    m.put("1.2.840.10040.4.3", "dsaWithSHA1");
                    m.put("2.16.840.1.101.3.4.3.1", "dsaWithSHA224");
                    m.put("2.16.840.1.101.3.4.3.2", "dsaWithSHA256");

                    // =========================================================================
                    // X.509 Certificate Extensions
                    // =========================================================================
                    m.put("2.5.29.9", "Subject Directory Attributes");
                    m.put("2.5.29.14", "Subject Key Identifier");
                    m.put("2.5.29.15", "Key Usage");
                    m.put("2.5.29.16", "Private Key Usage Period");
                    m.put("2.5.29.17", "Subject Alternative Name");
                    m.put("2.5.29.18", "Issuer Alternative Name");
                    m.put("2.5.29.19", "Basic Constraints");
                    m.put("2.5.29.20", "CRL Number");
                    m.put("2.5.29.21", "Reason Code");
                    m.put("2.5.29.23", "Hold Instruction Code");
                    m.put("2.5.29.24", "Invalidity Date");
                    m.put("2.5.29.27", "Delta CRL Indicator");
                    m.put("2.5.29.28", "Issuing Distribution Point");
                    m.put("2.5.29.29", "Certificate Issuer");
                    m.put("2.5.29.30", "Name Constraints");
                    m.put("2.5.29.31", "CRL Distribution Points");
                    m.put("2.5.29.32", "Certificate Policies");
                    m.put("2.5.29.33", "Policy Mappings");
                    m.put("2.5.29.35", "Authority Key Identifier");
                    m.put("2.5.29.36", "Policy Constraints");
                    m.put("2.5.29.37", "Extended Key Usage");
                    m.put("2.5.29.46", "Freshest CRL");
                    m.put("2.5.29.54", "Inhibit Any-Policy");

                    // Authority Information Access
                    m.put("1.3.6.1.5.5.7.1.1", "Authority Information Access");
                    m.put("1.3.6.1.5.5.7.1.11", "Subject Information Access");
                    m.put("1.3.6.1.5.5.7.48.1", "OCSP");
                    m.put("1.3.6.1.5.5.7.48.2", "CA Issuers");

                    // =========================================================================
                    // Extended Key Usages (EKU)
                    // =========================================================================
                    m.put("1.3.6.1.5.5.7.3.1", "Server Authentication");
                    m.put("1.3.6.1.5.5.7.3.2", "Client Authentication");
                    m.put("1.3.6.1.5.5.7.3.3", "Code Signing");
                    m.put("1.3.6.1.5.5.7.3.4", "Email Protection");
                    m.put("1.3.6.1.5.5.7.3.5", "IPSec End System");
                    m.put("1.3.6.1.5.5.7.3.6", "IPSec Tunnel");
                    m.put("1.3.6.1.5.5.7.3.7", "IPSec User");
                    m.put("1.3.6.1.5.5.7.3.8", "Time Stamping");
                    m.put("1.3.6.1.5.5.7.3.9", "OCSP Signing");

                    // Microsoft EKUs
                    m.put("1.3.6.1.4.1.311.10.3.1", "Microsoft Certificate Trust List Signing");
                    m.put("1.3.6.1.4.1.311.10.3.3", "Microsoft Server Gated Crypto");
                    m.put("1.3.6.1.4.1.311.10.3.4", "Microsoft Encrypted File System");
                    m.put("1.3.6.1.4.1.311.10.3.12", "Microsoft Document Signing");
                    m.put("1.3.6.1.4.1.311.20.2.2", "Microsoft Smart Card Logon");
                    m.put("1.3.6.1.4.1.311.21.5", "Microsoft CA Exchange");

                    // Netscape
                    m.put("2.16.840.1.113730.4.1", "Netscape Server Gated Crypto");

                    // CA/Browser Forum - Certificate Types
                    m.put("2.23.140.1.1", "Extended Validation (EV) SSL");
                    m.put("2.23.140.1.2.1", "Domain Validated (DV) SSL");
                    m.put("2.23.140.1.2.2", "Organization Validated (OV) SSL");
                    m.put("2.23.140.1.2.3", "Individual Validated SSL");
                    m.put("2.23.140.1.3", "Extended Validation (EV) Code Signing");
                    m.put("2.23.140.1.4.1", "Code Signing Requirements");
                    m.put("2.23.140.1.31", "Onion Domain Validation");

                    // =========================================================================
                    // Hash Algorithms
                    // =========================================================================
                    m.put("1.2.840.113549.2.5", "MD5");
                    m.put("1.3.14.3.2.26", "SHA-1");
                    m.put("2.16.840.1.101.3.4.2.4", "SHA-224");
                    m.put("2.16.840.1.101.3.4.2.1", "SHA-256");
                    m.put("2.16.840.1.101.3.4.2.2", "SHA-384");
                    m.put("2.16.840.1.101.3.4.2.3", "SHA-512");
                    m.put("2.16.840.1.101.3.4.2.5", "SHA-512/224");
                    m.put("2.16.840.1.101.3.4.2.6", "SHA-512/256");

                    // SHA-3
                    m.put("2.16.840.1.101.3.4.2.7", "SHA3-224");
                    m.put("2.16.840.1.101.3.4.2.8", "SHA3-256");
                    m.put("2.16.840.1.101.3.4.2.9", "SHA3-384");
                    m.put("2.16.840.1.101.3.4.2.10", "SHA3-512");
                    m.put("2.16.840.1.101.3.4.2.11", "SHAKE128");
                    m.put("2.16.840.1.101.3.4.2.12", "SHAKE256");

                    // =========================================================================
                    // Certificate Transparency (Google OIDs)
                    // =========================================================================
                    m.put("1.3.6.1.4.1.11129.2.4.2", "CT Precertificate SCTs");
                    m.put("1.3.6.1.4.1.11129.2.4.3", "CT Precertificate Poison");
                    m.put("1.3.6.1.4.1.11129.2.4.4", "CT Embedded SCT Extension");
                    m.put("1.3.6.1.4.1.11129.2.4.5", "CT Embedded OCSP SCTs");
                    m.put("1.3.6.1.4.1.11129.2.4.6", "CT Embedded CRL SCTs");

                    // =========================================================================
                    // Elliptic Curves (Named Curves)
                    // =========================================================================
                    m.put("1.2.840.10045.3.1.7", "prime256v1 (P-256/secp256r1)");
                    m.put("1.3.132.0.34", "secp384r1 (P-384)");
                    m.put("1.3.132.0.35", "secp521r1 (P-521)");
                    m.put("1.3.132.0.10", "secp256k1 (Bitcoin)");
                    m.put("1.2.840.10045.3.1.1", "prime192v1 (P-192)");
                    m.put("1.3.132.0.31", "secp192r1");
                    m.put("1.3.132.0.32", "secp224k1");
                    m.put("1.3.132.0.33", "secp224r1 (P-224)");

                    // Brainpool Curves
                    m.put("1.3.36.3.3.2.8.1.1.7", "brainpoolP256r1");
                    m.put("1.3.36.3.3.2.8.1.1.11", "brainpoolP384r1");
                    m.put("1.3.36.3.3.2.8.1.1.13", "brainpoolP512r1");

                    // =========================================================================
                    // PKCS Standards
                    // =========================================================================
                    m.put("1.2.840.113549.1.7.1", "PKCS#7 Data");
                    m.put("1.2.840.113549.1.7.2", "PKCS#7 Signed Data");
                    m.put("1.2.840.113549.1.7.3", "PKCS#7 Enveloped Data");
                    m.put("1.2.840.113549.1.7.6", "PKCS#7 Encrypted Data");
                    m.put("1.2.840.113549.1.9.3", "Content Type");
                    m.put("1.2.840.113549.1.9.4", "Message Digest");
                    m.put("1.2.840.113549.1.9.5", "Signing Time");
                    m.put("1.2.840.113549.1.9.6", "Counter Signature");
                    m.put("1.2.840.113549.1.9.14", "Extension Request");
                    m.put("1.2.840.113549.1.9.15", "S/MIME Capabilities");
                    m.put("1.2.840.113549.1.9.16.2.11", "Timestamp Token");
                    m.put("1.2.840.113549.1.9.22.1", "PKCS#9 Friendly Name");
                    m.put("1.2.840.113549.1.12.1.3", "PKCS#12 pbeWithSHAAnd3-KeyTripleDES-CBC");
                    m.put("1.2.840.113549.1.12.1.6", "PKCS#12 pbeWithSHAAnd40BitRC2-CBC");

                    // =========================================================================
                    // AES Encryption (for PKCS#12, CMS, etc.)
                    // =========================================================================
                    m.put("2.16.840.1.101.3.4.1.2", "AES-128-CBC");
                    m.put("2.16.840.1.101.3.4.1.6", "AES-128-GCM");
                    m.put("2.16.840.1.101.3.4.1.22", "AES-192-CBC");
                    m.put("2.16.840.1.101.3.4.1.26", "AES-192-GCM");
                    m.put("2.16.840.1.101.3.4.1.42", "AES-256-CBC");
                    m.put("2.16.840.1.101.3.4.1.46", "AES-256-GCM");

                    OID_MAP_internal = Collections.unmodifiableMap(m);

                }
            }
            finally {
                SecUtil.SEC_LOCK.unlock(true);
            }
        }

        return OID_MAP_internal.get(oid);
    }

    // =========================================================================
    // ASN.1 Tag Constants
    // =========================================================================

    /** BOOLEAN: A true/false value. Encoded as single byte: 0x00=false, non-zero=true */
    public static final int TAG_BOOLEAN = 0x01;

    /** INTEGER: Arbitrary precision signed integer in two's complement big-endian */
    public static final int TAG_INTEGER = 0x02;

    /**
     * BIT STRING: A sequence of bits with a leading byte indicating unused bits.
     * Format: [unused_bits_count] [bit_data...]
     * Used for: Key Usage flags, public keys, signatures
     */
    public static final int TAG_BIT_STRING = 0x03;

    /**
     * OCTET STRING: A sequence of arbitrary bytes (binary data).
     *
     * <p>An OCTET STRING is simply a container for raw binary data - an array of bytes
     * with no inherent meaning. The interpretation depends entirely on context:</p>
     *
     * <ul>
     *   <li><b>In extensions:</b> Wraps the actual extension value (another ASN.1 structure)</li>
     *   <li><b>In Subject Key Identifier:</b> Contains a hash (typically SHA-1) of the public key</li>
     *   <li><b>In encrypted data:</b> Contains ciphertext</li>
     *   <li><b>In PKCS structures:</b> May contain nested ASN.1 or raw data</li>
     * </ul>
     *
     * <p>Format: [tag=0x04] [length] [raw_bytes...]</p>
     */
    public static final int TAG_OCTET_STRING = 0x04;

    /** NULL: Represents absence of value. Always encoded as 05 00 */
    public static final int TAG_NULL = 0x05;

    /**
     * OBJECT IDENTIFIER (OID): A globally unique identifier as a sequence of integers.
     * First two arcs encoded as (first*40)+second, remaining use base-128 VLQ encoding.
     */
    public static final int TAG_OID = 0x06;

    /** UTF8String: Unicode text encoded as UTF-8 */
    public static final int TAG_UTF8_STRING = 0x0C;

    /** PrintableString: Restricted ASCII (A-Z, a-z, 0-9, space, '()+,-./:=?) */
    public static final int TAG_PRINTABLE_STRING = 0x13;

    /** T61String (TeletexString): Legacy 8-bit character encoding */
    public static final int TAG_T61_STRING = 0x14;

    /** IA5String: International Alphabet 5 (ASCII) - used for email, URI, DNS names */
    public static final int TAG_IA5_STRING = 0x16;

    /** UTCTime: Date/time as YYMMDDhhmmssZ (2-digit year, Z=UTC) */
    public static final int TAG_UTC_TIME = 0x17;

    /** GeneralizedTime: Date/time as YYYYMMDDhhmmss.fff (4-digit year, optional fractional seconds) */
    public static final int TAG_GENERALIZED_TIME = 0x18;

    /** BMPString: Unicode BMP (Basic Multilingual Plane) encoded as UTF-16BE */
    public static final int TAG_BMP_STRING = 0x1E;

    /** SEQUENCE: Ordered collection of elements (like a struct). Tag 0x30 = 0x10 | 0x20 (constructed) */
    public static final int TAG_SEQUENCE = 0x30;

    /** SET: Unordered collection of elements. Tag 0x31 = 0x11 | 0x20 (constructed) */
    public static final int TAG_SET = 0x31;

    // =========================================================================
    // Tag Modifiers
    // =========================================================================

    /** Context-specific tag mask - used for [0], [1], etc. in ASN.1 schemas */
    public static final int TAG_CONTEXT_SPECIFIC = 0x80;

    /** Constructed flag - indicates the value contains nested ASN.1 elements */
    public static final int TAG_CONSTRUCTED = 0x20;

    // =========================================================================
    // Inner Classes
    // =========================================================================

    /**
     * Parsing context that tracks position in a byte array.
     */
    public static class ParseContext {
        public final byte[] data;
        public int pos;

        public ParseContext(byte[] data, int pos) {
            this.data = data;
            this.pos = pos;
        }

        public int remaining() {
            return data.length - pos;
        }
    }

    /**
     * Tag and length pair from ASN.1 TLV structure.
     */
    public static class TagLength {
        public int tag;
        public int length;
        public boolean constructed;
    }

    private OIDCodecs() {
    }

    // =========================================================================
    // DataDecoder Instances
    // =========================================================================

    /**
     * Decodes ASN.1 BOOLEAN value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVBoolean
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> Boolean = (nv) -> {
        String name = nv.getName();
        ParseContext ctx = nv.getValue();
        boolean boolVal = ctx.data[ctx.pos++] != 0;
        return new NVBoolean(name, boolVal);
    };

    /**
     * Decodes ASN.1 INTEGER value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVLong for small integers, NVBigDecimal for large integers.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> Integer = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        if (length <= 8) {
            long value = 0;
            boolean negative = (ctx.data[ctx.pos] & 0x80) != 0;
            for (int i = 0; i < length; i++) {
                value = (value << 8) | (ctx.data[ctx.pos++] & 0xFF);
            }
            if (negative && length < 8) {
                value |= (-1L << (length * 8));
            }
            return new NVLong(name, value);
        } else {
            byte[] intBytes = new byte[length];
            System.arraycopy(ctx.data, ctx.pos, intBytes, 0, length);
            ctx.pos += length;
            BigInteger bigInt = new BigInteger(intBytes);
            return new NVBigDecimal(name, new java.math.BigDecimal(bigInt));
        }
    };

    /**
     * Decodes ASN.1 BIT STRING value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVGenericMap with binary representation and flags, or NVBlob for large bit strings.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> BitString = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        if (length < 1) {
            return new NVPair(name, "<empty bit string>");
        }

        int unusedBits = ctx.data[ctx.pos++] & 0xFF;
        int dataLen = length - 1;
        byte[] bits = new byte[dataLen];
        System.arraycopy(ctx.data, ctx.pos, bits, 0, dataLen);
        ctx.pos += dataLen;

        if (dataLen <= 2) {
            NVGenericMap result = new NVGenericMap(name);
            result.add(new NVPair("binary", toBinaryString(bits, unusedBits)));
            result.add(new NVInt("unusedBits", unusedBits));

            NVGenericMapList flags = new NVGenericMapList("flags");
            String[] keyUsageNames = {
                    "digitalSignature", "nonRepudiation", "keyEncipherment",
                    "dataEncipherment", "keyAgreement", "keyCertSign",
                    "cRLSign", "encipherOnly", "decipherOnly"
            };
            int totalBits = dataLen * 8 - unusedBits;
            for (int i = 0; i < Math.min(totalBits, keyUsageNames.length); i++) {
                int byteIdx = i / 8;
                int bitIdx = 7 - (i % 8);
                boolean set = (bits[byteIdx] & (1 << bitIdx)) != 0;
                if (set) {
                    NVGenericMap flag = new NVGenericMap();
                    flag.add(new NVPair("name", keyUsageNames[i]));
                    flags.add(flag);
                }
            }
            if (!flags.getValue().isEmpty()) {
                result.add(flags);
            }
            return result;
        }

        if (looksLikeASN1(bits)) {
            try {
                return decode(name, bits);
            } catch (Exception e) {
                // Fall back to blob
            }
        }

        return new NVBlob(name, bits);
    };

    /**
     * Decodes ASN.1 OCTET STRING value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: Nested decoded value if ASN.1, otherwise NVBlob.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> OctetString = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] octets = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, octets, 0, length);
        ctx.pos += length;

        if (length > 2 && looksLikeASN1(octets)) {
            try {
                return decode(name, octets);
            } catch (Exception e) {
                // Fall back to blob
            }
        }

        return new NVBlob(name, octets);
    };

    /**
     * Decodes ASN.1 NULL value.
     * Input: NamedValue with ParseContext.
     * Output: NVPair with "NULL" string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> Null = (nv) -> {
        return new NVPair(nv.getName(), "NULL");
    };


    /**
     * Decodes ASN.1 OBJECT IDENTIFIER value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with OID string and human-readable name if available.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> ObjectIdentifier = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] oidBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, oidBytes, 0, length);
        ctx.pos += length;

        String oidStr = decodeOIDBytes(oidBytes);
        String oidName = oidLookup(oidStr);
        if (oidName != null) {
            return new NVPair(name, oidName + " (" + oidStr + ")");
        }
        return new NVPair(name, oidStr);
    };

    /**
     * Decodes ASN.1 UTF8String value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with decoded string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> UTF8String = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] strBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, strBytes, 0, length);
        ctx.pos += length;

        try {
            return new NVPair(name, new String(strBytes, "UTF-8"));
        } catch (Exception e) {
            return new NVBlob(name, strBytes);
        }
    };

    /**
     * Decodes ASN.1 PrintableString/IA5String/T61String value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with decoded ASCII string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> ASCIIString = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] strBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, strBytes, 0, length);
        ctx.pos += length;

        return new NVPair(name, new String(strBytes, StandardCharsets.US_ASCII));
    };

    /**
     * Decodes ASN.1 BMPString value (UTF-16BE).
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with decoded string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> BMPString = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] strBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, strBytes, 0, length);
        ctx.pos += length;

        try {
            return new NVPair(name, new String(strBytes, "UTF-16BE"));
        } catch (Exception e) {
            return new NVBlob(name, strBytes);
        }
    };

    /**
     * Decodes ASN.1 UTCTime value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with parsed date string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> UTCTime = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] timeBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, timeBytes, 0, length);
        ctx.pos += length;

        String timeStr = new String(timeBytes, StandardCharsets.US_ASCII);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(timeStr);
            return new NVPair(name, date.toString());
        } catch (Exception e) {
            return new NVPair(name, timeStr);
        }
    };

    /**
     * Decodes ASN.1 GeneralizedTime value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVPair with parsed date string.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> GeneralizedTime = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();

        byte[] timeBytes = new byte[length];
        System.arraycopy(ctx.data, ctx.pos, timeBytes, 0, length);
        ctx.pos += length;

        String timeStr = new String(timeBytes, StandardCharsets.US_ASCII);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(timeStr);
            return new NVPair(name, date.toString());
        } catch (Exception e) {
            return new NVPair(name, timeStr);
        }
    };

    /**
     * Decodes ASN.1 SEQUENCE value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVGenericMap with indexed child elements.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> Sequence = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();
        int endPos = ctx.pos + length;

        NVGenericMap seq = new NVGenericMap(name);
        seq.add(new NVPair("type", "SEQUENCE"));
        int idx = 0;
        while (ctx.pos < endPos) {
            seq.add(decodeValue("" + idx++, ctx));
        }
        return seq;
    };

    /**
     * Decodes ASN.1 SET value.
     * Input: NamedValue with ParseContext, properties must contain "length".
     * Output: NVGenericMap with indexed child elements.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> Set = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        ParseContext ctx = nv.getValue();
        int endPos = ctx.pos + length;

        NVGenericMap set = new NVGenericMap(name);
        set.add(new NVPair("type", "SET"));
        int idx = 0;
        while (ctx.pos < endPos) {
            set.add(decodeValue("" + idx++, ctx));
        }
        return set;
    };

    /**
     * Decodes context-specific tagged value (e.g., [0], [1], [2]).
     * Input: NamedValue with ParseContext, properties must contain "length", "tag", "constructed".
     * Output: Interpreted value based on tag number.
     */
    public static final DataDecoder<NamedValue<ParseContext>, GetNameValue<?>> ContextSpecific = (nv) -> {
        String name = nv.getName();
        int length = nv.getProperties().getValue("length");
        int tag = nv.getProperties().getValue("tag");
        boolean constructed = nv.getProperties().getValue("constructed");
        ParseContext ctx = nv.getValue();
        int endPos = ctx.pos + length;

        int tagNum = tag & 0x1F;
        String tagName = name + "[" + tagNum + "]";

        if (constructed) {
            NVGenericMap nested = new NVGenericMap(tagName);
            int idx = 0;
            while (ctx.pos < endPos) {
                nested.add(decodeValue("item" + idx++, ctx));
            }
            return nested;
        } else {
            byte[] content = new byte[length];
            System.arraycopy(ctx.data, ctx.pos, content, 0, length);
            ctx.pos = endPos;
            return interpretContextSpecific(tagName, tagNum, content);
        }
    };

    // =========================================================================
    // Public Utility Methods
    // =========================================================================

    /**
     * Decode raw ASN.1 DER bytes into a human-readable structure.
     *
     * @param name name for the result
     * @param data ASN.1 DER encoded bytes
     * @return decoded structure
     */
    public static GetNameValue<?> decode(String name, byte[] data) {
        if (data == null || data.length == 0) {
            return new NVPair(name, "<empty>");
        }
        ParseContext ctx = new ParseContext(data, 0);
        return decodeValue(name, ctx);
    }

    /**
     * Decode a certificate extension by OID.
     *
     * @param cert the X.509 certificate
     * @param oid  the extension OID (e.g., "2.5.29.19")
     * @return decoded NVGenericMap or null if extension not found
     */
    public static NVGenericMap decodeExtension(X509Certificate cert, String oid) {
        byte[] extValue = cert.getExtensionValue(oid);
        if (extValue == null) {
            return null;
        }

        String name = oidLookup(oid);
        if (name == null) name = oid;

        NVGenericMap result = new NVGenericMap(name);
        result.add(new NVPair("oid", oid));
        result.add(new NVBoolean("critical", cert.getCriticalExtensionOIDs().contains(oid)));

        try {
            ParseContext ctx = new ParseContext(extValue, 0);
            TagLength tl = readTagLength(ctx);
            if (tl.tag == TAG_OCTET_STRING) {
                byte[] inner = new byte[tl.length];
                System.arraycopy(extValue, ctx.pos, inner, 0, tl.length);
                result.add(decode("value", inner));
            } else {
                result.add(decode("value", extValue));
            }
        } catch (Exception e) {
            result.add(new NVPair("error", e.getMessage()));
            result.add(new NVBlob("rawBytes", extValue));
        }

        return result;
    }

    /**
     * Decode all extensions from a certificate into human-readable format.
     *
     * @param cert the X.509 certificate
     * @return NVGenericMap containing all decoded extensions
     */
    public static NVGenericMap decodeAllExtensions(X509Certificate cert) {
        NVGenericMap result = new NVGenericMap("extensions");

        if (cert.getCriticalExtensionOIDs() != null) {
            for (String oid : cert.getCriticalExtensionOIDs()) {
                NVGenericMap ext = decodeExtension(cert, oid);
                if (ext != null) {
                    result.add(ext);
                }
            }
        }

        if (cert.getNonCriticalExtensionOIDs() != null) {
            for (String oid : cert.getNonCriticalExtensionOIDs()) {
                NVGenericMap ext = decodeExtension(cert, oid);
                if (ext != null) {
                    result.add(ext);
                }
            }
        }

        return result;
    }

    /**
     * Decode an OID byte array to dotted string format.
     *
     * @param data raw OID bytes (without tag and length)
     * @return dotted string like "2.5.29.19"
     */
    public static String decodeOIDBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int first = (data[0] & 0xFF) / 40;
        int second = (data[0] & 0xFF) % 40;
        sb.append(first).append(".").append(second);

        long value = 0;
        for (int i = 1; i < data.length; i++) {
            int b = data[i] & 0xFF;
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                sb.append(".").append(value);
                value = 0;
            }
        }

        return sb.toString();
    }

    /**
     * Encode a dotted OID string to bytes.
     *
     * @param oid dotted string like "2.5.29.19"
     * @return raw OID bytes (without tag and length)
     */
    public static byte[] encodeOIDBytes(String oid) {
        String[] parts = oid.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("OID must have at least 2 components");
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int first = java.lang.Integer.parseInt(parts[0]);
        int second = java.lang.Integer.parseInt(parts[1]);
        baos.write((first * 40) + second);

        for (int i = 2; i < parts.length; i++) {
            long value = Long.parseLong(parts[i]);
            encodeBase128(baos, value);
        }

        return baos.toByteArray();
    }

    /**
     * Read tag and length from ASN.1 TLV structure.
     *
     * @param ctx parsing context
     * @return TagLength with tag, length, and constructed flag
     */
    public static TagLength readTagLength(ParseContext ctx) {
        TagLength tl = new TagLength();

        if (ctx.remaining() < 2) {
            throw new IllegalArgumentException("Not enough bytes for tag/length");
        }

        tl.tag = ctx.data[ctx.pos++] & 0xFF;
        tl.constructed = (tl.tag & TAG_CONSTRUCTED) != 0;

        int lenByte = ctx.data[ctx.pos++] & 0xFF;
        if (lenByte < 0x80) {
            tl.length = lenByte;
        } else {
            int numLenBytes = lenByte & 0x7F;
            tl.length = 0;
            for (int i = 0; i < numLenBytes; i++) {
                tl.length = (tl.length << 8) | (ctx.data[ctx.pos++] & 0xFF);
            }
        }

        return tl;
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private static GetNameValue<?> decodeValue(String name, ParseContext ctx) {
        if (ctx.remaining() < 2) {
            return new NVPair(name, "<truncated>");
        }

        TagLength tl = readTagLength(ctx);

        // Create NamedValue with context and properties
        NamedValue<ParseContext> nv = new NamedValue<>(name, ctx);
        nv.getProperties().add(new NVInt("length", tl.length));
        nv.getProperties().add(new NVInt("tag", tl.tag));
        nv.getProperties().add(new NVBoolean("constructed", tl.constructed));

        // Check for context-specific tags
        if ((tl.tag & TAG_CONTEXT_SPECIFIC) != 0) {
            return ContextSpecific.decode(nv);
        }

        // Handle standard tags using DataDecoders
        switch (tl.tag) {
            case TAG_BOOLEAN:
                return Boolean.decode(nv);

            case TAG_INTEGER:
                return Integer.decode(nv);

            case TAG_BIT_STRING:
                return BitString.decode(nv);

            case TAG_OCTET_STRING:
                return OctetString.decode(nv);

            case TAG_NULL:
                return Null.decode(nv);

            case TAG_OID:
                return ObjectIdentifier.decode(nv);

            case TAG_UTF8_STRING:
                return UTF8String.decode(nv);

            case TAG_PRINTABLE_STRING:
            case TAG_IA5_STRING:
            case TAG_T61_STRING:
                return ASCIIString.decode(nv);

            case TAG_BMP_STRING:
                return BMPString.decode(nv);

            case TAG_UTC_TIME:
                return UTCTime.decode(nv);

            case TAG_GENERALIZED_TIME:
                return GeneralizedTime.decode(nv);

            case TAG_SEQUENCE:
                return Sequence.decode(nv);

            case TAG_SET:
                return Set.decode(nv);

            default:
                // Unknown tag - return as blob
                int endPos = ctx.pos + tl.length;
                byte[] unknown = new byte[tl.length];
                System.arraycopy(ctx.data, ctx.pos, unknown, 0, tl.length);
                ctx.pos = endPos;
                NVGenericMap unknownMap = new NVGenericMap(name);
                unknownMap.add(new NVPair("tag", String.format("0x%02X", tl.tag)));
                unknownMap.add(new NVBlob("data", unknown));
                return unknownMap;
        }
    }

    private static GetNameValue<?> interpretContextSpecific(String name, int tagNum, byte[] content) {
        switch (tagNum) {
            case 1: // rfc822Name (email)
            case 2: // dNSName
            case 6: // URI
                return new NVPair(name, new String(content, StandardCharsets.US_ASCII));

            case 7: // iPAddress
                if (content.length == 4) {
                    return new NVPair(name, String.format("%d.%d.%d.%d",
                            content[0] & 0xFF, content[1] & 0xFF,
                            content[2] & 0xFF, content[3] & 0xFF));
                } else if (content.length == 16) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 16; i += 2) {
                        if (i > 0) sb.append(":");
                        sb.append(String.format("%02x%02x", content[i] & 0xFF, content[i + 1] & 0xFF));
                    }
                    return new NVPair(name, sb.toString());
                }
                // Fall through

            default:
                return new NVBlob(name, content);
        }
    }

    private static void encodeBase128(java.io.ByteArrayOutputStream baos, long value) {
        if (value < 128) {
            baos.write((int) value);
        } else {
            int numBytes = 0;
            long temp = value;
            while (temp > 0) {
                numBytes++;
                temp >>= 7;
            }

            for (int i = numBytes - 1; i >= 0; i--) {
                int b = (int) ((value >> (i * 7)) & 0x7F);
                if (i > 0) b |= 0x80;
                baos.write(b);
            }
        }
    }

    private static boolean looksLikeASN1(byte[] data) {
        if (data == null || data.length < 2) return false;
        int tag = data[0] & 0xFF;
        return tag == TAG_SEQUENCE || tag == TAG_SET ||
                tag == TAG_INTEGER || tag == TAG_BIT_STRING ||
                tag == TAG_OCTET_STRING || tag == TAG_OID ||
                (tag & TAG_CONTEXT_SPECIFIC) != 0;
    }

    private static String toBinaryString(byte[] bytes, int unusedBits) {
        StringBuilder sb = new StringBuilder();
        int totalBits = bytes.length * 8 - unusedBits;
        for (int i = 0; i < totalBits; i++) {
            int byteIdx = i / 8;
            int bitIdx = 7 - (i % 8);
            sb.append((bytes[byteIdx] & (1 << bitIdx)) != 0 ? '1' : '0');
        }
        return sb.toString();
    }
}
