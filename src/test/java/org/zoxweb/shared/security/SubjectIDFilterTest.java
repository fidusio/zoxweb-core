package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zoxweb.shared.security.SecConst.SubjectIDFilter;

import static org.junit.jupiter.api.Assertions.*;

public class SubjectIDFilterTest {

    private final SubjectIDFilter filter = SubjectIDFilter.SINGLETON;

    @Test
    public void validLowercaseIsReturnedAsIs() {
        assertEquals("mario@zoxweb.com", filter.validate("mario@zoxweb.com"));
        assertEquals("username", filter.validate("username"));
        assertTrue(filter.isValid("username"));
    }

    @Test
    public void mixedCaseIsLowercased() {
        assertEquals("mario@zoxweb.com", filter.validate("MaRiO@ZoxWeb.COM"));
        assertEquals("johndoe1", filter.validate("JOHNDOE1"));
        assertTrue(filter.isValid("JOHNDOE1"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmed() {
        assertEquals("mario@zoxweb.com", filter.validate("  Mario@zoxweb.com \t\n"));
        assertTrue(filter.isValid("  Mario@zoxweb.com \t\n"));
    }

    @Test
    public void minimumLengthIsEnforcedAfterNormalization() {
        assertEquals(8, SubjectIDFilter.MIN_LENGTH);
        assertEquals("abcdefgh", filter.validate("ABCDEFGH"));
        assertEquals("abcdefgh", filter.validate("  abcdefgh  "));
        assertThrows(IllegalArgumentException.class, () -> filter.validate("abcdefg"));
        assertThrows(IllegalArgumentException.class, () -> filter.validate("   abcdefg   "));
        assertThrows(IllegalArgumentException.class, () -> filter.validate("a"));
        assertFalse(filter.isValid("abcdefg"));
        assertFalse(filter.isValid("test"));
    }

    @Test
    public void nullIsRejected() {
        assertThrows(NullPointerException.class, () -> filter.validate(null));
        assertFalse(filter.isValid(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n", "   \r\n  "})
    public void emptyOrBlankIsTreatedAsNull(String in) {
        // SUS.trimOrNull collapses blank input to null, so it is rejected the same way null is
        assertThrows(NullPointerException.class, () -> filter.validate(in));
        assertFalse(filter.isValid(in));
    }

    @Test
    public void validEmailBypassesMinimumLength() {
        // the email filter accepts first, so a short but well formed address is not subject to MIN_LENGTH
        assertEquals("a@b.com", filter.validate("A@B.com"));
        assertTrue(filter.isValid("a@b.com"));
    }

    @Test
    public void emailWithInvisibleCharacterFallsThroughAndIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> filter.validate("ma​rio@zoxweb.com"));
        assertThrows(IllegalArgumentException.class, () -> filter.validate("mario@zox­web.com"));
        assertFalse(filter.isValid("mario zebib@zoxweb.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mario zebib@zoxweb.com",   // space
            "mario\tzebib@zoxweb.com",  // tab
            "mario\nzebib@zoxweb.com",  // newline
            "mario\u0000zebib",         // NUL
            "mario\u0007zebib",         // BEL
            "mario\u007Fzebib",         // DEL
            "mario\u0085zebib",         // NEL (C1 control)
            "mario\u00A0zebib",         // no-break space
            "mario\u00ADzebib",         // soft hyphen
            "mario\u2000zebib",         // en quad
            "mario\u200Bzebib",         // zero width space
            "mario\u200Czebib",         // zero width non-joiner
            "mario\u200Dzebib",         // zero width joiner
            "mario\u200Ezebib",         // left-to-right mark
            "mario\u2028zebib",         // line separator
            "mario\u202Ezebib",         // right-to-left override
            "mario\u202Fzebib",         // narrow no-break space
            "mario\u2060zebib",         // word joiner
            "mario\u2066zebib",         // left-to-right isolate
            "mario\u3000zebib",         // ideographic space
            "mario\uFEFFzebib",         // byte order mark
            "\u200Bmario@zoxweb.com",   // leading zero width space survives trim
            "mario@zoxweb.com\u200B",   // trailing zero width space survives trim
            "\u00A0mario@zoxweb.com",   // leading NBSP survives trim
    })
    public void invisibleCharactersAreRejected(String in) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> filter.validate(in));
        assertTrue(e.getMessage().contains("invisible"), e.getMessage());
        assertFalse(filter.isValid(in));
    }

    @Test
    public void visibleNonAsciiIsAccepted() {
        assertEquals("josé@zoxweb.com", filter.validate("José@zoxweb.com"));
        assertEquals("münchen-user", filter.validate("München-User"));
        assertEquals("用户名称测试八号", filter.validate("用户名称测试八号"));
    }

    @Test
    public void isInvisibleClassifiesCharacters() {
        for (char c = 0; c <= 0x20; c++) assertTrue(SubjectIDFilter.isInvisible(c), "U+" + (int) c);
        for (char c = 0x7F; c <= 0xA0; c++) assertTrue(SubjectIDFilter.isInvisible(c), "U+" + (int) c);
        assertTrue(SubjectIDFilter.isInvisible('\u200B'));
        assertTrue(SubjectIDFilter.isInvisible('\uFEFF'));

        for (char c : "aZ09@._-+!#$%&'*/=?^`{|}~éñü".toCharArray()) {
            assertFalse(SubjectIDFilter.isInvisible(c), "U+" + (int) c);
        }
    }

    @Test
    public void canonicalID() {
        assertEquals("SUBJECT_ID_FILTER", filter.toCanonicalID());
    }
}
