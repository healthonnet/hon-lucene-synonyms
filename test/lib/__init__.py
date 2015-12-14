import solr
import json


def first_core_of_connection(solr_connection):
    response = solr.SearchHandler(solr_connection, "/admin/cores").raw(**{'action': 'STATUS', 'wt': 'json'})
    results = json.loads(response)
    core = results['status'].keys().pop()
    return core


def connection_with_core(solr_connection):
    return solr.SolrConnection('http://localhost:8983/solr/{}'.format(first_core_of_connection(solr_connection)))
