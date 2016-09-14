package com.github.healthonnet.search;

import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Test;

import java.util.ArrayList;

public class TestIssue65 extends HonLuceneSynonymTestCase {

    /**
     * This will verify that valid query is generated even when bf is present
     **/
    public TestIssue65() {
        defaultDocs =  new String[][] {
                {"id", "1", "name", "I have a backpack"},
                {"id", "2", "name", "I have a back pack"},
                {"id", "3", "name", "I have a house"},
                {"id", "4", "name", "I have a car"}
        };
        defaultRequest = new String[] {
                "fl", "*,score",
                "qf", "name",
                "defType", "synonym_edismax",
                "synonyms", "true",
                "debugQuery", "on",
                "bf", "10"
        };
        commitDocs();
    }

    @Test
    public void queries() {
        assertQuery("back pack", 2);
    }
}