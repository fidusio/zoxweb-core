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
package org.zoxweb.shared.api;

/**
 * The API notification interface which extends API service provider interface.
 *
 * @param <V>
 * @author mzebib
 */
public interface APINotification<V>
        extends APIServiceProvider<V, V> {
    /**
     * This method sends a message and returns transaction information.
     *
     * @param message
     * @param apind   NOW the message will be send at the message return, QUEUED the message will be queued and sent later
     * @return APITransactionInfo
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws APIException
     */
    APITransactionInfo sendAPIMessage(APIMessage message, APINotificationDelivery apind)
            throws NullPointerException, IllegalArgumentException, APIException;

    /**
     * This method updates transaction information.
     *
     * @param transaction
     * @return APITransactionInfo
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws APIException
     */
    APITransactionInfo updateTransactionInfo(APITransactionInfo transaction)
            throws NullPointerException, IllegalArgumentException, APIException;

}
