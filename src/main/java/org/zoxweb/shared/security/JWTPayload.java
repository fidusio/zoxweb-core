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
package org.zoxweb.shared.security;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;


import java.util.Date;



public class JWTPayload 
    //extends SetNameDescriptionDAO
    implements AppID<String>,
    		   GetNVProperties
{
	

	public enum Param
	    implements GetNVConfig, GetName
	{
		
		ISS(NVConfigManager.createNVConfig("iss", "Issuer Identifier", "Issuer", false, true, false, String.class, null)),
		SUB(NVConfigManager.createNVConfig("sub", "Subject Identifier", "SubjectID", true, true, false, String.class, null)),
		AUD(NVConfigManager.createNVConfig("aud", "Audience", "Audience", false, true, false, String.class, null)),	
		EXP(NVConfigManager.createNVConfig("exp", "Expiration time", "Expiration", false, true, false, Date.class, null)),
		NBF(NVConfigManager.createNVConfig("nbf", "Not Before", "NBF", false, true, false, Date.class, null)),
		IAT(NVConfigManager.createNVConfig("iat", "Issued At", "IAT", false, true, false, long.class, null)),
		JTI(NVConfigManager.createNVConfig("jti", "JWT ID", "JWTID", false, true, false, String.class, null)),
		//NAME(NVConfigManager.createNVConfig("name", "name", "Name", true, true, false, String.class, null)),
		ADMIN(NVConfigManager.createNVConfig("admin", "Admin", "admin", false, true, boolean.class)),
		DOMAIN_ID(NVConfigManager.createNVConfig("domain", "Domain identifier", "DomainID", false, true, false, String.class, FilterType.DOMAIN)),
		APP_ID(NVConfigManager.createNVConfig("app", "ApplicationID", "AppID", false, true, false, String.class, null)),
		NONCE(NVConfigManager.createNVConfig("nonce", "Nonce", "Nonce", false, true, false, long.class, null)),
		//RANDOM(NVConfigManager.createNVConfig("random", "Random Data", "Random", false, true, false, byte[].class, null)),
		
		
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
		public String getName()
		{
			return nvc.getName();
		}
	}
	
	public static final NVConfigEntity NVC_JWT_PAYLOAD = new NVConfigEntityPortable(  "jwt_payload",
																					null , 
																					"JWTPayload", 
																					true, 
																					false, 
																					false, 
																					false, 
																					JWTPayload.class, 
																					SharedUtil.extractNVConfigs(Param.values()), 
																					null, 
																					true, 
																					SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																				);

	
	private final NVGenericMap nvgm;

	public JWTPayload(NVGenericMap gnv) 
	{
		this.nvgm = gnv;
//		super(NVC_JWT_PAYLOAD);
//		
//		gnv = SharedUtil.toNVGenricMap(this);
	
		
		//genericMap.
	}

	@Override
	public String getDomainID() 
	{
		return nvgm.getValue((GetName)Param.DOMAIN_ID);
	}

	public String getName()
	{
		return nvgm.getValue("name");
	}
	
	public void setName(String name)
	{
		nvgm.add("name", name);
	}
	@Override
	public void setDomainID(String domainID) 
	{
		//setValue(Param.DOMAIN_ID, domainID);
	
		nvgm.add(Param.DOMAIN_ID,domainID);
	}

	public String getAppID() 
	{
		return nvgm.getValue((GetName)Param.APP_ID);
	}

	public void setAppID(String appID) 
	{
		nvgm.add(Param.APP_ID, appID);
	}
	
	public long getNonce() 
	{
		return nvgm.getValue(Param.NONCE.getName(), 0);
		
	}

	public String getSubjectID() 
	{
		return nvgm.getValue((GetName)Param.SUB);
	}

	public void setSubjectID(String sub) 
	{
		nvgm.add(Param.SUB,sub);
	}

	public boolean isAdmin() 
	{
		return nvgm.getValue(Param.ADMIN.getName(), false);
	}
	
	public void setAdmin(boolean isAdmin) 
	{
		nvgm.add(new NVBoolean(Param.ADMIN.getName(), isAdmin));
	}

	public void setNonce(long nonce) 
	{
		nvgm.add(new NVLong(Param.NONCE.getName(), nonce));
	}
	
//	public void setNonce(String nonce) 
//	{
//		setValue(Param.NONCE, nonce);
//	}

//	public byte[] getRandom() 
//	{
//		return lookupValue(Param.RANDOM);
//	}
//
//
//	public void setRandom(byte[] random) 
//	{
//		setValue(Param.RANDOM, random);
//	}

	public String getAudience() 
	{
		return nvgm.getValue((GetName)Param.AUD);
	}

	public void setAudience(String aud) 
	{
		nvgm.add(Param.AUD, aud);
	}

	public String getIssuer()
	{
		return nvgm.getValue((GetName)Param.ISS);
	}

	public void setIssuer(String iss)
	{
		nvgm.add(Param.ISS, iss);
	}
	
	/**
     * @return the number of seconds from 1970-01-01T00:00:00Z UTC
     */
	public long getExpirationTime() 
	{
		return nvgm.getValue((GetName)Param.EXP);
	}

	/**
	 * Set expiration in seconds
	 * @param exp
	 */
	public void setExpirationTime(long exp) 
	{
		nvgm.add(new NVLong(Param.EXP.getName(), exp));
	}
	public void setExpirationTime(Date exp)
	{
		nvgm.add(new NVLong(Param.EXP.getName(), Const.TimeInMillis.SECOND.convertTo(exp)));
	}
	
	
	/**
     * @return the number of seconds from 1970-01-01T00:00:00Z UTC
     */
	public long getNotBefore() 
	{
		return nvgm.getValue((GetName)Param.NBF);
	}

	public void setNotBefore(long nbf) 
	{
		nvgm.add(new NVLong(Param.NBF.getName(), nbf));
	}
	public void setNotBefore(Date nbf)
	{
		nvgm.add(new NVLong(Param.NBF.getName(), Const.TimeInMillis.SECOND.convertTo(nbf)));
	}
	
	/**
	 * 
	 * @return the number of seconds from 1970-01-01T00:00:00Z UTC
	 */
	public long getIssuedAt() 
	{
		return nvgm.getValue((GetName)Param.IAT);
	}

	/**
	 * Set time in seconds NOT Millis
	 * @param iat
	 */
	public void setIssuedAt(long iat) 
    {
        nvgm.add(new NVLong(Param.IAT.getName(), iat));
    }
	public void setIssuedAt(Date iat)
	{
		nvgm.add(new NVLong(Param.IAT.getName(), Const.TimeInMillis.SECOND.convertTo(iat)));
	}
	


	public String getJWTID() 
	{
		return nvgm.getValue((GetName)Param.JTI);
	}

	public void setJWTID(String jti) 
	{
		nvgm.add(Param.JTI, jti);
	}
	
	
	
	
	
	public NVGenericMap getProperties()
    {
        return nvgm;    
    }
	
	public void setProperties(NVGenericMap nvgm)
	{
		this.nvgm.clear();
		SUS.updateGetNVProperties(this, nvgm);
	}

	
}
