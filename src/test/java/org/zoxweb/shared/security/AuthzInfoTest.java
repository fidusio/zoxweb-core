package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.AppIDDAO;

import static org.junit.jupiter.api.Assertions.*;

class AuthzInfoTest {

    /**
     * Minimal concrete fixture so the abstract base can be exercised directly.
     */
    static class TestAuthzInfo extends AuthzInfo {
        public TestAuthzInfo() {
            super();
        }
    }

    // ---------- Construction ----------

    @Test
    void defaultConstructorHasNullFields() {
        TestAuthzInfo info = new TestAuthzInfo();

        assertNull(info.getGUID());
        assertNull(info.getSubjectGUID());
        assertNull(info.getBrokerGUID());
        assertNull(info.getName());
        assertNull(info.getDescription());
        assertNull(info.getAppIdDAO());
    }

    @Test
    void propertiesMapIsInitialized() {
        TestAuthzInfo info = new TestAuthzInfo();
        assertNotNull(info.getProperties());
    }

    // ---------- BrokerGUID ----------

    @Test
    void setAndGetBrokerGUID() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setBrokerGUID("broker-123");
        assertEquals("broker-123", info.getBrokerGUID());
    }

    @Test
    void setBrokerGUIDOverwritesPreviousValue() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setBrokerGUID("first");
        info.setBrokerGUID("second");
        assertEquals("second", info.getBrokerGUID());
    }

    @Test
    void setBrokerGUIDToNullClearsValue() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setBrokerGUID("broker-123");
        info.setBrokerGUID(null);
        assertNull(info.getBrokerGUID());
    }

    // ---------- AppId (AppIDDAO reference) ----------

    @Test
    void setAndGetAppIdDAO() {
        TestAuthzInfo info = new TestAuthzInfo();
        AppIDDAO ref = new AppIDDAO("acme.com", "billing");
        info.setAppIdDAO(ref);

        assertSame(ref, info.getAppIdDAO());
    }

    @Test
    void appIdCarriesDomainAndAppStrings() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setAppIdDAO(new AppIDDAO("acme.com", "billing"));

        assertEquals("acme.com", info.getAppIdDAO().getDomainID());
        assertEquals("billing", info.getAppIdDAO().getAppID());
    }

    @Test
    void setAppIdDAOOverwritesPreviousValue() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setAppIdDAO(new AppIDDAO("first.com", "app1"));
        info.setAppIdDAO(new AppIDDAO("second.com", "app2"));

        assertEquals("second.com", info.getAppIdDAO().getDomainID());
        assertEquals("app2", info.getAppIdDAO().getAppID());
    }

    @Test
    void setAppIdDAOToNullClearsValue() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setAppIdDAO(new AppIDDAO("acme.com", "billing"));
        info.setAppIdDAO(null);
        assertNull(info.getAppIdDAO());
    }

    // ---------- Inherited: GUID (ReferenceIDDAO) ----------

    @Test
    void setAndGetGUID() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setGUID("guid-abc");
        assertEquals("guid-abc", info.getGUID());
    }

    @Test
    void setGUIDToNullClearsValue() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setGUID("guid-abc");
        info.setGUID(null);
        assertNull(info.getGUID());
    }

    // ---------- Inherited: SubjectGUID (ReferenceIDDAO) ----------

    @Test
    void setAndGetSubjectGUID() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setSubjectGUID("subject-xyz");
        assertEquals("subject-xyz", info.getSubjectGUID());
    }

    @Test
    void guidAndSubjectGUIDAreIndependent() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setGUID("guid-1");
        info.setSubjectGUID("subject-1");

        assertEquals("guid-1", info.getGUID());
        assertEquals("subject-1", info.getSubjectGUID());
    }

    // ---------- Inherited: Name / Description ----------

    @Test
    void setAndGetName() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setName("read-files");
        assertEquals("read-files", info.getName());
    }

    @Test
    void setAndGetDescription() {
        TestAuthzInfo info = new TestAuthzInfo();
        info.setDescription("Permission to read files");
        assertEquals("Permission to read files", info.getDescription());
    }

    // ---------- Full round-trip ----------

    @Test
    void allFieldsRoundTripIndependently() {
        TestAuthzInfo info = new TestAuthzInfo();

        info.setGUID("guid-1");
        info.setSubjectGUID("subject-1");
        info.setBrokerGUID("broker-1");
        info.setName("perm.read");
        info.setDescription("Read permission");
        info.setAppIdDAO(new AppIDDAO("acme.com", "billing"));

        assertEquals("guid-1", info.getGUID());
        assertEquals("subject-1", info.getSubjectGUID());
        assertEquals("broker-1", info.getBrokerGUID());
        assertEquals("perm.read", info.getName());
        assertEquals("Read permission", info.getDescription());
        assertEquals("acme.com", info.getAppIdDAO().getDomainID());
        assertEquals("billing", info.getAppIdDAO().getAppID());
    }

    // ---------- Concrete subclasses inherit AuthzInfo behavior ----------

    @Test
    void permissionInfoIsAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new PermissionInfo());
    }

    @Test
    void roleInfoIsAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleInfo());
    }

    @Test
    void roleGroupInfoIsAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleGroupInfo());
    }

    @Test
    void permissionInfoNVCEMergesAuthzInfoAttributes() {
        // NVConfigEntityPortable merges the superClass's attributes inline rather than
        // storing a parent reference, so we verify the inherited slots are reachable.
        assertNotNull(PermissionInfo.NVC_PERMISSION_INFO.lookup("broker_guid"));
        assertNotNull(PermissionInfo.NVC_PERMISSION_INFO.lookup("app_id"));
        assertNotNull(PermissionInfo.NVC_PERMISSION_INFO.lookup("permission_token"));
    }

    @Test
    void roleInfoNVCEMergesAuthzInfoAttributes() {
        assertNotNull(RoleInfo.NVC_ROLE_INFO.lookup("broker_guid"));
        assertNotNull(RoleInfo.NVC_ROLE_INFO.lookup("app_id"));
        assertNotNull(RoleInfo.NVC_ROLE_INFO.lookup("permission_guids"));
    }

    @Test
    void roleGroupInfoNVCEMergesAuthzInfoAttributes() {
        assertNotNull(RoleGroupInfo.NVC_ROLE_GROUP_INFO.lookup("broker_guid"));
        assertNotNull(RoleGroupInfo.NVC_ROLE_GROUP_INFO.lookup("app_id"));
        assertNotNull(RoleGroupInfo.NVC_ROLE_GROUP_INFO.lookup("role_guids"));
    }

    @Test
    void permissionInfoInheritsBrokerGUIDAndAppId() {
        PermissionInfo p = new PermissionInfo("perm.read", "read:files");
        p.setBrokerGUID("broker-p");
        p.setAppIdDAO(new AppIDDAO("acme.com", "billing"));

        assertEquals("broker-p", p.getBrokerGUID());
        assertEquals("acme.com", p.getAppIdDAO().getDomainID());
        assertEquals("billing", p.getAppIdDAO().getAppID());
    }

    @Test
    void roleInfoInheritsBrokerGUIDAndAppId() {
        RoleInfo r = new RoleInfo();
        r.setBrokerGUID("broker-r");
        r.setAppIdDAO(new AppIDDAO("acme.com", "billing"));

        assertEquals("broker-r", r.getBrokerGUID());
        assertEquals("acme.com", r.getAppIdDAO().getDomainID());
        assertEquals("billing", r.getAppIdDAO().getAppID());
    }

    @Test
    void roleGroupInfoInheritsBrokerGUIDAndAppId() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.setBrokerGUID("broker-g");
        g.setAppIdDAO(new AppIDDAO("acme.com", "billing"));

        assertEquals("broker-g", g.getBrokerGUID());
        assertEquals("acme.com", g.getAppIdDAO().getDomainID());
        assertEquals("billing", g.getAppIdDAO().getAppID());
    }

    @Test
    void inheritedSettersAreIndependentAcrossSubclasses() {
        PermissionInfo p = new PermissionInfo("perm.name", "perm");
        RoleInfo r = new RoleInfo();
        RoleGroupInfo g = new RoleGroupInfo();

        p.setBrokerGUID("broker-p");
        r.setBrokerGUID("broker-r");
        g.setBrokerGUID("broker-g");

        assertEquals("broker-p", p.getBrokerGUID());
        assertEquals("broker-r", r.getBrokerGUID());
        assertEquals("broker-g", g.getBrokerGUID());
    }
}
