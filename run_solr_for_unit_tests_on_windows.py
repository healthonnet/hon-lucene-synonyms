Enter file contents here#!/usr/bin/env python
#
# Set it up so we can painless run the python nose tests against the localhost 8983.
# Summary:
# 
# ./run_solr_for_unit_tests.py [--debug] [--debug-port=9999] [--port=8983]
#
# Add the argument "--debug" to start the Jetty server in debug mode.  Specify the port with "--debug-port=XXXX".
# Assumes you're running this on a *nix machine.
#

import sys, os, shutil, urllib, xml.dom.minidom, tarfile, getopt, glob

scriptdir = os.path.dirname(os.path.abspath(__file__))

args = {'--debug-port' : 9999, '--port' : 8983}
args.update(dict(getopt.getopt(sys.argv[1:], '', ['debug', 'debug-port=', 'port='])[0]))

os.system("mvn clean package")

shutil.rmtree('target/webapp', ignore_errors=True)
os.mkdir('target/webapp')

def find_solr_version():
  pom = xml.dom.minidom.parse('pom.xml')

  def isSolrCoreDep(dep):
	return filter((lambda artifact : artifact.lastChild.data == 'solr-core'),	dep.getElementsByTagName('artifactId'))
  
  solrCoreDep = filter(isSolrCoreDep, pom.getElementsByTagName('dependency'))[0]

  return solrCoreDep.getElementsByTagName('version')[0].lastChild.data

# compare major.minor.patch-style versions, e.g. 1.2 vs 1.2.1 vs 1.2.3 etc.
def version_compare(ver1, ver2):
  def listify(str):
    return map(lambda x:int(x), str.split('.'))
  (list1, list2) = (listify(ver1), listify(ver2))
  # lexicographical comparison
  return -1 if list1 < list2 else (1 if list2 > list1 else 0)

solr_version = find_solr_version()

# they changed their naming convention in v 4.1.0
tgz_filename = ('solr-%s' if (version_compare(solr_version, '4.1.0') >= 0) else 'apache-solr-%s') % solr_version

local_filename = 'target/webapp/solr.tgz'
mvn_filename = os.environ['HOME'] + \
    ('/.m2/repository/org/healthonnet/hon-lucene-synonyms-solrdep/%s/hon-lucene-synonyms-solrdep-%s.tgz' \
    % (solr_version, solr_version))
mvn_filename = os.path.expandvars(mvn_filename)


if not os.path.isfile(mvn_filename):
  # download the tgz file
  print "Downloading solr tgz file version %s (I'll only have to do this once)" % solr_version
  tgz_url = 'http://archive.apache.org/dist/lucene/solr/%s/%s.tgz' % (solr_version, tgz_filename) 
  def reporthook(a,b,c): 
    print "% 3.1f%% of %d bytes\r" % (min(100, float(a * b) / c * 100), c),
    sys.stdout.flush()
  urllib.urlretrieve(tgz_url, local_filename, reporthook) 
  
  # use maven to store the file locally in the future
  install_cmd = """mvn install:install-file \
                -DgroupId=org.healthonnet \
                -DartifactId=hon-lucene-synonyms-solrdep \
                -Dversion=%s \
                -Dfile=%s \
                -Dpackaging=tgz""" % (solr_version, local_filename)
  os.system(install_cmd)
else:
 shutil.copy(mvn_filename, local_filename)

solrdir = 'target\\webapp\\' + tgz_filename

tar = tarfile.open(local_filename)
tar.extractall(path='target/webapp/')
os.mkdir(solrdir + '/example/myjar')

os.chdir(solrdir + '/example/myjar')
os.system('jar -xf ../webapps/solr.war' )
os.chdir(scriptdir)
for fname in glob.glob(r'target/hon-lucene-synonyms-*.jar'):
  shutil.copy(fname, solrdir + '/example/myjar/WEB-INF/lib')


os.chdir(solrdir + '/example/myjar')
os.system('jar -cf ../webapps/solr.war *' )
os.chdir(scriptdir)


# they changed the location of the example conf dir in solr 4.0.0
confdir = 'collection1/conf' if version_compare(solr_version, '4.0.0') >= 0 else 'conf'
shutil.copy('examples/example_synonym_file.txt', solrdir + '/example/solr/' + confdir)

# add the config to the config file
conf_to_add = open('examples/example_config.xml', 'r').read()


conf_filename = solrdir + '/example/solr/' + confdir + '/solrconfig.xml'
filein = open(conf_filename,'r')
filetext = filein.read()
filein.close()
fileout = open(conf_filename,'w')
fileout.write(filetext.replace('</config>', conf_to_add + '</config>'))
fileout.close()

debug = ('-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s' % args['--debug-port']) if '--debug' in args else '' 
cmd = 'java %(debug)s -Djetty.port=%(port)s -jar start.jar' % \
    {'debug' : debug, 'solrdir' : solrdir, 'port' : args['--port']}

print "Running jetty with command: " + cmd
os.chdir(solrdir + '/example')
os.system(cmd)
