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

import org.zoxweb.shared.data.AppIDDAO;
import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.Date;

/**
 * This class defines the transaction data access object used to create
 * transactions.
 *
 * @author mzebib
 */
@SuppressWarnings("serial")
public class FinancialTransactionDAO
        extends TimeStampDAO {
    /**
     * Sets the default transaction type to credit.
     */
    public static final TransactionType DEFAULT_TT = TransactionType.CREDIT;

    public enum Params
            implements GetNVConfig {
        APP_ID(NVConfigManager.createNVConfigEntity("app_id", "App ID", "AppID", true, false, AppIDDAO.NVC_APP_ID_DAO, NVConfigEntity.ArrayType.NOT_ARRAY)),
        CREATION_TS(NVConfigManager.createNVConfig("creation_ts", "Time in millis when the transaction was created", "CreationTS", true, false, false, false, Date.class, null)),
        EXTERNAL_REFERENCE(NVConfigManager.createNVConfig("external_reference", "External reference", "ExternalReference", false, true, String.class)),
        REFERENCED_NVE(NVConfigManager.createNVConfigEntity("referenced_nve", "Referenced NVEntity", "ReferencedNVEntity", false, false, NVEntity.class, ArrayType.NOT_ARRAY)),
        TRANSACTION_AMOUNT(NVConfigManager.createNVConfigEntity("transaction_amount", "Transaction amount", "TransactionAmount", true, true, AmountDAO.NVC_AMOUNT_DAO)),
        TRANSACTION_TYPE(NVConfigManager.createNVConfig("transaction_type", "Type of transaction either credit or debit", "TransactionType", false, true, TransactionType.class)),
        TRANSACTION_DESCRIPTOR(NVConfigManager.createNVConfig("transaction_descriptor", "Description of transaction", "TransactionDescriptor", false, true, String.class)),

        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        @Override
        public NVConfig getNVConfig() {
            return cType;
        }

    }

    public static final NVConfigEntity NVC_FINANCIAL_TRANSACTION_DAO = new NVConfigEntityPortable(
            "financial_transaction_dao",
            null,
            "FinancialTransactionDAO",
            true,
            false,
            false,
            false,
            FinancialTransactionDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            true,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );

    /**
     * The default constructor.
     */
    public FinancialTransactionDAO() {
        super(NVC_FINANCIAL_TRANSACTION_DAO);
    }

    /**
     * This constructor instantiates FinancialTransactionDAO based on given MoneyValueDAO.
     *
     * @param amount
     */
    public FinancialTransactionDAO(AmountDAO amount) {
        this(amount, DEFAULT_TT);
    }

    /**
     * This constructor instantiates FinancialTransactionDAO based on given transaction amount (MoneyValueDAO) and transaction type (TransactionType).
     * and type.
     *
     * @param amount
     * @param type
     */
    public FinancialTransactionDAO(AmountDAO amount, TransactionType type) {
        this(amount, type, null);
    }

    /**
     * This constructor instantiates FinancialTransactionDAO based on given transaction amount (MoneyValueDAO), transaction type (TransactionType), and descriptor.
     *
     * @param amount
     * @param type
     * @param descriptor
     */
    public FinancialTransactionDAO(AmountDAO amount, TransactionType type, String descriptor) {
        this();
        setAmount(amount);
        setType(type);
        setDescriptor(descriptor);
    }

    /**
     * Returns the app ID.
     *
     * @return
     */
    public AppIDDAO getAppIDDAO() {
        return lookupValue(Params.APP_ID);
    }

    /**
     * Sets the app ID.
     *
     * @param appID
     */
    public void setAppIDDAO(AppIDDAO appID) {
        setValue(Params.APP_ID, appID);
    }

    /**
     * Returns the external reference.
     *
     * @return external reference
     */
    public String getExternalReference() {
        return lookupValue(Params.EXTERNAL_REFERENCE);
    }

    /**
     * Sets the external reference.
     *
     * @param reference
     */
    public void setExternalReference(String reference) {
        setValue(Params.EXTERNAL_REFERENCE, reference);
    }

    /**
     * Returns the referenced NVEntity.
     *
     * @return nventity
     */
    public NVEntity getReferencedNVE() {
        return lookupValue(Params.REFERENCED_NVE);
    }

    /**
     * Sets the referenced NVEntity.
     *
     * @param nve
     */
    public void setReferencedNVE(NVEntity nve) {
        setValue(Params.REFERENCED_NVE, nve);
    }

    /**
     * Returns the transaction amount.
     *
     * @return money value
     */
    public AmountDAO getAmount() {
        return lookupValue(Params.TRANSACTION_AMOUNT);
    }

    /**
     * Sets the transaction amount.
     *
     * @param amount
     */
    public void setAmount(AmountDAO amount) {
        setValue(Params.TRANSACTION_AMOUNT, amount);
    }

    /**
     * Returns the transaction type.
     *
     * @return transaction type
     */
    public TransactionType getType() {
        return lookupValue(Params.TRANSACTION_TYPE);
    }

    /**
     * Sets the transaction type.
     *
     * @param type
     */
    public void setType(TransactionType type) {
        setValue(Params.TRANSACTION_TYPE, type);
    }

    /**
     * Returns the transaction descriptor.
     *
     * @return transaction descriptor
     */
    public String getDescriptor() {
        return lookupValue(Params.TRANSACTION_DESCRIPTOR);
    }

    /**
     * Sets the transaction descriptor.
     *
     * @param descriptor
     */
    public void setDescriptor(String descriptor) {
        setValue(Params.TRANSACTION_DESCRIPTOR, descriptor);
    }

}