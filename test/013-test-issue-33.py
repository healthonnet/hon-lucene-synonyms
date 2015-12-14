#
# Basic unit tests for HON-Lucene-Synonyms
#
# Test that synonyms are not harshly weighted when there are >2
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    #
    # We have the synonyms:
    #
    # dog, pooch, hound, canis familiaris
    # jigglypuff, purin
    #
    # This test confirms that 
    # Jigglypuff/purin don't get any special treatment, just because there are 2 and not 4 of them
    # (when synonyms.originalBoost and synonyms.synonymBoost are equal)
    #

    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "I have a dog."}, \
    {'id': '2', 'name': "I have a pooch."}, \
    {'id': '3', 'name': "I have a hound."}, \
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
        
        self.tst_same_score('jigglypuff', 2)
        self.tst_same_score('purin', 2)

        self.tst_same_score('dog', 3)
        self.tst_same_score('pooch', 3)
        self.tst_same_score('hound', 3)
        
    def tst_same_score(self, query, expected_num_docs):

        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true', \
                'synonyms.originalBoost' : 1.0, 'synonyms.synonymBoost' : 1.0}
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)

        # verify that all returned docs have the same score
        scores = [score for score in map((lambda doc : doc['score']), results)]
        print '\ngot scores back (expect all to be equal):', scores
        self.assertEqual(len(set(scores)), 1)

if __name__ == '__main__':
    unittest.main()
