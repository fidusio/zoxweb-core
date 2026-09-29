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

/**
 *
 */
public class FolderInfoTest {

	public static void main(String[] args) {
		try {
			FolderInfo fid = new FolderInfo();
			SetNameDescriptionDAO nve = new FolderInfo();
			nve.setName("Folder");
			fid.getFolderContent().add(nve);
			nve = new FileInfo();
			nve.setName("File");
			fid.getFolderContent().add(nve);
			
			
			System.out.println( "" + fid);
			System.out.println( "" + fid.getFolderContent());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}