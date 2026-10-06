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
package org.zoxweb.shared.data;

import org.zoxweb.shared.accounting.*;
import org.zoxweb.shared.api.*;
import org.zoxweb.shared.app.AppIDDefault;
import org.zoxweb.shared.app.AppVersionDAO;
import org.zoxweb.shared.data.ticket.TicketContainer;
import org.zoxweb.shared.data.ticket.TicketIssuer;
import org.zoxweb.shared.data.ticket.TicketResolution;
import org.zoxweb.shared.http.HTTPEndPoint;
import org.zoxweb.shared.http.HTTPServerConfig;
import org.zoxweb.shared.net.*;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.util.*;

import java.util.HashSet;
import java.util.Set;

/**
 * This NVEntity factory contains all NVEntity objects within this project.
 *
 * @author mzebib
 *
 */
public class ZWDataFactory
        implements NVEntityFactory {

    public enum NVEntityTypeClass
            implements GetName, NVEntityInstance {

        ACCESS_CODE_DAO(AccessCodeDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AccessCodeDAO newInstance() {
                return new AccessCodeDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AccessCodeDAO.NVC_ACCESS_CODE_DAO;
            }

        },
        ADDRESS_DAO(AddressDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AddressDAO newInstance() {
                return new AddressDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AddressDAO.NVC_ADDRESS_DAO;
            }
        } //	org.zoxweb.shared.data
        ,

        AGREEMENT_DAO(AgreementDoc.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AgreementDoc newInstance() {
                return new AgreementDoc();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AgreementDoc.NVC_AGREEMENT_DOC;
            }
        },
        AMOUNT_DAO(AmountDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AmountDAO newInstance() {
                return new AmountDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AmountDAO.NVC_AMOUNT_DAO;
            }
        } //	org.zoxweb.shared.accounting
        ,

        API_BATCH_RESULT(APIBatchResult.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public APIBatchResult<NVEntity> newInstance() {
                return new APIBatchResult<>();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return APIBatchResult.NVC_API_BATCH_RESULT;
            }

        },
        API_CONFIG_INFO_IMPL(APIConfigInfoImpl.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public APIConfigInfoImpl newInstance() {
                return new APIConfigInfoImpl();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return APIConfigInfoImpl.NVC_API_CONFIG_INFO_IMPL;
            }

        } //	org.zoxweb.shared.api
        ,
        API_CREDENTIALS_DAO(APICredentialsDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public APICredentialsDAO newInstance() {
                return new APICredentialsDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return APICredentialsDAO.NVC_CREDENTIALS_DAO;
            }

        },
        API_DATA_OP(APIDataOP.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public APIDataOP newInstance() {
                return new APIDataOP();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return APIDataOP.NVC_API_DATA_OP;
            }

        },
        API_ERROR(APIError.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public APIError newInstance() {
                return new APIError();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return APIError.API_ERROR;
            }
        },
        APPLICATION_VERSION_DAO(AppVersionDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AppVersionDAO newInstance() {
                return new AppVersionDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AppVersionDAO.NVC_APPLICATION_VERSION_DAO;
            }
        },
        APP_ACCESS_MODE(AppAccessMode.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AppAccessMode newInstance() {
                return new AppAccessMode();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AppAccessMode.NVC_APP_ACCESS_MODE;
            }
        },
        APP_DEVICE_INFO(AppDeviceInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            public AppDeviceInfo newInstance() {
                return new AppDeviceInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AppDeviceInfo.NVC_APP_DEVICE_INFO;
            }
        },
        APP_ID_DEFAULT(AppIDDefault.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AppIDDefault newInstance() {
                return new AppIDDefault();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AppIDDefault.NVC_APP_ID_DEFAULT;
            }
        },
        ASSOCIATION_DAO(AssociationInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public AssociationInfo newInstance() {
                return new AssociationInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return AssociationInfo.NVC_ASSOCIATION_INFO;
            }
        },
        BASIC_AUTH_TOKEN(BasicAuthToken.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public BasicAuthToken newInstance() {
                return new BasicAuthToken();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return BasicAuthToken.NVC_BASIC_AUTH_TOKEN;
            }
        },
        BILLING_ACCOUNT_DAO(BillingAccountDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public BillingAccountDAO newInstance() {
                return new BillingAccountDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return BillingAccountDAO.NVC_BILLING_ACCOUNT_DAO;
            }
        },
        BILLING_ITEMS_CONTAINER_DAO(BillingItemsContainerDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public BillingItemsContainerDAO newInstance() {
                return new BillingItemsContainerDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return BillingItemsContainerDAO.NVC_BILLING_ITEMS_CONTAINER_DAO;
            }
        },

        BILLING_ITEM_DAO(BillingItemDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public BillingItemDAO newInstance() {
                return new BillingItemDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return BillingItemDAO.NVC_BILLING_ITEM_DAO;
            }
        },
        GEN_CONFIG(GenConfig.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public GenConfig newInstance() {
                return new GenConfig();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return GenConfig.NVC_GEN_CONFIG;
            }
        },
        CONNECTION_CONFIG(ConnectionConfig.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ConnectionConfig newInstance() {
                return new ConnectionConfig();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ConnectionConfig.NVC_CONNECTION_CONFIG_DAO;
            }
        },
        CREDIT_CARD_DAO(CreditCardDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public CreditCardDAO newInstance() {
                return new CreditCardDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return CreditCardDAO.NVC_CREDIT_CARD_DAO;
            }
        },
        CRUD_NVENTITY_DAO(CRUDNVEntityInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public CRUDNVEntityInfo newInstance() {
                return new CRUDNVEntityInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return CRUDNVEntityInfo.NVC_CRUD_NVENTITY_INFO;
            }
        },
        CURRENT_TIMESTAMP(CurrentTimestamp.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public CurrentTimestamp newInstance() {
                return new CurrentTimestamp();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return CurrentTimestamp.NVC_CURRENT_TIMESTAMP;
            }
        },
        DATA_CONTENT_DAO(DocumentContent.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DocumentContent newInstance() {
                return new DocumentContent();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DocumentContent.NVC_DOCUMENT_CONTENT;
            }
        },
        DATA_DAO(DataContent.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DataContent newInstance() {
                return new DataContent();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DataContent.NVC_DATA_CONTENT;
            }
        },
        DETAILED_CREDIT_CARD_DAO(DetailedCreditCardDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DetailedCreditCardDAO newInstance() {
                return new DetailedCreditCardDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DetailedCreditCardDAO.NVC_DETAILED_CREDIT_CARD_DAO;
            }
        },
        DEVICE_INFO(DeviceInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DeviceInfo newInstance() {
                return new DeviceInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DeviceInfo.NVC_DEVICE_INFO;
            }
        },
        DOCUMENT_OPERATION_DAO(DocumentOperation.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DocumentOperation newInstance() {
                return new DocumentOperation();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DocumentOperation.NVC_DOCUMENT_OPERATION;
            }
        },
        DOMAIN_INFO(DomainInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public DomainInfo newInstance() {
                return new DomainInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return DomainInfo.NVC_DOMAIN_INFO;
            }
        },
        FILE_INFO(FileInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FileInfo newInstance() {
                return new FileInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FileInfo.NVC_FILE_INFO;
            }
        },
        FINANCIAL_TRANSACTION(FinancialTransaction.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FinancialTransaction newInstance() {
                return new FinancialTransaction();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FinancialTransaction.NVC_FINANCIAL_TRANSACTION;
            }
        },
        FOLDER_CONTENT_OP(FolderContentOp.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FolderContentOp newInstance() {
                return new FolderContentOp();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FolderContentOp.NVC_FOLDER_CONTENT_OP;
            }
        },
        FOLDER_INFO(FolderInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FolderInfo newInstance() {
                return new FolderInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FolderInfo.NVC_FOLDER_INFO;
            }
        },
        FORM_CONFIG_INFO(FormConfigInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FormConfigInfo newInstance() {
                return new FormConfigInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FormConfigInfo.NVC_FORM_CONFIG_INFO;
            }
        },

        FORM_INFO(FormInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public FormInfo newInstance() {
                return new FormInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return FormInfo.NVC_FORM_INFO;
            }
        },

        HTTP_END_POINT(HTTPEndPoint.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public HTTPEndPoint newInstance() {
                return new HTTPEndPoint();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return HTTPEndPoint.NVC_HTTP_END_POINT;
            }
        },

        HTTP_SERVER_CONFIG(HTTPServerConfig.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public HTTPServerConfig newInstance() {
                return new HTTPServerConfig();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return HTTPServerConfig.NVC_HTTP_SERVER_CONFIG;
            }
        },

        IMAGE_META_INFO(ImageMetaInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ImageMetaInfo newInstance() {
                return new ImageMetaInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ImageMetaInfo.NVC_IMAGE_META_INFO;
            }
        },
        INET_ADDRESS_INFO(InetAddressInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public InetAddressInfo newInstance() {
                return new InetAddressInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return InetAddressInfo.NVC_INET_ADDRESS_INFO;
            }
        },
        INET_FILTER_INFO(InetFilterInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public InetFilterInfo newInstance() {
                return new InetFilterInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return InetFilterInfo.NVC_INET_FILTER_INFO;
            }
        },
        INET_SOCKET_ADDRESS_DAO(IPAddress.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public IPAddress newInstance() {
                return new IPAddress();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return IPAddress.NVC_IP_ADDRESS;
            }
        },
        IP_BLOCKER_CONFIG(IPBlockerConfig.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public IPBlockerConfig newInstance() {
                return new IPBlockerConfig();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return IPBlockerConfig.NVC_IP_BLOCKER;
            }
        },
        IP_RANGE_DAO(IPRangeDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public IPRangeDAO newInstance() {
                return new IPRangeDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return IPRangeDAO.IP_RANGE_DAO;
            }
        },
        JWT(JWT.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public JWT newInstance() {
                return new JWT();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return org.zoxweb.shared.security.JWT.NVC_JWT;
            }
        } //        {
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
        //        HTTP_DEFAULT_RESPONSE_DAO(HTTPDefaultResponseDAO.class.getName())
        ,


        KEY_STORE_INFO_DAO(KeyStoreInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public KeyStoreInfo newInstance() {
                return new KeyStoreInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return KeyStoreInfo.NVC_KEY_STORE_INFO;
            }
        },
        LONG_SEQUENCE(LongSequence.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public LongSequence newInstance() {
                return new LongSequence();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return LongSequence.NVC_LONG_SEQUENCE;
            }
        } //		{
        //			@SuppressWarnings("unchecked")
        //			@Override
        //			public LoginTokenDAO newInstance()
        //			{
        //				return new LoginTokenDAO();
        //			}
        //
        //			@Override
        //			public NVConfigEntity getNVConfigEntity()
        //			{
        //				return LoginTokenDAO.NVC_LOGIN_IN_DAO;
        //			}
        //		},
        //		LOGIN_TOKEN_DAO(LoginTokenDAO.class.getName())
        ,


        MERCHANT_DAO(MerchantDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public MerchantDAO newInstance() {
                return new MerchantDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return MerchantDAO.NVC_MERCHANT_DAO;
            }
        },
        MESSAGE_TEMPLATE(MessageTemplate.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public MessageTemplate newInstance() {
                return new MessageTemplate();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return MessageTemplate.NVC_MESSAGE_TEMPLATE;
            }
        },
        NETWORK_INTERFACE_DAO(NetworkInterfaceDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public NetworkInterfaceDAO newInstance() {
                return new NetworkInterfaceDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return NetworkInterfaceDAO.NVC_NETWORK_INTERFACE_DAO;
            }
        },
        NI_CONFIG_DAO(NIConfigDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public NIConfigDAO newInstance() {
                return new NIConfigDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return NIConfigDAO.NVC_NI_CONFIG_DAO;
            }
        },
        NVENTITY_ACCESS_INFO(NVEntityAccessInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public NVEntityAccessInfo newInstance() {
                return new NVEntityAccessInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return NVEntityAccessInfo.NVC_NVENTITY_ACCESS_INFO;
            }
        },
        NVENTITY_CONTAINER_DAO(NVEntityContainerDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public NVEntityContainerDAO newInstance() {
                return new NVEntityContainerDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return NVEntityContainerDAO.NVC_NVENTITY_CONTAINER_DAO;
            }
        },
        PARAM_INFO(ParamInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ParamInfo newInstance() {
                return new ParamInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ParamInfo.NVC_PARAM_INFO;
            }

        },
        PAYMENT_INFO_DAO(PaymentInfoDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PaymentInfoDAO newInstance() {
                return new PaymentInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PaymentInfoDAO.NVC_PAYMENT_INFO_DAO;
            }
        },
        PERMISSION_GRANT(PermissionGrant.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PermissionGrant newInstance() {
                return new PermissionGrant();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PermissionGrant.NVC_PERMISSION_GRANT;
            }
        },
        PERMISSION_INFO(PermissionInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PermissionInfo newInstance() {
                return new PermissionInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PermissionInfo.NVC_PERMISSION_INFO;
            }
        },
        PHONE_DAO(PhoneDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PhoneDAO newInstance() {
                return new PhoneDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PhoneDAO.NVC_PHONE_DAO;
            }
        },
        PRINCIPAL_IDENTIFIER(PrincipalIdentifier.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PrincipalIdentifier newInstance() {
                return new PrincipalIdentifier();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PrincipalIdentifier.NVC_PRINCIPAL_IDENTIFIER;
            }
        },
        PROPERTY_DAO(PropertyDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public PropertyDAO newInstance() {
                return new PropertyDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return PropertyDAO.NVC_PROPERTY_DAO;
            }
        },
        RANGE(Range.class.getName()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public Range<?> newInstance() {
                return new Range();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return Range.NVC_RANGE;
            }
        } //	org.zoxweb.shared.accounting
        ,
        RATE_COUNTER(RateCounter.class.getName()) {
            @SuppressWarnings({"unchecked"})
            @Override
            public RateCounter newInstance() {
                return new RateCounter();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RateCounter.NVC_RATE_COUNTER;
            }
        },
        RESOURCE_MAP(ResourceMap.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ResourceMap newInstance() {
                return new ResourceMap();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ResourceMap.NVC_RESOURCE_MAP;
            }
        },
        RESOURCE_SECURITY_PROFILE(ResourceSecurityProfile.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ResourceSecurityProfile newInstance() {
                return new ResourceSecurityProfile();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ResourceSecurityProfile.NVC_RESOURCE_SECURITY_PROFILE;
            }
        },
        ROLE_GRANT(RoleGrant.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public RoleGrant newInstance() {
                return new RoleGrant();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RoleGrant.NVC_ROLE_GRANT;
            }
        },
        ROLE_GROUP_GRANT(RoleGroupGrant.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public RoleGroupGrant newInstance() {
                return new RoleGroupGrant();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RoleGroupGrant.NVC_ROLE_GROUP_GRANT;
            }
        },
        ROLE_GROUP_INFO(RoleGroupInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public RoleGroupInfo newInstance() {
                return new RoleGroupInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RoleGroupInfo.NVC_ROLE_GROUP_INFO;
            }
        },

        ROLE_INFO(RoleInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public RoleInfo newInstance() {
                return new RoleInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RoleInfo.NVC_ROLE_INFO;
            }
        },
        RUNTIME_RESULT_DAO(RuntimeResultData.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public RuntimeResultData newInstance() {
                return new RuntimeResultData();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return RuntimeResultData.RUNTIME_RESULT_DATA;
            }
        },
        SCAN_RESULT_DAO(ScanResult.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public ScanResult newInstance() {
                return new ScanResult();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return ScanResult.NVC_SCAN_RESULT;
            }

        },
        SECURE_DOCUMENT(SecureDocument.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SecureDocument newInstance() {
                return new SecureDocument();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SecureDocument.NVC_SECURE_DOCUMENT;
            }
        },


        SIMPLE_DOCUMENT(SimpleDocument.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SimpleDocument newInstance() {
                return new SimpleDocument();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SimpleDocument.NVC_SIMPLE_DOCUMENT;
            }
        },
        SIMPLE_MESSAGE(SimpleMessage.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SimpleMessage newInstance() {
                return new SimpleMessage();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SimpleMessage.NVC_SIMPLE_MESSAGE;
            }
        },
        STAT_COUNTER(StatCounter.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public StatCounter newInstance() {
                return new StatCounter();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return StatCounter.NVC_STAT_COUNTER_DAO;
            }
        },
        STAT_INFO(StatInfo.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public StatInfo newInstance() {
                return new StatInfo();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return StatInfo.NVC_STAT_INFO;
            }
        },
        SUBJECT_API_KEY(SubjectAPIKey.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SubjectAPIKey newInstance() {
                return new SubjectAPIKey();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SubjectAPIKey.NVC_SUBJECT_API_KEY;
            }
        },


        SUBJECT_IDENTIFIER(SubjectIdentifier.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SubjectIdentifier newInstance() {
                return new SubjectIdentifier();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SubjectIdentifier.NVC_SUBJECT_IDENTIFIER;
            }
        },
        SYSTEM_INFO_DAO(SystemInfoDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SystemInfoDAO newInstance() {
                return new SystemInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SystemInfoDAO.NVC_SYSTEM_INFO_DAO;
            }
        },
        TICKET_CONTAINER_DAO(TicketContainer.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public TicketContainer newInstance() {
                return new TicketContainer();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return TicketContainer.NVC_TICKET_CONTAINER;
            }
        },
        TICKET_ISSUER_DAO(TicketIssuer.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public TicketIssuer newInstance() {
                return new TicketIssuer();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return TicketIssuer.NVC_TICKET_ISSUER;
            }
        },


        TICKET_RESOLUTION_DAO(TicketResolution.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public TicketResolution newInstance() {
                return new TicketResolution();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return TicketResolution.NVC_TICKET_RESOLUTION;
            }
        },
        USER_ID_DAO(UserIDDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public UserIDDAO newInstance() {
                return new UserIDDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return UserIDDAO.NVC_USER_ID_DAO;
            }
        },
        USER_INFO_DAO(UserInfoDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public UserInfoDAO newInstance() {
                return new UserInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return UserInfoDAO.NVC_USER_INFO_DAO;
            }
        },
        USER_PREFERENCE_DAO(SubjectPreference.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public SubjectPreference newInstance() {
                return new SubjectPreference();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return SubjectPreference.NVC_SUBJECT_PREFERENCE;
            }
        },
        UUID_INFO_DAO(UUIDInfoDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public UUIDInfoDAO newInstance() {
                return new UUIDInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return UUIDInfoDAO.NVC_UUID_INFO_DAO;
            }
        },
        VM_INFO_DAO(VMInfoDAO.class.getName()) {
            @SuppressWarnings("unchecked")
            @Override
            public VMInfoDAO newInstance() {
                return new VMInfoDAO();
            }

            @Override
            public NVConfigEntity getNVConfigEntity() {
                return VMInfoDAO.NVC_VMINFO_DAO;
            }
        },

        ;

        private final String name;

        NVEntityTypeClass(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
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
    private ZWDataFactory() {

    }

    public void registerFactory(NVEntityFactory factory) {
        if (factory != null) {
            factoriesSet.add(factory);
        }
    }

    /**
     * Creates NVEntity object based on given canonical ID.
     *
     * @param canonicalID of the NVEntity
     */
    @Override
    public <V extends NVEntity> V createNVEntity(String canonicalID) {
        if (!SUS.isEmpty(canonicalID)) {
            NVEntityTypeClass type = SUS.lookupEnum(canonicalID, NVEntityTypeClass.values());

            if (type == null) {
                for (NVEntityTypeClass nveTypeClass : NVEntityTypeClass.values()) {
                    if (canonicalID.equals(nveTypeClass.getNVConfigEntity().toCanonicalID())
                            || canonicalID.equals(nveTypeClass.getNVConfigEntity().getName())) {
                        type = nveTypeClass;
                        break;
                    }
                }
            }

            if (type != null) {
                return type.newInstance();
            }
        }

        for (NVEntityFactory fac : factoriesSet) {
            V ret = fac.createNVEntity(canonicalID);

            if (ret != null) {
                return ret;
            }
        }

        return null;
    }

}