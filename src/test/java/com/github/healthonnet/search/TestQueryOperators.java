package com.github.healthonnet.search;

import org.junit.Test;

/**
 * Basic unit tests for HON-Lucene-Synonyms
 *
 * This one tests some of the problems found in issues #28 and #32
 *
 */
public class TestQueryOperators extends HonLuceneSynonymTestCase {

    public TestQueryOperators(){
        defaultRequest = new String[] {
                "qf",       "name",
                "mm",       "100%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "e-commerce"},
                {"id", "2", "name", "electronic commerce"}
        };
        commitDocs();
    }

    @Test
    public void queries() {
        assertQuery("commerce", 2);
        assertQuery("electronic commerce", 2);
        assertQuery("e-commerce", 2);

        // means "shouldn't contain the word commerce"
        assertQuery("e -commerce", 0);
        // it's also no good as a single word
        assertQuery("ecommerce", 0);

        // it doesn't expand synonyms when using a space instead of a hyphen
        // (i.e. it only matches document #1)
        assertQuery("e commerce", 1);
    }
}
