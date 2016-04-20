package com.github.healthonnet.search;

import org.junit.Test;

/**
 * Test that synonyms are not harshly weighted when there are >2
 */
public class TestIssue33 extends HonLuceneSynonymTestCase {

    public TestIssue33() {
        defaultRequest = new String[]{
                "qf",                       "name",
                "mm",                       "100%",
                "defType",                  "synonym_edismax",
                "synonyms",                 "true",
                "fl",                       "*,score",
                "synonyms.originalBoost",   "1.0",
                "synonyms.synonymBoost",    "1.0"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "I have a dog."},
                {"id", "2", "name", "I have a pooch."},
                {"id", "3", "name", "I have a hound."},
                {"id", "4", "name", "I have a jigglypuff."},
                {"id", "5", "name", "I have a purin."},
        };
        commitDocs();
    }

    /**
     * We have the synonyms:
     *
     * dog, pooch, hound, canis familiaris
     * jigglypuff, purin
     *
     * This test confirms that
     * Jigglypuff/purin don't get any special treatment, just because there are 2 and not 4 of them
     * (when synonyms.originalBoost and synonyms.synonymBoost are equal)
     *
     */
    @Test
    public void queries() {
        allScoresEqual("jigglypuff");
        allScoresEqual("purin");

        allScoresEqual("dog", 3);
        allScoresEqual("pooch", 3);
        allScoresEqual("hound", 3);
    }
}
