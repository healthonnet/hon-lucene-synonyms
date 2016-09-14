Lucene/Solr Synonym-Expanding EDisMax Parser [![Build Status](https://travis-ci.org/healthonnet/hon-lucene-synonyms.svg?branch=master)](https://travis-ci.org/healthonnet/hon-lucene-synonyms)
=========================

Current version : 5.0.5 ([changelog][15])

Maintainer
-----------

[Nolan Lawson][7]

[Health On the Net Foundation][6]

[Jan Høydahl][23]

License
-----------

[Apache 2.0][1].

Summary
-----------

Extension of the [ExtendedDisMaxQueryParserPlugin][3] that splits queries into a "normal" query and a "synonym" query. This enables proper query-time synonym expansion, with no reindexing required.

This also fixes lots of bugs with how Solr typically handles synonyms using the [SynonymFilterFactory][4].

For more details, read [my blog post on the subject][2].

Getting Started
----------------

The following tutorial will set up a working synonym-enabled Solr app using the techproducts sample project from Solr itself.

### Step 1

Download the latest JAR file:

  * [hon-lucene-synonyms-5.0.5.jar](http://central.maven.org/maven2/com/github/healthonnet/hon-lucene-synonyms/5.0.5/hon-lucene-synonyms-5.0.5.jar)

Or use Maven:

    mvn dependency:copy -Dartifact=com.github.healthonnet:hon-lucene-synonyms:5.0.5 -DoutputDirectory=.

```
<dependency>
    <groupId>com.github.healthonnet</groupId>
    <artifactId>hon-lucene-synonyms</artifactId>
    <version>5.0.5</version>
</dependency>
```

Or if you are using an older version of Solr, then you can use the last version of this plugin to support older Solr versions (1.3.5):

<table border="0" style="border-width:1px;border-color:#999999;border-collapse:collapse;border-style:solid;">
<tr style="background:gray;color:white;">
<td style="padding:0 1em;" align="center"><strong>JAR</strong></td>
<td style="padding:0 1em;" align="center"><strong>Solr</strong></td>
</tr>
<tr>
<td style="padding:0 1em;">
<a href='https://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.3.5-solr-3.x/hon-lucene-synonyms-1.3.5-solr-3.x.jar'>
hon-lucene-synonyms-1.3.5-solr-3.x.jar
</a>
</td>
<td style="padding:0 1em;">3.4.0, 3.5.0, and 3.6.x</td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;">
<a href='https://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.3.5-solr-4.0.0/hon-lucene-synonyms-1.3.5-solr-4.0.0.jar'>
hon-lucene-synonyms-1.3.5-solr-4.0.0.jar
</a>
</td>
<td style="padding:0 1em;">4.0.0</td>
</tr>
<tr>
<td style="padding:0 1em;">
<a href='https://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.3.5-solr-4.1.0/hon-lucene-synonyms-1.3.5-solr-4.1.0.jar'>
hon-lucene-synonyms-1.3.5-solr-4.1.0.jar
</a>
</td>
<td style="padding:0 1em;">4.1.0 and 4.2.x</td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;">
<a href='https://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.3.5-solr-4.3.0/hon-lucene-synonyms-1.3.5-solr-4.3.0.jar'>
hon-lucene-synonyms-1.3.5-solr-4.3.0.jar
</a>
</td>
<td style="padding:0 1em;">4.3+</td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;">
<a href='https://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/2.0.0/hon-lucene-synonyms-2.0.0.jar'>
hon-lucene-synonyms-2.0.0.jar
</a>
</td>
<td style="padding:0 1em;">5.3.1</td>
</tr>
</table>

### Step 2

Download Solr from [the Solr home page][8].  For this tutorial, we'll use [Solr 6.2.0][9].  You do not need
the sources; the `tgz` or `zip` file will work fine.

### Step 3

Extract the compressed file.

### Step 4

Copy `hon-lucene-synonyms-*.jar` file into `solr-6.2.0/server/solr-webapp/webapp/WEB-INF/lib/`.

    cp hon-lucene-synonyms-*.jar solr-6.2.0/server/solr-webapp/webapp/WEB-INF/lib/

Note that the jar may be placed in other locations if Solr is configured properly. The following tips are primarily valid only in Solr stand-alone and Solr Master/Slave configurations. The [Solr Plugins][21] section of the Solr CWIKI has more details about running plugins on Solr.

  * A collection or core can be configured to use the [Lib Directives in SolrConfig][19].
  * A Solr server itself can be configured to use `sharedLib` directive in [solr.xml][20].
  
If you want to configure the plugin for SolrCloud check out [Adding Custom Plugins in SolrCloud Mode][22].

### Step 5

Download [example_synonym_file.txt][5] and copy it to the `solr-6.2.0/server/solr/configsets/sample_techproducts_configs/conf/` directory.

### Step 6

Download [example_config.xml][18] and copy the `<queryParser>...</queryParser>` section into `solr-6.2.0/server/solr/configsets/sample_techproducts_configs/conf/solrconfig.xml` just before the ```</config>``` tag at the end.

This defines the analyzer that will be used to generate synonyms.

**Protip**: You can customize this analyzer based on your synonym set.  E.g. if your synonyms are all two words or less, you can safely set ```maxShingleSize``` to 2.

### Step 7

Start up the app by running `solr-6.2.0/bin/solr -e techproducts`.

### Step 8

In your browser, navigate to 

[```http://localhost:8983/solr/techproducts/select/?q=dog&debugQuery=on&qf=text&defType=synonym_edismax&synonyms=true```](http://localhost:8983/solr/techproducts/select/?q=dog&debugQuery=on&qf=text&defType=synonym_edismax&synonyms=true)

You should see a response like this:

```xml
<response>
  ...
<lst name="debug">
  <str name="rawquerystring">dog</str>
  <str name="querystring">dog</str>
  <str name="parsedquery">(DisjunctionMaxQuery((text:dog))^1.0 ((+(DisjunctionMaxQuery((text:canis)) DisjunctionMaxQuery((text:familiaris))))/no_coord^1.0) ((+DisjunctionMaxQuery((text:hound)))/no_coord^1.0) ((+(DisjunctionMaxQuery((text:man's)) DisjunctionMaxQuery((text:best)) DisjunctionMaxQuery((text:friend))))/no_coord^1.0) ((+DisjunctionMaxQuery((text:pooch)))/no_coord^1.0))</str>
  <str name="parsedquery_toString">(((text:dog))^1.0 ((+((text:canis) (text:familiaris)))^1.0) ((+(text:hound))^1.0) ((+((text:man's) (text:best) (text:friend)))^1.0) ((+(text:pooch))^1.0))</str>
  <lst name="explain"/>
  <arr name="queryToHighlight">
    <str>org.apache.lucene.search.BooleanClause:((text:dog))^1.0 ((+((text:canis) (text:familiaris)))^1.0) ((+(text:hound))^1.0) ((+((text:man's) (text:best) (text:friend)))^1.0) ((+(text:pooch))^1.0)</str>
  </arr>
  <arr name="expandedSynonyms">
    <str>canis familiaris</str>
    <str>dog</str>
    <str>hound</str>
    <str>man's best friend</str>
    <str>pooch</str>
  </arr>
  <lst name="mainQueryParser">
    <str name="QParser">ExtendedDismaxQParser</str>
    <null name="altquerystring"/>
    <null name="boost_queries"/>
    <arr name="parsed_boost_queries"/>
    <null name="boostfuncs"/>
  </lst>
  <lst name="synonymQueryParser">
    <str name="QParser">ExtendedDismaxQParser</str>
    <null name="altquerystring"/>
    <null name="boost_queries"/>
    <arr name="parsed_boost_queries"/>
    <null name="boostfuncs"/>
  </lst>
  ...
</lst>
</response>
```

Note that the input query `dog` has been expanded into `dog`, `hound`, `pooch`, `canis familiaris`, and `man's best friend`.

Tweaking the results
---------------------

Boost the non-synonym part to 1.2 and the synonym part to 1.1 by adding `synonyms.originalBoost=1.2&synonyms.synonymBoost=1.1`:

```
(((text:dog))^1.2 
((+((text:canis) (text:familiaris)))^1.1) 
((+(text:hound))^1.1) 
((+((text:man's) (text:best) (text:friend)))^1.1) 
((+(text:pooch))^1.1))
```

Apply a [minimum "should" match][16] of 75% by adding `mm=75%25`:

```
(((text:dog))^1.0 
((+(((text:canis) (text:familiaris))~1))^1.0) 
((+(text:hound))^1.0) 
((+(((text:man's) (text:best) (text:friend))~2))^1.0) 
((+(text:pooch))^1.0))
```

Observe how phrase queries are properly handled by using `q="dog"` instead of `q=dog`:

```
(((text:dog))^1.0 
((+(text:"canis familiaris"))^1.0) 
((+(text:hound))^1.0) 
((+(text:"man's best friend"))^1.0) 
((+(text:pooch))^1.0))
```


Gotchas
---------

Keep in mind that you must add ```defType=synonym_edismax``` and ```synonyms=true``` to enable 
the parser in the first place.

Also, you must either define ```qf``` in the query parameters or ```defaultSearchField``` in ```solr/conf/schema.xml```,
so that the parser knows which fields to use during synonym expansion.

If you enable debugging (with ```debugQuery=on```), the plugin will output helpful information about
how synonyms are being expanded.

Query parameters
------------

The following are parameters that you can use to tweak the synonym expansion.

<table border="0" style="border-width:1px;border-color:#999999;border-collapse:collapse;border-style:solid;">
<tr style="background:gray;color:white;">
<td style="padding:0 1em;" align="center"><strong>Param</strong></td>
<td style="padding:0 1em;" align="center"><strong>Type</strong></td>
<td style="padding:0 1em;" align="center"><strong>Default</strong></td>
<td style="padding:0 1em;" align="center"><strong>Summary</strong></td>
</tr>
<tr>
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1">Enable or disable synonym expansion entirely. True if enabled.</font></td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.analyzer</font></strong></td>
<td style="padding:0 1em;"><font size="-1">String</font></td>
<td style="padding:0 1em;"><font size="-1">null</font></td>
<td style="padding:0 1em;"><font size="-1">Name of the analyzer defined in <font face="monospace">solrconfig.xml</font> to use. (E.g. in the examples, it's <font face="monospace">myCoolAnalyzer</font>). This <em>must</em> be non-null, if you define more than one analyzer (e.g. for more than one language).</font></td>
</tr>
<tr>
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.originalBoost</font></strong></td>
<td style="padding:0 1em;"><font size="-1">float</font></td>
<td style="padding:0 1em;"><font size="-1">1.0</font></td>
<td style="padding:0 1em;"><font size="-1">Boost value applied to the original (non-synonym) part of the query.</font></td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.synonymBoost</font></strong></td>
<td style="padding:0 1em;"><font size="-1">float</font></td>
<td style="padding:0 1em;"><font size="-1">1.0</font></td>
<td style="padding:0 1em;"><font size="-1">Boost value applied to the synonym part of the query.</font></td>
</tr>
<tr>
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.disablePhraseQueries</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1">True if synonym expansion should be disabled when the user input contains a phrase query (i.e. a quoted query). This option is offered because expansion of phrase queries may be considered non-intuitive to users.</font></td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.constructPhrases</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1"><strong>v1.2.2+:</strong> True if expanded synonyms should always be treated like phrases (i.e. wrapped in quotes).  This option is offered in case your synonyms contain lots of phrases composed of common words (e.g. "man's best friend" for "dog").  Only affects the expanded synonyms; not the original query. See <a href='http://github.com/healthonnet/hon-lucene-synonyms/issues/5'>issue #5</a> for more discussion.</font></td>
</tr>
<tr>
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.ignoreQueryOperators</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1"><strong>v1.3.2+:</strong> If you treat query operators (e.g. AND and OR) as usual words and want the synonyms be added to the query anyhow, set this option to true.</font></td>
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.bag</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1"><strong>v1.3.2+:</strong> When false (default), this plugin generates additional synonym queries by using the original query string as a template: dog bite => dog bite, canis familiaris bite, dog chomp, canis familiaris chomp. When true a simpler, "bag of terms" query is created from the synonyms. IE dog bite => bite dog chomp canis familiaris. The simpler query will be more performant but loses positional information. Use with synonyms.constructPhrases to keep synonym phrases such as "canis familiaris".
</tr>
<tr style="background:#DDDDDD;">
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.ignoreMM</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1"><strong>v1.3.5+:</strong> When false (default), the <strong>mm</strong> param is applied to the original query and to the synonym queries. When true <strong>mm</strong> is ignored for the synonym queries and applied only to the original query.
</tr>
</table>



Compile it yourself
----------

Download the code and run:

```
mvn install
```

Testing
---------

Run the tests using maven:

```
mvn test
```

Changelog
------------
* 5.0.5
  * Tested with Solr 6.2.0
  * Fixed [#65][165]  Matches all docs if bf (Boost Function) present @janhoy
* 5.0.4
  * Solr 6.0.0 support.
  * Distributed on Maven central now.
* 2.0.0
  * **BREAKING CHANGE:** Updated to support Solr 5.3.1. Removed support for older versions of Solr.
  * Note that as of Lucene 5.2.0, when synonyms are parsed, original terms are now correctly marked as type `word` instead of type `synonym` [LUCENE-6400](https://issues.apache.org/jira/browse/LUCENE-6400).
* v1.3.5
  * Added ```synonyms.ignoreMM``` option
* v1.3.4
  * Fixed [#41][141] thanks to [@rpialum](https://github.com/rpialum).
* v1.3.3
  * Fixed [#33][133]: synonyms are now weighted equally, regardless of how many there are per word.
  * Fixed [#31][131]: synonyms are no longer given extra weight when using the params ```bq```, ```bf```, and ```boost```.
  * ```debugQuery=on``` now gives more helpful debug output.
  * Fixed [#9][109], [#26][126], [#32][132], and [#34][134].
    <em>Note that this is a documentation change; not a code change, so to get
    the benefits of this "fix," you'll need to manually perform [Step 6](#step-6) again.</em>
* v1.3.2
  * Added ```synonyms.ignoreQueryOperators``` option ([#28][128])
  * Added ```synonyms.bag``` option ([#30][130])
  * The ```run_solr_for_unit_tests.py``` script now downloads the proper version of Solr.
* v1.3.1
  * Avoid luceneMatchVersion in config ([#20][120])
* v1.3.0
  * Added support for Solr 4.3.0 ([#19][119])
  * New way of loading Tokenizers and TokenFilters
  * New XML syntax for config in solrconfig.xml
* v1.2.3
  * Fixed [#16][116]
  * Verified support for Solr 4.2.0 with the 4.1.0 branch (unit tests passed)
  * Improved automation of unit tests
* v1.2.2
  * Added ```synonyms.constructPhrases``` option to fix [#5][105]
  * Added proper handling for phrase slop settings
* v1.2.1
  * Added support for Solr 4.1.0 ([#4][104])
* v1.2
  * Added support for Solr 4.0.0 ([#3][103])
* v1.1
  * Added support for Solr 3.6.1 and 3.6.2 ([#1][101])
  * Added "Getting Started" instructions to clarify plugin usage ([#2][102])
* v1.0
  * Initial release

[1]: http://www.apache.org/licenses/LICENSE-2.0.html
[2]: http://nolanlawson.com/2012/10/31/better-synonym-handling-in-solr
[3]: https://cwiki.apache.org/confluence/display/solr/The+Extended+DisMax+Query+Parser
[4]: https://cwiki.apache.org/confluence/display/solr/Filter+Descriptions#FilterDescriptions-SynonymFilter
[5]: https://github.com/healthonnet/hon-lucene-synonyms/raw/master/src/test/resources/solr/collection1/conf/example_synonym_file.txt
[6]: http://www.healthonnet.org
[7]: http://nolanlawson.com
[8]: http://lucene.apache.org/solr/
[9]: http://www.apache.org/dyn/closer.cgi/lucene/solr/6.2.0
[15]: https://github.com/healthonnet/hon-lucene-synonyms#changelog
[16]: https://cwiki.apache.org/confluence/display/solr/The+DisMax+Query+Parser#TheDisMaxQueryParser-Themm(MinimumShouldMatch)Parameter
[18]: https://github.com/healthonnet/hon-lucene-synonyms/raw/master/src/test/resources/solr/collection1/conf/example_solrconfig.xml
[19]: https://cwiki.apache.org/confluence/display/solr/Lib+Directives+in+SolrConfig
[20]: https://cwiki.apache.org/confluence/display/solr/Format+of+solr.xml
[21]: https://cwiki.apache.org/confluence/display/solr/Solr+Plugins
[22]: https://cwiki.apache.org/confluence/display/solr/Adding+Custom+Plugins+in+SolrCloud+Mode
[23]: http://www.cominvent.com/
[101]: http://github.com/healthonnet/hon-lucene-synonyms/issues/1
[102]: http://github.com/healthonnet/hon-lucene-synonyms/issues/2
[103]: http://github.com/healthonnet/hon-lucene-synonyms/issues/3
[104]: http://github.com/healthonnet/hon-lucene-synonyms/issues/4
[105]: http://github.com/healthonnet/hon-lucene-synonyms/issues/5
[109]: http://github.com/healthonnet/hon-lucene-synonyms/issues/9
[116]: http://github.com/healthonnet/hon-lucene-synonyms/issues/16
[119]: http://github.com/healthonnet/hon-lucene-synonyms/issues/19
[120]: http://github.com/healthonnet/hon-lucene-synonyms/issues/20
[126]: http://github.com/healthonnet/hon-lucene-synonyms/issues/26
[128]: http://github.com/healthonnet/hon-lucene-synonyms/issues/28
[130]: http://github.com/healthonnet/hon-lucene-synonyms/issues/30
[131]: http://github.com/healthonnet/hon-lucene-synonyms/issues/31
[132]: http://github.com/healthonnet/hon-lucene-synonyms/issues/32
[133]: http://github.com/healthonnet/hon-lucene-synonyms/issues/33
[134]: http://github.com/healthonnet/hon-lucene-synonyms/issues/34
[141]: https://github.com/healthonnet/hon-lucene-synonyms/issues/41
[165]: https://github.com/healthonnet/hon-lucene-synonyms/issues/65
