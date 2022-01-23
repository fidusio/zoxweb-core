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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.server.util.ReflectionUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.http.HTTPEncoder;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.protocol.MessageStatus;
import org.zoxweb.shared.protocol.ProtocolDelimiter;
import org.zoxweb.shared.util.*;

/**
 * Contains HTTP utitlty methods.
 */
public class HTTPUtil 
{
	private static final Lock LOCK = new ReentrantLock();
	private static final AtomicBoolean extraMethodAdded =  new AtomicBoolean();

	/**
	 * The constructor is declared private to prevent instantiation.
	 */
	private HTTPUtil()
	{

	}

	/**
	 * Checks if given text is HTML.
	 * @param txt check if the txt is html
	 * @return status
	 */
	public static boolean isHTML(String txt)
	{
		String text = Jsoup.parse(txt).text();
		return !text.equals(txt);
	}

	public static HTTPMessageConfigInterface formatResponse(NVEntity nve, HTTPStatusCode statusCode)
			throws IOException
	{
		HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(null, null, (HTTPMethod) null);
		hmci.setHTTPStatusCode(statusCode);
		hmci.setContentType(HTTPMimeType.APPLICATION_JSON.format(HTTPConst.CHARSET_UTF_8));

		hmci.setContent(GSONUtil.toJSON(nve,false, false, true));
		return hmci;
	}

	public static HTTPMessageConfigInterface formatResponse(NVGenericMap nvgm, HTTPStatusCode statusCode)
			throws IOException
	{
		HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(null, null, (HTTPMethod) null);
		hmci.setHTTPStatusCode(statusCode);
		hmci.setContentType(HTTPMimeType.APPLICATION_JSON.format(HTTPConst.CHARSET_UTF_8));
		hmci.setContent(GSONUtil.toJSONGenericMap(nvgm,false, false, true));
		return hmci;
	}


	public static UByteArrayOutputStream formatResponse(HTTPMessageConfigInterface hcc, UByteArrayOutputStream ubaos)
	{
		if (ubaos == null)
		{
			ubaos = new UByteArrayOutputStream();
		}
		else
		{
			ubaos.reset();
		}

		HTTPVersion hv = hcc.getHTTPVersion();
		if(hv == null)
			hv = HTTPVersion.HTTP_1_1;
		// write the first line
		ubaos.write(hv.getValue() + " " + hcc.getHTTPStatusCode().CODE + " " +hcc.getHTTPStatusCode().REASON + ProtocolDelimiter.CRLF.getValue());
		// write headers
		if (hcc.getContent() != null && hcc.getContent().length > 0)
		{
			hcc.setContentLength(hcc.getContent().length);
		}

		for (GetNameValue<String> header : hcc.getHeaders().values())
		{
			ubaos.write(header.getName() + ": " + header.getValue() +  ProtocolDelimiter.CRLF.getValue());
		}

		ubaos.write(ProtocolDelimiter.CRLF.getValue().getBytes());

		if (hcc.getContent() != null && hcc.getContent().length > 0)
		{
			ubaos.write(hcc.getContent());
		}

		return ubaos;
	}

	public static UByteArrayOutputStream formatResponse(HTTPResponseData rd, UByteArrayOutputStream ubaos)
	{
		if (ubaos == null)
		{
			ubaos = new UByteArrayOutputStream();
		}
		else
		{
			ubaos.reset();
		}

		HTTPStatusCode hsc = HTTPStatusCode.statusByCode(rd.getStatus());
		// write the first line
		ubaos.write(HTTPVersion.HTTP_1_1.getValue() + " " + hsc.CODE + " " +hsc.REASON + ProtocolDelimiter.CRLF.getValue());
		// write headers

		Set<Map.Entry<String, List<String>>> set = rd.getResponseHeaders().entrySet();
		Iterator<Map.Entry<String, List<String>>> params= set.iterator();

		while (params.hasNext())
		{
			Map.Entry<String, List<String>> header = params.next();

			if (header.getKey() != null)
			{
				ubaos.write(header.getKey()+": ");
				boolean firstPass = false;
				for (String value : header.getValue())
				{
					if (firstPass)
					{
						ubaos.write(",");
					}

					ubaos.write(value);

					firstPass = true;
				}

				ubaos.write(ProtocolDelimiter.CRLF.getValue().getBytes());
			}
		}

		ubaos.write(ProtocolDelimiter.CRLF.getValue().getBytes());
		ubaos.write(rd.getData());

		return ubaos;
	}


