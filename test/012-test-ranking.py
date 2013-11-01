#
# Basic unit tests for HON-Lucene-Synonyms
#

import unittest, solr, urllib, time

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "I have a dog."}, \
    {'id': '2', 'name': "I have a pooch."}, \
    {'id': '3', 'name': "I have a canis familiaris."}, \
    ]
    solr_connection = None
    
    def setUp(self):
        self.solr_connection = solr.SolrConnection(self.url)
        self.solr_connection.delete_query('*:*')
        self.solr_connection.add_many(self.test_data)
        self.solr_connection.commit()

    def tearDown(self):
        self.solr_connection.delete_query('*:*')
        self.solr_connection.commit()
 
    def test_queries(self):
        
        # the document containing the actual term should be first, but all 3 docs should match
        self.tst_query('dog', 3, '1')
        self.tst_query('pooch', 3, '2')
        self.tst_query('canis familiaris', 3, '3')
        
    def tst_query(self, query, expected_num_docs, expected_doc_id):

        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true'}
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)
        self.assertEquals(results[0]['id'], expected_doc_id)

if __name__ == '__main__':
    unittest.main()
