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
package org.zoxweb.server.net.ssl;

import org.zoxweb.server.security.SSLGroupSetterInt;
import org.zoxweb.server.security.SecUtil;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.InstanceFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.security.*;

/**
 * SSLContextInfo is a factory class that encapsulates SSL/TLS configuration and provides
 * a convenient way to create pre-configured {@link SSLEngine} instances for both client
 * and server SSL/TLS connections.
 *
 * <p>This class implements {@link InstanceFactory.Creator} to support the factory pattern,
 * allowing it to be used in contexts where SSLEngine instances need to be created on demand,
 * such as NIO-based server implementations.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Supports both server-side and client-side SSL/TLS configurations</li>
 *   <li>Allows custom protocol versions (e.g., TLSv1.2, TLSv1.3)</li>
 *   <li>Allows custom cipher suites</li>
 *   <li>Supports custom named groups (elliptic curves) via {@link SSLGroupSetterInt}</li>
 *   <li>Supports trust-all mode for development/testing scenarios</li>
 *   <li>Thread-safe SSLEngine creation</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 *
 * <h3>Server-side with KeyStore:</h3>
 * <pre>{@code
 * // Load keystore containing server certificate and private key
 * KeyStore ks = KeyStore.getInstance("PKCS12");
 * ks.load(new FileInputStream("server.p12"), "password".toCharArray());
 *
 * // Create SSLContextInfo with TLSv1.3 only
 * SSLContextInfo serverInfo = new SSLContextInfo(
 *     ks,
 *     "password".toCharArray(),
 *     new String[]{"TLSv1.3"},
 *     null  // use default ciphers
 * );
 *
 * // Create SSLEngine for incoming connection
 * SSLEngine engine = serverInfo.newInstance();
 * engine.setUseClientMode(false);  // server mode
 * }</pre>
 *
 * <h3>Client-side with default trust store:</h3>
 * <pre>{@code
 * // Create client SSLContextInfo using JVM's default CA certificates
 * SSLContextInfo clientInfo = new SSLContextInfo(
 *     new InetSocketAddress("example.com", 443),
 *     false  // use standard certificate validation
 * );
 *
 * // Create SSLEngine for outgoing connection
 * SSLEngine engine = clientInfo.newInstance();
 * engine.setUseClientMode(true);  // client mode
 * }</pre>
 *
 * <h3>Client-side trust-all (development only):</h3>
 * <pre>{@code
 * // WARNING: Only use for development/testing - disables certificate validation
 * SSLContextInfo devClientInfo = new SSLContextInfo(
 *     new InetSocketAddress("localhost", 8443),
 *     true  // accept all certificates
 * );
 *
 * SSLEngine engine = devClientInfo.newInstance();
 * engine.setUseClientMode(true);
 * }</pre>
 *
 * @author mnael
 * @see SSLContext
 * @see SSLEngine
 * @see InstanceFactory.Creator
 */
