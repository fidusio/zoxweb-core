package org.zoxweb.shared.api;


import org.zoxweb.shared.data.DataConst;


import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;

import java.util.List;

@SuppressWarnings("serial")
public class APIDataOP
        extends SetNameDescriptionDAO {
    public enum Param
            implements GetNVConfig {
        DATA_OP(NVConfigManager.createNVConfig("data_op", "Data Operation", "DataOP", true, true, DataConst.DataOP.class)),
        PROP_NAMES(NVConfigManager.createNVConfig("prop_names", "name of parameters", "PropNames", true, true, NVStringList.class)),
        NVE(NVConfigManager.createNVConfigEntity("nve", "name of parameters", "PropNames", true, true, NVEntity.class, NVConfigEntity.ArrayType.NOT_ARRAY)),

        ;
        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }


    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
     */
    public static final NVConfigEntity NVC_API_DATA_OP = new NVConfigEntityPortable("api_data_op",
            null,
            "APIDataOP",
            true,
            false,
            false,
            false,
            APIDataOP.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);

    public APIDataOP() {
        super(NVC_API_DATA_OP);
    }

    public void setDataOP(DataConst.DataOP dataOP) {
        setValue(Param.DATA_OP, dataOP);
    }

    public DataConst.DataOP getDataOP() {
        return lookupValue(Param.DATA_OP);
    }

    public NVStringList getPropertiesNames() {
        return (NVStringList) lookup(Param.PROP_NAMES);
    }

    public synchronized void add(String... names) {
        List<String> list = getPropertiesNames().getValue();
        for (String name : names) {
            if (!SUS.isEmpty(name))
                list.add(name);
        }
    }

    public void setNVEntity(NVEntity nve) {
        setValue(Param.NVE, nve);
    }

    public NVEntity getNVEntity() {
        return lookupValue(Param.NVE);
    }
}