	public  static UByteArrayOutputStream formatRequest(HTTPMessageConfigInterface hcc, boolean parseURI, UByteArrayOutputStream ubaos, String ...headersToRemove)
	{

		//InetSocketAddressDAO  isad = HTTPUtil.parseHost(hcc.getURI());
		if (parseURI)
		{
			hcc.setURI(HTTPUtil.parseURI(hcc.getURI()));
		}

		if (headersToRemove != null)
		{
			for (String header : headersToRemove)
			{
				hcc.getHeaders().remove(header);
			}
		}

		if (ubaos == null)
		{
			ubaos = new UByteArrayOutputStream();
		}
		else
		{
			ubaos.reset();
		}

		// first line http request
		ubaos.write(hcc.getMethod().getName() + " " + hcc.getURI() + " " + hcc.getHTTPVersion().getValue() + ProtocolDelimiter.CRLF.getValue());
		// headers
		if (hcc.getContent() != null && hcc.getContent().length > 0)
		{
			hcc.setContentLength(hcc.getContent().length);
		}
		//hcc.getHeaderParameters().remove(HTTPHeaderName.HOST.getName());
		for (GetNameValue<String> header : hcc.getHeaders().values())
		{
			ubaos.write(header.getName() + ": " + header.getValue() + ProtocolDelimiter.CRLF.getValue());
		}
		// header termination
		ubaos.write(ProtocolDelimiter.CRLF.getBytes());

		// content if available
		if (hcc.getContent() != null && hcc.getContent().length > 0)
		{
			ubaos.write(hcc.getContent());
		}

		return ubaos;
	}

