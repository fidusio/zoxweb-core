package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

public class StringFilter extends
        DataFilter<String, String, Class<String>> {
    enum SFType {
        BETWEEN
    }

    private final NVGenericMap config;

    public StringFilter(String id, NVGenericMap config) {
        super(String.class, id, config.getValue("name"), config.getValue("description"));

        this.config = config;
    }

    /**
     * @param input
     * @return
     */
    @Override
    public String validate(String input) {
        SFType type = SUS.lookupEnum(config.getValue("type"), SFType.values());
        String ret = input;
        switch (type) {
            case BETWEEN:
                ret = SUS.valueAfterLeftToken(ret, config.getValue("prefix"));
                ret = SUS.valueBeforeLeftToken(ret, config.getValue("postfix"));
//                StringToken strToken = SUS.valueBetween(ret, config.getValue("prefix"), config.getValue("postfix"), false);
//                if (strToken != null)
//                    ret = strToken.getToken();
                break;
        }

        return ret;
    }


    public String getExtension() {
        return config.getValue("extension");
    }


}
