package org.zoxweb.server.net;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.InetProp.IPVersion;
import org.zoxweb.shared.net.ProxyType;
import org.zoxweb.shared.net.InetAddressDAO;
import org.zoxweb.shared.security.SecurityStatus;

import java.io.IOException;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;

public class NetUtilTest {

    // ==================== checkSecurityStatus Tests ====================

    @Test
    public void testCheckSecurityStatusWithNullManager() throws IOException {
        SecurityStatus status = NetUtil.checkSecurityStatus(null, "192.168.1.1", null);
        assertEquals(SecurityStatus.ALLOW, status);
    }

    @Test
    public void testCheckSecurityStatusSocketAddressWithNullManager() {
        SocketAddress addr = new InetSocketAddress("localhost", 8080);
        SecurityStatus status = NetUtil.checkSecurityStatus(null, addr, null);
        assertEquals(SecurityStatus.ALLOW, status);
    }

    @Test
    public void testCheckSecurityStatusInetAddressWithNullManager() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("127.0.0.1");
        SecurityStatus status = NetUtil.checkSecurityStatus(null, addr, null);
        assertEquals(SecurityStatus.ALLOW, status);
    }

    // ==================== lookup (ProxyType) Tests ====================

    @Test
    public void testLookupProxyTypeDirect() {
        assertEquals(Proxy.Type.DIRECT, NetUtil.lookup(ProxyType.DIRECT));
    }

    @Test
    public void testLookupProxyTypeHttp() {
        assertEquals(Proxy.Type.HTTP, NetUtil.lookup(ProxyType.HTTP));
    }

    @Test
    public void testLookupProxyTypeSocks() {
        assertEquals(Proxy.Type.SOCKS, NetUtil.lookup(ProxyType.SOCKS));
    }

    @Test
    public void testLookupProxyTypeNull() {
        assertEquals(Proxy.Type.DIRECT, NetUtil.lookup(null));
    }

    // ==================== parse Tests ====================

    @Test
    public void testParseAddressPort() {
        InetSocketAddress result = NetUtil.parse("localhost:8080");
        assertEquals("localhost", result.getHostName());
        assertEquals(8080, result.getPort());
    }

    @Test
    public void testParseIPAddressPort() {
        InetSocketAddress result = NetUtil.parse("192.168.1.1:443");
        assertEquals(443, result.getPort());
    }

    // ==================== toNetmaskIPV4 Tests ====================

    @Test
    public void testToNetmaskIPV4_24() throws IOException {
        InetAddress mask = NetUtil.toNetmaskIPV4((short) 24);
        assertEquals("255.255.255.0", mask.getHostAddress());
    }

    @Test
    public void testToNetmaskIPV4_16() throws IOException {
        InetAddress mask = NetUtil.toNetmaskIPV4((short) 16);
        assertEquals("255.255.0.0", mask.getHostAddress());
    }

    @Test
    public void testToNetmaskIPV4_8() throws IOException {
        InetAddress mask = NetUtil.toNetmaskIPV4((short) 8);
        assertEquals("255.0.0.0", mask.getHostAddress());
    }

    @Test
    public void testToNetmaskIPV4_32() throws IOException {
        InetAddress mask = NetUtil.toNetmaskIPV4((short) 32);
        assertEquals("255.255.255.255", mask.getHostAddress());
    }

    @Test
    public void testToNetmaskIPV4_0() throws IOException {
        InetAddress mask = NetUtil.toNetmaskIPV4((short) 0);
        assertEquals("0.0.0.0", mask.getHostAddress());
    }

    // ==================== getNetwork Tests ====================

    @Test
    public void testGetNetworkStringWithMask() throws IOException {
        String network = NetUtil.getNetwork("192.168.1.100", "255.255.255.0");
        assertEquals("192.168.1.0", network);
    }

    @Test
    public void testGetNetworkStringWithNullMask() throws IOException {
        String network = NetUtil.getNetwork("192.168.1.100", null);
        assertEquals("192.168.1.100", network);
    }

    @Test
    public void testGetNetworkClass16() throws IOException {
        String network = NetUtil.getNetwork("172.16.50.100", "255.255.0.0");
        assertEquals("172.16.0.0", network);
    }

    @Test
    public void testGetNetworkInetAddress() throws IOException {
        InetAddress address = InetAddress.getByName("10.20.30.40");
        InetAddress mask = InetAddress.getByName("255.255.255.0");
        InetAddress network = NetUtil.getNetwork(address, mask);
        assertEquals("10.20.30.0", network.getHostAddress());
    }

    // ==================== belongsToNetwork Tests ====================

    @Test
    public void testBelongsToNetworkTrue() throws IOException {
        byte[] network = InetAddress.getByName("192.168.1.0").getAddress();
        byte[] mask = InetAddress.getByName("255.255.255.0").getAddress();
        assertTrue(NetUtil.belongsToNetwork(network, mask, "192.168.1.50"));
    }

    @Test
    public void testBelongsToNetworkFalse() throws IOException {
        byte[] network = InetAddress.getByName("192.168.1.0").getAddress();
        byte[] mask = InetAddress.getByName("255.255.255.0").getAddress();
        assertFalse(NetUtil.belongsToNetwork(network, mask, "192.168.2.50"));
    }

    @Test
    public void testBelongsToNetworkWithNullMask() throws IOException {
        byte[] network = InetAddress.getByName("10.0.0.1").getAddress();
        assertTrue(NetUtil.belongsToNetwork(network, null, "10.0.0.1"));
    }

    @Test
    public void testBelongsToNetworkNullNetworkThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.belongsToNetwork(null, null, "192.168.1.1");
        });
    }

    @Test
    public void testBelongsToNetworkNullIPThrows() throws UnknownHostException {
        byte[] network = InetAddress.getByName("192.168.1.0").getAddress();
        assertThrows(NullPointerException.class, () -> {
            NetUtil.belongsToNetwork(network, null, null);
        });
    }

    // ==================== getInet4Address Tests ====================

    @Test
    public void testGetInet4AddressLocalhost() throws UnknownHostException {
        Inet4Address addr = NetUtil.getInet4Address("127.0.0.1");
        assertNotNull(addr);
        assertEquals("127.0.0.1", addr.getHostAddress());
    }

    @Test
    public void testGetInet4AddressNumeric() throws UnknownHostException {
        Inet4Address addr = NetUtil.getInet4Address("8.8.8.8");
        assertNotNull(addr);
        assertEquals("8.8.8.8", addr.getHostAddress());
    }

    // ==================== toPrivateIP Tests ====================

    @Test
    public void testToPrivateIP_10Network() {
        InetAddress addr = NetUtil.toPrivateIP("10.0.0.1");
        assertNotNull(addr);
        assertEquals("10.0.0.1", addr.getHostAddress());
    }

    @Test
    public void testToPrivateIP_10NetworkMax() {
        InetAddress addr = NetUtil.toPrivateIP("10.255.255.255");
        assertNotNull(addr);
        assertEquals("10.255.255.255", addr.getHostAddress());
    }

    @Test
    public void testToPrivateIP_192168Network() {
        InetAddress addr = NetUtil.toPrivateIP("192.168.1.1");
        assertNotNull(addr);
        assertEquals("192.168.1.1", addr.getHostAddress());
    }

    @Test
    public void testToPrivateIP_192168NetworkMax() {
        InetAddress addr = NetUtil.toPrivateIP("192.168.255.255");
        assertNotNull(addr);
    }

    @Test
    public void testToPrivateIP_172_16() {
        InetAddress addr = NetUtil.toPrivateIP("172.16.0.1");
        assertNotNull(addr);
        assertEquals("172.16.0.1", addr.getHostAddress());
    }

    @Test
    public void testToPrivateIP_172_31() {
        InetAddress addr = NetUtil.toPrivateIP("172.31.255.255");
        assertNotNull(addr);
    }

    @Test
    public void testToPrivateIP_172_15_NotPrivate() {
        InetAddress addr = NetUtil.toPrivateIP("172.15.0.1");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_172_32_NotPrivate() {
        InetAddress addr = NetUtil.toPrivateIP("172.32.0.1");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_PublicIP() {
        InetAddress addr = NetUtil.toPrivateIP("8.8.8.8");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_Null() {
        InetAddress addr = NetUtil.toPrivateIP(null);
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_Empty() {
        InetAddress addr = NetUtil.toPrivateIP("");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_Whitespace() {
        InetAddress addr = NetUtil.toPrivateIP("   ");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_Hostname() {
        InetAddress addr = NetUtil.toPrivateIP("localhost");
        assertNull(addr);
    }

    @Test
    public void testToPrivateIP_InvalidFormat() {
        InetAddress addr = NetUtil.toPrivateIP("not.an.ip.address");
        assertNull(addr);
    }

    // ==================== toInetAddressDAO Tests ====================

    @Test
    public void testToInetAddressDAOFromStringIPv4() throws IOException {
        InetAddressDAO dao = NetUtil.toInetAddressDAO("192.168.1.1");
        assertEquals("192.168.1.1", dao.getInetAddress());
        assertEquals(IPVersion.V4, dao.getIPVersion());
    }

    @Test
    public void testToInetAddressDAOFromStringIPv6() throws IOException {
        InetAddressDAO dao = NetUtil.toInetAddressDAO("::1");
        // IPv6 loopback can be represented as "::1" or expanded form
        assertTrue(dao.getInetAddress().equals("::1") || dao.getInetAddress().equals("0:0:0:0:0:0:0:1"));
        assertEquals(IPVersion.V6, dao.getIPVersion());
    }

    @Test
    public void testToInetAddressDAOFromStringNullThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.toInetAddressDAO((String) null);
        });
    }

    @Test
    public void testToInetAddressDAOFromInetAddressIPv4() throws IOException {
        InetAddress addr = InetAddress.getByName("10.0.0.1");
        InetAddressDAO dao = NetUtil.toInetAddressDAO(addr);
        assertEquals("10.0.0.1", dao.getInetAddress());
        assertEquals(IPVersion.V4, dao.getIPVersion());
    }

    @Test
    public void testToInetAddressDAOFromInetAddressIPv6() throws IOException {
        InetAddress addr = InetAddress.getByName("::1");
        InetAddressDAO dao = NetUtil.toInetAddressDAO(addr);
        assertEquals(IPVersion.V6, dao.getIPVersion());
    }

    @Test
    public void testToInetAddressDAOFromInetAddressNullThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.toInetAddressDAO((InetAddress) null);
        });
    }

    // ==================== areInetSocketAddressDAOEquals Tests ====================

    @Test
    public void testAreInetSocketAddressDAOEqualsTrue() throws UnknownHostException {
        IPAddress ip1 = new IPAddress("127.0.0.1", 8080);
        IPAddress ip2 = new IPAddress("127.0.0.1", 8080);
        assertTrue(NetUtil.areInetSocketAddressDAOEquals(ip1, ip2));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsSameHostDifferentPort() throws UnknownHostException {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        IPAddress ip2 = new IPAddress("localhost", 9090);
        assertFalse(NetUtil.areInetSocketAddressDAOEquals(ip1, ip2));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsDifferentHost() throws UnknownHostException {
        IPAddress ip1 = new IPAddress("192.168.1.1", 8080);
        IPAddress ip2 = new IPAddress("192.168.1.2", 8080);
        assertFalse(NetUtil.areInetSocketAddressDAOEquals(ip1, ip2));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsFirstNull() throws UnknownHostException {
        IPAddress ip2 = new IPAddress("localhost", 8080);
        assertFalse(NetUtil.areInetSocketAddressDAOEquals(null, ip2));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsSecondNull() throws UnknownHostException {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        assertFalse(NetUtil.areInetSocketAddressDAOEquals(ip1, null));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsBothNull() throws UnknownHostException {
        assertFalse(NetUtil.areInetSocketAddressDAOEquals(null, null));
    }

    @Test
    public void testAreInetSocketAddressDAOEqualsWithHostname() throws UnknownHostException {
        IPAddress ip1 = new IPAddress("localhost", 80);
        IPAddress ip2 = new IPAddress("127.0.0.1", 80);
        assertTrue(NetUtil.areInetSocketAddressDAOEquals(ip1, ip2));
    }

    // ==================== isActive Tests ====================

    @Test
    public void testIsActiveNullStringThrows() {
        // Note: isActive(String) does not handle null - it throws NPE from NetworkInterface.getByName()
        assertThrows(NullPointerException.class, () -> {
            NetUtil.isActive((String) null);
        });
    }

    @Test
    public void testIsActiveNonExistentInterface() throws IOException {
        assertFalse(NetUtil.isActive("non_existent_interface_xyz"));
    }

    @Test
    public void testIsActiveNullNetworkInterface() throws IOException {
        assertFalse(NetUtil.isActive((NetworkInterface) null));
    }

    // ==================== toNI Tests ====================

    @Test
    public void testToNINonExistent() throws IOException {
        NetworkInterface ni = NetUtil.toNI("non_existent_interface");
        assertNull(ni);
    }

    // ==================== ping Tests ====================

    @Test
    public void testPingNullHostThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.ping((String) null);
        });
    }

    @Test
    public void testPingNullAddressThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.ping((InetAddress) null, null);
        });
    }

    @Test
    public void testPingLocalhost() throws IOException {
        // Ping localhost should generally succeed
        boolean result = NetUtil.ping(InetAddress.getLoopbackAddress(), null, 255, 1000, false);
        // Note: This may fail on some systems depending on ICMP configuration
        System.out.println("Ping localhost result: " + result);
    }


    @Test
    public void testPingHosts() throws IOException {
        String[] hosts = new String[]{"127.0.0.1",
        "10.0.0.1","google.com"};

        for (String host : hosts) {

            boolean result = NetUtil.ping(InetAddress.getByName(host), null, 255, 1000, true);
            // Note: This may fail on some systems depending on ICMP configuration
            System.out.println("Ping localhost result: " + result);
        }
    }

    // ==================== Network Interface Tests (System Dependent) ====================

    @Test
    public void testGetLoopbackInterface() throws IOException {
        // Try to find loopback interface
        NetworkInterface loopback = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        if (loopback != null) {
            Inet4Address[] addresses = NetUtil.getIPV4AllAddresses(loopback);
            assertNotNull(addresses);
            System.out.println("Loopback IPv4 addresses: " + addresses.length);
        }
    }

    @Test
    public void testGetIPV4AllAddressesNullThrows() {
        assertThrows(NullPointerException.class, () -> {
            NetUtil.getIPV4AllAddresses((NetworkInterface) null);
        });
    }
}
