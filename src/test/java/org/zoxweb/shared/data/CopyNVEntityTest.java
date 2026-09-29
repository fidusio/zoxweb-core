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

import org.zoxweb.shared.data.DataConst.PhoneType;
import org.zoxweb.shared.data.FileInfo.FileType;

import org.zoxweb.shared.util.NVEntity;

public class CopyNVEntityTest {

	public static void main(String[] args) {
		
		PhoneDAO phoneDAO = new PhoneDAO();
		phoneDAO.setReferenceID("12345");
		phoneDAO.setSubjectGUID("20000");
		phoneDAO.setName("My Phone");
		phoneDAO.setDescription("My mobile number.");
		phoneDAO.setNumber("5551234");
		phoneDAO.setAreaCode("310");
		phoneDAO.setCountryCode("+1");
		phoneDAO.setPhoneType(PhoneType.MOBILE.name());
		
		NVEntity ret = SharedDataUtil.copyNVEntity(ZWDataFactory.SINGLETON, phoneDAO, true, false, false);
		
		System.out.println("NVEntity to copy: " + phoneDAO);
		System.out.println("Deep: " + true);
		System.out.println("Omit Ref ID: " + false);
		System.out.println("Omit User ID: " + false);
		System.out.println("Copied NVEntity: " + ret);
		System.out.println("Equal? " + ret.toString().equals(phoneDAO.toString()));
		
		ret = SharedDataUtil.copyNVEntity(ZWDataFactory.SINGLETON, phoneDAO, true, true, true);
		
		System.out.println();
		System.out.println("NVEntity to copy: " + phoneDAO);
		System.out.println("Deep: " + true);
		System.out.println("Omit Ref ID: " + true);
		System.out.println("Omit User ID: " + true);
		System.out.println("Copied NVEntity: " + ret);
		
		
		
		FolderInfo folderInfo = new FolderInfo();
		folderInfo.setName("My Folder");
		folderInfo.setDescription("Personal folder.");
		folderInfo.setCreationTime(System.currentTimeMillis());
		folderInfo.setReferenceID("00000");
		folderInfo.setSubjectGUID("20000");
		
		FileInfo fileInfo1 = new FileInfo();
		fileInfo1.setName("File 1");
		fileInfo1.setDescription("My file 1.");
		fileInfo1.setFileType(FileType.FILE);
		fileInfo1.setReferenceID("11111");
		fileInfo1.setSubjectGUID("20000");
		
		folderInfo.getFolderContent().add(fileInfo1);
		
		FileInfo fileInfo2 = new FileInfo();
		fileInfo2.setName("File 2");
		fileInfo2.setDescription("My file 2.");
		fileInfo2.setFileType(FileType.FILE);
		fileInfo2.setReferenceID("22222");
		fileInfo2.setSubjectGUID("20000");
		FileInfo remoteFileInfo = new FileInfo();
		remoteFileInfo.setReferenceID("99999");
		remoteFileInfo.setDescription("Remote file.");
		fileInfo2.setRemoteFileInfo(remoteFileInfo);
		
		folderInfo.getFolderContent().add(fileInfo2);
		
		ret = SharedDataUtil.copyNVEntity(ZWDataFactory.SINGLETON, folderInfo, true, false, false);
		
		System.out.println();
		System.out.println("NVEntity to copy: " + folderInfo);
		System.out.println("Deep: " + true);
		System.out.println("Omit Ref ID: " + false);
		System.out.println("Omit User ID: " + false);
		System.out.println("Copied NVEntity: " + ret);
		System.out.println("Equal? " + ret.toString().equals(folderInfo.toString()) + "\n");
		
		ret = SharedDataUtil.copyNVEntity(ZWDataFactory.SINGLETON, folderInfo, false, true, true);
		
		System.out.println();
		System.out.println("NVEntity to copy: " + folderInfo);
		System.out.println("Deep: " + false);
		System.out.println("Omit Ref ID: " + true);
		System.out.println("Omit User ID: " + true);
		System.out.println("Copied NVEntity: " + ret);
	}
	
}