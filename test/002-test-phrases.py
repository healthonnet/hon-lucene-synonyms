#
# Basic unit tests for HON-Lucene-Synonyms
# Assumes there's a solr service running at localhost:8983/solr
# Assumes the system was set up with the standard configuration
# described in the "Getting Started" section of the readme.
#

import unittest, solr, urllib
from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [
    {'id': '1', 'name': 'dog'},
    {'id': '2', 'name': 'pooch'},
    {'id': '3', 'name': 'hound'},
    {'id': '4', 'name': 'canis familiaris'},
    {'id': '5', 'name': 'canis'},
    {'id': '6', 'name': 'familiaris'},
    {'id': '7', 'name': "man's best friend"},
    {'id': '8', 'name': "man's"},
    {'id': '9', 'name': "best"},
    {'id': '10', 'name': "friend"},
    
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
 
    # min should match ('mm') should work the same regardless of how many tokens
    # there are in the input synonym
    def test_min_should_match(self):
            
        self.tst_query({'mm' : '100%'}, 'dog', 5)
        self.tst_query({'mm' : '100%'}, 'canis familiaris', 5)
        self.tst_query({'mm' : '100%'}, 'pooch', 5)
        self.tst_query({'mm' : '100%'}, 'hound', 5)
                        
        # suddenly one token in "canis familiaris" matches too
        self.tst_query({'mm' : '75%'}, 'dog', 7)
        self.tst_query({'mm' : '75%'}, 'canis familiaris', 7)
        self.tst_query({'mm' : '75%'}, 'pooch', 7)
        self.tst_query({'mm' : '75%'}, 'hound', 7)
        
        # suddenly "one token in "man's best friend" matches too
        self.tst_query({'mm' : '50%'}, 'dog', 10)
        self.tst_query({'mm' : '50%'}, 'canis familiaris', 10)
        self.tst_query({'mm' : '50%'}, 'pooch', 10)
        self.tst_query({'mm' : '50%'}, 'hound', 10)
                
                        
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
