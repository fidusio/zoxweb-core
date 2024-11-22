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
package org.zoxweb.server.http;

import okhttp3.*;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.ssl.SSLCheckDisabler;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.util.*;

import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OkHTTPCall
{

	public static final LogWrapper log = new LogWrapper(OkHTTPCall.class).setEnabled(false);

	static class RBNamedValue
		extends  RequestBody
	{

		private final  NamedValue<?> namedValue;
		RBNamedValue(NamedValue<?> nvc)
		{
			this.namedValue = nvc;
		}


		/**
		 * @return
		 */
		@Nullable
		@Override
		public MediaType contentType()
		{
			HTTPMediaType mt = namedValue.getProperties().getValue(HTTPConst.CNP.MEDIA_TYPE);
			return MediaType.parse(mt.getValue());
		}

		@Override
		public long contentLength()
				throws IOException
		{
			return namedValue.getProperties().getValue(HTTPConst.CNP.CONTENT_LENGTH);
		}

		/**
		 * @param bufferedSink
		 * @throws IOException
		 */
		@Override
		public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException
		{
			InputStream is = null;
			long totalRead = 0;
			try {
				if (namedValue.getValue() != null) {
					if (namedValue.getValue() instanceof File) {
						is = new FileInputStream((File) namedValue.getValue());
					} else if (namedValue.getValue() instanceof InputStream) {
						is = (InputStream) namedValue.getValue();
					}

					if (is != null) {
						byte[] buffer = new byte[4092]; // 4KB buffer
						int bytesRead;

						while ((bytesRead = is.read(buffer)) != -1)
						{
							bufferedSink.write(buffer, 0, bytesRead);
							bufferedSink.flush();
							totalRead+=bytesRead;
						}
					}
				}
			}
			finally
			{
				IOUtil.close(is);
				if(log.isEnabled()) log.getLogger().info("TotalRead :" + totalRead + " content-length: " + contentLength());
			}
		}
	}

	static class RBBinaryContent
		extends RequestBody
	{

		private final MediaType mediaType;
		private final byte[] content;

		RBBinaryContent(NVGenericMap nvgm)
		{
			mediaType = MediaType.parse(HTTPMediaType.APPLICATION_JSON.getValue());
			content = SharedStringUtil.getBytes(GSONUtil.toJSONDefault(nvgm));
		}

		RBBinaryContent(MediaType mediaType, String content)
		{
			this.mediaType = mediaType;
			this.content = SUS.isNotEmpty(content) ? SharedStringUtil.getBytes(content) : null;

		}
		RBBinaryContent(MediaType mediaType, byte[] content)
		{
			this.mediaType = mediaType;
			this.content = content;
		}
		/**
		 * @return the content type
		 */
		@Nullable
		@Override
		public MediaType contentType() {
			return mediaType;
		}

		@Override
		public long contentLength() throws IOException {
			if (content != null)
				return content.length;

			return super.contentLength();
		}

		/**
		 * @param bufferedSink to write to the connection stream
		 * @throws IOException in case of error
		 */
		@Override
		public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
			if(content != null)
			{
				if(log.isEnabled()) log.getLogger().info("Content size : " + content.length);
				ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
				byte[] buffer = new byte[4092]; // 8KB buffer
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
						try {
							bufferedSink.write(buffer, 0, bytesRead);
						}
						catch (IOException e)
						{
						e.printStackTrace();
						throw e;
					}
				}

				if(log.isEnabled()) log.getLogger().info("Finished");
			}
		}
	}


	public final static RateCounter OK_HTTP_CALLS = new RateCounter("ok-http-calls", "This counter is used the send static functions it capture the whole call duration including parameter formatting, call, result decoding.");

	public final static AtomicBoolean ENABLE_HTTP = new AtomicBoolean(true);
	private final HTTPMessageConfigInterface hmci;





	private static final OkHttpClient sslEnabledClient = createOkHttpBuilder(null, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND, true, 10, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND).build();
	private static final OkHttpClient sslDisabledClient = createOkHttpBuilder(null, null, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND,false, 10, HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND).build();

	private final OkHttpClient okHttpClient;
	public static OkHttpClient.Builder createOkHttpBuilder(ExecutorService executorService,
														   IPAddress proxy,
														   long timeoutInSecond,
														   boolean enableSSLCheck,
														   int connectionPool,
														   long kaTimeoutSecond)
	{
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		if(!enableSSLCheck)
		{
			builder.sslSocketFactory(SSLCheckDisabler.SINGLETON.getSSLFactory(), (X509TrustManager) SSLCheckDisabler.SINGLETON.getTrustManagers()[0]);
			builder.hostnameVerifier(SSLCheckDisabler.SINGLETON.getHostnameVerifier());
		}

		// Optionally configure timeouts
		builder.connectTimeout(timeoutInSecond, TimeUnit.SECONDS);
		builder.readTimeout(timeoutInSecond, TimeUnit.SECONDS);
		builder.writeTimeout(timeoutInSecond, TimeUnit.SECONDS);
		//builder.protocols(Arrays.asList(Protocol.HTTP_1_1));

		// allow redirect
		builder.followRedirects(true).followSslRedirects(true);
		if(proxy != null) {
			builder.setProxy$okhttp(IOUtil.toProxy(proxy));
		}
		if(connectionPool>0)
			builder.connectionPool(new ConnectionPool(connectionPool, kaTimeoutSecond, TimeUnit.SECONDS));
		if(executorService != null)
			builder.dispatcher(new Dispatcher(executorService));



		return builder;
	}


	public OkHTTPCall(HTTPMessageConfigInterface hmci)
	{
		this(null, hmci);
	}

	public OkHTTPCall(OkHttpClient client, HTTPMessageConfigInterface hmci)
	{
		SUS.checkIfNulls("HTTPMessageConfigInterface can't be null", hmci);
		this.hmci = hmci;
		if (client == null)
		{
			if (hmci.getProxyAddress() != null)
			{
				long timeout = hmci.getTimeout();
				if (timeout == 0)
					timeout = HTTPMessageConfigInterface.DEFAULT_TIMEOUT_20_SECOND;
				else
					timeout = TimeUnit.SECONDS.convert(timeout, TimeUnit.MILLISECONDS);
				okHttpClient = createOkHttpBuilder(null, hmci.getProxyAddress(), timeout, !hmci.isSecureCheckEnabled(), 0, 0).build();
			}
			else
				okHttpClient = hmci.isSecureCheckEnabled() ? sslEnabledClient : sslDisabledClient;
		}
		else
			okHttpClient = client;

	}


	/**
	 * 
	 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
	 * 
	 * @return HTTPResponseData
	 * @throws IOException in case of error
	 */
	public HTTPResponseData sendRequest() throws IOException
	{
		if(!ENABLE_HTTP.get())
			return null;

		long ts = System.currentTimeMillis();




		// check the HTTPMethod first
		// based on the content type build the rest of the body
		// check if application/x-www-form-urlencoded,  multipart/form-data
		// check the authorization check if the URL has a basic client, or set via hmci
		// create the request body
		// invoke the okHttClient
		// convert the response to HTTPResponseData
		// build the request body
		Request.Builder requestBuilder = new Request.Builder();

		// set the headers
		for (GetNameValue<String> gnv : hmci.getHeaders().asArrayValuesString().values())
		{
			requestBuilder.addHeader(gnv.getName(), gnv.getValue());
		}

		String fullURL = HTTPUtil.formatURI(SharedStringUtil.concat(hmci.getURL(), hmci.getURI(), "/"), hmci.getParameters());
		String urlEncodedParameter = null;
		// set the url
		// if method is get and hmci.isURLEncodingEnabled() merge uri + encoded parameters
		if (hmci.isURLEncodingEnabled())
		{
			urlEncodedParameter = SharedStringUtil.trimOrNull(HTTPEncoder.URL_ENCODED.format(hmci.getParameters().asArrayValuesString().values()));
			if (hmci.getMethod() == HTTPMethod.GET && urlEncodedParameter != null)
			{
				if(fullURL.indexOf('?') == -1)
					fullURL += "?" + urlEncodedParameter;
				else if (fullURL.endsWith("&"))
					fullURL += urlEncodedParameter;
				else
					fullURL += "&" + urlEncodedParameter;

			}
		}



		URL url = new URL(fullURL);
		requestBuilder.url(url);

		/////// set the authorization
		GetNameValue<String> authorizationHeader = HTTPAuthScheme.BASIC.toHTTPHeader(url.getUserInfo());

		if(authorizationHeader == null && hmci.getAuthorization() != null)
		{
			authorizationHeader = hmci.getAuthorization().toHTTPHeader();
		}

		if (authorizationHeader != null)
		{
			requestBuilder.header(authorizationHeader.getName(), authorizationHeader.getValue());
		}
		// end of authorization;

		// create the request body from hmci
		RequestBody requestBody = null;
		if(SUS.isNotEmpty(urlEncodedParameter) && hmci.isURLEncodingEnabled() && hmci.getMethod() != HTTPMethod.GET)
		{
			requestBody = new RBBinaryContent(null, urlEncodedParameter);
		}
		else if(hmci.isMultipartFormDataEnabled())
		{
			MultipartBody.Builder mbBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

			for(GetNameValue<?> gnv : hmci.getParameters().values())
			{
				if (gnv instanceof NamedValue)
				{
					mbBuilder.addFormDataPart(gnv.getName(),  ((NamedValue<?>) gnv).getProperties().getValue(HTTPConst.CNP.FILENAME), new RBNamedValue((NamedValue<?>) gnv));
				}
				else if (SUS.isPrimitiveGNV(gnv))
				{
					mbBuilder.addFormDataPart(gnv.getName(), "" + gnv.getValue());
				}
				else if (gnv instanceof NVGenericMap)
				{
					mbBuilder.addFormDataPart(gnv.getName(), gnv.getName(), new RBBinaryContent((NVGenericMap) gnv));
				}

			}
			requestBody = mbBuilder.build();
		}
		else if(SUS.isNotEmpty(hmci.getContent()))
		{
			requestBody = new RBBinaryContent(null, hmci.getContent());
		}


		requestBuilder.method(hmci.getMethod().getName(), requestBody);
		Request request = requestBuilder.build();
		if(log.isEnabled()) log.getLogger().info("request headers: " + request.headers().toMultimap());



		Response response = null;
		ResponseBody responseBody = null;
		Map<String, List<String>> respHeaders = null;
		HTTPStatusCode responseStatusCode = null;
		byte[] responseContent = null;
		HTTPResponseData hrd = null;
		try
		{
			response = okHttpClient.newCall(request).execute();

			if(log.isEnabled()) log.getLogger().info("response: " + response);

			responseBody = response.body();
			respHeaders = response.headers().toMultimap();
			responseContent = responseBody.bytes();
			responseStatusCode = HTTPStatusCode.statusByCode(response.code());

			hrd = new HTTPResponseData(responseStatusCode.CODE, respHeaders, responseContent, System.currentTimeMillis() - ts);
			if(!response.isSuccessful() && hmci.isHTTPErrorAsException())
			{
				throw new HTTPCallException(response.message(),  hrd);
			}
		}
		finally
		{
			IOUtil.close(responseBody, response);
		}



		//rb.method(hmci.getMethod().getValue());




//		ByteArrayOutputStream ret = null;
//		int status;
//		Map<String, List<String>> respHeaders = null;
//		// format the payload first
//		String encodedContentParams = HTTPUtil.formatParameters(hmci.getParameters().asArrayValuesString(), hmci.getCharset(), hmci.isURLEncodingEnabled(), hmci.getHTTPParameterFormatter());
//
//
//		boolean embedPostPutParamsInURI = false;
//		GetNameValue<String> contentType = hmci.getParameters().asArrayValuesString().get(HTTPHeader.CONTENT_TYPE.getName());
//		if (contentType != null && contentType.getValue() != null && contentType.getValue().toLowerCase().indexOf("x-www-form-urlencoded") == -1)
//		{
//			embedPostPutParamsInURI = true;
//		}
//
//		if (encodedContentParams.length() > 0)
//		{
//			switch(hmci.getMethod())
//			{
//
//			case POST:
//			case PUT:
//				if (!embedPostPutParamsInURI || hmci.isMultiPartEncoding())
//				{
//					HTTPMultiPartUtil.preMultiPart(hmci);
//					break;
//				}
//			case GET:
//			case DELETE:
//			case PATCH:
//			case PURGE:
//			case CONNECT:
//			case COPY:
//			case LINK:
//			case LOCK:
//			case PROPFIND:
//			case TRACE:
//			case UNLINK:
//			case UNLOCK:
//			case VIEW:
//				// if we have a GET
//
//				break;
//			default:
//				break;
//
//
//			}
//		}
//
//		OutputStream os = null;
//		InputStream is = null;
//		InputStream isError = null;
//		HttpURLConnection con = null;
//		Proxy proxy = null;
//
//

//		try
//		{
//
////			if (proxy != null)
////			{
////				con = (HttpURLConnection) url.openConnection(proxy);
////			}
////			else
////			{
////				con = (HttpURLConnection) url.openConnection();
////			}
////
//
//
//
//			con.setRequestMethod(hmci.getMethod().toString());
//
//			con.setDoInput(true);
//			con.setDoOutput(true);
//			con.setAllowUserInteraction(true);
//			con.setInstanceFollowRedirects(hmci.isRedirectEnabled());
//			con.setConnectTimeout(hmci.getConnectTimeout());
//			con.setReadTimeout(hmci.getReadTimeout());
//
//			URL url = new URL(hmci.getURL());
//
//
//
////			ArrayValues<GetNameValue<String>> reqHeaders = hcc.getHeaders().asArrayValuesString();
//			// set the request headers
//			if (hmci.getHeaders() != null)
//			{
//				for (GetNameValue<?> nvp : hmci.getHeaders().values())
//				{
//					con.setRequestProperty(nvp.getName(), ""+nvp.getValue());
//				}
//			}
//
//
//			// set the authentication from url or hcc
//			// if url.getUserInfo() is not null ?
//			// if hcc.getUser() && hcc.getPassword() != null
//			// if still null check if the HTTPAuthentication is not null
//			GetNameValue<String> authorizationHeader = HTTPAuthScheme.BASIC.toHTTPHeader(url.getUserInfo(), null);
//
//			if(authorizationHeader == null && hmci.getAuthorization() != null)
//			{
//				authorizationHeader = hmci.getAuthorization().toHTTPHeader();
//			}
//
//			if (authorizationHeader != null)
//			{
//				con.setRequestProperty(authorizationHeader.getName(), authorizationHeader.getValue());
//			}
//
//
//			// write the payload if it is a post
//			if (!embedPostPutParamsInURI)
//			{
//				switch (hmci.getMethod())
//				{
//				case POST:
//				case PUT:
//					os = con.getOutputStream();
//					if (hmci.isMultiPartEncoding())
//					{
//						HTTPMultiPartUtil.writeMultiPartContent(os, hmci.getBoundary(), hmci.getParameters().asArrayValuesString());
//					}
//					else
//						os.write(encodedContentParams.getBytes());
//					break;
//					default:
//
//				}
//			}
//
//
//
//
//
//			if (hmci.getContent() != null && hmci.getContent().length > 0)
//			{
//				if (os == null)
//				{
//					os = con.getOutputStream();
//				}
//
//				os.write(hmci.getContent());
//			}
//
//
//			IOUtil.flush(os);
//			status = (proxy != null && proxy.type() == Proxy.Type.SOCKS) ? 200 : con.getResponseCode();
//
//			// check if we have an error in the response
//			isError = con.getErrorStream();
//			if (isError != null)
//			{
//				ret  = IOUtil.inputStreamToByteArray(isError, false);
//				respHeaders = con.getHeaderFields();
//				HTTPResponseData hrd = new HTTPResponseData(status, respHeaders, ret.toByteArray(), System.currentTimeMillis() -ts );
//				if (hmci.isHTTPErrorAsException())
//					throw new HTTPCallException(con.getResponseMessage(),  hrd);
//				else
//					return hrd;
//			}
//
//			HTTPStatusCode hsc = HTTPStatusCode.statusByCode(status);
//
//			if (hsc != null && hmci.isRedirectEnabled())
//			{
//				switch(hsc)
//				{
//				case MOVED_PERMANENTLY:
//				case FOUND:
//				case SEE_OTHER:
//					NVPair cookie = HTTPUtil.extractCookie(con.getHeaderFields(), 0);
//					if (cookie != null)
//						hmci.getHeaders().add(cookie);
//					String newURL = con.getHeaderField("Location");
//
//					hmci.setURI(null);
//					hmci.setURL(newURL);
//
//					HTTPCall2 hccRedirect = new HTTPCall2(hmci);
//					return hccRedirect.sendRequest();
//
//				default:
//
//
//				}
//			}
//
//
//
//			is = con.getInputStream();
//
//
//
//
//			respHeaders = con.getHeaderFields();
//
//
//		}
//		finally
//		{
//			if ( con != null)
//			{
//			    try {
//			      con.disconnect();
//			    }
//			    catch(Exception e) {}
//			}
//
//			IOUtil.close(os);
//			IOUtil.close(is);
//			IOUtil.close(isError);
//
//		}
//
//
//		HTTPResponseData hrd = new HTTPResponseData(status, respHeaders, ret != null ? ret.toByteArray() : null, System.currentTimeMillis() - ts);


		return hrd;
	}



	public static HTTPResponseData send(HTTPMessageConfigInterface hmci)
			throws IOException
	{
		return send(null, hmci);
	}

	public static HTTPResponseData send(OkHttpClient okHttpClient, HTTPMessageConfigInterface hmci)
			throws IOException
	{
		HTTPResponseData ret;
		try
		{
			ret = new OkHTTPCall(okHttpClient, hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{
			OK_HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		OK_HTTP_CALLS.register(ret.getDuration());
		return ret;
	}

	public static <O> HTTPAPIResult<O> send(OkHttpClient okHttpClient, HTTPMessageConfigInterface hmci, Class<?> clazz)
			throws IOException
	{

		HTTPResponseData hrd;
		try
		{
			hrd = new OkHTTPCall(okHttpClient, hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{

			OK_HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		HTTPAPIResult<O> ret =  HTTPUtil.toHTTPResponseObject(hrd, clazz);
		OK_HTTP_CALLS.register(hrd.getDuration());
		return ret;
	}

	public static <O> HTTPAPIResult<O> send(OkHttpClient okHttpClient, HTTPMessageConfigInterface hmci, DataDecoder<HTTPResponseData, O> decoder)
			throws IOException
	{
		HTTPResponseData hrd ;
		try
		{
			hrd = new OkHTTPCall(okHttpClient, hmci).sendRequest();
		}
		catch(HTTPCallException e)
		{

			OK_HTTP_CALLS.register(e.getResponseData().getDuration());
			throw e;
		}
		O result = decoder.decode(hrd);
		HTTPAPIResult<O> ret = new HTTPAPIResult<O>(hrd.getStatus(), hrd.getHeaders(), result, hrd.getDuration());
		OK_HTTP_CALLS.register(ret.getDuration());
		return ret;
	}
}