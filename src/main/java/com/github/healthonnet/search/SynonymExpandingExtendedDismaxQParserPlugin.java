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
package com.github.healthonnet.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParser;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

import com.github.healthonnet.search.SynonymExpandingExtendedDismaxQParserPlugin.Const;
import com.github.healthonnet.search.SynonymExpandingExtendedDismaxQParserPlugin.Params;
import com.github.healthonnet.synonyms.AlternateQuery;
import com.github.healthonnet.synonyms.NoBoostSolrParams;
import com.github.healthonnet.synonyms.ReasonForNotExpandingSynonyms;
import com.github.healthonnet.synonyms.TextInQuery;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Main implementation of the synonym-expanding ExtendedDismaxQParser plugin for Solr. This parser was originally derived from ExtendedDismaxQParser, which itself was derived from the DismaxQParser from Solr.
 * 
 * @see <a href="https://github.com/healthonnet/hon-lucene-synonyms">https://github.com/healthonnet/hon-lucene-synonyms</a>
 */
public class SynonymExpandingExtendedDismaxQParserPlugin extends QParserPlugin implements ResourceLoaderAware {
    public static final String name = "synonym_edismax";

    /**
     * Convenience class for parameters
     */
    public static class Params {

        /**
         * @see <a href="https://cwiki.apache.org/confluence/display/solr/The+Extended+DisMax+Query+Parser">The Extended DisMax Query Parser</a>
         */
        public static String MULT_BOOST = "boost";

        public static final String SYNONYMS = "synonyms";
        public static final String SYNONYMS_ANALYZER = "synonyms.analyzer";
        public static final String SYNONYMS_DEFAULT_ANALYZER = "synonyms.default";
        public static final String SYNONYMS_ORIGINAL_BOOST = "synonyms.originalBoost";
        public static final String SYNONYMS_SYNONYM_BOOST = "synonyms.synonymBoost";
        public static final String SYNONYMS_DISABLE_PHRASE_QUERIES = "synonyms.disablePhraseQueries";
        public static final String SYNONYMS_CONSTRUCT_PHRASES = "synonyms.constructPhrases";
        public static final String SYNONYMS_IGNORE_QUERY_OPERATORS = "synonyms.ignoreQueryOperators";

        public static final String MAIN_PREANALYZIS = "synonyms.preanalyzis";
        public static final String MAIN_ANALYZER = "synonyms.preanalyzer";
        public static final String MAIN_DEFAULT_ANALYZER = "synonyms.defaultPreanalyzer";

        /**
         * instead of splicing synonyms into the original query string, ie dog bite canine familiaris bite dog chomp canine familiaris chomp do this: dog bite "canine familiaris" chomp with phrases off: dog bite canine familiaris chomp
         */
        public static final String SYNONYMS_BAG = "synonyms.bag";

        /**
         * if true, ignore mm param for the synonym query and use it only for the main query
         * 
         * @see org.apache.solr.common.params.DisMaxParams#MM
         */
        public static final String SYNONYMS_IGNORE_MM = "synonyms.ignoreMM";
    }

    /**
     * Convenience class for calling constants.
     * 
     * @author nolan
     */
    public static class Const {
        /**
         * A field we can't ever find in any schema, so we can safely tell DisjunctionMaxQueryParser to use it as our defaultField, and map aliases from it to any field in our schema.
         */
        static final String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";

        static final Pattern COMPLEX_QUERY_OPERATORS_PATTERN = Pattern.compile("(?:\\*|\\s-\\b|\\b(?:OR|AND|\\+)\\b)");
    }

    private NamedList<?> args;
    private Map<String, Analyzer> synonymAnalyzers;
    private Map<String, Analyzer> mainAnalyzers;
    private Version luceneMatchVersion = null;
    private SolrResourceLoader loader;

    @Override
    @SuppressWarnings("rawtypes")
    // TODO it would be nice if the user didn't have to encode tokenizers/filters
    // as a NamedList. But for now this is the hack I'm using
    public void init(NamedList args) {
        this.args = args;
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        if (luceneMatchVersion == null) {
            this.luceneMatchVersion = req.getCore().getSolrConfig().luceneMatchVersion;
            synonymAnalyzers = new HashMap<>();
            parseConfig(synonymAnalyzers, "synonymAnalyzers");
            mainAnalyzers = new HashMap<>();
            parseConfig(mainAnalyzers, "queryAnalyzers");

        }
        return new SynonymExpandingExtendedDismaxQParser(qstr, localParams, params, req, synonymAnalyzers, mainAnalyzers);
    }

