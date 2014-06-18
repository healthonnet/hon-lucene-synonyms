Enter file contents here#
# Basic unit tests for HON-Lucene-Synonyms
#
# Test that phrase queries synonyms do not get double quoted
# when search contains quotes. And do not quote single term synonyms
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
        
        self.tst_query('"dog"', 8)
        self.tst_query('"pooch"', 8)
        self.tst_query('"hound"', 8)
        self.tst_query('"canis familiaris"', 8)

        self.tst_query('dog', 2)
        self.tst_query('pooch', 2)
        self.tst_query('hound', 2)
        self.tst_query('canis familiaris', 2)
        
    def tst_query(self, query, quote_cnt):

        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'defType' : 'synonym_edismax', 'synonyms' : 'true', 'synonyms.constructPhrases' : 'true', 'debugQuery' : 'on' }
        
        response = self.solr_connection.query(**params)
        results = response.results

	   #TODO: Access the 'expandedSynonyms' list from the response and count
        #      the number of quotes present in the list and compare to value
        #      passed in, though the anticipated expanded list can also
        #      be passed in

if __name__ == '__main__':
    unittest.main()

