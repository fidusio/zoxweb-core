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

import org.zoxweb.shared.data.MessageTemplateDAO;
import org.zoxweb.shared.util.NVPair;

public class MessageTemplateDAOTest {
	
	public static void main(String[] args) {
		MessageTemplateDAO template = new MessageTemplateDAO();
		template.setTitle("Test");
		template.setBodyContent("Hello, this is John Smith.");
		template.getBodyTags().add(new NVPair("Alpha", "1"));
		template.getBodyTags().add(new NVPair("Beta", "2"));
		template.getBodyTags().add(new NVPair("Delta", "3"));
		
		System.out.println("Template: " + template);		
	}

}
