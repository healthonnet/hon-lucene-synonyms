#
# Basic unit tests for HON-Lucene-Synonyms
# Assumes there's a solr service running at localhost:8983/solr
# Assumes the system was set up with the standard configuration
# described in the "Getting Started" section of the readme.
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': 'my dog licks my face'}, \
    {'id': '2', 'name': 'my canis familiaris licks my face'}, \
    {'id': '3', 'name': "my man's best friend licks my face"}, \
    {'id': '4', 'name': 'backpack'},
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
    
    def test_construct_phrases_advanced(self):
        
        # sanity test to make sure the quotes are being applied to the right place

        self.tst_query({}, 'my dog licks my face', 3)
        self.tst_query({}, 'my hound licks my face', 3)
        self.tst_query({}, 'my canis familiaris licks my face', 3)
        self.tst_query({}, "my man's best friend licks my face", 3)
        
        self.tst_query({'synonyms.constructPhrases':'true'}, 'my dog licks my face', 3)
        self.tst_query({'synonyms.constructPhrases':'true'}, 'my hound licks my face', 3)
        self.tst_query({'synonyms.constructPhrases':'true'}, 'my canis familiaris licks my face', 3)
        self.tst_query({'synonyms.constructPhrases':'true'}, "my man's best friend licks my face", 3)

    def test_issue_16(self):
        self.tst_query({'mm' : '01%'}, 'meaningful token canis familiaris', 3)
        self.tst_query({'mm' : '01%'}, 'backpack', 1)
	self.tst_query({'mm' : '01%'}, 'back pack', 1)
	self.tst_query({'mm' : '01%'}, 'north land back pack', 1)
        self.tst_query({'mm' : '01%'}, 'north land backpack', 1)
                            
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
