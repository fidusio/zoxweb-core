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
package org.zoxweb.shared.net;

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class IPAddress
        extends SetNameDAO {

    private static final NVConfig INET_ADDRESS = NVConfigManager.createNVConfig("inet_address", "The ip address", "InetAddress", true, false, String.class);
    private static final NVConfig PORT = NVConfigManager.createNVConfig("port", "The port number", "Port", true, false, int.class);
    private static final NVConfig BACKLOG = NVConfigManager.createNVConfig("backlog", "How many pending connections", "BackLog", true, false, int.class);
    private static final NVConfig PROXY_TYPE = NVConfigManager.createNVConfig("proxy_type", "proxy type", "ProxyType", false, false, ProxyType.class);
    public static final NVConfigEntity NVC_IP_ADDRESS = new NVConfigEntityPortable("ip_address", null, "IPAddress", true, false, false, false, IPAddress.class, SharedUtil.toNVConfigList(INET_ADDRESS, PORT, BACKLOG, PROXY_TYPE), null, false, SetNameDAO.NVC_NAME_DAO);

    public IPAddress() {
        super(NVC_IP_ADDRESS);
    }

    public IPAddress(String addressPort) {
        this();
        String[] params = addressPort.split(":");
        switch (params.length) {
            case 1:
                // 2 possibilities
                // just port or address
                try {
                    setPort(Integer.parseInt(params[0]));
                } catch (Exception e) {
                    setInetAddress(params[0]);
                    setPort(-1);
                }
                break;
            case 3:
                setProxyType(params[2]);
            case 2:
                setInetAddress(params[0]);
                setPort(Integer.parseInt(params[1]));
                break;
            default:
                throw new IllegalArgumentException("Invalid address " + addressPort);
        }


//		setInetAddress(params[0]);
//
//		if (params.length > 1) {
//            setPort(Integer.parseInt(params[1]));
//        } else {
//            setPort(-1);
//        }
//		if(params.length > 2)
//		{
//			setProxyType(params[2]);
//		}
    }

    public IPAddress(String address, int port) {
        this(address, port, 128, null);
    }

    public IPAddress(String address, int port, ProxyType pt) {
        this(address, port, 128, pt);
    }

    public IPAddress(String address, int port, int backlog, ProxyType pt) {
        this();
        setInetAddress(address);
        setPort(port);
        setProxyType(pt);
        setBacklog(backlog);
    }

    public String getInetAddress() {
        return lookupValue(INET_ADDRESS);
    }

    public void setInetAddress(String address) {
        setValue(INET_ADDRESS, address);
    }

    public int getPort() {
        return lookupValue(PORT);
    }


    public void setProxyType(ProxyType pt) {
        setValue(PROXY_TYPE, pt);
    }

    public ProxyType getProxyType() {
        return lookupValue(PROXY_TYPE);
    }


    public void setPort(int port) {
        if (port < -1) {
            throw new IllegalArgumentException("Invalid port:" + port + " < 0 ");
        }

        setValue(PORT, port);
    }


    public void setBacklog(int blog) {
        if (blog < -1) {
            throw new IllegalArgumentException("Invalid backlog:" + blog + " < 0 ");
        }

        setValue(BACKLOG, blog);
    }


    public int getBacklog() {
        return lookupValue(BACKLOG);
    }

    public boolean equals(Object o) {
        if (o instanceof IPAddress) {
            if (getInetAddress() != null && getInetAddress().equalsIgnoreCase(((IPAddress) o).getInetAddress())) {

                if (getPort() == ((IPAddress) o).getPort()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        String val = "InetSocketAddressDAO:" + getInetAddress() + getPort();
        return val.hashCode();
    }


    public void setProxyType(String pType) {
        ProxyType pt = SharedUtil.lookupEnum(pType, ProxyType.values());
        setProxyType(pt);
    }

    @Override
    public String toString() {
        return SharedUtil.toCanonicalID(':', getInetAddress(), getPort());
    }

    public static IPAddress parse(String addressPortProxyType, ProxyType pt) {
        if (addressPortProxyType.toUpperCase().indexOf(pt.name()) == -1) {
            addressPortProxyType = addressPortProxyType + ":" + pt;
        }
        return new IPAddress(addressPortProxyType);
    }


    public static IPAddress parse(String addressPort) {
        return new IPAddress(addressPort);
    }

}