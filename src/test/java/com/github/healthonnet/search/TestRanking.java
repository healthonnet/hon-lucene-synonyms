package com.github.healthonnet.search;

import org.junit.Test;

/**
 * This test confirms that the main query is weighted higher than the synonym query,
 * when synonyms.originalBoost is higher than synonyms.synonymBoost.
 */
public class TestRanking extends HonLuceneSynonymTestCase {

    public TestRanking(){
        defaultRequest = new String[] {
                "qf",                       "name",
                "mm",                       "100%",
                "defType",                  "synonym_edismax",
                "synonyms",                 "true",
                "synonyms.originalBoost",   "2.0",
                "synonyms.synonymBoost",    "1.0"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "I have a dog."},
                {"id", "2", "name", "I have a pooch."},
                {"id", "3", "name", "I have a canis familiaris."},
                {"id", "4", "name", "I have a jigglypuff."},
                {"id", "5", "name", "I have a purin."}     
        };
        commitDocs();
    }

    private void assertQueryExpectedDocId(String query, int expectedNumDocs, String expectedDocId) {
        String[] expectedDocIdTests = new String[] {"//result/doc[1]/str[@name='id'][.='" + expectedDocId + "']"};
        assertQuery(expectedDocIdTests, query, expectedNumDocs);
    }

    @Test
    public void queries() {
        // the document containing the actual term should be first, but all related docs should match
        assertQueryExpectedDocId("dog", 3, "1");
        assertQueryExpectedDocId("pooch", 3, "2");
        assertQueryExpectedDocId("canis familiaris", 3, "3");
        assertQueryExpectedDocId("purin", 2, "5");
        assertQueryExpectedDocId("jigglypuff", 2, "4");
    }
}
