package org.zoxweb.shared.security;

import org.zoxweb.shared.app.AppIDDefault;
import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * This class is a common base for authorization entities. It provides GUID,
 * Subject GUID, Broker GUID, name, description, DomainID and AppID - through
 * an AppIdDao object - and properties metadata. Note: this class forces child
 * classes to have mandatory naming.
 */
public abstract class AuthzInfo extends PropertyDAO {

    public enum Param implements GetNVConfig {
        BROKER_GUID(NVConfigManager.createNVConfig("broker_guid", "the broker GUID, used for audit", "BrokerGUID", false, false, String.class)),
        APP_ID_DAO(NVConfigManager.createNVConfigEntity("app_id", "the app id object, representing the domain and app ids", "AppID", false, false, AppIDDefault.NVC_APP_ID_DEFAULT)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_AUTHZ_INFO = new NVConfigEntityPortable(
            "authz_info_dao",
            null,
            "AuthZInfoDao",
            true,
            false,
            false,
            false,
            AuthzInfo.class,
            SharedUtil.extractNVConfigs(DataConst.DataParam.MANDATORY_NAME, Param.BROKER_GUID, Param.APP_ID_DAO),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    /**
     * Constructor allowing the class to be extended
     *
     * @param nvce NVConfigEntity to pass down
     */
    protected AuthzInfo(NVConfigEntity nvce) {
        super(nvce);
    }


    /**
     *
     * @param id string broker id
     */
    public void setBrokerGUID(String id) {
        setValue(Param.BROKER_GUID, id);
    }

    /**
     *
     * @return a string broker id
     */
    public String getBrokerGUID() {
        return lookupValue(Param.BROKER_GUID);
    }

    /**
     *
     * @param id an AppIdDao object
     */
    public void setAppIdDAO(AppIDDefault id) {
        setValue(Param.APP_ID_DAO, id);
    }

    /**
     *
     * @return an AppIdDao object
     */
    public AppIDDefault getAppIdDAO() {
        return lookupValue(Param.APP_ID_DAO);
    }


}
