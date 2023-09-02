package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;

public class PatterMatchTest {
    private static String[] toTest =
            {
                "file.Json",
                "/etc/config/dhcp.conf",
                "NotToBeFound",
            };

    @Test
    public void testPattern()
    {
        MatchPatternFilter mpf = MatchPatternFilter.createMatchFilter( "*.conf", "*.json");


        for(String test : toTest)
        {
            System.out.println(test + " " + mpf.match(test));
        }

    }
}
