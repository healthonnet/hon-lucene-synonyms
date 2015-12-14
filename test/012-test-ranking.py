#
# Basic unit tests for HON-Lucene-Synonyms
#
# This test confirms that the main query is weighted higher than the synonym query,
# when synonyms.originalBoost is higher than synonyms.synonymBoost.
# 
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "I have a dog."}, \
    {'id': '2', 'name': "I have a pooch."}, \
    {'id': '3', 'name': "I have a canis familiaris."}, \
    {'id': '4', 'name': "I have a jigglypuff."}, \
    {'id': '5', 'name': "I have a purin."}, \
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
        
        # the document containing the actual term should be first, but all related docs should match
        self.tst_query('dog', 3, '1')
        self.tst_query('pooch', 3, '2')
        self.tst_query('canis familiaris', 3, '3')
        self.tst_query('purin', 2, '5')
        self.tst_query('jigglypuff', 2, '4')

        
    def tst_query(self, query, expected_num_docs, expected_doc_id):

        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true',\
                'synonyms.originalBoost' : 2.0, 'synonyms.synonymBoost' : 1.0}
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)
        self.assertEquals(results[0]['id'], expected_doc_id)

if __name__ == '__main__':
    unittest.main()
