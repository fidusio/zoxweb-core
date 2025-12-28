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
package org.zoxweb.shared.util;

import java.util.ArrayList;
import java.util.List;

import org.zoxweb.shared.accounting.FinancialTransactionDAO;
import org.zoxweb.shared.util.NVCEMap;
import org.zoxweb.shared.util.NVCMap;
import org.zoxweb.shared.util.NVEntity;

public class NVCEMapTest {

	public static void main(String[] args) {
		List<NVCMap> nvcMapList = new ArrayList<NVCMap>();
		NVCMap nvcDate = new NVCMap(FinancialTransactionDAO.Params.CREATION_TS.getNVConfig(), "Date");
		nvcDate.setSeparator(", ");
		nvcDate.setReadOnly(true);
		nvcMapList.add(nvcDate);
		NVCMap nvcDescription = new NVCMap(FinancialTransactionDAO.Params.TRANSACTION_DESCRIPTOR.getNVConfig(), "Description");
		nvcDescription.setSeparator(", ");
		nvcDescription.setReadOnly(true);
		nvcMapList.add(nvcDescription);
		NVCMap nvcType = new NVCMap(FinancialTransactionDAO.Params.TRANSACTION_TYPE.getNVConfig(), "Type");
		nvcType.setSeparator(", ");
		nvcType.setReadOnly(true);
		nvcMapList.add(nvcType);
		NVCMap nvcAmount = new NVCMap(FinancialTransactionDAO.Params.TRANSACTION_AMOUNT.getNVConfig(), "Amount");
		nvcAmount.setSeparator(", ");
		nvcAmount.setReadOnly(true);
		nvcMapList.add(nvcAmount);
		
		NVCEMap nvceMap = new NVCEMap(FinancialTransactionDAO.NVC_FINANCIAL_TRANSACTION_DAO, nvcMapList);
		
		for (NVEntity map : nvceMap.getNVCMapList().values()) {
			System.out.println("NVC map: " + map);
		}
		
		System.out.println(nvcAmount.valueToString(new FinancialTransactionDAO()));
	}

}
