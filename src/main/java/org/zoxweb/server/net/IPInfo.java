/*
 * Copyright 2012 ZoxWeb.com LLC.
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

package org.zoxweb.server.net;


import org.zoxweb.server.util.RuntimeUtil;
import org.zoxweb.shared.data.RuntimeResultDAO;
import org.zoxweb.shared.net.InetProp.IPVersion;
import org.zoxweb.shared.net.NIConfigDAO;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.util.SUS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;


/**
 * This class contains all the IP Information associated with a network connection.
 * This class will work with Linux for now windows support might come soon.
 * <br> This class will return the following info:
 * <ul>
 * <li> IP address.
 * <li> Network mask.
 * <li> Network address.
 * <li> Default gateway or router.
 * <li> Broadcast ip address.
 * </ul>
 *
 * @author mnael
 */

public class IPInfo {


    public static String LINUX_NI_CFG_PREFIX = "/etc/sysconfig/network-scripts/ifcfg-";
    public static String LINUX_DHCP_NI_CFG_PREFIX = "/var/lib/dhclient/dhclient-";
    public static String LINUX_DHCP_NI_CFG_POSTFIX = ".leases";
    public static String LINUX_DHCP_ROUTERS = "option routers";

    public static final String LINUX_BOOTPROTO = "BOOTPROTO";
    public static final String LINUX_GATEWAY = "GATEWAY";


    private IPVersion ipv;
    private NetworkInterface ni;

    private IPInfo(NetworkInterface ni, IPVersion ipv) {
        SUS.checkIfNulls("NetworkInterface or ipv can't be null", ni, ipv);
        if (ipv == IPVersion.V6) {
            throw new IllegalArgumentException("IPV6 is not supported");
        }
        this.ni = ni;
        this.ipv = ipv;
    }


    public InetAddress getIPAddress()
            throws IOException {
        InetAddress ret = null;
        switch (ipv) {
            case V4:
                ret = NetUtil.getIPV4MainAddress(ni);
                break;
            case V6:
                break;
        }


        return ret;
    }

    public InetAddress getNetworkMask() throws IOException {
        InetAddress ret = null;
        switch (ipv) {
            case V4:
                List<InterfaceAddress> lia = ni.getInterfaceAddresses();
                InetAddress main = getIPAddress();
                for (InterfaceAddress ia : lia) {

                    if (main.equals(ia.getAddress())) {
                        ret = NetUtil.toNetmaskIPV4(ia.getNetworkPrefixLength());
                        break;
                    }
                }
                break;
            case V6:
                break;
        }


        return ret;
    }

    public InetAddress getNetwork() throws IOException {
        InetAddress ret = null;
        switch (ipv) {
            case V4:
                List<InterfaceAddress> lia = ni.getInterfaceAddresses();
                InetAddress main = getIPAddress();
                for (InterfaceAddress ia : lia) {

                    if (main.equals(ia.getAddress())) {
                        ret = NetUtil.getNetwork(ia);
                        break;
                    }
                }
                break;
            case V6:
                break;
        }

        return ret;
    }

    public InetAddress getBroadcast() throws IOException {
        InetAddress ret = null;
        switch (ipv) {
            case V4:
                List<InterfaceAddress> lia = ni.getInterfaceAddresses();
                InetAddress main = getIPAddress();
                for (InterfaceAddress ia : lia) {

                    if (main.equals(ia.getAddress())) {
                        ret = ia.getBroadcast();
                        break;
                    }
                }
                break;
            case V6:
                break;
        }

        return ret;
    }

    public InetAddress getGateway()
            throws IOException {
        return getLinuxRouter();
    }


    private InetAddress getLinuxRouter()
            throws IOException {
        InetAddress ret = null;
        switch (ipv) {
            case V4:

                try {
                    RuntimeResultDAO rrd = RuntimeUtil.runAndFinish("netstat -rn");
                    BufferedReader br = new BufferedReader(new StringReader(rrd.getOutputData()));
                    String line;
                    int len = 8;
                    while ((line = br.readLine()) != null) {
                        String data[] = line.split("\\s+");
                        if (data.length == len) {
                            if (data[len - 1].equals(getNetworkInterface().getName())) {
                                return InetAddress.getByName(data[1]);
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                break;

            case V6:
                break;
        }

        return ret;
    }


    public IPVersion getIPVersion() {
        return ipv;
    }


    public NetworkInterface getNetworkInterface() {
        return ni;
    }


    public static IPInfo getV4IPInfo(String niName) throws IOException {
        return new IPInfo(NetworkInterface.getByName(niName), IPVersion.V4);
    }

    public static NIConfigDAO getNIConfigInfo(NIConfigDAO nicd) throws IOException {
        IPInfo ipi = getV4IPInfo(nicd.getNIName());
        nicd.setAddress(SharedNetUtil.toV4Address(ipi.getIPAddress().getAddress()));
        nicd.setNetmask(SharedNetUtil.toV4Address(ipi.getNetworkMask().getAddress()));
        nicd.setGateway(SharedNetUtil.toV4Address(ipi.getGateway().getAddress()));
        return nicd;
    }

    public static IPInfo getV4IPInfo(NetworkInterface ni)
            throws IOException {
        return new IPInfo(ni, IPVersion.V4);
    }


}
