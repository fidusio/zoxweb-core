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

import org.zoxweb.shared.util.GetName;

/**
 * Factory (service-provider interface) that builds and configures {@link APIServiceProvider}
 * instances for a single API type.
 * <p>
 * A creator ties together everything needed to stand up one kind of external API integration:
 * <ul>
 *     <li>{@link GetName#getName()} identifies the API type this creator handles
 *         (e.g. {@code "SMTP"}); it is the discriminator used to select a creator.</li>
 *     <li>{@link #createEmptyConfigInfo()} produces a blank {@link APIConfigInfo} template
 *         describing which configuration parameters the provider needs.</li>
 *     <li>{@link #createAPI(APIDataStore, APIConfigInfo)} instantiates a provider from a
 *         filled-in configuration.</li>
 *     <li>{@link #getExceptionHandler()} supplies the mapping from provider-native failures
 *         to {@link APIException}.</li>
 *     <li>{@link #getAPITokenManager()} exposes the provider's token manager, when it uses one.</li>
 * </ul>
 * Typical lifecycle: call {@link #createEmptyConfigInfo()} to obtain the parameter template,
 * populate it with actual values, then pass it to {@link #createAPI(APIDataStore, APIConfigInfo)}
 * to obtain a ready-to-connect provider.
 * <p>
 * See {@code org.zoxweb.server.api.provider.notification.smtp.SMTPCreator} for a reference
 * implementation.
 *
 * @author mzebib
 * @see APIServiceProvider
 * @see APIConfigInfo
 */
public interface APIServiceProviderCreator
        extends GetName {

    /**
     * Builds a blank configuration template for this API type.
     * <p>
     * The returned {@link APIConfigInfo} is pre-populated with the provider's metadata
     * (API type name, description, version, supported {@code APIServiceType}s) and the set of
     * configuration parameters the provider expects, each carrying its default value and value
     * filter (e.g. an encrypted-mask filter for a password). Callers fill in the parameter values
     * and hand the result back to {@link #createAPI(APIDataStore, APIConfigInfo)}.
     *
     * @return a new, unpopulated {@link APIConfigInfo} describing the required parameters
     */
    APIConfigInfo createEmptyConfigInfo();

    /**
     * Returns the handler that translates this provider's native exceptions into {@link APIException}.
     *
     * @return the exception handler for this API type (typically a shared singleton)
     */
    APIExceptionHandler getExceptionHandler();

    /**
     * Instantiates and configures an {@link APIServiceProvider} from a completed configuration.
     * <p>
     * The provider returned is configured but not necessarily connected; the caller drives
     * {@code connect()}/{@code newConnection()} on the result. The exception handler from
     * {@link #getExceptionHandler()} is normally installed on the provider here.
     *
     * @param dataStore the datastore backing the provider (may be used for persistence,
     *                  credential/token lookup, or dynamic-enum resolution); implementation specific
     * @param apiConfig the populated configuration, typically derived from
     *                  {@link #createEmptyConfigInfo()}
     * @return a configured, ready-to-connect service provider
     * @throws APIException if the provider cannot be created from the given configuration
     */
    APIServiceProvider<?, ?> createAPI(APIDataStore<?, ?> dataStore, APIConfigInfo apiConfig)
            throws APIException;

    /**
     * Returns the token manager for providers that authenticate via API tokens.
     *
     * @return the API token manager, or {@code null} if this provider does not use one
     */
    APITokenManager getAPITokenManager();

    /**
     * Get the APISecurityManager
     * @return
     */
    //APISecurityManager<?> getAPISecurityManager();

    /**
     *
     * @param apiSecurityManager
     */
    //void setAPISecurityManager(APISecurityManager<?> apiSecurityManager);
}
