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
package org.zoxweb.server.http.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.zoxweb.server.http.HTTPRequestAttributes;
import org.zoxweb.server.io.FileInfoStreamSource;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.ApplicationConfigManager;
import org.zoxweb.shared.data.ApplicationConfigDAO;
import org.zoxweb.shared.data.FileInfoDAO;
import org.zoxweb.shared.filters.FilenameFilter;
import org.zoxweb.shared.http.HTTPStatusCode;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SharedUtil;



/**
 * 
 * @author mnael
 *
 */

@SuppressWarnings("serial")
public class HTTPDownloadServlet
	extends HttpServlet
	implements HTTPDownloadHandler
{
	private static final transient Logger log = Logger.getLogger(HTTPDownloadServlet.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException,
				   IOException
	{
		doPost( req, resp);
	}
		
	
	public boolean isSecure()
	{
		return false;
	}
	
	
	@Override
	public FileInfoStreamSource lookupFileInfoSource(HttpServletRequest request, HTTPRequestAttributes attributes)
			throws AccessException, IOException
	{
		GetNameValue<String> gnv = SharedUtil.lookupNV(attributes.getParameters(), "filename");
		
		
		if ( gnv != null)
		{
			ApplicationConfigDAO acd = ApplicationConfigManager.SINGLETON.loadDefault();
			// example "/var/web/public/files"
			File publicDir = new File(acd.lookupValue("public_dir"));
			if ( !publicDir.isDirectory())
			{
				log.info("Public Directory not found:" + publicDir);
				throw new FileNotFoundException("Public Dir NOT FOUND");
			}
			
			File file = new File(publicDir, FilenameFilter.SINGLETON.validate(gnv.getValue()));
			if ( !file.exists() || file.isDirectory())
			{
				log.info("File Not Found:" + gnv.getValue() + ":" + file);
				throw new FileNotFoundException("File not found " + gnv.getValue());
			}
			
			InputStream is = file.toURI().toURL().openStream();
			FileInfoDAO fid = new FileInfoDAO();
			fid.setName( gnv.getValue());
			return new FileInfoStreamSource(fid, is);
		}
		
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updatedFileInfoDAO(FileInfoDAO fid)
	{
		// TODO Auto-generated method stub

	}
	
	
	
	public void authenticate(HttpServletRequest request, HttpServletResponse response) throws AccessException
	{
		
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException,
				   IOException
	{
		if ( isSecure() && !req.isSecure())
		{
			// must allow secure communication only
			resp.sendError( HTTPStatusCode.FORBIDDEN.CODE, "HTTPS request required.");
			return;
		}
		
		try
		{

			authenticate(req, resp);
			
			HTTPRequestAttributes attributes = HTTPServletUtil.extractRequestAttributes( req);
			
			
			
			FileInfoStreamSource fih = null;
				
			fih = lookupFileInfoSource( req, attributes);
			
		
		
		
			if ( fih != null && fih.getFileInfoDAO() != null  && fih.getSourceInputStream() != null)
			{
				//byte content [] = ServerJPA.SINGLETON.getFileInfoData(fi);
				resp.setContentType("application/octet-stream");
				resp.setHeader("Content-Disposition", "Attachment;Filename=\""+ fih.getFileInfoDAO().getName()+ "\"" );
				//resp.getOutputStream().write( content);
				try 
				{
					IOUtil.relayStreams( fih.getSourceInputStream(), resp.getOutputStream(), true, false);
					fih.getFileInfoDAO().setLastTimeRead(System.currentTimeMillis());
					try
					{
						updatedFileInfoDAO(fih.getFileInfoDAO());
					}
					catch( Exception e)
					{
						// the implementing class generated an error
						// will will just log it
						e.printStackTrace();
					}
				}
				catch (Exception e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					resp.sendError( HTTPStatusCode.INTERNAL_SERVER_ERROR.CODE);
				}
			}
			else
			{
				resp.sendError( HTTPStatusCode.BAD_REQUEST.CODE);
			}
		}
		catch ( AccessException e)
		{
			if ( e.getURLRedirect() == null)
				resp.sendError( HTTPStatusCode.UNAUTHORIZED.CODE, e.getMessage());
			else
				resp.encodeRedirectURL( e.getURLRedirect());
		}
		catch ( Exception e)
		{
			resp.sendError( HTTPStatusCode.INTERNAL_SERVER_ERROR.CODE);
		}
		
	}
}
