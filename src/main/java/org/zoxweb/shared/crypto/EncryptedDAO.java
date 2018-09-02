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
package org.zoxweb.shared.crypto;

import org.zoxweb.shared.data.TimeStampDAO;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVGenericMap;

import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SubjectID;

@SuppressWarnings("serial")
public class EncryptedDAO
    extends TimeStampDAO
    implements CryptoDAO,SubjectID<String>
{

	//private String algoName;
	//private String description;
//	private String subject[];// optional
//	
//	private String algoParameters[];// optional
//	private byte[] iv;// base64
//	private byte[] encryptedData; // base64 if string
//	private String expirationTime;// optional, long value or yyyy-mm-dd hh:mm:ss[.mmm] in GMT time zone only
	//private String hint;// optional
	//private String hmacAlgoName;
	//private byte[] hmac;// base64
	
	protected enum Param
        implements GetNVConfig
    {
		SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Optional subject ID", "SubjectID", false, true, String.class)),
		SUBJECT_PORPERTIES(NVConfigManager.createNVConfig("subject_properties", "Subject properties", "SubjectPropeties", false, true, NVGenericMap.class)),
		ALGO_PROPERTIES(NVConfigManager.createNVConfig("algo_properties", "Algorithm properties", "AlgorithmProperties", false, true, NVGenericMap.class)),
		IV(NVConfigManager.createNVConfig("iv", "Initialization vector", "IV", true, true, byte[].class)),
		DATA_LENGTH(NVConfigManager.createNVConfig("data_length", "The original data length in bytes", "DataLength", false, true, Long.class)),
		ENCRYPTED_DATA(NVConfigManager.createNVConfig("encrypted_data", "Encrypted data", "EncryptedData", true, true, byte[].class)),
		EXPIRATION_TIME(NVConfigManager.createNVConfig("expiration_time", "Expiration time", "ExpirationTime", false, true, String.class)),
		HINT(NVConfigManager.createNVConfig("hint", "Hint of the encrypted message", "Hint", false, true, String.class)),
		HMAC_ALOG_NAME(NVConfigManager.createNVConfig("hmac_algo_name", "The HMAC algorithm name", "HMACAlgoName", true, true, String.class)),
		HMAC(NVConfigManager.createNVConfig("hmac", "The HMAC Value", "HMAC", true, true, byte[].class)),
		HMAC_ALL(NVConfigManager.createNVConfig("hmac_all", "If true hmac will applied to global_id", "HMACAll", false, true, Boolean.class)),

		;

		private final NVConfig nvc;

        Param(NVConfig nvc)
        {
			this.nvc = nvc;
		}
		
		@Override
		public NVConfig getNVConfig()
        {
			return nvc;
		}	
	}

	public final static NVConfigEntity NVCE_ENCRYPTED_DAO = new NVConfigEntityLocal("encrypted_dao", null, "EncryptedDAO", false, true, false, false, EncryptedDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, TimeStampDAO.NVC_TIME_STAMP_DAO);

	public EncryptedDAO()
    {
		super(NVCE_ENCRYPTED_DAO);
	}

	protected EncryptedDAO( NVConfigEntity nvce)
    {
		super(nvce);
	}

	/**
	 * Get the list of principals
	 * @return null or the list
	 */
	public NVGenericMap  getSubjectProperties()
	{
		return (NVGenericMap) lookup(Param.SUBJECT_PORPERTIES);
	}
	
//	public void setSubjectProperties(ArrayValues<NVPair> subject)
//    {
//		if (subject == null)
//		{
//			getSubjectProperties().clear();
//		}
//		else
//        {
//			getSubjectProperties().add(subject.values(), true);
//		}
//	}


	public NVGenericMap getAlgoProperties()
    {
		return (NVGenericMap) lookup(Param.ALGO_PROPERTIES);
	}

//	public void setAlgoProperties(ArrayValues<NVPair> algo_parameters)
//    {
//		if (algo_parameters == null)
//		{
//			getAlgoProperties().clear();
//		}
//		else
//        {
//			getAlgoProperties().add(algo_parameters.values(), true);
//		}
//	}

	public byte[] getIV()
    {
		return lookupValue(Param.IV);
	}

	public void setIV(byte[] iv)
    {
		setValue(Param.IV, iv);
	}

	public byte[] getEncryptedData()
    {
		return lookupValue(Param.ENCRYPTED_DATA);
	}

	public void setEncryptedData(byte[] encrypted_data)
    {
		setValue(Param.ENCRYPTED_DATA, encrypted_data);
	}

	public String getExpirationTime()
    {
		return lookupValue(Param.EXPIRATION_TIME);
	}

	public void setExpirationTime(String expiration_time)
    {
		setValue(Param.EXPIRATION_TIME, expiration_time);
	}

	public String getHint()
    {
		return lookupValue(Param.HINT);
	}

	public void setHint(String hint)
    {
		setValue(Param.HINT, hint);
	}

	public String getHMACAlgoName()
    {
		return lookupValue(Param.HMAC_ALOG_NAME);
	}

	public void setHMACAlgoName(String hmac_algo_name)
    {
		setValue(Param.HMAC_ALOG_NAME, hmac_algo_name);
	}

	public byte[] getHMAC()
    {
		return lookupValue(Param.HMAC);
	}

	public void setHMAC(byte[] hmac)
    {
		setValue(Param.HMAC, hmac);
	}

	@Override
	public String toCanonicalID()
    {
		StringBuilder sb = new StringBuilder();
	
		sb.append(getName());
		sb.append(':');
		sb.append(getDescription());
		sb.append(':');
		sb.append(SharedUtil.toCanonicalID(',', (Object[])getSubjectProperties().values()));
		sb.append(':');
		sb.append(SharedUtil.toCanonicalID(',', (Object[])getAlgoProperties().values()));
		sb.append(':');
		sb.append(new String(SharedBase64.encode(getIV())));
		sb.append(':');
		sb.append(getDataLength());
		sb.append(':');
		sb.append(new String(SharedBase64.encode(getEncryptedData())));
		sb.append(':');
		sb.append(SharedStringUtil.trimOrEmpty(getExpirationTime()));
		sb.append(':');
		sb.append(SharedStringUtil.trimOrEmpty(getHint()));
		sb.append(':');
		sb.append(getHMACAlgoName());
		sb.append(':');
		sb.append(new String(SharedBase64.encode(getHMAC())));

		return sb.toString();
	}

	public long getDataLength()
    {
		return lookupValue(Param.DATA_LENGTH);
	}

	public void setDataLength(long data_length)
    {
		if (data_length < 0)
		{
			throw new IllegalArgumentException("Illegal data length " + data_length);
		}

		setValue(Param.DATA_LENGTH, data_length);
	}

	public static EncryptedDAO fromCanonicalID(String encryptedDAOCanonicalFormat)
        throws NullPointerException, IllegalArgumentException
    {
        if (SharedStringUtil.isEmpty(encryptedDAOCanonicalFormat))
        {
            throw new NullPointerException("empty dao");
        }

        String tokens[] = encryptedDAOCanonicalFormat.split(":");
        EncryptedDAO  ret = new EncryptedDAO();
        int index = 0;
        switch(tokens.length)
        {
        case 11:
            ret.setName(tokens[index++]);
            ret.setDescription(tokens[index++]);
            index++;// skip supbject prop
            index++;// skip algo prop
            ret.setIV(SharedBase64.decode(tokens[index++].getBytes()));
            ret.setDataLength(Long.parseLong(tokens[index++]));
            ret.setEncryptedData(SharedBase64.decode(tokens[index++].getBytes()));
            ret.setExpirationTime(tokens[index++]);
            ret.setHint(tokens[index++]);
            ret.setHMACAlgoName(tokens[index++]);
            ret.setHMAC(SharedBase64.decode(tokens[index++].getBytes()));

            break;
        default:
            throw new IllegalArgumentException("Invalid encrypted dao format");
        }

        return ret;
    }

	@Override
	public String getSubjectID() {
		// TODO Auto-generated method stub
		return lookupValue(Param.SUBJECT_ID);
	}

	@Override
	public void setSubjectID(String subjectID) {
		// TODO Auto-generated method stub
		setValue(Param.SUBJECT_ID, subjectID);
		
	}
	
	public boolean isHMACAll()
	{
	  return lookupValue(Param.HMAC_ALL);
	}
	
	public void setHMACAll(boolean hmacAll)
	{
	  setValue(Param.HMAC_ALL, hmacAll);
	}
	
}