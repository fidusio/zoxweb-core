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
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class was reintroduced for backward compatibility reasons.
 * IT SHOULD NEVER BE USED and might be deleted in future releases.

 * @author mnael
 *
 */
@SuppressWarnings("serial")
@Deprecated
public class HTTPCallConfig
    extends HTTPMessageConfig
{
	
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on AddressDAO.
	 */
	public static final NVConfigEntity NVC_HTTP_CALL_CONFIG = new NVConfigEntityLocal("http_call_config",
																					  null ,
																					  "HTTPCallConfig",
																					  true,
																					  false,
																					  false,
																					  false,
																					  HTTPCallConfig.class,
																					  SharedUtil.extractNVConfigs(Params.values()), null, false, HTTPMessageConfig.NVC_HTTP_MESSAGE_CONFIG);
	@Deprecated
	public HTTPCallConfig()
	{
		super(NVC_HTTP_CALL_CONFIG);
	}
}
