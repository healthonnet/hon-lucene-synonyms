package org.apache.solr.search;

import org.junit.Test;

/**
 * Test the "bag" synonym feature
 * by testing that we largely get the same results either way
 * (though phrase matches will be lost)
 */
public class TestBaggedSynonyms extends HonLuceneSynonymTestCase {

    public TestBaggedSynonyms() {
        defaultRequest = new String[]{
                "qf",       "name",
                "mm",       "100%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        commitDocs();
    }

    @Test
    public void withoutBaggedSynonyms() {
        assertQuery("dog", 4);
        assertQuery("pooch", 4);
        assertQuery("hound", 4);
        assertQuery("canis familiaris", 4);
        assertQuery("cat", 0);
        assertQuery("canis", 2);
    }

    @Test
    public void withBaggedSynonyms() {
        assertQuery("dog", 4, "synonyms.bag", "true");
        assertQuery("pooch", 4, "synonyms.bag", "true");
        assertQuery("hound", 4, "synonyms.bag", "true");
        assertQuery("canis familiaris", 4, "synonyms.bag", "true");
        assertQuery("cat", 0, "synonyms.bag", "true");
        assertQuery("canis", 2, "synonyms.bag", "true");
    }
}
