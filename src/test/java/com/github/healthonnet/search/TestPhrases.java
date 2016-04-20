package com.github.healthonnet.search;

import org.junit.Test;

public class TestPhrases extends HonLuceneSynonymTestCase {

    public TestPhrases() {
        defaultRequest = new String[] {
                "qf",       "name",
                "synonyms", "true",
                "defType",  "synonym_edismax"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "dog"},
                {"id", "2", "name", "pooch"},
                {"id", "3", "name", "hound"},
                {"id", "4", "name", "canis familiaris"},
                {"id", "5", "name", "canis"},
                {"id", "6", "name", "familiaris"},
                {"id", "7", "name", "man's best friend"},
                {"id", "8", "name", "man's"},
                {"id", "9", "name", "best"},
                {"id", "10", "name", "friend"}
        };
        commitDocs();
    }

    /**
     * min should match ('mm') should work the same regardless of how many tokens
     * there are in the input synonym
     */
    @Test
    public void minShouldMatch() {
        assertQuery("dog", 5, "mm", "100%");
        assertQuery("canis familiaris", 5, "mm", "100%");
        assertQuery("pooch", 5, "mm", "100%");
        assertQuery("hound", 5, "mm", "100%");
        
        // suddenly one token in "canis familiaris" matches too
        assertQuery("dog", 7, "mm", "75%");
        assertQuery("canis familiaris", 7, "mm", "75%");
        assertQuery("pooch", 7, "mm", "75%");
        assertQuery("hound", 7, "mm", "75%");
        
        // suddenly "one token in "man's best friend" matches too
        assertQuery("dog", 10, "mm", "50%");
        assertQuery("canis familiaris", 10, "mm", "50%");
        assertQuery("pooch", 10, "mm", "50%");
        assertQuery("hound", 10, "mm", "50%");
    }
}
