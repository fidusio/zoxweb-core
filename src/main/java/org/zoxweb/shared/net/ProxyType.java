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
package org.zoxweb.shared.net;

/**
 * Enumeration of proxy connection types.
 * Defines the different types of proxy connections that can be used
 * for network communications.
 *
 * @author mnael
 * @see IPAddress
 */
public enum ProxyType {
    /**
     * Represents a direct connection, or the absence of a proxy.
     */
    DIRECT,
    /**
     * Represents proxy for high level protocols such as HTTP or FTP.
     */
    HTTP,
    /**
     * Represents a SOCKS (V4 or V5) proxy.
     */
    SOCKS
}
