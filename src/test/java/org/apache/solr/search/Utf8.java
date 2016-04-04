package org.apache.solr.search;

import org.junit.Test;

/**
 * This one tests some of the problems found in issue #9
 */
public class Utf8 extends HonLuceneSynonymTestCase {

    public Utf8() {
        defaultRequest = new String[]{
                "qf",       "name",
                "mm",       "100%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "blood and bones"},
                {"id", "2", "name", "\u8840\u3068\u9aa8"},
                {"id", "3", "name", "bfo"},
                {"id", "4", "name", "brystforst\u00f8rrende operation"}
        };
        commitDocs();
    }

    @Test
    public void queries() {
        assertQuery("blood and bones", 2);
        assertQuery("\u8840\u3068\u9aa8", 2);

        assertQuery("bfo", 2);
        assertQuery("brystforst\u00f8rrende operation", 2);
    }
}
