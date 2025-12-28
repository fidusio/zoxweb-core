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
package org.zoxweb.shared.api;

import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;

import java.io.IOException;

/**
 * This interface must be implemented by APIProvider that requires and OAUTH2 token access
 */
public interface APITokenManager {

    APITokenDAO activateToken(APIDataStore<?, ?> dataStore, String userID, APIConfigInfo apiToken, NVPair... params)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;


    APITokenDAO activateToken(APIDataStore<?, ?> dataStore, ArrayValues<GetNameValue<String>> params)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;


    APITokenDAO refreshToken(APIDataStore<?, ?> dataStore, String userID, APITokenDAO apiToken)
            throws NullPointerException, IllegalArgumentException, IOException, AccessException, APIException;


    String generateOAuthURL(APIDataStore<?, ?> dataStore, String code)
            throws NullPointerException, IllegalArgumentException, AccessException;


}