	public static void addHTTPMethods() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		if (!extraMethodAdded.get())
		{
			try
			{
				LOCK.lock();

				if (!extraMethodAdded.get())
				{

					ReflectionUtil.updateFinalStatic(HttpURLConnection.class, "methods", HTTPMethod.toMethodNames());
//					new String[]
//							{
//								// We are adding patch at the end
//								"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH", "COPY", "LINK", "UNLINK", "PURGE", "LOCK", "UNLOCK"
//							});
					extraMethodAdded.set(true);
				}
			}
			finally
			{
				LOCK.unlock();
			}
		}
//		Class<?> httpUrlConnection = HttpURLConnection.class;
//		Field field = httpUrlConnection.getDeclaredField("methods");
//		System.out.println("" + field);
//		field.setAccessible( true);
//
//		String methods[] = (String[]) field.get((Object) null);
//////		ArrayList<String> strList = new ArrayList<String>();
////
////		for ( int i = 0; i < methods.length; i++)
////		{
////			//strList.add(methods[i]);
////			if ( methods[i].equals("TRACE"))
////			{
//////				strList.add(methods[i]);
////				methods[i] = HTTPMethod.PATCH.name();
////			}
////		}
//////		strList.add( HTTPMethod.PATCH.name());
//////
//////		methods = strList.toArray( new String[0]);
//////		System.out.println(" list " + strList);
//////		//field.set(null, methods);
//////		setFinalStatic( field, methods);
//
//
//		methods = (String[]) field.get((Object) null);
//
//		for ( int i = 0; i < methods.length; i++)
//		{
//			System.out.println( methods[i]);
//		}
	}


	@SuppressWarnings("unchecked")
	public static String formatParameters(List<GetNameValue<String>> params, String charset, boolean urlEncode, HTTPEncoder hpf) throws UnsupportedEncodingException
	{
		return formatParameters((GetNameValue<String>[])params.toArray(new GetNameValue<?>[params.size()]), charset, urlEncode, hpf);
	}


	public static String formatParameters(GetNameValue<String>[] params, String charset, boolean urlEncode, HTTPEncoder hpf) throws UnsupportedEncodingException
	{
		if (SharedStringUtil.isEmpty(charset))
		{
			charset = SharedStringUtil.UTF_8;
		}

		StringBuilder sb = new StringBuilder();

		if (params != null)
		{
			for (GetNameValue<String> nvp : params)
			{
				if (sb.length() > 0)
				{
					sb.append(hpf.getValue());
				}

				if (nvp != null && nvp.getName() != null)
				{
					if (hpf == HTTPEncoder.URL_ENCODED)
					{
						sb.append(urlEncode ? URLEncoder.encode(nvp.getName(), charset) : nvp.getName());
						sb.append(hpf.getNameValueSep());
					}

					if (nvp.getValue() != null)
					{
						String valueCharset = charset;

						if (nvp instanceof GetCharset)
						{
							if (((GetCharset)nvp).getCharset() != null)
							{
								valueCharset = ((GetCharset)nvp).getCharset();
							}
						}

						sb.append(urlEncode ? URLEncoder.encode(nvp.getValue(), valueCharset) : nvp.getValue());
					}
				}


			}
		}

		return sb.toString();
	}


	public static String formatParameters(ArrayValues<GetNameValue<String>> params, String charset, boolean urlEncode, HTTPEncoder hpf) throws UnsupportedEncodingException
	{
		return formatParameters(params.values(), charset, urlEncode, hpf);
	}


	public static UByteArrayOutputStream  formatBinaryParameters(ArrayValues<GetNameValue<?>> params, String charset, boolean urlEncode) throws UnsupportedEncodingException
	{
		if (SharedStringUtil.isEmpty(charset))
		{
			charset = SharedStringUtil.UTF_8;
		}

		UByteArrayOutputStream ret = new UByteArrayOutputStream();

		if (params != null)
		{
			for (GetNameValue<?> nvp : params.values())
			{
				if (ret.size() > 0)
				{
					ret.write('&');
				}

				if (nvp != null && nvp.getName() != null)
				{
					ret.write(urlEncode ? URLEncoder.encode(nvp.getName(), charset) : nvp.getName());
					ret.write('=');

					if (nvp.getValue() != null)
					{
						String valueCharset = charset;

						if (params instanceof GetCharset)
						{
							if (((GetCharset)params).getCharset() != null)
							{
								valueCharset = ((GetCharset)params).getCharset();
							}
						}

						if (nvp.getValue() instanceof String)
						{
							ret.write(urlEncode ? URLEncoder.encode((String)nvp.getValue(), valueCharset) : (String)nvp.getValue());
						}

						if (nvp.getValue() instanceof byte[])
						{
							ret.write((byte[])nvp.getValue());
						}

					}
				}


			}
		}
		return ret;
	}

	public static String formatParametersNoEncoding( List<GetNameValue<String>> params)
	{
		StringBuilder sb = new StringBuilder();

		if (params != null)
		{
			for (GetNameValue<String> nvp : params)
			{
				if (sb.length() > 0)
				{
					sb.append('&');
				}

				if (nvp != null && nvp.getName() != null)
				{
					sb.append( nvp.getName());
					sb.append('=');
					if (nvp.getValue() != null)
					{
						sb.append( nvp.getValue());
					}
				}
			}
		}
		return sb.toString();
	}

