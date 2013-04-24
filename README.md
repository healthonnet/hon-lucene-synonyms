Lucene/Solr Synonym-Expanding EDisMax Parser
=========================

Current version : 1.2.3 ([changelog][15])

Developer
-----------

[Nolan Lawson][7]

[Health On the Net Foundation][6]

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

The following tutorial will set up a working synonym-enabled Solr app using the ```example/``` directory from Solr itself, 
running in Jetty.

### Step 1

Download the latest JAR file depending on your Solr version:

* [hon-lucene-synonyms-1.2.3-solr-3.x.jar][12] for Solr 3.4.0, 3.5.0, 3.6.0, 3.6.1, and 3.6.2
* [hon-lucene-synonyms-1.2.3-solr-4.0.0.jar][13] for Solr 4.0.0
* [hon-lucene-synonyms-1.2.3-solr-4.1.0.jar][14] for Solr 4.1.0 and 4.2.0

### Step 2

Download Solr from [the Solr home page][8].  For this tutorial, we'll use [Solr 3.6.2][9].  You do not need
the sources; the ```tgz``` or ```zip``` file will work fine.

### Step 3

Extract the compressed file and cd to the ```example/``` directory.

### Step 4

Now, you need to bundle the ```hon-lucene-synonyms-*.jar``` file into ```webapps/solr.war```.
Below is a script that will work quite nicely on UNIX systems. **Be sure to change the 
```/path/to/my/hon-lucene-synonyms-*.jar``` part before running this script**.

```
mkdir myjar
cd myjar
jar -xf ../webapps/solr.war 
cp /path/to/my/hon-lucene-synonyms-*.jar WEB-INF/lib/
jar -cf ../webapps/solr.war *
cd ..
```

Note that this plugin will not work in any location other than the ```WEB-INF/lib/``` directory of the ```solr.war``` 
itself, because of [issues with the ClassLoader][102].

### Step 5

Download [example_synonym_file.txt][5] and copy it to the ```solr/conf/``` directory 
(or ```solr/collection1/conf/``` in Solr 4.x).

### Step 6

Edit ```solr/conf/solrconfig.xml``` (```solr/collection1/conf/solrconfig.xml``` in 4.x) and add these lines near the 
bottom (before ```</config>```):

```xml
<queryParser name="synonym_edismax" class="solr.SynonymExpandingExtendedDismaxQParserPlugin">
  <str name="luceneMatchVersion">LUCENE_36</str>
  <lst name="synonymAnalyzers">
    <lst name="myCoolAnalyzer">
      <lst name="tokenizer">
        <str name="class">solr.StandardTokenizerFactory</str>
      </lst>
      <lst name="filter">
        <str name="class">solr.ShingleFilterFactory</str>
        <str name="outputUnigramsIfNoShingles">true</str>
        <str name="outputUnigrams">true</str>
        <str name="minShingleSize">2</str>
        <str name="maxShingleSize">4</str>
      </lst>
      <lst name="filter">
        <str name="class">solr.SynonymFilterFactory</str>
        <str name="tokenizerFactory">solr.KeywordTokenizerFactory</str>
        <str name="synonyms">example_synonym_file.txt</str>
        <str name="expand">true</str>
        <str name="ignoreCase">true</str>
      </lst>
    </lst>
  </lst>
</queryParser>
```

Note that you must modify the ```luceneMatchVersion``` above to match the 
```<luceneMatchVersion>...</luceneMatchVersion>``` tag at the beginning of the ```solr/conf/solrconfig.xml``` file.

### Step 7

Start up the app by running ```java -jar start.jar```.  Jetty may print a ```ClassNotFoundException```, but
it shouldn't matter.

### Step 8

In your browser, navigate to 

