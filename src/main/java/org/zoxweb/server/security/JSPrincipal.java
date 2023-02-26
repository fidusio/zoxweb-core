package org.zoxweb.server.security;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SharedUtil;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

public class JSPrincipal implements Principal, GetName
{
    private final String name;
    public JSPrincipal(String name)
    {
        SharedUtil.checkIfNulls("Principal name can't be null", name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString()
    {
        return getName();
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj != null && obj instanceof Principal)
        {
            return name.equals(((Principal) obj).getName());
        }

        return false;
    }

    public Set<Principal> asSet(Principal ...principals)
    {
        Set<Principal> ret = new LinkedHashSet<Principal>();
        ret.add(this);
        if(principals != null)
        {
            for(Principal p: principals)
            {
                if( p != null)
                ret.add(p);
            }
        }

        return ret;
    }
}
