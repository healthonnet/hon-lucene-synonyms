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
    test_data = [
    {'id': '1', 'name': "man's best friend blah blah blah"},
    {'id': '2', 'name': "man's blah best blah blah friend"},
    
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
 
    def test_phrase_slop(self):
        # without phrase slop, the two should be equal
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'edismax'}, \
                ['1','2'], False)
                
        # with phrase slop, the first should have a higher score
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'edismax','ps':3,'pf':'name'}, \
                ['1','2'], True)
        
        # ditto for synonym_edismax
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'synonym_edismax'}, \
                ['1','2'], False)
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'synonym_edismax','ps':3,'pf':'name'}, \
                ['1','2'], True)        
                
        # but what about with... SYNONYMS?   dun dun dun!
        # (hint: it should be the same)
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'synonym_edismax',\
                        'synonyms':'true'}, \
                ['1','2'], False)
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"man's best friend",'qf':'name','defType':'synonym_edismax',\
                        'synonyms':'true','ps':3,'pf':'name'}, \
                ['1','2'], True)
        
        # and what about with a synonym as input?  oh now you're just getting crazy
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"dog",'qf':'name','defType':'synonym_edismax',\
                        'synonyms':'true'}, \
                ['1','2'], False)
        self.verify_expected_results( \
                {'fl':'id,score', 'q':"dog",'qf':'name','defType':'synonym_edismax',\
                        'synonyms':'true','ps':3,'pf':'name'}, \
                ['1','2'], True)        
        
        # cool story bro
    
    def verify_expected_results(self, params, expectedDocs, inequalResults):

        results = self.solr_connection.query(**params).results
        
        self.assertEqual(len(results),len(expectedDocs))
        
        if (inequalResults):
            for (expectedDoc, actualDoc) in zip(expectedDocs, results):
                self.assertEqual(expectedDoc, actualDoc['id'])
            self.assertTrue(results[0]['score'] > results[1]['score'])
        else:
            self.assertEqual(set(expectedDocs), set(map(lambda x:x['id'],results)))
            self.assertEqual(results[0]['score'], results[1]['score'])
        
    def get(self, extra_params, query, expected_num_docs):
        
        params = {'q': query, 'qf' : 'name', 'mm' : '100%', 'defType' : 'synonym_edismax', 'synonyms' : 'true'}
        params.update(extra_params)
        
        response = self.solr_connection.query(**params)
        results = response.results
        print '\ntesting ',self.url + '/select?' + urllib.urlencode(params),\
        '\n',map(lambda x: x['name'],results),'\nActual: %s, Expected: %s' % (len(results), expected_num_docs)
        
        self.assertEqual(len(results), expected_num_docs)

if __name__ == '__main__':
    unittest.main()
