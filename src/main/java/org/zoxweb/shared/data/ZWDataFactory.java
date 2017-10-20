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

import java.util.HashSet;
import java.util.Set;

import org.zoxweb.shared.accounting.MoneyValueDAO;
import org.zoxweb.shared.accounting.PaymentInfoDAO;
import org.zoxweb.shared.accounting.FinancialTransactionDAO;
import org.zoxweb.shared.accounting.BillingAccountDAO;
import org.zoxweb.shared.accounting.BillingItemsContainerDAO;
import org.zoxweb.shared.accounting.BillingItemDAO;
import org.zoxweb.shared.api.APIBatchResult;
import org.zoxweb.shared.api.APIConfigInfoDAO;
import org.zoxweb.shared.api.APICredentialsDAO;
import org.zoxweb.shared.api.APIError;

import org.zoxweb.shared.util.NVEntityInstance;
import org.zoxweb.shared.data.ticket.TicketContainerDAO;
import org.zoxweb.shared.data.ticket.TicketIssuerDAO;
import org.zoxweb.shared.data.ticket.TicketResolutionDAO;
import org.zoxweb.shared.net.IPRangeDAO;
import org.zoxweb.shared.net.InetAddressDAO;
import org.zoxweb.shared.net.InetFilterDAO;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.security.AccessCodeDAO;
import org.zoxweb.shared.security.IPBlockerConfig;
import org.zoxweb.shared.security.JWTHeader;
import org.zoxweb.shared.security.JWTPayload;
import org.zoxweb.shared.security.LoginTokenDAO;
import org.zoxweb.shared.security.ScanResultDAO;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import org.zoxweb.shared.security.SubjectAPIKey;
import org.zoxweb.shared.security.shiro.ShiroNVEntityCRUDs;
import org.zoxweb.shared.security.shiro.ShiroPermissionDAO;
import org.zoxweb.shared.security.shiro.ShiroRoleDAO;
import org.zoxweb.shared.security.shiro.ShiroRoleGroupDAO;
import org.zoxweb.shared.data.AppDeviceDAO;

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
		API_BATCH_RESULTS_DAO(APIBatchResult.class.getName())
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
				return APIBatchResult.NVC_API_BATCH_RESULT_DAO;
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
            };
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
		APPLICATION_INFO_DAO(ApplicationVersionDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public ApplicationVersionDAO newInstance()
			{
				return new ApplicationVersionDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return ApplicationVersionDAO.NVC_APPLICATION_VERSION_DAO;
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
		DETAILED_CREDIT_CARD(DetailedCreditCardDAO.class.getName())
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
				return InetSocketAddressDAO.NVC_REFERENCE_ID_DAO;
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
				return APIError.REST_ERROR;
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
		
		//	org.zoxweb.shared.accounting
		MONEY_VALUE_DAO(MoneyValueDAO.class.getName())
		{
			@SuppressWarnings("unchecked")
			@Override
			public MoneyValueDAO newInstance()
			{
				return new MoneyValueDAO();
			}

			@Override
			public NVConfigEntity getNVConfigEntity() 
			{
				return MoneyValueDAO.NVC_MONEY_VALUE_DAO;
			}
		},
		MOVE_FOLDER_CONTENT_OP(FolderContentOp.class.getName())
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
				return FolderContentOp.NVC_MOVE_FOLDER_CONTENT_OP;
			}
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
			};
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
			};
		},
		BILLING_ITEM_CONTAINER_DAO(BillingItemsContainerDAO.class.getName())
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
        JWT_HEADER(JWTHeader.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public JWTHeader newInstance()
            {
                return new JWTHeader();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return JWTHeader.NVC_JWT_HEADER;
            }
        },
        JWT_PAYLOAD(JWTPayload.class.getName())
        {
            @SuppressWarnings("unchecked")
            @Override
            public JWTPayload newInstance()
            {
                return new JWTPayload();
            }

            @Override
            public NVConfigEntity getNVConfigEntity()
            {
                return JWTPayload.NVC_JWT_PAYLOAD;
            }
        },
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

		;

		private String name;
		
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
	private Set<NVEntityFactory> factoriesSet = new HashSet<>();
	
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
	 * @param canonicalID
	 */
	@Override
	public <V extends NVEntity> V createNVEntity(String canonicalID) 
	{
		if (!SharedStringUtil.isEmpty(canonicalID))
		{
			NVEntityTypeClass type = (NVEntityTypeClass) SharedUtil.lookupEnum(NVEntityTypeClass.values(), canonicalID);
			
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