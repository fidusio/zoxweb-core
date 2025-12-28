/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.api.APINotificationMessage;

/**
 * The message content filter class validates message content.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class MessageContentFilter
    implements ValueFilter<APINotificationMessage, String>
{

	/**
	 * Initializes delimiter constant.
	 */
	public static final String DEFAULT_DELIMITER = "$$";
	
	/**
	 * Declares that only one instance of this class can be created.
	 */
	public static final MessageContentFilter SINGLETON = new MessageContentFilter();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private MessageContentFilter()
    {
		
	}
	
	/**
	 * Returns a string representation of the class.
	 */
	@Override
	public String toCanonicalID()
    {
		return null;
	}

	/**
	 * Validates the notification message.
	 * @param in
	 * @return validated 
	 */
	@Override
	public String validate(APINotificationMessage in)
        throws NullPointerException, IllegalArgumentException
    {

		String bodyContent = in.getBodyTemplate();

		if (!SUS.isEmpty(bodyContent))
		{
			for (NVPair nvp : in.getBodyTags().values())
			{
				String preTag = in.getPreTag() != null ? in.getPreTag() : DEFAULT_DELIMITER;
				String postTag = in.getPostTag() != null ? in.getPostTag() : DEFAULT_DELIMITER;

				if (nvp.getValue() != null)
				{
					bodyContent = bodyContent.replace(preTag + nvp.getName() + postTag, nvp.getValue());
				}
			}
		}
		
		return bodyContent;
	}

	/**
	 * Checks if the notification message is valid.
	 * @param in
	 * @return true if valid
	 */
	@Override
	public boolean isValid(APINotificationMessage in)
    {
		return in != null;
	}

}