    private Map<String, String> convertNamedListToMap(NamedList<?> namedList) {
        Map<String, String> result = new HashMap<>();

        for (Entry<String, ?> entry : namedList) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            }
        }

        return result;
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        // TODO: Can we assume that loader always is a sub type of SolrResourceLoader?
        this.loader = (SolrResourceLoader) loader;
    }

    /*
     * Expected call pattern: init(), inform(loader), createParser(), so we should now have config, loader and luceneMatchVersion needed for creating analyzer components
     */
    private void parseConfig(Map<String, Analyzer> analyzers, String argName) {
        try {

            Object xmlAnalyzers = args.get(argName);

            if (xmlAnalyzers != null && xmlAnalyzers instanceof NamedList) {
                NamedList<?> AnalyzersList = (NamedList<?>) xmlAnalyzers;
                for (Entry<String, ?> entry : AnalyzersList) {
                    String analyzerName = entry.getKey();
                    if (!(entry.getValue() instanceof NamedList)) {
                        continue;
                    }
                    NamedList<?> analyzerAsNamedList = (NamedList<?>) entry.getValue();

                    TokenizerFactory tokenizerFactory = null;
                    TokenFilterFactory filterFactory;
                    List<TokenFilterFactory> filterFactories = new LinkedList<>();

                    for (Entry<String, ?> analyzerEntry : analyzerAsNamedList) {
                        String key = analyzerEntry.getKey();
                        if (!(entry.getValue() instanceof NamedList)) {
                            continue;
                        }
                        Map<String, String> params = convertNamedListToMap((NamedList<?>) analyzerEntry.getValue());

                        String className = params.get("class");
                        if (className == null) {
                            continue;
                        }

                        params.put("luceneMatchVersion", luceneMatchVersion.toString());

                        if (key.equals("tokenizer")) {
                            try {
                                tokenizerFactory = TokenizerFactory.forName(className, params);
                            } catch (IllegalArgumentException iae) {
                                if (!className.contains(".")) {
                                    iae.printStackTrace();
                                }
                                // Now try by classname instead of SPI keyword
                                tokenizerFactory = loader.newInstance(className, TokenizerFactory.class, new String[] {}, new Class[] { Map.class }, new Object[] { params });
                            }
                            if (tokenizerFactory instanceof ResourceLoaderAware) {
                                ((ResourceLoaderAware) tokenizerFactory).inform(loader);
                            }
                        } else if (key.equals("filter")) {
                            try {
                                filterFactory = TokenFilterFactory.forName(className, params);
                            } catch (IllegalArgumentException iae) {
                                if (!className.contains(".")) {
                                    iae.printStackTrace();
                                }
                                // Now try by classname instead of SPI keyword
                                filterFactory = loader.newInstance(className, TokenFilterFactory.class, new String[] {}, new Class[] { Map.class }, new Object[] { params });
                            }
                            if (filterFactory instanceof ResourceLoaderAware) {
                                ((ResourceLoaderAware) filterFactory).inform(loader);
                            }
                            filterFactories.add(filterFactory);
                        }
                    }
                    if (tokenizerFactory == null) {
                        throw new SolrException(ErrorCode.SERVER_ERROR, "tokenizer must not be null for analyzer: " + analyzerName);
                    } else if (filterFactories.isEmpty()) {
                        throw new SolrException(ErrorCode.SERVER_ERROR, "filter factories must be defined for analyzer: " + analyzerName);
                    }

                    TokenizerChain analyzer = new TokenizerChain(tokenizerFactory, filterFactories.toArray(new TokenFilterFactory[filterFactories.size()]));

                    analyzers.put(analyzerName, analyzer);
                }
            }
        } catch (IOException e) {
            throw new SolrException(ErrorCode.SERVER_ERROR, "Failed to create parser. Check your config.", e);
        }
    }
}

class SynonymExpandingExtendedDismaxQParser extends QParser {

