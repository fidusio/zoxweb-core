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

import javax.net.ssl.SSLEngine;
import java.security.GeneralSecurityException;

/**
 * Abstract base class for configuring named groups (elliptic curves) on SSLEngine instances.
 *
 * <p>Named groups define the elliptic curves or finite field Diffie-Hellman groups used during
 * the TLS handshake for key exchange algorithms such as ECDHE (Elliptic Curve Diffie-Hellman
 * Ephemeral). Configuring specific named groups allows fine-grained control over the
 * cryptographic strength and compatibility of TLS connections.</p>
 *
 * <p>This class is abstract because the method for setting named groups varies across Java
 * versions. The {@link javax.net.ssl.SSLParameters#setNamedGroups(String[])} method was
 * introduced in Java 16, so implementations must handle version-specific logic.</p>
 *
 * <h2>Common Named Groups:</h2>
 * <ul>
 *   <li><b>x25519</b> - Curve25519, widely supported and efficient</li>
 *   <li><b>x448</b> - Curve448, higher security level</li>
 *   <li><b>secp256r1</b> - NIST P-256 curve (also known as prime256v1)</li>
 *   <li><b>secp384r1</b> - NIST P-384 curve</li>
 *   <li><b>secp521r1</b> - NIST P-521 curve</li>
 *   <li><b>ffdhe2048</b> - Finite Field DH with 2048-bit prime</li>
 *   <li><b>ffdhe3072</b> - Finite Field DH with 3072-bit prime</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a concrete implementation for Java 16+
 * SSLGroupSetter groupSetter = new SSLGroupSetterImpl(
 *     new String[]{"x25519", "secp256r1", "secp384r1"}
 * );
 *
 * // Apply to SSLContextInfo
 * SSLContextInfo sslContextInfo = new SSLContextInfo(sslContext);
 * sslContextInfo.setSSLGroupSetter(groupSetter);
 *
 * // Named groups will be applied when creating new SSLEngine instances
 * SSLEngine engine = sslContextInfo.newInstance();
 * }</pre>
 *
 * @author mnael
 * @see javax.net.ssl.SSLEngine
 * @see javax.net.ssl.SSLParameters
 * @see org.zoxweb.server.net.ssl.SSLContextInfo
 */
public interface SSLGroupSetterInt {

    /**
     * Returns the configured named groups.
     *
     * @return array of named group identifiers configured for this setter.
     *         Never returns null.
     */
    String[] getGroups();

    /**
     * Applies the configured named groups to the specified SSLEngine.
     *
     * <p>Implementations should configure the SSLEngine's SSL parameters to use
     * the named groups specified during construction. This typically involves
     * getting the current SSLParameters, setting the named groups, and applying
     * the parameters back to the engine.</p>
     *
     * <p>The implementation may return the same SSLEngine instance after modification,
     * or a wrapped/proxy instance if needed.</p>
     *
     * @param sslEngine the SSLEngine to configure with named groups. Must not be null.
     * @return the configured SSLEngine instance (may be the same instance or a new one)
     * @throws GeneralSecurityException if an error occurs while configuring the groups,
     *                                  such as unsupported groups or security restrictions
     */
    SSLEngine setGroups(SSLEngine sslEngine) throws GeneralSecurityException;

}
