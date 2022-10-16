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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.data.RuntimeResultDAO;
import org.zoxweb.shared.data.VMInfoDAO;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.Const.JavaClassVersion;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.data.RuntimeResultDAO.ResultAttribute;

public class RuntimeUtil
{

	private RuntimeUtil()
    {

	}

	/**
	 * Get the output of the command as string. IT will block until the
	 * executing process closes the stream.
	 * @param p 
	 * @param ra 
	 * 
	 * @return the output of the command. If there is no output, "" is returned.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static String getRuntimeResponse(Process p, ResultAttribute ra ) 
        throws IOException, InterruptedException
    {

		if (p == null)
		{
            return "";
        }

		InputStream is = null;

		switch(ra)
		{
		case OUTPUT:
			is = p.getInputStream();
			break;
		case ERROR:
			is = p.getErrorStream();
			break;
		case EXIT_CODE:
			p.waitFor();
			return ""+p.exitValue();
		}

		p.waitFor();
		
		return IOUtil.inputStreamToString(is, false);
	}

	/**
	 * This will execute a system command till it finishes.
	 * 
	 * @param command to be executed.
	 * @param params command parameters.
	 * @return The execution result the process exit code and the output stream
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static RuntimeResultDAO runAndFinish(String command, String ...params)
        throws InterruptedException, IOException
    {
	  if (params.length > 0)
	  {
	    String parameters = SharedUtil.toCanonicalID(' ', (Object[])params);
	    if(!SharedStringUtil.isEmpty(parameters))
	    {
	      command = command + " " + parameters; 
	    }
	  } 
      return runAndFinish(command, ResultAttribute.OUTPUT);
    }

	/**
	 * This will execute a system command till it finishes.
	 * 
	 * @param command
	 *            to be executed.
	 * @param ra 
	 * @return The execution result the process exit code and the output stream
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static RuntimeResultDAO runAndFinish(String command, ResultAttribute ra)
        throws InterruptedException, IOException
    {
		Process p = Runtime.getRuntime().exec(command);
		String ret = getRuntimeResponse(p, ra);
	
		return new RuntimeResultDAO(p.exitValue(), ret);
	}


	/**
	 * This method will create an executable file scripts based on the command and f on the file system 
	 * and then execute it and command line.
	 * @param command
	 * @param f
	 * @return RuntimeResultDAO
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static RuntimeResultDAO runAndFinish(String command, File f)
        throws InterruptedException, IOException
    {
		f.createNewFile();
		f.setExecutable(true);
		IOUtil.writeToFile(f, command.getBytes());
		return runAndFinish(f.getCanonicalPath());
	}


	public static VMInfoDAO vmSnapshot()
	{
		return vmSnapshot(null);
	}
    public static VMInfoDAO vmSnapshot(Const.SizeInBytes sib)
    {
		Runtime rt = Runtime.getRuntime();
		VMInfoDAO ret = new VMInfoDAO();
		if(sib == null)
			sib = Const.SizeInBytes.B;
		ret.setName("VMSnapshot");
		
		ret.setCoreCount(rt.availableProcessors());
		ret.setMaxMemory(sib.convertBytes(rt.maxMemory()));
		ret.setFreeMemory(sib.convertBytes(rt.freeMemory()));
		ret.setUsedMemory(sib.convertBytes(rt.totalMemory() - rt.freeMemory()));
		ret.setTotalMemory(sib.convertBytes(rt.totalMemory()));
		ret.setTimeStamp(new Date());
		return ret;
	}

	public static JavaClassVersion checkClassVersion(String filename)
        throws IOException
    {
		FileInputStream fis = null;

		try
        {
        	File file = new File(filename);
        	if (!file.exists())
        	{
        		file = new File(filename + ".class");
        	}

        	if (!file.exists())
        	{
        		throw new FileNotFoundException("File:" + filename);
        	}

        	fis = new FileInputStream(file);

        	return checkClassVersion(fis);
        }
        finally
        {
        	IOUtil.close(fis);
        }
    }

	public static JavaClassVersion checkClassVersion(InputStream fis)
        throws IOException
    {
	    DataInputStream in = null;

	    try
        {
	        in = new DataInputStream(fis);
	        int magic = in.readInt();

	        if (magic != 0xcafebabe)
	        {
	          throw new IOException("Invalid class!");
	        }

	        int minor = in.readUnsignedShort();
	        int major = in.readUnsignedShort();
	        return JavaClassVersion.lookup(major, minor);
	    }
	    finally
        {
	    	IOUtil.close(fis);
	    	IOUtil.close(in);
	   
        }
	}
	/**
	 * Loads a native shared library. It tries the standard System.loadLibrary
	 * method first and if it fails, it looks for the library in the current
	 * class path. It will handle libraries packed within jar files, too.
	 *
	 * @param name name of the library to load
	 * @param classLoader to be used
	 * @throws IOException if the library cannot be extracted from a jar file
	 * into a temporary file
	 */
	public static void loadSharedLibrary(String name, ClassLoader classLoader) 
			throws IOException
	{
		try
		{
			System.loadLibrary(name);
		}
		catch (UnsatisfiedLinkError e)
		{
			String filename = System.mapLibraryName(name);
			int pos = filename.lastIndexOf('.');
			File file = File.createTempFile(filename.substring(0, pos), filename.substring(pos));
			file.deleteOnExit();
			IOUtil.relayStreams(classLoader.getResourceAsStream(filename), new FileOutputStream(file), true);	
			System.load(file.getAbsolutePath());
		}
	}

	public static void main(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            try
            {
                System.out.println(args[i] + ":" + checkClassVersion(args[i]));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
	 }
	
	
	public static String stackTrace()
	{
	  StackTraceElement[] elements = Thread.currentThread().getStackTrace();
	  StringBuilder sb = new StringBuilder();
	  for (int i = 2; i < elements.length; i++) {
	    StackTraceElement s = elements[i];
	    sb.append("\tat " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")\n");
	  }

	  return sb.toString();
	}


	/**
	 * This method return the java major version ex: 11.0.5 will return 11
	 * @return java vm major version
	 */
	public static int getJavaVersion()
	{
		String javaVersion = System.getProperty("java.version");
		Const.JavaClassVersion version = Const.JavaClassVersion.lookup(javaVersion);
		if (version.MAJOR > Const.JavaClassVersion.VER_1_4.MAJOR)
			return Integer.parseInt(version.VERSION);

		return 0;

	}


}