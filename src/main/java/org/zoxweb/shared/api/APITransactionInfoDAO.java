/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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
package org.zoxweb.shared.api;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class extends the API transaction information interface to create
 * the API transaction information data access object.
 *
 * @author mzebib
 */
@SuppressWarnings("serial")
public class APITransactionInfoDAO
        extends SetNameDescriptionDAO
        implements APITransactionInfo {

    public enum Params
            implements GetNVConfig {
        MESSAGE(NVConfigManager.createNVConfig("message", "Message", "Message", true, true, APINotificationMessage.class)),
        DELIVER_STATUS(NVConfigManager.createNVConfig("deliver_status", "Deliver status", "DeliverStatus", true, true, APINotificationStatus.class)),
        UPDATE_TIME_STAMP(NVConfigManager.createNVConfig("udpate_time_stamp", "Update time stamp", "UpdateTimeStamp", true, true, long.class)),
        CREATE_TIME_STAMP(NVConfigManager.createNVConfig("create_time_stamp", "Create time stamp", "CreateTimeStamp", true, true, long.class)),
        REMOTE_ID(NVConfigManager.createNVConfig("remote_id", "Remote provider ID", "RemoteID", true, true, String.class)),

        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }

    }

    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on APITransactionInfoDAO.
     */
    public static final NVConfigEntity NVC_API_TRANSACTION_INFO_DAO = new NVConfigEntityPortable
            (
                    "api_transaction_info_dao",
                    null,
                    "APITransactionInfoDAO",
                    true,
                    false,
                    false,
                    false,
                    APITransactionInfoDAO.class,
                    SharedUtil.extractNVConfigs(Params.values()),
                    null,
                    false,
                    SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
            );

    /**
     * This is the default constructor.
     */
    public APITransactionInfoDAO() {
        super(NVC_API_TRANSACTION_INFO_DAO);
    }

    /**
     * This constructor instantiates APITransactionInfoDAO based on list of NVConfigEntity type.
     * @param list
     */
//	protected APITransactionInfoDAO(List<NVConfigEntity> list)
//	{
//		super(SharedUtil.merge(list, NVC_API_TRANSACTION_INFO_DAO));
//	}

    /**
     * Returns the message.
     */
    @Override
    public APINotificationMessage getMessage() {
        return lookupValue(Params.MESSAGE);
    }

    /**
     * Sets the message.
     *
     * @param message
     */
    @Override
    public void setMessage(APINotificationMessage message) {
        setValue(Params.MESSAGE, message);
    }

    /**
     * Returns the delivery status.
     */
    @Override
    public APINotificationStatus getDeliverStatus() {
        return lookupValue(Params.DELIVER_STATUS);
    }

    /**
     * Sets the delivery status.
     *
     * @param status
     */
    @Override
    public void setDeliverStatus(APINotificationStatus status) {
        setValue(Params.DELIVER_STATUS, status);
    }

    /**
     * Returns the update time stamp.
     */
    @Override
    public long getUpdateTimeStamp() {
        return lookupValue(Params.UPDATE_TIME_STAMP);
    }

    /**
     * Sets the update time stamp.
     *
     * @param ts
     */
    @Override
    public void setUpdateTimeStamp(long ts)
            throws IllegalArgumentException {
        setValue(Params.UPDATE_TIME_STAMP, ts);
    }

    /**
     * Returns the create time stamp.
     */
    @Override
    public long getCreateTimeStamp() {
        return lookupValue(Params.CREATE_TIME_STAMP);
    }

    /**
     * Sets the create time stamp.
     *
     * @param ts
     */
    @Override
    public void setCreateTimeStamp(long ts)
            throws IllegalArgumentException {
        setValue(Params.CREATE_TIME_STAMP, ts);
    }

    /**
     * Returns the remote ID.
     */
    @Override
    public String getRemoteID() {
        return lookupValue(Params.REMOTE_ID);
    }

    /**
     * Sets the remote ID.
     *
     * @param remoteProviderID
     */
    @Override
    public void setRemoteID(String remoteProviderID) {
        setValue(Params.REMOTE_ID, remoteProviderID);
    }

}