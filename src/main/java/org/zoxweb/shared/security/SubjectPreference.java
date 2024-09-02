package org.zoxweb.shared.security;

import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.CreditCardDAO;
import org.zoxweb.shared.data.DataConst.Language;
import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class the subject preferences based on the AppGUID, it is mapped to the subject via
 * the subjectGUID
 * Created on 8/4/17
 */
@SuppressWarnings("serial")
public class SubjectPreference
	extends SetNameDescriptionDAO
{

    public enum Param
        implements GetNVConfig {

        APP_GUID(NVConfigManager.createNVConfig("app_guid", "App GUID","AddGUID", true, false, String.class)),
        DEFAULT_LANGUAGE(NVConfigManager.createNVConfig("language", "Default language", "DefaultLanguage", false, true, Language.class)),
        DEFAULT_DELIVERY_ADDRESS(NVConfigManager.createNVConfigEntity("delivery_address", "Default delivery address", "DefaultDeliveryAddress", false, true, AddressDAO.NVC_ADDRESS_DAO, ArrayType.NOT_ARRAY)),
        DEFAULT_BILLING_ADDRESS(NVConfigManager.createNVConfigEntity("billing_address", "Default billing address", "DefaultBillingAddress", false, true, AddressDAO.NVC_ADDRESS_DAO, ArrayType.NOT_ARRAY)),
        DEFAULT_CREDIT_CARD(NVConfigManager.createNVConfigEntity("credit_card", "Default credit card", "DefaultCreditCard", false, true, CreditCardDAO.NVC_CREDIT_CARD_DAO, ArrayType.NOT_ARRAY)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_SUBJECT_PREFERENCE = new NVConfigEntityLocal(
            "subject_preference",
            null,
            "SubjectPreference",
            true,
            false,
            false,
            false,
            SubjectPreference.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
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

    public AddressDAO getDefaultDeliveryAddress()
    {
        return lookupValue(Param.DEFAULT_DELIVERY_ADDRESS);
    }

    public void setDefaultDeliveryAddress(AddressDAO address)
    {
        setValue(Param.DEFAULT_DELIVERY_ADDRESS, address);
    }

    public AddressDAO getDefaultBillingAddress()
    {
        return lookupValue(Param.DEFAULT_BILLING_ADDRESS);
    }

    public void setDefaultBillingAddress(AddressDAO address)
    {
        setValue(Param.DEFAULT_BILLING_ADDRESS, address);
    }

    public CreditCardDAO getDefaultCreditCard()
    {
        return lookupValue(Param.DEFAULT_CREDIT_CARD);
    }

    public void setDefaultCreditCard(CreditCardDAO creditCard)
    {
        setValue(Param.DEFAULT_CREDIT_CARD, creditCard);
    }

    public void setAppGUID(String appGUID)
    {
        setValue(Param.APP_GUID, appGUID);
    }
    public String getAppGUID()
    {
        return lookupValue(Param.APP_GUID);
    }


}