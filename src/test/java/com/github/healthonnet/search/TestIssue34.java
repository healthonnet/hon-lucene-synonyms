package com.github.healthonnet.search;

import org.junit.Test;

/**
 * Test that synonyms.disablePhraseQueries is working properly
 */
public class TestIssue34 extends HonLuceneSynonymTestCase {

    public TestIssue34() {
        defaultRequest = new String[]{
                "qf",                       "name",
                "mm",                       "1%",
                "defType",                  "synonym_edismax",
                "synonyms",                 "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "I have a dog."},
                {"id", "2", "name", "I have a pooch."},
                {"id", "3", "name", "I have a hound."},
                {"id", "4", "name", "I have a canis."}
        };
        commitDocs();
    }

    /**
     * We have the synonyms:
     *
     * dog, pooch, hound, canis familiaris
     */
    @Test
    public void queries() {
        assertQuery("\"dog\"", 3);
        assertQuery("\"pooch\"", 3);
        assertQuery("\"hound\"", 3);
        assertQuery("\"canis familiaris\"", 3);

        assertQuery("\"dog\"", 1, "synonyms.disablePhraseQueries", "true");
        assertQuery("\"pooch\"", 1, "synonyms.disablePhraseQueries", "true");
        assertQuery("\"hound\"", 1, "synonyms.disablePhraseQueries", "true");
        assertQuery("\"canis familiaris\"", 0, "synonyms.disablePhraseQueries", "true");

        assertQuery("dog", 3, "synonyms.constructPhrases", "true");
        assertQuery("pooch", 3, "synonyms.constructPhrases", "true");
        assertQuery("hound", 3, "synonyms.constructPhrases", "true");
        assertQuery("canis familiaris", 4, "synonyms.constructPhrases", "true");
    }
}
