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
package org.zoxweb;

import java.util.ArrayList;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.db.QueryMatchLong;
import org.zoxweb.shared.db.QueryRequest;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.Const.RelationalOperator;

import com.google.gson.Gson;

public class JSONQuery {

	private static String test = "{\"canonical_id\":\"org.zoxweb.shared.accounting.FinancialTransactionDAO\", \"batch_size\":250, \"query\":[{\"creation_ts\":1435647600354, \"relational_operator\":\"GT\"},{\"logical_operator\":\"AND\"},{\"creation_ts\":1435710917354, \"relational_operator\":\"LT\"}]}";

	public static void main(String[] args) {
		Gson gson = new Gson();
		
		QueryRequest qr = new QueryRequest();
		qr.setCanonicalID("org.zoxweb.shared.accounting.FinancialTransactionDAO");
		qr.setBatchSize(250);

		ArrayList<QueryMarker> query = new ArrayList<QueryMarker>();
		QueryMatchLong qm1 = new QueryMatchLong(RelationalOperator.GT, 1435647600354l, TimeStampDAO.Param.CREATION_TS);
		query.add(qm1);
		query.add(LogicalOperator.AND);
		QueryMatchLong qm2 = new QueryMatchLong(RelationalOperator.LT, 1435710917354l, TimeStampDAO.Param.CREATION_TS);
		query.add(qm2);
		qr.setQuery(query);
		
		System.out.println(test);
		System.out.println(gson.toJson(qr));
		System.out.println(Long.class.toString());
		
		try {
			QueryRequest qrFJ = GSONUtil.fromQueryRequest(test);
			System.out.println(gson.toJson(qrFJ));
		} catch(Exception e) {
			
		}
	}
}
