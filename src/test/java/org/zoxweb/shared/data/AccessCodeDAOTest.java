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

import org.zoxweb.shared.security.AccessCodeDAO;

/**
 * @author mnael
 *
 */
public class AccessCodeDAOTest {

	public static void main(String[] args) {
		AccessCodeDAO acd = new AccessCodeDAO();
		
		acd.setAccessCode("Password");
		
		String toValidates[] ={
				null,
				"marwan",
				"password",
				"Password",
				"nael",
				"",
		};
		
		for (int i=0; i < toValidates.length; i++) {
			try {
				System.out.println(toValidates[i] + ":" + acd.validateAccessCode(toValidates[i]));
			} catch(Exception e) {
				System.out.println(toValidates[i] + ":Error:" + e );
			}
		}
		
		acd.setAccessQuota(5);

		for (int j=0; j<4; j ++)
		for (int i=0; i < toValidates.length; i++) {
			try {
				System.out.println(toValidates[i] + ":" + acd.validateAccessCode(toValidates[i]));
			} catch(Exception e) {
				System.out.println(toValidates[i] + ":Error:" + e );
			}
		}

		acd.setAccessCode(null);

		for (int i=0; i < toValidates.length; i++) {
			try {
				System.out.println(toValidates[i] + ":" + acd.validateAccessCode(toValidates[i]));
			} catch(Exception e) {
				System.out.println(toValidates[i] + ":Error:" + e );
			}
		}
		
	}
}