//	public static String formatParametersNoEncoding( ArrayValues<GetNameValue<String>> params)
//	{
//		StringBuilder sb = new StringBuilder();
//		if ( params != null)
//		{
//			for ( GetNameValue<String> nvp : params.values())
//			{
//				if ( sb.length() > 0)
//				{
//					sb.append('&');
//				}
//				if ( nvp != null && nvp.getName() != null)
//				{
//					sb.append( nvp.getName());
//					sb.append('=');
//					if (nvp.getValue() != null)
//						sb.append( nvp.getValue());
//				}
//			}
//		}
//		return sb.toString();
//	}


	public static List<GetNameValue<String>> parseQuery(URL url)
	{
		return parseQuery(url.getQuery(), false);
	}


	public static List<GetNameValue<String>> parseQuery(String query, boolean fullURI)
	{
		if (query == null)
		{
			return null;
		}
		if(fullURI)
		{
			int index = query.indexOf("?");
			if (index != -1)
				query = query.substring(index + 1);
		}

		String allParams[] = query.split("&");
		ArrayList<GetNameValue<String>> ret = new ArrayList<GetNameValue<String>>();

		for (String param : allParams)
		{
			NVPair nvp = SharedUtil.toNVPair(param);
			if (nvp != null && !SharedStringUtil.isEmpty(nvp.getName()))
			{
				ret.add(nvp);
			}
		}

		return ret;
	}


	public static ArrayValues<GetNameValue<String>> parseQuery(ArrayValues<GetNameValue<String>> av, String query, boolean fullURI)
	{
		if (query == null)
		{
			return null;
		}
		if(fullURI)
		{
			int index = query.indexOf("?");
			if (index != -1)
				query = query.substring(index + 1);
		}

		String allParams[] = query.split("&");


		for (String param : allParams)
		{
			NVPair nvp = SharedUtil.toNVPair(param);
			if (nvp != null && !SharedStringUtil.isEmpty(nvp.getName()))
			{
				av.add(nvp);
			}
		}

		return av;
	}

	/**
	 * Return path value
	 * @param path
	 * @return
	 */
	public static String basePath(String path, boolean excludeLastPathSep)
	{
		if(path != null)
		{
			int index = path.indexOf('{');
			if (index != -1)
			{
				String ret = path.substring(0, index);
				if (excludeLastPathSep && ret.length() > 0 && ret.endsWith("/"))
					ret = ret.substring(0, ret.length() -1);
				return ret;
			}
		}
		return path;
	}

	/**
	 * Return path parameters base on the meta path and the value path
	 * <ul>
	 * <li>pathWithMetas: /start/abc/{id}/{info}</li>
	 * <li>pathWithValues:/start/abc/12345/postPath</li>
	 * <ul></ul>
	 * <br>
	 *     NVGenericMap will return NVLong("id", 12345), NVPair("info","batata")
	 * @param pathWithMetas
	 * @param pathWithValues
	 * @param addMissing
	 * @return
	 */
	public static Map<String, Object> parsePathParameters(String pathWithMetas, String pathWithValues, boolean addMissing)
	{
		Map<String, Object> ret = new LinkedHashMap<String, Object>();
		String[] paramNames = pathWithMetas.split("/");
		String[] paramValues = pathWithValues.split("/");
		for(int i = 0; i < paramNames.length; i++)
		{
			List<CharSequence> ch = SharedStringUtil.parseGroup(paramNames[i], "{","}", false);
			if(ch.size() == 1)
			{
				String name = ch.get(0).toString();
				String value = i < paramValues.length ? paramValues[i] : null;
				//we will only add found values
				if(value != null || addMissing)
					ret.put(name, value);
			}
		}
		return ret;
	}

