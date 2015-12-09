#
# Basic unit tests for HON-Lucene-Synonyms
#

import unittest, solr, urllib, time

from lib import connection_with_core

class TestBasic(unittest.TestCase):
    
    url = 'http://localhost:8983/solr'
    test_data = [ \
    {'id': '1', 'name': "I have a dog and a pooch and a canis familiaris."}, \
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
        
        self.tst_highlight('dog')
        self.tst_highlight('hound')
        self.tst_highlight('pooch')
        self.tst_highlight('canis familiaris')
        
    def tst_highlight(self, query):

        params = {'q': query, 'qf' : 'name', 'hl.simple.pre' : '<em>', 'hl.simple.post' : '</em>', \
                'hl' : 'on', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true'}
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), 1)
        
        self.assertEqual(len(results), 1)
        expected_hl = ["I have a <em>dog</em> and a <em>pooch</em> and a <em>canis</em> <em>familiaris</em>."]
        self.assertEquals(expected_hl, response.highlighting['1']['name'])

if __name__ == '__main__':
    unittest.main()
