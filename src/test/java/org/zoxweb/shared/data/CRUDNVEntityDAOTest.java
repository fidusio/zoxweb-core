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

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.CRUD;

public class CRUDNVEntityDAOTest {
	
	public static void main(String[] args) {

		CRUDNVEntityDAO crudNVE = new CRUDNVEntityDAO();
		FolderInfoDAO folder = new FolderInfoDAO();
		folder.setName("My Folder");
		//folder.setStateOrProvince("CA");

		FileInfoDAO nve = new FileInfoDAO();
		nve.setName("File");
		nve.setReferenceID("bata");
		folder.getFolderContent().add(nve);
		
		CRUD crud = CRUD.UPDATE;
		
		crudNVE.setCRUD(crud);
		crudNVE.setNVEntity(folder);
		
		System.out.println("CRUD: " + crudNVE.getCRUD());
		System.out.println("NVEntity: " + crudNVE.getNVEntity());
		
		try {
			String json = GSONUtil.toJSON(crudNVE, true, true, true);
			System.out.println("JSON: " + json);
			crudNVE = GSONUtil.fromJSON(json, CRUDNVEntityDAO.class);
			System.out.println("CRUD NVE: " + crudNVE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
