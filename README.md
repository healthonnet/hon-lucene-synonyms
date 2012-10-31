HON Lucene Synonyms
=========================

Developer
-----------

[Nolan Lawson][7]

[Health On the Net Foundation][6]

License
-----------

[Apache 2.0][1].

Summary
-----------

Extension of the [ExtendedDisMaxQueryParserPlugin][3] that splits queries into a "normal" query and a "synonym" query.

This fixes lots of bugs with how Solr typically handles synonyms, using the [SynonymFilterFactory][4].

For usage instructions and full details, read [my blog post on the subject][2].

[Link to the latest JAR file][5].

Query parameters
------------

Be sure to add ```defType=synonym_edismax``` to enable the parser.

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
<td style="padding:0 1em;"><font size="-1">Name of the analyzer defined in <font face="monospace">solrconfig.xml</font> to use. (E.g. in the examples, it's <font face="monospace">myCoolAnalyzer</font>). This <em>must</em> be non-null, if you define more than one analyzer.</font></td>
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
<td style="padding:0 1em;"><font size="-1">Enable or disable synonym expansion when the user input contains a phrase query (i.e. a quoted query).</font></td>
</tr>
</table>



Compile it yourself
----------

Download the code and do:

```
mvn install
```

[1]: http://www.apache.org/licenses/LICENSE-2.0.html
[2]: http://nolanlawson.com/2012/10/31/better-synonym-handling-in-solr
[3]: http://wiki.apache.org/solr/ExtendedDisMax
[4]: http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory
[5]: https://github.com/downloads/HON-Khresmoi/hon-lucene-synonyms/hon-lucene-synonyms-1.0.jar
[6]: http://www.hon.ch
[7]: http://nolanlawson.com
