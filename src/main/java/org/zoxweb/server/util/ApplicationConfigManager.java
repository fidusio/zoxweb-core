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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.logging.Logger;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.data.ApplicationConfigDAO;
import org.zoxweb.shared.data.ApplicationConfigDAO.ApplicationDefaultParam;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.SharedUtil;

public class ApplicationConfigManager
{

	private static final transient Logger log = Logger.getLogger(Const.LOGGER_NAME);
	public static final ApplicationConfigManager SINGLETON = new ApplicationConfigManager();
	public static final GetValue<?>[] DEFAULT_DIR_NAMES = { ApplicationDefaultParam.CONF_DIR,
															ApplicationDefaultParam.CACHE_DIR,
															ApplicationDefaultParam.DATA_DIR,
															ApplicationDefaultParam.PUBLIC_DIR,
															ApplicationDefaultParam.SSL_DIR,
															ApplicationDefaultParam.TEMP_DIR,
															ApplicationDefaultParam.VAR_DIR
	};
	
	private ApplicationConfigManager()
    {
		
	}

	private ApplicationConfigDAO defaultACD = null;
	
	private File defaultFile = null;
	private long defaultFileLastAccess = 0;

	public synchronized ApplicationConfigDAO loadDefault()
        throws NullPointerException, IOException
    {
		return load();
	}

	public static String getDefaultApplicationEnvVar()
    {
		String envURLLocation = System.getenv(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);

		if (envURLLocation == null)
		{
            envURLLocation = System.getProperty(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);
        }

		return envURLLocation;
	}

	public static String getDefaultApplicationEnvVar(String defaultValue)
    {
		String envURLLocation = System.getenv(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);

		if (envURLLocation == null)
		{
            envURLLocation = System.getProperty(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);
        }

		if (envURLLocation == null)
		{
			System.getProperties().put(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR, defaultValue);
			return defaultValue;
		}

		return envURLLocation;
	}

	private ApplicationConfigDAO load()
        throws NullPointerException, IOException
    {
		if (defaultFile == null)
		{
            defaultFile = new File(SharedUtil.toCanonicalID('/', getDefaultApplicationEnvVar(), ApplicationDefaultParam.CONF_DIR.getValue()), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);
        }

		if (!defaultFile.exists())
		{
			System.out.println(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR + "=" + getDefaultApplicationEnvVar());
			log.info( defaultFile + " not found");
			throw new FileNotFoundException(defaultFile.getName());
		}
		
		if (defaultFile.lastModified() != defaultFileLastAccess)
		{
			System.out.println(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR + "=" + getDefaultApplicationEnvVar());
			String jsonString = IOUtil.inputStreamToString( defaultFile.toURI().toURL().openStream(), true);
			defaultACD =  GSONUtil.create(true).fromJson(jsonString, ApplicationConfigDAO.class);
			defaultFileLastAccess = defaultFile.lastModified();
		}

		return defaultACD;
	}

	public  ApplicationConfigDAO load(InputStream is)
        throws NullPointerException, IOException
    {
		String jsonString = IOUtil.inputStreamToString( is, true);
		ApplicationConfigDAO ret = GSONUtil.create(true).fromJson(jsonString, ApplicationConfigDAO.class);	
		return ret;
	}

	public String concatAsDirName(ApplicationConfigDAO acd, String varName)
    {
		String base = getDefaultApplicationEnvVar();
		String varValue = acd.lookupValue(varName);
		
		if (varValue != null)
		{
			return SharedUtil.toCanonicalID('/', base, varValue) +"/";
		}
		
		return base;
	}
	
	public String concatWithEnvVar(String... vars)
    {
		String base = getDefaultApplicationEnvVar();

		if (vars != null)
		{
			String values[] = new String[vars.length+1];
			int index = 0;
			values[index++] = base;

			for (String s: vars)
			{
				values[index++] = s;
			}

			return SharedUtil.toCanonicalID('/', (Object[])values);
		}
		
		return base;
	}
	
	public void save(ApplicationConfigDAO acd)
        throws NullPointerException, IOException
    {
		//File file = new File(concatAsDirName( acd, acd.lookupValue(ApplicationDefaultParam.CONF_DIR)), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);
		File file = new File(SharedUtil.toCanonicalID('/', getDefaultApplicationEnvVar(), ApplicationDefaultParam.CONF_DIR.getValue()), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);	
		String jsonString =  GSONUtil.create(true).toJson(acd);
		
		FileOutputStream fos = null;

		try
        {
			fos = new FileOutputStream( file);
			fos.write( jsonString.getBytes());
		}
		finally
        {
			IOUtil.close( fos);
		}	
	}

	public File locateFile(ApplicationConfigDAO acd, String varName)
        throws NullPointerException, IOException
    {
		
		if (acd == null)
		{
			acd = loadDefault();
		}
		
		String varValue = acd.lookupValue(varName);

		if (varValue == null)
		{
			varValue = varName;
		}
		
		
		// varValue is an absolute file
		File file = new File(varValue);
		if (file.exists())
		{
            return file;
        }

		// varValue is a file in conf dir
		file = new File(concatWithEnvVar(), varValue);

		if (file.exists())
		{
            return file;
        }
		
		for (GetValue<?> dir: DEFAULT_DIR_NAMES)
		{
			file = new File( concatWithEnvVar((String)dir.getValue(),varValue));

			if (file.exists())
			{
                return file;
            }
		}
		
		return null;
	}

	public String readConfigurationContent(ApplicationConfigDAO acd, String varName)
        throws NullPointerException, IOException
    {
		File configFile = locateFile(acd, varName);

		if (configFile != null && configFile.isFile())
		{
			InputStream is = null;

			try
            {
				is = new FileInputStream(configFile);
				return IOUtil.inputStreamToString( is, true);
			}
			finally
            {
				// this close is required just in case we encouter IO error
				// to prevent unclosed file descriptors
				IOUtil.close(is);
			}
		}

		return null;
	}
	
	public void save(ApplicationConfigDAO scd, OutputStream os)
        throws NullPointerException, IOException
    {
		String jsonString = GSONUtil.create(true).toJson(scd);

		try
        {
			os.write( jsonString.getBytes());
		}
		finally
        {
			IOUtil.close( os);
		}
	}

}