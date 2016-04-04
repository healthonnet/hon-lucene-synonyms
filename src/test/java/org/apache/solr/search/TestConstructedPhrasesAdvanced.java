package org.apache.solr.search;

import org.junit.Test;

public class TestConstructedPhrasesAdvanced extends HonLuceneSynonymTestCase {

    public TestConstructedPhrasesAdvanced() {
        defaultRequest = new String[] {
                "qf",       "name",
                "mm",       "100%",
                "defType",  "synonym_edismax",
                "synonyms", "true"
        };
        defaultDocs = new String[][] {
                {"id", "1", "name", "my dog licks my face"},
                {"id", "2", "name", "my canis familiaris licks my face"},
                {"id", "3", "name", "my man's best friend licks my face"},
                {"id", "4", "name", "backpack"}
        };
        commitDocs();
    }

    @Test
    public void constructPhrasesAdvanced() {
        // sanity test to make sure the quotes are being applied to the right place
        assertQuery("my dog licks my face", 3);
        assertQuery("my hound licks my face", 3);
        assertQuery("my canis familiaris licks my face", 3);
        assertQuery("my man's best friend licks my face", 3);

        assertQuery("my dog licks my face", 3, "synonyms.constructPhrases", "true");
        assertQuery("my hound licks my face", 3, "synonyms.constructPhrases", "true");
        assertQuery("my canis familiaris licks my face", 3, "synonyms.constructPhrases", "true");
        assertQuery("my man's best friend licks my face", 3, "synonyms.constructPhrases", "true");
    }

    @Test
    public void issue16() {
        assertQuery("meaningful token canis familiaris", 3, "mm", "01%");
        assertQuery("backpack", 1, "mm", "01%");
        assertQuery("back pack", 1, "mm", "01%");
        assertQuery("north land back pack", 1, "mm", "01%");
        assertQuery("north land backpack", 1, "mm", "01%");
    }
}
