/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This parser was originally derived from DismaxQParser from Solr.
 * All changes are Copyright 2008, Lucid Imagination, Inc.
 */

package org.apache.solr.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;

/**
 * An advanced multi-field query parser.
 * 
 * @lucene.experimental
 */
public class SynonymExpandingExtendedDismaxQParserPlugin extends QParserPlugin implements
        ResourceLoaderAware {
    public static final String NAME = "synonym_edismax";

    private NamedList<?> args;
    private Map<String, Analyzer> synonymAnalyzers;

    @SuppressWarnings("rawtypes")
    public void init(NamedList args) {
        this.args = (NamedList<?>)args;
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new SynonymExpandingExtendedDismaxQParser(qstr, localParams, params, req, synonymAnalyzers);
    }
    
    private Map<String, String> convertNamedListToMap(NamedList<?> namedList) {
        Map<String, String> result = new HashMap<String, String>();
        
        for (Entry<String, ?> entry : namedList) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String)entry.getValue());
            }
        }
        
        return result;
    }

    public void inform(ResourceLoader loader) throws IOException {
        // TODO it would be nice if the user didn't have to encode tokenizers/filters
        // as a NamedList.  But for now this is the hack I'm using
        synonymAnalyzers = new HashMap<String, Analyzer>();

        Object testMatchVersion = args.get("luceneMatchVersion");
        Version luceneMatchVersion = null;
        if (testMatchVersion == null || !(testMatchVersion instanceof String)) {
            throw new SolrException(ErrorCode.SERVER_ERROR,
                    "luceneMatchVersion must be defined for the synonym_edismax parser");
        } else {
            luceneMatchVersion = Version.valueOf(args.get("luceneMatchVersion").toString());
        }
        
        Object xmlSynonymAnalyzers = args.get("synonymAnalyzers");
        
        if (xmlSynonymAnalyzers != null && xmlSynonymAnalyzers instanceof NamedList) {
            NamedList<?> synonymAnalyzersList = (NamedList<?>) xmlSynonymAnalyzers;
            for (Entry<String, ?> entry : synonymAnalyzersList) {
                String analyzerName = entry.getKey();
                if (!(entry.getValue() instanceof NamedList)) {
                    continue;
                }
                NamedList<?> analyzerAsNamedList = (NamedList<?>) entry.getValue();
                
                TokenizerFactory tokenizerFactory = null;
                List<TokenFilterFactory> filterFactories = new LinkedList<TokenFilterFactory>();
                
                for (Entry<String, ?> analyzerEntry : analyzerAsNamedList) {
                    String key = analyzerEntry.getKey();
                    if (!(entry.getValue() instanceof NamedList)) {
                        continue;
                    }
                    Map<String, String> params = convertNamedListToMap((NamedList<?>)analyzerEntry.getValue());
                    
                    if (!params.containsKey("class")) {
                        continue;
                    }

                    String className = params.get("class");
                    if (key.equals("tokenizer")) {
                        tokenizerFactory = (TokenizerFactory) loader.newInstance(className, TokenizerFactory.class);
                        tokenizerFactory.setLuceneMatchVersion(luceneMatchVersion);
                        tokenizerFactory.init(params);
                        if (tokenizerFactory instanceof ResourceLoaderAware) {
                            ((ResourceLoaderAware)tokenizerFactory).inform(loader);
                        }
                    } else if (key.equals("filter")) {
                        TokenFilterFactory filterFactory = (TokenFilterFactory) loader.newInstance(className, TokenFilterFactory.class);
                        filterFactory.setLuceneMatchVersion(luceneMatchVersion);
                        filterFactory.init(params);
                        if (filterFactory instanceof ResourceLoaderAware) {
                            ((ResourceLoaderAware)filterFactory).inform(loader);
                        }
                        filterFactories.add(filterFactory);
                    }
                }
                if (tokenizerFactory == null) {
                    throw new SolrException(ErrorCode.SERVER_ERROR, 
                            "tokenizer must not be null for synonym analyzer: " + analyzerName);
                } else if (filterFactories.isEmpty()) {
                    throw new SolrException(ErrorCode.SERVER_ERROR, 
                            "filter factories must be defined for synonym analyzer: " + analyzerName);
                }
                
                TokenizerChain analyzer = new TokenizerChain(tokenizerFactory, 
                        filterFactories.toArray(new TokenFilterFactory[filterFactories.size()]));
                
                synonymAnalyzers.put(analyzerName, analyzer);
            }
        }
    }

}

