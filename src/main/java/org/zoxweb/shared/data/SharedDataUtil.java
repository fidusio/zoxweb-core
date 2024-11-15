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
package org.zoxweb.shared.data;

import org.zoxweb.shared.filters.MatchPatternFilter;
import org.zoxweb.shared.filters.NVEntityFilter;
import org.zoxweb.shared.util.*;

import java.util.*;

/**
 * The SharedDataUtil class is a utility class.
 * @author mzebib
 */
public class SharedDataUtil 
{
	
	private SharedDataUtil()
	{
		
	}
	
	private static String fullPathName(List<FolderInfoDAO> folderList, NVEntity nve, char sep)
	{
		StringBuilder sb = new StringBuilder();
		
		for (FolderInfoDAO fid : folderList)
		{
			// take into account main, trash folder
			sb.append(fid.getName());
			sb.append(sep);
		}
		
		sb.append(nve.getName());
		
		return sb.toString();
	}
	
	private static Map<String, String> discover(Map<String, String> ret, FolderInfoDAO folder, List<FolderInfoDAO> folderList, NVEntity match, char sep)
	{
		SUS.checkIfNulls("null folder", folder);
		
		if (ret == null)
			ret = new HashMap<String, String>();
				
		if (folderList == null)
		{
			folderList = new ArrayList<FolderInfoDAO>();
			folderList.add(folder);
		}
		
					
		for (NVEntity nve: folder.getFolderContent().values())
		{
			String fullName =  fullPathName(folderList, nve, sep);
			ret.put(nve.getReferenceID(), fullName);
			if (nve instanceof FolderInfoDAO)
			{
				folderList.add((FolderInfoDAO)nve);
				discover(ret, (FolderInfoDAO)nve, folderList, match, sep);
				folderList.remove(folderList.size()-1);
			}
			
			
			if (match != null && ret.get(match.getReferenceID()) != null)
			{
				break;
			}
		}
		
		return ret;
	}
	
	
	private static Map<NVEntity, String> discoverNVE(List<NVEntity> matchResult, Map<NVEntity, String> ret, FolderInfoDAO folder, List<FolderInfoDAO> folderList, MatchPatternFilter match, char sep)
	{
		SUS.checkIfNulls("null folder", folder);
		if (ret == null)
			ret = new HashMap<NVEntity, String>();
				
		if (folderList == null)
		{
			folderList = new ArrayList<FolderInfoDAO>();
			//folderList.add(folder);
		}
		
					
		for (NVEntity nve: folder.getFolderContent().values())
		{
			String fullName =  fullPathName(folderList, nve, sep);
			if (!fullName.startsWith("/"))
			{
				fullName = "/" + fullName;
			}
			ret.put( nve, fullName);
			if (nve instanceof FolderInfoDAO)
			{
				folderList.add((FolderInfoDAO)nve);
				discoverNVE(matchResult, ret, (FolderInfoDAO)nve, folderList, match, sep);
				folderList.remove(folderList.size()-1);
			}
			
			
			if (match != null && match.match(fullName))
			{
				matchResult.add(nve);
				if (!match.isRecursive())
					break;
			}
		}
		
		return ret;
	}

	public static List<NVEntity> search(FolderInfoDAO entry, String ...args)
	{
		SUS.checkIfNulls("Null parameters", entry, args);
		List<NVEntity> matches = new ArrayList<NVEntity>();
		//if (!SharedStringUtil.isEmpty(match))
		{
			MatchPatternFilter mpf = MatchPatternFilter.createMatchFilter(args);
			//Map<NVEntity, String> matchMap = 
			discoverNVE(matches, null, entry, null, mpf, '/');
			
//			for (Map.Entry<NVEntity, String>  map : matchMap.entrySet())
//			{
//				//System.out.println(map.getValue());
//				if (mpf.match(map.getValue()))
//				{
//					matches.add(map.getKey());
//				}
//			}
		}
		
		return matches;
		
	}
	
	public static NVEntity lookupInFolder(FolderInfoDAO entry, String refID, boolean deepSearch)
	{
		SUS.checkIfNulls("Null parameters", entry, refID);
		
		ArrayValues<NVEntity> content = entry.getFolderContent();
		
		for (NVEntity nve : content.values())
		{
			if (refID.equals(nve.getReferenceID()))
			{
				return nve;
			}
			else if (nve instanceof FolderInfoDAO && deepSearch)
			{
				NVEntity temp = lookupInFolder((FolderInfoDAO) nve, refID, deepSearch);
				
				if (temp != null)
				{
					return temp;
				}
			}
		}
		
		return null;
	}
	
	
	public static Map<String, NVEntity> mapReferenceIDToNVEntity(Map<String, NVEntity> ret, FolderInfoDAO folder, List<FolderInfoDAO> folderList)
	{
		SUS.checkIfNulls("null folder", folder);
		if (ret == null)
		{
			ret = new HashMap<String, NVEntity>();
		}
				
		if (folderList == null)
		{
			folderList = new ArrayList<FolderInfoDAO>();
			folderList.add(folder);
		}
		
		for (NVEntity nve: folder.getFolderContent().values())
		{
			ret.put(nve.getReferenceID(), nve);
			if (nve instanceof FolderInfoDAO)
			{
				folderList.add((FolderInfoDAO)nve);
				mapReferenceIDToNVEntity(ret, (FolderInfoDAO)nve, folderList);
				folderList.remove(folderList.size()-1);
			}
		}
		
		return ret;
	}
	
