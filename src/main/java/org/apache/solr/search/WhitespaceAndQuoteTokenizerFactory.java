package org.apache.solr.search;

import java.io.Reader;
import java.util.Map;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.AttributeSource.AttributeFactory;

/**
 * Copy of the WhitespaceTokenizerFactory, but tokenizes on quotes as well.  Seems to work really well for most
 * of our synonym-related use cases.
 * @author nolan
 *
 */
public class WhitespaceAndQuoteTokenizerFactory extends TokenizerFactory {

    public WhitespaceAndQuoteTokenizerFactory(Map<String, String> args) {
        super(args);
        assureMatchVersion();
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public WhitespaceAndQuoteTokenizer create(AttributeFactory factory, Reader input) {
        return new WhitespaceAndQuoteTokenizer(luceneMatchVersion, factory, input);
    }
    
    private static class WhitespaceAndQuoteTokenizer extends CharTokenizer {

        public WhitespaceAndQuoteTokenizer(Version matchVersion, Reader in) {
            super(matchVersion, in);
        }

        public WhitespaceAndQuoteTokenizer(Version matchVersion, AttributeFactory factory, Reader in) {
            super(matchVersion, factory, in);
        }

        /**
         * Collects only characters which do not satisfy
         * {@link Character#isWhitespace(int)}. or '"' This method represents the main
         * difference between this class and the normal WhitespaceTokenizer.
         */
        @Override
        protected boolean isTokenChar(int c) {
            return c != '"' && !Character.isWhitespace(c);
        }
    }
}
