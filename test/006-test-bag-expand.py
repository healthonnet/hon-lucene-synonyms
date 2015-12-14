
# Assumes there's a solr service running at localhost:8983/solr
# Assumes the system was set up with the standard configuration
# Test the "bag" synonym feature
# by testing that we largely get the same results either way
# (though phrase matches will be lost)

import unittest, solr

from lib import connection_with_core

class TestBaggedSynonyms(unittest.TestCase):
    
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
        self.solr_connection = connection_with_core(solr.SolrConnection('http://localhost:8983/solr'))
        self.solr_connection.delete_query('*:*')
        self.solr_connection.add_many(self.test_data)
        self.solr_connection.commit()

    def tearDown(self):
        self.solr_connection.delete_query('*:*')
        self.solr_connection.commit()

    def test_without_bagged_synonyms(self):
        self.tst_query(False, 'dog', 4)
        self.tst_query(False, 'pooch', 4)
        self.tst_query(False, 'hound', 4)
        self.tst_query(False, 'canis familiaris', 4)
        self.tst_query(False, 'cat', 0)
        self.tst_query(False, 'canis', 2)
    
    def test_with_bagged_synonyms(self):
        self.tst_query(True, 'dog', 4)
        self.tst_query(True, 'pooch', 4)
        self.tst_query(True, 'hound', 4)
        self.tst_query(True, 'canis familiaris', 4)
        self.tst_query(True, 'cat', 0)
        self.tst_query(True, 'canis', 2)

    def tst_query(self, bag_synonyms, query, expected_num_docs):

        params = {'qf': 'name', 'mm': '100%', 'defType': 'edismax'}
        params.update({'synonyms': 'true', 'defType': 'synonym_edismax'})

        if bag_synonyms:
            params.update({'synonyms.bag': 'true'})

        response = self.solr_connection.query(query, **params)
        results = response.results

        self.assertEqual(len(results), expected_num_docs)

if __name__ == '__main__':
    unittest.main()
