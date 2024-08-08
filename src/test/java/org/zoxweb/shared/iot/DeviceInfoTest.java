package org.zoxweb.shared.iot;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.Range;
import org.zoxweb.shared.util.NVGenericMap;

import java.util.Set;

public class DeviceInfoTest
{
    @Test
    public void attiny84InfoTest()
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("ATTINY84");
        deviceInfo.setDescription("14 pin Atmel microcontroller");
        deviceInfo.setManufacturer("Microchip").setModel("PU").setForm("DIP14/SOIC14");


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        deviceInfo.addProtocol(i2c);

        deviceInfo.addPort(new PortInfo("VCC", "Input voltage").setPort(1).setFunctions("VCC"));
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




        deviceInfo.addPort(new PortInfo("GND", "ground").setPort(14).setFunctions("GND"));
        Set<PortInfo> ports = deviceInfo.lookupPorts("PB");
        System.out.println(ports.size() + ": "  + ports);


        System.out.println(GSONUtil.toJSONDefault(deviceInfo, true));
    }


    @Test
    public void attiny85InfoTest()
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("ATTINY85");
        deviceInfo.setDescription("8 pin Atmel microcontroller");
        deviceInfo.setManufacturer("Microchip").setForm("DIP8");
        deviceInfo.setModel("P");


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        deviceInfo.addProtocol(i2c);




        deviceInfo.addPort(new PortInfo("PB5", "General pin").setPort(1).setFunctions("PCINT5", "0", "5", "RESET", "ADC0"));
        deviceInfo.addPort(new PortInfo("PB3", "General pin").setPort(2).setFunctions("PCINT3", "3", "3", "ADC3","XTAL2"));
        deviceInfo.addPort(new PortInfo("PB4", "General pin").setPort(3).setFunctions("PCINT4", "4", "2", "OC1B", "ADC2", "XTAL1"));
        deviceInfo.addPort(new PortInfo("GND", "ground").setPort(4));


        deviceInfo.addPort(new PortInfo("PB0", "General pin").setPort(5).setFunctions("PCINT0", "0", "MOSI", "DI", "SDA", "OC0A", "AIN0", "AREF", "TXD"));
        deviceInfo.addPort(new PortInfo("PB1", "General pin").setPort(6).setFunctions("PCINT1", "1", "MISO", "DO", "OC0B", "OC1A", "AIN1", "RDX"));
        deviceInfo.addPort(new PortInfo("PB2", "General pin").setPort(7).setFunctions("PCINT2", "2", "1", "SCK", "SCL", "INT0", "ADC1"));
        deviceInfo.addPort(new PortInfo("8-VCC", "Input voltage").setPort(8).setFunctions("VCC"));





        Set<PortInfo> ports = deviceInfo.lookupPorts("PB");
        System.out.println(ports.size() + ": "  + ports);


        System.out.println(GSONUtil.toJSONDefault(deviceInfo, true));
    }


    @Test
    public void atmega328PInfoTest()
    {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("ATMEGA328P");
        deviceInfo.setDescription("32 pin Atmel microcontroller");
        deviceInfo.setManufacturer("Microchip").setForm("32-TQFP");
        deviceInfo.setModel("PB");
        // memory
        deviceInfo.getProperties().add(new NVGenericMap("memory").build("program", "32K")
                .build("eeprom", "1K")
                .build("ram", "2K"));
        // CPU
        deviceInfo.getProperties().add(new NVGenericMap("cpu").build("processor", "AVR")
                .build("core", "8-Bits")
                .build("max_speed", "20MHz")
                .build(Range.toRange("[-40, 105]","OpTemp", "C")));


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        deviceInfo.addProtocol(i2c);






//        deviceInfo.addPort(new PortInfo("PB5", "General pin").setPort(1).setFunctions("PCINT5", "0", "5", "RESET", "ADC0"));
//        deviceInfo.addPort(new PortInfo("PB3", "General pin").setPort(2).setFunctions("PCINT3", "3", "3", "ADC3","XTAL2"));
//        deviceInfo.addPort(new PortInfo("PB4", "General pin").setPort(3).setFunctions("PCINT4", "4", "2", "OC1B", "ADC2", "XTAL1"));
//        deviceInfo.addPort(new PortInfo("GND", "ground").setPort(4));
//
//
//        deviceInfo.addPort(new PortInfo("PB0", "General pin").setPort(5).setFunctions("PCINT0", "0", "MOSI", "DI", "SDA", "OC0A", "AIN0", "AREF", "TXD"));
//        deviceInfo.addPort(new PortInfo("PB1", "General pin").setPort(6).setFunctions("PCINT1", "1", "MISO", "DO", "OC0B", "OC1A", "AIN1", "RDX"));
//        deviceInfo.addPort(new PortInfo("PB2", "General pin").setPort(7).setFunctions("PCINT2", "2", "1", "SCK", "SCL", "INT0", "ADC1"));
//        deviceInfo.addPort(new PortInfo("8-VCC", "Input voltage").setPort(8).setFunctions("VCC"));
//
//
//
//
//
//        Set<PortInfo> ports = deviceInfo.lookupPorts("PB");
//        System.out.println(ports.size() + ": "  + ports);

        String json = GSONUtil.toJSONDefault(deviceInfo, true);
        System.out.println();
        DeviceInfo rebuild = GSONUtil.fromJSONDefault(json, DeviceInfo.class);
        String json2 = GSONUtil.toJSONDefault(rebuild, true);
        assert json2.equals(json);
        NVGenericMap cpu = rebuild.getProperties().lookupSubNVMG("cpu", false);
        assert cpu !=null;
        Range temp = cpu.getValue("OpTemp");
        System.out.println(temp + " " + temp.getUnit());
    }



    @Test
    public void testPortInfo()
    {
        PortInfo pi = new PortInfo().setFunctions("batata").setAlias("dsfdsf");
    }

}
