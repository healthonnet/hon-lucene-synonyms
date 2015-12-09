#
# Basic unit tests for HON-Lucene-Synonyms
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [
    {
        "id":"1",
        "name":"Jigglypuff is top-tier.",
        "cat":"Y",
        "last_modified":"2012-01-01T00:00:00Z"
   },
   {
        "id":"2",
        "name":"Purin is top-tier.",
        "cat":"Y",
        "last_modified":"2012-01-01T00:00:00Z"
   }
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
        
        # confirm that the documents have the same scores, since their dates and categories are the same
        
        self.tst_all_scores_equal({}, 'jigglypuff', 2)
        self.tst_all_scores_equal({}, 'purin', 2)

        bq = 'cat:Y^1000'
        bf = 'recip(ms(NOW,last_modified),3.16e-11,1,1)' # standard date boosting function recommended in the solr docs

        # bq
        self.tst_all_scores_equal({'bq' : bq}, 'jigglypuff', 2)
        self.tst_all_scores_equal({'bq' : bq}, 'purin', 2)
        
        # bf
        self.tst_all_scores_equal({'bf' : bf}, 'jigglypuff', 2)
        self.tst_all_scores_equal({'bf' : bf}, 'purin', 2)
        
        # boost  (w/ standard date boosting function)
        self.tst_all_scores_equal({'boost' : bf}, 'jigglypuff', 2)
        self.tst_all_scores_equal({'boost' : bf}, 'purin', 2)
        
    def tst_all_scores_equal(self, extra_params, query, expected_num_docs):
        
        params = {'q': query, 'fl' : '*,score', 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true',\
            'synonyms.originalBoost' : 1.0, 'synonyms.synonymBoost' : 1.0}
        params.update(extra_params)
        
        response = self.solr_connection.query(**params)
        results = response.results

        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)

        # ensure only one unique score returned
        scores = [score for score in map((lambda doc : doc['score']), results)]
        print '\ngot scores back (expect all to be equal):', scores
        self.assertEqual(len(set(scores)), 1)

if __name__ == '__main__':
    unittest.main()
