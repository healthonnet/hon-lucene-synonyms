package org.apache.solr.search;

import org.junit.Assert;
import org.junit.Test;

public class SynonymExpandingParserTest {

    @Test
    public void test1() throws ClassNotFoundException {
        Class<?> cast = Class.forName("org.apache.solr.search.QParserPlugin");
        Class<?> clazz = Class.forName("org.apache.solr.search.SynonymExpandingExtendedDismaxQParserPlugin");
        
        Assert.assertTrue(cast.isAssignableFrom(clazz));
        
    }
    
}
