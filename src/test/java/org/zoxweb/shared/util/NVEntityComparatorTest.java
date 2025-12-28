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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.zoxweb.shared.accounting.Currency;
import org.zoxweb.shared.accounting.FinancialTransactionDAO;
import org.zoxweb.shared.accounting.AmountDAO;
import org.zoxweb.shared.data.CreditCardDAO;
import org.zoxweb.shared.data.FormInfoDAO;
import org.zoxweb.shared.util.NVEntityComparator;

public class NVEntityComparatorTest {
	
	private static void testFormInfoDAO() {

		List<FormInfoDAO> list = new ArrayList<FormInfoDAO>();
		
		FormInfoDAO formInfo1 = new FormInfoDAO();
		CreditCardDAO creditCard1 = new CreditCardDAO();
		creditCard1.setName("Visa");
		formInfo1.setFormReference(creditCard1);	
		list.add(formInfo1);
		
		FormInfoDAO formInfo2 = new FormInfoDAO();
		CreditCardDAO creditCard2= new CreditCardDAO();
		creditCard2.setName("AMEX");
		formInfo2.setFormReference(creditCard2);	
		list.add(formInfo2);
		
		FormInfoDAO formInfo3 = new FormInfoDAO();
		CreditCardDAO creditCard3 = new CreditCardDAO();
		creditCard3.setName("Master Card");
		formInfo3.setFormReference(creditCard3);	
		list.add(formInfo3);
		
		System.out.println("UNSORTED LIST");

		for (FormInfoDAO doc : list) {
			System.out.println("Document: " + doc);
		}
		
		Collections.sort(list, new NVEntityComparator("form_reference.name", FormInfoDAO.NVC_FORM_INFO_DAO));
		
		System.out.println("SORTED LIST");

		for (FormInfoDAO doc : list) {
			System.out.println("Document: " + doc);
		}
	}
	
	private static void testFinancialTransactionDAO() {
		List<FinancialTransactionDAO> list = new ArrayList<FinancialTransactionDAO>();
		
		FinancialTransactionDAO transaction1 = new FinancialTransactionDAO();
		transaction1.setAmount(new AmountDAO(new BigDecimal("100.00"), Currency.USD));
		list.add(transaction1);
		
		FinancialTransactionDAO transaction2 = new FinancialTransactionDAO();
		transaction2.setAmount(new AmountDAO(new BigDecimal("10.00"), Currency.USD));
		list.add(transaction2);
		
		FinancialTransactionDAO transaction3 = new FinancialTransactionDAO();
		transaction3.setAmount(new AmountDAO(new BigDecimal("1000.00"), Currency.USD));
		list.add(transaction3);
		
		System.out.println("UNSORTED LIST");

		for (FinancialTransactionDAO trans : list) {
			System.out.println("Transaction: " + trans);
		}
		
		Collections.sort(list, new NVEntityComparator("transaction_amount.money_value", FinancialTransactionDAO.NVC_FINANCIAL_TRANSACTION_DAO));
		
		System.out.println("SORTED LIST");

		for (FinancialTransactionDAO trans : list) {
			System.out.println("Transaction: " + trans);
		}
	}
	
	public static void main(String[] args) {
		testFormInfoDAO();
		testFinancialTransactionDAO();		
	}
	
	
}