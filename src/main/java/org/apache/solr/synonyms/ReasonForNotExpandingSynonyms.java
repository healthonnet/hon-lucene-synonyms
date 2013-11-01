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
package org.apache.solr.synonyms;

import org.apache.solr.common.util.NamedList;

/**
 * Helpful enum used for debugging.  Lists all the reasons synonyms might be skipped.
 * @author nolan
 *
 */
public enum ReasonForNotExpandingSynonyms {
    
    PluginDisabled("You have to set synonyms=true to enable the plugin."),
    NoAnalyzerSpecified("You defined >1 synonym analyzer in your configuration, but you left synonyms.analyzer empty."),
    AnalyzerNotFound("There's no analyzer with the name you specified in synonyms.analyzer."),
    IgnoringPhrases("synonyms.disablePhraseQueries is set to true, and this query contains a phrase (\"like this\")"),
    UnhandledException("Whoops, we ran into an exception we couldn't handle!  File a bug."),
    HasComplexQueryOperators("synonyms.ignoreQueryOperators is set to true, and this query contains complex query "
            + "operators (e.g. AND, OR, *, -, etc.)"),
    DidntFindAnySynonyms("No synonyms found for this query.  Check your synonyms file."),
    ;
    
    private String explanation;
    
    private ReasonForNotExpandingSynonyms(String explanation) {
        this.explanation = explanation;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public NamedList<Object> toNamedList() {
        NamedList<Object> result = new NamedList<Object>();
        result.add("name", name());
        result.add("explanation", explanation);
        return result;
    }
    
}