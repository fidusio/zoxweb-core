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

import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.CreditCardDAO;
import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class is used to define payment information used for
 * payment during transactions.
 *
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class PaymentInfoDAO
        extends SetNameDescriptionDAO {

    public enum Params
            implements GetNVConfig {

        CREDIT_CARD(NVConfigManager.createNVConfigEntity("credit_card", "Credit card information includes: card type, card holder name, card number, expiration date, and security code", "CreditCard", true, true, CreditCardDAO.NVC_CREDIT_CARD_DAO, ArrayType.NOT_ARRAY)),
        BILLING_ADDRESS(NVConfigManager.createNVConfigEntity("billing_address", "Billing address includes: street, city, state/province, zip/postal code, and country", "BillingAddress", true, true, AddressDAO.NVC_ADDRESS_DAO, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }
    }

    public static final NVConfigEntity NVC_PAYMENT_INFO_DAO = new NVConfigEntityPortable(
            "payment_info_dao",
            null,
            "PaymentInfoDAO",
            true,
            false,
            false,
            false,
            PaymentInfoDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );


    /**
     * The default constructor.
     */
    public PaymentInfoDAO() {
        super(NVC_PAYMENT_INFO_DAO);
    }

    /**
     * Returns the credit card information.
     *
     * @return Credit card
     */
    public CreditCardDAO getCreditCard() {
        return lookupValue(Params.CREDIT_CARD);
    }

    /**
     * Sets the credit card information.
     *
     * @param card
     */
    public void setCreditCard(CreditCardDAO card) {
        setValue(Params.CREDIT_CARD, card);
    }

    /**
     * Returns the billing address.
     *
     * @return billing address
     */
    public AddressDAO getBillingAddress() {
        return lookupValue(Params.BILLING_ADDRESS);
    }

    /**
     * Sets the billing address.
     *
     * @param address
     */
    public void setBillingAddress(AddressDAO address) {
        setValue(Params.BILLING_ADDRESS, address);
    }

}