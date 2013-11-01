#
# Basic unit tests for HON-Lucene-Synonyms
#
# Test that synonyms.disablePhraseQueries is working properly
#

import unittest, solr, urllib, time

class TestBasic(unittest.TestCase):
    
    #
    # We have the synonyms:
    #
    # dog, pooch, hound, canis familiaris
    #

    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "I have a dog."}, \
    {'id': '2', 'name': "I have a pooch."}, \
    {'id': '3', 'name': "I have a hound."}, \
    {'id': '4', 'name': "I have a canis."}, \
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
        
        self.tst_query('"dog"', False, 3)
        self.tst_query('"pooch"', False, 3)
        self.tst_query('"hound"', False, 3)
        self.tst_query('"canis familiaris"', False, 3)

        self.tst_query('"dog"', True, 1)
        self.tst_query('"pooch"', True, 1)
        self.tst_query('"hound"', True, 1)
        self.tst_query('"canis familiaris"', True, 0)
        
    def tst_query(self, query, disable_phrase_queries, expected_num_docs):

        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'mm' : '1%', 'defType' : 'synonym_edismax', 'synonyms' : 'true', \
                'synonyms.disablePhraseQueries' : str(disable_phrase_queries).lower()}
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)

if __name__ == '__main__':
    unittest.main()
