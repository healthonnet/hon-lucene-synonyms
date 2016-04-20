package com.github.healthonnet.search;

import org.junit.Test;

public class TestConstructedPhrases extends HonLuceneSynonymTestCase {

    public TestConstructedPhrases() {
        defaultRequest = new String[]{
                "qf",       "name",
                "mm",       "100%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "man's best friend"},
                {"id", "2", "name", "the best friend of man's"},
                {"id", "3", "name", "canis familiaris"},
                {"id", "4", "name", "species familiaris, of the genus canis"}
        };
        commitDocs();
    }

    /**
     * adding 'synonyms.constructPhrases' should automatically build phrase queries
     * out of synonyms, since they're known to be meaningful combinations of words (Issue #5)
     */
    @Test
    public void constructPhrases() {
        // we're being lax, all four docs can match
        assertQuery("dog", 4);
        assertQuery("canis familiaris", 4);
        assertQuery("pooch", 4);
        assertQuery("hound", 4);
        assertQuery("man's best friend", 4);

        // whoops, now suddenly "canis familiaris" and "man's best friend" are understood to be phrases!
        assertQuery("dog", 2, "synonyms.constructPhrases", "true");
        assertQuery("pooch", 2, "synonyms.constructPhrases", "true");
        assertQuery("hound", 2, "synonyms.constructPhrases", "true");

        // but only when they're the synonyms - the original query is still unmodified
        assertQuery("man's best friend", 3, "synonyms.constructPhrases", "true");
        assertQuery("canis familiaris", 3, "synonyms.constructPhrases", "true");

        // (sanity test: the default is false, right?)
        assertQuery("dog", 4, "synonyms.constructPhrases", "false");
        assertQuery("pooch", 4, "synonyms.constructPhrases", "false");
        assertQuery("hound", 4, "synonyms.constructPhrases", "false");
        assertQuery("canis familiaris", 4, "synonyms.constructPhrases", "false");
        assertQuery("man's best friend", 4, "synonyms.constructPhrases", "false");

    }

}
