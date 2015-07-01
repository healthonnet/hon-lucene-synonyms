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

import java.util.Iterator;

import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SynonymExpandingExtendedDismaxQParserPlugin.Params;

import com.google.common.collect.ImmutableSet;

/**
 * Extends a set of solr params, hiding the boost-related parameters (bq, bf, boost).  This is useful
 * for constructing the synonym queries using the superclass, because we don't want to boost them artificially
 * 
 * @see issue 31
 * @author nolan
 *
 */
@SuppressWarnings("serial")
public class NoBoostSolrParams extends SolrParams {

    private static final ImmutableSet<String> BOOST_PARAMS = ImmutableSet.of(
            DisMaxParams.BQ, DisMaxParams.BF, Params.MULT_BOOST);
    
    private SolrParams delegateParams;
    
    private NoBoostSolrParams(SolrParams delegate) {
        this.delegateParams = delegate;
    }
    
    @Override
    public String get(String param) {
        if (param != null && param.equals(DisMaxParams.MM)) {
            if (delegateParams.getBool(Params.SYNONYMS_IGNORE_MM, false)) {
                return null;
            }
        }
        return delegateParams.get(param);
    }

    @Override
    public String[] getParams(String param) {
        if (param != null && BOOST_PARAMS.contains(param)) {
            return null;
        }
        return delegateParams.getParams(param);
    }

    @Override
    public Iterator<String> getParameterNamesIterator() {
        return delegateParams.getParameterNamesIterator();
    }
    
    public static NoBoostSolrParams wrap(SolrParams delegateParams) {
        return delegateParams == null ? null : new NoBoostSolrParams(delegateParams);
    }
}