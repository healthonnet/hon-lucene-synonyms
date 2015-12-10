package org.apache.solr.search;

import org.junit.Test;

import static junit.framework.Assert.*;

public class BasicSynonymExpandingParserTest {

    @Test
    public void test1() throws ClassNotFoundException {
        Class<?> cast = Class.forName("org.apache.solr.search.QParserPlugin");
        Class<?> clazz = Class.forName("org.apache.solr.search.SynonymExpandingExtendedDismaxQParserPlugin");
        
        assertTrue(cast.isAssignableFrom(clazz));
        
    }
    
}
