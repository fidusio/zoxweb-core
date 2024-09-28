package org.zoxweb.shared.data.events;

import org.zoxweb.shared.net.IPAddress;

@SuppressWarnings("serial")
public class IPAddressEvent extends BaseEventObject<IPAddress> {
    public IPAddressEvent(Object source, IPAddress address) {
       super(source, address);
    }
}



