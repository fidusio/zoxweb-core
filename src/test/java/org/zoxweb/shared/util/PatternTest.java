package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.filters.FilterType;

public class PatternTest {

    @Test
    public void patterTest()
    {
        String pattern = "\\{*\\}";
        String[] toBeTested = {"path", "/path/{id}", "/path/{p1{khara}}/{p2}"};

        for (String str: toBeTested)
        {
            System.out.println(str + ":" + str.matches(pattern));
            System.out.println(SUS.parseGroup(str, "{", "}", false));
        }
    }

    @Test
    public void emailPattern()
    {
        String emailPattern = "^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.(([0-9]{1,3})|([a-zA-Z]{2,3})|(aero|coop|info|museum|name))$";
        String[] emails = {
          "John@xlogistx.io",
          "john@xlogistx.com",
          "john.doe@email.com",
          "john@email@zoxweb.mobi",
          "#batat@email.com",
          "user.emai.com"
        };

        for(String email : emails)
        {
            System.out.println(email + ":" + FilterType.EMAIL.isValid(email) + " " + email.matches(emailPattern));
        }
    }

}
