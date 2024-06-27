package org.zoxweb.shared.iot;


import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.util.*;

public class PortInfo
extends IOTBase
{
    public enum Param
            implements GetNVConfig
    {
        PORT(NVConfigManager.createNVConfig("port", "Port/Pin number on the device", "Port/PIN", true, true, int.class)),
        TYPE(NVConfigManager.createNVConfig("type", "Port/Pin type", "Type", false, true, String.class)),
        TAG(NVConfigManager.createNVConfig("port_tag", "Port tag", "Tag", false, true, String.class)),
        SPEEDS(NVConfigManager.createNVConfig("speeds", "Port speeds", "Speeds", false, true, NVStringList.class)),
        FUNCTIONS(NVConfigManager.createNVConfig("functions", "Port functions", "Functions", false, true, NVStringList.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_PORT_INFO = new NVConfigEntityLocal("port_info",
            null,
            "PortInfo",
            true,
            false,
            false,
            false,
            PortInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            IOTBase.NVC_IOT_BASE);


    private static final CanonicalIDSetter CIDS = new CanonicalIDSetter('-', Param.TAG, Param.PORT, DataConst.DataParam.NAME, Param.TYPE);

    public PortInfo()
    {
        super(NVC_PORT_INFO, CIDS);
    }

    public PortInfo(String name, String description)
    {
        this();
        setName(name);
        setDescription(description);
    }


    public int getPort()
    {
        return lookupValue(Param.PORT);
    }

    public PortInfo setPort(int port)
    {
        setValue(Param.PORT, port);
        return this;
    }


    public String getType()
    {
        return lookupValue(Param.TYPE);
    }

    public PortInfo setType(String type)
    {
        setValue(Param.TYPE, type);
        return this;
    }


    public String[] getSpeeds()
    {
        return ((NVStringList)lookup(Param.SPEEDS)).getValues();
    }

    public synchronized PortInfo setSpeeds(String ...speeds)
    {
        NVStringList speedsAV = lookup(Param.SPEEDS);
        for(String speed: speeds)
            speedsAV.add(speed);
        return this;
    }

    public PortInfo addSpeed(String speed)
    {
        ((NVStringList)lookup(Param.SPEEDS)).add(speed);
        return this;
    }



    public String[] getFunctions()
    {
        return ((NVStringList)lookup(Param.FUNCTIONS)).getValues();
    }

    public synchronized PortInfo setFunctions(String ...functions)
    {
        NVStringList functionsNVSL = lookup(Param.FUNCTIONS);
        for(String function: functions)
            functionsNVSL.add(function);

        return this;
    }

    public PortInfo addFunction(String function)
    {
        ((NVStringList)lookup(Param.FUNCTIONS)).add(function);
        return this;
    }

    public String getTag()
    {
        return lookupValue(Param.TAG);
    }

    public PortInfo setTag(String tag)
    {
        setValue(Param.TAG, tag);
        return this;
    }


}