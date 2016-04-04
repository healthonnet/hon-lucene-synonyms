package org.apache.solr.search;

import org.junit.Test;

public class TestSynonymsBasic extends HonLuceneSynonymTestCase {

    public TestSynonymsBasic(){
        defaultRequest = new String[] {
                "qf",       "name",
                "mm",       "100%",
                "defType",  "edismax"
        };
        commitDocs();
    }

    @Test
    public void withoutSynonyms() {
        assertQuery("dog", 1);
        assertQuery("pooch", 1);
        assertQuery("hound", 1);
        assertQuery("canis familiaris", 1);
        assertQuery("cat", 0);
        assertQuery("canis", 2);
    }

    @Test
    public void withSynonyms() {
        assertQuery("dog", 4, "synonyms", "true", "defType", "synonym_edismax");
        assertQuery("pooch", 4, "synonyms", "true", "defType", "synonym_edismax");
        assertQuery("hound", 4, "synonyms", "true", "defType", "synonym_edismax");
        assertQuery("canis familiaris", 4, "synonyms", "true", "defType", "synonym_edismax");
        assertQuery("cat", 0, "synonyms", "true", "defType", "synonym_edismax");
        assertQuery("canis", 2, "synonyms", "true", "defType", "synonym_edismax");
    }
}
