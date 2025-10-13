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
package org.zoxweb.server.net;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.net.InetFilterDAO;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InetFilterRulesManager {

    public static final LogWrapper log = new LogWrapper(InetFilterRulesManager.class).setEnabled(false);
    private final List<InetFilterRule> set = new ArrayList<>();

    @SuppressWarnings("serial")
    public static class InetFilterRule
            extends SetNameDAO
            implements Comparable<InetFilterRule> {
        private transient byte[] networkBytes = null;
        private transient byte[] maskBytes = null;

        public enum Params
                implements GetNVConfig {
            IP_FILTER(NVConfigManager.createNVConfigEntity("inet_filter_dao", null, "InetFilterDAO", false, true, InetFilterDAO.NVC_INET_FILTER_DAO)),
            STATUS(NVConfigManager.createNVConfig("security_status", null, "SecurityStatus", false, true, SecurityStatus.class));

            private final NVConfig cType;

            Params(NVConfig c) {
                cType = c;
            }

            public NVConfig getNVConfig() {
                return cType;
            }

        }

        /**
         * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on AddressDAO.
         */
        public static final NVConfigEntity NVC_INET_FILTER_RULE = new NVConfigEntityPortable("inet_filter_prop", null, "InetFilterProp", true, false, false, false, InetFilterRule.class, SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDAO.NVC_NAME_DAO);

        public InetFilterRule() {
            super(NVC_INET_FILTER_RULE);
        }

        public InetFilterRule(InetFilterDAO filter, SecurityStatus ss) throws IOException {
            super(NVC_INET_FILTER_RULE);
            setInetFilterDAO(filter);
            setSecurityStatus(ss);
        }

        /**
         * @return the ipFilter
         */
        public InetFilterDAO getInetFilterDAO() {
            return lookupValue(Params.IP_FILTER);
        }

        /**
         * @param inetFilter the ipFilter to set
         * @throws IOException
         */
        public synchronized void setInetFilterDAO(InetFilterDAO inetFilter) throws IOException {
            setValue(Params.IP_FILTER, inetFilter);
            //maskBytes    = InetAddress.getByName(inetFilter.getNetworkMask()).getAddress();
            byte[] networkBytesTemp = InetAddress.getByName(NetUtil.getNetwork(inetFilter.getIP(), inetFilter.getNetworkMask())).getAddress();
            inetFilter.setNetwork(InetAddress.getByAddress(networkBytesTemp).getHostAddress());

        }

        public byte[] getNetworkBytes() {

            if (networkBytes == null) {
                synchronized (this) {
                    if (networkBytes == null) {
                        try {
                            networkBytes = InetAddress.getByName(NetUtil.getNetwork(getInetFilterDAO().getIP(), getInetFilterDAO().getNetworkMask())).getAddress();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
            return networkBytes;
        }

        public byte[] getNetMaskBytes() {
            if (maskBytes == null && getInetFilterDAO().getNetworkMask() != null) {
                synchronized (this) {
                    if (maskBytes == null) {
                        try {
                            maskBytes = InetAddress.getByName(getInetFilterDAO().getNetworkMask()).getAddress();
                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
            return maskBytes;
        }

        /**
         * @return the filterType
         */
        public SecurityStatus getSecurityStatus() {
            return lookupValue(Params.STATUS);
        }

        /**
         * @param filterType the filterType to set
         */
        public void setSecurityStatus(SecurityStatus filterType) {
            setValue(Params.STATUS, filterType);
        }


        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(InetFilterRule o) {
            int ret = getSecurityStatus().compareTo(o.getSecurityStatus());
            if (ret == 0 && this != o) {
                return -1;
            }
            return ret;
        }
    }


    /**
     * Add a filter rule as a String format ip-netmask-[ALLOW|DENY] ex: 10.0.0.1-255.255.255.0-ALLOW, fidus-store.com-255.255.255.0-DENY
     *
     * @param rule
     * @throws IOException
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void addInetFilterProp(String rule) throws IOException, NullPointerException, IllegalArgumentException {

        String rules[] = rule.split("-");
        if (rules.length < 3) {
            throw new IllegalArgumentException("Invalid rule " + rule + "\n format ip-netmask-[deny|allow]");
        }
        int index = rules.length;
        SecurityStatus ss = (SecurityStatus) SharedUtil.lookupEnum(rules[--index], SecurityStatus.values());
        if (ss == null) {
            throw new IllegalArgumentException("Invalid rule " + rule + "\n format ip-netmask-[deny|allow]");
        }
        String netMask = rules[--index];
        StringBuilder sbIP = new StringBuilder();


        for (int i = 0; i < index; i++) {
            sbIP.append(rules[i]);
            if (i + 1 != index)
                sbIP.append('-');
        }

        InetFilterDAO ifd = new InetFilterDAO(sbIP.toString(), netMask);
        addInetFilterProp(new InetFilterRule(ifd, ss));

    }


    public void addInetFilterProp(InetFilterDAO ifd, SecurityStatus ss) throws IOException {
        addInetFilterProp(new InetFilterRule(ifd, ss));
    }


    public synchronized void addInetFilterProp(InetFilterRule ipfp) throws IOException {
        if (ipfp != null) {
            ipfp.getInetFilterDAO().setNetwork(NetUtil.getNetwork(ipfp.getInetFilterDAO().getIP(), ipfp.getInetFilterDAO().getNetworkMask()));
            set.add(ipfp);
        }
    }


//	public static IPFilterManager load(String filename) throws IOException
//	{
//		Object obj[] = ServerUtil.readXMLObjects(filename);
//		IPFilterManager ret = new IPFilterManager();
//		
//		for ( Object prop: obj)
//		{
//			if ( prop instanceof IPFilterProp)
//				ret.addIPFilterProp( (IPFilterProp) prop);
//		}
//		
//		return ret;
//	}
//	
//	
//	public static void save(String filename, IPFilterManager ipfm) throws IOException
//	{
//		
//	
//		XMLEncoder xe= UXMLHelper.createEncoder(filename);
//		try
//		{
//			for ( IPFilterProp prop : ipfm.getAll())
//			{
//				xe.writeObject( prop);
//				xe.flush();
//			}
//		}
//		
//		finally
//		{
//			ServerUtil.close(xe);
//		}
//	}


    public synchronized void removeInetFilterProp(InetFilterRule ipfp) {
        if (ipfp != null)
            set.remove(ipfp);

    }


    public SecurityStatus lookupSecurityStatus(InetAddress address) {
        if(log.isEnabled()) log.getLogger().info("address " + address);
        if (address.isLoopbackAddress()) {
            return SecurityStatus.ALLOW;
        }


        if (address instanceof Inet4Address) {
            if(log.isEnabled()) log.getLogger().info("Inet4Address to check:" + address);
            return checkIPSecurityStatus(address.getAddress());
        }

        if (address instanceof Inet6Address) {
            if(log.isEnabled()) log.getLogger().info("Inet6Address to check:" + address);
            return checkIPSecurityStatus(address.getAddress());
        }


        if(log.isEnabled()) log.getLogger().info("we have ip v6 deny access:" + address);
        // we ip v6
        return SecurityStatus.DENY;

    }


    public SecurityStatus lookupSecurityStatus(SocketAddress address) {
        if (address instanceof InetSocketAddress) {
            return lookupSecurityStatus(((InetSocketAddress) address).getAddress());
        }
        if(log.isEnabled()) log.getLogger().info("we have ip v6 deny access:" + address);
        // we ip v6
        return SecurityStatus.DENY;

    }


    public SecurityStatus lookupSecurityStatus(String ipAddress) throws IOException {
        return lookupSecurityStatus(InetAddress.getByName(ipAddress));
    }


    public synchronized SecurityStatus checkIPSecurityStatus(byte[] ipAddress) {

        SecurityStatus ret = null;

        for (InetFilterRule ipfp : set) {

            switch (ipfp.getSecurityStatus()) {
                case ALLOW:
                    try {

                        //.info(""+ipfp);
                        ret = SecurityStatus.DENY;
                        if (SharedNetUtil.belongsToNetwork(ipAddress, ipfp.getNetMaskBytes(), ipfp.getNetworkBytes())) {
                            return SecurityStatus.ALLOW;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case REJECT:
                case DENY:
                    try {
                        if(log.isEnabled()) log.getLogger().info(""+ipfp);
                        ret = SecurityStatus.ALLOW;
                        if (SharedNetUtil.belongsToNetwork(ipAddress, ipfp.getNetMaskBytes(), ipfp.getNetworkBytes())) {
                            if(log.isEnabled()) log.getLogger().info("deny:"+ipfp);
                            return SecurityStatus.DENY;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;

            }
        }
        // we had an empty list
        if (ret == null) {
            // default allow
            ret = SecurityStatus.ALLOW;
        }

        return ret;
    }

    public List<InetFilterRule> getAll() {
        return Arrays.asList(set.toArray(new InetFilterRule[0]));
    }


    public synchronized void setAll(List<InetFilterRule> props) throws IOException {
        set.clear();
        for (InetFilterRule ipfp : props) {
            addInetFilterProp(ipfp);
        }

    }


}
