package org.zoxweb.shared.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateIPTest {

    @Test
    public void privateIPs() {
        String[] privateList = {
                "10.0.0.0",
                "10.0.0.1",
                "10.255.255.255",
                "10.1.2.3",
                "192.168.0.0",
                "192.168.0.1",
                "192.168.1.100",
                "192.168.255.255",
                "172.16.0.0",
                "172.16.0.1",
                "172.20.10.5",
                "172.31.255.255",
        };

        for (String ip : privateList) {
            assertTrue(SharedNetUtil.isPrivateIP(ip), ip + " should be private");
        }
    }

    @Test
    public void publicIPs() {
        String[] publicList = {
                "1.1.1.1",
                "8.8.8.8",
                "9.255.255.255",
                "11.0.0.0",
                "100.64.0.1",
                "126.0.0.1",
                "172.15.255.255",
                "172.32.0.0",
                "172.0.0.1",
                "192.167.255.255",
                "192.169.0.0",
                "128.128.128.128",
                "255.255.255.255",
        };

        for (String ip : publicList) {
            assertFalse(SharedNetUtil.isPrivateIP(ip), ip + " should not be private");
        }
    }

    @Test
    public void invalidInputs() {
        String[] invalidList = {
                null,
                "",
                "   ",
                "batata.com",
                "not-an-ip",
                "abc.def.ghi.jkl",
                "10",
                "1921680",
        };

        for (String ip : invalidList) {
            assertFalse(SharedNetUtil.isPrivateIP(ip), "[" + ip + "] should not be private");
        }
    }

    @Test
    public void trimmedInput() {
        assertTrue(SharedNetUtil.isPrivateIP("  10.0.0.1  "), "leading/trailing whitespace should be trimmed");
        assertFalse(SharedNetUtil.isPrivateIP("  8.8.8.8  "));
    }
}