	public static Map<String, FolderInfoDAO> mapReferenceIDToContainerFolder(Map<String, FolderInfoDAO> ret, FolderInfoDAO folder, List<FolderInfoDAO> folderList)
	{
		SUS.checkIfNulls("null folder", folder);
		if (ret == null)
		{
			ret = new HashMap<String, FolderInfoDAO>();
		}
				
		if (folderList == null)
		{
			folderList = new ArrayList<FolderInfoDAO>();
			folderList.add(folder);
		}
		
		for (NVEntity nve: folder.getFolderContent().values())
		{
			ret.put(nve.getReferenceID(), folder);
			if (nve instanceof FolderInfoDAO)
			{
				folderList.add((FolderInfoDAO)nve);
				mapReferenceIDToContainerFolder(ret, (FolderInfoDAO)nve, folderList);
				folderList.remove(folderList.size()-1);
			}
		}
		
		return ret;
	}
	
	public static List<NVEntity> searchByName(FolderInfoDAO folder, String ... args)
	{
		MatchPatternFilter mpf = MatchPatternFilter.createMatchFilter(args);
		
		return searchByName(null, folder, mpf);
	}
	
	private static List<NVEntity> searchByName(List<NVEntity> results, FolderInfoDAO folder, MatchPatternFilter mpf)
	{
		if (results == null)
		{
			results = new ArrayList<NVEntity>();
		}

		for (NVEntity nve : folder.getFolderContent().values())
		{
			if (nve != null)
			{
				if (nve.getName() != null && mpf.match(nve.getName()))
				{
					results.add(nve);
				}
				
				if (nve instanceof FolderInfoDAO && mpf.isRecursive())
				{
					searchByName(results, (FolderInfoDAO) nve, mpf);
				}
			}
		}
		
		return results;
	}
	
	public static NVEntity searchByRefID(FolderInfoDAO folder, String refID)
	{
		if (refID != null)
		{
			for (NVEntity nve : folder.getFolderContent().values())
			{
				if (nve != null)
				{
					if (nve.getReferenceID() != null && refID.equals(nve.getReferenceID()))
					{
						return nve;
					}
					
					if (nve instanceof FolderInfoDAO)
					{
						searchByRefID((FolderInfoDAO) nve, refID);
					}
				}
			}
		}
		
		return null;
	}
	
	public static FolderInfoDAO lookupContainingFolder(FolderInfoDAO entryFolder, NVEntity nve)
	{
		if (entryFolder != null && nve != null)
		{
			for (NVEntity v : entryFolder.getFolderContent().values())
			{
				
				if (nve.getReferenceID() != null && nve.getReferenceID().equals(v.getReferenceID()))
				{
					return entryFolder;
				}
				
				if (v instanceof FolderInfoDAO)
				{
					FolderInfoDAO ret = lookupContainingFolder((FolderInfoDAO) v, nve);
					
					if (ret != null)
					{
						return ret;
					}
				}
			}			
		}
		
		return null;
	}
	
	public static Set<NVEntity> searchFolderByNVConfigEntity(FolderInfoDAO folderInfo, NVConfigEntity ... nvces)
	{
		if (folderInfo != null)
		{
			Set<NVEntity> results = searchFolderByNVConfigEntity(null, folderInfo, new NVEntityFilter(nvces));
			
			if (results != null && results.size() > 0)
			{
				return results;
			}
		}
		
		return null;
	}
	
	private static Set<NVEntity> searchFolderByNVConfigEntity(Set<NVEntity> matches, FolderInfoDAO folderInfo, NVEntityFilter filter)
	{
		if (folderInfo != null && filter != null)
		{
			if (matches == null)
			{
				matches = new HashSet<NVEntity>();
			}
			
			for (NVEntity nve : folderInfo.getFolderContent().values())
			{
				if (nve != null)
				{
					if (filter.isValid(nve))
					{
						matches.add(nve);
					}
					
					if (nve instanceof FolderInfoDAO)
					{
						searchFolderByNVConfigEntity(matches, (FolderInfoDAO) nve, filter);
					}
					else if (nve instanceof FormInfoDAO)
					{
						FormInfoDAO formInfo = (FormInfoDAO) nve;
						
						if (formInfo.getFormReference() != null && filter.isValid(formInfo.getFormReference()))
						{
							matches.add(formInfo.getFormReference());
						}
					}
				}
			}
		}
		
		return matches;
	}
	
