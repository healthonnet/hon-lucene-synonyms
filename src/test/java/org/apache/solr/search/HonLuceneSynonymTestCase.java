package org.apache.solr.search;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class HonLuceneSynonymTestCase extends AbstractSolrTestCase {
    String[] defaultRequest = {
            "qf", "name",
            "mm", "100%",
    };
    String[][] defaultDocs = {
            {"id", "1", "name", "dog"},
            {"id", "2", "name", "pooch"},
            {"id", "3", "name", "hound"},
            {"id", "4", "name", "canis familiaris"},
            {"id", "5", "name", "canis"},
            {"id", "6", "name", "familiaris"}
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("example_solrconfig.xml", "example_schema.xml");
    }

    void commitDocs() {
        for( String[] d: defaultDocs) {
            assertU(adoc(d));
        }
        assertU(commit());
        assertU(optimize());
    }

    /**
     * Returns an array of parameters with params being first to override anything in default.
     */
    private static String[] queryArray(String[] defaults, String... params) {
        if (params.length  > 0) {
            return Stream.concat(Arrays.stream(params), Arrays.stream(defaults)).toArray(String[]::new);
        } else {
            return defaults;
        }
    }
    void assertQuery(String[] tests, String query, int expectedNumDocs, String... params) {
        String[] defaultTests = new String[] {String.format("//result[@numFound=%1$d]", expectedNumDocs)};
        if (tests == null) {
            tests = defaultTests;
        } else {
            tests = Stream.concat(Arrays.stream(defaultTests), Arrays.stream(tests)).toArray(String[]::new);
        }
        assertQ(req(queryArray(defaultRequest, params),
                "q", query),
                tests);
    }

    void assertQuery(String query, int expectedNumDocs, String... params) {
        assertQuery(null, query, expectedNumDocs, params);
    }

    static ArrayList<Document> docList(DocList dl, SolrQueryRequest req) throws IOException {
        ArrayList<Document> docList = new ArrayList<>();
        DocIterator di = dl.iterator();
        while(di.hasNext()) {
            Document doc = req.getSearcher().doc(di.nextDoc());
            docList.add(doc);
        }
        return docList;
    }

    static String[] idArray(ArrayList<Document> docList) {
        ArrayList<String> idStash = docList.stream().map(d -> d.get("id")).collect(Collectors.toCollection(ArrayList::new));
        String[] idReturn = new String[idStash.size()];
        return idStash.toArray(idReturn);
    }

    SolrQueryRequest constructRequest(String query, String... params) {
        String[] queryStr;
        if (params.length  > 0) {
            queryStr = Stream.concat(Arrays.stream(defaultRequest), Arrays.stream(params)).toArray(String[]::new);
        } else {
            queryStr = defaultRequest;
        }
        return req(queryStr, "q", query);
    }

    /**
     * allScoresEqual with a default expectedNumDocs = 2
     */
    void allScoresEqual(String query, String... params) {
        allScoresEqual(query, 2, params);
    }

    void allScoresEqual(String query, int expectedNumDocs, String... params) {
        String[] numFoundTest = new String[] {String.format("//result[@numFound=%1$d]", expectedNumDocs)};
        SolrCore core = h.getCore();
        SolrQueryRequest req = constructRequest(query,params);
        assertQ(req, numFoundTest);

        // verify that all returned docs have the same score
        SolrQueryResponse rsp = new SolrQueryResponse();
        core.execute(core.getRequestHandler(req.getParams().get(CommonParams.QT)), req, rsp);
        float[] scores = ((DocSlice) ((ResultContext) rsp.getValues().get("response")).docs).scores;
        Set<Float> scoreSet = new HashSet<>();
        for(float s: scores) {
            scoreSet.add(s);
        }
        assertEquals(1, scoreSet.size());
        req.close();
    }
}
