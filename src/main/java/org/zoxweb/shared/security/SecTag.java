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
package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * Represents a security tag that associates a provider with a specific security identifier.
 *
 * <p>SecTag is an immutable value object used to tag and categorize security-related
 * configurations, certificates, or credentials by provider and type. All string values
 * are automatically converted to uppercase for consistent key generation and lookup.</p>
 *
 * <h2>Key Concepts:</h2>
 * <ul>
 *   <li><b>Provider ID:</b> Identifies the security provider or source (e.g., "ACME", "LETSENCRYPT")</li>
 *   <li><b>Tag ID:</b> Identifies the type of security artifact (e.g., "TLS", "X509", "X500")</li>
 *   <li><b>Tag Value:</b> An optional value associated with the tag, defaults to the tag ID</li>
 * </ul>
 *
 * <h2>Key Generation:</h2>
 * <p>Keys are generated in the format {@code PROVIDER_ID:TAG_ID} using the colon separator.
 * Since all values are uppercased, keys are case-insensitive during registration and lookup.</p>
 *
 * <h2>Global Registry:</h2>
 * <p>SecTag provides a global {@link #REGISTRAR} for storing and retrieving SecTag instances.
 * Tags can be registered and later looked up by their generated key.</p>
 *
 * <h2>Usage Examples:</h2>
 *
 * <h3>Creating SecTag instances:</h3>
 * <pre>{@code
 * // Using TagID enum
 * SecTag tlsTag = new SecTag("MyProvider", SecTag.TagID.TLS);
 *
 * // Using string tag ID
 * SecTag customTag = new SecTag("MyProvider", "CUSTOM_TAG");
 *
 * // With custom tag value
 * SecTag certTag = new SecTag("LetsEncrypt", SecTag.TagID.X509, "production-cert");
 * }</pre>
 *
 * <h3>Registering and looking up tags:</h3>
 * <pre>{@code
 * // Register a tag
 * SecTag tag = new SecTag("ACME", SecTag.TagID.TLS);
 * SecTag.REGISTRAR.registerValue(tag);
 *
 * // Lookup by key
 * SecTag found = SecTag.REGISTRAR.lookup("ACME:TLS");
 *
 * // Lookup using static helper
 * SecTag found2 = SecTag.lookup("ACME", SecTag.TagID.TLS);
 * }</pre>
 *
 * <h3>Generating keys:</h3>
 * <pre>{@code
 * // From instance
 * String key = tag.key();  // "ACME:TLS"
 *
 * // Static key generation
 * String key2 = SecTag.key("acme", "tls");  // "ACME:TLS"
 * String key3 = SecTag.key("acme", SecTag.TagID.TLS);  // "ACME:TLS"
 * }</pre>
 *
 * @author mnael
 * @see RegistrarMapDefault
 * @see DataDecoder
 */
public class SecTag {

    public static final String SUN_JSSE = "SunJSSE";

    /**
     * Decoder that generates a lookup key from a SecTag instance.
     *
     * <p>The key format is {@code PROVIDER_ID:TAG_ID} using colon as separator.
     * This decoder is used by the {@link #REGISTRAR} for automatic key generation
     * when registering tags.</p>
     *
     * @see #key()
     * @see SUS#toCanonicalID(char, Object...)
     */
    public static final DataDecoder<SecTag, String> ToKey = (st) -> SUS.toCanonicalID(':', st.getProviderID(), st.getTagID());

    /**
     * Global registry for storing and retrieving SecTag instances.
     *
     * <p>The registrar uses {@link #ToKey} decoder for automatic key generation,
     * allowing tags to be registered using {@code registerValue(tag)} without
     * explicitly providing a key.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * // Register with automatic key generation
     * SecTag.REGISTRAR.registerValue(tag);
     *
     * // Register with explicit key
     * SecTag.REGISTRAR.register(tag.key(), tag);
     *
     * // Lookup
     * SecTag found = SecTag.REGISTRAR.lookup("PROVIDER:TAGID");
     * }</pre>
     *
     * @see RegistrarMapDefault
     */
    public static final RegistrarMapDefault<String, SecTag> REGISTRAR = new RegistrarMapDefault<String, SecTag>(null, ToKey);


    /**
     * Predefined tag identifiers for common security artifact types.
     *
     * <p>These constants provide type-safe identifiers for commonly used
     * security protocols and certificate standards.</p>
     */
    public enum TagID
            implements GetName {
        /**
         * Transport Layer Security protocol identifier.
         * <p>Used for tagging TLS-related configurations and certificates.</p>
         */
        TLS("TLS"),

        /**
         * X.509 certificate standard identifier.
         * <p>Used for tagging X.509 public key certificates.</p>
         */
        X509("X509"),

        /**
         * X.500 directory services standard identifier.
         * <p>Used for tagging X.500 distinguished names and directory entries.</p>
         */
        X500("X500");

        /** The string name of the tag identifier */
        private final String name;

        /**
         * Constructs a TagID with the specified name.
         *
         * @param name the string representation of this tag ID
         */
        TagID(String name) {
            this.name = name;
        }

        /**
         * Returns the string name of this tag identifier.
         *
         * @return the tag ID name
         */
        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * The provider identifier, stored in uppercase.
     * <p>Identifies the source or provider of the security artifact.</p>
     */
    private final String providerID;

    /**
     * The tag identifier, stored in uppercase.
     * <p>Identifies the type of security artifact (e.g., TLS, X509).</p>
     */
    private final String tagID;

    /**
     * The tag value, stored in uppercase.
     * <p>An optional value associated with the tag, defaults to the tag ID if not specified.</p>
     */
    private final String tagValue;

    /**
     * Creates a SecTag with the specified provider and tag ID.
     *
     * <p>The tag value defaults to the tag ID. All values are converted to uppercase.</p>
     *
     * @param providerID the provider identifier (e.g., "ACME", "LetsEncrypt")
     * @param tagID      the tag identifier (e.g., "TLS", "X509")
     * @throws NullPointerException if providerID or tagID is null
     */
    public SecTag(String providerID, String tagID) {
        this(providerID, tagID, tagID);
    }

    /**
     * Creates a SecTag with the specified provider and TagID enum.
     *
     * <p>The tag value defaults to the TagID name. All values are converted to uppercase.</p>
     *
     * @param providerID the provider identifier (e.g., "ACME", "LetsEncrypt")
     * @param tagID      the tag identifier enum
     * @throws NullPointerException if providerID or tagID is null
     */
    public SecTag(String providerID, TagID tagID) {
        this(providerID, tagID.getName(), tagID.getName());
    }

    /**
     * Creates a SecTag with the specified provider, TagID enum, and custom value.
     *
     * <p>All values are converted to uppercase.</p>
     *
     * @param providerID the provider identifier (e.g., "ACME", "LetsEncrypt")
     * @param tagID      the tag identifier enum
     * @param tagValue   the tag value
     * @throws NullPointerException if any parameter is null
     */
    public SecTag(String providerID, TagID tagID, String tagValue) {
        this(providerID, tagID.getName(), tagValue);
    }

    /**
     * Creates a SecTag with the specified provider, tag ID, and tag value.
     *
     * <p>This is the primary constructor. All values are converted to uppercase
     * for consistent key generation and lookup.</p>
     *
     * @param providerID the provider identifier (e.g., "ACME", "LetsEncrypt")
     * @param tagID      the tag identifier (e.g., "TLS", "X509")
     * @param tagValue   the tag value
     * @throws NullPointerException if any parameter is null
     */
    public SecTag(String providerID, String tagID, String tagValue) {
        SUS.checkIfNulls("providerID or tagID or tagValue null", providerID, tagID, tagValue);
        this.providerID = providerID;
        this.tagID = DataEncoder.StringUpper.encode(tagID);
        this.tagValue = DataEncoder.StringUpper.encode(tagValue);
    }

    /**
     * Returns the provider identifier.
     *
     * @return the provider ID in uppercase
     */
    public String getProviderID() {
        return providerID;
    }

    /**
     * Returns the tag identifier.
     *
     * @return the tag ID in uppercase
     */
    public String getTagID() {
        return tagID;
    }

    /**
     * Returns the tag value.
     *
     * @return the tag value in uppercase
     */
    public String getTagValue() {
        return tagValue;
    }

    /**
     * Generates the lookup key for this SecTag.
     *
     * <p>The key format is {@code PROVIDER_ID:TAG_ID}.</p>
     *
     * @return the key string for registry lookup
     * @see #key(String, String)
     * @see #ToKey
     */
    public String key() {
        return ToKey.decode(this);
    }

    /**
     * Generates a lookup key from provider ID and tag ID strings.
     *
     * <p>Both parameters are converted to uppercase before key generation.
     * The key format is {@code PROVIDER_ID:TAG_ID}.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      the tag identifier
     * @return the key string for registry lookup
     */
    public static String key(String providerID, String tagID) {
        return SUS.toCanonicalID(':', providerID, DataEncoder.StringUpper.encode(tagID));
    }

    /**
     * Generates a lookup key from provider ID and a GetName instance.
     *
     * <p>Both parameters are converted to uppercase before key generation.
     * The key format is {@code PROVIDER_ID:TAG_ID}.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      a GetName instance (typically a {@link TagID} enum)
     * @return the key string for registry lookup
     */
    public static String key(String providerID, GetName tagID) {
        return SUS.toCanonicalID(':', providerID, tagID.getName());
    }

    /**
     * Looks up a SecTag in the global registry by provider ID and TagID.
     *
     * <p>This is a convenience method that generates the key and performs
     * the lookup in a single call.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      the tag identifier enum
     * @return the registered SecTag, or null if not found
     * @see #REGISTRAR
     */
    public static SecTag lookup(String providerID, TagID tagID) {
        return REGISTRAR.get(key(providerID, tagID));
    }

    /**
     * Looks up a tag value in the global registry by provider ID and TagID.
     *
     * <p>This is a convenience method that generates the key and performs
     * the lookup in a single call.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      the tag identifier enum
     * @return the registered SecTag.tagValue, or null if not found
     * @see #REGISTRAR
     */
    public static String lookupTagValue(String providerID, TagID tagID) {
        SecTag tag = lookup(providerID != null ? providerID : SUN_JSSE, tagID);
        return tag != null ? tag.getTagValue() : null;
    }

    /**
     * Looks up a SecTag in the global registry by provider ID and TagID.
     *
     * <p>This is a convenience method that generates the key and performs
     * the lookup in a single call.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      the tag identifier enum
     * @return the registered SecTag, or null if not found
     * @see #REGISTRAR
     */
    public static SecTag lookup(String providerID, String tagID) {
        return REGISTRAR.get(key(providerID != null ? providerID : SUN_JSSE, tagID));
    }

    /**
     * Looks up a tag value in the global registry by provider ID and TagID.
     *
     * <p>This is a convenience method that generates the key and performs
     * the lookup in a single call.</p>
     *
     * @param providerID the provider identifier
     * @param tagID      the tag identifier enum
     * @return the registered SecTag.tagValue, or null if not found
     * @see #REGISTRAR
     */
    public static String lookupTagValue(String providerID, String tagID) {
        SecTag tag = lookup(providerID != null ? providerID : SUN_JSSE, tagID);
        return tag != null ? tag.getTagValue() : null;
    }

    /**
     * Returns a string representation of this SecTag.
     *
     * <p>The format is {@code PROVIDER_ID:TAG_ID:TAG_VALUE}.</p>
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return SUS.toCanonicalID(':', providerID, tagID, tagValue);
    }

}