//	public static NVGenericMap parsePathParameters(String pathWithMetas, String pathWithValues, ReflectionUtil.MethodAnnotations metaData)
//	{
//		NVGenericMap nvgm = new NVGenericMap();
//		String[] paramNames = pathWithMetas.split("/");
//		String[] paramValues = pathWithValues.split("/");
//		int paramCounter = 0;
//		for(int i = 0; i < paramNames.length; i++)
//		{
//			List<CharSequence> ch = SharedStringUtil.parseGroup(paramNames[i], "{","}", false);
//
//			if(ch.size() == 1)
//			{
//				String value = i < paramValues.length ? paramValues[i] : null;
//				Parameter
//				if(value != null)
//				{
//
//				}
//				nvgm.add(ch.get(0).toString(), value);
//			}
//		}
//
//
//		return nvgm;
//	}


	public static String parseHostURL(URL url)
	{
		StringBuilder ret = new StringBuilder();
		ret.append(url.getProtocol());
		ret.append("://");
		ret.append(url.getHost());
		int port = url.getPort();
		if (port != -1 && port != 80 && port != 443)
		{
			ret.append(':');
			ret.append(port);
		}

		return ret.toString();
	}


	public static String parseURI(URL url, boolean includeParam)
	{
		StringBuilder ret = new StringBuilder();
		//ret.append(url.getPath());
		ret.append(url.getPath());
		if (includeParam && url.getQuery()!=null)
		{
			ret.append("?");
			ret.append(url.getQuery());
		}

		return ret.toString();
	}


	public static String parseURI(String url)
	{
		int index = url.indexOf(ProtocolDelimiter.COLON_PATH.getValue());
		int hostStart;
		if (index == -1)
		{
			hostStart = 0;
		}
		else
		{
			hostStart = index + ProtocolDelimiter.COLON_PATH.getValue().getBytes().length;
		}
		int hostEnd;
		index = url.indexOf('/', hostStart);
		if (index == -1)
		{
			hostEnd = url.length();
		}
		else
		{
			hostEnd = index;
		}

		String ret = url.substring(hostEnd);
		if (SharedStringUtil.isEmpty(ret))
			return "/";

		return ret;
	}
	public static InetSocketAddressDAO parseHost(String url)
	{
		return parseHost(url, -1);
	}
	public static InetSocketAddressDAO parseHost(String url, int defaultPort)
	{
		int index = url.indexOf(ProtocolDelimiter.COLON_PATH.getValue());
		int hostStart;
		if (index == -1)
		{
			hostStart = 0;
		}
		else
		{
			hostStart = index + ProtocolDelimiter.COLON_PATH.getValue().getBytes().length;
		}

		// check if there is a user:password@
		index = url.indexOf('@');
		if (index != -1 && url.indexOf('/', hostStart) > index)
		{
			hostStart = index + 1;
		}


		int hostEnd;
		index = url.indexOf('/', hostStart);
		if (index == -1)
		{
			hostEnd = url.length();
		}
		else
		{
			hostEnd = index;
		}

		InetSocketAddressDAO ret = new InetSocketAddressDAO(url.substring(hostStart, hostEnd));

		if (ret.getPort() == -1)
		// detect scheme type
		{
			URIScheme us = URIScheme.match(url);
			if (us == URIScheme.HTTPS || us == URIScheme.WSS)
			{
				ret.setPort(us.getValue());
			}
			else if (us == URIScheme.HTTP || us == URIScheme.WS)
			{
				ret.setPort(us.getValue());
			}
			else
			{
				ret.setPort(defaultPort);
			}
		}

		return ret;
	}


	public static HTTPMessageConfig parseURL(URL fullURL)
	{
		String url = parseHostURL(fullURL);
		String uri = parseURI(fullURL, false);
		HTTPMessageConfig hcc = new HTTPMessageConfig();
		hcc.setURL(url);
		hcc.setURI(uri);
		hcc.setParameters(parseQuery(fullURL));
		hcc.setMethod(HTTPMethod.GET);


		return hcc;
	}


	public static String formatFullURL(HTTPMessageConfig hcc)
			throws UnsupportedEncodingException
	{
		String encodedContentParams = HTTPUtil.formatParameters(hcc.getParameters(), null, hcc.isURLEncodingEnabled(), hcc.getHTTPParameterFormatter());
		String urlURI = SharedStringUtil.concat(hcc.getURL(), hcc.getURI(), "/");

		if (encodedContentParams.length() > 0)
		{
			urlURI += "?" + encodedContentParams;
		}

		return urlURI;
	}


	public static void removeParams(List<GetNameValue<String>> list, List<String> params)
	{
		if ( list != null && params != null)
		{
			for (String param : params)
			{
				GetNameValue<String> gnvs = SharedUtil.lookupNV(list, param);

				if (gnvs != null)
				{
					list.remove( gnvs);
				}

			}
		}
	}


	public static String extractRequestCookie(HTTPResponseData rd)
	{
		List<String> cookies = SharedUtil.lookupMap(rd.getResponseHeaders(), "Set-Cookie", true);

		if (cookies!= null && cookies.size() > 0)
		{
			StringBuilder sb = new StringBuilder();

			for (String cookie : cookies)
			{
				List<HttpCookie> httpCookies = HttpCookie.parse( cookie);

				for (HttpCookie httpCookie : httpCookies )
				{
					if (sb.length() > 0)
					{
						sb.append( "; ");
					}

					sb.append(httpCookie.getName() + (httpCookie.getValue() != null ? "="+httpCookie.getValue() : ""));
					//httpCookie.setVersion(version);
					//sb.append(httpCookie);
				}
			}

			return sb.toString() ;
		}

		return null;
	}

	public static GetNameValue<String> extractHeaderCookie(HTTPResponseData rd)
	{
		List<String> cookies = SharedUtil.lookupMap(rd.getResponseHeaders(), "Set-Cookie", true);

		if (cookies!= null && cookies.size() > 0)
		{
			StringBuilder sb = new StringBuilder();

			for (String cookie : cookies)
			{
				List<HttpCookie> httpCookies = HttpCookie.parse( cookie);

				for (HttpCookie httpCookie : httpCookies)
				{
					if (sb.length() > 0)
					{
						sb.append( "; ");
					}

					sb.append(httpCookie.getName() + (httpCookie.getValue() != null ? "="+httpCookie.getValue() : ""));
					//httpCookie.setVersion(version);
					//sb.append(httpCookie);
				}
			}

			return  new NVPair(HTTPHeaderName.COOKIE, sb.toString());
		}

		return null;
	}

	public static NVPair extractCookie(Map<String, List<String>> rd, int version)
	{
		List<String> cookies = SharedUtil.lookupMap(rd, "Set-Cookie", true);

		if (cookies!= null && cookies.size() > 0)
		{
			StringBuilder sb = new StringBuilder();

			for (String cookie : cookies)
			{
				List<HttpCookie> httpCookies = HttpCookie.parse( cookie);

				for (HttpCookie httpCookie : httpCookies )
				{
					if (sb.length() > 0)
					{
						sb.append( "; ");
					}

					sb.append(httpCookie.getName() + (httpCookie.getValue() != null ? "="+httpCookie.getValue() : ""));
					//httpCookie.setVersion(version);
					//sb.append(httpCookie);
				}
			}

			return new NVPair("Cookie", sb.toString()) ;
		}

		return null;
	}


	public static List<HttpCookie> extractCookies(HTTPResponseData rd)
	{
		List<HttpCookie> ret = new ArrayList<HttpCookie>();
		List<String> cookies = SharedUtil.lookupMap(rd.getResponseHeaders(), "Set-Cookie", true);

		if (cookies!= null && cookies.size() > 0)
		{
			for (String cookie : cookies)
			{
				List<HttpCookie> httpCookies = HttpCookie.parse( cookie);
				ret.addAll(httpCookies);
			}
		}

		return ret;
	}


	public static HttpCookie lookupCookieByName(HTTPResponseData rd, String name)
	{
		if (name != null)
		{
			List<String> cookies = SharedUtil.lookupMap(rd.getResponseHeaders(), "Set-Cookie", true);

			if (cookies!= null && cookies.size() > 0)
			{
				for (String cookie : cookies)
				{
					List<HttpCookie> httpCookies = HttpCookie.parse( cookie);
					for (HttpCookie httpCookie : httpCookies)
					{
						if (name.equalsIgnoreCase(httpCookie.getName()))
							return httpCookie;
					}

				}
			}
		}
		return null;
	}


	public static List<HTTPMessageConfigInterface> extractFormsContent(HTTPResponseData rd, int cookieVersion) throws UnsupportedEncodingException, MalformedURLException
	{
		List<HTTPMessageConfigInterface> retList = new ArrayList<HTTPMessageConfigInterface>();
		Document doc = 	Jsoup.parse( new String(rd.getData(), SharedStringUtil.UTF_8), "", Parser.xmlParser());

		Elements elements = doc.select("form");

		for (Element element : elements)
		{
			String formAction = element.attr("action");
			String formMethod = element.attr("method");
			String formEncoding = element.attr("enctype");
			String formName = element.attr("name");
			//String formID = element.attr("id");
			HTTPMimeType encoding = HTTPMimeType.lookup(formEncoding);
			if ( encoding == null)
			{
				encoding = HTTPMimeType.APPLICATION_WWW_URL_ENC;
			}



			Elements formInputs = element.getAllElements();
			//System.out.println(SharedUtil.toCanonicalID('#', formName, formID, formAction, formMethod, formEncoding));
			//System.out.println("Inputs:" + formInputs);


			// extract cookie if it exists
			String reqCookie = extractRequestCookie(rd);

			//ArrayList<GetNameValue<String>> params = new ArrayList<GetNameValue<String>>();
			HTTPMessageConfig ret = new HTTPMessageConfig();

			for (Element ele : formInputs)
			{

				String name = ele.attr("name");
				String value = ele.attr("value");
				if (!SharedStringUtil.isEmpty(name))
				{
					//System.out.println( name + ":" + value);
					switch( encoding)
					{

					case MULTIPART_FORM_DATA:
						ret.getParameters().add(new HTTPMultiPartParameter(name, value));
						break;
					case TEXT_PLAIN:
					case APPLICATION_WWW_URL_ENC:
						ret.getParameters().add(new NVPair(name, value));
						break;
					default:
						break;

					}
				}
			}

			if (encoding == HTTPMimeType.MULTIPART_FORM_DATA)
			{
				ret.setContentType(HTTPMimeType.MULTIPART_FORM_DATA);
				//ret.setMultiPartEncoding( true);
			}
			//ret.setParameters(params);
			ret.setName(formName);
			formAction = URLDecoder.decode(formAction,  SharedStringUtil.UTF_8);
			try
			{
				URL url = new URL(formAction);
				ret.setURL(HTTPUtil.parseHostURL(url));
				ret.setURI(HTTPUtil.parseURI(url, true));
			}
			catch(IOException e)
			{
				ret.setURI(formAction);
			}

			ret.setMethod(formMethod);


			if (reqCookie != null)
				ret.setCookie(reqCookie);

			retList.add(ret);

		}

		return retList;
	}

	public static MessageStatus checkRequestStatus(HTTPMessageConfigInterface hmci)
	{
		if (hmci != null)
		{
			if (hmci.getMethod() != null)
			{
				//int tempContent = hcc.getContentLength();
				//byte content[] = hcc.getContent();
				if (hmci.getContent() != null && hmci.getContent().length == hmci.getContentLength())
				{
					return MessageStatus.COMPLETE;
				}

				if (hmci.getContentLength() < 1 && (hmci.getContent() == null || hmci.getContent().length == 0))
				{
					return MessageStatus.COMPLETE;
				}

//				if (hmci.getContentLength() >= 0 && hmci.getContent() != null && hmci.getContent().length != hmci.getContentLength())
//				{
//					return MessageStatus.PARTIAL;
//				}

				if (hmci.getContentLength() > 0 && (hmci.getContent() == null || (hmci.getContent().length != hmci.getContentLength())))
				{
					return MessageStatus.PARTIAL;
				}
			}
		}

		return MessageStatus.INVALID;
	}


	public static boolean isContentComplete(HTTPMessageConfigInterface hcc)
	{
		int contentLength = hcc.getContentLength();
		byte content[] = hcc.getContent();


		if (content != null && content.length == contentLength)
		{
			return true;
		}

		if (contentLength < 1 && (content == null || content.length == 0))
		{
			return true;
		}

		return false;
	}

	public static HTTPMessageConfigInterface parseRawHTTPRequest(UByteArrayOutputStream ubaos, HTTPMessageConfigInterface hmci, boolean headerOnly)
		throws NullPointerException, IllegalArgumentException
	{
		// locate the end of headers
		int endOfHeadersIndex = ubaos.indexOf(ProtocolDelimiter.CRLFCRLF.getBytes());

		if (endOfHeadersIndex > 0 && (hmci == null || hmci.getHeaders().size() == 0))
		{
			// find the termination
			int lineCounter = 0;
			for(int i=0; i < endOfHeadersIndex;)
			{
				int endOfCurrentLine = ubaos.indexOf(i, ProtocolDelimiter.CRLF.getBytes(), 0, ProtocolDelimiter.CRLF.getBytes().length);

				if (endOfCurrentLine != -1 )
				{
					lineCounter++;
					String oneLine = new String(Arrays.copyOfRange(ubaos.getInternalBuffer(), i, endOfCurrentLine));

					if (lineCounter == 1)
					{
						HTTPRequestLine rl = new  HTTPRequestLine(oneLine);

						if (rl.getHTTPMethod() == null)
						{
							throw new IllegalArgumentException("Missing method");
						}

						if (rl.getHTTPVersion() == null)
						{
							throw new IllegalArgumentException("Missing http version");
						}

						if (hmci == null)
						{
							hmci = new HTTPMessageConfig();
						}

						hmci.setHTTPVersion(rl.getHTTPVersion());
						hmci.setMethod(rl.getHTTPMethod());
						hmci.setURI(rl.getURI());
					}
					else
					{
						hmci.getHeaders().add(SharedUtil.toNVPair(oneLine, ":", true));
					}
				}

				i=endOfCurrentLine+ProtocolDelimiter.CRLF.getBytes().length;
			}
		}

		if (!headerOnly && hmci != null && endOfHeadersIndex !=-1)
		{
			byte[] content = Arrays.copyOfRange(ubaos.getInternalBuffer(), endOfHeadersIndex + ProtocolDelimiter.CRLFCRLF.getBytes().length , ubaos.size());

			if (content != null && content.length > 0)
			{
				hmci.setContent(content);
			}
		}

		return hmci;
	}

}
