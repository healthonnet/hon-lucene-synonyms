package com.github.healthonnet.search;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestBasicSynonymExpandingParser {

    @Test
    public void test1() throws ClassNotFoundException {
        Class<?> cast = Class.forName("org.apache.solr.search.QParserPlugin");
        Class<?> clazz = Class.forName("com.github.healthonnet.search.SynonymExpandingExtendedDismaxQParserPlugin");
        
        assertTrue(cast.isAssignableFrom(clazz));
        
    }
    
}
