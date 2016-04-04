package org.apache.solr.search;

import org.junit.Test;

public class TestIssue31 extends HonLuceneSynonymTestCase {

    public TestIssue31() {
        defaultRequest = new String[]{
                "qf",                       "name",
                "mm",                       "100%",
                "defType",                  "synonym_edismax",
                "synonyms",                 "true",
                "fl",                       "*,score",
                "synonyms.originalBoost",   "1.0",
                "synonyms.synonymBoost",    "1.0"
        };
        defaultDocs = new String[][] {
                {
                        "id","1",
                        "name","Jigglypuff is top-tier.",
                        "cat","Y",
                        "last_modified","2012-01-01T00:00:00Z"
                },
                {
                        "id","2",
                        "name","Purin is top-tier.",
                        "cat","Y",
                        "last_modified","2012-01-01T00:00:00Z"
                }
        };
        commitDocs();
    }

    @Test
    public void queries() {

        // confirm that the documents have the same scores, since their dates and categories are the same
        allScoresEqual("jigglypuff");
        allScoresEqual("purin");

        String bq = "cat:Y^1000";
        String bf = "recip(ms(NOW,last_modified),3.16e-11,1,1)"; // standard date boosting function recommended in the solr docs

        // bq
        allScoresEqual("jigglypuff", "bq", bq);
        allScoresEqual("purin", "bq", bq);

        // bf
        allScoresEqual("jigglypuff", "bf", bf);
        allScoresEqual("purin", "bf", bf);

        // boost  (w/ standard date boosting function)
        allScoresEqual("jigglypuff", "boost", bf);
        allScoresEqual("purin", "boost", bf);
    }
}
