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
package org.zoxweb.shared.accounting;

//import org.junit.Test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

//import static org.junit.Assert.assertEquals;

public class AccountTest {

	private static BigDecimal balance = new BigDecimal("0.00");

	@Test
	public void testDebitAndCredit() {
        System.out.format("Current Balance: $ %s\t", balance);
        Assertions.assertEquals(new BigDecimal("0.00"), balance);

        System.out.println("Credit: $" + credit("100.00"));
        System.out.format("Current Balance: $ %s\t", balance);
        Assertions.assertEquals(new BigDecimal("100.00"), balance);

        System.out.println("Credit: $" + credit("400.55"));
        System.out.format("Current Balance: $ %s\t", balance);
        Assertions.assertEquals(new BigDecimal("500.55"), balance);

        System.out.println("Debit:  ($" + debit("100.35888") + ")");
        System.out.format("Current Balance: $ %s\t", balance);
        Assertions.assertEquals(new BigDecimal("400.19"), balance);
    }

    private static BigDecimal credit(String amount) {
        BigDecimal credit = new BigDecimal(amount);

        balance = balance.add(credit);
        balance.setScale(2, BigDecimal.ROUND_HALF_EVEN);

        return credit;
    }

    private static BigDecimal debit(String amount) {
        BigDecimal debit = new BigDecimal(amount);

        balance = balance.subtract(debit);
        balance = balance.setScale(2, BigDecimal.ROUND_HALF_EVEN);

        return debit;
    }

}