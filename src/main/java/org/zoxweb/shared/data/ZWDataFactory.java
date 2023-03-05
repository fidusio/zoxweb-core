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

import org.zoxweb.shared.accounting.*;
import org.zoxweb.shared.api.*;
import org.zoxweb.shared.app.AppVersionDAO;
import org.zoxweb.shared.data.ticket.TicketContainerDAO;
import org.zoxweb.shared.data.ticket.TicketIssuerDAO;
import org.zoxweb.shared.data.ticket.TicketResolutionDAO;
import org.zoxweb.shared.http.HTTPEndPoint;
import org.zoxweb.shared.http.HTTPServerConfig;
import org.zoxweb.shared.net.*;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.security.shiro.*;
import org.zoxweb.shared.util.*;

import java.util.HashSet;
import java.util.Set;

/**
 * This NVEntity factory contains all NVEntity objects within this project.
 * @author mzebib
 *
 */
public class ZWDataFactory 
	implements NVEntityFactory
{
	
	public enum NVEntityTypeClass
		implements GetName, NVEntityInstance
	{
		
		ACCESS_CODE_DAO(AccessCodeDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AccessCodeDAO newInstance()
			{
				return new AccessCodeDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return AccessCodeDAO.NVC_ACCESS_CODE_DAO;
			}
			
		},
		//	org.zoxweb.shared.accounting
		AMOUNT_DAO(AmountDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AmountDAO newInstance()
			{
				return new AmountDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return AmountDAO.NVC_AMOUNT_DAO;
			}
		},
		API_BATCH_RESULT(APIBatchResult.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public APIBatchResult<NVEntity> newInstance()
			{
				return new APIBatchResult<>();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return APIBatchResult.NVC_API_BATCH_RESULT;
			}

		},
		//	org.zoxweb.shared.api
		API_CONFIG_INFO_DAO(APIConfigInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public APIConfigInfoDAO newInstance()
			{
				return new APIConfigInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return APIConfigInfoDAO.NVC_API_CONFIG_INFO_DAO;
			}
			
		},
		API_CREDENTIALS_DAO(APICredentialsDAO.class.getName())
		{

			@SuppressWarnings("unchecked")
			@Override
			public APICredentialsDAO newInstance()
			{
				return new APICredentialsDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return APICredentialsDAO.NVC_CREDENTIALS_DAO;
			}
			
		},
		API_DATA_OP(APIDataOP.class.getName())
		{

			@SuppressWarnings("unchecked")
			@Override
			public APIDataOP newInstance()
			{
				return new APIDataOP();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return APIDataOP.NVC_API_DATA_OP;
			}

		},
	   APP_DEVICE_DAO(AppDeviceDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            public AppDeviceDAO newInstance()
            {
                return new AppDeviceDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return AppDeviceDAO.NVC_APP_DEVICE_DAO;
            }
        },
		//	org.zoxweb.shared.data
		ADDRESS_DAO(AddressDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AddressDAO newInstance()
			{
				return new AddressDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return AddressDAO.NVC_ADDRESS_DAO;
			}
		},	
		AGREEMENT_DAO(AgreementDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AgreementDAO newInstance()
			{
				return new AgreementDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return AgreementDAO.NVC_AGREEMENT_DAO;
			}
		},
		APPLICATION_VERSION_DAO(AppVersionDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AppVersionDAO newInstance()
			{
				return new AppVersionDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return AppVersionDAO.NVC_APPLICATION_VERSION_DAO;
			}
		},
		ASSOCIATION_DAO(AssociationDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public AssociationDAO newInstance()
			{
				return new AssociationDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return AssociationDAO.NVC_ASSOCIATION_DAO;
			}
		},
		BILLING_ACCOUNT_DAO(BillingAccountDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public BillingAccountDAO newInstance()
			{
				return new BillingAccountDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return BillingAccountDAO.NVC_BILLING_ACCOUNT_DAO;
			}
		},
		BASIC_AUTH_TOKEN(BasicAuthToken.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public BasicAuthToken newInstance()
			{
				return new BasicAuthToken();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return BasicAuthToken.NVC_BASIC_AUTH_TOKEN;
			}
		},
		CONFIG_DAO(ConfigDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ConfigDAO newInstance()
			{
				return new ConfigDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return ConfigDAO.NVC_CONFIG_DAO;
			}
		},
		CONNECTION_CONFIG(ConnectionConfig.class.getName())
				{
					@SuppressWarnings("unchecked")
					@Override
					public ConnectionConfig newInstance()
					{
						return new ConnectionConfig();
					}

					@Override
					public NVConfigEntity getNVConfigEntity()
					{
						return ConnectionConfig.NVC_CONNECTION_CONFIG_DAO;
					}
				},
		PROPERTY_DAO(PropertyDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public PropertyDAO newInstance()
            {
                return new PropertyDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return PropertyDAO.NVC_PROPERTY_DAO;
            }
        },
		CREDIT_CARD_DAO(CreditCardDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public CreditCardDAO newInstance()
            {
                return new CreditCardDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return CreditCardDAO.NVC_CREDIT_CARD_DAO;
            }
        },
		
		CRUD_NVENTITY_DAO(CRUDNVEntityDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public CRUDNVEntityDAO newInstance()
			{
				return new CRUDNVEntityDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return CRUDNVEntityDAO.NVC_CRUD_NVENTITY_DAO;
			}
		},
		CURRENT_TIMESTAMP(CurrentTimestamp.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public CurrentTimestamp newInstance()
			{
				return new CurrentTimestamp();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return CurrentTimestamp.NVC_CURRENT_TIMESTAMP;
			}
		},
		DATA_CONTENT_DAO(DataContentDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DataContentDAO newInstance()
			{
				return new DataContentDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return DataContentDAO.NVC_DATA_CONTENT_DAO;
			}
		},
		DATA_DAO(DataDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DataDAO newInstance()
			{
				return new DataDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return DataDAO.NVC_DATA_DAO;
			}
		},
		DETAILED_CREDIT_CARD_DAO(DetailedCreditCardDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DetailedCreditCardDAO newInstance()
			{
				return new DetailedCreditCardDAO();
			}
			
			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return DetailedCreditCardDAO.NVC_DETAILED_CREDIT_CARD_DAO;
			}
		},
		DEVICE_DAO(DeviceDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DeviceDAO newInstance()
			{
				return new DeviceDAO();
			}
			
			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return DeviceDAO.NVC_DEVICE_DAO;
			}
		},		
		DOCUMENT_OPERATION_DAO(DocumentOperationDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DocumentOperationDAO newInstance()
			{
				return new DocumentOperationDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return DocumentOperationDAO.NVC_DOCUMENT_OPERATION_DAO;
			}
		},	
		DOMAIN_INFO_DAO(DomainInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public DomainInfoDAO newInstance()
			{
				return new DomainInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return DomainInfoDAO.NVC_DOMAIN_INFO_DAO;
			}
		},	
		FILE_INFO_DAO(FileInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FileInfoDAO newInstance()
			{
				return new FileInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return FileInfoDAO.NVC_FILE_INFO_DAO;
			}
		},	
		FOLDER_INFO_DAO(FolderInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FolderInfoDAO newInstance()
			{
				return new FolderInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return FolderInfoDAO.NVC_FOLDER_INFO_DAO;
			}
		},
		FORM_CONFIG_INFO_DAO(FormConfigInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FormConfigInfoDAO newInstance()
			{
				return new FormConfigInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return FormConfigInfoDAO.NVC_FORM_CONFIG_INFO_DAO;
			}
		},
		FORM_INFO_DAO(FormInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FormInfoDAO newInstance()
			{
				return new FormInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return FormInfoDAO.NVC_FORM_INFO_DAO;
			}
		},
		HTTP_END_POINT(HTTPEndPoint.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public HTTPEndPoint newInstance()
			{
				return new HTTPEndPoint();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return HTTPEndPoint.NVC_HTTP_END_POINT;
			}
		},
		HTTP_SERVER_CONFIG(HTTPServerConfig.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public HTTPServerConfig newInstance()
            {
                return new HTTPServerConfig();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() 
            {
                return HTTPServerConfig.NVC_HTTP_SERVER_CONFIG;
            }
        },
		INET_ADDRESS_DAO(InetAddressDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public InetAddressDAO newInstance()
			{
				return new InetAddressDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return InetAddressDAO.NVC_INET_ADDRESS_DAO;
			}
		},
		INET_FILTER_DAO(InetFilterDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public InetFilterDAO newInstance()
			{
				return new InetFilterDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return InetFilterDAO.NVC_INET_FILTER_DAO;
			}
		},
		INET_SOCKET_ADDRESS_DAO(InetSocketAddressDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public InetSocketAddressDAO newInstance()
			{
				return new InetSocketAddressDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return InetSocketAddressDAO.NVC_INET_SOCKET_ADDRESS_DAO;
			}
		},
		
		IP_BLOCKER_CONFIG(IPBlockerConfig.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public IPBlockerConfig newInstance()
			{
				return new IPBlockerConfig();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return IPBlockerConfig.NVC_IP_BLOCKER;
			}
		},	
		
		IP_RANGE_DAO(IPRangeDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public IPRangeDAO newInstance()
			{
				return new IPRangeDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return IPRangeDAO.IP_RANGE_DAO;
			}
		},	
		
		LOGIN_TOKEN_DAO(LoginTokenDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public LoginTokenDAO newInstance()
			{
				return new LoginTokenDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return LoginTokenDAO.NVC_LOGIN_IN_DAO;
			}
		},
		LONG_SEQUENCE(LongSequence.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public LongSequence newInstance()
			{
				return new LongSequence();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return LongSequence.NVC_LONG_SEQUENCE;
			}
		},
		
		MERCHANT_DAO(MerchantDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public MerchantDAO newInstance()
			{
				return new MerchantDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return MerchantDAO.NVC_MERCHANT_DAO;
			}
		},	
		MESSAGE_TEMPLATE_DAO(MessageTemplateDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public MessageTemplateDAO newInstance()
			{
				return new MessageTemplateDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return MessageTemplateDAO.NVC_MESSAGE_TEMPLATE_DAO;
			}
		},
		NETWORK_INTERFACE_DAO(NetworkInterfaceDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public NetworkInterfaceDAO newInstance()
			{
				return new NetworkInterfaceDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return NetworkInterfaceDAO.NVC_NETWORK_INTERFACE_DAO;
			}
		},
		NI_CONFIG_DAO(NIConfigDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public NIConfigDAO newInstance()
            {
                return new NIConfigDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() 
            {
                return NIConfigDAO.NVC_NI_CONFIG_DAO;
            }
        },
		NVENTITY_ACCESS_INFO(NVEntityAccessInfo.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public NVEntityAccessInfo newInstance()
			{
				return new NVEntityAccessInfo();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return NVEntityAccessInfo.NVC_NVENTITY_ACCESS_INFO;
			}
		},
		NVENTITY_CONTAINER_DAO(NVEntityContainerDAO.class.getName())
		{
		
			@SuppressWarnings("unchecked")
			@Override
			public NVEntityContainerDAO newInstance()
			{
				return new NVEntityContainerDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return NVEntityContainerDAO.NVC_NVENTITY_CONTAINER_DAO;
			}
		},
		PHONE_DAO(PhoneDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public PhoneDAO newInstance()
			{
				return new PhoneDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return PhoneDAO.NVC_PHONE_DAO;
			}
		},	
		API_ERROR(APIError.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public APIError newInstance()
			{
				return new APIError();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return APIError.API_ERROR;
			}
		},
		//	org.zoxweb.shared.accounting
		RANGE(Range.class.getName())
		{
			@SuppressWarnings({"unchecked", "rawtypes"})
			@Override
			public Range<?> newInstance()
			{
				return new Range();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return Range.NVC_RANGE;
			}
		},
		RATE_COUNTER(RateCounter.class.getName())
		{
			@SuppressWarnings({"unchecked"})
			@Override
			public RateCounter newInstance()
			{
				return new RateCounter();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return RateCounter.NVC_RATE_COUNTER;
			}
		},
		RUNTIME_RESULT_DAO(RuntimeResultDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public RuntimeResultDAO newInstance()
			{
				return new RuntimeResultDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return RuntimeResultDAO.RUNTIME_RESULT_DAO;
			}
		},
		SECURE_DOCUMENT_DAO(SecureDocumentDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public SecureDocumentDAO newInstance()
			{
				return new SecureDocumentDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return SecureDocumentDAO.NVC_SECURE_DOCUMENT_DAO;
			}
		},	
		SIMPLE_DOCUMENT_DAO(SimpleDocumentDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public SimpleDocumentDAO newInstance()
			{
				return new SimpleDocumentDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return SimpleDocumentDAO.NVC_SIMPLE_DOCUMENT_DAO;
			}
		},
		SIMPLE_MESSAGE(SimpleMessage.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public SimpleMessage newInstance()
            {
                return new SimpleMessage();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() 
            {
                return SimpleMessage.NVC_SIMPLE_MESSAGE;
            }
        },
		SUBJECT_API_KEY(SubjectAPIKey.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public SubjectAPIKey newInstance()
			{
				return new SubjectAPIKey();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return SubjectAPIKey.NVC_SUBJECT_API_KEY;
			}
		},
		SYSTEM_INFO_DAO(SystemInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public SystemInfoDAO newInstance()
			{
				return new SystemInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return SystemInfoDAO.NVC_SYSTEM_INFO_DAO;
			}
		},	
		USER_ID_DAO(UserIDDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public UserIDDAO newInstance()
			{
				return new UserIDDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return UserIDDAO.NVC_USER_ID_DAO;
			}
		},	
		USER_INFO_DAO(UserInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public UserInfoDAO newInstance()
			{
				return new UserInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return UserInfoDAO.NVC_USER_INFO_DAO;
			}
		},	
		VM_INFO_DAO(VMInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public VMInfoDAO newInstance()
			{
				return new VMInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return VMInfoDAO.NVC_VMINFO_DAO;
			}
		},
		FOLDER_CONTENT_OP(FolderContentOp.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FolderContentOp newInstance()
			{
				return new FolderContentOp();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return FolderContentOp.NVC_FOLDER_CONTENT_OP;
			}
		},
		PARAM_INFO(ParamInfo.class.getName())
		{
		    @SuppressWarnings("unchecked")
            @Override
			public ParamInfo newInstance() {return new ParamInfo();}
		    
		    @Override
			public NVConfigEntity getNVConfigEntity() {return ParamInfo.NVC_PARAM_INFO;}

		},
		PAYMENT_INFO_DAO(PaymentInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public PaymentInfoDAO newInstance()
			{
				return new PaymentInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return PaymentInfoDAO.NVC_PAYMENT_INFO_DAO;
			}
		},
		FINANCIAL_TRANSACTION_DAO(FinancialTransactionDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public FinancialTransactionDAO newInstance()
			{
				return new FinancialTransactionDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return FinancialTransactionDAO.NVC_FINANCIAL_TRANSACTION_DAO;
			}
		},

		SCAN_RESULT_DAO(ScanResultDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ScanResultDAO newInstance()
			{
				return new ScanResultDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return ScanResultDAO.NVC_SCAN_RESULT_DAO;
			}
			
		},
		SECURITY_PROFILE(SecurityProfile.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public SecurityProfile newInstance()
			{
				return new SecurityProfile();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return SecurityProfile.NVC_SECURITY_PROFILE;
			}
		},

		//	org.zoxweb.shared.data.shiro
		SHIRO_NVENTITY_CRUDS(ShiroNVEntityCRUDs.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ShiroNVEntityCRUDs newInstance()
			{
				return new ShiroNVEntityCRUDs();
			}
			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return ShiroNVEntityCRUDs.NVC_SHIRO_NVENTITY_CRUDS;
			}
		},
		SHIRO_ROLE_DAO(ShiroRoleDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ShiroRoleDAO newInstance()
			{
				return new ShiroRoleDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return ShiroRoleDAO.NVC_SHIRO_ROLE_DAO;
			}
		},
		SHIRO_ROLE_GROUP_DAO(ShiroRoleGroupDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ShiroRoleGroupDAO newInstance()
			{
				return new ShiroRoleGroupDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return ShiroRoleGroupDAO.NVC_SHIRO_ROLE_GROUP_DAO;
			}
		},
		SHIRO_PERMISSION_DAO(ShiroPermissionDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ShiroPermissionDAO newInstance()
			{
				return new ShiroPermissionDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return ShiroPermissionDAO.NVC_SHIRO_PERMISSION_DAO;
			}
		},
		SHIRO_SUBJECT_DATA(ShiroSubjectData.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public ShiroSubjectData newInstance()
            {
                return new ShiroSubjectData();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() 
            {
                return ShiroSubjectData.NVC_SUBJECT_DATA;
            }
        },
		STAT_COUNTER(StatCounter.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public StatCounter newInstance()
			{
				return new StatCounter();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return StatCounter.NVC_STAT_COUNTER_DAO;
			}
		},
		STAT_INFO(StatInfo.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public StatInfo newInstance()
			{
				return new StatInfo();
			}

			@Override
			public NVConfigEntity getNVConfigEntity()
			{
				return StatInfo.NVC_STAT_INFO;
			}
		},
		UUID_INFO_DAO(UUIDInfoDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public UUIDInfoDAO newInstance()
			{
				return new UUIDInfoDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return UUIDInfoDAO.NVC_UUID_INFO_DAO;
			}
		},
		
		
		BILLING_ITEM_DAO(BillingItemDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public BillingItemDAO newInstance()
			{
				return new BillingItemDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return BillingItemDAO.NVC_BILLING_ITEM_DAO;
			}
		},
		BILLING_ITEMS_CONTAINER_DAO(BillingItemsContainerDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public BillingItemsContainerDAO newInstance()
			{
				return new BillingItemsContainerDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return BillingItemsContainerDAO.NVC_BILLING_ITEMS_CONTAINER_DAO;
			}
		},
		TICKET_CONTAINER_DAO(TicketContainerDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public TicketContainerDAO newInstance()
			{
				return new TicketContainerDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return TicketContainerDAO.NVC_TICKET_CONTAINER_DAO;
			}
		},
		TICKET_ISSUER_DAO(TicketIssuerDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public TicketIssuerDAO newInstance()
			{
				return new TicketIssuerDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return TicketIssuerDAO.NVC_TICKET_ISSUER_DAO;
			}
		},
		TICKET_RESOLUTION_DAO(TicketResolutionDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public TicketResolutionDAO newInstance()
			{
				return new TicketResolutionDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return TicketResolutionDAO.NVC_TICKET_RESOLUTION_DAO;
			}
		},


        APP_ID_DAO(AppIDDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public AppIDDAO newInstance()
            {
                return new AppIDDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return AppIDDAO.NVC_APP_ID_DAO;
            }
        },
//        HTTP_DEFAULT_RESPONSE_DAO(HTTPDefaultResponseDAO.class.getName())
//        {
//            @SuppressWarnings("unchecked")
//            @Override
//            public HTTPDefaultResponseDAO newInstance()
//            {
//                return new HTTPDefaultResponseDAO();
//            }
//
//            @Override
//            public NVConfigEntity getNVConfigEntity()
//            {
//                return HTTPDefaultResponseDAO.NVC_HTTP_DEFAULT_RESPONSE_DAO;
//            }
//        },
//        JWT_HEADER(JWTHeader.class.getName())
//        {
//            @SuppressWarnings("unchecked")
//            @Override
//            public JWTHeader newInstance()
//            {
//                return new JWTHeader();
//            }
//
//            @Override
//            public NVConfigEntity getNVConfigEntity()
//            {
//                return JWTHeader.NVC_JWT_HEADER;
//            }
//        },
//        JWT_PAYLOAD(JWTPayload.class.getName())
//        {
//            @SuppressWarnings("unchecked")
//            @Override
//            public JWTPayload newInstance()
//            {
//                return new JWTPayload();
//            }
//
//            @Override
//            public NVConfigEntity getNVConfigEntity()
//            {
//                return JWTPayload.NVC_JWT_PAYLOAD;
//            }
//        },
        JWT(org.zoxweb.shared.security.JWT.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public org.zoxweb.shared.security.JWT newInstance()
            {
                return new org.zoxweb.shared.security.JWT();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return org.zoxweb.shared.security.JWT.NVC_JWT;
            }
        },
        IMAGE_DAO(ImageDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public ImageDAO newInstance()
            {
                return new ImageDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return ImageDAO.NVC_IMAGE_DAO;
            }
        },
        USER_PREFERENCE_DAO(UserPreferenceDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public UserPreferenceDAO newInstance()
            {
                return new UserPreferenceDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return UserPreferenceDAO.NVC_USER_PREFERENCE_DAO;
            }
        },
        KEY_STORE_INFO_DAO(KeyStoreInfoDAO.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public KeyStoreInfoDAO newInstance()
            {
                return new KeyStoreInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return KeyStoreInfoDAO.NVC_KEY_STORE_INFO_DAO;
            }
        },
        APP_ACCESS_MODE(AppAccessMode.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public AppAccessMode newInstance()
            {
                return new AppAccessMode();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return AppAccessMode.NVC_APP_ACCESS_MODE;
            }
        },

		;

		private final String name;
		
		NVEntityTypeClass(String name)
		{
			this.name = name;
		}
		
		@Override
		public String getName() 
		{
			return name;
		}
		
	}
	
	/**
	 * Creates an instance of this class.
	 */
	public static final ZWDataFactory SINGLETON = new ZWDataFactory();
	private final Set<NVEntityFactory> factoriesSet = new HashSet<>();
	
	/**
	 * The default constructor is declared private to prevent outside instantiation of this class.
	 */
	private ZWDataFactory()
	{
		
	}
	
	public void registerFactory(NVEntityFactory factory)
	{
		if (factory != null)
		{
			factoriesSet.add(factory);
		}
	}
	
	/**
	 * Creates NVEntity object based on given canonical ID.
	 * @param canonicalID of the NVEntity
	 */
	@Override
	public <V extends NVEntity> V createNVEntity(String canonicalID) 
	{
		if (!SharedStringUtil.isEmpty(canonicalID))
		{
			NVEntityTypeClass type = SharedUtil.lookupEnum(canonicalID, NVEntityTypeClass.values());
			
			if (type == null)
			{
				for (NVEntityTypeClass nveTypeClass : NVEntityTypeClass.values())
				{
					if (canonicalID.equals(nveTypeClass.getNVConfigEntity().toCanonicalID())
                            || canonicalID.equals(nveTypeClass.getNVConfigEntity().getName()))
					{
						type = nveTypeClass;
						break;
					}
				}
			}
			
			if (type != null)
			{
				return type.newInstance();
			}
		}
		
		for (NVEntityFactory fac : factoriesSet)
		{
			V ret = fac.createNVEntity(canonicalID);

			if (ret != null)
			{
				return ret;
			}
		}

		return null;
	}

}