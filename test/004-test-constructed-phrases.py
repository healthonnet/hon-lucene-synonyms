#
# Basic unit tests for HON-Lucene-Synonyms
# Assumes there's a solr service running at localhost:8983/solr
# Assumes the system was set up with the standard configuration
# described in the "Getting Started" section of the readme.
#

import solr
import unittest
import urllib


from lib import connection_with_core


class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "man's best friend"}, \
    {'id': '2', 'name': "the best friend of man's"}, \
    {'id': '3', 'name': "canis familiaris"}, \
    {'id': '4', 'name': "species familiaris, of the genus canis"}, \
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
 
    # adding 'synonyms.constructPhrases' should automatically build phrase queries
    # out of synonyms, since they're known to be meaningful combinations of words (Issue #5)
    def test_construct_phrases(self):
            
        # we're being lax, all four docs can match
        self.tst_query({}, 'dog', 4)
        self.tst_query({}, 'canis familiaris', 4)
        self.tst_query({}, 'pooch', 4)
        self.tst_query({}, 'hound', 4)
        self.tst_query({}, "man's best friend", 4)
        
        # whoops, now suddenly "canis familiaris" and "man's best friend" are understood to be phrases!
        self.tst_query({'synonyms.constructPhrases':'true'}, 'dog', 2)
        self.tst_query({'synonyms.constructPhrases':'true'}, 'pooch', 2)
        self.tst_query({'synonyms.constructPhrases':'true'}, 'hound', 2)
        
        # but only when they're the synonyms - the original query is still unmodified
        self.tst_query({'synonyms.constructPhrases':'true'}, "man's best friend", 3)
        self.tst_query({'synonyms.constructPhrases':'true'}, "canis familiaris", 3)
        
        # (sanity test: the default is false, right?)
        self.tst_query({'synonyms.constructPhrases':'false'}, 'dog', 4)
        self.tst_query({'synonyms.constructPhrases':'false'}, 'pooch', 4)
        self.tst_query({'synonyms.constructPhrases':'false'}, 'hound', 4)
        self.tst_query({'synonyms.constructPhrases':'false'}, 'canis familiaris', 4)
        self.tst_query({'synonyms.constructPhrases':'false'}, "man's best friend", 4)
                            
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
