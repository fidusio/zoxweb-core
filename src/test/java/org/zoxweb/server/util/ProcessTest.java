package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ProcessTest {

    @Test
    public void dirTest()
    {
        try {
            RuntimeUtil.PIOs pios = RuntimeUtil.runCommand("/bin/echoname.bat");
            String line;
            while((line = pios.stdIn.readLine()) != null)
            {
                System.out.println(line);
                if(line.contains("name")) {
                    pios.stdOut.write("marwan\n");
                    pios.stdOut.flush();
                }
            }
            while((line = pios.stdErr.readLine()) != null)
            {
                System.out.println(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
