
/*
 * Copyright 2012 ZoxWeb.com LLC.
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

import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.zoxweb.server.crypto.CryptoUtil;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * This class is a singleton object used to create a fake validation for SSL check.
 * It is mainly used to connect to expired certificates or self signed certificates.
 * How to use it:
 * <pre>
 *  // create the secure connection
 * 	HttpsURLConnection httpsCon = ...;
 *  // update the connection SSLFactory and HostVerifier
 * 	SSLCheckDisabler.updateURLConnection( httpsCon);
 *  // make the connection as usual
 * <pre>
 * Note: using this class in production is no recommended since it will not validate the end point of the connection
 */
public class SSLCheckDisabler
implements SSLSocketProp
{
	
	
	private SSLSocketFactory disabledSSLFactory = null;
	
	private HostnameVerifier allHostsValid = null;
	
	
	//private SSLSocketFactory defaultSSLFactory = null;
	
	//private HostnameVerifier defaultHostnameVerifier = null;
	
	
	/**
	 * The SINGLETON class created
	 */
	public static final SSLCheckDisabler SINGLETON = new SSLCheckDisabler();
	
	
	private SSLCheckDisabler()
	{
		try 
		{
			disableSSLValidation();
		} 
		catch (KeyManagementException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Return the bogus SSL factory
	 */
	public SSLSocketFactory getSSLFactory()
	{
		return  disabledSSLFactory;
	}
	
	
	
	/**
	 * Return the bogus hostname verifier
	 */
	public HostnameVerifier getHostnameVerifier()
	{
		return allHostsValid;
	}
	
	/**
	 * 
	 */
	public void updateURLConnection(URLConnection con)
	{
		if ( con != null && con instanceof HttpsURLConnection)
		{
			((HttpsURLConnection)con).setSSLSocketFactory(getSSLFactory());
			((HttpsURLConnection)con).setHostnameVerifier(getHostnameVerifier());
		}
	}
	
	
	/**
	 * Create a bogus SSL factory and hostname verifier
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private void disableSSLValidation() throws NoSuchAlgorithmException, KeyManagementException
	{
		//defaultSSLFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		//defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager()
				{
					public java.security.cert.X509Certificate[] getAcceptedIssuers()
					{
					return null;
					}
					public void checkClientTrusted(X509Certificate[] certs, String authType)
					{
					}
					public void checkServerTrusted(X509Certificate[] certs, String authType)
					{
					}
			}
		};

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, CryptoUtil.defaultSecureRandom());
		disabledSSLFactory = sc.getSocketFactory();
		// Create all-trusting host name verifier
		allHostsValid = new HostnameVerifier()
		{
			public boolean verify(String hostname, SSLSession session)
			{
				return true;
			}
		};
	
		
	}
	
	

	/**
	 * Return the system default SSL Factory
	 * @return
	 */
//	public SSLSocketFactory getDefaultSSLSocketFactory()
//	{
//		return defaultSSLFactory;
//	}
//	
//	/**
//	 * Return the system default hostname verifier
//	 * @return
//	 */
//	public HostnameVerifier getDefaultHostnameVerifier()
//	{
//		return defaultHostnameVerifier;
//	}
	
}
