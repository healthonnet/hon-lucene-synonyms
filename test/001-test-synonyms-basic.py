#
# Basic unit tests for HON-Lucene-Synonyms
# Assumes there's a solr service running at localhost:8983/solr
# Assumes the system was set up with the standard configuration
# described in the "Getting Started" section of the readme.
#

import unittest, solr

class TestBasic(unittest.TestCase):
    
    test_data = [
    {'id': '1', 'name': 'dog'},
    {'id': '2', 'name': 'pooch'},
    {'id': '3', 'name': 'hound'},
    {'id': '4', 'name': 'canis familiaris'},
    {'id': '5', 'name': 'canis'},
    {'id': '6', 'name': 'familiaris'},
    ]
    solr_connection = None
    
    def setUp(self):
        self.solr_connection = solr.SolrConnection('http://localhost:8983/solr')
        self.solr_connection.delete_query('*:*')
        self.solr_connection.add_many(self.test_data)
        self.solr_connection.commit()
    
    def test_without_synonyms(self):
            
        self.query_test(False, 'dog', 1)
        self.query_test(False, 'pooch', 1)
        self.query_test(False, 'hound', 1)
        self.query_test(False, 'canis familiaris', 1)
        self.query_test(False, 'cat', 0)
        self.query_test(False, 'canis', 2)
    
    def test_with_synonyms(self):
        
        self.query_test(True, 'dog', 4)
        self.query_test(True, 'pooch', 4)
        self.query_test(True, 'hound', 4)
        self.query_test(True, 'canis familiaris', 4)
        self.query_test(True, 'cat', 0)
        self.query_test(True, 'canis', 2)
    
    def query_test(self, enable_synonyms, query, expected_num_docs):
        
        params = {'qf' : 'name', 'mm' : '100%', 'defType' : 'edismax'}
        if (enable_synonyms):
            params.update({'synonyms' : 'true', 'defType' : 'synonym_edismax'})
        
        response = self.solr_connection.query(query, **params)
        results = response.results
        
        self.assertEqual(len(results), expected_num_docs)

if __name__ == '__main__':
    unittest.main()