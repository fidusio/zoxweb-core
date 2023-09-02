package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;


public class TokenFilter
    implements ValueFilter<String, String>

{
    public static final TokenFilter UPPER_COLON = new TokenFilter("UpperColon", Const.StringType.UPPER, ":");
    public static final TokenFilter LOWER_COLON = new TokenFilter("LowerColon", Const.StringType.LOWER, ":");
    public static final TokenFilter COLON = new TokenFilter("Colon", Const.StringType.AS_IS, ":");


    private final String canID;
    private final String sep;
    private final Const.StringType st;
    
    public TokenFilter(String canID, Const.StringType st, String sep){
        this.canID = canID;
        this.sep = SharedStringUtil.trimOrNull(sep);
        this.st = st;
    }


    @Override
    public String validate(String value) throws NullPointerException, IllegalArgumentException {
        value = SharedStringUtil.trimOrNull(value);
        if(value == null)
            throw new NullPointerException("value can be null or empty");

        if(sep != null)
        {
            int index = value.indexOf(sep);
            if(index > 0)
                value = value.substring(0, index);
            if(index == 0)
                throw new IllegalArgumentException("value can't start with separator " + sep);
        }

        switch(st)
        {

            case UPPER:
                return value.toUpperCase();
            case LOWER:
                return value.toLowerCase();
        }


        return value;
    }

    @Override
    public boolean isValid(String value)
    {
        value = SharedStringUtil.trimOrNull(value);
        if(value != null && sep != null)
        {

            return (value.indexOf(sep) != 0);
        }
        return false;
    }

    @Override
    public String toCanonicalID() {
        return canID;
    }
}
