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

import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.math.BigDecimal;

/**
 * This class defines account based on transactions which can be used to update
 * the current balance after a transaction is applied.
 *
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class BillingAccountDAO
        extends TimeStampDAO {

    public enum Params
            implements GetNVConfig {
        CURRENT_BALANCE(NVConfigManager.createNVConfigEntity("current_balance", "Current balance", "CurrentBalance", true, true, AmountDAO.class, ArrayType.NOT_ARRAY)),
        PAYMENT_INFO(NVConfigManager.createNVConfigEntity("payment_info", "Payment info", "PaymentInfo", false, true, PaymentInfoDAO.class, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }
    }

    public static final NVConfigEntity NVC_BILLING_ACCOUNT_DAO = new NVConfigEntityPortable(
            "billing_account_dao",
            null,
            "BillingAccountDAO",
            true,
            false,
            false,
            false,
            BillingAccountDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );


    /**
     * The default constructor.
     */
    public BillingAccountDAO() {
        super(NVC_BILLING_ACCOUNT_DAO);
    }

    /**
     * This constructor instantiates BillingAccountDAO based given NVConfigEntity.
     *
     * @param nvce
     */
    protected BillingAccountDAO(NVConfigEntity nvce) {
        super(nvce);
    }

    /**
     * Returns the current balance.
     *
     * @return MoneyValueDAO
     */
    public AmountDAO getCurrentBalance() {
        return lookupValue(Params.CURRENT_BALANCE);
    }

    /**
     * Sets the current balance.
     *
     * @param balance
     */
    public synchronized void setCurrentBalance(AmountDAO balance) {
        setValue(Params.CURRENT_BALANCE, balance);
    }

    /**
     * Returns the payment information.
     *
     * @return PaymentInfoDAO
     */
    public PaymentInfoDAO getPaymentInfo() {
        return lookupValue(Params.PAYMENT_INFO);
    }

    /**
     * Sets the payment information.
     *
     * @param info
     */
    public void setPaymentInfo(PaymentInfoDAO info) {
        setValue(Params.PAYMENT_INFO, info);
    }

    /**
     * Applies the given transaction and updates the current balance.
     *
     * @param transaction
     * @return the updated value after applying the transaction
     */
    public synchronized BigDecimal applyTransaction(FinancialTransactionDAO transaction) {
        if (getCurrentBalance().getCurrency() != transaction.getAmount().getCurrency()) {
            throw new IllegalArgumentException("Currency mismatch: " + transaction.getAmount().getCurrency());
        }

        switch (transaction.getType()) {
            case CREDIT:
                getCurrentBalance().setAmount((getCurrentBalance().getAmount().add(transaction.getAmount().getAmount())));
                break;
            case DEBIT:
                getCurrentBalance().setAmount((getCurrentBalance().getAmount().subtract(transaction.getAmount().getAmount())));
                break;
        }

        return getCurrentBalance().getAmount();
    }

}