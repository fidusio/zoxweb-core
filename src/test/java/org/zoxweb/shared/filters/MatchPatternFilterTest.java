/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatchPatternFilterTest {

    // ==================== Asterisk Wildcard Tests ====================

    @Test
    public void testAsteriskSuffix() {
        // Pattern: *.java should match any file ending with .java
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        assertTrue(filter.match("Test.java"));
        assertTrue(filter.match("MyClass.java"));
        assertTrue(filter.match("a.java"));
        assertFalse(filter.match("Test.txt"));
        assertFalse(filter.match("java"));
        assertFalse(filter.match("Test.java.bak"));
    }

    @Test
    public void testAsteriskPrefix() {
        // Pattern: file.* should match file with any extension
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("file.*");

        assertTrue(filter.match("file.txt"));
        assertTrue(filter.match("file.java"));
        assertTrue(filter.match("file.pdf"));
        assertFalse(filter.match("myfile.txt"));
        assertFalse(filter.match("file"));
    }

    @Test
    public void testAsteriskMiddle() {
        // Pattern: test*.txt should match test followed by anything ending in .txt
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("test*.txt");

        assertTrue(filter.match("test.txt"));
        assertTrue(filter.match("test123.txt"));
        assertTrue(filter.match("testfile.txt"));
        assertFalse(filter.match("mytest.txt"));
        assertFalse(filter.match("test.pdf"));
    }

    @Test
    public void testAsteriskBothEnds() {
        // Pattern: *.java.* should match anything with .java. in the middle
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java.*");

        assertTrue(filter.match("Test.java.bak"));
        assertTrue(filter.match("MyClass.java.aes"));
        assertFalse(filter.match("Test.java"));
        assertFalse(filter.match("java.txt"));
    }

    @Test
    public void testSingleAsterisk() {
        // Pattern: * should match anything
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*");

        assertTrue(filter.match("anything"));
        assertTrue(filter.match("test.java"));
        assertTrue(filter.match("a"));
    }

    // ==================== Question Mark Wildcard Tests ====================

    @Test
    public void testQuestionMarkSingleCharacter() {
        // Pattern: ?.txt should match single character followed by .txt
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("?.txt");

        assertTrue(filter.match("a.txt"));
        assertTrue(filter.match("1.txt"));
        assertFalse(filter.match("ab.txt"));
        assertFalse(filter.match(".txt"));
    }

    @Test
    public void testQuestionMarkMultiple() {
        // Pattern: D?c?m?nts.pdf should match with any single characters in place of ?
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("D?c?m?nts.pdf");

        assertTrue(filter.match("D1c2m3nts.pdf"));
        assertTrue(filter.match("Documents.pdf"));
        assertTrue(filter.match("DacXmYnts.pdf"));
        assertFalse(filter.match("Dcmnts.pdf"));  // missing characters
        assertFalse(filter.match("Documents.txt"));
    }

    @Test
    public void testQuestionMarkMiddle() {
        // Pattern: test?.java should match test + single char + .java
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("test?.java");

        assertTrue(filter.match("test1.java"));
        assertTrue(filter.match("testA.java"));
        assertFalse(filter.match("test.java"));
        assertFalse(filter.match("test12.java"));
    }

    // ==================== Combined Wildcard Tests ====================

    @Test
    public void testCombinedAsteriskAndQuestionMark() {
        // Pattern: *?.java should match anything ending with single char before .java
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*?.java");

        assertTrue(filter.match("Test1.java"));
        assertTrue(filter.match("a.java"));
        assertTrue(filter.match("MyClassX.java"));
    }

    // ==================== Case Sensitivity Tests ====================

    @Test
    public void testCaseSensitiveByDefault() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.Java");

        assertTrue(filter.isCaseSensitive());
        assertTrue(filter.match("Test.Java"));
        assertFalse(filter.match("Test.java"));
        assertFalse(filter.match("Test.JAVA"));
    }

    @Test
    public void testCaseInsensitiveFlag() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-i", "*.java");

        assertFalse(filter.isCaseSensitive());
        assertTrue(filter.match("Test.java"));
        assertTrue(filter.match("Test.JAVA"));
        assertTrue(filter.match("Test.Java"));
        assertTrue(filter.match("TEST.JAVA"));
    }

    @Test
    public void testCaseInsensitiveFlagWithString() {
        // Test with single string containing space-separated values
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-i *.java");

        assertFalse(filter.isCaseSensitive());
        assertTrue(filter.match("Test.java"));
        assertTrue(filter.match("Test.JAVA"));
    }

    // ==================== Recursive Flag Tests ====================

    @Test
    public void testRecursiveFlagFalseByDefault() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        assertFalse(filter.isRecursive());
    }

    @Test
    public void testRecursiveFlagEnabled() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-r", "*.java");

        assertTrue(filter.isRecursive());
    }

    @Test
    public void testBothFlagsEnabled() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-r", "-i", "*.java");

        assertTrue(filter.isRecursive());
        assertFalse(filter.isCaseSensitive());
    }

    // ==================== Multiple Patterns Tests ====================

    @Test
    public void testMultiplePatterns() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java", "*.txt", "*.pdf");

        assertTrue(filter.match("Test.java"));
        assertTrue(filter.match("readme.txt"));
        assertTrue(filter.match("document.pdf"));
        assertFalse(filter.match("image.png"));
        assertFalse(filter.match("script.sh"));
    }

    @Test
    public void testMultiplePatternsWithFlags() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-i", "-r", "*.java", "*.txt");

        assertTrue(filter.isRecursive());
        assertFalse(filter.isCaseSensitive());
        assertTrue(filter.match("Test.JAVA"));
        assertTrue(filter.match("README.TXT"));
    }

    @Test
    public void testGetMatchPatterns() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java", "*.txt");

        String[] patterns = filter.getMatchPatterns();
        // Should include the flags as patterns too (based on current implementation)
        assertTrue(patterns.length >= 2);
    }

    // ==================== Validate and IsValid Tests ====================

    @Test
    public void testIsValidTrue() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        assertTrue(filter.isValid("Test.java"));
    }

    @Test
    public void testIsValidFalse() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        assertFalse(filter.isValid("Test.txt"));
    }

    @Test
    public void testValidateSuccess() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        String result = filter.validate("Test.java");
        assertEquals("Test.java", result);
    }

    @Test
    public void testValidateFailure() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        assertThrows(IllegalArgumentException.class, () -> filter.validate("Test.txt"));
    }

    // ==================== Edge Cases Tests ====================

    @Test
    public void testConsecutiveAsterisksThrowsException() {
        // Consecutive asterisks should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
            () -> MatchPatternFilter.createMatchFilter("**.java"));
    }

    @Test
    public void testPathWithWildcard() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*store*.java");

        assertTrue(filter.match("fidus-store-test.java"));
        assertTrue(filter.match("store.java"));
        assertTrue(filter.match("mystoreclass.java"));
    }

    @Test
    public void testExactMatch() {
        // Pattern without wildcards should match exactly
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("exact.txt");

        assertTrue(filter.match("exact.txt"));
        assertFalse(filter.match("notexact.txt"));
        assertFalse(filter.match("exact.txt.bak"));
    }

    @Test
    public void testSpecialCharactersInFilename() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.txt");

        assertTrue(filter.match("my-file.txt"));
        assertTrue(filter.match("my_file.txt"));
        assertTrue(filter.match("my.file.txt"));
    }

    @Test
    public void testNumericFilenames() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.log");

        assertTrue(filter.match("123.log"));
        assertTrue(filter.match("2024-01-01.log"));
    }

    // ==================== MatchLiteral Enum Tests ====================

    @Test
    public void testMatchLiteralValues() {
        assertEquals("*", MatchPatternFilter.MatchLiteral.ASTERISK.getValue());
        assertEquals("?", MatchPatternFilter.MatchLiteral.QUESTION_MARK.getValue());
        assertEquals("-i", MatchPatternFilter.MatchLiteral.CASE_INSENSITIVE.getValue());
        assertEquals("-r", MatchPatternFilter.MatchLiteral.RECURSIVE.getValue());
    }

    // ==================== toCanonicalID Test ====================

    @Test
    public void testToCanonicalID() {
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("*.java");

        // Current implementation returns null
        assertNull(filter.toCanonicalID());
    }

    // ==================== Single String Pattern Input Test ====================

    @Test
    public void testSingleStringWithMultiplePatterns() {
        // When a single string is passed, it should be split by spaces
        MatchPatternFilter filter = MatchPatternFilter.createMatchFilter("-i -r *.java *.txt  *.well-known*");

        assertTrue(filter.isRecursive());
        assertFalse(filter.isCaseSensitive());
        assertTrue(filter.match("Test.JAVA"));
        assertTrue(filter.match("readme.TXT"));
        assertTrue(filter.match("/.well-known"));
        assertTrue(filter.match("/.well-known/baata"));
        assertFalse(filter.isValid("/.Not-known/baata"));

    }


}