public class SSLContextInfo
        implements InstanceFactory.Creator<SSLEngine> {

    /**
     * Configuration parameter names used for external configuration of SSL/TLS settings.
     *
     * <p>These parameters can be used when loading SSL configuration from external sources
     * such as configuration files or NVGenericMap instances.</p>
     */
    public enum Param
            implements GetName {
        /**
         * Parameter name for specifying enabled SSL/TLS protocols.
         * <p>Example values: "TLSv1.2", "TLSv1.3"</p>
         */
        PROTOCOLS("protocols"),

        /**
         * Parameter name for specifying enabled cipher suites.
         * <p>Example values: "TLS_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"</p>
         */
        CIPHERS("ciphers"),

        /**
         * Parameter name for specifying named groups (elliptic curves) for key exchange.
         * <p>Named groups define the elliptic curves or finite field groups used during
         * the TLS handshake for key agreement algorithms like ECDHE.</p>
         * <p>Example values: "x25519", "secp256r1", "secp384r1"</p>
         */
        GROUPS("groups"),

        ;

        /** The string name of the parameter */
        private final String name;

        /**
         * Constructs a Param enum constant with the specified name.
         *
         * @param name the string name of the parameter
         */
        Param(String name) {
            this.name = name;
        }

        /**
         * Returns the string name of this parameter.
         *
         * @return the parameter name
         */
        @Override
        public String getName() {
            return name;
        }
    }

    /** The underlying SSLContext used to create SSLEngine instances */
    private final SSLContext sslContext;

    /**
     * Array of enabled SSL/TLS protocol versions.
     * <p>If null or empty, the SSLEngine will use its default protocols.</p>
     * <p>Common values: "TLSv1.2", "TLSv1.3"</p>
     */
    private final String[] protocols;

    /**
     * Array of enabled cipher suites.
     * <p>If null or empty, the SSLEngine will use its default cipher suites.</p>
     */
    private final String[] ciphers;


    /**
     * Optional SSLGroupSetter for configuring named groups (elliptic curves) on SSLEngine instances.
     * <p>When set, the {@link #newInstance()} method will apply the configured named groups
     * to each created SSLEngine via the {@link SSLGroupSetterInt#setGroups(SSLEngine)} method.</p>
     * <p>Named groups control which elliptic curves or finite field groups are used during
     * TLS key exchange (e.g., x25519, secp256r1).</p>
     *
     * @see SSLGroupSetterInt
     * @see #setSSLGroupSetter(SSLGroupSetterInt)
     * @see #getSSLGroupSetter()
     */
    private SSLGroupSetterInt sslGroupSetter = null;

    /**
     * The remote server address for client-mode connections.
     * <p>When non-null, indicates this is a client-side configuration.
     * The hostname and port are used to enable SNI (Server Name Indication)
     * and proper session caching.</p>
     */
    private final InetSocketAddress clientAddress;


    /**
     * Creates a server-side SSLContextInfo from a KeyStore.
     *
     * <p>This constructor initializes an SSL context suitable for server-side TLS connections.
     * The KeyStore should contain the server's certificate chain and private key.</p>
     *
     * <p>The SSL context is initialized using TLS protocol with the provided KeyStore
     * for both key material and trust anchors.</p>
     *
     * @param ks               the KeyStore containing the server's certificate and private key.
     *                         Must not be null.
     * @param providerName     the JSSE provider, null default
     * @param keyStorePassword the password to access the KeyStore and the private key.
     *                         The same password is used for both.
     * @param protocols        array of SSL/TLS protocol versions to enable (e.g., {"TLSv1.2", "TLSv1.3"}).
     *                         If null or empty, default protocols are used.
     * @param ciphers          array of cipher suites to enable.
     *                         If null or empty, default cipher suites are used.
     * @throws GeneralSecurityException if there is an error initializing the SSL context,
     *                                  such as invalid KeyStore or password
     * @see SecUtil#initSSLContext(String, String, KeyStore, char[], char[], KeyStore)
     */
    public SSLContextInfo(KeyStore ks, String providerName, char[] keyStorePassword, String[] protocols, String[] ciphers)
            throws GeneralSecurityException {
        this(SecUtil.initSSLContext("TLS", providerName, ks, keyStorePassword, keyStorePassword, null),
                protocols,
                ciphers);
    }

    /**
     * Creates a server-side SSLContextInfo with an existing SSLContext using default settings.
     *
     * <p>This is a convenience constructor that uses default protocols and cipher suites.
     * Equivalent to calling {@code new SSLContextInfo(sslContext, null, null)}.</p>
     *
     * @param sslContext the pre-configured SSLContext to use. Must not be null.
     */
    public SSLContextInfo(SSLContext sslContext) {
        this(sslContext, null, null);
    }

    /**
     * Creates a server-side SSLContextInfo with an existing SSLContext and custom settings.
     *
     * <p>This constructor provides full control over the SSL configuration for server-side
     * connections. The provided SSLContext should already be initialized with appropriate
     * KeyManagers and TrustManagers.</p>
     *
     * @param sslContext the pre-configured SSLContext to use. Must not be null.
     * @param protocols  array of SSL/TLS protocol versions to enable on created SSLEngines.
     *                   Common values include "TLSv1.2" and "TLSv1.3".
     *                   If null or empty, the SSLEngine's default protocols are used.
     * @param ciphers    array of cipher suite names to enable on created SSLEngines.
     *                   If null or empty, the SSLEngine's default cipher suites are used.
     */
    public SSLContextInfo(SSLContext sslContext, String[] protocols, String[] ciphers) {
        this.sslContext = sslContext;
        this.protocols = protocols;
        this.ciphers = ciphers;
        // null clientAddress indicates server mode
        this.clientAddress = null;
    }


    /**
     * Creates a client-side SSLContextInfo using an IPAddress.
     *
     * <p>This constructor configures SSL for client connections to the specified server.
     * The server address is used to enable SNI (Server Name Indication) which is required
     * by many modern servers hosting multiple domains.</p>
     *
     * @param clientAddress the server address to connect to, containing hostname and port.
     *                      Must not be null.
     * @param certValidationEnabled     if {@code false}, disables certificate validation (trust all certificates).
     *                      <b>WARNING:</b> Only false {@code false} for development/testing.
     *                      Using {@code false} in production makes the connection vulnerable
     *                      to man-in-the-middle attacks.
     *                      If {@code true}, uses the JVM's default trust store (cacerts)
     *                      for certificate validation.
     * @throws NoSuchAlgorithmException if the TLS algorithm is not available
     * @throws KeyManagementException   if there is an error initializing the SSL context
     * @see IPAddress
     */
    public SSLContextInfo(IPAddress clientAddress, boolean certValidationEnabled)
            throws NoSuchAlgorithmException, KeyManagementException {
        this(new InetSocketAddress(clientAddress.getInetAddress(), clientAddress.getPort()), certValidationEnabled);
    }

    /**
     * Creates a client-side SSLContextInfo using an InetSocketAddress.
     *
     * <p>This constructor configures SSL for client connections to the specified server.
     * The server address is stored and used when creating SSLEngine instances to enable
     * SNI (Server Name Indication) and proper SSL session caching.</p>
     *
     * <b>Certificate Validation Modes:</b>
     * <ul>
     *   <li><b>certValidationEnabled = true (recommended for production):</b> Uses the JVM's default
     *       trust store (typically $JAVA_HOME/lib/security/cacerts) which contains
     *       certificates from well-known Certificate Authorities.</li>
     *   <li><b>certValidationEnabled = false (development only):</b> Disables all certificate validation.
     *       The connection will trust any certificate, including self-signed and expired ones.
     *       This is useful for development but creates serious security vulnerabilities
     *       in production environments.</li>
     * </ul>
     *
     * @param clientAddress the client address to connect to, containing hostname and port.
     *                      The hostname is used for SNI. Must not be null.
     * @param certValidationEnabled     if {@code false}, disables certificate validation.
     *                      <b>WARNING:</b> Never use {@code false} in production.
     *                      If {@code true}, uses standard certificate validation.
     * @throws NoSuchAlgorithmException if the TLS algorithm is not available in the JVM
     * @throws KeyManagementException   if there is an error initializing the SSL context
     * @see SSLCheckDisabler
     */
    public SSLContextInfo(InetSocketAddress clientAddress, boolean certValidationEnabled)
            throws NoSuchAlgorithmException, KeyManagementException {
        if (certValidationEnabled) {
            // Production mode: use JVM's default trust store
            this.sslContext = SSLContext.getDefault();
        } else {
            // Development mode: trust all certificates (INSECURE)
            this.sslContext = SSLContext.getInstance("TLS");
            this.sslContext.init(null, SSLCheckDisabler.SINGLETON.getTrustManagers(), new SecureRandom());
        }
        // Client mode uses default protocols and ciphers
        this.protocols = null;
        this.ciphers = null;
        // Store address for SNI support
        this.clientAddress = clientAddress;
    }


    /**
     * Returns the SSLGroupSetter configured for this SSLContextInfo.
     *
     * <p>The SSLGroupSetter is used to configure named groups (elliptic curves)
     * on SSLEngine instances created by {@link #newInstance()}.</p>
     *
     * @return the configured SSLGroupSetter, or null if no group setter is configured
     * @see #setSSLGroupSetter(SSLGroupSetterInt)
     */
    public SSLGroupSetterInt getSSLGroupSetter() {
        return sslGroupSetter;
    }

    /**
     * Sets the SSLGroupSetter for configuring named groups on SSLEngine instances.
     *
     * <p>When an SSLGroupSetter is configured, the {@link #newInstance()} method will
     * apply the named groups to each created SSLEngine. This allows fine-grained control
     * over which elliptic curves or finite field groups are used during TLS key exchange.</p>
     *
     * <b>Example:</b>
     * <pre>{@code
     * SSLContextInfo info = new SSLContextInfo(sslContext);
     * info.setSSLGroupSetter(new SSLGroupSetterImpl(
     *     new String[]{"x25519", "secp256r1"}
     * ));
     * }</pre>
     *
     * @param sslGroupSetter the SSLGroupSetter to use, or null to disable group configuration
     * @return this SSLContextInfo instance for method chaining
     * @see SSLGroupSetterInt
     * @see #getSSLGroupSetter()
     */
    public SSLContextInfo setSSLGroupSetter(SSLGroupSetterInt sslGroupSetter) {
        this.sslGroupSetter = sslGroupSetter;
        return this;
    }

    /**
     * Creates a new SSLEngine instance configured according to this SSLContextInfo.
     *
     * <p>The returned SSLEngine is pre-configured with the protocols and cipher suites
     * specified during construction. For client-side configurations, the SSLEngine is
     * created with the server's hostname and port to enable SNI (Server Name Indication).</p>
     *
     * <b>Important:</b>
     * <p>The caller must set the SSLEngine's mode before use:</p>
     * <ul>
     *   <li>For server connections: {@code engine.setUseClientMode(false)}</li>
     *   <li>For client connections: {@code engine.setUseClientMode(true)}</li>
     * </ul>
     *
     * <b>Example Usage:</b>
     * <pre>{@code
     * SSLEngine engine = sslContextInfo.newInstance();
     * engine.setUseClientMode(sslContextInfo.isClient());
     * engine.beginHandshake();
     * }</pre>
     *
     * @return a new SSLEngine instance configured with this object's protocols and ciphers.
     *         Never returns null.
     */
    @Override
    public SSLEngine newInstance() {
        // Create SSLEngine with or without peer information based on mode
        SSLEngine ret = clientAddress != null
                ? sslContext.createSSLEngine(clientAddress.getHostName(), clientAddress.getPort())
                : sslContext.createSSLEngine();

        // Apply custom protocols if specified
        if (protocols != null && protocols.length > 0) {
            ret.setEnabledProtocols(protocols);
        }

        // Apply custom cipher suites if specified
        if (ciphers != null && ciphers.length > 0) {
            ret.setEnabledCipherSuites(ciphers);
        }

        // need to add groups here
        if (getSSLGroupSetter() != null) {
            try {
                ret = getSSLGroupSetter().setGroups(ret);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    /**
     * Determines whether this SSLContextInfo is configured for client-side connections.
     *
     * <p>This method returns {@code true} if this instance was created using one of the
     * client-side constructors (those accepting {@link InetSocketAddress} or {@link IPAddress}),
     * and {@code false} if created using server-side constructors.</p>
     *
     * <p>This can be used to automatically set the SSLEngine mode:</p>
     * <pre>{@code
     * SSLEngine engine = sslContextInfo.newInstance();
     * engine.setUseClientMode(sslContextInfo.isClient());
     * }</pre>
     *
     * @return {@code true} if this is a client-side configuration,
     *         {@code false} if this is a server-side configuration
     */
    public boolean isClient() {
        return clientAddress != null;
    }

    /**
     * Returns the underlying SSLContext.
     *
     * <p>This method provides access to the SSLContext for advanced use cases
     * where direct access is needed, such as creating SSLSocketFactory instances.</p>
     *
     * @return the SSLContext used by this instance. Never returns null.
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }

    /**
     * Returns the configured SSL/TLS protocols.
     *
     * @return array of enabled protocol names, or null if using defaults
     */
    public String[] getProtocols() {
        return protocols;
    }

    /**
     * Returns the configured cipher suites.
     *
     * @return array of enabled cipher suite names, or null if using defaults
     */
    public String[] getCiphers() {
        return ciphers;
    }

    /**
     * Returns the client address for client-side configurations.
     *
     * @return the server address to connect to, or null for server-side configurations
     */
    public InetSocketAddress getClientAddress() {
        return clientAddress;
    }
}
