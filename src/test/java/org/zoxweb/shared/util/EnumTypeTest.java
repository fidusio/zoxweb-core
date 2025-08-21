package org.zoxweb.shared.util;


import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.security.SubjectInfo;
import org.zoxweb.shared.util.Const.TimeInMillis;

import java.util.List;

public class EnumTypeTest {

    public static class EnumTester
            extends PropertyDAO {
        public enum Param
                implements GetNVConfig {
            SUBJECT_GUID(NVConfigManager.createNVConfig(MetaToken.SUBJECT_GUID.getName(), "The subject global identifier.", "SubjectGUID", true, false, true, String.class, null)),
            EMAIL(NVConfigManager.createNVConfig("email", "Primary email address", "Email", true, true, false, String.class, FilterType.EMAIL)),
            SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "Subject Type", "SubjectType", true, true, BaseSubjectID.SubjectType.class)),
            SUBJECT_STATUS(NVConfigManager.createNVConfig("subject_status", "Subject status", "SubjectStatus", true, true, CryptoConst.SubjectStatus[].class));
            private final NVConfig nvc;

            Param(NVConfig nvc) {
                this.nvc = nvc;
            }

            public NVConfig getNVConfig() {
                return nvc;
            }
        }

        public static final NVConfigEntity NVC_ENUM_TESTER = new NVConfigEntityLocal(
                "enum_tester",
                null,
                SubjectInfo.class.getName(),
                true,
                false,
                false,
                false,
                SubjectInfo.class,
                SharedUtil.extractNVConfigs(Param.values()),
                null,
                false,
                PropertyDAO.NVC_PROPERTY_DAO
        );

        public EnumTester() {
            super(NVC_ENUM_TESTER);
        }


        public BaseSubjectID.SubjectType getSubjectType() {
            return lookupValue(Param.SUBJECT_TYPE);
        }

        public void setSubjectType(BaseSubjectID.SubjectType type) {
            setValue(Param.SUBJECT_TYPE, type);
        }
    }

    @Test
    public void testType() {
        System.out.println(TimeInMillis.class.getTypeName());
    }


    @Test
    void meta() {
        EnumTester et = new EnumTester();
        et.setSubjectType(BaseSubjectID.SubjectType.USER);
        List<CryptoConst.SubjectStatus> statusList = et.lookupValue(EnumTester.Param.SUBJECT_STATUS);
        statusList.add(CryptoConst.SubjectStatus.ACTIVE);
        statusList.add(CryptoConst.SubjectStatus.PENDING_ACCOUNT_ACTIVATION);
        String json = GSONUtil.toJSONDefault(et, true);
        System.out.println(json);
        NVConfigEntity nvce = (NVConfigEntity) et.getNVConfig();
        NVConfig nvc = nvce.lookup(EnumTester.Param.SUBJECT_STATUS.getNVConfig().getName());
        Class<?> c = nvc.getMetaType();
        Class<?> base = nvc.getMetaTypeBase();
        et = GSONUtil.fromJSONDefault(json, EnumTester.class);
        String json2 = GSONUtil.toJSONDefault(et, true);
        assert json2.equals(json);
        System.out.println(SUS.toCanonicalID(',', base, base.isArray(), base.isEnum()) + " " + SUS.toCanonicalID(',', c, c.isArray(), c.isEnum()));
        System.out.println(nvc.isEnum() + " " + nvc.isArray());


    }

}