class SynonymExpandingExtendedDismaxQParser extends ExtendedDismaxQParser {

    /**
     * Convenience class for parameters
     */
    public static class Params {
        public static final String SYNONYMS = "synonyms";
        public static final String SYNONYMS_ANALYZER = "synonyms.analyzer";
        public static final String SYNONYMS_ORIGINAL_BOOST = "synonyms.originalBoost";
        public static final String SYNONYMS_SYNONYM_BOOST = "synonyms.synonymBoost";
        public static final String SYNONYMS_DISABLE_PHRASE_QUERIES = "synonyms.disablePhraseQueries";
        public static final String SYNONYMS_CONSTRUCT_PHRASES = "synonyms.constructPhrases";
        
    }

    /**
     * Convenience class for calling constants.
     * @author nolan
     *
     */
    private static class Const {
        /**
         * A field we can't ever find in any schema, so we can safely tell
         * DisjunctionMaxQueryParser to use it as our defaultField, and map aliases
         * from it to any field in our schema.
         */
        static final String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";
        
        static final Pattern COMPLEX_QUERY_OPERATORS_PATTERN = Pattern.compile("(?:\\*|\\b(?:OR|AND|-|\\+)\\b)"); 
    }

    private Map<String, Analyzer> synonymAnalyzers;
    private Query queryToHighlight;
    
    public SynonymExpandingExtendedDismaxQParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req, Map<String, Analyzer> synonymAnalyzers) {
        super(qstr, localParams, params, req);
        this.synonymAnalyzers = synonymAnalyzers;
    }



    @Override
    public Query getHighlightQuery() throws ParseException {
        return queryToHighlight != null ? queryToHighlight : super.getHighlightQuery();
    }
    

    @Override
    public Query parse() throws ParseException {
        Query query = super.parse();

        SolrParams localParams = getLocalParams();
        SolrParams params = getParams();
        SolrParams solrParams = localParams == null ? params : SolrParams.wrapDefaults(localParams, params);

        // disable/enable synonym handling altogether
        if (!solrParams.getBool(Params.SYNONYMS, false)) {
            return query;
        }
        
        // check to make sure the analyzer exists
        String analyzerName = solrParams.get(Params.SYNONYMS_ANALYZER, null);
        if (analyzerName == null) { // no synonym analyzer specified
            if (synonymAnalyzers.size() == 1) {
                // only one analyzer defined; just use that one
                analyzerName = synonymAnalyzers.keySet().iterator().next();
            } else {
                return query;
            }
        }
        
        Analyzer synonymAnalyzer = synonymAnalyzers.get(analyzerName);
        
        if (synonymAnalyzer == null) { // couldn't find analyzer
            return query;
        }
        
        if (solrParams.getBool(Params.SYNONYMS_DISABLE_PHRASE_QUERIES, false)
                && getQueryStringFromParser().indexOf('"') != -1) {
            // disable if a phrase query is detected, i.e. there's a '"'
            return query;
        }

        try {
            attemptToApplySynonymsToQuery(query, solrParams, synonymAnalyzer);
        } catch (IOException e) {
            // TODO: better error handling - for now just bail out
            e.printStackTrace(System.err);
        }

        return query;
    }

    private void attemptToApplySynonymsToQuery(Query query, SolrParams solrParams, Analyzer synonymAnalyzer) throws IOException {
        
        List<Query> synonymQueries = generateSynonymQueries(synonymAnalyzer, solrParams);
        
        boolean hasComplexQueryOperators = Const.COMPLEX_QUERY_OPERATORS_PATTERN.matcher(getQueryStringFromParser()).find();
        
        if (hasComplexQueryOperators // TODO: support complex operators
                || synonymQueries.isEmpty()) { // didn't find more than 0 synonyms, i.e. it's just the original phrase
            return;
        }
        
        float originalBoost = solrParams.getFloat(Params.SYNONYMS_ORIGINAL_BOOST, 1.0F);
        float synonymBoost = solrParams.getFloat(Params.SYNONYMS_SYNONYM_BOOST, 1.0F);
        
        applySynonymQueries(query, synonymQueries, originalBoost, synonymBoost);
    }

    /**
     * Find the main query and its surrounding clause, make it SHOULD instead of MUST and append a bunch
     * of other SHOULDs to it, then wrap it in a MUST
     * 
     * E.g. +(text:dog) becomes
     * +((text:dog)^1.5 ((text:hound) (text:pooch))^1.2)
     * @param query
     * @param synonymQueries
     * @param originalBoost
     * @param synonymBoost
     */
    private void applySynonymQueries(Query query, List<Query> synonymQueries, float originalBoost, float synonymBoost) {

        if (query instanceof BoostedQuery) {
            applySynonymQueries(((BoostedQuery) query).getQuery(), synonymQueries, originalBoost, synonymBoost);
        } else if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery =(BooleanQuery) query;
            
            for (BooleanClause booleanClause : booleanQuery.getClauses()) {
                if (Occur.MUST == booleanClause.getOccur()) {
                    // standard 'must occur' clause - i.e. the main user query    
                    
                    Query mainUserQuery = booleanClause.getQuery();
                    mainUserQuery.setBoost(originalBoost);
                    
                    // combine all synonym queries together with the same boost
                    BooleanQuery allSynonymQueries = new BooleanQuery();
                    for (Query synonymQuery : synonymQueries) {
                        allSynonymQueries.add(synonymQuery, Occur.SHOULD);
                    }
                    
                    allSynonymQueries.setBoost(synonymBoost);
                    
                    // now combine with the original main user query
                    BooleanQuery combinedQuery = new BooleanQuery();
                    combinedQuery.add(mainUserQuery, Occur.SHOULD);
                    combinedQuery.add(allSynonymQueries, Occur.SHOULD);
                    booleanClause.setQuery(combinedQuery);
                    queryToHighlight = combinedQuery;
                }
            }
        }
    }

    /**
     * Given the synonymAnalyzer, returns a list of all alternate queries expanded from the original user query.
     * @param synonymAnalyzer
     * @param solrParams
     * @return
     */
    private List<Query> generateSynonymQueries(Analyzer synonymAnalyzer, SolrParams solrParams) throws IOException {

        // TODO: make the token stream reusable?
        TokenStream tokenStream = synonymAnalyzer.tokenStream(Const.IMPOSSIBLE_FIELD_NAME,
                new StringReader(getQueryStringFromParser()));
        
        SortedMap<Integer, SortedSet<TextInQuery>> startPosToTextsInQuery = new TreeMap<Integer, SortedSet<TextInQuery>>();
        
        
        boolean constructPhraseQueries = solrParams.getBool(Params.SYNONYMS_CONSTRUCT_PHRASES, false);
        
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAttribute = tokenStream.getAttribute(TypeAttribute.class);
                
                if (!typeAttribute.type().equals("shingle")) {
                    // ignore shingles; we only care about synonyms and the original text
                    // TODO: filter other types as well
                    
                    String termToAdd = term.toString();
                    
                    if (constructPhraseQueries && typeAttribute.type().equals("SYNONYM")) {
                        // make a phrase out of the synonym
                        termToAdd = new StringBuilder(termToAdd).insert(0,'"').append('"').toString();
                    }
                    
                    TextInQuery textInQuery = new TextInQuery(termToAdd, 
                            offsetAttribute.startOffset(), 
                            offsetAttribute.endOffset());
                    
                    // brain-dead multimap logic... man, I wish we had Google Guava here
                    SortedSet<TextInQuery> existingList = startPosToTextsInQuery.get(offsetAttribute.startOffset());
                    if (existingList == null) {
                        existingList = new TreeSet<TextInQuery>();
                        startPosToTextsInQuery.put(offsetAttribute.startOffset(), existingList);
                    }
                    existingList.add(textInQuery);
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            throw new RuntimeException("uncaught exception in synonym processing", e);
        } finally {
            try {
                tokenStream.close();
            } catch (IOException e) {
                throw new RuntimeException("uncaught exception in synonym processing", e);
            }
        }
        
        List<List<TextInQuery>> sortedTextsInQuery = new ArrayList<List<TextInQuery>>(
                startPosToTextsInQuery.values().size());
        for (SortedSet<TextInQuery> sortedSet : startPosToTextsInQuery.values()) {
            sortedTextsInQuery.add(new ArrayList<TextInQuery>(sortedSet));
        }
        
        // have to use the start positions and end positions to figure out all possible combinations
        List<String> alternateQueries = buildUpAlternateQueries(solrParams, sortedTextsInQuery);

        return createSynonymQueries(solrParams, alternateQueries);
    }

    /**
     * From a list of texts in the original query that were deemed to be interested (i.e. synonyms or the original text
     * itself), build up all possible alternate queries as strings.
     * 
     * For instance, if the query is "dog bite" and the synonyms are dog -> [dog,hound,pooch] and bite -> [bite,nibble],
     * then the result will be:
     * 
     * dog bite
     * hound bite
     * pooch bite
     * dog nibble
     * hound nibble
     * pooch nibble
     * 
     * @param solrParams
     * @param textsInQueryLists
     * @return
     */
    private List<String> buildUpAlternateQueries(SolrParams solrParams, List<List<TextInQuery>> textsInQueryLists) {
        
        String originalUserQuery = getQueryStringFromParser();
        
        if (textsInQueryLists.isEmpty()) {
            return Collections.emptyList();
        }
        
        // initialize results
        List<AlternateQuery> alternateQueries = new ArrayList<AlternateQuery>();
        for (TextInQuery textInQuery : textsInQueryLists.get(0)) {
            // add the text before the first user query token, e.g. a space or a "
            StringBuilder stringBuilder = new StringBuilder(
                    originalUserQuery.subSequence(0, textInQuery.getStartPosition()))
                    .append(textInQuery.getText());
            alternateQueries.add(new AlternateQuery(stringBuilder, textInQuery.getEndPosition()));
        }
        
        for (int i = 1; i < textsInQueryLists.size(); i++) {
            List<TextInQuery> textsInQuery = textsInQueryLists.get(i);
            
            // compute the length in advance, because we'll be adding new ones as we go
            int alternateQueriesLength = alternateQueries.size();
            
            for (int j = 0; j < alternateQueriesLength; j++) {
                
                // When we're working with a lattice, assuming there's only one path to take in the next column,
                // we can (and MUST) use all the original objects in the current column.
                // It's only when we have >1 paths in the next column that we need to start taking copies.
                // So if a lot of this logic seems tortured, it's only because I'm trying to minimize object
                // creation.
                AlternateQuery currentAlternateQuery = alternateQueries.get(j);
                AlternateQuery originalAlternateQuery = currentAlternateQuery;
                
                boolean usedFirst = false;
                
                for (int k = 0;k < textsInQuery.size(); k++) {
                    
                    TextInQuery textInQuery =  textsInQuery.get(k);
                    if (originalAlternateQuery.getEndPosition() > textInQuery.getStartPosition()) {
                        // cannot be appended, e.g. "canis" token in "canis familiaris"
                        continue;
                    }
                    if (!usedFirst) {
                        // re-use the existing object
                        usedFirst = true;
                        if (textsInQuery.size() > 1) {
                            // make a defensive clone for future usage
                            originalAlternateQuery = (AlternateQuery) currentAlternateQuery.clone();
                        }
                    } else if (k == textsInQuery.size() - 1) {
                        // we're sure we're the last one to use it, so we can just use the original clone
                        currentAlternateQuery = originalAlternateQuery;
                    } else {
                        // need to clone to a new object
                        currentAlternateQuery = (AlternateQuery) originalAlternateQuery.clone();
                        alternateQueries.add(currentAlternateQuery);
                    }
                    // text in the original query between the two tokens, usually a space, comma, etc.
                    CharSequence betweenTokens = originalUserQuery.subSequence(
                            currentAlternateQuery.getEndPosition(), textInQuery.getStartPosition());
                    currentAlternateQuery.getStringBuilder().append(betweenTokens).append(textInQuery.getText());
                    currentAlternateQuery.setEndPosition(textInQuery.getEndPosition());
                }
            }
        }
        
        List<String> result = new ArrayList<String>();
        
        for (AlternateQuery alternateQuery : alternateQueries) {
            
            StringBuilder sb = alternateQuery.getStringBuilder();
            
            // append whatever text followed the last token, e.g. '"'
            sb.append(originalUserQuery.subSequence(alternateQuery.getEndPosition(), originalUserQuery.length()));
            
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * From a list of alternate queries in text format, parse them using the default
     * ExtendedSolrQueryParser and return the queries.
     * 
     * @param solrParams
     * @param alternateQueryTexts
     * @return
     */
    private List<Query> createSynonymQueries(SolrParams solrParams, List<String> alternateQueryTexts) {
        
        String originalString = getString();
        String nullsafeOriginalString = getQueryStringFromParser();
        
        List<Query> result = new ArrayList<Query>();
        for (String alternateQueryText : alternateQueryTexts) {
            if (alternateQueryText.equalsIgnoreCase(nullsafeOriginalString)) { 
                // alternate query is the same as what the user entered
                continue;
            }
            
            super.setString(alternateQueryText);
            try {
                result.add(super.parse());
            } catch (ParseException e) {
                // TODO: better error handling - for now just bail out; ignore this synonym
                e.printStackTrace(System.err);
            }
        }
        
        super.setString(originalString); // cover our tracks
        
        return result;
    }

    /**
     * Ensures that we return a valid string, even if null
     * @return the entered query string fetched from QParser.getString()
     */
    private String getQueryStringFromParser() {
      return (getString() == null) ? "" : getString();
    }
    
    /**
     * Simple POJO for representing a piece of text found in the original query or expanded using shingles/synonyms.
     * @author nolan
     *
     */
    private static class TextInQuery implements Comparable<TextInQuery> {

        private String text;
        private int endPosition;
        private int startPosition;
        
        public TextInQuery(String text, int startPosition, int endPosition) {
            this.text = text;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
        
        public String getText() {
            return text;
        }
        public int getEndPosition() {
            return endPosition;
        }
        public int getStartPosition() {
            return startPosition;
        }
        
        @Override
        public String toString() {
            return "TextInQuery [text=" + text + ", endPosition=" + endPosition + ", startPosition=" + startPosition + "]";
        }

        public int compareTo(TextInQuery other) {
            if (this.startPosition != other.startPosition) {
                return this.startPosition - other.startPosition;
            } else if (this.endPosition != other.endPosition) {
                return this.endPosition - other.endPosition;
            }
            return this.text.compareTo(other.text);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endPosition;
            result = prime * result + startPosition;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TextInQuery other = (TextInQuery) obj;
            if (endPosition != other.endPosition)
                return false;
            if (startPosition != other.startPosition)
                return false;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }
    }
    
    /**
     * Simple POJO for containing an alternate query that we're building up
     * @author nolan
     *
     */
    private static class AlternateQuery implements Cloneable {

        private StringBuilder stringBuilder;
        private int endPosition;
        
        public AlternateQuery(StringBuilder stringBuilder, int endPosition) {
            this.stringBuilder = stringBuilder;
            this.endPosition = endPosition;
        }

        public StringBuilder getStringBuilder() {
            return stringBuilder;
        }

        public int getEndPosition() {
            return endPosition;
        }
        
        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public Object clone() {
            return new AlternateQuery(new StringBuilder(stringBuilder), endPosition);
        }

        @Override
        public String toString() {
            return "AlternateQuery [stringBuilder=" + stringBuilder + ", endPosition=" + endPosition + "]";
        }
    }

}
