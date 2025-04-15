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
package org.zoxweb;

import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPResponseData;
import org.zoxweb.shared.util.Const;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

public class FormParserTest {

	public static void main(String[] args) {
		try {
			String str = IOUtil.inputStreamToString(new FileInputStream("/temp/test.html"), true);
			HTTPResponseData rd = new HTTPResponseData(200, new HashMap<String, List<String>>(), str.getBytes(Const.UTF_8), 0);

			for (HTTPMessageConfigInterface hcci : HTTPUtil.extractFormsContent(rd, 0)) {
                System.out.println(hcci);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}