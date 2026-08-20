package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.ConfigurableType;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

public class TransportState extends State<NVGenericMap>
{

    public enum Params implements ConfigurableType {
        AUTO_SSL("auto-ssl", true),
        SSL_UPGRADABLE("ssl-upgradable", true),
        SSL_CONFIG_INT("ssl-config-int", false),

        ;
        private final String name;
        private final boolean configurable;
        Params(String name, boolean configurable)
        {
            this.name = name;
            this.configurable = configurable;
        }

        @Override
        public String getName() {return name;}

        @Override
        public boolean isConfigurable() {
            return configurable;
        }
    }

    public enum EventType
        implements GetName
    {
//        IN_RAW_DATA
        ;

        private final String name;
        private final SharedIOUtil.IOType ioType;
        EventType(String name, SharedIOUtil.IOType ioType) {
            this.name = name;
            this.ioType = ioType;
        }

        /**
         * @return the name of the object
         */
        @Override
        public String getName() {
            return name;
        }

        public SharedIOUtil.IOType getIoType() {
            return ioType;
        }
    }


    public TransportState(String name, NVGenericMap map) {
        super(name, map);
    }
}
