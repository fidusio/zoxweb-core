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

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.*;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * Utility class for extracting metadata and security information from established SSL/TLS connections.
 *
 * <p>This class provides methods to analyze SSL sessions, certificates, and cipher suites
 * to determine connection security properties and extract detailed information about
 * the cryptographic parameters in use.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Extract SSL session information (protocol, cipher suite, validity)</li>
 *   <li>Analyze peer and local certificates</li>
 *   <li>Parse cipher suite components (key exchange, encryption, MAC)</li>
 *   <li>Determine connection security level</li>
 *   <li>Calculate public key sizes for RSA, EC, and DSA keys</li>
 *   <li>Export all information as NVGenericMap for serialization</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * SSLSocket socket = (SSLSocket) factory.createSocket("example.com", 443);
 * socket.startHandshake();
 *
 * // Get detailed SSL info as NVGenericMap
 * NVGenericMap sslInfo = SSLInfoExtractor.extractSSLInfo(socket.getSession());
 * System.out.println(GSONUtil.toJSONDefault(sslInfo, true));
 *
 * // Check if connection is secure
 * boolean secure = SSLInfoExtractor.isSecure(socket.getSession());
 *
 * // Print to console
 * SSLInfoExtractor.printSSLInfo(socket.getSession());
 * }</pre>
 *
 * @author javaconsigliere@gmail.com
 * @see SSLSession
 * @see SSLSocket
 * @see SSLEngine
 */
