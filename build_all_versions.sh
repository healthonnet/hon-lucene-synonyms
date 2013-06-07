#!/bin/bash
# build all versions of the hon-lucene-synonyms plugin, for the supported Solr versions
#
# designed to make it easier to upload these JAR files to S3
#
# writes to directory target/s3

SOLR_VERSIONS='3.x 4.0.0 4.1.0 4.3.0';
PLUGIN_VERSION='1.3.1';

rm -fr target/s3;
mkdir target/s3;

for SOLR_VERSION in $SOLR_VERSIONS; do
    git checkout solr-$SOLR_VERSION;
    mvn install;
    FULL_VERSION_NAME=$PLUGIN_VERSION-solr-$SOLR_VERSION;
    mkdir target/s3/$FULL_VERSION_NAME;
    cp target/hon-lucene-synonyms-$FULL_VERSION_NAME.jar target/s3/$FULL_VERSION_NAME;
done;

echo "wrote all versions to target/s3"
