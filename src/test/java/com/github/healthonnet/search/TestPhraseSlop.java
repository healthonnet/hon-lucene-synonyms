package com.github.healthonnet.search;

import org.apache.lucene.document.Document;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSlice;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class TestPhraseSlop extends HonLuceneSynonymTestCase {

    public TestPhraseSlop() {
        defaultRequest = new String[] {
                "qf", "name",
                "fl", "id,score",
                "defType", "edismax"
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

    private void verifyExpectedResults(String query, Boolean inequalResults, String... params) {
        SolrCore core = h.getCore();
        try (SolrQueryRequest req = constructRequest(query, params)){
            SolrQueryResponse rsp = new SolrQueryResponse();
            core.execute(core.getRequestHandler(req.getParams().get(CommonParams.QT)), req, rsp);
            DocSlice docs = (DocSlice) ((ResultContext) rsp.getValues().get("response")).getDocList();
            ArrayList<Document> dl = docList(docs, req);
            Iterator<Document> di = dl.iterator();
            Iterator<String> ei = expectedDocIterator();
            assertEquals(dl.size(), expectedDocs.length);
            ArrayList<Float> scoreList = new ArrayList<>();
            DocIterator docItr = ((DocSlice) ((ResultContext) rsp.getValues().get("response")).getDocList()).iterator();
            while(docItr.hasNext()) {
                docItr.next();
                scoreList.add(docItr.score());
            }
            Float [] scores = scoreList.toArray(new Float[scoreList.size()]);
            if (inequalResults) {
                while (ei.hasNext() && di.hasNext()) {
                    assertEquals(ei.next(), di.next().get("id"));
                }
                assertTrue(String.format("%1$f > %2$f", scores[0], scores[1]), scores[0] > scores[1]);
            } else {
                assertArrayEquals(expectedDocs, idArray(dl));
                assertEquals(scores[0], scores[1], 0.001);
            }
        } catch (IOException e2) {
            throw new RuntimeException("Exception during query", e2);
        }
    }
    
    @Test
    public void phraseSlop() throws IOException{
        // without phrase slop, the two should be equal
        verifyExpectedResults("man's best friend", false);

        // with phrase slop, the first should have a higher score
        verifyExpectedResults("man's best friend", true, "ps", "3", "pf", "name");

        // ditto for synonym_edismax
        verifyExpectedResults("man's best friend", false, "defType", "synonym_edismax");
        verifyExpectedResults("man's best friend", true, "defType", "synonym_edismax", "ps", "3", "pf", "name");

        // but what about with... SYNONYMS?   dun dun dun!
        // (hint: it should be the same)
        verifyExpectedResults("man's best friend", false, "defType", "synonym_edismax", "synonyms", "true");
        verifyExpectedResults("man's best friend", true, "defType", "synonym_edismax", "synonyms", "true", "ps", "3", "pf", "name");

        // and what about with a synonym as input?  oh now you're just getting crazy
        verifyExpectedResults("dog", false, "defType", "synonym_edismax", "synonyms", "true");
        verifyExpectedResults("dog", true, "defType", "synonym_edismax", "synonyms", "true", "ps", "3", "pf", "name");
    }
}
