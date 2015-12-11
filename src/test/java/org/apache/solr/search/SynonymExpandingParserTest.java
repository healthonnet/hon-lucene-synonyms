package org.apache.solr.search;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.BeforeClass;
import java.util.ArrayList;

public class SynonymExpandingParserTest extends AbstractSolrTestCase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("example_solrconfig.xml", "example_schema.xml");
        assertU(adoc("id", "1", "name", "I have a dog."));
        assertU(adoc("id", "2", "name", "I have a pooch."));
        assertU(adoc("id", "3", "name", "I have a hound."));
        assertU(adoc("id", "4", "name", "I have a canis."));
        assertU(commit());
    }

    public void tQuery(String query, int numFound) {
        SolrQueryRequest request = req("q", query,
                "fl", "*,score",
                "qf", "name",
                "defType", "synonym_edismax",
                "synonyms", "true",
                "synonyms.constructPhrases", "true",
                "debugQuery", "on");
        assertQuoteCount(request, numFound);
    }

    @Test
    public void testAllDocsQuery() {
        /* We have the synonyms:
        dog, pooch, hound, canis familiaris, man's best friend
        */
        tQuery("\"dog\"", 10);
        tQuery("\"pooch\"", 10);
        tQuery("\"hound\"", 10);
        tQuery("\"canis familiaris\"", 10);

        tQuery("dog",4);
        tQuery("pooch",4);
        tQuery("hound",4);
        tQuery("canis familiaris",2);
    }

    @Test
    @Ignore
    public void test_issue_51() {
        /* https://github.com/healthonnet/hon-lucene-synonyms/issues/51
        */
        tQuery("canis familiaris",4);
    }

    /** Validates a query matches some XPath test expressions and closes the query */
    public static void assertQuoteCount(SolrQueryRequest req, int quoteCount) {
        try {
            SolrQueryResponse rsp = h.queryAndResponse("/search",req);
            SimpleOrderedMap debug = (SimpleOrderedMap)rsp.getValues().get("debug");
            ArrayList<String> expandedSynonyms = (ArrayList<String>) debug.get("expandedSynonyms");
            AbstractSolrTestCase.log.info(expandedSynonyms.toString());
            int count = 0;
            for (String synonym : expandedSynonyms) {
                count += synonym.length() - synonym.replace("\"", "").length();
            }
            AbstractSolrTestCase.log.info("Quotes found count = " + count);
            req.close();
            assertEquals(quoteCount, count);
        } catch (Exception e2) {
            req.close();
            SolrException.log(log,"REQUEST FAILED: " + req.getParamString(), e2);
            throw new RuntimeException("Exception during query", e2);
        }
    }


}
