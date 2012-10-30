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
import java.util.List;
import java.util.Map;
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
import org.apache.solr.analysis.ShingleFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.SynonymFilterFactory;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.ResourceLoader;
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

    private NamedList args;
    private Map<String, Analyzer> synonymAnalyzers;

    public void init(NamedList args) {
        this.args = args;
    }

    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new SynonymExpandingExtendedDismaxQParser(qstr, localParams, params, req,
                synonymAnalyzers.get("text_en"));
    }

    public void inform(ResourceLoader loader) {
        // TODO Auto-generated method stub
        synonymAnalyzers = new HashMap<String, Analyzer>();

        SynonymFilterFactory synonymFilterFactory = new SynonymFilterFactory();

        Map<String, String> params = new HashMap<String, String>();
        params.put("tokenizerFactory", "solr.KeywordTokenizerFactory");
        params.put("synonyms", "mesh_synonyms_en.txt");
        params.put("expand", "true");
        params.put("ignoreCase", "true");
        params.put("luceneMatchVersion", "LUCENE_34"); // TODO

        synonymFilterFactory.init(params);
        synonymFilterFactory.inform(loader);
        
        ShingleFilterFactory shingleFilterFactory = new ShingleFilterFactory();
        Map<String, String> params3 = new HashMap<String, String>();
        params3.put("outputUnigramsIfNoShingles", "true");
        params3.put("outputUnigrams", "true");
        params3.put("maxShingleSize", "4");
        params3.put("minShingleSize", "2");
        params3.put("luceneMatchVersion", "LUCENE_34"); // TODO
        
        shingleFilterFactory.init(params3);

        // TODO Auto-generated method stub
        StandardTokenizerFactory standardTokenizerFactory = new StandardTokenizerFactory();
        Map<String, String> params2 = new HashMap<String, String>();
        params2.put("luceneMatchVersion", "LUCENE_34"); // TODO
        standardTokenizerFactory.init(params2);
        TokenizerChain analyzer = new TokenizerChain(standardTokenizerFactory, new TokenFilterFactory[] { 
                shingleFilterFactory,
                synonymFilterFactory });

        synonymAnalyzers.put("text_en", analyzer);
    }

}

/**
 * Convenience class for calling constants.
 * @author nolan
 *
 */
class CONST {
    /**
     * A field we can't ever find in any schema, so we can safely tell
     * DisjunctionMaxQueryParser to use it as our defaultField, and map aliases
     * from it to any field in our schema.
     */
    static final String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";
    
    static final Pattern NO_MINMATCH_PATTERN = Pattern.compile("\\b(?:OR|AND|-|\\+)\\b"); 
}

/** shorten the class references for utilities */
class U extends SolrPluginUtils {
  /* :NOOP */
}

class SynonymExpandingExtendedDismaxQParser extends ExtendedDismaxQParser {

    Analyzer synonymAnalyzer;

    public SynonymExpandingExtendedDismaxQParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req, Analyzer synonymAnalyzer) {
        super(qstr, localParams, params, req);
        this.synonymAnalyzer = synonymAnalyzer;
    }

    @Override
    public Query parse() throws ParseException {
        Query query = super.parse();
        
        SolrParams localParams = getLocalParams();
        SolrParams params = getParams();
        SolrParams solrParams = localParams == null ? params : new DefaultSolrParams(localParams, params);

        List<Query> synonymQueries = generateSynonymQueries(solrParams);
        
        String minShouldMatch = solrParams.get(DisMaxParams.MM, "100%");
        boolean doMinShouldMatch = !CONST.NO_MINMATCH_PATTERN.matcher(getString()).find();
        
        if (synonymQueries.size() > 1) { // found more than one synonym, i.e. not just the original phrase
            applySynonymQueries(query, synonymQueries, doMinShouldMatch, minShouldMatch);
        }

        return query;
    }

    private void applySynonymQueries(Query query, List<Query> synonymQueries, boolean doMinShouldMatch, String minShouldMatch) {
        // find the main query and its surrounding clause, make it SHOULD instead of MUST and append a bunch
        // of other SHOULDs to it, then wrap it in a MUST

        if (query instanceof BoostedQuery) {
            applySynonymQueries(((BoostedQuery) query).getQuery(), synonymQueries, doMinShouldMatch, minShouldMatch);
        } else if (query instanceof BooleanQuery) {
            BooleanQuery booleanQuery =(BooleanQuery) query;
            
            for (BooleanClause booleanClause : booleanQuery.getClauses()) {
                if (Occur.MUST == booleanClause.getOccur()) {
                    // standard 'must occur' clause - i.e. the main user query    
                    BooleanQuery combinedQuery = new BooleanQuery();
                    Query mainUserQuery = booleanClause.getQuery();
                    mainUserQuery.setBoost(1.5F);
                    combinedQuery.add(mainUserQuery, Occur.SHOULD);
                    for (Query synonymQuery : synonymQueries) {
                        BooleanQuery booleanSynonymQuery = convertToBooleanQuery(synonymQuery);
                        U.setMinShouldMatch(booleanSynonymQuery, minShouldMatch);
                        booleanSynonymQuery.setBoost(1.2F); // TODO
                        combinedQuery.add(booleanSynonymQuery, Occur.SHOULD);
                    }
                    booleanClause.setQuery(combinedQuery);
                }
            }
        }
    }
    
    private BooleanQuery convertToBooleanQuery(Query query) {
        if (query instanceof BooleanQuery) {
            return (BooleanQuery)query;
        }
        BooleanQuery result = new BooleanQuery();
        result.add(query, Occur.SHOULD);
        return result;
    }

    private List<Query> generateSynonymQueries(SolrParams solrParams) throws ParseException {

        TokenStream tokenStream = synonymAnalyzer.tokenStream(CONST.IMPOSSIBLE_FIELD_NAME, 
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
        
        if (textsInQueryLists.isEmpty()) {
            return Collections.emptyList();
        }
        
        // initialize results
        List<AlternateQuery> alternateQueries = new ArrayList<AlternateQuery>();
        for (TextInQuery textInQuery : textsInQueryLists.get(0)) {
            alternateQueries.add(new AlternateQuery(
                    new StringBuilder(textInQuery.getText()), 
                    textInQuery.getEndPosition()));
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
                    alternateQuery.getStringBuilder().append(' ').append(textInQuery.getText());
                    alternateQuery.setEndPosition(textInQuery.getEndPosition());
                }
            }
        }
        
        List<String> result = new ArrayList<String>();
        
        for (AlternateQuery alternateQuery : alternateQueries) {
            result.add(alternateQuery.getStringBuilder().toString());
        }
        return result;
    }

    private List<Query> createSynonymQueries(SolrParams solrParams, List<String> terms) throws ParseException {
        
        // copied from ExtendedDismaxQParser
        float tiebreaker = solrParams.getFloat(DisMaxParams.TIE, 0.0f);
        int qslop = solrParams.getInt(DisMaxParams.QS, 0);
        ExtendedSolrQueryParser up = new ExtendedSolrQueryParser(this,
                CONST.IMPOSSIBLE_FIELD_NAME);
        up.addAlias(CONST.IMPOSSIBLE_FIELD_NAME, tiebreaker, queryFields);
        up.setPhraseSlop(qslop); // slop for explicit user phrase queries
        up.setAllowLeadingWildcard(true);
        
        List<Query> result = new ArrayList<Query>();
        for (String term : terms) {
            result.add(up.parse(term));
        }
        
        return result;
    }

}
