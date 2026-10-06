package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.RuntimeResultData;
import org.zoxweb.shared.task.CallableConsumerTask;

import java.io.IOException;

public class ProcessTest {

    @Test
    public void inputScriptTest()
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


    @Test
    public void syncTest()
    {
        RuntimeUtil.ProcessExec pe = new RuntimeUtil.ProcessExec("dir");
        RuntimeResultData rr = pe.call();
        System.out.println(rr);


//        pe = new RuntimeUtil.ProcessExec("/bin/enablejdk11.bat");
//        rr = pe.call();
//        System.out.println(rr);
    }

    @Test
    public void asyncTest()
    {
        CallableConsumerTask<RuntimeResultData> cct = new CallableConsumerTask<RuntimeResultData>()
                .setCallable(RuntimeUtil.ProcessExec.create(RuntimeUtil.ShellType.CMD, "dir /s"))
                .setConsumer((rr)->{System.out.println(rr);});

        TaskUtil.defaultTaskProcessor().submit(cct);

        //TaskUtil.sleep(Const.TimeInMillis.SECOND.mult(5));
        TaskUtil.waitIfBusyThenClose(25);
    }
}
