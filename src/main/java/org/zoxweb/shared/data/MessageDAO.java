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

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 *
 */
@SuppressWarnings("serial")
public class MessageDAO
    extends TimeStampDAO
{
		// sender id in this case user id
		// timestamp
		// message id ie refid
		// content as NVEntity
	
	public enum Param
		implements GetNVConfig
	{
		CONTENT(NVConfigManager.createNVConfigEntity("content", "Content", "Content", true, true, NVEntity.class, ArrayType.NOT_ARRAY)),
		
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
	
	public static final NVConfigEntity NVC_MESSAGE_DAO = new NVConfigEntityPortable(
            "message_dao",
            null,
            "MessageDAO",
            true,
            false,
            false,
            false,
            DataContentDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );
	
	
	/**
	 * The default constructor.
	 */
	public MessageDAO() 
	{
		super(NVC_MESSAGE_DAO);
	}
	
	
	/**
	 * Returns the content.
	 * @return content
	 */
	public NVEntity getContent()
	{
		return lookupValue(Param.CONTENT);
	}
	
	/**
	 * Sets the content.
	 * @param content
	 */
	public void setContent(NVEntity content)
	{
		setValue(Param.CONTENT, content);
	}
	
}