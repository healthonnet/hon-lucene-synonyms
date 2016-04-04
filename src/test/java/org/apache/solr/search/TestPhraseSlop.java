package org.apache.solr.search;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class TestPhraseSlop extends HonLuceneSynonymTestCase {

    public TestPhraseSlop() {
        defaultRequest = new String[] {
                "qf", "name",
                "fl", "id,score"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "man's best friend blah blah blah"},
                {"id", "2", "name", "man's blah best blah blah friend"}
        };
        commitDocs();
    }

    private static final String[] expectedDocs = { "1", "2" };

    private static Iterator<String> expectedDocIterator() {
        return Arrays.asList(expectedDocs).iterator();
    }

    private void verifyExpectedResults(Boolean synonymEDisMax, String query, Boolean inequalResults, String... params) throws IOException {
        SolrCore core = h.getCore();
        SolrQueryRequest req;
        String[] queryStr;
        if (params.length  > 0) {
            queryStr = Stream.concat(Arrays.stream(defaultRequest), Arrays.stream(params)).toArray(String[]::new);
        } else {
            queryStr = defaultRequest;
        }
        if (synonymEDisMax) {
            req = req(queryStr,
                    "q",        query,
                    "defType", "synonym_edismax");
        } else {
            req = req(queryStr,
                    "defType",  "edismax",
                    "q",        query);
        }
        SolrQueryResponse rsp = new SolrQueryResponse();
        core.execute(core.getRequestHandler(req.getParams().get(CommonParams.QT)), req, rsp);
        DocSlice docs = (DocSlice) ((ResultContext) rsp.getValues().get("response")).docs;
        ArrayList<Document> dl = docList(docs, req);
        Iterator<Document> di = dl.iterator();
        Iterator<String> ei = expectedDocIterator();
        assertEquals(dl.size(),expectedDocs.length);
        float[] scores = ((DocSlice) ((ResultContext) rsp.getValues().get("response")).docs).scores;
        if (inequalResults) {
            while(ei.hasNext() && di.hasNext()) {
                assertEquals(ei.next(), di.next().get("id"));
            }
            assertTrue(scores[0] > scores[1]);
        } else {
            assertArrayEquals(expectedDocs, idArray(dl));
            assertEquals(scores[0], scores[1], 0.001);
        }
        req.close();
    }
    
    @Test
    public void phraseSlop() throws IOException{
        // without phrase slop, the two should be equal
        verifyExpectedResults(false, "man's best friend", false);

        // with phrase slop, the first should have a higher score
        verifyExpectedResults(false, "man's best friend", true, "ps", "3", "pf", "name");

        // ditto for synonym_edismax
        verifyExpectedResults(true, "man's best friend", false);
        verifyExpectedResults(true, "man's best friend", true, "ps", "3", "pf", "name");

        // but what about with... SYNONYMS?   dun dun dun!
        // (hint: it should be the same)
        verifyExpectedResults(true, "man's best friend", false, "synonyms", "true");
        verifyExpectedResults(true, "man's best friend", true, "synonyms", "true", "ps", "3", "pf", "name");

        // and what about with a synonym as input?  oh now you're just getting crazy
        verifyExpectedResults(true, "dog", false, "synonyms", "true");
        verifyExpectedResults(true, "dog", true, "synonyms", "true", "ps", "3", "pf", "name");
    }
}
