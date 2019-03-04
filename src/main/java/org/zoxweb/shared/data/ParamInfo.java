package org.zoxweb.shared.data;


import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.SharedUtil;

public class ParamInfo
extends SetNameDescriptionDAO
{
    public enum ValueType
    {
        NONE,
        SINGLE,
        MULTI
    }


    public enum Param
            implements GetNVConfig
    {
        NAME(NVConfigManager.createNVConfig("name", "Name", "Name", true, true, String.class)),
        VALUE_TYPE(NVConfigManager.createNVConfig("value_type", "The value type", "ValueType", true, true, ValueType.class)),
        MANDATORY(NVConfigManager.createNVConfig("mandatory", "Mandatoty", "Mandatory", true, true, Boolean.class)),
        CASE_SENSITIVE(NVConfigManager.createNVConfig("case_sensitive", "Case Sensitive", "CaseSensitive", true, true, Boolean.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }





    public static final NVConfigEntity NVC_PARAM_INFO = new NVConfigEntityLocal(
            "param_info",
            null,
            "ParamInfo",
            true,
            false,
            false,
            false,
            ParamInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
    public ParamInfo() {
        super(NVC_PARAM_INFO);
    }

    public ValueType getValueType()
    {
        return lookupValue(Param.VALUE_TYPE);
    }

    public void setValueType(ValueType vt)
    {
        setValue(Param.VALUE_TYPE, vt);
    }


    public boolean isMandatory()
    {
        return lookupValue(Param.MANDATORY);
    }

    public void setMandatory(boolean mandatory)
    {
        setValue(Param.MANDATORY, mandatory);
    }

    public boolean isCaseSensitive()
    {
        return lookupValue(Param.CASE_SENSITIVE);
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        setValue(Param.CASE_SENSITIVE, caseSensitive);
    }
}
