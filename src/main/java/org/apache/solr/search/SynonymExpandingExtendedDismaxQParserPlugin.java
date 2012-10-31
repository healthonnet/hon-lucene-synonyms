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
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.DefaultSolrParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.function.BoostedQuery;
import org.apache.solr.util.SolrPluginUtils;
import org.apache.solr.util.plugin.ResourceLoaderAware;

/**
 * An advanced multi-field query parser.
 * 
 * @lucene.experimental
 */
public class SynonymExpandingExtendedDismaxQParserPlugin extends ExtendedDismaxQParserPlugin implements
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

    public void inform(ResourceLoader loader) {
        // TODO it would be nice if the user didn't have to encode tokenizers/filters
        // as a NamedList.  But for now this is the hack I'm using
        synonymAnalyzers = new HashMap<String, Analyzer>();
        
        Object luceneMatchVersion = args.get("luceneMatchVersion");
        if (luceneMatchVersion == null || !(luceneMatchVersion instanceof String)) {
            throw new SolrException(ErrorCode.SERVER_ERROR, 
                    "luceneMatchVersion must be defined for the synonym_edismax parser");
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
                    
                    // add the lucene match version because it's usually required
                    params.put("luceneMatchVersion", (String)luceneMatchVersion);
                    
                    if (!params.containsKey("class")) {
                        continue;
                    }

                    String className = params.get("class");
                    if (key.equals("tokenizer")) {
                        tokenizerFactory = (TokenizerFactory) loader.newInstance(className);
                        tokenizerFactory.init(params);
                        if (tokenizerFactory instanceof ResourceLoaderAware) {
                            ((ResourceLoaderAware)tokenizerFactory).inform(loader);
                        }
                    } else if (key.equals("filter")) {
                        TokenFilterFactory filterFactory = (TokenFilterFactory) loader.newInstance(className);
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
        
        static final Pattern COMPLEX_QUERY_OPERATORS_PATTERN = Pattern.compile("\\b(?:OR|AND|-|\\+)\\b"); 
    }

    /** shorten the class references for utilities */
    private static class U extends SolrPluginUtils {
      /* :NOOP */
    }
    
    Map<String, Analyzer> synonymAnalyzers;
    
    public SynonymExpandingExtendedDismaxQParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req, Map<String, Analyzer> synonymAnalyzers) {
        super(qstr, localParams, params, req);
        this.synonymAnalyzers = synonymAnalyzers;
    }

    @Override
    public Query parse() throws ParseException {
        Query query = super.parse();
        
        SolrParams localParams = getLocalParams();
        SolrParams params = getParams();
        SolrParams solrParams = localParams == null ? params : new DefaultSolrParams(localParams, params);

        // disable/enable synonym handling altogether
        if (!solrParams.getBool(Params.SYNONYMS, false)) {
            return query;
        }
        
        // check to make sure the analyzer exists
        String analyzerName = solrParams.get(Params.SYNONYMS_ANALYZER, null);
        if (analyzerName == null) { // no synonym analyzer specified
            return query;
        }
        
        Analyzer synonymAnalyzer = synonymAnalyzers.get(analyzerName);
        
        if (synonymAnalyzer == null) { // couldn't find analyzer
            return query;
        }
        
        if (solrParams.getBool(Params.SYNONYMS_DISABLE_PHRASE_QUERIES, false)
                && getString().indexOf('"') != -1) {
            // disable if a phrase query is detected, i.e. there's a '"'
            return query;
        }
        
        attemptToApplySynonymsToQuery(query, solrParams, synonymAnalyzer);

        return query;
    }

    private void attemptToApplySynonymsToQuery(Query query, SolrParams solrParams, Analyzer synonymAnalyzer) {
        
        List<Query> synonymQueries = generateSynonymQueries(synonymAnalyzer, solrParams);
        
        boolean hasComplexQueryOperators = Const.COMPLEX_QUERY_OPERATORS_PATTERN.matcher(getString()).find();
        
        if (hasComplexQueryOperators // TODO: support complex operators
                || synonymQueries.size() < 2) { // found more than one synonym, i.e. not just the original phrase
            return;
        }
        
        // TODO: EDisMax does not do minShouldMatch if complex query operators exist, and neither do we.
        // But in the future we might, so keep doMinShouldMatch separate for now
        boolean doMinShouldMatch = true;
        String minShouldMatch = solrParams.get(DisMaxParams.MM, "100%");
        
        float originalBoost = solrParams.getFloat(Params.SYNONYMS_ORIGINAL_BOOST, 1.0F);
        float synonymBoost = solrParams.getFloat(Params.SYNONYMS_SYNONYM_BOOST, 1.0F);
        
        applySynonymQueries(query, synonymQueries, originalBoost, synonymBoost, doMinShouldMatch, minShouldMatch);
    }

    private void applySynonymQueries(Query query, List<Query> synonymQueries, float originalBoost, float synonymBoost,
            boolean doMinShouldMatch, String minShouldMatch) {
        // find the main query and its surrounding clause, make it SHOULD instead of MUST and append a bunch
        // of other SHOULDs to it, then wrap it in a MUST

        if (query instanceof BoostedQuery) {
            applySynonymQueries(((BoostedQuery) query).getQuery(), synonymQueries, originalBoost, synonymBoost, 
                    doMinShouldMatch, minShouldMatch);
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
                        if (doMinShouldMatch && synonymQuery instanceof BooleanQuery) {
                            U.setMinShouldMatch((BooleanQuery)synonymQuery, minShouldMatch);
                        }
                        allSynonymQueries.add(synonymQuery, Occur.SHOULD);
                    }
                    
                    allSynonymQueries.setBoost(synonymBoost);
                    
                    // now combine with the original main user query
                    BooleanQuery combinedQuery = new BooleanQuery();
                    combinedQuery.add(mainUserQuery, Occur.SHOULD);
                    combinedQuery.add(allSynonymQueries, Occur.SHOULD);
                    booleanClause.setQuery(combinedQuery);
                }
            }
        }
    }
    
    private BooleanQuery convertToBooleanQuery(Query query) {
        // wrap the query in a boolean query, if necessary
        if (query instanceof BooleanQuery) {
            return (BooleanQuery)query;
        }
        BooleanQuery result = new BooleanQuery();
        result.add(query, Occur.SHOULD);
        return result;
    }

    private List<Query> generateSynonymQueries(Analyzer synonymAnalyzer, SolrParams solrParams) {

        TokenStream tokenStream = synonymAnalyzer.tokenStream(Const.IMPOSSIBLE_FIELD_NAME, 
                new StringReader(getString()));

        // build up a list of alternate queries based on possible synonyms
        // for instance, if dog -> [hound, pooch], then "dog treats" would expand to
        // "hound treats" and "pooch treats"
        
        SortedMap<Integer, SortedSet<TextInQuery>> startPosToTextsInQuery = new TreeMap<Integer, SortedSet<TextInQuery>>();
        
        try {
            while (tokenStream.incrementToken()) {
                CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                TypeAttribute typeAttribute = tokenStream.getAttribute(TypeAttribute.class);
                
                if (!typeAttribute.type().equals("shingle")) { // ignore shingles
                    TextInQuery textInQuery = new TextInQuery(term.toString(), 
                            offsetAttribute.startOffset(), 
                            offsetAttribute.endOffset());
                    
                    SortedSet<TextInQuery> existingList = startPosToTextsInQuery.get(offsetAttribute.startOffset());
                    if (existingList == null) {
                        existingList = new TreeSet<TextInQuery>();
                        startPosToTextsInQuery.put(offsetAttribute.startOffset(), existingList);
                    }
                    existingList.add(textInQuery);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("uncaught exception in synonym processing", e);
        }
        
        List<List<TextInQuery>> sortedTextsInQuery = new ArrayList<List<TextInQuery>>(
                startPosToTextsInQuery.values().size());
        for (SortedSet<TextInQuery> sortedSet : startPosToTextsInQuery.values()) {
            sortedTextsInQuery.add(new ArrayList<TextInQuery>(sortedSet));
        }
        
        // have to use the start positions and end positions to figure out all possible combinations
        List<String> alternateQueries = buildUpAlternateQueries(sortedTextsInQuery);

        return createSynonymQueries(solrParams, alternateQueries);
    }

    private List<String> buildUpAlternateQueries(List<List<TextInQuery>> textsInQueryLists) {
        
        String originalUserQuery = getString();
        
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
                AlternateQuery alternateQuery = alternateQueries.get(j);
                
                boolean usedFirst = false;
                
                for (int k = 0;k < textsInQuery.size(); k++) {
                    TextInQuery textInQuery =  textsInQuery.get(k);
                    if (alternateQuery.getEndPosition() > textInQuery.getStartPosition()) { // cannot be appended
                        break; // already in order, so we can safely break
                    }
                    if (!usedFirst) {
                        // re-use the existing object
                        usedFirst = true;
                    } else {
                        // need to clone to a new object
                        alternateQuery = (AlternateQuery) alternateQuery.clone();
                        alternateQueries.add(alternateQuery);
                    }
                    // text in the original query between the two tokens, usually a space
                    CharSequence betweenTokens = originalUserQuery.subSequence(
                            alternateQuery.getEndPosition(), textInQuery.getStartPosition());
                    alternateQuery.getStringBuilder().append(betweenTokens).append(textInQuery.getText());
                    alternateQuery.setEndPosition(textInQuery.getEndPosition());
                }
            }
        }
        
        List<String> result = new ArrayList<String>();
        
        for (AlternateQuery alternateQuery : alternateQueries) {
            // append whatever text followed the last token, e.g. '"'
            alternateQuery.getStringBuilder().append(originalUserQuery.subSequence(
                    alternateQuery.getEndPosition(), originalUserQuery.length()));
            result.add(alternateQuery.getStringBuilder().toString());
        }
        return result;
    }

    private List<Query> createSynonymQueries(SolrParams solrParams, List<String> terms) {
        
        //
        // begin copied code from ExtendedDismaxQParser
        //
        float tiebreaker = solrParams.getFloat(DisMaxParams.TIE, 0.0f);
        int qslop = solrParams.getInt(DisMaxParams.QS, 0);
        ExtendedSolrQueryParser up = new ExtendedSolrQueryParser(this,
                Const.IMPOSSIBLE_FIELD_NAME);
        up.addAlias(Const.IMPOSSIBLE_FIELD_NAME, tiebreaker, queryFields);
        up.setPhraseSlop(qslop); // slop for explicit user phrase queries
        up.setAllowLeadingWildcard(true);
        //
        // end copied code
        //
        
        List<Query> result = new ArrayList<Query>();
        for (String term : terms) {
            try {
                result.add(up.parse(term));
            } catch (ParseException e) {
                // TODO: better error handling - for now just bail out; ignore this synonym
                e.printStackTrace(System.err);
            }
        }
        
        return result;
    }

}
