package org.zoxweb.shared.iot;

import org.zoxweb.shared.util.NVGenericMap;

public interface DeviceFactory
{
    /**
     * Create a device info based on its params, params can be null based on the implementation  it can return null
     * or a default device
     * @param param specs of the required device
     * @return the device info object or null depending on implementation
     */
    DeviceInfo createDevice(NVGenericMap param);
}
