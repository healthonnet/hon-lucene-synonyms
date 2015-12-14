#!/usr/bin/env bash

./run_solr_for_unit_tests.py &
SOLR_PID=$!

while [[ $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8983/solr/) != '200' ]]; do
  echo "waiting for localhost:8983 to be available..."
  sleep 10;
done

echo "localhost:8983 is available"

nosetests test/

EXIT_CODE=$?

if [[ ! -z $SOLR_PID ]]; then
  echo "killing solr..."
  kill -INT $SOLR_PID
  # couldn't figure out any better way to kill the java subprocess
  kill `ps -ef | grep techproducts | grep -v grep | awk '{print $2}'`
fi

exit $EXIT_CODE
