package org.zoxweb.shared.data;

import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class StatInfo
    extends PropertyImpl
{

    public enum Param
            implements GetNVConfig
    {
        CALL_COUNTER(NVConfigManager.createNVConfig("call_counter", "Call counter", "CallCounter", false, true, long.class)),
        VERSION(NVConfigManager.createNVConfig("version", "App version", "Version", false, true, String.class)),
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


    public static final NVConfigEntity NVC_STAT_INFO = new NVConfigEntityPortable("stat_info",
            null,
            "StatInfo",
            true,
            false,
            false,
            false,
            StatInfo.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyImpl.NVC_PROPERTY_IMPL);

    public StatInfo()
    {
        super(NVC_STAT_INFO);
    }

    protected StatInfo(NVConfigEntity nvce)
    {
        super(nvce);
    }

    public long getCallCounter()
    {
        return lookupValue(Param.CALL_COUNTER);
    }

    public void setCallCounter(long counter)
    {
        setValue(Param.CALL_COUNTER, counter);
    }

    public long incCallCounter()
    {
        long counter = getCallCounter();
        counter++;
        setCallCounter(counter);
        return counter;
    }

    public String getVersion()
    {
        return lookupValue(Param.VERSION);
    }

    public void setVersion(String version)
    {
        setValue(Param.VERSION, version);
    }

    public String uptime()
    {
        return Const.TimeInMillis.toString(uptimeInMillis());
    }

    public long uptimeInMillis()
    {
        return System.currentTimeMillis() - getCreationTime();
    }
}
