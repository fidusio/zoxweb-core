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
package org.zoxweb.server.security;

import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.util.SharedStringUtil;

public class HashTest {

	public static void main(String[] args) {
		try {
			String alorigthm = null;
			String seqs[] = null;
			int index = 0;

			for (int i = 0; i < args.length; i++) {
				if (i == 0) {
					alorigthm = args[i];
				} else {
					if (seqs == null) {
						seqs = new String[args.length - 1];
					}

					seqs[index++] = args[i];
				}
			}

			byte hash[] = HashUtil.hashSequence(alorigthm, seqs);
			System.out.println(SharedStringUtil.bytesToHex(hash).toLowerCase());
			
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
}