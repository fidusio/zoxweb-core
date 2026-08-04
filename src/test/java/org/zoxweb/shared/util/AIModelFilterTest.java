package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.filters.TokenMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates using {@link TokenMatcher} as a filter over AI model identifiers:
 * a small set of glob rules classifies model IDs as Claude, ChatGPT/OpenAI, or neither.
 */
public class AIModelFilterTest {

    // Current Claude model IDs (aliases as served by the Anthropic API)
    private static final String[] CLAUDE_MODELS = {
            "claude-fable-5",
            "claude-mythos-5",
            "claude-opus-5",
            "claude-opus-4-8",
            "claude-opus-4-7",
            "claude-opus-4-6",
            "claude-sonnet-5",
            "claude-sonnet-4-6",
            "claude-haiku-4-5",
            "claude-haiku-4-5-20251001",   // dated full ID
            "claude-opus-4-5-20251101",    // legacy dated full ID
    };

    // ChatGPT / OpenAI model IDs
    private static final String[] CHATGPT_MODELS = {
            "gpt-5",
            "gpt-5-mini",
            "gpt-5-nano",
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4.1",
            "gpt-4.1-mini",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
            "chatgpt-4o-latest",
            "o3",
            "o3-mini",
            "o4-mini",
    };

    // Models that must NOT pass the filter
    private static final String[] OTHER_MODELS = {
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "llama-3.1-70b",
            "mistral-large-2411",
            "deepseek-r1",
            "grok-3",
    };

    private static TokenMatcher claudeMatcher() {
        return new TokenMatcher(true, "claude-*");
    }

    private static TokenMatcher chatGPTMatcher() {
        // gpt-* covers gpt-4o/gpt-5..., chatgpt-* covers chatgpt-4o-latest,
        // o? / o?-* cover the reasoning models (o3, o3-mini, o4-mini)
        return new TokenMatcher(true, "gpt-*", "chatgpt-*", "o?", "o?-*");
    }

    private static String[] allModels() {
        List<String> all = new ArrayList<String>();
        all.addAll(Arrays.asList(CLAUDE_MODELS));
        all.addAll(Arrays.asList(CHATGPT_MODELS));
        all.addAll(Arrays.asList(OTHER_MODELS));
        return all.toArray(new String[0]);
    }

    @Test
    public void filterClaudeModels() {
        TokenMatcher tm = claudeMatcher();
        List<String> hits = new ArrayList<String>();
        for (String model : allModels()) {
            if (tm.matches(model))
                hits.add(model);
        }
        assertEquals(Arrays.asList(CLAUDE_MODELS), hits);
    }

    @Test
    public void filterChatGPTModels() {
        TokenMatcher tm = chatGPTMatcher();
        List<String> hits = new ArrayList<String>();
        for (String model : allModels()) {
            if (tm.matches(model))
                hits.add(model);
        }
        assertEquals(Arrays.asList(CHATGPT_MODELS), hits);
    }

    @Test
    public void combinedFilterExcludesOtherVendors() {
        TokenMatcher tm = new TokenMatcher(true,
                "claude-*", "gpt-*", "chatgpt-*", "o?", "o?-*");
        for (String model : CLAUDE_MODELS)
            assertTrue(tm.matches(model), model);
        for (String model : CHATGPT_MODELS)
            assertTrue(tm.matches(model), model);
        for (String model : OTHER_MODELS)
            assertFalse(tm.matches(model), model);
    }

    @Test
    public void matchFirstIdentifiesVendorRule() {
        TokenMatcher tm = new TokenMatcher(true, "claude-*", "gpt-*");
        assertEquals("claude-*", tm.matchFirst("claude-opus-5"));
        assertEquals("gpt-*", tm.matchFirst("gpt-4o"));
        assertNull(tm.matchFirst("gemini-2.5-pro"));
    }

    @Test
    public void narrowerRules() {
        // Filter down to a model family, not just a vendor
        TokenMatcher tm = new TokenMatcher(true, "claude-opus-*", "gpt-5*");
        assertTrue(tm.matches("claude-opus-5"));
        assertTrue(tm.matches("claude-opus-4-8"));
        assertTrue(tm.matches("gpt-5"));
        assertTrue(tm.matches("gpt-5-mini"));
        assertFalse(tm.matches("claude-sonnet-5"));
        assertFalse(tm.matches("gpt-4o"));
    }

    @Test
    public void matchInLineOneShot() {
        // Ad-hoc filtering without building a matcher instance
        List<String> claude = TokenMatcher.matchInLine("claude-opus-*", allModels());
        System.out.println(claude);
        //assertEquals(Arrays.asList(CLAUDE_MODELS), claude);

        List<String> minis = TokenMatcher.matchInLine("*-mini", allModels());
        assertEquals(Arrays.asList("gpt-5-mini", "gpt-4o-mini", "gpt-4.1-mini", "o3-mini", "o4-mini"), minis);
    }

    @Test
    public void isValidAndValidateAsValueFilter() {
        // TokenMatcher is a ValueFilter — usable anywhere the filter contract is expected
        TokenMatcher tm = claudeMatcher();
        assertTrue(tm.isValid("claude-sonnet-5"));
        assertFalse(tm.isValid("gpt-4o"));
        // case-insensitive matcher canonicalizes to lower case on validate
        assertEquals("claude-opus-5", tm.validate("  CLAUDE-OPUS-5  "));
        assertThrows(IllegalArgumentException.class, () -> tm.validate("gemini-2.5-pro"));
    }
}
