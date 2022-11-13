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
package org.zoxweb.shared.util;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ParseGroupTest 
{

	@Test
	public void tokes() {
		String str = "Dear $$personal_title$$ $$first_name$$ $$last_name$$, {{bracket,{sub-bracket}}}";

		List<CharSequence> listOfCharSeq = SharedStringUtil.parseGroup(str, "$$", "$$", false);
		
		System.out.println(listOfCharSeq);
		
		listOfCharSeq = SharedStringUtil.parseGroup(str, "{", "}", false);
		
		System.out.println(listOfCharSeq);

		listOfCharSeq = SharedStringUtil.parseGroup(str, "$", "$", false);
		
		System.out.println(listOfCharSeq);
	}
	
}