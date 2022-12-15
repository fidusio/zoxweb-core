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
package org.zoxweb.server.http.proxy;

@SuppressWarnings("serial")
public class OnURLAction 
	implements java.io.Serializable
{

	private String customErrorText = null, desc = null, newLocation = null;
	private boolean log, block, customText, http_rq, anotherLocation;

	public OnURLAction(String desc)
	{
		this.desc = desc;
	}

	public void denyAccess(String customErrorText)
	{
		this.block = true;
		this.customText = true;
		this.customErrorText = customErrorText;
	}

	public void denyAccess()
	{
		block = true;
	}

	public void logAccess()
	{
		log = true;
	}

	public void anotherLocation(String newlocation)
	{
		this.anotherLocation = true;
		this.newLocation = newlocation;
	}

	public boolean onAccessDeny()
	{
		return block;
	}

	public boolean onAccessLog()
	{
		return log;
	}

	public boolean onAccessDenyWithCustomText()
	{
		return customText;
	}

	public boolean onAccessSendHTTPRequest()
	{
		return http_rq;
	}

	public boolean onAccessRedirect()
	{
		return this.anotherLocation;
	}

	public String newLocation()
	{
		return this.newLocation;
	}

	public void setHTTPAction(boolean http_rq, String httppath)
	{
		this.http_rq = http_rq;
		// this.httppath=httppath;
	}

	public String getCustomErrorText()
	{
		return customErrorText;

	}

	public String getDescription()
	{
		return desc;
	}

	public String toString()
	{
		return desc;
	}

}
