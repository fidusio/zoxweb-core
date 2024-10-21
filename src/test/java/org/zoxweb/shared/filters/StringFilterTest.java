package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVGenericMap;

public class StringFilterTest
{

    @Test
    public void stringFilterBetweenTest()
    {
        NVGenericMap config = new NVGenericMap();
        config.build("name", "test-between")
                .build("description", "Testing between tokens")
                .build("type","BETWEEN")
                .build("prefix", "```java")
                .build("postfix", "```");


        StringFilter sf = new StringFilter("batata", config);

        String[] toTest = {"Johny", "---{missing end", "missning front---}", "fgfdkgfdkagjfdakjg```java\nvalue between\n```-====df,kdslkfgdslgsdgfsdag"};
        for (String test : toTest)
        {
            System.out.println(test  + " result: " + sf.decode(test));
        }

        System.out.println("\n" + GSONUtil.toJSONDefault(config, true));

    }
}
