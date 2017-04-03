package com.github.healthonnet.search;

import java.io.IOException;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Test;

public class TestMultiAnalyzer extends HonLuceneSynonymTestCase {

    public TestMultiAnalyzer() {
        defaultRequest = new String[] { "qf", "name", "mm", "100%", "synonyms", "true", "defType", "synonym_edismax", "debugQuery", "on" };
        commitDocs();
    }

    @SuppressWarnings("rawtypes")
    private void verifyExpectedResults(String query, String expParsedQuery, int NumDoc, String... params) {
        SolrCore core = h.getCore();
        try (SolrQueryRequest req = constructRequest(query, params)) {
            SolrQueryResponse rsp = new SolrQueryResponse();
            core.execute(core.getRequestHandler(req.getParams().get(CommonParams.QT)), req, rsp);

            String parsedQuery = ((NamedList) rsp.getValues().get("debug")).get("parsedquery_toString").toString();
            int numFound = ((BasicResultContext) rsp.getValues().get("response")).getDocList().matches();
            assertEquals(expParsedQuery, parsedQuery);
            assertEquals(NumDoc, numFound);

        }
    }

    @Test
    public void multiSynonymsFile() throws IOException {
        verifyExpectedResults("cat", "+(name:cat)", 0);
        verifyExpectedResults("cat", "+(name:cat)", 0, "synonyms.analyzer", "myCoolAnalyzer");
        verifyExpectedResults("cat", "+(((name:cat))^1.0 ((+(name:dog))^1.0))", 1, "synonyms.analyzer", "mySecondAnalyzer");

    }

}
