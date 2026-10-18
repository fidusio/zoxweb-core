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

import org.zoxweb.shared.filters.ChainedFilter;
import org.zoxweb.shared.filters.CreditCardNumberFilter;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.Date;

@SuppressWarnings("serial")
public class DetailedCreditCardInfo
        extends CanonicalIDImpl
        implements EmailID {

    public enum Param
            implements GetNVConfig {
        EMAIL(NVConfigManager.createNVConfig("email", "The issuer's email address.", "Email", false, true, false, String.class, FilterType.EMAIL)),
        PHONE_NUMBER(NVConfigManager.createNVConfigEntity("phone_number", "Phone number.", "Phone Number", false, true, PhoneInfo.class, ArrayType.NOT_ARRAY)),

        CARD_HOLDER_NAME(NVConfigManager.createNVConfig("card_holder_name", "Name of card holder", "Cardholder Name", true, true, String.class)),
        CARD_TYPE(NVConfigManager.createNVConfig("credit_card_type", "Type of credit card", "Credit Card Type", true, true, CreditCardType.class)),

        CARD_NUMBER(NVConfigManager.createNVConfig("card_number", "Credit card number", "CardNumber", true, true, false, String.class, new ChainedFilter(CreditCardNumberFilter.SINGLETON, FilterType.ENCRYPT))),
        EXPIRATION_DATE(NVConfigManager.createNVConfig("expiration_date", "Card expiration date", "Expiration Date", true, true, Date.class)),
        SECURITY_CODE(NVConfigManager.createNVConfig("security_code", "Card security code", "Security Code", true, true, String.class)),

        FRONTEND_IMAGE(NVConfigManager.createNVConfigEntity("frontend_image", "Frontend image of credit card.", "Frontend Image", false, true, FileInfo.class, ArrayType.NOT_ARRAY)),
        BACKEND_IMAGE(NVConfigManager.createNVConfigEntity("backend_image", "Backend image of credit card.", "Backend Image", false, true, FileInfo.class, ArrayType.NOT_ARRAY)),

        BILLING_ADDRESS(NVConfigManager.createNVConfigEntity("billing_address", "The credit card billing address.", "Billing Address", true, true, AddressInfo.class, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_DETAILED_CREDIT_CARD_INFO = new NVConfigEntityPortable
            (
                    "detailed_credit_card_info",
                    null,
                    DetailedCreditCardInfo.class.getSimpleName(),
                    true,
                    false,
                    false,
                    false,
                    DetailedCreditCardInfo.class,
                    SUS.extractNVConfigs(Param.values()),
                    null,
                    false,
                    CanonicalIDImpl.NVC_CANONICAL_ID_IMPL
            );

    public DetailedCreditCardInfo() {
        super(NVC_DETAILED_CREDIT_CARD_INFO);
    }


    public String getEmailID() {
        return lookupValue(Param.EMAIL);
    }

    public void setEmailID(String email) {
        setValue(Param.EMAIL, email);
    }


    public PhoneInfo getPhoneNumber() {
        return lookupValue(Param.PHONE_NUMBER);
    }

    public void setPhoneNumber(PhoneInfo phone) {
        setValue(Param.PHONE_NUMBER, phone);
    }


    public String getCardHolderName() {
        return lookupValue(Param.CARD_HOLDER_NAME);
    }

    public void setCardHolderName(String name) {
        setValue(Param.CARD_HOLDER_NAME, name);
    }


    public CreditCardType getCardType() {
        return lookupValue(Param.CARD_TYPE);
    }

    public void setCardType(CreditCardType type) {
        setValue(Param.CARD_TYPE, type);
    }


    public String getCardNumber() {
        return lookupValue(Param.CARD_NUMBER);
    }

    public void setCardNumber(String number) {
        setValue(Param.CARD_NUMBER, number);
    }


    public long getExpirationDate() {
        return lookupValue(Param.EXPIRATION_DATE);
    }

    public void setExpirationDate(long date) {
        setValue(Param.EXPIRATION_DATE, date);
    }


    public String getSecurityCode() {
        return lookupValue(Param.SECURITY_CODE);
    }

    public void setSecurityCode(String code) {
        setValue(Param.SECURITY_CODE, CreditCardNumberFilter.validateCVV(getCardNumber(), code));
    }


    public FileInfo getFrontEndImage() {
        return lookupValue(Param.FRONTEND_IMAGE);
    }

    public void setFrontEndImage(FileInfo file) {
        setValue(Param.FRONTEND_IMAGE, file);
    }


    public FileInfo getBackEndImage() {
        return lookupValue(Param.BACKEND_IMAGE);
    }

    public void setBackEndImage(FileInfo file) {
        setValue(Param.BACKEND_IMAGE, file);
    }


    public AddressInfo getBillingAddress() {
        return lookupValue(Param.BILLING_ADDRESS);
    }

    public void setBillingAddress(AddressInfo address) {
        setValue(Param.BILLING_ADDRESS, address);
    }

}