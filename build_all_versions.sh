#!/bin/bash
#
# Build all versions of the hon-lucene-synonyms plugin, for the supported Solr versions
#
# Designed to make it easier to upload these JAR files to S3.  Make sure your git workspace is clean before running this!
#
# Writes to directory target/s3
#

SOLR_VERSIONS='3.x 4.0.0 4.1.0 4.3.0 5.3.1';
PLUGIN_VERSION=`cat pom.xml | grep version | head -1 | python -c "import re, sys; print re.search('>(.*?)-solr',sys.stdin.read()).group(1)"`

echo -e "\nPlugin version is ${PLUGIN_VERSION}, building for Solr versions ${SOLR_VERSIONS}...\n"

rm -fr target/s3;
mkdir target/s3;

for SOLR_VERSION in $SOLR_VERSIONS; do
    echo -e "\nBuilding for Solr version ${SOLR_VERSION}...\n"
    git checkout solr-$SOLR_VERSION;
    mvn install;
    FULL_VERSION_NAME=$PLUGIN_VERSION-solr-$SOLR_VERSION;
    mkdir target/s3/$FULL_VERSION_NAME;
    cp target/hon-lucene-synonyms-$FULL_VERSION_NAME.jar target/s3/$FULL_VERSION_NAME;
done;

echo -e "Wrote all jar files to target/s3.  Here they are:\n"
find target/s3 -type f
