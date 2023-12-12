package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.PasswordDAO;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.Console;
import java.util.Arrays;

public class PasswordToHash {

    private static void error(String msg, int errorCode)
    {
        System.err.println("**Error**: " + msg);
        System.err.println("usage: [aglo] [iteration] [password]");
        System.exit(errorCode);
    }

    public static void main(String[] args)
    {
        try
        {
            int index = 0;
            String algo = args[index++];
            int iteration = Integer.parseInt(args[index++]);
            String rawPassword = index < args.length ? args[index] : null;
            if(rawPassword == null)
            {
                Console console = System.console();
                if (console == null)
                    error("No java console available", -1);

                char[] passwd1 = console.readPassword("Enter your password: ");
                char[] passwd2 = console.readPassword("Re enter your password: ");
                if (!Arrays.equals(passwd1, passwd2))
                    error("Password miss match", -1);

                rawPassword = SharedStringUtil.toString(passwd1);

            }

            PasswordDAO passwordDAO = HashUtil.toPassword(algo, 0, iteration, rawPassword);
            System.out.println(passwordDAO.toCanonicalID());

            HashUtil.validatePassword(passwordDAO, rawPassword);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error("error", -1 );

        }

    }
}
