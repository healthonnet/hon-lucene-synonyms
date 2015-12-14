#
# Basic unit tests for HON-Lucene-Synonyms
#
# Test that phrase queries synonyms do not get double quoted
# when search contains quotes. And do not quote single term synonyms
#

from urllib2 import *
import unittest, solr, urllib, time

from lib import connection_with_core, first_core_of_connection

class TestBasic(unittest.TestCase):
    
  #
    # We have the synonyms:
    #
    # dog, pooch, hound, canis familiaris, man's best friend
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
        self.solr_connection = connection_with_core(solr.SolrConnection(self.url))
        self.solr_connection.delete_query('*:*')
        self.solr_connection.add_many(self.test_data)
        self.solr_connection.commit()

    def tearDown(self):
        self.solr_connection.delete_query('*:*')
        self.solr_connection.commit()
 
    def test_queries(self):
        
        self.tst_query('"dog"', 10)
        self.tst_query('"pooch"', 10)
        self.tst_query('"hound"', 10)
        self.tst_query('"canis familiaris"', 10)

        self.tst_query('dog', 4)
        self.tst_query('pooch', 4)
        self.tst_query('hound', 4)
        self.tst_query('canis familiaris', 2)


    def tst_query(self, query, quote_cnt):
        #Properly format spaces in the query
        #
        query = urllib.quote_plus(query)
        connstr = self.url + '/' + first_core_of_connection(solr.SolrConnection(self.url)) +'/select?q='+query+'&fl=*,score&qf=name&defType=synonym_edismax&synonyms=true&synonyms.constructPhrases=true&debugQuery=on'
        #Add wt=python so response is formatted as python readable
        conn = urlopen(connstr+'&wt=python')
        rsp = eval( conn.read() )
        #print "number of matches=", rsp['response']['numFound']
        print rsp['debug']['expandedSynonyms']

        #Count the number of quotes in our expandedSynonyms Debug element
        cnt = 0
        for str in rsp['debug']['expandedSynonyms']:
            #print str
            cnt += str.count('"')
        print 'Quotes found count = ', cnt

        self.assertEqual(cnt, quote_cnt)

if __name__ == '__main__':
    unittest.main()

