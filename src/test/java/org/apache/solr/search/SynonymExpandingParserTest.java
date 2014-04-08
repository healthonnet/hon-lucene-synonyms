package org.apache.solr.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.Version;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class SynonymExpandingParserTest {

    @Test
    public void test1() throws ClassNotFoundException {
        Class<?> cast = Class.forName("org.apache.solr.search.QParserPlugin");
        Class<?> clazz = Class.forName("org.apache.solr.search.SynonymExpandingExtendedDismaxQParserPlugin");
        
        assertTrue(cast.isAssignableFrom(clazz));
    }

    @Test
    @PrepareForTest({SynonymExpandingExtendedDismaxQParser.class, Analyzer.class, RamUsageEstimator.class})
    public void testLargeQueries() throws Exception {
        //Test large queries should not break Solr by creating huge permutations of synonyms.
        final ExtendedDismaxQParser dismaxQParser = Mockito.mock(ExtendedDismaxQParser.class);
        when(dismaxQParser.parse()).thenReturn(new TermQuery(new Term("parsedQuery")));
        Answer<ExtendedDismaxQParser> dismaxQParserAnswer = new Answer<ExtendedDismaxQParser>() {
            public ExtendedDismaxQParser answer(InvocationOnMock invocation) throws Throwable {
                return dismaxQParser;
            }
        };
        PowerMockito.whenNew(ExtendedDismaxQParser.class).withAnyArguments().thenAnswer(dismaxQParserAnswer);

        String hugeQuery = "dog byte dog byte";
        StringReader reader = new StringReader(hugeQuery);
        SolrParams params = new ModifiableSolrParams();
        LetterTokenizer tokenStream = new LetterTokenizer(Version.LUCENE_43, reader);
        SynonymMap.Builder synonymMapBuilder = new SynonymMap.Builder(true);
        synonymMapBuilder.add(new CharsRef("dog"), new CharsRef("hound"), false);
        synonymMapBuilder.add(new CharsRef("dog"), new CharsRef("pooch"), false);
        synonymMapBuilder.add(new CharsRef("dog"), new CharsRef("best man's friend"), false);
        synonymMapBuilder.add(new CharsRef("byte"), new CharsRef("nibble"), false);

        SynonymFilter synonymFilter = new SynonymFilter(tokenStream, synonymMapBuilder.build(), true);

        SynonymExpandingExtendedDismaxQParser parser = new SynonymExpandingExtendedDismaxQParser(hugeQuery, params, params, null, null);
        parser.setString(hugeQuery);

        Analyzer analyzer = PowerMockito.mock(Analyzer.class);
        PowerMockito.when(analyzer.tokenStream(anyString(), any(Reader.class))).thenReturn(synonymFilter);
        int maxAlterateQueries = 5;
        parser.setAlternateQueryLimit(maxAlterateQueries);

        try {
            List<Query> result = parser.generateSynonymQueries(analyzer, params);
            assertEquals(maxAlterateQueries, result.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}