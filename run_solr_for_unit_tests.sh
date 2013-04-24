#!/bin/bash
#
# Set it up so we can painless run the python nose tests against the localhost 8983.
#
# Downloads the latest Solr 4.2.0, puts in the synonyms jar, adds a synonyms file, and starts up a local solr on 8983.
#
# Optionally takes solr-4.2.0.tgz as an argument, if you have it.  If not, it wgets it.
#

SOLRJAR=$1

mvn clean package

mkdir -p target/webapp

cd target/webapp

if [[ -z $SOLRJAR ]]; then 
    wget 'http://archive.apache.org/dist/lucene/solr/4.2.0/solr-4.2.0.tgz'
else
    cp $SOLRJAR ./solr-4.2.0.tgz
fi

tar -xzf solr-*.tgz
cd solr*/example
mkdir myjar
cd myjar
jar -xf ../webapps/solr.war
cp ../../../../hon-lucene-synonyms-*.jar WEB-INF/lib
jar -cf ../webapps/solr.war *
cd ..

cp ../../../../examples/example_synonym_file.txt solr/collection1/conf/

sed_command='sed'
if [ $(uname) == 'Darwin' ]; then sed_command='gsed'; fi


$sed_command -i '/<\/config>/i<queryParser name="synonym_edismax" class="solr.SynonymExpandingExtendedDismaxQParserPlugin"> <str name="luceneMatchVersion">LUCENE_41</str> <lst name="synonymAnalyzers"> <lst name="myCoolAnalyzer"> <lst name="tokenizer"> <str name="class">solr.StandardTokenizerFactory</str> </lst> <lst name="filter"> <str name="class">solr.ShingleFilterFactory</str> <str name="outputUnigramsIfNoShingles">true</str> <str name="outputUnigrams">true</str> <str name="minShingleSize">2</str> <str name="maxShingleSize">4</str> </lst> <lst name="filter"> <str name="class">solr.SynonymFilterFactory</str> <str name="tokenizerFactory">solr.KeywordTokenizerFactory</str> <str name="synonyms">example_synonym_file.txt</str> <str name="expand">true</str> <str name="ignoreCase">true</str> </lst> </lst> </lst></queryParser>' solr/collection1/conf/solrconfig.xml

java -jar start.jar