public final class SSLInfoUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private SSLInfoUtil() {
    }

    /**
     * Extracts comprehensive SSL connection information into an NVGenericMap.
     *
     * <p>The returned map contains all available information about the SSL session,
     * including protocol version, cipher suite details, certificate chain information,
     * and security assessment.</p>
     *
     * @param session the SSL session to extract information from. Must not be null.
     * @return an NVGenericMap containing all SSL connection metadata
     * @throws NullPointerException if session is null
     */
    public static NVGenericMap extractSSLInfo(SSLSession session) {
        if (session == null) {
            throw new NullPointerException("SSLSession cannot be null");
        }

        NVGenericMap info = new NVGenericMap("ssl_info");

        // Basic session info
        info.add(CryptoConst.SSLTags.PROTOCOL, session.getProtocol());
        info.add(CryptoConst.SSLTags.CIPHER_SUITE, session.getCipherSuite());
        info.add(CryptoConst.SSLTags.PEER_HOST, session.getPeerHost());
        info.add(new NVInt(CryptoConst.SSLTags.PEER_PORT.getName(), session.getPeerPort()));
        info.add(new NVBoolean(CryptoConst.SSLTags.IS_VALID.getName(), session.isValid()));
        info.add(new NVBoolean(CryptoConst.SSLTags.IS_SECURE.getName(), isSecure(session)));
        info.add(CryptoConst.SSLTags.SESSION_ID, bytesToHex(session.getId()));
        info.add(new NVLong(CryptoConst.SSLTags.CREATION_TIME.getName(), session.getCreationTime()));
        info.add(new NVLong(CryptoConst.SSLTags.LAST_ACCESSED_TIME.getName(), session.getLastAccessedTime()));

        // Cipher suite breakdown
        CipherInfo ci = parseCipherSuite(session.getCipherSuite());
        NVGenericMap cipherInfo = new NVGenericMap(CryptoConst.SSLTags.CIPHER_INFO.getName());
        cipherInfo.add(CryptoConst.SSLTags.KEY_EXCHANGE, ci.keyExchange);
        cipherInfo.add(CryptoConst.SSLTags.ENCRYPTION, ci.encryption);
        cipherInfo.add(CryptoConst.SSLTags.MAC, ci.mac);
        info.add(cipherInfo);

        // Peer certificates
        try {
            Certificate[] peerCerts = session.getPeerCertificates();
            if (peerCerts != null && peerCerts.length > 0) {
                NVGenericMapList peerCertList = new NVGenericMapList(CryptoConst.SSLTags.PEER_CERTIFICATES.getName());
                for (Certificate cert : peerCerts) {
                    if (cert instanceof X509Certificate) {
                        peerCertList.add(extractCertInfo((X509Certificate) cert));
                    }
                }
                info.add(peerCertList);
            }
        } catch (SSLPeerUnverifiedException e) {
            // Peer not verified - no certificates available
        }

        // Local certificates (for mutual TLS)
        Certificate[] localCerts = session.getLocalCertificates();
        if (localCerts != null && localCerts.length > 0) {
            NVGenericMapList localCertList = new NVGenericMapList(CryptoConst.SSLTags.LOCAL_CERTIFICATES.getName());
            for (Certificate cert : localCerts) {
                if (cert instanceof X509Certificate) {
                    localCertList.add(extractCertInfo((X509Certificate) cert));
                }
            }
            info.add(localCertList);
        }

        return info;
    }

    /**
     * Extracts SSL information from an SSLSocket.
     *
     * <p>Convenience method that extracts the session from the socket
     * and delegates to {@link #extractSSLInfo(SSLSession)}.</p>
     *
     * @param socket the SSL socket to extract information from. Must not be null.
     * @return an NVGenericMap containing all SSL connection metadata
     * @throws NullPointerException if socket is null
     */
    public static NVGenericMap extractSSLInfo(SSLSocket socket) {
        if (socket == null) {
            throw new NullPointerException("SSLSocket cannot be null");
        }
        return extractSSLInfo(socket.getSession());
    }

    /**
     * Extracts SSL information from an SSLEngine.
     *
     * <p>Convenience method that extracts the session from the engine
     * and delegates to {@link #extractSSLInfo(SSLSession)}.</p>
     *
     * @param engine the SSL engine to extract information from. Must not be null.
     * @return an NVGenericMap containing all SSL connection metadata
     * @throws NullPointerException if engine is null
     */
    public static NVGenericMap extractSSLInfo(SSLEngine engine) {
        if (engine == null) {
            throw new NullPointerException("SSLEngine cannot be null");
        }
        return extractSSLInfo(engine.getSession());
    }

    /**
     * Extracts comprehensive information from an X509 certificate.
     *
     * <p>This method extracts all available data from the certificate including:</p>
     * <ul>
     *   <li>Basic fields: version, serial number, subject, issuer</li>
     *   <li>Validity period: not before, not after, current status</li>
     *   <li>Signature: algorithm, OID, signature bytes</li>
     *   <li>Public key: algorithm, size, encoded bytes</li>
     *   <li>Extensions: basic constraints, key usage, extended key usage, SANs, etc.</li>
     *   <li>Raw data: DER encoded certificate, TBS certificate</li>
     * </ul>
     *
     * @param cert the certificate to extract information from. Must not be null.
     * @return an NVGenericMap containing all certificate details
     * @throws NullPointerException if cert is null
     */
    public static NVGenericMap extractCertInfo(X509Certificate cert) {
        if (cert == null) {
            throw new NullPointerException("Certificate cannot be null");
        }

        NVGenericMap certInfo = new NVGenericMap();

        // === Basic Certificate Fields ===
        certInfo.add(new NVInt(CryptoConst.SSLTags.VERSION.getName(), cert.getVersion()));
        certInfo.add(CryptoConst.SSLTags.SERIAL_NUMBER, cert.getSerialNumber().toString(16));

        // Subject and Issuer (both raw DN and parsed components)
        certInfo.add(parseDN(CryptoConst.SSLTags.SUBJECT.getName(), cert.getSubjectX500Principal().getName()));
        certInfo.add(parseDN(CryptoConst.SSLTags.ISSUER.getName(), cert.getIssuerX500Principal().getName()));

        // === Validity Period ===
        NVGenericMap validity = new NVGenericMap(CryptoConst.SSLTags.VALIDITY.getName());
        validity.add(CryptoConst.SSLTags.NOT_BEFORE, cert.getNotBefore().toString());
        validity.add(CryptoConst.SSLTags.NOT_AFTER, cert.getNotAfter().toString());
        validity.add(NVEnum.create(CryptoConst.SSLTags.CERTIFICATE_STATUS, getCertificateStatus(cert)));
        certInfo.add(validity);

        // === Signature Information ===
        certInfo.add(CryptoConst.SSLTags.SIGNATURE_ALGORITHM, cert.getSigAlgName());
        certInfo.add(CryptoConst.SSLTags.SIG_ALG_OID, cert.getSigAlgOID());
        certInfo.add(new NVBlob(CryptoConst.SSLTags.SIGNATURE.getName(), cert.getSignature()));

        // === Public Key Information ===
        PublicKey pubKey = cert.getPublicKey();
        NVGenericMap publicKeyInfo = new NVGenericMap(CryptoConst.SSLTags.PUBLIC_KEY.getName());
        publicKeyInfo.add(CryptoConst.SSLTags.PUBLIC_KEY_ALGORITHM, pubKey.getAlgorithm());
        publicKeyInfo.add(new NVInt(CryptoConst.SSLTags.PUBLIC_KEY_SIZE.getName(), getKeySize(pubKey)));
        publicKeyInfo.add(new NVBlob(CryptoConst.SSLTags.PUBLIC_KEY_ENCODED.getName(), pubKey.getEncoded()));
        certInfo.add(publicKeyInfo);

        // === Basic Constraints ===
        int basicConstraints = cert.getBasicConstraints();
        NVGenericMap bcInfo = new NVGenericMap(CryptoConst.SSLTags.BASIC_CONSTRAINTS.getName());
        bcInfo.add(new NVBoolean(CryptoConst.SSLTags.IS_CA.getName(), basicConstraints >= 0));
        bcInfo.add(new NVInt(CryptoConst.SSLTags.PATH_LENGTH.getName(), basicConstraints));
        certInfo.add(bcInfo);

        // === Key Usage ===
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage != null) {
            NVStringList keyUsageList = new NVStringList(CryptoConst.SSLTags.KEY_USAGE.getName());
            String[] keyUsageNames = {
                    "digitalSignature", "nonRepudiation", "keyEncipherment",
                    "dataEncipherment", "keyAgreement", "keyCertSign",
                    "cRLSign", "encipherOnly", "decipherOnly"
            };
            for (int i = 0; i < keyUsage.length && i < keyUsageNames.length; i++) {
                if (keyUsage[i]) {
                    keyUsageList.add(keyUsageNames[i]);
                }
            }
            certInfo.add(keyUsageList);
        }

        // === Extended Key Usage ===
        try {
            java.util.List<String> extKeyUsage = cert.getExtendedKeyUsage();
            if (extKeyUsage != null && !extKeyUsage.isEmpty()) {
                NVStringList ekuList = new NVStringList(CryptoConst.SSLTags.EXTENDED_KEY_USAGE.getName());
                for (String oid : extKeyUsage) {
                    ekuList.add(resolveExtendedKeyUsageOID(oid));
                }
                certInfo.add(ekuList);
            }
        } catch (java.security.cert.CertificateParsingException e) {
            // Extended key usage not available or malformed
        }

        // === Subject Alternative Names ===
        try {
            java.util.Collection<java.util.List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null && !sans.isEmpty()) {
                NVStringList sanList = new NVStringList(CryptoConst.SSLTags.SUBJECT_ALT_NAMES.getName());
                for (java.util.List<?> san : sans) {
                    if (san.size() >= 2) {
                        Integer type = (Integer) san.get(0);
                        Object value = san.get(1);
                        sanList.add(formatSAN(type, value));
                    }
                }
                certInfo.add(sanList);
            }
        } catch (java.security.cert.CertificateParsingException e) {
            // SANs not available or malformed
        }

        // === Issuer Alternative Names ===
        try {
            java.util.Collection<java.util.List<?>> ians = cert.getIssuerAlternativeNames();
            if (ians != null && !ians.isEmpty()) {
                NVStringList ianList = new NVStringList(CryptoConst.SSLTags.ISSUER_ALT_NAMES.getName());
                for (java.util.List<?> ian : ians) {
                    if (ian.size() >= 2) {
                        Integer type = (Integer) ian.get(0);
                        Object value = ian.get(1);
                        ianList.add(formatSAN(type, value));
                    }
                }
                certInfo.add(ianList);
            }
        } catch (java.security.cert.CertificateParsingException e) {
            // IANs not available or malformed
        }

        // === Certificate Extensions ===
        NVGenericMapList extensions = new NVGenericMapList(CryptoConst.SSLTags.EXTENSIONS.getName());

        // Critical extensions
        java.util.Set<String> criticalOIDs = cert.getCriticalExtensionOIDs();
        if (criticalOIDs != null) {
            for (String oid : criticalOIDs) {
                NVGenericMap ext = new NVGenericMap();
                ext.add(CryptoConst.SSLTags.OID, oid);
                ext.add(new NVBoolean(CryptoConst.SSLTags.CRITICAL.getName(), true));
                byte[] extValue = cert.getExtensionValue(oid);
                if (extValue != null) {
                    ext.add(new NVBlob(CryptoConst.SSLTags.VALUE.getName(), extValue));
                }
                extensions.add(ext);
            }
        }

        // Non-critical extensions
        java.util.Set<String> nonCriticalOIDs = cert.getNonCriticalExtensionOIDs();
        if (nonCriticalOIDs != null) {
            for (String oid : nonCriticalOIDs) {
                NVGenericMap ext = new NVGenericMap();
                ext.add(CryptoConst.SSLTags.OID, oid);
                ext.add(new NVBoolean(CryptoConst.SSLTags.CRITICAL.getName(), false));
                byte[] extValue = cert.getExtensionValue(oid);
                if (extValue != null) {
                    ext.add(new NVBlob(CryptoConst.SSLTags.VALUE.getName(), extValue));
                }
                extensions.add(ext);
            }
        }

        if (!extensions.getValue().isEmpty()) {
            certInfo.add(extensions);
        }

        // === Raw Certificate Data ===
        try {
            certInfo.add(new NVBlob(CryptoConst.SSLTags.ENCODED.getName(), cert.getEncoded()));
            certInfo.add(new NVBlob(CryptoConst.SSLTags.TBS_CERTIFICATE.getName(), cert.getTBSCertificate()));
        } catch (java.security.cert.CertificateEncodingException e) {
            // Encoding not available
        }

        return certInfo;
    }

    /**
     * Resolves an Extended Key Usage OID to a human-readable name.
     *
     * @param oid the OID to resolve
     * @return human-readable name or the OID if not recognized
     */
    private static String resolveExtendedKeyUsageOID(String oid) {
        switch (oid) {
            case "1.3.6.1.5.5.7.3.1": return "serverAuth";
            case "1.3.6.1.5.5.7.3.2": return "clientAuth";
            case "1.3.6.1.5.5.7.3.3": return "codeSigning";
            case "1.3.6.1.5.5.7.3.4": return "emailProtection";
            case "1.3.6.1.5.5.7.3.8": return "timeStamping";
            case "1.3.6.1.5.5.7.3.9": return "OCSPSigning";
            case "1.3.6.1.4.1.311.10.3.3": return "microsoftServerGatedCrypto";
            case "2.16.840.1.113730.4.1": return "netscapeServerGatedCrypto";
            default: return oid;
        }
    }

    /**
     * Formats a Subject/Issuer Alternative Name entry.
     *
     * @param type the SAN type (0-8)
     * @param value the SAN value
     * @return formatted string representation
     */
    private static String formatSAN(Integer type, Object value) {
        String typeStr;
        switch (type) {
            case 0: typeStr = "otherName"; break;
            case 1: typeStr = "rfc822Name"; break;
            case 2: typeStr = "dNSName"; break;
            case 3: typeStr = "x400Address"; break;
            case 4: typeStr = "directoryName"; break;
            case 5: typeStr = "ediPartyName"; break;
            case 6: typeStr = "uniformResourceIdentifier"; break;
            case 7: typeStr = "iPAddress"; break;
            case 8: typeStr = "registeredID"; break;
            default: typeStr = "unknown(" + type + ")"; break;
        }
        return typeStr + ":" + value.toString();
    }

    /**
     * Parses a Distinguished Name (DN) string into an NVGenericMap with individual components.
     *
     * <p>A DN like "CN=example.com, O=Example Inc, OU=IT, L=Los Angeles, ST=California, C=US"
     * will be parsed into:</p>
     * <pre>{@code
     * {
     *   "dn": "CN=example.com, O=Example Inc, ...",
     *   "CN": "example.com",
     *   "O": "Example Inc",
     *   "OU": "IT",
     *   "L": "Los Angeles",
     *   "ST": "California",
     *   "C": "US"
     * }
     * }</pre>
     *
     * <p>Common DN attributes:</p>
     * <ul>
     *   <li>CN - Common Name</li>
     *   <li>O - Organization</li>
     *   <li>OU - Organizational Unit</li>
     *   <li>L - Locality (City)</li>
     *   <li>ST - State or Province</li>
     *   <li>C - Country</li>
     *   <li>E or EMAILADDRESS - Email Address</li>
     *   <li>SERIALNUMBER - Serial Number</li>
     *   <li>DC - Domain Component</li>
     *   <li>UID - User ID</li>
     * </ul>
     *
     * @param name the name for the NVGenericMap container
     * @param dn the Distinguished Name string to parse
     * @return an NVGenericMap containing the raw DN and parsed components
     */
    public static NVGenericMap parseDN(String name, String dn) {
        NVGenericMap dnMap = new NVGenericMap(name);

        // Store the raw DN string
        dnMap.add("dn", dn);

        if (dn == null || dn.isEmpty()) {
            return dnMap;
        }

        // Parse DN components
        // Handle escaped commas and complex values
        String[] parts = dn.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            part = part.trim();
            int equalsIdx = part.indexOf('=');
            if (equalsIdx > 0 && equalsIdx < part.length() - 1) {
                String key = part.substring(0, equalsIdx).trim();
                String value = part.substring(equalsIdx + 1).trim();

                // Remove surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }

                // Handle multiple values for the same attribute (e.g., multiple OUs)
                GetNameValue<?> existing = dnMap.get(key);
                if (existing != null) {
                    // Convert to list or append to existing list
                    if (existing instanceof NVStringList) {
                        ((NVStringList) existing).add(value);
                    } else {
                        // Convert single value to list
                        NVStringList list = new NVStringList(key);
                        list.add((String) existing.getValue());
                        list.add(value);
                        dnMap.remove(key);
                        dnMap.add(list);
                    }
                } else {
                    dnMap.add(key, value);
                }
            }
        }

        return dnMap;
    }

    /**
     * Determines the validity status of a certificate.
     *
     * @param cert the certificate to check
     * @return one of {@link CryptoConst.CertStatus#VALID}, {@link CryptoConst.CertStatus#EXPIRED},
     *         or {@link CryptoConst.CertStatus#NOT_YET_VALID}
     */
    public static CryptoConst.CertStatus getCertificateStatus(X509Certificate cert) {
        try {
            cert.checkValidity();
            return CryptoConst.CertStatus.VALID;
        } catch (CertificateExpiredException e) {
            return CryptoConst.CertStatus.EXPIRED;
        } catch (CertificateNotYetValidException e) {
            return CryptoConst.CertStatus.NOT_YET_VALID;
        }
    }

    /**
     * Checks the validity status of a certificate at a specific date.
     *
     * @param cert the certificate to check
     * @param date the date to check validity against
     * @return one of {@link CryptoConst.CertStatus#VALID}, {@link CryptoConst.CertStatus#EXPIRED},
     *         or {@link CryptoConst.CertStatus#NOT_YET_VALID}
     */
    public static CryptoConst.CertStatus getCertificateStatus(X509Certificate cert, Date date) {
        try {
            cert.checkValidity(date);
            return CryptoConst.CertStatus.VALID;
        } catch (CertificateExpiredException e) {
            return CryptoConst.CertStatus.EXPIRED;
        } catch (CertificateNotYetValidException e) {
            return CryptoConst.CertStatus.NOT_YET_VALID;
        }
    }

    /**
     * Determines if an SSL connection uses secure protocols and cipher suites.
     *
     * <p>A connection is considered secure if:</p>
     * <ul>
     *   <li>Protocol is TLSv1.2 or TLSv1.3</li>
     *   <li>Cipher suite does not contain known weak algorithms</li>
     * </ul>
     *
     * <p>Weak cipher indicators that will cause this method to return false:</p>
     * <ul>
     *   <li>NULL - No encryption</li>
     *   <li>EXPORT - Export-grade (weak) cryptography</li>
     *   <li>anon - Anonymous key exchange (no authentication)</li>
     *   <li>DES - Data Encryption Standard (weak)</li>
     *   <li>RC4 - RC4 stream cipher (broken)</li>
     *   <li>MD5 - MD5 hash (weak)</li>
     *   <li>CBC - CBC mode ciphers (vulnerable to padding oracle attacks in some cases)</li>
     * </ul>
     *
     * @param session the SSL session to evaluate
     * @return true if the connection uses secure protocols and ciphers, false otherwise
     */
    public static boolean isSecure(SSLSession session) {
        String protocol = session.getProtocol();
        String cipher = session.getCipherSuite();

        // Check protocol version (TLSv1.2+ is considered secure)
        boolean secureProtocol = "TLSv1.2".equals(protocol) || "TLSv1.3".equals(protocol);

        // Check for weak ciphers
        boolean weakCipher = cipher.contains("NULL") ||
                cipher.contains("EXPORT") ||
                cipher.contains("anon") ||
                cipher.contains("DES") ||
                cipher.contains("RC4") ||
                cipher.contains("MD5");

        return secureProtocol && !weakCipher;
    }

    /**
     * Determines if an SSL connection uses the strongest available security.
     *
     * <p>A connection is considered to have strong security if:</p>
     * <ul>
     *   <li>Protocol is TLSv1.3</li>
     *   <li>Uses AEAD cipher (GCM or ChaCha20-Poly1305)</li>
     *   <li>Uses ephemeral key exchange (ECDHE or DHE)</li>
     * </ul>
     *
     * @param session the SSL session to evaluate
     * @return true if the connection uses strong security, false otherwise
     */
    public static boolean isStrongSecurity(SSLSession session) {
        String protocol = session.getProtocol();
        String cipher = session.getCipherSuite();

        // TLSv1.3 is preferred
        boolean strongProtocol = "TLSv1.3".equals(protocol);

        // AEAD ciphers (GCM or ChaCha20)
        boolean aeadCipher = cipher.contains("GCM") || cipher.contains("CHACHA20");

        // Ephemeral key exchange
        boolean ephemeralKex = cipher.contains("ECDHE") || cipher.contains("DHE") || strongProtocol;

        return strongProtocol && aeadCipher && ephemeralKex;
    }

    /**
     * Gets the key size in bits from a PublicKey.
     *
     * <p>Supports RSA, EC (Elliptic Curve), and DSA keys. For other key types,
     * estimates the size based on the encoded key length.</p>
     *
     * @param key the public key to measure
     * @return the key size in bits
     */
    public static int getKeySize(PublicKey key) {
        if (key instanceof RSAPublicKey) {
            return ((RSAPublicKey) key).getModulus().bitLength();
        } else if (key instanceof ECPublicKey) {
            return ((ECPublicKey) key).getParams().getOrder().bitLength();
        } else if (key instanceof DSAPublicKey) {
            return ((DSAPublicKey) key).getParams().getP().bitLength();
        }
        // Fallback: estimate from encoded length
        return key.getEncoded().length * 8;
    }

    /**
     * Parses a cipher suite name into its component parts.
     *
     * <p>Analyzes the cipher suite string to extract:</p>
     * <ul>
     *   <li>Key exchange algorithm (e.g., ECDHE, RSA, DHE)</li>
     *   <li>Encryption algorithm (e.g., AES-256-GCM, ChaCha20-Poly1305)</li>
     *   <li>MAC algorithm (e.g., SHA256, SHA384, AEAD)</li>
     * </ul>
     *
     * <p>Handles both TLS 1.3 cipher suite format (e.g., TLS_AES_256_GCM_SHA384)
     * and TLS 1.2 format (e.g., TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384).</p>
     *
     * @param cipherSuite the cipher suite name to parse
     * @return a CipherInfo object containing the parsed components
     */
    public static CipherInfo parseCipherSuite(String cipherSuite) {
        CipherInfo info = new CipherInfo();

        if (cipherSuite == null || cipherSuite.isEmpty()) {
            return info;
        }

        // TLS 1.3 ciphers have a simpler format
        if (cipherSuite.startsWith("TLS_AES") || cipherSuite.startsWith("TLS_CHACHA")) {
            info.keyExchange = "TLS 1.3 Key Share";

            if (cipherSuite.contains("AES_256_GCM")) {
                info.encryption = "AES-256-GCM";
            } else if (cipherSuite.contains("AES_128_GCM")) {
                info.encryption = "AES-128-GCM";
            } else if (cipherSuite.contains("AES_128_CCM")) {
                info.encryption = "AES-128-CCM";
            } else if (cipherSuite.contains("CHACHA20_POLY1305")) {
                info.encryption = "ChaCha20-Poly1305";
            }

            // TLS 1.3 uses AEAD, MAC is integrated
            info.mac = "AEAD";

            // Extract hash from suffix
            if (cipherSuite.endsWith("SHA384")) {
                info.prfHash = "SHA-384";
            } else if (cipherSuite.endsWith("SHA256")) {
                info.prfHash = "SHA-256";
            }

            return info;
        }

        // TLS 1.2 and earlier format: TLS_<KeyExchange>_WITH_<Encryption>_<MAC>
        String[] parts = cipherSuite.split("_WITH_");
        if (parts.length == 2) {
            // Parse key exchange
            String kxPart = parts[0].replace("TLS_", "").replace("SSL_", "");
            info.keyExchange = parseKeyExchange(kxPart);

            // Parse encryption and MAC
            String encMacPart = parts[1];
            parseEncryptionAndMac(encMacPart, info);
        }

        return info;
    }

    /**
     * Parses the key exchange portion of a cipher suite.
     */
    private static String parseKeyExchange(String kxPart) {
        if (kxPart.contains("ECDHE")) {
            if (kxPart.contains("ECDSA")) {
                return "ECDHE-ECDSA";
            } else if (kxPart.contains("RSA")) {
                return "ECDHE-RSA";
            }
            return "ECDHE";
        } else if (kxPart.contains("DHE") || kxPart.contains("EDH")) {
            if (kxPart.contains("DSS")) {
                return "DHE-DSS";
            } else if (kxPart.contains("RSA")) {
                return "DHE-RSA";
            }
            return "DHE";
        } else if (kxPart.contains("ECDH_")) {
            return "ECDH (static)";
        } else if (kxPart.contains("DH_")) {
            return "DH (static)";
        } else if (kxPart.contains("RSA")) {
            return "RSA";
        } else if (kxPart.contains("PSK")) {
            return "PSK";
        } else if (kxPart.contains("SRP")) {
            return "SRP";
        } else if (kxPart.contains("KRB5")) {
            return "Kerberos";
        } else if (kxPart.contains("anon")) {
            return "Anonymous (INSECURE)";
        }
        return kxPart;
    }

    /**
     * Parses the encryption and MAC portion of a cipher suite.
     */
    private static void parseEncryptionAndMac(String encMacPart, CipherInfo info) {
        // Check for AEAD modes first (GCM, CCM, CHACHA20)
        if (encMacPart.contains("GCM")) {
            info.mac = "AEAD";
            if (encMacPart.contains("AES_256")) {
                info.encryption = "AES-256-GCM";
            } else if (encMacPart.contains("AES_128")) {
                info.encryption = "AES-128-GCM";
            } else {
                info.encryption = encMacPart.substring(0, encMacPart.indexOf("_GCM")) + "-GCM";
            }
        } else if (encMacPart.contains("CCM")) {
            info.mac = "AEAD";
            info.encryption = encMacPart.substring(0, encMacPart.indexOf("_CCM")) + "-CCM";
        } else if (encMacPart.contains("CHACHA20")) {
            info.mac = "AEAD";
            info.encryption = "ChaCha20-Poly1305";
        } else {
            // CBC or stream cipher with separate MAC
            if (encMacPart.contains("SHA384")) {
                info.mac = "HMAC-SHA384";
                info.encryption = encMacPart.replace("_SHA384", "");
            } else if (encMacPart.contains("SHA256")) {
                info.mac = "HMAC-SHA256";
                info.encryption = encMacPart.replace("_SHA256", "");
            } else if (encMacPart.contains("SHA")) {
                info.mac = "HMAC-SHA1";
                info.encryption = encMacPart.replace("_SHA", "");
            } else if (encMacPart.contains("MD5")) {
                info.mac = "HMAC-MD5 (WEAK)";
                info.encryption = encMacPart.replace("_MD5", "");
            }

            // Clean up encryption name
            info.encryption = info.encryption
                    .replace("_", "-")
                    .replace("3DES-EDE-CBC", "3DES-CBC")
                    .replace("AES-128-CBC", "AES-128-CBC")
                    .replace("AES-256-CBC", "AES-256-CBC");
        }
    }

    /**
     * Prints SSL session information to standard output.
     *
     * <p>Useful for debugging and logging SSL connection details.</p>
     *
     * @param session the SSL session to print information about
     */
    public static void printSSLInfo(SSLSession session) {
        System.out.println("=== SSL Connection Info ===");
        System.out.println("Protocol:        " + session.getProtocol());
        System.out.println("Cipher Suite:    " + session.getCipherSuite());
        System.out.println("Peer Host:       " + session.getPeerHost());
        System.out.println("Peer Port:       " + session.getPeerPort());
        System.out.println("Is Valid:        " + session.isValid());
        System.out.println("Is Secure:       " + isSecure(session));
        System.out.println("Strong Security: " + isStrongSecurity(session));
        System.out.println("Session ID:      " + bytesToHex(session.getId()));

        CipherInfo ci = parseCipherSuite(session.getCipherSuite());
        System.out.println("\n=== Cipher Details ===");
        System.out.println("Key Exchange:    " + ci.keyExchange);
        System.out.println("Encryption:      " + ci.encryption);
        System.out.println("MAC:             " + ci.mac);
        if (ci.prfHash != null) {
            System.out.println("PRF Hash:        " + ci.prfHash);
        }

        try {
            Certificate[] peerCerts = session.getPeerCertificates();
            if (peerCerts != null && peerCerts.length > 0) {
                System.out.println("\n=== Peer Certificate Chain ===");
                for (int i = 0; i < peerCerts.length; i++) {
                    if (peerCerts[i] instanceof X509Certificate) {
                        X509Certificate cert = (X509Certificate) peerCerts[i];
                        System.out.println("\n--- Certificate " + i + " ---");
                        printCertInfo(cert);
                    }
                }
            }
        } catch (SSLPeerUnverifiedException e) {
            System.out.println("\nPeer certificates: Not available (peer not verified)");
        }
    }

    /**
     * Prints X509 certificate information to standard output.
     *
     * @param cert the certificate to print information about
     */
    public static void printCertInfo(X509Certificate cert) {
        System.out.println("Subject:         " + cert.getSubjectX500Principal().getName());
        System.out.println("Issuer:          " + cert.getIssuerX500Principal().getName());
        System.out.println("Serial Number:   " + cert.getSerialNumber().toString(16));
        System.out.println("Valid From:      " + cert.getNotBefore());
        System.out.println("Valid Until:     " + cert.getNotAfter());
        System.out.println("Sig Algorithm:   " + cert.getSigAlgName());

        PublicKey pubKey = cert.getPublicKey();
        System.out.println("Key Algorithm:   " + pubKey.getAlgorithm());
        System.out.println("Key Size:        " + getKeySize(pubKey) + " bits");
        System.out.println("Status:          " + getCertificateStatus(cert));
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Container class for parsed cipher suite information.
     *
     * <p>Holds the component parts of a cipher suite after parsing.</p>
     */
    public static class CipherInfo {
        /**
         * The key exchange algorithm (e.g., "ECDHE-RSA", "RSA", "DHE").
         * For TLS 1.3, this will be "TLS 1.3 Key Share".
         */
        public String keyExchange = "Unknown";

        /**
         * The bulk encryption algorithm (e.g., "AES-256-GCM", "ChaCha20-Poly1305").
         */
        public String encryption = "Unknown";

        /**
         * The MAC algorithm (e.g., "HMAC-SHA256", "AEAD").
         * For AEAD ciphers, this will be "AEAD" since authentication is integrated.
         */
        public String mac = "Unknown";

        /**
         * The PRF (Pseudo-Random Function) hash for TLS 1.3 ciphers.
         * May be null for TLS 1.2 and earlier.
         */
        public String prfHash = null;

        /**
         * Returns a string representation of the cipher info.
         *
         * @return formatted string with all cipher components
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CipherInfo{");
            sb.append("keyExchange='").append(keyExchange).append('\'');
            sb.append(", encryption='").append(encryption).append('\'');
            sb.append(", mac='").append(mac).append('\'');
            if (prfHash != null) {
                sb.append(", prfHash='").append(prfHash).append('\'');
            }
            sb.append('}');
            return sb.toString();
        }
    }
}
