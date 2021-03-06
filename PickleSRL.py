#!/usr/bin/jython

from __future__ import division
from __future__ import print_function
import os
import pickle
from java.lang import System
from os.path import join
import argparse
import sys

wd = '/home/dick/srl/goto'
os.chdir(wd)
System.setProperty("user.dir", os.getcwd())
parser = argparse.ArgumentParser()
group = parser.add_mutually_exclusive_group()
choices = ['Agent','Origin','Destination','Location','Result','RawMaterial','Theme','Time']
group.add_argument('-a', '--useArgid', action='store', choices=choices)
group.add_argument('-b', '--useBinarize', action='store_true')
args = parser.parse_args()
# peterbe.com
def f7(seq):
    seen = set()
    seen_add = seen.add
    return [x for x in seq if not (x in seen or seen_add(x))]

import sys
from glob import iglob
sys.path.extend([join(wd, s) for s in [
    'classes/production/process-extraction-2013-9-26',
    'lib/gurobi.jar',
    'lib/joda-time.jar',
    'lib/jollyday.jar',
    'lib/stanford-corenlp-1.3.5-javadoc.jar',
    'lib/stanford-corenlp-1.3.5-models.jar',
    '/home/dick/.local/bin',
    '/usr/local/lib/python2.7/dist-packages/pyre-0.8_pathos-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/pox-0.2.2-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/dill-0.2.5-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/pp-1.5.7_pathos-py2.7.egg',
    '/home/dick/.local/lib/python2.7/site-packages/pathos-0.2a1.dev0-py2.7.egg',
    '/home/dick/.local/lib/python2.7/site-packages/multiprocess-0.70.4-py2.7-linux-x86_64.egg',
    '/home/dick/.local/lib/python2.7/site-packages/ppft-1.6.4.6-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/threetaps-0.2.2-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/feedparser-5.2.0-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/pyflakes-0.9.2-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/pylint-1.4.4-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/astroid-1.3.6-py2.7.egg',
    '/usr/local/lib/python2.7/dist-packages/logilab_common-1.0.2-py2.7.egg',
    '/usr/lib/python2.7',
    '/usr/lib/python2.7/plat-x86_64-linux-gnu',
    '/usr/lib/python2.7/lib-tk',
    '/usr/lib/python2.7/lib-old',
    '/usr/lib/python2.7/lib-dynload',
    '/home/dick/.local/lib/python2.7/site-packages',
    '/usr/local/lib/python2.7/dist-packages',
    '/usr/lib/python2.7/dist-packages',
    '/usr/lib/python2.7/dist-packages/PILcompat',
    '/usr/lib/python2.7/dist-packages/gtk-2.0',
    '/usr/lib/pymodules/python2.7',
    '/home/dick/.local/lib/python2.7/site-packages/IPython/extensions',
    '/home/dick/.ipython'
]]);
sys.path = f7(sys.path)
# classPathHacker.classPathHacker()
#for fn in iglob(join(wd, '/*.jar')):
#    classPathHacker.loadJar(fn)


# from java.lang import ClassLoader
# cl = ClassLoader.getSystemClassLoader()
# paths = map(lambda url: url.getFile(), cl.getURLs())
# print paths

from edu.stanford.nlp.bioprocess import Main
main = Main()
#beach.main('-datasetDir lib/Dataset -execPoolDir state/execs -mode init -runOn sample -runModel global'.split())
from java.util import HashMap
from java.util import List
from fig.exec import Execution

useArgid = ' -useArgid ' + args.useArgid + ' ' if args.useArgid else ''
useBinarize = ' -useBinarize ' if args.useBinarize else ''
Execution.init('-datasetDir lib/Dataset {}{}-execPoolDir state/execs -mode init -runOn dev -runModel global'.format(useBinarize, useArgid).split(), 'Main', main)
# need to get ParamOne initialized
main.run()
hmap = HashMap()
for subdir in ['train', 'test', 'sample']:
    hmap.put(subdir, join(wd, 'lib/Dataset', subdir) + '/')
listbio = main.runSRLPrediction(hmap)

pickleMe = [{ 'exampleID': d.exampleID, 'rankedRoleProbs':[(x.first(), x.second()) for x in d.rankedRoleProbs] , 'features': {y[0]:y[1] for y in [x.split('=') for x in d.features.getFeatures()]}, 'bestRoleIndex': d.bestRoleIndex, 'entityNode': d.entityNode.toString(), 'eventNode': d.eventNode.toString(), 'guessRole': d.guessRole, 'role': d.role(), 'sentence': d.sentence.toString(), 'word': d.word  } for d in listbio if d.role() != 'NONE' or d.guessRole != 'NONE']

print('Pickling %d biodatums' % len(pickleMe))
print('Throwing out %d NONE-NONEs' % (len(listbio) - len(pickleMe)))
fn = 'lin'
if (args.useArgid):
    fn = args.useArgid
if (args.useBinarize):
    fn = 'log'
with open(join(wd, 'lib/Dataset', fn + '.p'), 'wb') as fp:
    pickle.dump(pickleMe, fp)
    fp.close()
Execution.finish()
