package org.zoxweb.server.util;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SharedUtil;

import java.lang.reflect.Modifier;

public enum JMod
        implements GetName
{
    PUBLIC(Modifier.PUBLIC, "public"),
    PROTECTED(Modifier.PROTECTED, "protected"),
    PRIVATE(Modifier.PRIVATE, "private"),
    STATIC(Modifier.STATIC, "static"),
    FINAL(Modifier.FINAL, "final"),
    TRANSIENT(Modifier.TRANSIENT, "transient"),
    VOLATILE(Modifier.VOLATILE, "volatile"),
    SYNCHRONIZED(Modifier.SYNCHRONIZED, "interface"),
    NATIVE(Modifier.NATIVE, "native"),
    INTERFACE(Modifier.INTERFACE, "interface"),
    ABSTRACT(Modifier.ABSTRACT, "abstract"),
    STRICT(Modifier.STRICT, "strictfp"),

    ;

    public final int MOD;
    private final String name;
    JMod(int mod, String name)
    {
        this.MOD = mod;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }


    public static int toModifier(String ...tokens)
    {

        int m = 0x0;
        for (String s : tokens)
        {
            JMod jm = SharedUtil.lookupEnum(s, JMod.values());
            if (jm != null)
            {
                m |= jm.MOD;
            }
        }
        return m;
    }

    public static int toModifier(JMod...modifiers)
    {
        int m = 0x0;
        for (JMod jm : modifiers)
        {
            if (jm != null)
            {
                m |= jm.MOD;
            }
        }
        return m;
    }

    public static String toString(int mod)
    {
        // this is just a helper method
        return Modifier.toString(mod);
    }
}
