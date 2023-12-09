package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.PasswordDAO;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.Console;

public class PasswordToHash {

    private static void error(String msg)
    {
        System.err.println(msg);
        System.err.println("usage: [aglo] [iteration] [password]");
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
                if (console == null) {
                    System.out.println("No console available");
                    return;
                }

                rawPassword = SharedStringUtil.toString(console.readPassword("Enter your password: "));
            }

            PasswordDAO passwordDAO = HashUtil.toPassword(algo, 0, iteration, rawPassword);
            System.out.println(passwordDAO.toCanonicalID());

            HashUtil.validatePassword(passwordDAO, rawPassword);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error("error");

        }

    }
}
