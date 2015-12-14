#
# Basic unit tests for HON-Lucene-Synonyms
#
# This one tests some of the problems found in issue #9
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "blood and bones"}, \
    {'id': '2', 'name': u"\u8840\u3068\u9aa8"}, \
    {'id': '3', 'name': 'bfo'}, \
    {'id': '4', 'name': u'brystforst\xf8rrende operation'}, \
    ]
    solr_connection = None
    
    def setUp(self):
        self.solr_connection = connection_with_core(solr.SolrConnection(self.url))
        self.solr_connection.delete_query('*:*')
        self.solr_connection.add_many(self.test_data)
        self.solr_connection.commit()

    def tearDown(self):
        self.solr_connection.delete_query('*:*')
        self.solr_connection.commit()
 
    def test_queries(self):
        
        self.tst_query({}, 'blood and bones', 2)
        self.tst_query({}, u"\u8840\u3068\u9aa8".encode('utf-8'), 2)

        self.tst_query({}, 'bfo', 2)
        self.tst_query({}, u'brystforst\xf8rrende operation'.encode('utf-8'), 2)
        
    def tst_query(self, extra_params, query, expected_num_docs):
        
        params = {'q': query, 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true'}
        params.update(extra_params)
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)

if __name__ == '__main__':
    unittest.main()
