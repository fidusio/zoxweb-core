/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

/**
 * Exception used to General Security issues
 */
@SuppressWarnings("serial")
public class AccessSecurityException
        extends AccessException {
    public AccessSecurityException() {
        super();
    }

    public AccessSecurityException(String message) {
        super(message);
    }

    public AccessSecurityException(String message, Reason reason) {
        super(message, reason);
    }


    public AccessSecurityException(String message, String urlRedirect) {
        super(message, urlRedirect, false);
    }

    public AccessSecurityException(String message, String urlRedirect, boolean reload) {
        super(message, urlRedirect, reload);
    }


}
