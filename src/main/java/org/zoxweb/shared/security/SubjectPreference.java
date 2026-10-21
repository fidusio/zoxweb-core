package org.zoxweb.shared.security;

import org.zoxweb.shared.data.AddressInfo;
import org.zoxweb.shared.data.CreditCardInfo;
import org.zoxweb.shared.data.DataConst.Language;
import org.zoxweb.shared.data.SetNameDescriptionImpl;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class the subject preferences based on the AppGUID, it is mapped to the subject via
 * the subjectGUID
 * Created on 8/4/17
 */
@SuppressWarnings("serial")
public class SubjectPreference
	extends SetNameDescriptionImpl
{

    public enum Param
        implements GetNVConfig {

        // app_guid is the inherited ReferenceIDImpl.NVC_APP_GUID since 2026-10-19 (dual ownership): no redeclaration,
        // which would override the inherited attribute (NVConfigEntityPortable.setAttributes on a repeated name)
        DEFAULT_LANGUAGE(NVConfigManager.createNVConfig("language", "Default language", "DefaultLanguage", false, true, Language.class)),
        DEFAULT_DELIVERY_ADDRESS(NVConfigManager.createNVConfigEntity("delivery_address", "Default delivery address", "DefaultDeliveryAddress", false, true, AddressInfo.NVC_ADDRESS_INFO, ArrayType.NOT_ARRAY)),
        DEFAULT_BILLING_ADDRESS(NVConfigManager.createNVConfigEntity("billing_address", "Default billing address", "DefaultBillingAddress", false, true, AddressInfo.NVC_ADDRESS_INFO, ArrayType.NOT_ARRAY)),
        DEFAULT_CREDIT_CARD(NVConfigManager.createNVConfigEntity("credit_card", "Default credit card", "DefaultCreditCard", false, true, CreditCardInfo.NVC_CREDIT_CARD_INFO, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_SUBJECT_PREFERENCE = new NVConfigEntityPortable(
            "subject_preference",
            null,
            "SubjectPreference",
            true,
            false,
            false,
            false,
            SubjectPreference.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionImpl.NVC_NAME_DESCRIPTION_IMPL
    );

    public SubjectPreference()
    {
        super(NVC_SUBJECT_PREFERENCE);
    }


    public Language getDefaultLanguage()
    {
        return lookupValue(Param.DEFAULT_LANGUAGE);
    }

    public void setDefaultLanguage(Language language)
    {
        setValue(Param.DEFAULT_LANGUAGE, language);
    }

    public AddressInfo getDefaultDeliveryAddress()
    {
        return lookupValue(Param.DEFAULT_DELIVERY_ADDRESS);
    }

    public void setDefaultDeliveryAddress(AddressInfo address)
    {
        setValue(Param.DEFAULT_DELIVERY_ADDRESS, address);
    }

    public AddressInfo getDefaultBillingAddress()
    {
        return lookupValue(Param.DEFAULT_BILLING_ADDRESS);
    }

    public void setDefaultBillingAddress(AddressInfo address)
    {
        setValue(Param.DEFAULT_BILLING_ADDRESS, address);
    }

    public CreditCardInfo getDefaultCreditCard()
    {
        return lookupValue(Param.DEFAULT_CREDIT_CARD);
    }

    public void setDefaultCreditCard(CreditCardInfo creditCard)
    {
        setValue(Param.DEFAULT_CREDIT_CARD, creditCard);
    }

    // getAppGUID / setAppGUID: inherited from ReferenceIDImpl (app_guid), the app this preference belongs to

}