	public static String toCanonicalID(char sep, FolderInfoDAO folder, NVEntity nve)
	{
		Map<String, String> result = discover(null, folder, null, nve, sep);
		
		return result.get(nve.getReferenceID());
	}
	

	
	/**
	 * Returns the the short hand display of the given NVEntity.
	 * 
	 * @param nve
	 * @return short description of nve
	 */
	public static String getNVEntityShortHand(NVEntity nve)
	{
		String ret = null;
		
		if (nve != null)
		{
			if (nve instanceof AddressDAO)
			{
				AddressDAO address = (AddressDAO) nve;
				StringBuilder sb = new StringBuilder();
				
				if (!SUS.isEmpty(address.getName()) && !SUS.isEmpty(address.getStreet()))
				{
					sb.append(address.getName());
					sb.append(", ");
					sb.append(address.getStreet());
				}
				else if (!SUS.isEmpty(address.getStreet()))
				{
					sb.append(address.getStreet());
				}
				
				if (!SUS.isEmpty(address.getCity()))
				{
					sb.append(", ");
					sb.append(address.getCity());
				}
				
				if (!SUS.isEmpty(address.getStateOrProvince()))
				{
					sb.append(", ");
					sb.append(address.getStateOrProvince());
				}
				
				if (!SUS.isEmpty(address.getCountry()))
				{
					String country = address.getCountry();
					
					if (country.equals("USA"))
					{
						sb.append(", ");
						sb.append(country);
					}
					else
					{
						NVPair nvp = DataConst.COUNTRIES.lookup(country);
						
						if (nvp != null && nvp.getValue() != null)
						{
							country = nvp.getValue();
						}
						
						sb.append(", ");
						sb.append(country);
					}
				}
				
				if (!SUS.isEmpty(address.getZIPOrPostalCode()))
				{
					sb.append(", ");
					sb.append(address.getZIPOrPostalCode());
				}
				
				ret = sb.toString();
			}
			else if (nve instanceof PhoneDAO)
			{
				PhoneDAO phone = (PhoneDAO) nve;
				StringBuilder sb = new StringBuilder();
				
				if (!SUS.isEmpty(phone.getName()))
				{
					sb.append(phone.getName());
					sb.append(", ");
				}
					
				if (!SUS.isEmpty(phone.getCountryCode()))
				{
					sb.append(phone.getCountryCode());
				}
				
				if (!SUS.isEmpty(phone.getAreaCode()))
				{
					sb.append(" (" + phone.getAreaCode() + ") ");
				}
				
				if (!SUS.isEmpty(phone.getNumber()))
				{
					sb.append(phone.getNumber());
				}
				
				if (!SUS.isEmpty(phone.getExtension()))
				{
					sb.append(" ext. " + phone.getExtension());
				}
				
				ret = sb.toString();
			}
			else if (nve instanceof CreditCardDAO)
			{
				CreditCardDAO card = (CreditCardDAO) nve;
				
				if (!SUS.isEmpty(card.getName()) && card.getCardType() != null && card.getCardNumber() != null)
				{
					ret = card.getName() + ", " + card.getCardType().getDisplay() + " ending in " + getCreditCardLastFourDigits(card.getCardNumber());
				}
				else if (card.getCardType() != null && card.getCardNumber() != null)
				{
					ret = card.getCardType().getDisplay() + " ending in " + getCreditCardLastFourDigits(card.getCardNumber());
				}
			}
			else
			{
				ret = NVConfigMapUtil.toString(nve, null, false);
			}
		}
		
		return ret;
	}

