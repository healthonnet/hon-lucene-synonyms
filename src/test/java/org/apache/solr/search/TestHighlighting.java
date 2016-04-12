package org.apache.solr.search;

import org.junit.Test;

public class TestHighlighting extends HonLuceneSynonymTestCase {

    public TestHighlighting() {
        defaultRequest = new String[]{
                "qf",               "name",
                "mm",               "100%",
                "defType",          "synonym_edismax",
                "synonyms",         "true",
                "hl.simple.pre",    "<em>",
                "hl.simple.post",   "</em>",
                "hl",               "on"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "I have a dog and a pooch and a canis familiaris."}
        };
        commitDocs();
    }

    @Test
    public void queries() {
        String [] extraTests = {
                "//lst[@name='1']/arr[@name='name']/str[.='I have a <em>dog</em> and a <em>pooch</em> and a <em>canis</em> <em>familiaris</em>.']"
        };
        assertQuery(extraTests, "dog", 1);
        assertQuery(extraTests, "hound", 1);
        assertQuery(extraTests, "pooch", 1);
        assertQuery(extraTests, "canis familiaris", 1);
    }
}
