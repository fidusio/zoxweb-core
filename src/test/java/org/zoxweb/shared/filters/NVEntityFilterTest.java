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
package org.zoxweb.shared.filters;


import org.zoxweb.shared.data.FileInfo;
import org.zoxweb.shared.data.FolderInfo;
import org.zoxweb.shared.data.FormInfo;
import org.zoxweb.shared.data.PhoneDAO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NVEntityFilterTest {

	@Test
	public void test1() {
		NVEntityFilter filter = new NVEntityFilter(FormInfo.NVC_FORM_INFO);

		FolderInfo folderInfo = new FolderInfo();
        boolean valid = filter.isValid(folderInfo);
        assertFalse(valid);

		FileInfo fileInfo = new FileInfo();
        valid = filter.isValid(fileInfo);
        assertFalse(valid);

		FormInfo formInfo = new FormInfo();
        valid = filter.isValid(formInfo);
        assertTrue(valid);
	}
	
	@Test
	public void test2() {
		NVEntityFilter filter = new NVEntityFilter(FolderInfo.NVC_FOLDER_INFO, FileInfo.NVC_FILE_INFO, FormInfo.NVC_FORM_INFO);

		FolderInfo folderInfo = new FolderInfo();
        boolean valid = filter.isValid(folderInfo);
        assertTrue(valid);

		FileInfo fileInfo = new FileInfo();
        valid = filter.isValid(fileInfo);
        assertTrue(valid);

		FormInfo formInfo = new FormInfo();
        valid = filter.isValid(formInfo);
        assertTrue(valid);
	}
	
	@Test
	public void test3() {
		NVEntityFilter filter = new NVEntityFilter();

		FolderInfo folderInfo = new FolderInfo();
        boolean valid = filter.isValid(folderInfo);
        assertFalse(valid);

		FileInfo fileInfo = new FileInfo();
        valid = filter.isValid(fileInfo);
        assertFalse(valid);

		FormInfo formInfo = new FormInfo();
        valid = filter.isValid(formInfo);
        assertFalse(valid);
	}
	
	@Test
	public void test4() {
		NVEntityFilter filter = new NVEntityFilter(PhoneDAO.NVC_PHONE_DAO);

		FolderInfo folderInfo = new FolderInfo();
        boolean valid = filter.isValid(folderInfo);
        assertFalse(valid);

		FileInfo fileInfo = new FileInfo();
        valid = filter.isValid(fileInfo);
        assertFalse(valid);

		FormInfo formInfo = new FormInfo();
        valid = filter.isValid(formInfo);
        assertFalse(valid);

		PhoneDAO phone = new PhoneDAO();
        valid = filter.isValid(phone);
        assertTrue(valid);
	}
	
}