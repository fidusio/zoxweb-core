package org.zoxweb.shared.util;


import org.zoxweb.shared.app.AppIDDefault;

public class AppIDURI {
    private AppIDDefault appID;
    private String[] rest;

    private AppIDURI() {

    }

    public AppIDDefault getAppIDDAO() {
        return appID;
    }

    public String[] getRest() {
        return rest;
    }


    public static AppIDURI parse(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        String tokens[] = path.split("/");

        if (tokens.length < 2)
            throw new IllegalArgumentException("path too short");

        AppIDURI ret = new AppIDURI();
        int index = 0;
        ret.appID = new AppIDDefault(tokens[index++], tokens[index++]);

        if (tokens.length > index) {
            ret.rest = new String[tokens.length - index];
            for (int marker = index; index < tokens.length; index++) {
                ret.rest[index - marker] = tokens[index];
            }
        } else {
            ret.rest = Const.EMPTY_STRING_ARRAY;
        }


        return ret;


    }
}
