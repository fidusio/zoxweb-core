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
package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.data.DataDAO;
import org.zoxweb.shared.filters.MatchPatternFilter;
import org.zoxweb.shared.util.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarTool {
	
	private JarTool() {
		
	}
	
	public static URL findClassJarURL(Class<?> entryPointClass)
    {
		return entryPointClass.getProtectionDomain().getCodeSource().getLocation();
	}

	public static Map<String, URL> findFilesInJarAndExpandAsTempFiles(String jarFilename, boolean deleteOnExit, String... searchCriteria)
        throws IOException
    {
		HashMap<String, URL>  jarURLs = new HashMap<String, URL>();
		MatchPatternFilter mpf = MatchPatternFilter.createMatchFilter(searchCriteria);
		File jarFile = new File(jarFilename);
		JarFile jf  = null;

		try
        {
			jf = new JarFile( jarFile);
			Enumeration < JarEntry> eje = jf.entries();
			jarURLs.put( jarFilename, jarFile.toURI().toURL());
			FileOutputStream fos = null;
			
			while(eje.hasMoreElements())
            {
				JarEntry je = eje.nextElement();
			
				// look up for embedded jar files
				
				if (mpf.match(je.getName()))
				{
					//System.out.println("jar found:" + je.getName());
					try
                    {
						String fileExt = SharedStringUtil.valueAfterRightToken(je.getName(), ".");
						if (fileExt.equals(je.getName()))
						{
							fileExt = "";
						}

						int length = je.getName().length() - fileExt.length();
						// replace / with _
						File tempFile = File.createTempFile(je.getName().substring(0, length), fileExt);

						if (deleteOnExit)
						{
							tempFile.deleteOnExit();
						}
						
						fos = new FileOutputStream(tempFile);
						
						IOUtil.relayStreams(jf.getInputStream( je), fos, true);

						jarURLs.put(je.getName(), tempFile.toURI().toURL());
					}
					catch( Exception e)
                    {
						e.printStackTrace();
					}
					finally
                    {
						IOUtil.close(fos);
					}
				}
			}
		}
		finally
        {
			IOUtil.close(jf);
		}
		
		return jarURLs;
	}

	/**
     * Loads library from current JAR archive
     * 
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     * 
     * @param path The filename inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters (restriction).
     */
    public static void loadLibraryFromJar(String path)
        throws IOException
    {
        if (!path.startsWith("/"))
        {
            throw new IllegalArgumentException("The path to be absolute (start with '/').");
        }
 
        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
 
        // Split filename to prexif and suffix (extension)
        String prefix = "";
        String suffix = null;

        if (filename != null)
        {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "."+parts[parts.length - 1] : null; // Thanks, davs! :-)
        }
 
        // Check if the filename is okay
        if (filename == null || prefix.length() < 3)
        {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }
 
        // Prepare temporary file
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();
 
        if (!temp.exists())
        {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }
 
        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;
 
        // Open and check input stream
        InputStream is = JarTool.class.getResourceAsStream(path);
        if (is == null)
        {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }
 
        // Open output stream and copy data between source file in JAR and the temporary file
        OutputStream os = new FileOutputStream(temp);
        try
        {
            while ((readBytes = is.read(buffer)) != -1)
            {
                os.write(buffer, 0, readBytes);
            }
        }
        finally
        {
            // If read/write fails, close streams safely before throwing an exception
            os.close();
            is.close();
        }
 
        // Finally, load the library
        System.load(temp.getAbsolutePath());
    }

	public static ClassLoader createClassLoader( HashMap<String, URL> urls, ClassLoader parent)
    {
		URL[] all = urls.entrySet().toArray( new URL[0]);

		if (parent == null)
		{
			return new URLClassLoader( all);
		}
		else
        {
			return new URLClassLoader( all, parent);
		}
	}




	public static String[] zipList(String zipFilename, String ...patterns)
			throws IOException
	{
		return zipList(new File(zipFilename), patterns);
	}

	public static String[] zipList(File zipFile, String ...patterns)
			throws IOException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(zipFile);
			return zipList(fis, patterns);
		}
		finally
		{
			IOUtil.close(fis);
		}
	}





	public static String[] zipList(InputStream zipStream, String ...patterns)
			throws IOException
	{
		List<String> ret = new ArrayList<>();
		try(ZipInputStream zipIs = zipStream instanceof ZipInputStream ? (ZipInputStream) zipStream : new ZipInputStream(zipStream)) {
			ZipEntry ze = null;
			MatchPatternFilter mpf = patterns != null && patterns.length > 0 && patterns[0]  != null ? MatchPatternFilter.createMatchFilter(patterns) : null;

			while ((ze = zipIs.getNextEntry()) != null)
			{
				if (!ze.isDirectory()) {

					if (mpf != null)
					{
						if (!mpf.match(ze.getName()))
							continue; // no match found
					}
					ret.add(ze.getName());
				}
			}
		}
		return ret.toArray(new String[0]);
	}

	public static DataDAO[] loadContents(String zipFilename, String ...patterns)
			throws IOException
	{
		return loadContents(new File(zipFilename), patterns);
	}

	public static DataDAO[] loadContents(File zipFile, String ...patterns)
			throws IOException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(zipFile);
			return loadContents(fis, patterns);
		}
		finally
		{
			IOUtil.close(fis);
		}
	}



	public static DataDAO[] loadContents(InputStream zipStream, String ...patterns)
			throws IOException
	{
		List<DataDAO> ret = new ArrayList<>();
		try(ZipInputStream zipIs = zipStream instanceof ZipInputStream ? (ZipInputStream) zipStream : new ZipInputStream(zipStream)) {
			ZipEntry ze = null;
			MatchPatternFilter mpf = patterns != null && patterns.length > 0 && patterns[0]  != null ? MatchPatternFilter.createMatchFilter(patterns) : null;
			UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while ((ze = zipIs.getNextEntry()) != null)
			{
				if (!ze.isDirectory()) {

					if (mpf != null)
					{
						if (!mpf.match(ze.getName()))
							continue; // no match found
					}

					ubaos.reset();
					int read;
					while((read = zipIs.read(buffer)) > 0)
					{
						ubaos.write(buffer, 0 , read);
					}
					DataDAO toAdd = new DataDAO();

					toAdd.setFullName(ze.getName());
					toAdd.setName(SharedStringUtil.valueAfterRightToken(ze.getName(), "/"));
					toAdd.setDescription(ze.getComment());
					toAdd.setData(ubaos.toByteArray());


					ret.add(toAdd);
				}
			}
		}
		return ret.toArray(new DataDAO[0]);
	}


	public static void main(String ...args)
	{
		try
		{
			ParamUtil.ParamMap paramMap = ParamUtil.parse("=", args);

			System.out.println(paramMap);
			System.out.println("files: " + Arrays.toString(paramMap.namelessValues()));
			String pattern = paramMap.stringValue("pattern", null, true);



			RateCounter rc = new RateCounter("ZipExtraction");
			long ts = System.currentTimeMillis();

			for (String zipFile : paramMap.namelessValues())
			{


				GetNameValue<String> load = paramMap.asNVPair("load");
				if (load != null)
				{
					DataDAO[] matches = loadContents(zipFile, pattern);
					System.out.println("Zip File: " + zipFile + " matches found: " + matches.length);

					for (DataDAO matched : matches) {
						rc.inc();
						System.out.println("\t\t" + SharedUtil.toCanonicalID(':', matched.getName(), matched.getFullName(), matched.getData().length));
						//System.out.println(SharedStringUtil.toString(matched.getData()));
					}
				}
				else
				{
					String[] matches = zipList(zipFile, pattern);
					System.out.println("Zip File: " + zipFile + " matches found: " + matches.length);

					for (String matched : matches) {
						rc.inc();
						System.out.println("\t\t" + matched);
					}
				}
			}

			rc.registerTimeStamp(ts);
			System.out.println(rc + " " + Const.TimeInMillis.toString(rc.getDeltas()));



		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static byte[] gzip(String str)
			throws NullPointerException, IllegalArgumentException, IOException
	{
		return gzip(SharedStringUtil.getBytes(str));
	}

	public static byte[] gzip(byte[] content)
			throws NullPointerException, IllegalArgumentException, IOException
	{
		SharedUtil.checkIfNulls("Can't zip null content", content);

		if (content.length == 0)
		{
			throw new IllegalArgumentException("Content is empty");
		}

		ByteArrayOutputStream output = null;
		GZIPOutputStream gzipOutputStream = null;

		try
		{
			output = new ByteArrayOutputStream(content.length);
			gzipOutputStream = new GZIPOutputStream(output);
			gzipOutputStream.write(content);
			gzipOutputStream.flush();
			gzipOutputStream.finish();
			return output.toByteArray();
		}
		finally
		{
			IOUtil.close(gzipOutputStream);
		}
	}

	public static byte[] gunzip(byte[] content)
			throws NullPointerException, IllegalArgumentException, IOException
	{
		SharedUtil.checkIfNulls("Can't zip null content", content);

		if (content.length == 0)
		{
			throw new IllegalArgumentException("Content is empty");
		}

		GZIPInputStream gzipInputStream = null;

		try
		{
			return IOUtil.inputStreamToByteArray(new GZIPInputStream(new ByteArrayInputStream(content)), false).toByteArray();
		}
		finally
		{
			IOUtil.close(gzipInputStream);
		}
	}

}