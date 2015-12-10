package org.apache.solr.search;

import org.junit.Test;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.BeforeClass;

import java.text.MessageFormat;

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
        assertQ(req("q", "'" + query + "'",
                "fl", "*,score",
                "qf", "name",
                "defType", "synonym_edismax",
                "synonyms", "true",
                "synonyms.constructPhrases", "true",
                "debugQuery", "on"),
                MessageFormat.format("//*[@numFound={0,number,integer}]", numFound));	// tests are xpath queries against solrxml);
    }

    @Test
    public void testAllDocsQuery() {
        /* We have the synonyms:
        dog, pooch, hound, canis familiaris, man's best friend
        */
        tQuery("\"dog\"",3);
        tQuery("\"canis familiaris\"",3);
        tQuery("caanis familiaris",3);
    }
}
