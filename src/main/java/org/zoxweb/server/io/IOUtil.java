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
package org.zoxweb.server.io;

import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.*;
import java.net.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IOUtil 
{
	
	//public static final SimpleDateFormat SDF = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");

	private IOUtil(){}
	
	/**
	 * Close an AutoCloseable object if c is null the action is discarded, while closing catch any exception silently
	 * @param acs auto closable array
	 */
	public static void close(AutoCloseable ...acs)
	{
		if (acs != null)
		{
			for (AutoCloseable c : acs)
			{
				try
				{
					c.close();
				}
				catch (Exception e)
				{

				}
			}
		}
	}


	public static String resourceToString(String resourceID) throws IOException {
		return resourceToString(IOUtil.class, resourceID);
	}

	public static String resourceToString(Class<?> clazz, String resourceID) throws IOException {
		return inputStreamToString(clazz.getResourceAsStream(resourceID), true);
	}

	public static File findFile(String filename)
	{
		filename = SharedStringUtil.trimOrNull(filename);
		SUS.checkIfNulls("Filename can't be null.", filename);

		return findFile(new File(filename));
	}

	public static File findFile(File file)
	{
		SUS.checkIfNulls("File can't be null.", file);
		return (file.exists() && file.isFile()) ? file : null;
	}
	
	
	public static RandomAccessFile endOfFile(RandomAccessFile br)
        throws IOException
	{
		br.seek(br.length());
		return br;
	}

	/**
	 * 
	 * @param c closable
	 * @return IOException or null if none generated
	 */
	public static IOException close(Closeable c)
	{
		if (c != null)
		{
			try
			{
				c.close();
			}
			catch(IOException e)
			{
				return e;
			}
		}
		
		return null;
	}

	/**
	 * This method will read all the response part of the url connection
	 * @param url to be read
	 * @return ByteArrayOutputStream.
	 * @throws IOException in case of an io exception
	 */
	public static ByteArrayOutputStream readAllURLResponse(URL url)
		throws IOException
	{
		URLConnection con = url.openConnection();
		con.setDoInput(true);
		con.setDoOutput(true);
		
		con.setUseCaches(false);
		con.connect();
		return inputStreamToByteArray(con.getInputStream(), true); 
	}
	
	public static File locateFile(String filename)
	{
		File ret = findFile(filename);
		if (ret != null)
			return ret;
		return locateFile(ClassLoader.getSystemClassLoader(), filename);
	}
	
	
	
	public static File locateFile(ClassLoader cl, String filename)
	{
		File ret = new File(filename);
		if (!ret.exists() || !ret.isFile())
		{
			ret = new File(cl.getResource(filename)
					.getFile());
		}
		
		return ret;
	}

	/**
	 * Delete a directory recursively
	 * @param path to be deleted
	 * @throws IOException in case of an error encountered
	 */
	public static void deleteDirectoryRecursively(Path path)
			throws IOException
	{
		if (Files.isDirectory(path))
		{
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path))
			{
				for (Path entry : entries)
				{
					deleteDirectoryRecursively(entry);
				}
			}
		}

		Files.delete(path);
	}

	public static List<URL> listMatches(Path rootDir, String filterPattern, String filterExclusion)
			throws IOException
	{

		List<URL> ret = new ArrayList<>();

		Predicate<Path> composition = null;
		Predicate<Path> pattern = p -> p.toString().matches(filterPattern);
		if(SUS.isEmpty(filterExclusion))
		{
			Predicate<Path> exclusion = p -> p.toString().matches(filterExclusion);
			composition = pattern.and(exclusion.negate());
		}
		else
		{
			composition = pattern;
		}

		try (Stream<Path> paths = Files.walk(rootDir))
		{
			paths.filter(Files::isRegularFile)
					.filter(composition)
					.forEach(p ->
					{
						try
						{
							ret.add(p.toUri().toURL());
						}
						catch (MalformedURLException e)
						{
							e.printStackTrace();
						}
					});
		}

		return ret;
	}


	/**
     *
     * @param is
     * @param charsetEncoding
     * @param close
     * @return
     * @throws IOException
     */
	public static String inputStreamToString(InputStream is, String charsetEncoding, boolean close)
        throws IOException
	{
		UByteArrayOutputStream baos = (UByteArrayOutputStream) inputStreamToByteArray(is, close);
		return new String( baos.getInternalBuffer(), 0 , baos.size(),charsetEncoding );
	}

	public static long countInputStreamBytes(InputStream is, boolean close)
			throws IOException
	{
		byte[] buffer = new byte[4096];
		int read;
		long ret = 0;

		try
		{
			while ((read = is.read(buffer, 0, buffer.length)) != -1)
			{
				ret += read;
			}

		}
		finally
		{
			if (close)
			{
				close(is);
			}
		}


		return ret;

	}


    /**
     * @param filename
     * @return
     * @throws IOException
     */
	public static String inputStreamToString(String filename) 
		throws IOException
	{
		return inputStreamToString(new FileInputStream(filename), true);
	}
	
	/**
	 * Load file 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String inputStreamToString(File file) 
			throws IOException
	{
		return inputStreamToString(new FileInputStream(file), true);
	}

    /**
     *
     * @param is
     * @param close
     * @return
     * @throws IOException
     */
	public static String inputStreamToString(InputStream is, boolean close) 
        throws IOException
	{
		byte[] buffer = new byte[4096];
		StringBuilder sb = new StringBuilder();
		int read;

		while ((read = is.read(buffer, 0, buffer.length)) > 0)
		{
			sb.append(new String(buffer, 0, read, SharedStringUtil.UTF_8));
		}
		
		if ( close)
		{
			close(is);
		}

		return sb.toString();
	}
	
	public static String readerToString(Reader is, boolean close) 
	        throws IOException
		{
			char[] buffer = new char[4096];
			StringBuilder sb = new StringBuilder();
			int read;

			while ((read = is.read(buffer, 0, buffer.length)) > 0)
			{
				sb.append(buffer, 0, read);
			}
			
			if ( close)
			{
				close(is);
			}

			return sb.toString();
		}
	
	
	/**
	 * Read the all the content of an input stream and return it as a byte array
	 * @param is to be read
	 * @param close if true is will closed after reading
	 * @return byte array
	 * @throws IOException
	 */
	public static UByteArrayOutputStream inputStreamToByteArray(InputStream is, boolean close)
        throws IOException
	{
		UByteArrayOutputStream baos = new UByteArrayOutputStream();

		try
		{
			int read;
			byte buffer [] = new byte[4096];
			while((read = is.read( buffer)) != -1)
			{
				baos.write( buffer, 0, read);
			}
		}
		finally
		{
			if (close)
			{
				close(is);
			}
		}
		
		return baos;
	}
	
	
	/**
	 * Read the all the content of an input stream and return it as a byte array
	 * @param filename to be read
	 * @param close if true is will closed after reading
	 * @return byte array
	 * @throws IOException
	 */
	public static UByteArrayOutputStream inputStreamToByteArray(String filename, boolean close)
        throws IOException
	{
		return inputStreamToByteArray(new File(filename), close);
	}
	/**
	 * Read the all the content of an input stream and return it as a byte array
	 * @param file to be read
	 * @param close if true is will closed after reading
	 * @return byte array
	 * @throws IOException
	 */
	public static UByteArrayOutputStream inputStreamToByteArray(File file, boolean close)
        throws IOException
	{
		InputStream is = null;
		try
		{
			is = new FileInputStream(file);
			return inputStreamToByteArray(is, close);
		}
		finally 
		{
			IOUtil.close(is);
		}
	}


	/**
	 * Write a string to file
	 * @param file
	 * @param toWrite
	 * @throws IOException
	 */
	public static void writeToFile(File file, String toWrite)
			throws IOException
	{
		writeToFile(file, SharedStringUtil.getBytes(toWrite));
	}

    /**
     *
     * @param file
     * @param buffer
     * @throws IOException
     */
	public static void writeToFile(File file, byte buffer[])
        throws IOException
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(file);
			fos.write(buffer);
		}
		finally
		{
			IOUtil.close(fos);
		}
				
	}
	
	/**
	 * Return the file creation in millis
	 * @param file
	 * @return file creation time in millis
	 * @throws IOException
	 */
	public static long fileCreationTime(File file)
        throws IOException
	{
		if (!file.exists())
		{
			throw new FileNotFoundException();
		}

		return fileCreationTime(file.toPath());	
	}
	
	/**
	 * Return the file creation in millis
	 * @param path
	 * @return file creation time in millis
	 * @throws IOException
	 */
	public static long fileCreationTime(Path path)
        throws IOException
	{
		BasicFileAttributes attributes =  Files.readAttributes(path, BasicFileAttributes.class);
		return attributes.creationTime().to(TimeUnit.MILLISECONDS);
	}

    /**
     *
     * @param dirName
     * @return
     */
	public static File createDirectory(String dirName)
	{
		File dir = new File( dirName);

		if (!dir.exists())
		{
			dir.mkdirs();
		}

		return dir;
	}

    /**
     *
     * @param is
     * @param os
     * @param closeBoth
     * @return
     * @throws IOException
     */
	public static long relayStreams(InputStream is, OutputStream os, boolean closeBoth)
	    throws IOException
	{
		return relayStreams(is, os, closeBoth, closeBoth);
	}

    /**
     *
     * @param is
     * @param os
     * @param closeIS
     * @param closeOS
     * @return
     * @throws IOException
     */
	public static long relayStreams(InputStream is, OutputStream os, boolean closeIS, boolean closeOS)
	    throws IOException
	{
		long totalCopied = 0;

		try
		{	
			int read;
			byte buffer [] = new byte[4096];
			while((read = is.read( buffer)) != -1)
			{
				os.write(buffer, 0, read);
				totalCopied+=read;
			}
			os.flush();
		}
		finally
		{
			if (closeIS || is instanceof CloseEnabledInputStream)
			{
				close(is);
			}
			
			if (closeOS || os instanceof CloseEnabledOutputStream)
			{
				close(os);
			}
		}

		return totalCopied;
	}
	
	/**
	 * This utility method will check if the output stream != null then invoke flush 
	 * @param os output stream 
	 * @throws IOException if os.flush() throw an exception
	 */
	public static void flush(OutputStream os)
		throws IOException
	{
		if (os != null)
		{
			os.flush();
		}
	}

	public static List<byte[]> parse(UByteArrayOutputStream uboas, byte[] delim, boolean noEmpty)
	{
		if (uboas == null || delim == null)
		{
			throw new NullPointerException("uboas and/or delimiter cannot be null.");
		}

		if (delim.length == 0)
		{
			throw new IllegalArgumentException("Empty delimiter is not accepted.");
		}

		int match = 0;
		ArrayList<byte[]> matchedTokens = new ArrayList<byte[]>();
		int index = 0;
		synchronized (uboas)
		{
			while ((match = uboas.indexOf(index, delim)) != -1)
			{
				byte[] message = Arrays.copyOfRange(uboas.getInternalBuffer(), index, match);
				//uboas.removeAt(0, match + delim.length);

				if(!noEmpty || message.length > 0)
					matchedTokens.add(message);
				index = match+delim.length;
			}
			if (index > 0)
			{
				uboas.removeAt(0, index);
			}
		}

		return matchedTokens;
	}


	public static Proxy.Type toProxyType(ProxyType pt)
	{
        switch (pt) {
            case DIRECT:
				return Proxy.Type.DIRECT;
            case HTTP:
				return Proxy.Type.HTTP;
            case SOCKS:
				return Proxy.Type.SOCKS;
        }

		return null;
	}


	public static Proxy toProxy(IPAddress proxyInfo)
	{
		return new Proxy(toProxyType(proxyInfo.getProxyType()), new InetSocketAddress(proxyInfo.getInetAddress(), proxyInfo.getPort()));
	}

	public static boolean isFileInDirectory(String directoryPathStr, String filePathStr)
	{
		try
		{
			Path filePath = Paths.get(filePathStr).toRealPath();
			Path directoryPath = Paths.get(directoryPathStr).toRealPath();

			return filePath.startsWith(directoryPath);
		} catch (IOException e) {
		}
		return false;
	}

	public static boolean isFileInDirectory(File directory, File file) {
		try {
			String fileCanonicalPath = file.getCanonicalPath();
			String directoryCanonicalPath = directory.getCanonicalPath();

			return fileCanonicalPath.startsWith(directoryCanonicalPath + File.separator);
		} catch (IOException e) {
		}

		return false;
	}
}
