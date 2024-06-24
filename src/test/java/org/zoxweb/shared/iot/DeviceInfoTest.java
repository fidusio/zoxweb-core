package org.zoxweb.shared.iot;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;

public class DeviceInfoTest
{
    @Test
    public void deviceInfoTest()
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("TEST-DEVICE");
        deviceInfo.setDescription("This is a test device");
        deviceInfo.setManufacturer("xlogistx inc");
        deviceInfo.setModel("APLHA-1");
        deviceInfo.setVersion("1.0");

        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        deviceInfo.addProtocol(i2c);


        System.out.println(GSONUtil.toJSONDefault(deviceInfo));
    }
}
