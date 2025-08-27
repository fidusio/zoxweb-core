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

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The AgreementDAO class defines agreement data access object used to create agreements.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class AgreementDAO 
	extends SetNameDescriptionDAO
{

	/**
	 * This enum contains the following variables:
	 * agreement title, agreement content, and 
	 * agreement check title.
	 */
	public enum Param
		implements GetNVConfig
	{
		AGREEMENT_TITLE(NVConfigManager.createNVConfig("agreement_title", "The title of the agreement.", "AgreementTitle", true, true, String.class)),
		AGREEMENT_CONTENT(NVConfigManager.createNVConfig("agreement_content", "The content of the agreement.", "AgreementContent", true, true, String.class)),
		AGREEMENT_CHECK_TITLE(NVConfigManager.createNVConfig("agreement_check_title", "The title of the agreement check box.", "AgreementCheckTitle", false, true, String.class)),

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
	
	}
	
	public static final NVConfigEntity NVC_AGREEMENT_DAO = new NVConfigEntityPortable(
        "agreement_dao",
        null,
        "Agreement",
        true,
        false,
        false,
        false,
        AgreementDAO.class,
        SharedUtil.extractNVConfigs(Param.values()),
        null,
        false,
        SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
	
	
	/**
	 * The default constructor.
	 */
	public AgreementDAO()
	{
		super(NVC_AGREEMENT_DAO);
	}
	
	/**
	 * This constructor instantiates AgreementDAO based on given language, check title, agreement title, and agreement content.
	 * @param language
	 * @param checkTitle
	 * @param agreementTitle
	 * @param content
	 */
	public AgreementDAO(String language, String checkTitle, String agreementTitle, String content)
	{
		this();
		setLanguage(language);
		setAgreementCheckTitle(checkTitle);
		setAgreementContent(content);
		setAgreementTitle(agreementTitle);
	}

	/**
	 * Gets the language of the agreement.
	 * @return language
	 */
	public String getLanguage() 
	{
		return getName();
	}
	
	/**
	 * Sets the language of the agreement.
	 * @param language
	 */
	public void setLanguage(String language) 
	{
		setName(language);
	}
	
	/**
	 * Gets the title of the agreement.
	 * @return the agreement title
	 */
	public String getAgreementTitle() 
	{
		return lookupValue(Param.AGREEMENT_TITLE);
	}
	
	/**
	 * Sets the title of the agreement.
	 * @param title
	 */
	public void setAgreementTitle(String title) 
	{
		setValue(Param.AGREEMENT_TITLE, title);
	}
	
	/**
	 * Gets the content of the agreement.
	 * @return the agreement content
	 */
	public String getAgreementContent() 
	{
		return lookupValue(Param.AGREEMENT_CONTENT);
	}
	
	/**
	 * Sets the content of the agreement.
	 * @param content
	 */
	public void setAgreementContent(String content) 
	{
		setValue(Param.AGREEMENT_CONTENT, content);
	}
	
	/**
	 * Gets the check box title of the agreement.
	 * @return the agreement check title
	 */
	public String getAgreementCheckTitle() 
	{
		return lookupValue(Param.AGREEMENT_CHECK_TITLE);
	}
	
	/**
	 * Sets the check box title of the agreement.
	 * @param checkTitle
	 */
	public void setAgreementCheckTitle(String checkTitle) 
	{
		setValue(Param.AGREEMENT_CHECK_TITLE, checkTitle);
	}
	
}