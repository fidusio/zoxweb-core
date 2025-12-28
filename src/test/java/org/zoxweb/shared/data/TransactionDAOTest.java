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

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.accounting.Currency;
import org.zoxweb.shared.accounting.FinancialTransactionDAO;
import org.zoxweb.shared.accounting.AmountDAO;
import org.zoxweb.shared.accounting.TransactionDescriptor;
import org.zoxweb.shared.accounting.TransactionType;

//import static org.junit.Assert.*;

//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;

public class TransactionDAOTest {
	
	private FinancialTransactionDAO transaction;

	@BeforeEach
	public void setUp() throws Exception {
		transaction = new FinancialTransactionDAO(new AmountDAO("100.00"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		transaction = null;
	}
	
	@Test
	public void testGetCurrency() {
		Assertions.assertTrue(Currency.USD.equals(transaction.getAmount().getCurrency()));
	}

	@Test//(expected = NullPointerException.class)
	public void testSetCurrency() {
		Assertions.assertThrows(NullPointerException.class, ()->transaction.getAmount().setCurrency(null));
	}

	
	@Test
	public void testGetTransactionAmount() {
		Assertions.assertEquals(transaction.getAmount(), new BigDecimal("100.00"));
	}
	
	@Test//(expected = NullPointerException.class)
	public void testSetTransactionAmount() {
		Assertions.assertThrows(NullPointerException.class, ()->transaction.setAmount(null));
	}

	@Test
	public void testGetTransactionType() {
		Assertions.assertTrue(TransactionType.CREDIT.equals(transaction.getType()));
	}

	@Test
	public void testSetTransactionType() {
		transaction.setType(TransactionType.CREDIT);
	}

	
	//@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testGetTransactionDescriptor() {
		Assertions.assertFalse(TransactionDescriptor.MONTHLY_PAYMENT.equals(transaction.getDescriptor()));
	}

	@Test
	public void testSetTransactionDescriptor() {
		transaction.setDescriptor(TransactionDescriptor.MONTHLY_PAYMENT.name());
	}

	


	
//	private TransactionDAO trans = new TransactionDAO("100");
//	
//	
//    @Test
//	public void testTransactionAmount1()
//	{
//    	assertEquals("Amount verfication: ", new BigDecimal("100.00"), trans.getTransactionAmount());
//	}
//	
//	@Test
//	public void testTransactionAmount2()
//	{
//	 	assertFalse("False: ", new BigDecimal("99").equals(trans.getTransactionAmount()));      
//	}
//	
//    @Test
//	public void testTransactionAmount3()
//    {
//    	assertTrue("True: ", new BigDecimal("100.0").equals(trans.getTransactionAmount()));
//	}
//	
//	@Test
//	public void testTransactionAmount4()
//	{
//		TransactionDAO trans1 = new TransactionDAO("100");
//		TransactionDAO trans2 = new TransactionDAO("200");
//	 	assertNotSame("Not the same: ", trans1.getTransactionAmount(), trans2.getTransactionAmount());      
//	}
//	
//	@Test
//	public void testTransactionAmount5()
//	{
//		BigDecimal amount1 = trans.getTransactionAmount();
//		BigDecimal amount2 = trans.getTransactionAmount();
//	 	assertSame("Same: ", amount1, amount2);      
//	}
//	
//	@Test
//	public void testTransactionAmount6()
//	{
//	 	assertNotNull("Transaction amount NOT null: ", trans.getTransactionAmount());      
//	}
//	
//	@Test
//	public void testTransactionAmount7()
//	{
//	 	assertNull("Transaction amount null: ", trans.getTransactionAmount());      
//	}
//	
//	@Test
//	public void testTransactionAmount8()
//	{
//		fail("Not yet implemented!");
//	}	
//	
//	
//	
//	
//	
//    @Test
//	public void testCurrency1()
//	{
//	 	assertEquals("Currency verification: ", Currency.USD, trans.getCurrency());   
//	}
//	
//	@Test
//	public void testCurrency2()
//	{
//	 	assertFalse("False: ", Currency.EUR.equals(trans.getCurrency()));    
//	}
//	
//    @Test
//	public void testCurrency3()
//	{
//    	assertTrue("True: ", Currency.USD.equals(trans.getCurrency()));
//	}
//	
//	@Test
//	public void testCurrency4()
//	{
//		TransactionDAO trans1 = new TransactionDAO("100");
//		TransactionDAO trans2 = new TransactionDAO("200");
//	 	assertNotSame("Not the same: ", trans1.getCurrency(), trans2.getCurrency());      
//	}
//	
//	@Test
//	public void testCurrency5()
//	{
//		Currency currency1 = trans.getCurrency();
//		Currency currency2 = trans.getCurrency();
//	 	assertSame("Same: ", currency1, currency2);
//	}
//	
//	@Test
//	public void testCurrency6()
//	{
//	 	assertNotNull("Currency NOT null: ", trans.getCurrency());      
//	}
//	
//	@Test
//	public void testCurrency7()
//	{
//	 	assertNull("Currency null: ", trans.getCurrency());  
//	}
//	
//	@Test
//	public void testCurrency8()
//	{
//		fail("Not yet implemented!");
//	}	
//	
//	
//	
//	
//	
//    @Test
//	public void testTransactionType1()
//	{
//	 	assertEquals("Transaction type verification: ", TransactionType.CREDIT, trans.getTransactionType()); 
//	}
//	
//	@Test
//	public void testTransactionType2()
//	{
//	 	assertFalse("False: ", TransactionType.DEBIT.equals(trans.getTransactionType())); 
//	}
//	
//    @Test
//	public void testTransactionType3()
//	{
//    	assertTrue("True: ", TransactionType.CREDIT.equals(trans.getTransactionType())); 
//	}
//	
//	@Test
//	public void testTransactionType4()
//	{
//		TransactionDAO trans1 = new TransactionDAO("100");
//		TransactionDAO trans2 = new TransactionDAO("200");
//	 	assertNotSame("Not the same: ", trans1.getTransactionType(), trans2.getTransactionType());      
//	}
//	
//	@Test
//	public void testTransactionType5()
//	{
//		TransactionType type1 = trans.getTransactionType();
//		TransactionType type2 = trans.getTransactionType();
//	 	assertSame("Same: ", type1, type2);
//	}
//	
//	@Test
//	public void testTransactionType6()
//	{
//	 	assertNotNull("Transaction type NOT null: ", trans.getTransactionType());
//	}
//	
//	@Test
//	public void testTransactionType7()
//	{
//	 	assertNull("Transaction type null: ", trans.getTransactionType());
//	}
//	
//	@Test
//	public void testTransactionType8()
//	{
//		fail("Not yet implemented!");
//	}	
//
//	
//	
//	
//	
//    @Test
//	public void testTransactionDescriptor1()
//	{
//	 	assertEquals("Transaction descriptor verification: ", TransactionDescriptor.MONTHLY_PAYMENT, trans.getTransactionDescriptor()); 
//	}
//	
//	@Test
//	public void testTransactionDescriptor2()
//	{
//	 	assertFalse("False: ", TransactionDescriptor.MONTHLY_PAYMENT.equals(trans.getTransactionDescriptor())); 
//	}
//	
//    @Test
//	public void testTransactionDescriptor3()
//	{
//    	assertTrue("True: ", TransactionDescriptor.MONTHLY_PAYMENT.equals(trans.getTransactionDescriptor()));
//	}
//	
//	@Test
//	public void testTransactionDescriptor4()
//	{
//		TransactionDAO trans1 = new TransactionDAO("100");
//		TransactionDAO trans2 = new TransactionDAO("200");
//	 	assertNotSame("Not the same: ", trans1.getTransactionDescriptor(), trans2.getTransactionDescriptor());      
//	}
//	
//	@Test
//	public void testTransactionDescriptor5()
//	{
//		TransactionDescriptor descriptor1 = trans.getTransactionDescriptor();
//		TransactionDescriptor descriptor2 = trans.getTransactionDescriptor();
//	 	assertSame("Same: ", descriptor1, descriptor2);
//	}
//	
//	@Test
//	public void testTransactionDescriptor6()
//	{
//	 	assertNotNull("Transaction descriptor NOT null: ", trans.getTransactionDescriptor());
//	}
//	
//	@Test
//	public void testTransactionDescriptor7()
//	{
//	 	assertNull("Transaction descriptor null: ", trans.getTransactionDescriptor());
//	}
//	
//	@Test
//	public void testTransactionDescriptor8()
//	{
//		fail("Not yet implemented!");
//	}	
	
	
}
