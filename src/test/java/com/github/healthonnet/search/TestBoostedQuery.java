/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.healthonnet.search;

import org.junit.Test;

public class TestBoostedQuery extends HonLuceneSynonymTestCase {

    public TestBoostedQuery() {
        defaultDocs = new String[][]{
                {"id", "1", "name", "I have a backpack"},
                {"id", "2", "name", "I have a back pack"},
                {"id", "3", "name", "I have a house"},
                {"id", "4", "name", "I have a car"}
        };
        defaultRequest = new String[]{
                "fl", "*,score",
                "qf", "name",
                "defType", "synonym_edismax",
                "synonyms", "true",
                "debugQuery", "on",
                "bf", "10"
        };
        commitDocs();
    }

    @Test
    public void queries() {
        assertQuery("back pack", 2); // no boost

        assertQuery("back pack", 2, "boost", "numdocs()"); // boost applied

        assertQuery("back pack", 2, // a combination of boosts applied
                "boost", "numdocs()",
                "boost", "recip(ms(NOW/HOUR,last_modified),3.16e-11,1,1)");

        // doc having the term 'back' goes first
        assertQuery(new String[]{
                "//result/doc[1]/str[@name='id'][.='2']",
                "//result/doc[2]/str[@name='id'][.='1']"
        }, "back pack", 2, "boost", "if(termfreq(name,'back'),1,0)");

        // doc having the term 'backpack' goes first
        assertQuery(new String[]{
                "//result/doc[1]/str[@name='id'][.='1']",
                "//result/doc[2]/str[@name='id'][.='2']"
        }, "back pack", 2, "boost", "if(termfreq(name,'backpack'),1,0)");
    }

}