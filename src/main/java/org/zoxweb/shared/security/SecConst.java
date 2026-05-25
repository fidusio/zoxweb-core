package org.zoxweb.shared.security;

import org.zoxweb.shared.util.GetName;

public final class SecConst {
    private  SecConst() {
    }


    public enum AuthenticationType
            implements GetName {
        ALL("All"),
        API_KEY("ApiKey"), // custom authentication like opaque key SSWS etc
        BASIC("Basic"),
        BEARER("Bearer"),
        DIGEST("Digest"),
        DOMAIN("Domain"),
        JWT("JWT"),
        LDAP("LDAP"),
        HOBA("HOBA"),
        NONE("None"),
        OAUTH("OAuth"),
        //SSWS("SSWS"),
        ;

        private final String name;

        AuthenticationType(String val) {
            name = val;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return name;
        }
    }
}