[```http://localhost:8983/solr/select/?q=dog&debugQuery=on&qf=text&defType=synonym_edismax&synonyms=true```](http://localhost:8983/solr/select/?q=dog&debugQuery=on&qf=text&defType=synonym_edismax&synonyms=true)

You should see a response like this:

```xml
<response>
  ...
  <result name="response" numFound="0" start="0"/>
  <lst name="debug">
    <str name="rawquerystring">dog</str>
    <str name="querystring">dog</str>
    <str name="parsedquery">
        +(DisjunctionMaxQuery((text:dog)) (((DisjunctionMaxQuery((text:canis)) 
        DisjunctionMaxQuery((text:familiaris)))~2) DisjunctionMaxQuery((text:hound)) 
        ((DisjunctionMaxQuery((text:man's)) DisjunctionMaxQuery((text:best)) 
        DisjunctionMaxQuery((text:friend)))~3) DisjunctionMaxQuery((text:pooch))))
    </str>
    <str name="parsedquery_toString">
        +((text:dog) ((((text:canis) (text:familiaris))~2) (text:hound) 
        (((text:man's) (text:best) (text:friend))~3) (text:pooch)))
    </str>
    <lst name="explain"/>
    <str name="QParser">SynonymExpandingExtendedDismaxQParser</str>
    ...
  </lst>
</response>
```

Note that the input query ```dog``` has been expanded into ```dog```, ```hound```, ```pooch```, ```canis familiaris```, and ```man's best friend```.

Tweaking the results
---------------------

Boost the non-synonym part to 1.2 and the synonym part to 1.1 by adding ```synonyms.originalBoost=1.1&synonyms.synonymBoost=1.2```:

```
+((text:dog)^1.1 (((((text:canis) (text:familiaris))~2) (text:hound) 
(((text:man's) (text:best) (text:friend))~3) (text:pooch))^1.2))
```

Apply a [minimum "should" match][16] of 75% by adding ```mm=75%25```:

```
+((text:dog) ((((text:canis) (text:familiaris))~1) (text:hound) 
(((text:man's) (text:best) (text:friend))~2) (text:pooch)))
```

Observe how phrase queries are properly handled by using ```q="dog"``` instead of ```q=dog```:

<pre style="white-space:normal;">
+((text:dog) ((text:"canis familiaris") (text:hound) (text:"man's best friend") (text:pooch)))
</pre>


Gotchas
---------

Keep in mind that you must add ```defType=synonym_edismax``` and ```synonyms=true``` to enable 
the parser in the first place.

Also, you must either define ```qf``` in the query parameters or ```defaultSearchField``` in ```solr/conf/schema.xml```,
so that the parser knows which fields to use during synonym expansion. 

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
<tr>
<td style="padding:0 1em;"><strong><font face="monospace" size="-1">synonyms.constructPhrases</font></strong></td>
<td style="padding:0 1em;"><font size="-1">boolean</font></td>
<td style="padding:0 1em;"><font size="-1">false</font></td>
<td style="padding:0 1em;"><font size="-1"><strong>v1.2.2+:</strong> True if expanded synonyms should always be treated like phrases (i.e. wrapped in quotes).  This option is offered in case your synonyms contain lots of phrases composed of common words (e.g. "man's best friend" for "dog").  Only affects the expanded synonyms; not the original query. See <a href='http://github.com/healthonnet/hon-lucene-synonyms/issues/5'>issue #5</a> for more discussion.</font></td>
</tr>
</table>



Compile it yourself
----------

Download the code and run:

```
mvn install
```

Since there are several branches depending on the Solr version, there's also a build script that will ```git checkout```
each branch, build it, and put it in the ```target/s3``` direcdtory:

```
./build_all_versions.sh
```

Testing
---------

Python-based unit tests are in the ```test/``` directory. You can run them using: 

```
# launches Solr on localhost:8983. Alternatively, you can just follow the "Getting Started" directions
./run_solr_for_unit_tests.sh /path/to/my/optional/solr-4.2.0.tgz

# run some Python unit tests against the local Solr on localhost:8983
nosetests test/
```

Currently I test against Solr 4.2.

Changelog
------------

* v1.2.3
 * Fixed [#16][116]
 * Verified support for Solr 4.2.0 with the 4.1.0 branch (unit tests passed)
 * Improved automation of unit tests
* v1.2.2
 * Added ```synonyms.constructPhrases``` option to fix [issue #5][105]
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
[3]: http://wiki.apache.org/solr/ExtendedDisMax
[4]: http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory
[5]: http://raw.github.com/healthonnet/hon-lucene-synonyms/master/examples/example_synonym_file.txt
[6]: http://www.hon.ch
[7]: http://nolanlawson.com
[8]: http://lucene.apache.org/solr/
[9]: http://www.apache.org/dyn/closer.cgi/lucene/solr/3.6.2
[12]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.3-solr-3.x/hon-lucene-synonyms-1.2.3-solr-3.x.jar
[13]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.3-solr-4.0.0/hon-lucene-synonyms-1.2.3-solr-4.0.0.jar
[14]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.3-solr-4.1.0/hon-lucene-synonyms-1.2.3-solr-4.1.0.jar
[15]: https://github.com/healthonnet/hon-lucene-synonyms#changelog
[16]: http://wiki.apache.org/solr/DisMaxQParserPlugin#mm_.28Minimum_.27Should.27_Match.29
[101]: http://github.com/healthonnet/hon-lucene-synonyms/issues/1
[102]: http://github.com/healthonnet/hon-lucene-synonyms/issues/2
[103]: http://github.com/healthonnet/hon-lucene-synonyms/issues/3
[104]: http://github.com/healthonnet/hon-lucene-synonyms/issues/4
[105]: http://github.com/healthonnet/hon-lucene-synonyms/issues/5
[116]: http://github.com/healthonnet/hon-lucene-synonyms/issues/16
