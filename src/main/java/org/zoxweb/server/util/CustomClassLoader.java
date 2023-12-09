package org.zoxweb.server.util;

import org.zoxweb.server.logging.LogWrapper;

import java.net.URL;
import java.net.URLClassLoader;

public class CustomClassLoader
        extends URLClassLoader
{
    public static final LogWrapper logger = new LogWrapper(CustomClassLoader.class);
    public CustomClassLoader(URL[] urls)
    {
        super(urls);
    }
    public  Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return super.loadClass(name);
    }



}