	/**
	 * Gets the last four digits of a credit card number.
	 * Example: cardNumber = "4400 0000 0000 1234" returns "1234"
	 * 
	 * @param cardNumber
	 * @return last 4 digit of CC 
	 */
	public static String getCreditCardLastFourDigits(String cardNumber)
	{
		if (cardNumber != null)
		{
			if (cardNumber.length() == 4)
			{
				return cardNumber;
			}
			else if (cardNumber.length() > 4)
			{
				return cardNumber.substring(cardNumber.length() - 4, cardNumber.length());
			}
			
		}
		
		return null;
	}
	
	
	/**
	 * Return the reference id of all the contained refids
	 * @param entry 
	 * @return set of all the contained ref ids in an nventity
	 */
	public static Set<String> getAllContainedRefIDs(NVEntity entry)
	{
		return getAllContainedRefIDs(null, entry);
	}
	
	
	private static Set<String> getAllContainedRefIDs(Set<String> ret, NVEntity entry)
	{
		if (ret == null)
		{
			ret = new HashSet<String>();
		}
		
		
		if (entry != null)
		{
			if (entry.getReferenceID() != null)
			{
				ret.add(entry.getReferenceID());
			}
			
			NVConfigEntity nvce = (NVConfigEntity) entry.getNVConfig();
			
			for (NVConfig nvc : nvce.getAttributes())
			{
				if (nvc instanceof NVConfigEntity)
				{
					NVConfigEntity nvceChild = (NVConfigEntity) nvc;
					if (nvceChild.isArray())
					{
					
						@SuppressWarnings("unchecked")
						ArrayValues<NVEntity> toAdds = (ArrayValues<NVEntity>) entry.lookup(nvc);
						if (toAdds != null)
						{
							for (NVEntity toAdd : toAdds.values())
							{
								getAllContainedRefIDs(ret, toAdd);
							}
						}
					}
					else
					{
						getAllContainedRefIDs(ret, (NVEntity)entry.lookupValue(nvc));
					}
				}
			}	
		}
		
		return ret;
	}
	
	/***
	 * Converts credit card number to masked format.
	 * Example: Given card number = "4444000000001234", returns "XXXXXXXXXXXX1234".
	 * 
	 * @param cardNumber
	 * @return masked credit card number
	 */
	public static String maskCreditCardNumber(String cardNumber)
	{	
		if (cardNumber != null && cardNumber.length() > 4)
		{
			cardNumber = SharedStringUtil.trimOrNull(cardNumber);
			cardNumber = cardNumber.replaceAll("[ -]", "");
			
			String lastFourDigits = getCreditCardLastFourDigits(cardNumber);
			
			if (lastFourDigits != null)
			{
				int prefixLength = cardNumber.length() - 4;
				StringBuilder sb = new StringBuilder();
				
				for (int i = 0; i < prefixLength; i++)
				{
					sb.append("X");
				}
				
				sb.append(lastFourDigits);
				
				return sb.toString();
			}
		}
		
		return null;
	}

	/**
	 * Returns a copy of the given NVEntity.
	 * @param factory 
	 * @param nveToCopy - NVEntity object to copy
	 * @param deep - recursive call to check for inner array of NVEntity or NVEntity objects
	 * @param omitRefID - omit the reference ID (if true, sets reference ID of NVE object to null)
	 * @param omitUserID - omit the user ID (if true, sets user ID of NVE object to null)
	 * @return copied NVEntity
	 */
	@SuppressWarnings("unchecked")
	public static NVEntity copyNVEntity(NVEntityFactory factory, NVEntity nveToCopy, boolean deep, boolean omitRefID, boolean omitUserID) {
		
		SUS.checkIfNulls("NVEntity object to copy is null.", nveToCopy, factory);
		
		NVConfigEntity nvce = (NVConfigEntity) nveToCopy.getNVConfig();
		
		//	Create the NVEntity object to return based on given NVEntity type.
		NVEntity ret = factory.createNVEntity(nvce.getName());
		
		for (NVConfig nvc : nvce.getAttributes())
		{
			if (nvc.getName().equals(MetaToken.REFERENCE_ID.getName()) && omitRefID)
			{
				//	If reference ID should be omitted, ref ID is left null.
				//ret.setReferenceID(nveToCopy.getReferenceID());			
				continue;
			}
			else if (nvc.getName().equals(MetaToken.SUBJECT_GUID.getName()) && omitUserID)
			{
				// If user ID should be not be omitted, user ID is left null.
				//ret.setUserID(nveToCopy.getUserID());
				continue;
			}
			else if (nvc instanceof NVConfigEntity && deep)
			{
				if (((NVConfigEntity) nvc).isArray())
				{
					ArrayValues<NVEntity> toAdds = (ArrayValues<NVEntity>) nveToCopy.lookup(nvc);
					
					if (toAdds != null)
					{
						for (NVEntity toAdd : toAdds.values())
						{
							NVEntity copiedNVE = copyNVEntity(factory, toAdd, deep, omitRefID, omitUserID);
							((ArrayValues<NVEntity>) ret.lookup(nvc)).add(copiedNVE);
						}		
					}
				}
				else 
				{
					NVEntity value = nveToCopy.lookupValue(nvc);

					if (value != null)
					{
						ret.setValue(nvc, copyNVEntity(factory, value, deep, omitRefID, omitUserID));
					}
				}
			}
			else
            {
				if (nveToCopy.lookupValue(nvc) != null)
				{
					ret.setValue(nvc, nveToCopy.lookupValue(nvc));
				}
			}
		}
		
		return ret;	
	}
	
}