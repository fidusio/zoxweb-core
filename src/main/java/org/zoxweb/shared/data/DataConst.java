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


import org.zoxweb.shared.util.DynamicEnumMap;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVPair;


public class DataConst 
{

	public static final Object[] EMPTY_VALUES = new Object[0];
    public enum Language
        implements GetName {

        ENGLISH("English"),
        SPANISH("Espanol"),

        ;

        private String name;

        Language(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
	
	public enum PhoneType 
	{
		HOME,
		WORK,
		MOBILE,
		FAX,
		OTHER,
		;
		
	}

	public enum DataOP
	{
		CREATE,
		DELETE,
		PATCH,
		READ,
		UPDATE,
	}
	
	public enum ContextAccess
	{
		// the context access is based on the account
		ACCOUNT_ID,
		// the context access is based on the domain
		DOMAIN_ID,
		// no context assoicated
		NONE,
		// the context access is based on the user
		USER_ID,
	}

	
	
	public enum SessionParam 
	implements GetName
	{
		FS_TOKEN("fstoken"),
		USER_ID("user_id"),
		SESSION_CACHE("session_cache"),
		SUBJECT("subject"),
		WEBSOCKET_SESSION("websocket_session"),
		LOCK_OBJECT("lock_object")
		
		;
		
		private String name;
		
		SessionParam(String name)
		{
			this.name = name;
		}
	
		@Override
		public String getName() 
		{
			return name;
		}
	
		public String toString()
		{
			return name;
		}
	}
	
	public enum DataParam
	    implements GetNVConfig
	{
		CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "Canonical ID", "CanonicalID", true, false, String.class)),
		UNIQUE_CANONICAL_ID(NVConfigManager.createNVConfig("canonical_id", "Canonical ID", "CanonicalID", true, false, true, String.class, null)),
		DESCRIPTION(NVConfigManager.createNVConfig("description", null, "Description", false, true, String.class)),
		DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "Domain ID", "DomainID", true, false, String.class)),
		NAME(NVConfigManager.createNVConfig("name", null,"Name", false, true, String.class)),
		UNIQUE_NAME(NVConfigManager.createNVConfig("name", null,"Name", true, true, true, String.class, null)),
		;
		
		private NVConfig nvc;
		
		DataParam(NVConfig nvc)
		{
            this.nvc = nvc;
		}

		public String toString()
		{
			return getNVConfig().getName();
		}
	
	
		/* (non-Javadoc)
		 * @see org.zoxweb.shared.util.GetNVConfig#getNVConfig()
		 */
		@Override
		public NVConfig getNVConfig()
		{
			return nvc;
		}
	}
	
	/**
	 * Dynamic enum map: Personal titles
	 */
	public final static DynamicEnumMap PERSONAL_TITLES =
			new DynamicEnumMap("Personal Titles",
					
					//	Mr.
					new NVPair("MR", "Mr."),
					//	Mrs.
					new NVPair("MRS", "Mrs."),
					//	Ms.
					new NVPair("MS", "Ms.")
					
			);
	

	/**
	 * Dynamic enum map: Gender
	 */
	public final static DynamicEnumMap GENDER =
			new DynamicEnumMap("Gender",
					
					//	Female
					new NVPair("F", "Female"),
					//	Male
					new NVPair("M", "Male")
			);

	
	/**
	 * Dynamic enum map: States, federal district, and territories of the United States.
	 */
	public final static DynamicEnumMap US_STATES = 
			new DynamicEnumMap("US States",
				
					//	Alabama
					new NVPair("AL", "Alabama"),
					//	Alaska
					new NVPair("AK", "Alaska"),
					//	Arizona
					new NVPair("AZ", "Arizona"),
					//	Arkansas
					new NVPair("AR", "Arkansas"),
					//	California
					new NVPair("CA", "California"),
					//	Colorado
					new NVPair("CO", "Colorado"),
					//	Connecticut
					new NVPair("CT", "Connecticut"),
					//	Delaware
					new NVPair("DE", "Delaware"),
					//	District of Columbia
					new NVPair("DC", "District of Columbia"),
					//	Florida
					new NVPair("FL", "Florida"),
					// 	Georgia
					new NVPair("GA", "Georgia"),
					// 	Hawaii
					new NVPair("HI", "Hawaii"),
					// 	Idaho
					new NVPair("ID", "Idaho"),
					// 	Illinois
					new NVPair("IL", "Illinois"),		
					// 	Indiana
					new NVPair("IN", "Indiana"),
					// 	Iowa
					new NVPair("IA", "Iowa"),
					// 	Kansas
					new NVPair("KS", "Kansas"),
					// 	Kentucky
					new NVPair("KY", "Kentucky"),
					//	Louisiana
					new NVPair("LA", "Louisiana"),
					// 	Maine
					new NVPair("ME", "Maine"),
					// 	Maryland
					new NVPair("MD", "Maryland"),
					// 	Massachusetts
					new NVPair("MA", "Massachusetts"),
					//	Michigan
					new NVPair("MI", "Michigan"),
					//	Minnesota
					new NVPair("MN", "Minnesota"),
					//	Mississippi
					new NVPair("MS", "Mississippi"),
					//	Missouri
					new NVPair("MO", "Missouri"),
					//	Montana
					new NVPair("MT", "Montana"),
					// 	Nebraska
					new NVPair("NE", "Nebraska"),
					// 	Nevada
					new NVPair("NV", "Nevada"),
					//	New Hampshire
					new NVPair("NH", "New Hampshire"),
					// 	New Jersey
					new NVPair("NJ", "New Jersey"),
					//	New Mexico
					new NVPair("NM", "New Mexico"),
					//	New York
					new NVPair("NY", "New York"),
					//	North Carolina
					new NVPair("NC", "North Carolina"),
					//	North Dakota
					new NVPair("ND", "North Dakota"),
					//	Ohio
					new NVPair("OH", "Ohio"),
					//	Oklahoma
					new NVPair("OK", "Oklahoma"),
					//	Oregon
					new NVPair("OR", "Oregon"),
					//	Pennsylvania
					new NVPair("PA", "Pennsylvania"),
					//	Rhode Island
					new NVPair("RI", "Rhode Island"),
					// 	South Carolina
					new NVPair("SC", "South Carolina"),
					//	South Dakota
					new NVPair("SD", "South Dakota"),
					//	Tennessee
					new NVPair("TN", "Tennessee"),
					//	Texas
					new NVPair("TX", "Texas"),
					//	Utah
					new NVPair("UT", "Utah"),
					// 	Vermont
					new NVPair("VT", "Vermont"),
					//	Virginia
					new NVPair("VA", "Virginia"),
					//	Washington
					new NVPair("WA", "Washington"),
					//	West Virginia
					new NVPair("WV", "West Virginia"),
					//	Wisconsin
					new NVPair("WI", "Wisconsin"),
					//	Wyoming
					new NVPair("WY", "Wyoming"),
					//	American Samoa
					new NVPair("AS", "American Samoa"),
					//	Guam
					new NVPair("GU", "Guam"),
					//	Northern Mariana Islands
					new NVPair("MP","Northern Mariana Islands"),
					//	Puerto Rico
					new NVPair("PR", "Puerto Rico"),
					//	U.S. Virgin Islands
					new NVPair("VI", "U.S. Virgin Islands")
			);

	/**
	 * Dynamic enum map: Provinces and territories of Canada
	 */
	public final static DynamicEnumMap CANADIAN_PROVINCES = 
			new DynamicEnumMap("Canadian Provinces",
				
					//	Alberta
					new NVPair("AB", "Alberta"),
					//	British Columbia
					new NVPair("BC", "British Columbia"),
					//	Manitoba
					new NVPair("MB", "Manitoba"),
					//	New Brunswick
					new NVPair("NB", "New Brunswick"),
					//	Newfoundland and Labrador 
					new NVPair("NL", "Newfoundland and Labrador"),
					//	Nova Scotia
					new NVPair("NS", "Nova Scotia"),
					//	Northwest Territories
					new NVPair("NT", "Northwest Territories"),
					//	Nunavut
					new NVPair("NU", "Nunavut"),
					//	Ontario
					new NVPair("ON", "Ontario"),
					//	Prince Edward Island
					new NVPair("PE", "Prince Edward Island"),
					//	Quebec
					new NVPair("QC", "Quebec"),
					//	Saskatchewan
					new NVPair("SK", "Saskatchewan"),
					//	Yukon
					new NVPair("YT", "Yukon")					
			);
	
	/**
	 * Dynamic enum map: Countries and regions
	 */
	public final static DynamicEnumMap COUNTRIES = 
			new DynamicEnumMap("Countries", 
					
					//  United States 						US 	USA
					new NVPair("USA", "United States"),
					//	Canada 								CA 	CAN
					new NVPair("CAN", "Canada"),
					
					//	A
					//	Afghanistan  						AF 	AFG
					new NVPair("AFG", "Afghanistan"), 
					//	Albania 							AL 	ALB
					new NVPair("ALB", "Albania"), 
					//	Algeria								DZ 	DZA
					new NVPair("DZA", "Algeria"), 
					//	American Samoa						AS 	ASM
					new NVPair("ASM", "American Samoa"),
					//	Andorra 							AD 	AND
					new NVPair("AND", "Andorra"), 
					//	Angola 								AO 	AGO 
					new NVPair("AGO", "Angola"), 
					//	Anguilla							AI	AIA
					new NVPair("AIA", "Anguilla"),
					//	Antigua and Barbuda 				AG 	ATG
					new NVPair("ATG", "Antigua and Barbuda "), 
					//	Argentina 							AR 	ARG
					new NVPair("ARG", "Argentina"), 
					//	Armenia 							AM 	ARM
					new NVPair("ARM", "Armenia"), 
					//	Aruba 								AW 	ABW
					new NVPair("ABW", "Aruba"), 
					//	Australia 							AU 	AUS
					new NVPair("AUS", "Australia"), 
					//	Austria 							AT 	AUT
					new NVPair("AUT", "Austria "), 
					//	Azerbaijan 							AZ 	AZE
					new NVPair("AZE", "Azerbaijan"), 
					
					//	B
					//	Bahamas 							BS 	BHS
					new NVPair("BHS", "Bahamas"), 
					//	Bahrain 							BH 	BHR 
					new NVPair("BHR", "Bahrain"), 
					//	Bangladesh 							BD 	BGD
					new NVPair("BGD", "Bangladesh"), 
					//	Barbados 							BB 	BRB 
					new NVPair("BRB", "Barbados"), 
					//	Belarus 							BY 	BLR
					new NVPair("BLR", "Belarus"), 
					//	Belgium 							BE 	BEL
					new NVPair("BEL", "Belgium"), 
					//	Belize 								BZ 	BLZ 
					new NVPair("BLZ", "Belize"), 
					//	Benin 								BJ 	BEN
					new NVPair("BEN", "Benin"),
					//	Bermuda								BM	BMU
					new NVPair("BMU", "Bermuda"),
					//	Bhutan 								BT 	BTN
					new NVPair("BTN", "Bhutan"), 
					//	Bolivia 							BO 	BOL
					new NVPair("BOL", "Bolivia"), 
					//	Bosnia and Herzegovina 				BA 	BIH
					new NVPair("BIH", "Bosnia and Herzegovina"), 
					//	Botswana 							BW 	BWA
					new NVPair("BWA", "Botswana"), 
					//	Brazil 								BR 	BRA
					new NVPair("BRA", "Brazil"), 
					//	British Virgin Islands				VG	VGB
					new NVPair("VGB", "British Virgin Islands"),
					//	Brunei Darussalam					BN 	BRN
					new NVPair("BRN", "Brunei Darussalam"), 
					//	Bulgaria 							BG 	BGR
					new NVPair("BGR", "Bulgaria "), 
					//	Burkina Faso						BF 	BFA
					new NVPair("BFA", "Burkina Faso"), 
					//	Burma (Myanmar) 					MM 	MMR
					new NVPair("MMR", "Burma"), 
					//	Burundi 							BI 	BDI
					new NVPair("BDI", "Burundi"), 
					
					//	C
					//	Cambodia 							KH 	KHM
					new NVPair("KHM", "Cambodia"), 
					//	Cameroon 							CM 	CMR 
					new NVPair("CMR", "Cameroon"), 
					//	Cape Verde 							CV 	CPV 
					new NVPair("CPV", "Cape Verde"), 
					//	Cayman Islands						KY	CYM
					new NVPair("CYM", "Cayman Islands"),					
					//	Central African Republic 			CF 	CAF
					new NVPair("CAF", "Central African Republic"), 
					//	Chad 								TD 	TCD 
					new NVPair("TCD", "Chad"), 
					//	Chile 								CL 	CHL
					new NVPair("CHL", "Chile"), 
					//	China 								CN 	CHN
					new NVPair("CHN", "China"), 
					//	Colombia 							CO 	COL
					new NVPair("COL", "Colombia"), 
					//	Comoros 							KM 	COM
					new NVPair("COM", "Comoros"), 
					//	Congo, Democratic Republic of the 	CD 	COD
					new NVPair("COD", "Congo, Democratic Republic of the"), 
					//	Congo, Republic of the 				CG 	COG
					new NVPair("COG", "Congo, Republic of the"), 
					//	Cook Islands						CK 	COK
					new NVPair("COK", "Cook Islands"),
					//	Costa Rica  						CR 	CRI 
					new NVPair("CRI", "Costa Rica"), 
					//	Cote d'Ivoire (Ivory Coast)			CI 	CIV 
					new NVPair("CIV", "Cote d'Ivoire (Ivory Coast)"), 
					//	Croatia 							HR 	HRV
					new NVPair("HRV", "Croatia"),
					//	Cyprus 								CY 	CYP 
					new NVPair("CYP", "Cyprus"), 
					//	Czech Republic 						CZ 	CZE
					new NVPair("CZE", "Czech Republic"), 
					
					//	D
					//	Denmark 							DK 	DNK
					new NVPair("DNK", "Denmark"), 
					//	Djibouti 							DJ	DJI
					new NVPair("DJI", "Djibouti"), 
					//	Dominica 							DM 	DMA
					new NVPair("DMA", "Dominica"), 
					//	Dominican Republic 					DO 	DOM
					new NVPair("DOM", "Dominican Republic "), 
				
					//	E
					//	Ecuador 							EC 	ECU
					new NVPair("ECU", "Ecuador"), 
					//	Egypt 								EG 	EGY
					new NVPair("EGY", "Egypt"), 
					//	El Salvador 						SV 	SLV
					new NVPair("SLV", "El Salvador"), 
					//	Equatorial Guinea 					GQ 	GNQ
					new NVPair("GNQ", "Equatorial Guinea"), 
					//	Eritrea 							ER	ERI
					new NVPair("ERI", "Eritrea"), 
					//	Estonia 							EE 	EST
					new NVPair("EST", "Estonia"), 
					//	Ethiopia 							ET 	ETH
					new NVPair("ETH", "Ethiopia"), 
					
					//	F
					//	Falkland Islands					FK	FLK
					new NVPair("FLK", "Falkland Islands"),
					//	Fiji 								FJ 	FJI 
					new NVPair("FJI", "Fiji"), 
					//	Finland 							FI 	FIN 
					new NVPair("FIN", "Finland"), 
					//	France 								FR 	FRA
					new NVPair("FRA", "France"), 
					//	French Guiana						GF	GUF
					new NVPair("GUF", "French Guiana"),
					//	French Polynesia					PF	PYF
					new NVPair("PYF", "French Polynesia"),
					
					
					//	G
					//	Gabon 								GA 	GAB 
					new NVPair("GAB", "Gabon"), 
					//	Gambia								GM 	GMB 
					new NVPair("GMB", "Gambia"), 
					//	Georgia 							GE 	GEO
					new NVPair("GEO", "Georgia"), 
					//	Germany 							DE 	DEU 
					new NVPair("DEU", "Germany"), 
					//	Ghana 								GH 	GHA
					new NVPair("GHA", "Ghana"), 
					//	Gibraltar							GI	GIB
					new NVPair("GIB", "Gibraltar"),
					//	Greece 								GR 	GRC
					new NVPair("GRC", "Greece"), 
					//	Greenland							GL	GRL
					new NVPair("GRL", "Greenland"),
					//	Grenada 							GD 	GRD
					new NVPair("GRD", "Grenada"), 
					//	Guadeloupe							GP	GLP
					new NVPair("GLP", "Guadeloupe"),					
					//	Guam								GU 	GUM
					new NVPair("GUM", "Guam"),
					//	Guatemala 							GT 	GTM 
					new NVPair("GTM", "Guatemala"), 
					//	Guinea 								GN 	GIN
					new NVPair("GIN", "Guinea "), 
					//	Guinea-Bissau 						GW 	GNB 
					new NVPair("GNB", "Guinea-Bissau"), 
					//	Guyana 								GY 	GUY
					new NVPair("GUY", "Guyana"), 
					
					//	H
					//	Haiti 								HT 	HTI
					new NVPair("HTI", "Haiti"), 
					//	Honduras 							HN 	HND
					new NVPair("HND", "Honduras"), 
					//	Hong Kong 							HK 	HKG
					new NVPair("HKG", "Hong Kong"), 
					//	Hungary 							HU 	HUN
					new NVPair("HUN", "Hungary"), 
					
					//	I
					//	Iceland 							IS 	ISL
					new NVPair("ISL", "Iceland"), 
					//	India 								IN 	IND
					new NVPair("IND", "India"), 
					//	Indonesia 							ID 	IDN 
					new NVPair("IDN", "Indonesia"), 
					//	Iraq  								IQ 	IRQ
					new NVPair("IRQ", "Iraq"), 
					//	Ireland 							IE 	IRL
					new NVPair("IRL", "Ireland"), 
					//	Israel 								IL 	ISR
					new NVPair("ISR", "Israel"), 
					//	Italy  								IT 	ITA
					new NVPair("ITA", "Italy"), 
					
					//	J
					//  Jamaica								JM 	JAM
					new NVPair("JAM", "Jamaica"), 
					//  Japan 								JP 	JPN 
					new NVPair("JPN", "Japan"), 
					//  Jordan 								JO 	JOR
					new NVPair("JOR", "Jordan"), 
					
					//	K
					//  Kazakhstan 							KZ 	KAZ
					new NVPair("KAZ", "Kazakhstan"), 
					//  Kenya 								KE 	KEN
					new NVPair("KEN", "Kenya"), 
					//  Kiribati 							KI 	KIR
					new NVPair("KIR", "Kiribati "),
					//  Korea, South 						KR 	KOR 
					new NVPair("KOR", "Korea, Republic of"), 
					//  Kuwait 								KW 	KWT
					new NVPair("KWT", "Kuwait"), 
					//  Kyrgyzstan 							KG 	KGZ
					new NVPair("KGZ", "Kyrgyzstan"), 
					
					//	L
					//	Laos 								LA 	LAO
					new NVPair("LAO", "Laos"), 
					//	Latvia 								LV 	LVA 
					new NVPair("LVA", "Latvia"), 
					//	Lebanon 							LB 	LBN
					new NVPair("LBN", "Lebanon"), 
					//	Lesotho 							LS 	LSO 
					new NVPair("LSO", "Lesotho"), 
					//	Liberia 							LR 	LBR 
					new NVPair("LBR", "Liberia"), 
					//	Libya 								LY 	LBY
					new NVPair("LBY", "Libya"), 
					//	Liechtenstein 						LI 	LIE
					new NVPair("LIE", "Liechtenstein"), 
					//	Lithuania 							LT 	LTU
					new NVPair("LTU", "Lithuania"), 
					//	Luxembourg 							LU 	LUX
					new NVPair("LUX", "Luxembourg"), 
					
					//	M
					//	Macau 								MO 	MAC 
					new NVPair("MAC", "Macau"), 
					//	Macedonia 							MK 	MKD
					new NVPair("MKD", "Macedonia"), 
					//	Madagascar  						MG 	MDG
					new NVPair("MDG", "Madagascar"), 
					//	Malawi 								MW 	MWI
					new NVPair("MWI", "Malawi"), 
					//	Malaysia 							MY 	MYS 
					new NVPair("MYS", "Malaysia"), 
					//	Maldives							MV 	MDV
					new NVPair("MDV", "Maldives"), 
					//	Mali								ML 	MLI
					new NVPair("MLI", "Mali"), 
					//	Malta 								MT 	MLT
					new NVPair("MLT", "Malta"), 
					//	Marshall Islands 					MH 	MHL
					new NVPair("MHL", "Marshall Islands"), 
					//	Mauritania 							MR 	MRT
					new NVPair("MRT", "Mauritania"), 
					//	Mauritius 							MU 	MUS 
					new NVPair("MUS", "Mauritius"), 
					//	Mexico  							MX 	MEX
					new NVPair("MEX", "Mexico"), 
					//	Micronesia 							FM 	FSM
					new NVPair("FSM", "Micronesia"), 
					//	Moldova 							MD 	MDA 
					new NVPair("MDA", "Moldova"), 
					//	Monaco 								MC 	MCO
					new NVPair("MCO", "Monaco"), 
					//	Mongolia 							MN 	MNG
					new NVPair("MNG", "Mongolia"), 
					//	Montenegro 							ME 	MNE
					new NVPair("MNE", "Montenegro"), 
					//	Morocco  							MA 	MAR
					new NVPair("MAR", "Morocco"), 
					//	Mozambique 							MZ 	MOZ
					new NVPair("MOZ", "Mozambique"), 
					
					//	N
					//	Namibia 							NA 	NAM
					new NVPair("NAM", "Namibia"), 
					//	Nauru 								NR 	NRU
					new NVPair("NRU", "Nauru"), 
					//	Nepal 								NP 	NPL
					new NVPair("NPL", "Nepal"), 
					//	Netherlands 						NL 	NLD 
					new NVPair("NLD", "Netherlands"), 
					//	Netherlands Antilles 				AN 	ANT
					new NVPair("ANT", "Netherlands Antilles"), 
					//	New Zealand 						NZ 	NZL
					new NVPair("NZL", "New Zealand"), 
					//	Nicaragua 							NI 	NIC
					new NVPair("NIC", "Nicaragua"), 
					//	Niger 								NE 	NER
					new NVPair("NER", "Niger"), 
					//	Nigeria 							NG 	NGA 
					new NVPair("NGA", "Nigeria"), 
					//	Norway 								NO 	NOR 
					new NVPair("NOR", "Norway"), 
					
					//	O
					// Oman 								OM 	OMN 
					new NVPair("OMN", "Oman"), 
					
					//	P
					//	Pakistan 							PK 	PAK 
					new NVPair("PAK", "Pakistan"), 
					//	Palau 								PW 	PLW 
					new NVPair("PLW", "Palau"), 
					//	Panama 								PA 	PAN
					new NVPair("PAN", "Panama"), 
					//	Papua New Guinea 					PG 	PNG
					new NVPair("PNG", "Papua New Guinea"), 
					//	Paraguay 							PY 	PRY 
					new NVPair("PRY", "Paraguay"), 
					//	Peru 								PE 	PER
					new NVPair("PER", "Peru"), 
					//	Philippines 						PH 	PHL
					new NVPair("PHL", "Philippines"), 
					//	Poland 								PL 	POL
					new NVPair("POL", "Poland"), 
					//	Portugal 							PT 	PRT
					new NVPair("PRT", "Portugal"), 
					//	Puerto Rico							PR	PRI
					new NVPair("PRI", "Puerto Rico"),
					
					//	Q
					//	Qatar 								QA 	QAT 
					new NVPair("QAT", "Qatar"), 
					
					//	R
					//	Romania 							RO 	ROU 
					new NVPair("ROU", "Romania"), 
					//	Russian Federation					RU 	RUS 
					new NVPair("RUS", "Russian Federation"), 
					//	Rwanda 								RW 	RWA 
					new NVPair("RWA", "Rwanda"), 
					
					//	S
					//	Saint Kitts and Nevis 				KN 	KNA
					new NVPair("KNA", "Saint Kitts and Nevis"), 
					//	Saint Lucia 						LC 	LCA 
					new NVPair("LCA", "Saint Lucia "), 
					//	Saint Martin						MF	MAF
					new NVPair("MAF", "Saint Martin"), 
					//	Saint Vincent and the Grenadines 	VC 	VCT 
					new NVPair("VCT", "Saint Vincent and the Grenadines"), 
					//	Samoa 								WS 	WSM 
					new NVPair("WSM", "Samoa "), 
					//	San Marino 							SM 	SMR
					new NVPair("SMR", "San Marino"), 
					//	Sao Tome and Principe 				ST 	STP
					new NVPair("STP", "Sao Tome and Principe"), 
					//	Saudi Arabia 						SA 	SAU
					new NVPair("SAU", "Saudi Arabia"), 
					//	Senegal  							SN 	SEN
					new NVPair("SEN", "Senegal"), 
					//	Serbia 								RS 	SRB 
					new NVPair("SRB", "Serbia"), 
					//	Seychelles 							SC 	SYC
					new NVPair("SYC", "Seychelles"), 
					//	Sierra Leone 						SL 	SLE
					new NVPair("SLE", "Sierra Leone"), 
					//	Singapore 							SG 	SGP 
					new NVPair("SGP", "Singapore"), 
					//	Sint Maarten 						SX 	SXM
					new NVPair("SXM", "Sint Marteen"), 
					//	Slovakia 							SK 	SVK
					new NVPair("SVK", "Slovakia"), 
					//	Slovenia 							SI 	SVN
					new NVPair("SVN", "Slovenia"), 
					//	Solomon Islands 					SB 	SLB 
					new NVPair("SLB", "Solomon Islands"), 
					//	Somalia 							SO 	SOM 
					new NVPair("SOM", "Somalia"), 
					//	South Africa 						ZA 	ZAF
					new NVPair("ZAF", "South Africa"), 
					//	South Sudan 						SS 	SSD 
					new NVPair("SSD", "South Sudan"), 
					//	Spain 								ES 	ESP
					new NVPair("ESP", "Spain"), 
					//	Sri Lanka 							LK 	LKA 
					new NVPair("LKA", "Sri Lanka"),
					//	Suriname  							SR 	SUR 
					new NVPair("SUR", "Suriname"), 
					//	Swaziland 							SZ 	SWZ
					new NVPair("SWZ", "Swaziland"), 
					//	Sweden  							SE 	SWE
					new NVPair("SWE", "Sweden"), 
					//	Switzerland  						CH 	CHE 
					new NVPair("CHE", "Switzerland"),
				
					//	T
					//	Taiwan								TW 	TWN
					new NVPair("TWN", "Taiwan"), 
					//	Tajikistan 							TJ 	TJK 
					new NVPair("TJK", "Tajikistan"), 
					//	Tanzania 							TZ 	TZA 
					new NVPair("TZA", "Tanzania"), 
					//	Thailand 							TH 	THA 
					new NVPair("THA", "Thailand"), 
					//	Timor-Leste (East Timor) 			TL 	TLS 
					new NVPair("TLS", "Timor-Leste (East Timor)"), 
					//	Togo 								TG 	TGO
					new NVPair("TGO", "Togo"), 
					//	Tonga 								TO 	TON 
					new NVPair("TON", "Tonga"), 
					//	Trinidad and Tobago 				TT 	TTO
					new NVPair("TTO", "Trinidad and Tobago"), 
					//	Tunisia 							TN 	TUN
					new NVPair("TUN", "Tunisia"), 
					//	Turkey  							TR 	TUR
					new NVPair("TUR", "Turkey"), 
					//	Turkmenistan 						TM 	TKM
					new NVPair("TKM", "Turkmenistan"), 
					//	Tuvalu 								TV 	TUV
					new NVPair("TUV", "Tuvalu"), 
					
					//	U
					//	Uganda 								UG 	UGA 
					new NVPair("UGA", "Uganda"), 
					//	Ukraine 							UA 	UKR
					new NVPair("UKR", "Ukraine"), 
					//	United Arab Emirates 				AE 	ARE
					new NVPair("ARE", "United Arab Emirates"), 
					//	United Kingdom 						GB 	GBR
					new NVPair("GBR", "United Kingdom"), 
					//	Uruguay 							UY 	URY 
					new NVPair("URY", "Uruguay"), 
					// 	U.S. Virgin Islands					VI	VIR
					new NVPair("VIR", "U.S. Virgin Islands"),
					//	Uzbekistan 							UZ 	UZB
					new NVPair("UZB", "Uzbekistan"), 
					
					//	V
					//	Vanuatu 							VU 	VUT
					new NVPair("VUT", "Vanuatu"),
					//	Vatican City State (Holy See)		VA 	VAT
					new NVPair("VAT", "Vatican City State"), 
					//	Venezuela 							VE 	VEN 
					new NVPair("VEN", "Venezula"), 
					//	Vietnam 							VN 	VNM 
					new NVPair("VNM", "Vietnam"), 
					
					//	Y
					//	Yemen 								YE 	YEM
					new NVPair("YEM", "Yemen"), 
					
					//	Z
					//	Zambia 								ZM 	ZMB 
					new NVPair("ZMB", "Zambia"), 
					//	Zimbabwe 							ZW 	ZWE 
					new NVPair("ZWE", "Zimbabwe")
			
			
				// Countries not included
				//	//	Cuba 								CU 	CUB
				//	new NVPair("CUB", "Cuba"), 
				//	//	Iran 								IR 	IRN
				//	new NVPair("IRN", "Iran"),
				//	//  Korea, North 						KP 	PRK
				//	new NVPair("PRK", "Korea, North"), 
				//	//	Syria 								SY 	SYR
				//	new NVPair("SYR", "Syria"),
				//	Sudan  									SD 	SDN 
				//	new NVPair("SDN", "Sudan"), 
			
			);

	
	

	/**
	 * Dynamic enum map: Phone types
	 */
	public final static DynamicEnumMap PHONE_TYPES = 
			new DynamicEnumMap("Phone Types",
					
					//	Home
					new NVPair("HOME", "Home"),
					//	Work
					new NVPair("WORK", "Work"),
					//	Mobile
					new NVPair("MOBILE", "Mobile"),
					//	Fax
					new NVPair("FAX", "Fax"),
					//	Other
					new NVPair("OTHER", "Other")
			);
	
	/**
	 * Dynamic enum map: Country codes
	 */
	public final static DynamicEnumMap COUNTRY_CODES = 
			new DynamicEnumMap("Country Codes",
							
					// 	+1 		North America (United States and Canada)
					new NVPair("+1", "+1 (United States and Canada)"),
					
					// 	+93		Afghanistan
					new NVPair("+93", "+93 (Afghanistan)"),
					//	+355 	Albania
					new NVPair("+355", "+93 (Albania)"),
					//	+213 	Algeria
					new NVPair("+213", "+213 (Algeria)"),
					//	+376 	Andorra
					new NVPair("+376", "+376 (Andorra)"),
					//	+244 	Angola
					new NVPair("+244", "+244 (Angola)"),
					//	+1268 	Antigua & Barbuda
					new NVPair("+1268", "+1268 (Antigua & Barbuda)"),
					//	+54 	Argentina
					new NVPair("+54", "+54 (Argentina)"),
					//	+374 	Armenia
					new NVPair("+374", "+374 (Armenia)"),
					//	+61 	Australia
					new NVPair("+61", "+61 (Australia)"),
					//	+43 	Austria
					new NVPair("+43", "+43 (Austria)"),
					//	+994 	Azerbaijan
					new NVPair("+994", "+994 (Azerbaijan)"),
					
					//	+1242 	Bahamas
					new NVPair("+1242", "+1242 (Bahamas)"),
					//	+1246 	Barbados
					new NVPair("+1246", "+1246 (Barbados)"),
					//	+973 	Bahrain
					new NVPair("+973", "+973 (Bahrain)"),
					//	+880 	Bangladesh
					new NVPair("+880", "+880 (Bangladesh)"),
					//	+375 	Belarus
					new NVPair("+375", "+375 (Belarus)"),
					//	+32 	Belgium
					new NVPair("+32", "+32 (Belgium)"),
					//	+501 	Belize
					new NVPair("+501", "+501 (Belize)"),
					//	+229 	Benin
					new NVPair("+229", "+229 (Benin)"),
					//	+975 	Bhutan
					new NVPair("+975", "+975 (Bhutan)"),
					//	+591 	Bolivia
					new NVPair("+591", "+591 (Bolivia)"),
					//	+387 	Bosnia and Herzegovina
					new NVPair("+387", "+387 (Bosnia and Herzegovina)"),
					//	+267 	Botswana
					new NVPair("+267", "+267 (Botswana)"),
					//	+55 	Brazil
					new NVPair("+55", "+55 (Brazil)"),
					//	+673 	Brunei
					new NVPair("+673", "+673 (Brunei)"),
					//	+359 	Bulgaria
					new NVPair("+359", "+359 (Bulgaria)"),
					//	+226 	Burkina Faso
					new NVPair("+226", "+226 (Burkina Faso)"),
					//	+257 	Burundi
					new NVPair("+257", "+257 (Burundi)"),
					
					//	+855 	Cambodia
					new NVPair("+855", "+855 (Cambodia)"),
					//	+237 	Cameroon
					new NVPair("+237", "+237 (Cameroon)"),
					//	+238 	Cape Verde
					new NVPair("+238", "+238 (Cape Verde)"),
					//	+236 	Central African Republic
					new NVPair("+236", "+236 (Central African Republic)"),
					//	+235 	Chad
					new NVPair("+235", "+235 (Chad)"),
					//	+56 	Chile
					new NVPair("+56", "+56 (Chile)"),
					//	+86 	China
					new NVPair("+86", "+86 (China)"),
					//	+57 	Colombia
					new NVPair("+57", "+57 (Columbia)"),
					//	+269 	Comoros
					new NVPair("+269", "+269 (Comoros)"),
					//	+242 	Congo
					new NVPair("+242", "+242 (Congo)"),
					//	+243 	Congo, Democratic Republic of the
					new NVPair("+243", "+243 (Congo, Democratic Republic of the)"),
					//	+506 	Costa Rica
					new NVPair("+506", "+506 (Costa Rica)"),
					//	+225 	Cote d'Ivoire
					new NVPair("+225", "+225 (Cote d'Ivoire)"),
					//	+385 	Croatia
					new NVPair("+385", "+385 (Croatia)"),
					//	+53 	Cuba
					new NVPair("+53", "+53 (Cuba)"),
					//	+357 	Cyprus
					new NVPair("+357", "+357 (Cyprus)"),
					//	+420	Czech Republic
					new NVPair("+420", "+420 (Czech Republic)"),
					
					//	+45 	Denmark
					new NVPair("+45", "+45 (Denmark)"),
					//	+253 	Djibouti
					new NVPair("+253", "+253 (Djibouti)"),
					//	+1767 	Dominca
					new NVPair("+1767", "+1767 (Dominca)"),
					//	+1809 	Dominican Republic
					new NVPair("+1809", "+1809 (Dominican Republic)"),
						
					//	+593 	Ecuador
					new NVPair("+593", "+593 (Ecuador)"),
					//	+670	East Timor
					new NVPair("+670", "+670 (East Timor)"),
					//	+20 	Egypt
					new NVPair("+20", "+20 (Egypt)"),
					//	+503 	El Salvador
					new NVPair("+503", "+503 (El Salvador)"),
					//	+240 	Equatorial Guinea
					new NVPair("+240", "+240 (Equatorial Guinea)"),
					//	+291 	Eritrea
					new NVPair("+291", "+291 (Eritrea)"),
					//	+372 	Estonia
					new NVPair("+372", "+372 (Estonia)"),
					//	+251 	Ethiopia
					new NVPair("+251", "+251 (Ethiopia)"),
					
					//	+679 	Fiji
					new NVPair("+679", "+679 (Fiji)"),
					//	+358 	Finland
					new NVPair("+358", "+358 (Finland)"),
					//	+33 	France
					new NVPair("+33", "+33 (France)"),
					
					//	+241 	Gabon
					new NVPair("+241", "+241 (Gabon)"),
					//	+220 	Gambia
					new NVPair("+220", "+220 (Gambia)"),
					//	+995 	Georgia
					new NVPair("+995", "+995 (Georgia)"),
					//	+49 	Germany
					new NVPair("+49", "+49 (Germany)"),
					//	+233 	Ghana
					new NVPair("+233", "+233 (Ghana)"),
					//	+30 	Greece
					new NVPair("+30", "+30 (Greece)"),
					//	+1473 	Grenada
					new NVPair("+1473", "+1473 (Grenada)"),
					//	+502 	Guatemala
					new NVPair("+502", "+502 (Guatemala)"),
					//	+224 	Guinea
					new NVPair("+224", "+224 (Guinea)"),
					//	+245 	Guinea-Bissau
					new NVPair("+245", "+245 (Guinea-Bissau)"),
					//	+592 	Guyana
					new NVPair("+592", "+592 (Guyana)"),
					
					//	+509 	Haiti
					new NVPair("+509", "+509 (Haiti)"),
					//	+504 	Honduras
					new NVPair("+504", "+504 (Honduras)"),
					//	+36 	Hungary
					new NVPair("+36", "+36 (Hungary)"),	

					//	+354 	Iceland
					new NVPair("+354", "+354 (Iceland)"),
					//	+91 	India
					new NVPair("+91", "+91 (India)"),
					//	+62 	Indonesia
					new NVPair("+62", "+62 (Indonesia)"),
					//	+964 	Iraq
					new NVPair("+964", "+964 (Iraq)"),
					//	+353 	Ireland
					new NVPair("+353", "+353 (Ireland)"),
					//	+972 	Israel
					new NVPair("+972", "+972 (Israel)"),
					//	+39 	Italy
					new NVPair("+39", "+39 (Italy)"),
					
					//	+1876 	Jamaica
					new NVPair("+1876", "+1876 (Jamaica)"),
					//	+81 	Japan
					new NVPair("+81", "+81 (Japan)"),
					//	+962 	Jordan
					new NVPair("+962", "+962 (Jordan)"),

					//	+7 		Kazakhstan
					new NVPair("+7", "+7 (Kazakhstan)"),
					//	+254 	Kenya
					new NVPair("+254", "+254 (Kenya)"),
					//	+686 	Kiribati
					new NVPair("+686", "+686 (Kiribati)"),
					//	+82 	Korea, Republic of
					new NVPair("+82", "+82 (Korea, Republic of)"),
					//	+965 	Kuwait
					new NVPair("+965", "+965 (Kuwait)"),
					//	+996 	Kyrgyzstan
					new NVPair("+996", "+996 (Kyrgyzstan)"),
					
					//	+856 	Laos
					new NVPair("+856", "+856 (Laos)"),
					//	+371 	Latvia
					new NVPair("+371", "+371 (Latvia)"),
					//	+961 	Lebanon
					new NVPair("+961", "+961 (Lebanon)"),
					//	+266 	Lesotho
					new NVPair("+266", "+266 (Lesotho)"),
					//	+231 	Liberia
					new NVPair("+231", "+231 (Liberia)"),
					//	+218 	Libya
					new NVPair("+218", "+218 (Libya)"),
					//	+423 	Liechtenstein
					new NVPair("+423", "+423 (Liechtenstein)"),
					//	+370 	Lithuania
					new NVPair("+370", "+370 (Lithuania)"),
					//	+352 	Luxembourg
					new NVPair("+352", "+352 (Luxembourg)"),
					
					//	+389 	Macedonia
					new NVPair("+389", "+389 (Macedonia)"),
					//	+261 	Madagascar
					new NVPair("+261", "+261 (Madagascar)"),
					//	+265 	Malawi
					new NVPair("+265", "+265 (Malawi)"),
					//	+60 	Malaysia
					new NVPair("+60", "+60 (Malaysia)"),
					//	+960 	Maldives
					new NVPair("+960", "+960 (Maldives)"),
					//	+223 	Mali
					new NVPair("+223", "+223 (Mali)"),
					//	+356 	Malta
					new NVPair("+356", "+356 (Malta)"),
					//	+692 	Marshall Islands
					new NVPair("+692", "+692 (Marshall Islands)"),
					//	+222 	Mauritania
					new NVPair("+222", "+222 (Mauritania)"),
					//	+230 	Mauritius
					new NVPair("+230", "+230 (Mauritius)"),
					//	+52 	Mexico
					new NVPair("+52", "+52 (Mexico)"),
					//	+691 	Micronesia
					new NVPair("+691", "+691 (Micronesia)"),
					//	+373 	Moldova
					new NVPair("+373", "+373 (Moldova)"),
					//	+377 	Monaco
					new NVPair("+377", "+377 (Monaco)"),
					//	+976 	Mongolia
					new NVPair("+976", "+976 (Mongolia)"),
					//	+382 	Montenegro
					new NVPair("+382", "+382 (Montenegro)"),
					//	+212 	Morocco
					new NVPair("+212", "+212 (Morocco)"),
					//	+258 	Mozambique
					new NVPair("+258", "+258 (Mozambique)"),
					//	+95 	Myanmar
					new NVPair("+95", "+95 (Myanmar)"),
					
					//	+264 	Namibia
					new NVPair("+264", "+264 (Namibia)"),
					//	+674 	Nauru
					new NVPair("+674", "+674 (Nauru)"),
					//	+977 	Nepal
					new NVPair("+977", "+977 (Nepal)"),
					//	+31 	Netherlands
					new NVPair("+31", "+31 (Netherlands)"),
					//	+64 	New Zealand
					new NVPair("+64", "+64 (New Zealand)"),
					//	+505 	Nicaragua
					new NVPair("+505", "+505 (Nicaragua)"),
					//	+227 	Niger
					new NVPair("+227", "+227 (Niger)"),
					//	+234 	Nigeria
					new NVPair("+234", "+234 (Nigeria)"),
					//	+47 	Norway
					new NVPair("+47", "+47 (Norway)"),
					
					//	+968 	Oman
					new NVPair("+968", "+968 (Oman)"),
					
					//	+92 	Pakistan
					new NVPair("+92", "+92 (Pakistan)"),
					//	+680 	Palau
					new NVPair("+680", "+680 (Palau)"),
					//	+507 	Panama
					new NVPair("+507", "+507 (Panama)"),
					//	+675 	Papua New Guinea
					new NVPair("+675", "+675 (Papua New Guinea)"),
					//	+595 	Paraguay
					new NVPair("+595", "+595 (Paraguay)"),
					//	+51 	Peru
					new NVPair("+51", "+51 (Peru)"),
					//	+63 	Philippines
					new NVPair("+63", "+63 (Philippines)"),
					//	+48 	Poland
					new NVPair("+48", "+48 (Poland)"),
					//	+351 	Portugal
					new NVPair("+351", "+351 (Portugal)"),
					
					//	+974 	Qatar
					new NVPair("+974", "+974 (Qatar)"),	
					//	+40 	Romania
					new NVPair("+40", "+40 (Romania)"),
					//	+7 		Russia
					new NVPair("+7", "+7 (Russia)"),
					//	+250 	Rwanda
					new NVPair("+250", "+250 (Rwanda)"),
					
					//	+1869 	St. Kitts & Nevis
					new NVPair("+1869", "+1869 (St. Kitts & Nevis)"),
					//	+1758 	St. Lucia
					new NVPair("+1758", "+1758 (St. Lucia)"),
					//	+1784 	St. Vincent & The Grenadines
					new NVPair("+1784", "+1784 (St. Vincent & The Grenadines)"),
					//	+685 	Samoa
					new NVPair("+685", "+685 (Samoa)"),
					//	+378 	San Marino
					new NVPair("+378", "+378 (San Marino)"),
					//	+239 	Sao Tome and Principe
					new NVPair("+239", "+239 (Sao Tome and Principe)"),
					//	+966 	Saudi Arabia
					new NVPair("+966", "+966 (Saudi Arabia)"),
					//	+221 	Senegal
					new NVPair("+221", "+221 (Senegal)"),
					//	+381 	Serbia
					new NVPair("+381", "+381 (Serbia)"),
					//	+248 	Seychelles
					new NVPair("+248", "+248 (Seychelles)"),
					//	+232 	Sierra Leone
					new NVPair("+232", "+232 (Sierra Leone)"),
					//	+65 	Singapore
					new NVPair("+65", "+65 (Singapore)"),
					//	+421 	Slovakia
					new NVPair("+421", "+421 (Slovakia)"),
					//	+386 	Slovenia
					new NVPair("+386", "+386 (Slovenia)"),
					//	+677 	Solomon Islands
					new NVPair("+677", "+677 (Solomon Islands)"),
					//	+252 	Somalia
					new NVPair("+252", "+252 (Somalia)"),
					//	+27 	South Africa
					new NVPair("+27", "+27 (South Africa)"),
					//	+249 	South Sudan
					new NVPair("+249", "+249 (South Sudan)"),
					//	+34 	Spain
					new NVPair("+34", "+34 (Spain)"),
					//	+94 	Sri Lanka
					new NVPair("+94", "+94 (Sri Lanka)"),
					//	+249 	Sudan
					new NVPair("+249", "+249 (Sudan)"),
					//	+597 	Suriname
					new NVPair("+597", "+597 (Suriname)"),
					//	+268 	Swaziland
					new NVPair("+268", "+268 (Swaziland)"),
					//	+46 	Sweden
					new NVPair("+46", "+46 (Sweden)"),
					//	+41 	Switzerland
					new NVPair("+41", "+41 (Switzerland)"),
					//	+963 	Syria
					new NVPair("+963", "+963 (Syria)"),
					
					//	+886 	Taiwan
					new NVPair("+886", "+886 (Taiwan)"),
					//	+992	Tajikistan
					new NVPair("+992", "+992 (Tajikistan)"),
					//	+255 	Tanzania
					new NVPair("+255", "+255 (Tanzania)"),
					//	+66 	Thailand
					new NVPair("+66", "+66 (Thailand)"),
					//	+228 	Togo
					new NVPair("+228", "+228 (Togo)"),
					//	+676 	Tonga
					new NVPair("+676", "+676 (Tonga)"),
					//	+1868 	Trinidad & Tobago
					new NVPair("+1868", "+1868 (Trinidad & Tobago)"),
					//	+216 	Tunisia
					new NVPair("+216", "+216 (Tunisia)"),
					//	+90 	Turkey
					new NVPair("+90", "+90 (Turkey)"),
					//	+993 	Turkmenistan
					new NVPair("+993", "+993 (Turkmenistan)"),
					//	+688 	Tuvalu
					new NVPair("+688", "+688 (Tuvalu)"),
					
					//	+256 	Uganda
					new NVPair("+256", "+256 (Uganda)"),
					//	+380 	Ukraine
					new NVPair("+380", "+380 (Ukraine)"),
					//	+971 	United Arab Emirates
					new NVPair("+971", "+971 (United Arab Emirates)"),
					//	+44 	United Kingdom
					new NVPair("+44", "+44 (United Kingdom)"),
					//	+598 	Uruguay
					new NVPair("+598", "+598 (Uruguay)"),
					//	+998 	Uzbekistan
					new NVPair("+998", "+998 (Uzbekistan)"),	

					//	+678 	Vanuatu
					new NVPair("+678", "+678 (Vanuatu)"),
					//	+39 	Vatican City
					new NVPair("+39", "+39 (Vatican City)"),
					//	+58 	Venezuela
					new NVPair("+58", "+58 (Venezuela)"),
					//	+84 	Vietnam
					new NVPair("+84", "+84 (Vietnam)"),	
					
					//	+967 	Yemen
					new NVPair("+967", "+967 (Yemen)"),	

					//	+260 	Zambia
					new NVPair("+260", "+260 (Zambia)"),
					//	+263 	Zimbabwe
					new NVPair("+263", "+263 (Zimbabwe)")

			
				//	Country codes not supported
				//	//	+98 	Iran
				//	new NVPair("+98 (Iran)", "+98"),
				//	//	+850 	Korea, North
				//	new NVPair("+850 (Korea, North)", "+850"),
				//	//	+249 	Sudan
				//	new NVPair("+249 (Sudan)", "+249"),
				//	//	+963 	Syria
				//	new NVPair("+963 (Syria)", "+963"),
			
					
			);

	/**
	 * Dynamic enum map: Currency
	 */
	public final static DynamicEnumMap CURRENCY = 
			new DynamicEnumMap("Currency",
					
					//	USD - US dollar
					new NVPair("USD", "US dollar"),
					//	EUR - Euro
					new NVPair("EUR", "Euro"),
					//	CAD - Canadian dollar
					new NVPair("CAD", "Canadian dollar")
			);
	
	
	public enum EmailType
		implements GetName
	{
		PERSONAL("Personal"),
		WORK("Work"),
		OTHER("Other")
		
		;
		
		private String name;
		
		EmailType(String name)
		{
			this.name = name;
		}

		@Override
		public String getName() 
		{
			return name;
		}
	}
	

	public enum DocumentStatus 
		implements GetName
	{
		AVAILABLE("Available"),
		UNAVAILABLE("Unavailable"),
		PENDING("Pending"),
		ERROR("Error"),
		
		;
		
		private String name;
		
		DocumentStatus(String name)
		{
			this.name = name;
		}
		
		@Override
		public String getName()
		{
			return name;
		}
	}
	
	public enum FormMode
	{
		DESIGN,
		EDIT,
		READ
		
		;
	}
	
	public enum DomainExtension
		implements GetName
	{
		COM(".com"),
		ORG(".org"),
		NET(".net"),
		INT(".int"),
		EDU(".edu"),
		GOV(".gov"),
		MIL(".mil"),
		CA(".ca"),
		US(".us"),
        IO(".io"),
        LB(".lb"),

	
		;
		
		private String name;
		
		DomainExtension(String name)
		{
			this.name = name;
		}

		@Override
		public String getName() 
		{
			return name;
		}
		
		public static boolean isValidExtension(String str)
		{
	    	for (DomainExtension ext : DomainExtension.values())
	    	{
	    		if (str.endsWith(ext.getName()))
	    		{
	    			return true;
	    		}
	    	}
	    	
	    	return false;
		}
	}
	
	
	public enum APIProperty
	    implements GetName
	{
		RETRY_DELAY("retry_delay"),
		ASYNC_CREATE("async_create"),
		ASYNC_DELETE("async_delete"),
		ASYNC_READ("async_read"),
		ASYNC_UPDATE("async_update"),
		
		;
	
		private final String name;
		
		APIProperty(String name)
		{
			this.name = name;
		}
		
		@Override
		public final String getName()
		{
			return name;
		}
		
	}
	
	public enum APIParameters
		implements GetName
	{
		NAME("name"),
		REFERENECE_ID("ref_id"),
		NVENTITY("nve"),
		NVENTITY_LIST("nve_list"),
		NVPAIR("nvp"),
		NVPAIR_LIST("nvp_list"),
		SYNC("sync"),
		REFERENCE_ONLY("reference_only"),
		INCLUDE_PARAM("include_param"),
		NVC_PARAM_NAMES("nvc_param_names"),
		DEEP_DELETE("deep"),
		CLASS_NAME("class_name"),
		
		PASSWORD("password"),
		NEW_PASSWORD("new_password"),
		
		//API_CONFIG_INFO_NAME("api_name"),
		API_TYPE_NAME("api_type_name"),
		API_VERSION("api_version"),
		API_CODE("code"),
		
		TO_FOLDER("to_folder"),
		FROM_FOLDER("from_folder"),
		NVES_TO_ADD("nves_to_add"),
		NVES_TO_DELETE("nves_to_delete"),
		REMOTE_DELETE("remote_delete"),
		NVES_TO_MOVE("nves_to_move"),
		
		DOCUMENT_INFO("document_info"),
		
		LANGUAGE("langauge"),
		
		;
	
		
		private String name;
		
		APIParameters(String name)
		{
			this.name = name;
		}
		
		@Override
		public String getName() 
		{
			return name;
		}
	
	}
}