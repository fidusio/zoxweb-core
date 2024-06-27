package org.zoxweb.shared.iot;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.Const;

public class DeviceInfoTest
{
    @Test
    public void deviceInfoTest()
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("ATTINY84");
        deviceInfo.setDescription("14 pin Atmel microcontroller");
        deviceInfo.setManufacturer("Microchip");
        deviceInfo.setModel("PU");


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        deviceInfo.addProtocol(i2c);

        deviceInfo.addPort(new PortInfo("VCC", "Input voltage").setPort(1));
        deviceInfo.addPort(new PortInfo("PB0", "General pin").setPort(2).setFunctions("PCINT8", "10", "0", "XTAL1"));
        deviceInfo.addPort(new PortInfo("PB1", "General pin").setPort(3).setFunctions("PCINT9", "9", "1", "XTAL2"));
        deviceInfo.addPort(new PortInfo("PB3", "General pin").setPort(4).setFunctions("PCINT11", "11", "11", "RESET"));
        deviceInfo.addPort(new PortInfo("PB2", "General pin").setPort(5).setFunctions("PCINT10", "8", "2", "OC0A", "INT0"));
        deviceInfo.addPort(new PortInfo("PA7", "General pin").setPort(6).setFunctions("PCINT7", "7", "3", "OC0B", "ADC7"));
        deviceInfo.addPort(new PortInfo("PA6", "General pin").setPort(7).setFunctions("PCINT6", "6", "4", "MOSI", "DI", "SDA", "OC1A", "ADC6"));
        deviceInfo.addPort(new PortInfo("PA5", "General pin").setPort(8).setFunctions("PCINT5", "5", "5", "MISO", "DO", "OC1B", "ADC5"));
        deviceInfo.addPort(new PortInfo("PA4", "General pin").setPort(9).setFunctions("PCINT4", "4", "6", "SCK", "SCL", "ADC4"));
        deviceInfo.addPort(new PortInfo("PA3", "General pin").setPort(10).setFunctions("PCINT3", "3", "7", "ADC3"));
        deviceInfo.addPort(new PortInfo("PA2", "General pin").setPort(11).setFunctions("PCINT2", "2", "8", "AIN1", "ADC2"));
        deviceInfo.addPort(new PortInfo("PA1", "General pin").setPort(12).setFunctions("PCINT1", "1", "9", "AIN0", "ADC1"));
        deviceInfo.addPort(new PortInfo("PA0", "General pin").setPort(13).setFunctions("PCINT0", "0", "10", "AREF", "ADC0"));




        deviceInfo.addPort(new PortInfo("GND", "ground").setPort(14));

        String regex = Const.RegEx.CONTAINS_NO_CASE.toRegEx("PB");
        System.out.println(regex);
        System.out.println(deviceInfo.lookupPorts(regex));



        System.out.println(GSONUtil.toJSONDefault(deviceInfo, true));
    }

//    @Test
//    public void regex()
//    {
//        String[] texts = {"{tag>content</tag>", "<TAG>CONTENT</TAG}", "<[Tag>Content</Tag>", "fail"};
//        String tag = "tag";
//
//        // Construct the regex pattern in a case-insensitive manner
//        String patternString = "(?i).*" + tag + "(.*?)";
//
//        for (String text : texts) {
//            boolean matches = text.matches(patternString);
//            System.out.println("Text: " + text + " Matches: " + matches);
//        }
//
//
//        for (String text : texts) {
//            boolean matches = text.matches(Const.RegEx.CONTAINS_NO_CASE.toRegEx("TaG"));
//            System.out.println("Text: " + text + " Matches: " + matches);
//        }
//    }
}
