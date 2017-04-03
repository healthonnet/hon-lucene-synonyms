package com.github.healthonnet.search;

import java.io.IOException;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Test;

public class TestPreanalyzis extends HonLuceneSynonymTestCase {

    public TestPreanalyzis() {
        defaultRequest = new String[] { "qf", "name", "mm", "100%", "synonyms", "true", "defType", "synonym_edismax", "synonyms.preanalyzis", "true", "debugQuery", "on" };
        commitDocs();
    }

    @SuppressWarnings("rawtypes")
    private void verifyExpectedResults(String query, String expecteAnalysis, String expParsedQuery, int NumDoc, String... params) {
        SolrCore core = h.getCore();
        try (SolrQueryRequest req = constructRequest(query, params)) {
            SolrQueryResponse rsp = new SolrQueryResponse();
            core.execute(core.getRequestHandler(req.getParams().get(CommonParams.QT)), req, rsp);

            String preparsedQuery = ((NamedList) rsp.getValues().get("debug")).get("originalPreparsedQuery").toString();
            String parsedQuery = ((NamedList) rsp.getValues().get("debug")).get("parsedquery_toString").toString();
            int numFound = ((BasicResultContext) rsp.getValues().get("response")).getDocList().matches();
            assertEquals(expecteAnalysis, preparsedQuery);
            assertEquals(expParsedQuery, parsedQuery);
            assertEquals(NumDoc, numFound);

        }
    }

    @Test
    public void preAnalize() throws IOException {
        verifyExpectedResults("the dog", "dog", "+(((name:dog))^1.0 ((+(((name:canis) (name:familiaris))~2))^1.0) ((+(name:hound))^1.0) ((+(((name:man's) (name:best) (name:friend))~3))^1.0))", 3, "synonyms.preanalyzer", "mainFirstAnalyzer");
        verifyExpectedResults("a pooch", "", "MatchNoDocsQuery(\"\")", 0, "synonyms.preanalyzer", "mainFirstAnalyzer");
        verifyExpectedResults("a dog", "dog", "+(((name:dog))^1.0 ((+(name:familiaris))^1.0) ((+(name:hound))^1.0) ((+(((name:man's) (name:best) (name:friend))~3))^1.0) ((+(name:pooch))^1.0))", 5, "synonyms.preanalyzer", "mainSecondAnalyzer");

    }
}
