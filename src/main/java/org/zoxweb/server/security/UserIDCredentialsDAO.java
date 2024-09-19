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
package org.zoxweb.server.security;

import java.util.Date;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;

/**
 * This class defines user credentials data access object used to create and store user
 * credentials.
 *
 * @author mzebib
 */
@SuppressWarnings("serial")
public class UserIDCredentialsDAO
    extends SetNameDescriptionDAO
    implements DoNotExpose, SetCanonicalID
{



  /**
   * This enum contains user credential variables including: user id, user status, last status
   * update time stamp, pending token, pending pin, and password.
   *
   * @author mzebib
   */
  public enum Param
      implements GetNVConfig {

    //USER_ID_DAO(NVConfigManager.createNVConfigEntity("user_id_dao", "User ID data access object.", "UserIDDAO", true, true, UserIDDAO.NVC_USER_ID_DAO)),
    USER_STATUS(NVConfigManager
        .createNVConfig("user_status", "User status", "UserStatus", true, true,
            CryptoConst.SubjectStatus.class)),
    LAST_STATUS_UPDATE_TIMESTAMP(NVConfigManager
        .createNVConfig("last_status_update_timestamp", "Timestamp of last status update",
            "LastStatusUpdateTimestamp", true, true, Date.class)),
    PENDING_TOKEN(NVConfigManager
        .createNVConfig("pending_token", "Pending token", "PendingToken", true, true,
            String.class)),
    PENDING_PIN(NVConfigManager
        .createNVConfig("pending_pin", "Pending pin", "PendingPin", true, true, String.class)),
    PASSWORD(NVConfigManager.createNVConfigEntity("password", "Password", "Password", true, true,
        CIPassword.NVCE_CI_PASSWORD)),
    CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "CanonicalID map", "CanonicalID", true, false, String.class)),

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
   * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object
   * based on UserCredentialsStatusDAO.
   */
  public static final NVConfigEntity NVC_USER_ID_CREDENTIALS_DAO = new NVConfigEntityLocal(
      "user_id_credentials_dao",
      null,
      "UserIDCredentialsDAO",
      true,
      false,
      false,
      false,
      UserIDCredentialsDAO.class,
      SharedUtil.extractNVConfigs(Param.values()),
      null,
      false,
      SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
  );

  /**
   * This is the default constructor.
   */
  public UserIDCredentialsDAO() {
    super(NVC_USER_ID_CREDENTIALS_DAO);
  }

  /**
   * Returns the user status.
   *
   * @return UserStatus
   */
  public CryptoConst.SubjectStatus getUserStatus() {
    return lookupValue(Param.USER_STATUS);
  }

  /**
   * Sets the user status.
   */
  public void setUserStatus(CryptoConst.SubjectStatus status) {
    setValue(Param.USER_STATUS, status);
  }

  /**
   * Returns the time stamp of the last status update.
   *
   * @return in millis last status update
   */
  public long getLastStatusUpdateTimestamp() {
    return lookupValue(Param.LAST_STATUS_UPDATE_TIMESTAMP);
  }

  /**
   * Sets the time stamp for the last status update.
   */
  public void setLastStatusUpdateTimestamp(long timestamp) {
    setValue(Param.LAST_STATUS_UPDATE_TIMESTAMP, timestamp);
  }

  /**
   * Returns the pending token.
   *
   * @return the pending token
   */
  public String getPendingToken() {
    return lookupValue(Param.PENDING_TOKEN);
  }

  /**
   * Sets the pending token.
   */
  public void setPendingToken(String token) {
    setValue(Param.PENDING_TOKEN, token);
  }


  /**
   * Returns the pending pin.
   *
   * @return pending pin
   */
  public String getPendingPin() {
    return lookupValue(Param.PENDING_PIN);
  }

  /**
   * Sets the pending pin.
   */
  public void setPendingPin(String pin) {
    setValue(Param.PENDING_PIN, pin);
  }

  /**
   * Returns the password.
   *
   * @return password dao
   */
  public CIPassword getPassword() {
    return lookupValue(Param.PASSWORD);
  }

  /**
   * Sets the password.
   */
  public void setPassword(CIPassword password) {
    setValue(Param.PASSWORD, password);
  }

  @Override
  public String getCanonicalID() {
    return lookupValue(Param.CANONICAL_ID);
  }

  @Override
  public void setCanonicalID(String canonicalID) {
    setValue(Param.CANONICAL_ID, canonicalID);
  }

  @Override
  public String toCanonicalID() {
    return getCanonicalID();
  }
}