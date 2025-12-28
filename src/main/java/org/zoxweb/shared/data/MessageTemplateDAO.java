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
package org.zoxweb.shared.data;

import java.util.List;


import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedUtil;

/**
 * 
 * @author mzebib
 */
@SuppressWarnings("serial")
public class MessageTemplateDAO 
	extends SimpleDocumentDAO
{
	
	public enum Param
		implements GetNVConfig
	{
		BODY_CONTENT(NVConfigManager.createNVConfig("body_content", "Body content", "BodyContent", true, true, String.class)),
		BODY_TAGS(NVConfigManager.createNVConfig("body_tags", "Body tags", "BodyTags", true, true, true, String[].class, null)),
		TITLE(NVConfigManager.createNVConfig("title", "Message title", "Title", true, true, String.class)),
		PRE_TOKEN_TAG(NVConfigManager.createNVConfig("pre_token_tag", "The pre token tag.", "PreTokenTag", false, true, String.class)),
		POST_TOKEN_TAG(NVConfigManager.createNVConfig("post_token_tag", "The post token tag.", "PostTokenTag", false, true, String.class)),
				
		;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
	}
	
	public static final NVConfigEntity NVC_MESSAGE_TEMPLATE_DAO = new NVConfigEntityPortable(
            "message_template_dao",
            null,
            "MessageTemplateDAO",
            true,
            false,
            false,
            false,
            MessageTemplateDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SimpleDocumentDAO.NVC_SIMPLE_DOCUMENT_DAO
    );
																					
	
	/**
	 * The default constructor.
	 */
	public MessageTemplateDAO()
	{
		super(NVC_MESSAGE_TEMPLATE_DAO);
	}
	
	/**
	 * Returns the message body content.
	 * @return body content
	 */
	public String getBodyContent() 
	{
		if (getContent() == null)
        {
            return lookupValue(Param.BODY_CONTENT);
        }
		
		return getContent();
	}
	
	/**
	 * Sets the message body content.
	 * @param content
	 */
	public void setBodyContent(String content)
	{
		setContent(content);
		//setValue(Params.BODY_CONTENT, content);
	}	
	
	/**
	 * Returns the message body tags.
	 * @return tags
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getBodyTags()
	{		
		return (ArrayValues<NVPair>) lookup(Param.BODY_TAGS);
	}
	
	/**
	 * Sets the message body tags.
	 * @param tags
	 */
	@SuppressWarnings("unchecked")
	public void setBodyTags(ArrayValues<NVPair> tags)
	{
		ArrayValues<NVPair> content = (ArrayValues<NVPair>) lookup(Param.BODY_TAGS);
		content.add(tags.values(), true);
	}	
	
	/**
	 * Sets the message body tags.
	 * @param tags
	 */
	@SuppressWarnings("unchecked")
	public void setBodyTags(List<NVPair> tags)
	{
		ArrayValues<NVPair> content = (ArrayValues<NVPair>) lookup(Param.BODY_TAGS);
		content.add(tags.toArray(new NVPair[0]), true);
	}
	
	/**
	 * Returns the message title.
	 * @return title
	 */
	public String getTitle()
	{
		return lookupValue(Param.TITLE);
	}

	/**
	 * Sets the message title.
	 * @param title
	 */
	public void setTitle(String title)
	{
		setValue(Param.TITLE, title);
	}
	
	/**
	 * Returns the pre-token tag.
	 * @return pretag
	 */
	public String getPreTag() 
	{
		return  lookupValue(Param.PRE_TOKEN_TAG);
	}
	
	/**
	 * Sets the pre-token tag.
	 * @param preTokenTag
	 */
	public void setPreTag(String preTokenTag) 
	{
		setValue(Param.PRE_TOKEN_TAG, preTokenTag);
	}
	
	/**
	 * Returns the post-token tag.
	 * @return post tag
	 */
	public String getPostTag() 
	{
		return  lookupValue(Param.POST_TOKEN_TAG);
	}

	/**
	 * Sets the post-token tag.
	 * @param postTokenTag
	 */
	public void setPostTag(String postTokenTag)
	{
		setValue(Param.POST_TOKEN_TAG, postTokenTag);
	}
	
}