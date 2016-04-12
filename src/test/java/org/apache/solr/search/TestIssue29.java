package org.apache.solr.search;

import org.junit.Test;

public class TestIssue29 extends HonLuceneSynonymTestCase {

    public TestIssue29() {
        defaultRequest = new String[]{
                "qf",       "name",
                "mm",       "66%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "Everybody loves rutabagas."},
                {"id", "2", "name", "But what the hell are Swedish turnips?"},
                {"id", "3", "name", "Answer, the same thing, apparently."}
        };
        commitDocs();
    }

    @Test
    public void queries() {
        assertQuery("Swedish turnips", 2);
        assertQuery("healthy Swedish turnips", 2);
    }
}
