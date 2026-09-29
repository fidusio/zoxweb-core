package org.zoxweb.shared.iot;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.Range;
import org.zoxweb.shared.util.NVGenericMap;

import java.util.Set;

public class IOTDeviceInfoTest
{
    @Test
    public void attiny84InfoTest()
    {
        IOTDeviceInfo iotDeviceInfo = new IOTDeviceInfo();
        iotDeviceInfo.setName("ATTINY84");
        iotDeviceInfo.setDescription("14 pin Atmel microcontroller");
        iotDeviceInfo.setManufacturer("Microchip").setModel("PU").setForm("DIP14/SOIC14");


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        iotDeviceInfo.addProtocol(i2c);

        iotDeviceInfo.addPort(new PortInfo("VCC", "Input voltage").setPort(1).setFunctions("VCC"));
        iotDeviceInfo.addPort(new PortInfo("PB0", "General pin").setPort(2).setFunctions("PCINT8", "10", "0", "XTAL1"));
        iotDeviceInfo.addPort(new PortInfo("PB1", "General pin").setPort(3).setFunctions("PCINT9", "9", "1", "XTAL2"));
        iotDeviceInfo.addPort(new PortInfo("PB3", "General pin").setPort(4).setFunctions("PCINT11", "11", "11", "RESET"));
        iotDeviceInfo.addPort(new PortInfo("PB2", "General pin").setPort(5).setFunctions("PCINT10", "8", "2", "OC0A", "INT0"));
        iotDeviceInfo.addPort(new PortInfo("PA7", "General pin").setPort(6).setFunctions("PCINT7", "7", "3", "OC0B", "ADC7"));
        iotDeviceInfo.addPort(new PortInfo("PA6", "General pin").setPort(7).setFunctions("PCINT6", "6", "4", "MOSI", "DI", "SDA", "OC1A", "ADC6"));
        iotDeviceInfo.addPort(new PortInfo("PA5", "General pin").setPort(8).setFunctions("PCINT5", "5", "5", "MISO", "DO", "OC1B", "ADC5"));
        iotDeviceInfo.addPort(new PortInfo("PA4", "General pin").setPort(9).setFunctions("PCINT4", "4", "6", "SCK", "SCL", "ADC4"));
        iotDeviceInfo.addPort(new PortInfo("PA3", "General pin").setPort(10).setFunctions("PCINT3", "3", "7", "ADC3"));
        iotDeviceInfo.addPort(new PortInfo("PA2", "General pin").setPort(11).setFunctions("PCINT2", "2", "8", "AIN1", "ADC2"));
        iotDeviceInfo.addPort(new PortInfo("PA1", "General pin").setPort(12).setFunctions("PCINT1", "1", "9", "AIN0", "ADC1"));
        iotDeviceInfo.addPort(new PortInfo("PA0", "General pin").setPort(13).setFunctions("PCINT0", "0", "10", "AREF", "ADC0"));




        iotDeviceInfo.addPort(new PortInfo("GND", "ground").setPort(14).setFunctions("GND"));
        Set<PortInfo> ports = iotDeviceInfo.lookupPorts("PB");
        System.out.println(ports.size() + ": "  + ports);


        System.out.println(GSONUtil.toJSONDefault(iotDeviceInfo, true));
    }


    @Test
    public void attiny85InfoTest()
    {
        IOTDeviceInfo iotDeviceInfo = new IOTDeviceInfo();
        iotDeviceInfo.setName("ATTINY85");
        iotDeviceInfo.setDescription("8 pin Atmel microcontroller");
        iotDeviceInfo.setManufacturer("Microchip").setForm("DIP8");
        iotDeviceInfo.setModel("P");


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        iotDeviceInfo.addProtocol(i2c);




        iotDeviceInfo.addPort(new PortInfo("PB5", "General pin").setPort(1).setFunctions("PCINT5", "0", "5", "RESET", "ADC0"));
        iotDeviceInfo.addPort(new PortInfo("PB3", "General pin").setPort(2).setFunctions("PCINT3", "3", "3", "ADC3","XTAL2"));
        iotDeviceInfo.addPort(new PortInfo("PB4", "General pin").setPort(3).setFunctions("PCINT4", "4", "2", "OC1B", "ADC2", "XTAL1"));
        iotDeviceInfo.addPort(new PortInfo("GND", "ground").setPort(4));


        iotDeviceInfo.addPort(new PortInfo("PB0", "General pin").setPort(5).setFunctions("PCINT0", "0", "MOSI", "DI", "SDA", "OC0A", "AIN0", "AREF", "TXD"));
        iotDeviceInfo.addPort(new PortInfo("PB1", "General pin").setPort(6).setFunctions("PCINT1", "1", "MISO", "DO", "OC0B", "OC1A", "AIN1", "RDX"));
        iotDeviceInfo.addPort(new PortInfo("PB2", "General pin").setPort(7).setFunctions("PCINT2", "2", "1", "SCK", "SCL", "INT0", "ADC1"));
        iotDeviceInfo.addPort(new PortInfo("8-VCC", "Input voltage").setPort(8).setFunctions("VCC"));





        Set<PortInfo> ports = iotDeviceInfo.lookupPorts("PB");
        System.out.println(ports.size() + ": "  + ports);


        System.out.println(GSONUtil.toJSONDefault(iotDeviceInfo, true));
    }


    @Test
    public void atmega328PInfoTest()
    {
        IOTDeviceInfo iotDeviceInfo = new IOTDeviceInfo();
        iotDeviceInfo.setName("ATMEGA328P");
        iotDeviceInfo.setDescription("32 pin Atmel microcontroller");
        iotDeviceInfo.setManufacturer("Microchip").setForm("32-TQFP");
        iotDeviceInfo.setModel("PB");
        // memory
        iotDeviceInfo.getProperties().add(new NVGenericMap("memory").build("program", "32K")
                .build("eeprom", "1K")
                .build("ram", "2K"));
        // CPU
        iotDeviceInfo.getProperties().add(new NVGenericMap("cpu").build("processor", "AVR")
                .build("core", "8-Bits")
                .build("max_speed", "20MHz")
                .build(Range.toRange("[-40, 105]","OpTemp", "C")));


        ProtocolInfo i2c = new ProtocolInfo();
        i2c.setName("I2C");
        i2c.setDescription("Inter-Integrated Circuit Protocol");
        iotDeviceInfo.addProtocol(i2c);






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

        String json = GSONUtil.toJSONDefault(iotDeviceInfo, true);
        System.out.println();
        IOTDeviceInfo rebuild = GSONUtil.fromJSONDefault(json, IOTDeviceInfo.class);
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
