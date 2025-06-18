package org.zoxweb.server.security;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

public class JSPrincipal implements Principal, GetName {
    private final String name;

    public JSPrincipal(String name) {
        SUS.checkIfNulls("Principal name can't be null", name);
        this.name = SharedStringUtil.toLowerCase(name);
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Principal) {
            return name.equals(((Principal) obj).getName());
        }

        return false;
    }

    public Set<Principal> asSet(Principal... principals) {
        Set<Principal> ret = new LinkedHashSet<Principal>();
        ret.add(this);
        if (principals != null) {
            for (Principal p : principals) {
                if (p != null)
                    ret.add(p);
            }
        }

        return ret;
    }
}