    // delegate all our parsing to these two parsers - one for the "synonym" query and the other for the main query
    private ExtendedDismaxQParser synonymQueryParser;
    private ExtendedDismaxQParser mainQueryParser;

    private Map<String, Analyzer> synonymAnalyzers;
    private Map<String, Analyzer> mainAnalyzers;
    private Analyzer mainAnalyzer = null;
    private Query queryToHighlight;

    /**
     * variables used purely for debugging
     */
    private List<String> expandedSynonyms;
    private ReasonForNotExpandingSynonyms reasonForNotExpandingSynonyms;
    private String parsedQuery = null;
    private String originalQuery = null;

    public SynonymExpandingExtendedDismaxQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, Map<String, Analyzer> synonymAnalyzers, Map<String, Analyzer> mainAnalyzers) {
        super(qstr, localParams, params, req);
        mainQueryParser = new ExtendedDismaxQParser(qstr, localParams, params, req);
        originalQuery = qstr;
        // ensure the synonyms aren't artificially boosted
        synonymQueryParser = new ExtendedDismaxQParser(qstr, NoBoostSolrParams.wrap(localParams), NoBoostSolrParams.wrap(params), req);
        this.synonymAnalyzers = synonymAnalyzers;
        this.mainAnalyzers = mainAnalyzers;
    }

    @Override
    public String[] getDefaultHighlightFields() {
        return mainQueryParser.getDefaultHighlightFields();
    }

    @Override
    public Query getHighlightQuery() throws SyntaxError {
        return queryToHighlight != null ? queryToHighlight : mainQueryParser.getHighlightQuery();
    }

    @Override
    public void addDebugInfo(NamedList<Object> debugInfo) {
        if (queryToHighlight != null) {
            debugInfo.add("queryToHighlight", queryToHighlight);
        }
        if (expandedSynonyms != null) {
            debugInfo.add("expandedSynonyms", Ordering.natural().nullsFirst().sortedCopy(expandedSynonyms));
        }
        if (reasonForNotExpandingSynonyms != null) {
            debugInfo.add("reasonForNotExpandingSynonyms", reasonForNotExpandingSynonyms.toNamedList());
        }
        debugInfo.add("originalQuery", originalQuery);
        if (parsedQuery != null)
            debugInfo.add("originalPreparsedQuery", parsedQuery);
        debugInfo.add("mainQueryParser", createDebugInfo(mainQueryParser));
        debugInfo.add("synonymQueryParser", createDebugInfo(synonymQueryParser));
    }

    @Override
    public Query parse() throws SyntaxError {

        SolrParams localParams = getLocalParams();
        SolrParams params = getParams();
        SolrParams solrParams = localParams == null ? params : SolrParams.wrapDefaults(localParams, params);

        String defmainAnalyser = solrParams.get(Params.MAIN_DEFAULT_ANALYZER, null);
        String defSynonymsAnalyser = solrParams.get(Params.SYNONYMS_DEFAULT_ANALYZER, null);

        Boolean preanalyzis = solrParams.getBool(Params.MAIN_PREANALYZIS, false);

        // check to make sure the analyzer exists
        if (preanalyzis) {
            String preAnalyzerName = solrParams.get(Params.MAIN_ANALYZER, null);
            if (preAnalyzerName == null) { // no query analyzer specified
                if (defmainAnalyser != null && defmainAnalyser.length() > 0) {
                    preAnalyzerName = defmainAnalyser;
                } else {
                    if (mainAnalyzers.size() >= 1) {
                        // only one analyzer defined; just use that one
                        preAnalyzerName = mainAnalyzers.keySet().iterator().next();
                    }
                }
            }

            if (preAnalyzerName != null) {

                mainAnalyzer = mainAnalyzers.get(preAnalyzerName);

                analyzeMainQuery(mainAnalyzer);
            } else
                mainAnalyzer = null;
        } else {
            mainAnalyzer = null;
        }

        Query query = mainQueryParser.parse();

        // disable/enable synonym handling altogether
        if (!solrParams.getBool(Params.SYNONYMS, false)) {
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.PluginDisabled;
            return query;
        }

        // check to make sure the analyzer exists
        String analyzerName = solrParams.get(Params.SYNONYMS_ANALYZER, null);
        if (analyzerName == null) { // no synonym analyzer specified
            if (defSynonymsAnalyser != null && defSynonymsAnalyser.length() > 0) {
                analyzerName = defSynonymsAnalyser;
            } else {
                if (synonymAnalyzers.size() >= 1) {
                    // only one analyzer defined; just use that one
                    analyzerName = synonymAnalyzers.keySet().iterator().next();
                } else {
                    reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.NoAnalyzerSpecified;
                    return query;
                }
            }
        }

        Analyzer synonymAnalyzer = synonymAnalyzers.get(analyzerName);

        if (synonymAnalyzer == null) { // couldn't find analyzer
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.AnalyzerNotFound;
            return query;
        }

        if (solrParams.getBool(Params.SYNONYMS_DISABLE_PHRASE_QUERIES, false) && getQueryStringFromParser().indexOf('"') != -1) {
            // disable if a phrase query is detected, i.e. there's a '"'
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.IgnoringPhrases;
            return query;
        }

        try {
            query = attemptToApplySynonymsToQuery(query, solrParams, synonymAnalyzer);
        } catch (IOException e) {
            // TODO: better error handling - for now just bail out
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.UnhandledException;
            e.printStackTrace(System.err);
        }

        return query;
    }

    private void analyzeMainQuery(Analyzer analyzer) {

        String newQuery = analyzeQuery(getString(), analyzer);

        parsedQuery = newQuery;
        this.mainQueryParser.setString(newQuery);
        this.synonymQueryParser.setString(newQuery);
        this.setString(newQuery);

    }

    private String analyzeQuery(String query, Analyzer analyzer) {

        if (analyzer != null && query != null && query.length() > 0) {
            TokenStream tokenStream = analyzer.tokenStream(Const.IMPOSSIBLE_FIELD_NAME, new StringReader(query));

            StringBuilder newQueryB = new StringBuilder();
            try {
                tokenStream.reset();
                while (tokenStream.incrementToken()) {
                    CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);
                    // OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                    // TypeAttribute typeAttribute = tokenStream.getAttribute(TypeAttribute.class);

                    newQueryB.append(term.toString());
                    newQueryB.append(' ');

                }
                tokenStream.end();
                return newQueryB.toString().trim();

            } catch (IOException e) {
                throw new RuntimeException("uncaught exception in synonym processing", e);
            } finally {
                try {
                    tokenStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("uncaught exception in synonym processing", e);
                }
            }
        }

        return query;

    }

    private Query attemptToApplySynonymsToQuery(Query query, SolrParams solrParams, Analyzer synonymAnalyzer) throws IOException {

        List<Query> synonymQueries = generateSynonymQueries(synonymAnalyzer, solrParams);

        boolean ignoreQueryOperators = solrParams.getBool(Params.SYNONYMS_IGNORE_QUERY_OPERATORS, false);
        boolean hasComplexQueryOperators = ignoreQueryOperators ? false : Const.COMPLEX_QUERY_OPERATORS_PATTERN.matcher(getQueryStringFromParser()).find();

        if (hasComplexQueryOperators) { // TODO: support complex operators
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.HasComplexQueryOperators;
            return query;
        } else if (synonymQueries.isEmpty()) { // didn't find more than 0 synonyms, i.e. it's just the original phrase
            reasonForNotExpandingSynonyms = ReasonForNotExpandingSynonyms.DidntFindAnySynonyms;
            return query;
        }

        float originalBoost = solrParams.getFloat(Params.SYNONYMS_ORIGINAL_BOOST, 1.0F);
        float synonymBoost = solrParams.getFloat(Params.SYNONYMS_SYNONYM_BOOST, 1.0F);

        query = applySynonymQueries(query, synonymQueries, originalBoost, synonymBoost);
        return query;
    }

    /**
     * Find the main query and its surrounding clause, make it SHOULD instead of MUST and append a bunch of other SHOULDs to it, then wrap it in a MUST E.g. +(text:dog) becomes +((text:dog)^1.5 ((text:hound) (text:pooch))^1.2)
     * 
     * @param query
     * @param synonymQueries
     * @param originalBoost
     * @param synonymBoost
     */
    private Query applySynonymQueries(Query query, List<Query> synonymQueries, float originalBoost, float synonymBoost) {
        if (query instanceof BoostedQuery) {
            return applySynonymQueries(((BoostedQuery) query).getQuery(), synonymQueries, originalBoost, synonymBoost);
        } else if (query instanceof BooleanQuery) {
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for (BooleanClause booleanClause : ((BooleanQuery) query).clauses()) {
                if (Occur.MUST == booleanClause.getOccur()) {
                    BooleanQuery.Builder combinedQueryBuilder = new BooleanQuery.Builder();
                    combinedQueryBuilder.add(new BoostQuery(booleanClause.getQuery(), originalBoost), Occur.SHOULD);
                    // standard 'must occur' clause - i.e. the main user query

                    for (Query synonymQuery : synonymQueries) {
                        if (synonymQuery != null) {
                            BooleanQuery.Builder booleanSynonymQueryBuilder = new BooleanQuery.Builder();
                            booleanSynonymQueryBuilder.add(new BoostQuery(synonymQuery, synonymBoost), Occur.SHOULD);
                            combinedQueryBuilder.add(booleanSynonymQueryBuilder.build(), Occur.SHOULD);
                        }
                    }
                    booleanQueryBuilder.add(combinedQueryBuilder.build(), Occur.MUST);
                } else {
                    booleanQueryBuilder.add(booleanClause);
                }
            }
            query = booleanQueryBuilder.build();
            queryToHighlight = query;
        }
        return query;
    }

    /**
     * Given the synonymAnalyzer, returns a list of all alternate queries expanded from the original user query.
     * 
     * @param synonymAnalyzer
     * @param solrParams
     * @return
     */
    private List<Query> generateSynonymQueries(Analyzer synonymAnalyzer, SolrParams solrParams) {

        String origQuery = getQueryStringFromParser();
        int queryLen = origQuery.length();

        // TODO: make the token stream reusable?
        TokenStream tokenStream = synonymAnalyzer.tokenStream(Const.IMPOSSIBLE_FIELD_NAME, new StringReader(origQuery));

        SortedSetMultimap<Integer, TextInQuery> startPosToTextsInQuery = TreeMultimap.create();

        boolean constructPhraseQueries = solrParams.getBool(Params.SYNONYMS_CONSTRUCT_PHRASES, false);

        boolean bag = solrParams.getBool(Params.SYNONYMS_BAG, false);
        List<String> synonymBag = new ArrayList<>();

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

                    if (typeAttribute.type().equals("SYNONYM")) {
                        synonymBag.add(termToAdd);
                    }

                    // Don't quote sibgle term term synonyms
                    if (constructPhraseQueries && typeAttribute.type().equals("SYNONYM") && termToAdd.contains(" ")) {
                        // Don't Quote when original is already surrounded by quotes
                        if (offsetAttribute.startOffset() == 0 || offsetAttribute.endOffset() == queryLen || origQuery.charAt(offsetAttribute.startOffset() - 1) != '"' || origQuery.charAt(offsetAttribute.endOffset()) != '"') {
                            // make a phrase out of the synonym
                            termToAdd = new StringBuilder(termToAdd).insert(0, '"').append('"').toString();
                        }
                    }
                    if (!bag) {
                        // create a graph of all possible synonym combinations,
                        // e.g. dog bite, hound bite, dog nibble, hound nibble, etc.
                        TextInQuery textInQuery = new TextInQuery(termToAdd, offsetAttribute.startOffset(), offsetAttribute.endOffset());

                        startPosToTextsInQuery.put(offsetAttribute.startOffset(), textInQuery);
                    }
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

        List<String> alternateQueries = synonymBag;

        if (!bag) {
            // use a graph rather than a bag
            List<List<TextInQuery>> sortedTextsInQuery = new ArrayList<>(startPosToTextsInQuery.values().size());
            sortedTextsInQuery.addAll(startPosToTextsInQuery.asMap().values().stream().map(ArrayList::new).collect(Collectors.toList()));

            // have to use the start positions and end positions to figure out all possible combinations
            alternateQueries = buildUpAlternateQueries(solrParams, sortedTextsInQuery);
        }

        // save for debugging purposes
        expandedSynonyms = alternateQueries;

        return createSynonymQueries(solrParams, alternateQueries);
    }

    /**
     * From a list of texts in the original query that were deemed to be interested (i.e. synonyms or the original text itself), build up all possible alternate queries as strings. For instance, if the query is "dog bite" and the synonyms are dog -> [dog,hound,pooch] and bite -> [bite,nibble], then the result will be: dog bite hound bite pooch bite dog nibble hound nibble pooch nibble
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
        List<AlternateQuery> alternateQueries = new ArrayList<>();
        for (TextInQuery textInQuery : textsInQueryLists.get(0)) {
            // add the text before the first user query token, e.g. a space or a "
            StringBuilder stringBuilder = new StringBuilder(originalUserQuery.subSequence(0, textInQuery.getStartPosition())).append(textInQuery.getText());
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
                AlternateQuery originalAlternateQuery = alternateQueries.get(j);

                boolean usedFirst = false;

                for (int k = 0; k < textsInQuery.size(); k++) {

                    TextInQuery textInQuery = textsInQuery.get(k);
                    if (originalAlternateQuery.getEndPosition() > textInQuery.getStartPosition()) {
                        // cannot be appended, e.g. "canis" token in "canis familiaris"
                        continue;
                    }

                    AlternateQuery currentAlternateQuery;

                    if (!usedFirst) {
                        // re-use the existing object
                        usedFirst = true;
                        currentAlternateQuery = originalAlternateQuery;

                        if (k < textsInQuery.size() - 1) {
                            // make a defensive clone for future usage
                            originalAlternateQuery = (AlternateQuery) currentAlternateQuery.clone();
                        }
                    } else if (k == textsInQuery.size() - 1) {
                        // we're sure we're the last one to use it, so we can just use the original clone
                        currentAlternateQuery = originalAlternateQuery;
                        alternateQueries.add(currentAlternateQuery);
                    } else {
                        // need to clone to a new object
                        currentAlternateQuery = (AlternateQuery) originalAlternateQuery.clone();
                        alternateQueries.add(currentAlternateQuery);
                    }
                    // text in the original query between the two tokens, usually a space, comma, etc.
                    CharSequence betweenTokens = originalUserQuery.subSequence(currentAlternateQuery.getEndPosition(), textInQuery.getStartPosition());
                    currentAlternateQuery.getStringBuilder().append(betweenTokens).append(textInQuery.getText());
                    currentAlternateQuery.setEndPosition(textInQuery.getEndPosition());
                }
            }
        }

        // Make sure result is unique
        HashSet<String> result = new LinkedHashSet<>();

        for (AlternateQuery alternateQuery : alternateQueries) {

            StringBuilder sb = alternateQuery.getStringBuilder();

            // append whatever text followed the last token, e.g. '"'
            sb.append(originalUserQuery.subSequence(alternateQuery.getEndPosition(), originalUserQuery.length()));

            result.add(sb.toString());
        }
        return new ArrayList<>(result);
    }

    /**
     * From a list of alternate queries in text format, parse them using the default ExtendedSolrQueryParser and return the queries.
     * 
     * @param solrParams
     * @param alternateQueryTexts
     * @return
     */
    private List<Query> createSynonymQueries(SolrParams solrParams, List<String> alternateQueryTexts) {

        String nullsafeOriginalString = getQueryStringFromParser();

        List<Query> result = new ArrayList<>();
        for (String alternateQueryText : alternateQueryTexts) {
            if (alternateQueryText.equalsIgnoreCase(nullsafeOriginalString)) {
                // alternate query is the same as what the user entered
                continue;
            }

            synonymQueryParser.setString(analyzeQuery(alternateQueryText, mainAnalyzer));
            try {
                result.add(synonymQueryParser.parse());
            } catch (SyntaxError e) {
                // TODO: better error handling - for now just bail out; ignore this synonym
                e.printStackTrace(System.err);
            }
        }

        return result;
    }

    /**
     * Ensures that we return a valid string, even if null
     * 
     * @return the entered query string fetched from QParser.getString()
     */
    private String getQueryStringFromParser() {
        return (getString() == null) ? "" : getString();
    }

    /**
     * Convenience method to simplify code
     * 
     * @param qparser
     * @return
     */
    private static NamedList<Object> createDebugInfo(QParser qparser) {
        NamedList<Object> result = new NamedList<>();
        qparser.addDebugInfo(result);
        return result;
    }

}
