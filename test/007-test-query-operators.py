#
# Basic unit tests for HON-Lucene-Synonyms
#
# This one tests some of the problems found in issues #28 and #32
#

import unittest, solr, urllib

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "e-commerce"}, \
    {'id': '2', 'name': "electronic commerce"}, \
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
            
        self.tst_query({}, 'commerce', 2)
        self.tst_query({}, 'electronic commerce', 2)
        self.tst_query({}, 'e-commerce', 2)

        # means "shouldn't contain the word commerce"
        self.tst_query({}, 'e -commerce', 0)
        # it's also no good as a single word
        self.tst_query({}, 'ecommerce', 0)

        # it doesn't expand synonyms when using a space instead of a hyphen
        # (i.e. it only matches document #1)
        self.tst_query({}, 'e commerce', 1)
        
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
