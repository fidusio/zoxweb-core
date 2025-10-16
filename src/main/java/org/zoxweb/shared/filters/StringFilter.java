package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

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
    public String decode(String input) {
        SFType type = SharedUtil.lookupEnum(config.getValue("type"), SFType.values());
        String ret = input;
        switch (type) {
            case BETWEEN:
                ret = SharedStringUtil.valueAfterLeftToken(ret, config.getValue("prefix"));
                ret = SharedStringUtil.valueBeforeLeftToken(ret, config.getValue("postfix"));
//                StringToken strToken = SharedStringUtil.valueBetween(ret, config.getValue("prefix"), config.getValue("postfix"), false);
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
