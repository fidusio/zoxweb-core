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
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.crypto.CryptoConst.JWTAlgo;


public class JWTHeader
implements GetNVProperties
//    extends SetNameDescriptionDAO
{
	public enum Param
	    implements GetNVConfig, GetName
	{
		ALG(NVConfigManager.createNVConfig("alg", "Algorithm", "Alg", true, true, JWTAlgo.class)),
		CTY(NVConfigManager.createNVConfig("cty", "Content Type", "ContentType", false, false, String.class)),
		TYP(NVConfigManager.createNVConfig("typ", "Token type", "TokenType",false, false, String.class)),
		KID(NVConfigManager.createNVConfig("kid", "Key ID", "KeyID",false, false, String.class)),
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
	
	public static final NVConfigEntity NVC_JWT_HEADER = new NVConfigEntityLocal(
																					"jwt_header", 
																					null , 
																					"JWTHeader", 
																					true, 
																					false, 
																					false, 
																					false, 
																					JWTHeader.class, 
																					SharedUtil.extractNVConfigs(Param.values()), 
																					null, 
																					false, 
																					SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																				);

	private NVGenericMap nvgm;
	public JWTHeader(NVGenericMap nvgm)
	{
		//super(NVC_JWT_HEADER);
		this.nvgm = nvgm;
		
	}
	
	public JWTAlgo getJWTAlgorithm()
	{
		return JWTAlgo.valueOf((String)nvgm.getValue((GetName)Param.ALG));
	}
	
	public void setJWTAlgorithm(JWTAlgo type)
	{
		nvgm.add(Param.ALG, type.name());
		
	}
	
	public String getTokenType()
	{
		return nvgm.getValue((GetName)Param.TYP);
	}
	
	public void setTokenType(String type)
	{
		nvgm.add(Param.TYP, type);
	}
	
	
	public String getContentType()
	{
		return nvgm.getValue((GetName)Param.CTY);
	}
	
	public void setContentType(String contentType)
	{
		nvgm.add(Param.CTY, contentType);
	}


	public String getKeyID()
	{
		return nvgm.getValue((GetName)Param.KID);
	}

	public void setKeyID(String keyID)
	{
		nvgm.add(Param.KID, keyID);
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