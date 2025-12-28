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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;

import java.math.BigDecimal;

/**
 * @author javaconsigliere@gmail.com
 *
 */
@SuppressWarnings("serial")
public class AmountDAO
        extends SetNameDescriptionDAO
        implements RenderableValue<String> {

    /**
     * Default currency set to U.S. dollar (USD).
     */
    public static final Currency DEFAULT_CURRENCY = Currency.USD;

    public enum Params
            implements GetNVConfig {
        CURRENCY(NVConfigManager.createNVConfig("currency", "Currency", "Currency", true, true, Currency.class)),
        AMOUNT(NVConfigManager.createNVConfig("amount", "Amount", "Amount", true, true, BigDecimal.class)),

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

    public static final NVConfigEntity NVC_AMOUNT_DAO = new NVConfigEntityPortable(
            "amount_dao",
            null,
            "AmountDAO",
            true,
            false,
            false,
            false,
            AmountDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            true,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );


    /**
     * The default constructor.
     */
    public AmountDAO() {
        super(NVC_AMOUNT_DAO);
        setCurrency(DEFAULT_CURRENCY);
    }

    /**
     * This constructor instantiates MoneyValueDAO based on given amount (BigDecimal) and currency (Currency).
     *
     * @param amount
     * @param currency
     */
    public AmountDAO(BigDecimal amount, Currency currency) {
        this();
        setAmount(amount);
        if (currency != null) {
            setCurrency(currency);
        }
    }

    public AmountDAO(BigDecimal amount) {
        this(amount, null);
    }

    /**
     * This constructor instantiates MoneyValueDAO given the amount (String).
     * The default currency is applied.
     *
     * @param amount
     */
    public AmountDAO(String amount) {
        this(new BigDecimal(amount), null);
    }

    /**
     * This constructor instantiates MoneyValueDAO given the amount (long).
     * The default currency is applied.
     *
     * @param amount
     */
    public AmountDAO(long amount) {
        this(new BigDecimal(amount), null);
    }

    /**
     * This constructor instantiates MoneyValueDAO given the amount (float).
     * The default currency is applied.
     *
     * @param amount
     */
    public AmountDAO(float amount) {
        this(BigDecimal.valueOf(amount), null);
    }

    /**
     * This constructor instantiates MoneyValueDAO based on given currency (Currency).
     *
     * @param currency
     */
    public AmountDAO(Currency currency) {
        this(null, currency);
    }

    /**
     * Returns the currency.
     *
     * @return currency
     */
    public Currency getCurrency() {
        return lookupValue(Params.CURRENCY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {

            if (obj instanceof AmountDAO) {
                AmountDAO mvd = (AmountDAO) obj;
                if (mvd.getCurrency() == getCurrency() && mvd.getAmount().equals(getAmount())) {
                    return true;
                }

            } else if (obj instanceof BigDecimal) {
                return getAmount().equals(obj);
            }

        }

        return false;
    }

    /**
     * Sets the currency.
     *
     * @param currency
     */
    public void setCurrency(Currency currency) {
        setValue(Params.CURRENCY, currency);
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public BigDecimal getAmount() {
        BigDecimal ret = lookupValue(Params.AMOUNT);
        if (ret != null) {
            ret = ret.setScale(2);
        }

        return ret;
    }

    /**
     * Sets the value.
     *
     * @param value
     */
    public void setAmount(BigDecimal value) {
        setValue(Params.AMOUNT, value);
    }

    /**
     * Returns the value as String.
     */
    @Override
    public String toValue() {
        return toString();
    }

    /**
     * Returns a string representation of the value and currency.
     * Examples:
     * Value = 100.00 and currency = USD ($), returns $100.00,
     * Value = -100.00 and currency = USD ($), returns -$100.00
     */
    @Override
    public String toString() {
        String ret = null;

        if (getAmount() != null && getCurrency() != null) {

            if (getAmount().signum() < 0) {
                ret = "-" + getCurrency().getValue() + getAmount().abs();
            } else {
                ret = getCurrency().getValue() + getAmount();
            }
        }

        return ret;
    }
}