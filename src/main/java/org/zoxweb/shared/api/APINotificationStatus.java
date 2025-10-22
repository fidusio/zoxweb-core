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
 * This enum contains notification status types.
 *
 * @author mzebib
 *
 */
public enum APINotificationStatus {
    // Provider confirmed delivery
    DELIVERY_CONFIRMED,
    // Sent to provider, provider can't confirm delivery
    DELIVERED,
    // Provider received message but failed to deliver it
    FAILED,
    // Provider received the message and it is queued waiting for delivery confirmation
    PENDING,
    // Exception occurred at the provider level
    PROVIDER_EXCEPTION,
    // Expired message delivery
    EXPIRED

}
