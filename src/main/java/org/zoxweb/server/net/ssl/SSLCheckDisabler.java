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

import javax.net.ssl.*;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * This class is a singleton object used to create a fake validation for SSL check. It is mainly
 * used to connect to expired certificates or self-signed certificates. How to use it:
 * <code>
 * // create the secure connection HttpsURLConnection httpsCon = ...; // update the connection
 * SSLFactory and HostVerifier SSLCheckDisabler.updateURLConnection( httpsCon); // make the
 * connection as usual
 * </code>
 * Note: using this class in production is not recommended since it will not validate the end point
 * of the connection
 */
public class SSLCheckDisabler
        implements SSLSocketProp {

    /**
     * The SINGLETON class created
     */
    public static final SSLCheckDisabler SINGLETON = new SSLCheckDisabler();
    private final SSLSocketFactory disabledSSLFactory;
    private final TrustManager[] trustAllCerts;
    private final HostnameVerifier allHostsValid;
    private final X509Certificate[] certificates = new X509Certificate[]{};
    private final SSLContext checkDisabledSSLContext;


    private SSLCheckDisabler() {
        try {
            trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return certificates;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {

                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {

                        }
                    }
            };

            // Install the all-trusting trust manager
            checkDisabledSSLContext = SSLContext.getInstance("TLS");
            checkDisabledSSLContext.init(null, trustAllCerts, new SecureRandom());
            disabledSSLFactory = checkDisabledSSLContext.getSocketFactory();
            // Create all-trusting host name verifier
            allHostsValid = (hostname, session) -> true;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }

    /**
     * Return the bogus SSL factory
     */
    public SSLSocketFactory getSSLFactory() {
        return disabledSSLFactory;
    }

    /**
     * Return the bogus hostname verifier
     */
    public HostnameVerifier getHostnameVerifier() {
        return allHostsValid;
    }

    public void updateURLConnection(URLConnection con) {
        if (con instanceof HttpsURLConnection) {
            ((HttpsURLConnection) con).setSSLSocketFactory(getSSLFactory());
            ((HttpsURLConnection) con).setHostnameVerifier(getHostnameVerifier());
        }
    }

    public TrustManager[] getTrustManagers() {
        return trustAllCerts;
    }


    public SSLContext getCheckDisabledSSLContext() {
        return checkDisabledSSLContext;
    }

}