package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExTest {
    @Test
    public void regex()
    {
        String[] texts = {"{tag>content</tag>", "<TAG>CONTENT</TAG}", "<[Tag>Content</Tag>", "fail"};
        String tag = "tag";

        // Construct the regex pattern in a case-insensitive manner
        String patternString = "(?i).*" + tag + "(.*?)";

        for (String text : texts) {
            boolean matches = text.matches(patternString);
            System.out.println("Text: " + text + " Matches: " + matches);
        }


        for (String text : texts) {
            boolean matches = text.matches(Const.RegEx.CONTAINS_NO_CASE.toRegEx("TaG",true));
            System.out.println("Text: " + text + " Matches: " + matches);
        }


        String text = "Visit the websites example.com and example.org for more information.";
        String[] literalStrings = {"example.com", "example.org"};

        // Construct the regex pattern for multiple literal strings
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < literalStrings.length; i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(literalStrings[i]));
        }
        patternString = patternBuilder.toString();

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);

        boolean matches = matcher.find(); // Use find() to search within the string

        System.out.println("Contains one of the literal strings: " + matches + " pattern " + patternString);
    }
}
