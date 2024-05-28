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
package org.zoxweb.server.net.ssl;

import javax.net.ssl.*;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * This class is a singleton object used to create a fake validation for SSL check. It is mainly
 * used to connect to expired certificates or self signed certificates. How to use it:
 * <code>
 * // create the secure connection HttpsURLConnection httpsCon = ...; // update the connection
 * SSLFactory and HostVerifier SSLCheckDisabler.updateURLConnection( httpsCon); // make the
 * connection as usual
 * </code>
 * Note: using this class in production is no recommended since it will not validate the end point
 * of the connection
 */
public class SSLCheckDisabler
    implements SSLSocketProp {

  /**
   * The SINGLETON class created
   */
  public static final SSLCheckDisabler SINGLETON = new SSLCheckDisabler();
  private SSLSocketFactory disabledSSLFactory = null;
  private TrustManager[] trustAllCerts = null;
  //private SSLSocketFactory defaultSSLFactory = null;

  //private HostnameVerifier defaultHostnameVerifier = null;
  private HostnameVerifier allHostsValid = null;


  private SSLContext sc = null;



  private SSLCheckDisabler() {
    try {
      disableSSLValidation();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
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
    if (con != null && con instanceof HttpsURLConnection) {
      ((HttpsURLConnection) con).setSSLSocketFactory(getSSLFactory());
      ((HttpsURLConnection) con).setHostnameVerifier(getHostnameVerifier());
    }
  }

  public TrustManager[] getTrustManagers()
  {
    return trustAllCerts;
  }

  /**
   * Create a bogus SSL factory and hostname verifier
   */
  private void disableSSLValidation() throws NoSuchAlgorithmException, KeyManagementException {
    //defaultSSLFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    //defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          private final X509Certificate[] certificates = new X509Certificate[]{};
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return certificates;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {

          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {

          }
        }
    };

    // Install the all-trusting trust manager
    sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new SecureRandom());
    disabledSSLFactory = sc.getSocketFactory();
    // Create all-trusting host name verifier
    allHostsValid = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
  }

}