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
package org.zoxweb.shared.api;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.TimeInMillis;

import java.util.Date;

/**
 * This class defines the box token object used to set up and refresh
 * access tokens for Box API.
 *
 * @author mzebib
 */
@SuppressWarnings("serial")
public class APIAccessTokenDAO
        extends TimeStampDAO {

    public enum Token
            implements GetNVConfig {
        //		ACCESS_TOKEN(NVConfigManager.createNVConfig("access_token", "Access token", "AccessToken", false, true, false, String.class, FilterType.ENCRYPT)),
        ACCESS_TOKEN(CryptoConst.OAuthParam.ACCESS_TOKEN.getNVConfig()),
        //		EXPIRES_IN(NVConfigManager.createNVConfig("expires_in", "Expiration time value", "ExpiresIn", false, true, Integer.class)),
        EXPIRES_IN(CryptoConst.OAuthParam.EXPIRES_IN.getNVConfig()),
        //		EXPIRATION_UNIT(NVConfigManager.createNVConfig("expiration_unit", "Expiration time unit", "ExpirationUnit", false, true, TimeInMillis.class)),
        EXPIRATION_UNIT(CryptoConst.OAuthParam.EXPIRATION_UNIT.getNVConfig()),
        //		REFRESH_TOKEN(NVConfigManager.createNVConfig("refresh_token", "Refresh token", "RefreshToken", false, true, false, String.class, FilterType.ENCRYPT)),
        REFRESH_TOKEN(CryptoConst.OAuthParam.REFRESH_TOKEN.getNVConfig()),
        //		TOKEN_TYPE(NVConfigManager.createNVConfig("token_type", "Token type", "TokenType", false, true, String.class)),
        TOKEN_TYPE(CryptoConst.OAuthParam.TOKEN_TYPE.getNVConfig()),

        ;

        private final NVConfig cType;

        Token(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }
    }

    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on BoxTokenDAO.
     */
    public static final NVConfigEntity NVC_API_ACCESS_TOKEN_DAO = new NVConfigEntityPortable("api_access_token_dao",
            null,
            "APIAccessTokenDAO",
            true,
            false,
            false,
            false,
            APIAccessTokenDAO.class,
            SharedUtil.extractNVConfigs(Token.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );


    /**
     * This is the default constructor.
     */
    public APIAccessTokenDAO() {
        super(NVC_API_ACCESS_TOKEN_DAO);
    }

    protected APIAccessTokenDAO(NVConfigEntity nvce) {
        super(nvce);
    }

    /**
     * Returns the access token.
     *
     * @return the access token.
     */
    public String getAccessToken() {
        return lookupValue(Token.ACCESS_TOKEN);
    }

    /**
     * Sets the access token.
     *
     * @param token
     */
    public void setAccessToken(String token) {
        setValue(Token.ACCESS_TOKEN, token);
    }

    /**
     * Returns the token expiration.
     *
     * @return the expiration token.
     */
    public int getTokenExpiration() {
        return lookupValue(Token.EXPIRES_IN);
    }

    /**
     * Sets the token expiration.
     *
     * @param value
     */
    public void setTokenExpiration(int value) {
        setValue(Token.EXPIRES_IN, value);
        setLastTimeUpdated(System.currentTimeMillis());
    }

    /**
     * Returns the refresh token.
     *
     * @return the refresh token.
     */
    public String getRefreshToken() {
        return lookupValue(Token.REFRESH_TOKEN);
    }

    /**
     * Sets the refresh token.
     *
     * @param refreshToken
     */
    public void setRefreshToken(String refreshToken) {
        setValue(Token.REFRESH_TOKEN, refreshToken);
    }

    /**
     * Returns the token type.
     *
     * @return the token type.
     */
    public String getTokenType() {
        return lookupValue(Token.TOKEN_TYPE);
    }

    /**
     * Sets the token type.
     *
     * @param type
     */
    public void setTokenType(String type) {
        setValue(Token.TOKEN_TYPE, type);
    }

    /**
     * Sets the creation time.
     *
     * @param time
     */
    public void setCreationTime(Date time) {
        setCreationTime(time.getTime());
    }

    /**
     * Check if the token is expired.
     *
     * @return
     */
    public boolean isTokenExpired() {
//		long expiration = getTokenExpiration();
//		Const.TimeInMillis tim = getExpirationTimeUnit();
//		if (tim == null)
//		{
//			tim = Const.TimeInMillis.SECOND;
//		}
//		// zero or negative value token does not expire
//		if (expiration <=0)
//			return false;

        return (millisToExpire() < 0);
    }

    public long millisToExpire() {
        long expiration = getTokenExpiration();
        Const.TimeInMillis tim = getExpirationTimeUnit();

        if (tim == null) {
            tim = Const.TimeInMillis.SECOND;
        }

        // zero or negative value token does not expire
//		if (expiration <= 0)
//			return Const.TimeInMillis.WEEK.MILLIS;

        return ((getLastTimeUpdated() + expiration * tim.MILLIS) - System.currentTimeMillis());
    }

    public TimeInMillis getExpirationTimeUnit() {
        return lookupValue(Token.EXPIRATION_UNIT);
    }

    public void setExpirationTimeUnit(TimeInMillis unit) {
        setValue(Token.EXPIRATION_UNIT, unit);
    }

}