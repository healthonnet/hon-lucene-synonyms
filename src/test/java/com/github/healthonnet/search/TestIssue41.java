package com.github.healthonnet.search;

import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Test;

import java.util.ArrayList;

public class TestIssue41 extends HonLuceneSynonymTestCase {

    public TestIssue41() {
        defaultDocs =  new String[][] {
                {"id", "1", "name", "I have a dog."},
                {"id", "2", "name", "I have a pooch."},
                {"id", "3", "name", "I have a hound."},
                {"id", "4", "name", "I have a canis."}
        };
        defaultRequest = new String[] {
                "fl", "*,score",
                "qf", "name",
                "defType", "synonym_edismax",
                "synonyms", "true",
                "synonyms.constructPhrases", "true",
                "debugQuery", "on"
        };
        commitDocs();
    }

    @Test
    public void testAllDocsQuery() {
        /* We have the synonyms:
        dog, pooch, hound, canis familiaris, man's best friend
        */
        assertQuoteCount("\"dog\"", 10);
        assertQuoteCount("\"pooch\"", 10);
        assertQuoteCount("\"hound\"", 10);
        assertQuoteCount("\"canis familiaris\"", 10);

        assertQuoteCount("dog",4);
        assertQuoteCount("pooch",4);
        assertQuoteCount("hound",4);
        assertQuoteCount("canis familiaris",2);
    }

    /**
     * Validates a query matches some XPath test expressions and closes the query.
     */
    private void assertQuoteCount(String query, int quoteCount) {
        try (SolrQueryRequest req = constructRequest(query)) {
            SolrQueryResponse rsp = h.queryAndResponse("/search", req);
            SimpleOrderedMap debug = (SimpleOrderedMap) rsp.getValues().get("debug");
            ArrayList<String> expandedSynonyms = (ArrayList<String>) debug.get("expandedSynonyms");
            int count = 0;
            for (String synonym : expandedSynonyms) {
                count += synonym.length() - synonym.replace("\"", "").length();
            }
            assertEquals(quoteCount, count);
        } catch (Exception e2) {
            throw new RuntimeException("Exception during query", e2);
        }
    }
}
