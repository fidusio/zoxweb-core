package org.zoxweb.shared.db;

import org.junit.jupiter.api.Test;

public class QueryStringTest {

    @Test
    public void positiveTest()
    {
        String[] toTest =
                {
                  "name=mario",
                  "temp>16c",
                  "temp<=0",
                  "temp!=0",
                  "tem{}gfrt"

                };

        for(String test : toTest)
        {
            QueryMatchString qms = QueryMatchString.toQueryMatch(test);
            System.out.println(test + " " + qms);
        }
    }
}
