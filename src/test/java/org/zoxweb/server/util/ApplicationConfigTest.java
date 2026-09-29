/*
 * Copyright (c) 2012-2014 ZoxWeb.com LLC.
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

import org.zoxweb.shared.data.ApplicationConfigInfo;

public class ApplicationConfigTest {

	public static void main(String[] args) {

		try {
			System.out.println("Env Var:" + ApplicationConfigManager.getDefaultApplicationEnvVar());
			ApplicationConfigInfo acd = ApplicationConfigManager.SINGLETON.loadDefault();
			System.out.println("Config:" + ApplicationConfigManager.SINGLETON.loadDefault());
			System.out.println("Config:" + ApplicationConfigManager.SINGLETON.concatAsDirName(acd, ApplicationConfigInfo.ApplicationDefaultParam.CONF_DIR.getName()));
			System.out.println("Config file:" + ApplicationConfigManager.SINGLETON.locateFile(acd, "ApplicationConf.json"));
			System.out.println("Cache dir:" + ApplicationConfigManager.SINGLETON.locateFile(acd, "cache_dir"));
			System.out.println("Config file:" + ApplicationConfigManager.SINGLETON.readConfigurationContent(null, "mongod_conf"));
			
			System.out.println("is_secure:" + acd.isSecureEnabled());
		} catch( Exception e) {
			e.printStackTrace();
		}
	}

}