package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;

public interface HTTPAPIBuilder {
    enum Prop
            implements GetName {
        URL("url"),
        AUTHZ("authz"),

        ;
        private final String name;

        Prop(String name) {
            this.name = name;
        }

        /**
         * @return the name of the object
         */
        @Override
        public String getName() {
            return name;
        }

        public static NVGenericMap toProp(String url, HTTPAuthorization authorization) {
            NVGenericMap ret = new NVGenericMap();
            return ret.build(URL, url).build(new NamedValue<>(AUTHZ, authorization));
        }
    }

    <V extends HTTPAPICaller> V createAPI(String name, String description, NVGenericMap props);
}
