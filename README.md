Lucene/Solr Synonym-Expanding EDisMax Parser
=========================

Current version : 1.2.1

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

* [hon-lucene-synonyms-1.2.1-solr-3.x.jar][12] for Solr 3.4.0, 3.5.0, 3.6.0, 3.6.1, and 3.6.2
* [hon-lucene-synonyms-1.2.1-solr-4.0.0.jar][13] for Solr 4.0.0
* [hon-lucene-synonyms-1.2.1-solr-4.1.0.jar][14] for Solr 4.1.0

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
itself, because of [issues with the ClassLoader][11].

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
    <str name="parsedquery">+(DisjunctionMaxQuery((text:dog)) (((DisjunctionMaxQuery((text:canis)) DisjunctionMaxQuery((text:familiaris)))~2) DisjunctionMaxQuery((text:hound)) DisjunctionMaxQuery((text:pooch))))</str>
    <str name="parsedquery_toString">+((text:dog) ((((text:canis) (text:familiaris))~2) (text:hound) (text:pooch)))</str>
    <lst name="explain"/>
    <str name="QParser">SynonymExpandingExtendedDismaxQParser</str>
    ...
  </lst>
</response>
```

Note that the input query ```dog``` has been expanded into ```dog```, ```canis familiaris```, ```hound```, and ```pooch```.

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
</table>



Compile it yourself
----------

Download the code and run:

```
mvn install
```


[1]: http://www.apache.org/licenses/LICENSE-2.0.html
[2]: http://nolanlawson.com/2012/10/31/better-synonym-handling-in-solr
[3]: http://wiki.apache.org/solr/ExtendedDisMax
[4]: http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory
[5]: http://raw.github.com/healthonnet/hon-lucene-synonyms/master/examples/example_synonym_file.txt
[6]: http://www.hon.ch
[7]: http://nolanlawson.com
[8]: http://lucene.apache.org/solr/
[9]: http://www.apache.org/dyn/closer.cgi/lucene/solr/3.6.2
[11]: http://github.com/healthonnet/hon-lucene-synonyms/issues/2
[12]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.1-solr-3.x/hon-lucene-synonyms-1.2.1-solr-3.x.jar
[13]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.1-solr-4.0.0/hon-lucene-synonyms-1.2.1-solr-4.0.0.jar
[14]: http://nolanlawson.s3.amazonaws.com/dist/org.healthonnet.lucene.synonyms/release/1.2.1-solr-4.1.0/hon-lucene-synonyms-1.2.1-solr-4.1.0.jar
